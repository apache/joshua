/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.joshua.decoder;

import static org.apache.joshua.decoder.ff.FeatureMap.hashFeature;
import static org.apache.joshua.decoder.ff.tm.hash_based.TextGrammarFactory.createCustomGrammar;
import static org.apache.joshua.decoder.ff.tm.hash_based.TextGrammarFactory.createGlueTextGrammar;
import static org.apache.joshua.util.Constants.spaceSeparator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureMap;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.PhraseModel;
import org.apache.joshua.decoder.ff.StatefulFF;
import org.apache.joshua.decoder.ff.lm.LanguageModelFF;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.decoder.ff.tm.OwnerId;
import org.apache.joshua.decoder.ff.tm.OwnerMap;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.ff.tm.hash_based.TextGrammarFactory;
import org.apache.joshua.decoder.ff.tm.packed.PackedGrammar;
import org.apache.joshua.decoder.io.TranslationRequestStream;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.io.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigValue;

/**
 * This class handles decoder initialization and the complication introduced by multithreading.
 *
 * After initialization, the main entry point to the Decoder object is
 * decodeAll(TranslationRequest), which returns a set of Translation objects wrapped in an iterable
 * TranslationResponseStream object. It is important that we support multithreading both (a) across the sentences
 * within a request and (b) across requests, in a round-robin fashion. This is done by maintaining a
 * fixed sized concurrent thread pool. When a new request comes in, a RequestParallelizer thread is
 * launched. This object iterates over the request's sentences, obtaining a thread from the
 * thread pool, and using that thread to decode the sentence. If a decoding thread is not available,
 * it will block until one is in a fair (FIFO) manner. RequestParallelizer thereby permits intra-request
 * parallelization by separating out reading the input stream from processing the translated sentences,
 * but also ensures that round-robin parallelization occurs, since RequestParallelizer uses the
 * thread pool before translating each request.
 *
 * TODO(fhieber): this documentation should be updated
 * A decoding thread is handled by DecoderTask and launched from DecoderThreadRunner. The purpose
 * of the runner is to record where to place the translated sentence when it is done (i.e., which
 * TranslationResponseStream object). TranslationResponseStream itself is an iterator whose next() call blocks until the next
 * translation is available.
 *
 * @author Matt Post post@cs.jhu.edu
 * @author Zhifei Li, zhifei.work@gmail.com
 * @author wren ng thornton wren@users.sourceforge.net
 * @author Lane Schwartz dowobeha@users.sourceforge.net
 * @author Kellen Sunderland kellen.sunderland@gmail.com
 * @author Felix Hieber felix.hieber@gmail.com
 */
public class Decoder {

  private static final Logger LOG = LoggerFactory.getLogger(Decoder.class);

  /*
   * Holds the common (immutable) decoder state (features, grammars etc.) after initialization
   */
  private final DecoderConfig decoderConfig;
  
  private static final ImmutableList<String> GRAMMAR_PACKAGES = ImmutableList.of(
      "org.apache.joshua.decoder.ff.tm.hash_based",
      "org.apache.joshua.decoder.ff.tm.packed",
      "org.apache.joshua.decoder.phrase");
  
  private static final ImmutableList<String> FEATURE_PACKAGES = ImmutableList.of(
      "org.apache.joshua.decoder.ff",
      "org.apache.joshua.decoder.ff.lm",
      "org.apache.joshua.decoder.ff.phrase");
  
  /**
   * Constructor method that creates a new decoder using the specified configuration file.
   *
   * @param joshuaConfiguration a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   */
  public Decoder(Config config) {
    this.decoderConfig = initialize(config); 
  }
  
  /**
   * Returns the default Decoder flags.
   */
  public static Config getDefaultFlags() {
    final ConfigParseOptions options = ConfigParseOptions.defaults().setAllowMissing(false);
    return ConfigFactory.parseResources(Decoder.class, "Decoder.conf", options).resolve();
  }
  
  /**
   * Returns the DecoderConfig
   */
  public DecoderConfig getDecoderConfig() {
    return decoderConfig;
  }

  /**
   * This function is the main entry point into the decoder. It translates all the sentences in a
   * (possibly boundless) set of input sentences. Each request launches its own thread to read the
   * sentences of the request.
   *
   * @param request the populated {@link TranslationRequestStream}
   * @throws RuntimeException if any fatal errors occur during translation
   * @return an iterable, asynchronously-filled list of TranslationResponseStream
   */
  public TranslationResponseStream decodeAll(TranslationRequestStream request) {
    TranslationResponseStream results = new TranslationResponseStream(request);
    CompletableFuture.runAsync(() -> decodeAllAsync(request, results));
    return results;
  }

  private void decodeAllAsync(TranslationRequestStream request,
                              TranslationResponseStream responseStream) {

    // Give the threadpool a friendly name to help debuggers
    final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("TranslationWorker-%d")
            .setDaemon(true)
            .build();
    int numParallelDecoders = this.decoderConfig.getFlags().getInt("num_parallel_decoders");
    ExecutorService executor = Executors.newFixedThreadPool(numParallelDecoders, threadFactory);
    try {
      for (; ; ) {
        Sentence sentence = request.next();

        if (sentence == null) {
          break;
        }

        executor.execute(() -> {
          try {
            Translation result = decode(sentence);
            responseStream.record(result);
          } catch (Throwable ex) {
            responseStream.propagate(ex);
          }
        });
      }
      responseStream.finish();
    } finally {
      executor.shutdown();
    }
  }


  /**
   * Decode call for a single sentence.
   * Creates a sentence-specific {@link DecoderConfig} including
   * sentence-specific OOVGrammar.
   *
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @return the sentence {@link org.apache.joshua.decoder.Translation}
   */
  public Translation decode(Sentence sentence) {
    final DecoderConfig sentenceConfig = createSentenceDecoderConfig(sentence, decoderConfig);
    final DecoderTask decoderTask = new DecoderTask(sentenceConfig, sentence);
    return decoderTask.translate();
  }
  
  /**
   * Creates a sentence-specific {@link DecoderConfig}.
   * Most importantly, adds an OOV grammar for the words of this
   * sentence.
   */
  private static DecoderConfig createSentenceDecoderConfig(
      final Sentence sentence, final DecoderConfig globalConfig) {
    
    // create a new list of grammars that includes the OOVgrammar
    // this is specific to the search algorithm
    final ImmutableList.Builder<Grammar> grammars = new ImmutableList.Builder<>();
    switch (globalConfig.getSearchAlgorithm()) {
    case cky:
      grammars
        .add(TextGrammarFactory.createOovGrammarForSentence(sentence, globalConfig));
    case stack:
      grammars 
        .add(TextGrammarFactory.createEndRulePhraseTable(sentence, globalConfig))
        .add(TextGrammarFactory.createOovPhraseTable(sentence, globalConfig));
    }
    
    return new DecoderConfig(
        globalConfig.getFlags(),
        globalConfig.getFeatureFunctions(),
        grammars.addAll(globalConfig.getGrammars()).build(),
        globalConfig.getCustomGrammar(),
        globalConfig.getVocabulary(),
        globalConfig.getWeights(),
        globalConfig.getFeatureMap(),
        globalConfig.getOwnerMap());
  }

  /**
   * Clean shutdown of Decoder, resetting all
   * static variables, such that any other instance of Decoder
   * afterwards gets a fresh start.
   */
  public void cleanUp() {
    resetGlobalState();
  }

  public static void resetGlobalState() {
    // clear/reset static variables
    OwnerMap.clear();
    FeatureMap.clear();
    Vocabulary.clear();
    Vocabulary.unregisterLanguageModels();
    LanguageModelFF.resetLmIndex();
    StatefulFF.resetGlobalStateIndex();
  }

  /**
   * Initialize all parts of the Decoder.
   */
  private DecoderConfig initialize(final Config config) {
    
    LOG.info("Initializing decoder ...");
    long initTime = System.currentTimeMillis();
    
    /*
     * (1) read weights (denoted by parameter "weights-file")
     * or directly in the Joshua config. Config file values take precedent.
     */
    final FeatureVector weights = readWeights(config);
    
    /*
     * (2) initialize/instantiate translation grammars
     * Unfortunately this can not be static due to customPhraseTable member.
     */
    final List<Grammar> grammars = initializeTranslationGrammars(config);
    final Grammar customGrammar = createCustomGrammar(SearchAlgorithm.valueOf(config.getString("search_algorithm")));
    grammars.add(customGrammar);
    
    /*
     * (3) initialize/instantiate feature functions 
     */
    final ImmutableList<FeatureFunction> featureFunctions = initializeFeatureFunctions(config, grammars, weights);
    
    /*
     * (4) Optionally sort the grammars for cube-pruning
     */
    if (config.getBoolean("amortized_sorting")) {
      LOG.info("Grammar sorting happening lazily on-demand.");
    } else {
      long preSortTime = System.currentTimeMillis();
      for (final Grammar grammar : grammars) {
        grammar.sortGrammar(featureFunctions);
      }
      LOG.info("Grammar sorting took {} seconds.", (System.currentTimeMillis() - preSortTime) / 1000);
    }
    
    LOG.info("Initialization done ({} seconds)", (System.currentTimeMillis() - initTime) / 1000);
    // TODO(fhieber): right now we still rely on static variables for vocab etc.
    // this should be changed and then we pass the instance of vocab etc. in here
    return new DecoderConfig(
        config,
        featureFunctions,
        ImmutableList.copyOf(grammars),
        customGrammar,
        null,
        weights,
        null,
        null);
  }

  /**
   * Returns a list of initialized {@link Grammar}s
   */
  private List<Grammar> initializeTranslationGrammars(final Config config) {
    
    final List<Grammar> result = new ArrayList<>();
    
    // collect packedGrammars to check if they use a shared vocabulary
    final List<PackedGrammar> packedGrammars = new ArrayList<>();
    
    final long startTime = System.currentTimeMillis();
    
    for (final Config grammarConfig : config.getConfigList("grammars")) {
      final Class<?> clazz = getClassFromPackages(grammarConfig.getString("class"), GRAMMAR_PACKAGES);
      try {
        final Constructor<?> constructor = clazz.getConstructor(Config.class);
        final Grammar grammar = (Grammar) constructor.newInstance(grammarConfig);
        result.add(grammar);
      } catch (Exception e) {
        LOG.error("Unable to instantiate grammar '{}'", clazz.getName());
        Throwables.propagate(e);
      }
    }
    
    if (result.isEmpty()) {
      
      LOG.warn("no grammars supplied! Supplying dummy glue grammar.");
      result.add(createGlueTextGrammar(
          config.getString("goal_symbol"),
          config.getString("default_non_terminal")));
      
    } else {
      
      checkSharedVocabularyChecksumsForPackedGrammars(packedGrammars);
      
      if (config.getBoolean("lattice_decoding")) {
        LOG.info("Creating an epsilon-deleting grammar");
        result.add(TextGrammarFactory.addEpsilonDeletingGrammar(
            config.getString("goal_symbol"),
            config.getString("default_non_terminal")));
      }
    }
    
    LOG.info("Grammar loading took: {} seconds.", (System.currentTimeMillis() - startTime) / 1000);
    return result;
  }

  /**
   * Checks if multiple packedGrammars have the same vocabulary by comparing their vocabulary file checksums.
   */
  private static void checkSharedVocabularyChecksumsForPackedGrammars(final List<PackedGrammar> packed_grammars) {
    String previous_checksum = "";
    for (PackedGrammar grammar : packed_grammars) {
      final String checksum = grammar.computeVocabularyChecksum();
      if (previous_checksum.isEmpty()) {
        previous_checksum = checksum;
      } else {
        if (!checksum.equals(previous_checksum)) {
          throw new RuntimeException(
              "Trying to load multiple packed grammars with different vocabularies!" +
                  "Have you packed them jointly?");
        }
        previous_checksum = checksum;
      }
    }
  }

  /**
    * This function reads the weights for the model either
    * from the weights_file in format
    * NAME VALUE
    * once per line;
    * or from the weights section in the {@link Config} object.
    * The latter take precedence.
    */
  private static FeatureVector readWeights(final Config config) {
    final FeatureVector weights = new FeatureVector(5);
    
    // read from optional weights_file
    if (config.hasPath("weights_file")
        && new File(config.getString("weights_file")).exists()) {
      final String weightsFilename = config.getString("weights_file");
      try (LineReader lineReader = new LineReader(weightsFilename);) {
        for (String line : lineReader) {
          line = line.replaceAll(spaceSeparator, " ");
          if (line.equals("") || line.startsWith("#") || line.startsWith("//") || line.indexOf(' ') == -1) {
            continue;
          }
          final String tokens[] = line.split(spaceSeparator);
          String feature = tokens[0];
          final float value = Float.parseFloat(tokens[1]);

          // Kludge for compatibility with Moses tuners
          if (config.getBoolean("moses")) {
            feature = demoses(feature);
          }

          weights.put(hashFeature(feature), value);
        }
        LOG.info("Read {} weights from file '{}'", weights.size(), weightsFilename);
      } catch (IOException e) {
        Throwables.propagate(e);
      }
    }
    
    // overwrite with config values
    for (Entry<String, ConfigValue> entry : config.getConfig("weights").entrySet()) {
      final String name = entry.getKey();
      float value = ((Number) entry.getValue().unwrapped()).floatValue();
      weights.put(hashFeature(name), value);
    }
    LOG.info("Read {} weights", weights.size());
    return weights;
  }

  private static String demoses(String feature) {
    if (feature.endsWith("="))
      feature = feature.replace("=", "");
    if (feature.equals("OOV_Penalty"))
      feature = "OOVPenalty";
    else if (feature.startsWith("tm-") || feature.startsWith("lm-"))
      feature = feature.replace("-",  "_");
    return feature;
  }

  /**
   * Initializes & instantiates feature functions.
   * Required a list of previously loaded grammars to instantiate the PhraseModel feature function
   * as well.
   */
  private static ImmutableList<FeatureFunction> initializeFeatureFunctions(
      final Config config, final List<Grammar> grammars, final FeatureVector weights) {
    
    final ImmutableList.Builder<FeatureFunction> result = new ImmutableList.Builder<>();
    
    // (1) create PhraseModel feature function for every owner
    final Set<OwnerId> ownersSeen = new HashSet<>();
    for (final Grammar grammar: grammars) {
      final OwnerId owner = grammar.getOwner();
      if (!ownersSeen.contains(owner)) {
        result.add(new PhraseModel(owner, ConfigFactory.empty(), weights));
        ownersSeen.add(owner);
      }
    }
    
    // (2) instantiate other feature functions by class name
    for (Config featureConfig : config.getConfigList("feature_functions")) {
      final Class<?> clazz = getClassFromPackages(featureConfig.getString("class"), FEATURE_PACKAGES);
      try {
        final Constructor<?> constructor = clazz.getConstructor(Config.class, FeatureVector.class);
        final FeatureFunction feature = (FeatureFunction) constructor.newInstance(featureConfig, weights);
        result.add(feature);
      } catch (Exception e) {
        LOG.error("Unable to instantiate feature '{}'", clazz.getName());
        Throwables.propagate(e);
      }
    }
    
    final ImmutableList<FeatureFunction> features = result.build(); 
    for (final FeatureFunction feature : features) {
      LOG.info("FEATURE: {}", feature.logString());
    }
    return features;
  }
  
  /**
   * Searches a list of paths for classes and returns the first one found.
   * Used for instantiating grammars and feature functions.
   */
  private static Class<?> getClassFromPackages(String className, ImmutableList<String> packages) {
    Class<?> clas = null;
    for (String path : packages) {
      try {
        clas = Class.forName(String.format("%s.%s", path, className));
        break;
      } catch (ClassNotFoundException e) {
        try {
          clas = Class.forName(String.format("%s.%sFF", path, className));
          break;
        } catch (ClassNotFoundException e2) {
          // do nothing
        }
      }
    }
    return clas;
  }

  /**
   * Adds a rule to the custom grammar.
   *
   * @param rule the rule to add
   */
  public void addCustomRule(Rule rule) {
    decoderConfig.getCustomGrammar().addRule(rule);
    rule.estimateRuleCost(decoderConfig.getFeatureFunctions());
  }

  public Grammar getCustomPhraseTable() {
    return decoderConfig.getCustomGrammar();
  }
}
