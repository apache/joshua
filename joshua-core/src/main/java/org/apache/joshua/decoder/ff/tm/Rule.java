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
package org.apache.joshua.decoder.ff.tm;

import static org.apache.joshua.decoder.ff.tm.OwnerMap.UNKNOWN_OWNER_ID;
import static org.apache.joshua.util.Constants.NT_REGEX;
import static org.apache.joshua.util.Constants.fieldDelimiter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.tm.format.HieroFormatReader;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines the interface for Rule. Components of a rule (left-hand-side,
 * source words, target words, features, alignments) are final and can not be modified.
 * This forces creators of Rule instances to decide on feature namespaces and owner in advances and greatly
 * simplifies the code.
 * 
 * @author Zhifei Li, zhifei.work@gmail.com
 * @author Matt Post post@cs.jhu.edu
 * @author fhieber
 */
public class Rule implements Comparator<Rule>, Comparable<Rule> {
  
  private static final Logger LOG = LoggerFactory.getLogger(Rule.class);
  
  /** left hand side vocabulary id */
  private final int lhs;

  /** source vocabulary ids */
  private final int[] source;
  
  /** target vocabulary ids */
  private final int[] target;
  
  /** arity of the rule (number of non-terminals) */
  protected final int arity;

  /** the {@link FeatureVector} associated with this {@link Rule} */
  private final FeatureVector featureVector;

  /** The {@link OwnerId} this rule belongs to. */
  private OwnerId owner = UNKNOWN_OWNER_ID;

  /**
   * This is the rule cost computed only from local rule context. This cost is
   * needed to sort the rules in the grammar for cube pruning, but isn't the full cost of applying
   * the rule (which will include contextual features that can't be computed until the rule is
   * applied).
   */
  private float estimatedCost = Float.NEGATIVE_INFINITY;

  private final byte[] alignments;
  
  /**
   * Constructs a rule given its dependencies. ownerId should be the same as used for
   * 'hashing'/creating the {@link FeatureVector} features.
   */
  public Rule(int lhs, int[] source, int[] target, int arity, FeatureVector features, byte[] alignments, OwnerId ownerId) {
    this.lhs = lhs;
    this.source = source;
    this.target = target;
    this.arity = arity;
    this.featureVector = features;
    this.alignments = alignments;
    this.owner = ownerId;
  }

  public int[] getTarget() {
    return this.target;
  }

  /**
   * Two Rules are equal of they have the same LHS, the same source RHS and the same target RHS.
   * 
   * @param o the object to check for equality
   * @return true if o is the same Rule as this rule, false otherwise
   */
  public boolean equals(Object o) {
    if (!(o instanceof Rule)) {
      return false;
    }
    Rule other = (Rule) o;
    if (getLHS() != other.getLHS()) {
      return false;
    }
    if (!Arrays.equals(getSource(), other.getSource())) {
      return false;
    }
    if (!Arrays.equals(target, other.getTarget())) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    // I just made this up. If two rules are equal they'll have the
    // same hashcode. Maybe someone else can do a better job though?
    int frHash = Arrays.hashCode(getSource());
    int enHash = Arrays.hashCode(target);
    return frHash ^ enHash ^ getLHS();
  }
  
  public int getArity() {
    return this.arity;
  }

  public OwnerId getOwner() {
    return this.owner;
  }

  public int getLHS() {
    return this.lhs;
  }

  public int[] getSource() {
    return this.source;
  }

  public FeatureVector getFeatureVector() {
    return featureVector;
  }

  /**
   * This function returns the estimated cost of a rule, which should have been computed when the
   * grammar was first sorted via a call to Rule::estimateRuleCost(). This function is a getter
   * only; it will not compute the value if it has not already been set. It is necessary in addition
   * to estimateRuleCost(models) because sometimes the value needs to be retrieved from contexts
   * that do not have access to the feature functions.
   * 
   * This function is called by the rule comparator when sorting the grammar. As such it may be
   * called many times and any implementation of it should be a cached implementation.
   * 
   * @return the estimated cost of the rule (a lower bound on the true cost)
   */
  public float getEstimatedCost() {
    return estimatedCost;
  }
  
  /**
   * This function estimates the cost of a rule, which is used for sorting the rules for cube
   * pruning. The estimated cost is basically the set of precomputable features (features listed
   * along with the rule in the grammar file) along with any other estimates that other features
   * would like to contribute (e.g., a language model estimate). This cost will be a lower bound on
   * the rule's actual cost.
   * 
   * The value of this function is used only for sorting the rules. When the rule is later applied
   * in context to particular hypernodes, the rule's actual cost is computed.
   * 
   * @param models the list of models available to the decoder
   * @return estimated cost of the rule
   */
  public float estimateRuleCost(List<FeatureFunction> models) {
    
    if (this.estimatedCost <= Float.NEGATIVE_INFINITY) {
      float result = 0.0f;
      LOG.debug("estimateRuleCost({} ;; {})", getSourceWords(), getTargetWords());
      for (final FeatureFunction ff : models) {
        float val = ff.estimateCost(this, null);
        LOG.debug("  FEATURE {} -> {}", ff.getName(), val);
        result += val; 
      }
      this.estimatedCost = result;
    }

    return estimatedCost;
  }

  /**
   * Returns an informative String for the rule, including estimated cost and the rule's owner.
   */
  @Override
  public String toString() {
    return new StringBuffer(textFormat())
        .append(fieldDelimiter)
        .append(getEstimatedCost())
        .append(fieldDelimiter)
        .append(OwnerMap.getOwner(getOwner()))
        .toString();
  }
  
  /**
   * Returns a string version of the rule parsable by the {@link HieroFormatReader}.
   */
  public String textFormat() {
    return new StringBuffer()
        .append(Vocabulary.word(this.getLHS()))
        .append(fieldDelimiter)
        .append(getSourceWords())
        .append(fieldDelimiter)
        .append(getTargetWords())
        .append(fieldDelimiter)
        .append(getFeatureVector().textFormat())
        .append(fieldDelimiter)
        .append(getAlignmentString())
        .toString();
  }

  /**
   * Returns an alignment as a sequence of integers. The integers at positions i and i+1 are paired,
   * with position i indexing the source and i+1 the target.
   * 
   * @return a byte[]
   */
  public byte[] getAlignment() {
    return this.alignments;
  }
  
  public String getAlignmentString() {
    byte[] alignments = getAlignment();
    if (alignments == null || alignments.length == 0) {
      return "";
    }
    final StringBuilder b = new StringBuilder();
    for (int i = 0; i < alignments.length - 1; i+=2) {
      b.append(alignments[i]).append("-").append(alignments[i+1]).append(" ");
    }
    return b.toString().trim();
  }

  /**
   * The nonterminals on the target side are pointers to the source side nonterminals (-1 and -2),
   * rather than being directly encoded. These number indicate the correspondence between the
   * nonterminals on each side, introducing a level of indirection however when we want to resolve
   * them. So to get the ID, we need to look up the corresponding source side ID.
   * 
   * @return The string of target words
   */
  public String getTargetWords() {
    int[] foreignNTs = getForeignNonTerminals();
  
    StringBuilder sb = new StringBuilder();
    for (Integer index : getTarget()) {
      if (index >= 0)
        sb.append(Vocabulary.word(index) + " ");
      else
        sb.append(Vocabulary.word(foreignNTs[-index - 1]).replace("]",
            String.format(",%d] ", Math.abs(index))));
    }
  
    return sb.toString().trim();
  }

  /**
   * Return the source nonterminals as list of Strings
   * 
   * @return a list of strings
   */
  public int[] getForeignNonTerminals() {
    int[] nts = new int[getArity()];
    int index = 0;
    for (int id : getSource())
      if (id < 0)
        nts[index++] = -id;
    return nts;
  }
  
  /**
   * Returns an array of size getArity() containing the source indeces of non terminals.
   * 
   * @return an array of size getArity() containing the source indeces of non terminals
   */
  public int[] getNonTerminalSourcePositions() {
    int[] nonTerminalPositions = new int[getArity()];
    int ntPos = 0;
    for (int sourceIdx = 0; sourceIdx < getSource().length; sourceIdx++) {
      if (getSource()[sourceIdx] < 0)
        nonTerminalPositions[ntPos++] = sourceIdx;
    }
    return nonTerminalPositions;
  }
  
  /**
   * Parses the Alignment byte[] into a Map from target to (possibly a list of) source positions.
   * Used by the WordAlignmentExtractor.
   * 
   * @return a {@link java.util.Map} of alignments
   */
  public Map<Integer, List<Integer>> getAlignmentMap() {
    byte[] alignmentArray = getAlignment();
    Map<Integer, List<Integer>> alignmentMap = new HashMap<Integer, List<Integer>>();
    if (alignmentArray != null) {
      for (int alignmentIdx = 0; alignmentIdx < alignmentArray.length; alignmentIdx += 2 ) {
        int s = alignmentArray[alignmentIdx];
        int t = alignmentArray[alignmentIdx + 1];
        List<Integer> values = alignmentMap.get(t);
        if (values == null)
          alignmentMap.put(t, values = new ArrayList<Integer>());
        values.add(s);
      }
    }
    return alignmentMap;
  }

  /**
   * Return the target nonterminals as list of Strings
   * 
   * @return list of strings
   */
  public int[] getTargetNonTerminals() {
    int[] nts = new int[getArity()];
    int[] foreignNTs = getForeignNonTerminals();
    int index = 0;
  
    for (int i : getTarget()) {
      if (i < 0)
        nts[index++] = foreignNTs[Math.abs(getTarget()[i]) - 1];
    }
  
    return nts;
  }

  private int[] getNormalizedTargetNonterminalIndices() {
    int[] result = new int[getArity()];
  
    int ntIndex = 0;
    for (Integer index : getTarget()) {
      if (index < 0)
        result[ntIndex++] = -index - 1;
    }
  
    return result;
  }

  public boolean isInverting() {
    int[] normalizedTargetNonTerminalIndices = getNormalizedTargetNonterminalIndices();
    if (normalizedTargetNonTerminalIndices.length == 2) {
      if (normalizedTargetNonTerminalIndices[0] == 1) {
        return true;
      }
    }
    return false;
  }

  public String getSourceWords() {
    return Vocabulary.getWords(getSource());
  }

  private Pattern getPattern() {
    String source = getSourceWords();
    String pattern = Pattern.quote(source);
    pattern = pattern.replaceAll(NT_REGEX, "\\\\E.+\\\\Q");
    pattern = pattern.replaceAll("\\\\Q\\\\E", "");
    pattern = "(?:^|\\s)" + pattern + "(?:$|\\s)";
    return Pattern.compile(pattern);
  }

  /**
   * Matches the string representation of the rule's source side against a sentence
   * 
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @return true if there is a match
   */
  public boolean matches(Sentence sentence) {
    return getPattern().matcher(sentence.fullSource()).find();
  }

  /**
   * This comparator is used for sorting the rules during cube pruning. An estimate of the cost
   * of each rule is computed and used to sort. 
   */
  public static Comparator<Rule> EstimatedCostComparator = new Comparator<Rule>() {
    public int compare(Rule rule1, Rule rule2) {
      return Float.compare(rule1.getEstimatedCost(),  rule2.getEstimatedCost());
    }
  };
  
  public int compare(Rule rule1, Rule rule2) {
    return EstimatedCostComparator.compare(rule1, rule2);
  }

  public int compareTo(Rule other) {
    return EstimatedCostComparator.compare(this, other);
  }
}
