/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "lm/enumerate_vocab.hh"
#include "lm/model.hh"
#include "lm/left.hh"
#include "lm/state.hh"
#include "util/murmur_hash.hh"

#include <iostream>

#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <pthread.h>

// Grr.  Everybody's compiler is slightly different and I'm trying to not depend on boost.
#include <unordered_set>
#include <vector>

// Verify that jint and lm::ngram::WordIndex are the same size. If this breaks
// for you, there's a need to revise probString.
namespace {

template<bool> struct StaticCheck {
};

template<> struct StaticCheck<true> {
  typedef bool StaticAssertionPassed;
};

typedef StaticCheck<sizeof(jint) == sizeof(lm::WordIndex)>::StaticAssertionPassed FloatSize;

// Could be uint64_t if you wanted to have 33-bit support.
typedef uint32_t StateIndex;
typedef std::vector<lm::ngram::ChartState> StateVector;

class HashIndex : public std::unary_function<StateIndex, uint64_t> {
  public:
    explicit HashIndex(const StateVector &vec) : vec_(vec) {}

    uint64_t operator()(StateIndex index) const {
      return hash_value(vec_[index]);
    }

  private:
    const StateVector &vec_;
};

class EqualIndex : public std::binary_function<StateIndex, StateIndex, bool> {
  public:
    explicit EqualIndex(const StateVector &vec) : vec_(vec) {}

    bool operator()(StateIndex first, StateIndex second) const {
      return vec_[first] == vec_[second];
    }

  private:
    const StateVector &vec_;
};

typedef std::unordered_set<StateIndex, HashIndex, EqualIndex> Lookup;

/**
 * A Chart bundles together a vector holding CharStates and an unordered_set of StateIndexes
 * which provides a mapping between StateIndexes and the positions of ChartStates in the vector.
 * This allows for duplicate states to avoid allocating separate state objects at multiple places
 * throughout a sentence.
 */
class Chart {
  public:
    Chart(long* ngramBuffer) : 
    ngramBuffer_(ngramBuffer),
    lookup_(1000, HashIndex(vec_), EqualIndex(vec_)) {}

    StateIndex Intern(const lm::ngram::ChartState &state) {
      vec_.push_back(state);
      std::pair<Lookup::iterator, bool> ins(lookup_.insert(vec_.size() - 1));
      if (!ins.second) {
        vec_.pop_back();
      }
      return *ins.first + 1; // +1 so that the first id is 1, not 0.  We use sign bit to 
                             // distinguish ChartState from vocab id.  
    }

    const lm::ngram::ChartState &InterpretState(StateIndex index) const {
      return vec_[index - 1];
    }
    long* ngramBuffer_;

  private:
    StateVector vec_;
    Lookup lookup_;
};

// Vocab ids above what the vocabulary knows about are unknown and should
// be mapped to that.
void MapArray(const std::vector<lm::WordIndex>& map, jint *begin, jint *end) {
  for (jint *i = begin; i < end; ++i) {
    *i = map[*i];
  }
}

char *PieceCopy(const StringPiece &str) {
  char *ret = (char*) malloc(str.size() + 1);
  memcpy(ret, str.data(), str.size());
  ret[str.size()] = 0;
  return ret;
}

// Rather than handle several different instantiations over JNI, we'll just
// do virtual calls C++-side.
class VirtualBase {
public:
  virtual ~VirtualBase() {
  }

  // compute/return n-gram probability for array of Joshua word ids
  virtual float Prob(jint *begin, jint *end) const = 0;

  // Compute/return n-gram probability for array of lm:WordIndexes
  virtual float ProbForWordIndexArray(jint *begin, jint *end) const = 0;

  // Returns the internal lm::WordIndex for a string
  virtual uint GetLmId(const StringPiece& word) const = 0;

  virtual bool IsLmOov(const int joshua_id) const = 0;

  virtual bool IsKnownWordIndex(const lm::WordIndex& id) const = 0;

  virtual float ProbRule(lm::ngram::ChartState& state, const Chart &chart) const = 0;

  virtual float ProbString(jint * const begin, jint * const end,
      jint start) const = 0;

  virtual float EstimateRule(jlong *begin, jlong *end) const = 0;

  virtual uint8_t Order() const = 0;

  virtual bool RegisterWord(const StringPiece& word, const int joshua_id) = 0;

protected:
  VirtualBase() {
  }
};

template<class Model> class VirtualImpl: public VirtualBase {
public:
  VirtualImpl(const char *name) :
      m_(name) {
    // Insert unknown id mapping.
    map_.push_back(0);
  }

  ~VirtualImpl() {
  }

  float Prob(jint * const begin, jint * const end) const {
    // map Joshua word ids to lm::WordIndexes
    MapArray(map_, begin, end);
    return ProbForWordIndexArray(begin, end);
  }

  float ProbForWordIndexArray(jint * const begin, jint * const end) const {
    std::reverse(begin, end - 1);
    lm::ngram::State ignored;
    return m_.FullScoreForgotState(
        reinterpret_cast<const lm::WordIndex*>(begin),
        reinterpret_cast<const lm::WordIndex*>(end - 1), *(end - 1),
        ignored).prob;
  }

  uint GetLmId(const StringPiece& word) const {
    return m_.GetVocabulary().Index(word);
  }

  bool IsLmOov(const int joshua_id) const {
    if (map_.size() <= joshua_id) {
      return true;
    }
    return !IsKnownWordIndex(map_[joshua_id]);
  }

  bool IsKnownWordIndex(const lm::WordIndex& id) const {
      return id != m_.GetVocabulary().NotFound();
  }

  float ProbRule(lm::ngram::ChartState& state, const Chart &chart) const {

    // By convention the first long in the ngramBuffer denotes the size of the buffer
    long* begin = chart.ngramBuffer_ + 1;
    long* end = begin + *chart.ngramBuffer_;

    if (begin == end) return 0.0;
    lm::ngram::RuleScore<Model> ruleScore(m_, state);

    if (*begin < 0) {
      ruleScore.BeginNonTerminal(chart.InterpretState(-*begin));
    } else {
      const lm::WordIndex word = map_[*begin];
      if (word == m_.GetVocabulary().BeginSentence()) {
        ruleScore.BeginSentence();
      } else {
        ruleScore.Terminal(word);
      }
    }
    for (jlong* i = begin + 1; i != end; i++) {
      long word = *i;
      if (word < 0)
        ruleScore.NonTerminal(chart.InterpretState(-word));
      else
        ruleScore.Terminal(map_[word]);
    }
    return ruleScore.Finish();
  }

  float EstimateRule(jlong * const begin, jlong * const end) const {
    if (begin == end) return 0.0;
    lm::ngram::ChartState nullState;
    lm::ngram::RuleScore<Model> ruleScore(m_, nullState);

    if (*begin < 0) {
      ruleScore.Reset();
    } else {
      const lm::WordIndex word = map_[*begin];
      if (word == m_.GetVocabulary().BeginSentence()) {
        ruleScore.BeginSentence();
      } else {
        ruleScore.Terminal(word);
      }
    }
    for (jlong* i = begin + 1; i != end; i++) {
      long word = *i;
      if (word < 0)
        ruleScore.Reset();
      else
        ruleScore.Terminal(map_[word]);
    }
    return ruleScore.Finish();
  }

  float ProbString(jint * const begin, jint * const end, jint start) const {
    MapArray(map_, begin, end);

    float prob;
    lm::ngram::State state;
    if (start == 0) {
      prob = 0;
      state = m_.NullContextState();
    } else {
      std::reverse(begin, begin + start);
      prob = m_.FullScoreForgotState(
          reinterpret_cast<const lm::WordIndex*>(begin),
          reinterpret_cast<const lm::WordIndex*>(begin + start),
          begin[start], state).prob;
      ++start;
    }
    lm::ngram::State state2;
    for (const jint *i = begin + start;;) {
      if (i >= end)
        break;
      float got = m_.Score(state, *i, state2);
      i++;
      prob += got;
      if (i >= end)
        break;
      got = m_.Score(state2, *i, state);
      i++;
      prob += got;
    }
    return prob;
  }

  uint8_t Order() const {
    return m_.Order();
  }

  bool RegisterWord(const StringPiece& word, const int joshua_id) {
    if (map_.size() <= joshua_id) {
      map_.resize(joshua_id + 1, 0);
    }
    bool already_present = false;
    if (map_[joshua_id] != 0)
      already_present = true;
    map_[joshua_id] = m_.GetVocabulary().Index(word);
    return already_present;
  }

private:
  Model m_;
  std::vector<lm::WordIndex> map_;
};

VirtualBase *ConstructModel(const char *file_name) {
  using namespace lm::ngram;
  ModelType model_type;
  if (!RecognizeBinary(file_name, model_type))
    model_type = HASH_PROBING;
  switch (model_type) {
  case PROBING:
    return new VirtualImpl<ProbingModel>(file_name);
  case REST_PROBING:
    return new VirtualImpl<RestProbingModel>(file_name);
  case TRIE:
    return new VirtualImpl<TrieModel>(file_name);
  case ARRAY_TRIE:
    return new VirtualImpl<ArrayTrieModel>(file_name);
  case QUANT_TRIE:
    return new VirtualImpl<QuantTrieModel>(file_name);
  case QUANT_ARRAY_TRIE:
    return new VirtualImpl<QuantArrayTrieModel>(file_name);
  default:
    UTIL_THROW(
        lm::FormatLoadException,
        "Unrecognized file format " << (unsigned) model_type
            << " in file " << file_name);
  }
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_construct(
    JNIEnv *env, jclass, jstring file_name) {
  const char *str = env->GetStringUTFChars(file_name, 0);
  if (!str)
    return 0;

  VirtualBase *ret;
  try {
    ret = ConstructModel(str);
  } catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  env->ReleaseStringUTFChars(file_name, str);
  return reinterpret_cast<jlong>(ret);
}

JNIEXPORT void JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_destroy(
    JNIEnv *env, jclass, jlong pointer) {
  delete reinterpret_cast<VirtualBase*>(pointer);
}

JNIEXPORT jlong JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_createPool(
    JNIEnv *env, jclass, jobject arr) {
  jlong* ngramBuffer = (jlong*)env->GetDirectBufferAddress(arr);
  Chart *newChart = new Chart(ngramBuffer);
  return reinterpret_cast<long>(newChart);
}

JNIEXPORT void JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_destroyPool(
    JNIEnv *env, jclass, jlong pointer) {
  delete reinterpret_cast<Chart*>(pointer);
}

JNIEXPORT jint JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_order(
    JNIEnv *env, jclass, jlong pointer) {
  return reinterpret_cast<VirtualBase*>(pointer)->Order();
}

JNIEXPORT jboolean JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_registerWord(
    JNIEnv *env, jclass, jlong pointer, jstring word, jint id) {
  const char *str = env->GetStringUTFChars(word, 0);
  if (!str)
    return false;
  jint ret;
  try {
    ret = reinterpret_cast<VirtualBase*>(pointer)->RegisterWord(str, id);
  } catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  env->ReleaseStringUTFChars(word, str);
  return ret;
}

JNIEXPORT jfloat JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_prob(
    JNIEnv *env, jclass, jlong pointer, jintArray arr) {
  jint length = env->GetArrayLength(arr);
  if (length <= 0)
    return 0.0;
  // GCC only.
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);

  return reinterpret_cast<const VirtualBase*>(pointer)->Prob(values,
      values + length);
}

JNIEXPORT jfloat JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_probForString(
    JNIEnv *env, jclass, jlong pointer, jobjectArray arr) {
  jint length = env->GetArrayLength(arr);
  if (length <= 0)
    return 0.0;
  jint values[length];
  const VirtualBase* lm_base = reinterpret_cast<const VirtualBase*>(pointer);
  for (int i=0; i<length; i++) {
      jstring word = (jstring) env->GetObjectArrayElement(arr, i);
      const char *str = env->GetStringUTFChars(word, 0);
      values[i] = lm_base->GetLmId(str);
      env->ReleaseStringUTFChars(word, str);
  }
  return lm_base->ProbForWordIndexArray(values,
      values + length);
}

JNIEXPORT jboolean JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_isLmOov(
    JNIEnv *env, jclass, jlong pointer, jint word) {
    const VirtualBase* lm_base = reinterpret_cast<const VirtualBase*>(pointer);
    return lm_base->IsLmOov(word);
}

JNIEXPORT jboolean JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_isKnownWord(
    JNIEnv *env, jclass, jlong pointer, jstring word) {
    const char *str = env->GetStringUTFChars(word, 0);
    if (!str)
      return false;
    bool ret;
    const VirtualBase* lm_base = reinterpret_cast<const VirtualBase*>(pointer);
    lm::WordIndex id = lm_base->GetLmId(str);
    ret = lm_base->IsKnownWordIndex(id);
    env->ReleaseStringUTFChars(word, str);
    return ret;
}

JNIEXPORT jfloat JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_probString(
    JNIEnv *env, jclass, jlong pointer, jintArray arr, jint start) {
  jint length = env->GetArrayLength(arr);
  if (length <= start)
    return 0.0;
  // GCC only.
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);

  return reinterpret_cast<const VirtualBase*>(pointer)->ProbString(values,
      values + length, start);
}

union FloatConverter {
  float f;
  uint32_t i;
};

JNIEXPORT jlong JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_probRule(
  JNIEnv *env, jclass, jlong pointer, jlong chartPtr) {

  // Compute the probability
  lm::ngram::ChartState outState;
  const VirtualBase *base = reinterpret_cast<const VirtualBase*>(pointer);
  Chart* chart = reinterpret_cast<Chart*>(chartPtr);
  FloatConverter prob;
  prob.f = base->ProbRule(outState, *chart);
  StateIndex index = chart->Intern(outState);
  return static_cast<uint64_t>(index) << 32 | static_cast<uint64_t>(prob.i);
}

JNIEXPORT jfloat JNICALL Java_org_apache_joshua_decoder_ff_lm_KenLM_estimateRule(
  JNIEnv *env, jclass, jlong pointer, jlongArray arr) {
  jint length = env->GetArrayLength(arr);
  // GCC only.
  jlong values[length];
  env->GetLongArrayRegion(arr, 0, length, values);

  // Compute the probability
  return reinterpret_cast<const VirtualBase*>(pointer)->EstimateRule(values,
      values + length);
}

} // extern
