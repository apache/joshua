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
package org.apache.joshua.decoder.ff;

import static org.apache.joshua.decoder.ff.FeatureMap.hashFeature;

import java.util.List;

import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;

import com.typesafe.config.Config;

/**
 * <p>This class defines Joshua's feature function interface, for both sparse and
 * dense features. It is immediately inherited by StatelessFF and StatefulFF,
 * which provide functionality common to stateless and stateful features,
 * respectively. Any feature implementation should extend those classes, and not
 * this one. The distinction between stateless and stateful features is somewhat
 * narrow: all features have the opportunity to return an instance of a
 * {@link DPState} object, and stateless ones just return null.</p>
 * 
 * <p>Features in Joshua work like templates. Each feature function defines any
 * number of actual features, which are associated with weights. The task of the
 * feature function is to compute the features that are fired in different
 * circumstances and then return the inner product of those features with the
 * weight vector. Feature functions can also produce estimates of their future
 * cost (via {@link org.apache.joshua.decoder.ff.FeatureFunction#estimateCost(Rule, Sentence)}); 
 * these values are not used in computing the
 * score, but are only used for sorting rules during cube pruning. The
 * individual features produced by each template should have globally unique
 * names; a good convention is to prefix each feature with the name of the
 * template that produced it.</p>
 * 
 * <p>Joshua does not retain individual feature values while decoding, since this
 * requires keeping a sparse feature vector along every hyperedge, which can be
 * expensive. Instead, it computes only the weighted cost of each edge. If the
 * individual feature values are requested, the feature functions are replayed
 * in post-processing, say during k-best list extraction. This is implemented in
 * a generic way by passing an {@link Accumulator} object to the compute()
 * function. During decoding, the accumulator simply sums weighted features in a
 * scalar. During k-best extraction, when individual feature values are needed,
 * a {@link FeatureAccumulator} is used to retain the individual values.</p>
 * 
 * @author Matt Post post@cs.jhu.edu
 * @author Juri Ganitkevich juri@cs.jhu.edu
 */
public abstract class FeatureFunction {

  /*
   * The name of the feature function; this generally matches the weight name on
   * the config file. This can also be used as a prefix for feature / weight
   * names, for templates that define multiple features.
   */
  protected String name = null;
  
  /*
   * The hashed feature id correspondig to name. This can be changed if name is changed as well
   * but provides a good default id for most cases. 
   */
  protected int featureId;

  // The configuration passed to the feature
  protected final Config featureConfig;
  
  // reference to the global decoder weights
  protected final FeatureVector weights;

  public String getName() {
    return name;
  }

  // Whether the feature has state.
  public abstract boolean isStateful();

  public FeatureFunction(final String name, final Config featureConfig, final FeatureVector weights) {
    this.name = name;
    this.featureId = hashFeature(this.name); // TODO(fhieber) proper hashing here
    this.featureConfig = featureConfig;
    this.weights = weights;
  }

  public String logString() {
    return String.format("%s (weight %.3f)", name, weights.getOrDefault(featureId));
  }

  /**
   * This is the main function for defining feature values. The implementor
   * should compute all the features along the hyperedge, calling 
   * {@link org.apache.joshua.decoder.ff.Accumulator#add(String, float)}
   * for each feature. It then returns the newly-computed dynamic
   * programming state for this feature (for example, for the
   * {@link org.apache.joshua.decoder.ff.lm.LanguageModelFF} feature, this returns the new language model
   * context). For stateless features, this value is null.
   * 
   * Note that the accumulator accumulates *unweighted* feature values. The
   * feature vector is multiplied times the weight vector later on.
   * 
   * @param rule {@link org.apache.joshua.decoder.ff.tm.Rule} to be utilized within computation
   * @param tailNodes {@link java.util.List} of {@link org.apache.joshua.decoder.hypergraph.HGNode} tail nodes
   * @param i todo
   * @param j todo
   * @param sourcePath information about a path taken through the source {@link org.apache.joshua.lattice.Lattice}
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @param acc {@link org.apache.joshua.decoder.ff.Accumulator} object permitting generalization of feature computation
   * @return the new dynamic programming state (null for stateless features)
   */
  public abstract DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, Sentence sentence, Accumulator acc);

  /**
   * Feature functions must override this. StatefulFF and StatelessFF provide
   * reasonable defaults since most features do not fire on the goal node.
   * 
   * @param tailNode single {@link org.apache.joshua.decoder.hypergraph.HGNode} representing tail node
   * @param i todo
   * @param j todo
   * @param sourcePath information about a path taken through the source {@link org.apache.joshua.lattice.Lattice}
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @param acc {@link org.apache.joshua.decoder.ff.Accumulator} object permitting generalization of feature computation
   * @return the DPState (null if none)
   */
  public abstract DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc);

  /**
   * This is a convenience function for retrieving the features fired when
   * applying a rule, provided for backward compatibility.
   * 
   * Returns the *unweighted* cost of the features delta computed at this
   * position. Note that this is a feature delta, so existing feature costs of
   * the tail nodes should not be incorporated, and it is very important not to
   * incorporate the feature weights. This function is used in the kbest
   * extraction code but could also be used in computing the cost.
   * 
   * @param rule {@link org.apache.joshua.decoder.ff.tm.Rule} to be utilized within computation
   * @param tailNodes {@link java.util.List} of {@link org.apache.joshua.decoder.hypergraph.HGNode} tail nodes
   * @param i todo
   * @param j todo
   * @param sourcePath information about a path taken through the source {@link org.apache.joshua.lattice.Lattice}
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @return an *unweighted* feature delta
   */
  public final FeatureVector computeFeatures(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, Sentence sentence) {

    FeatureAccumulator features = new FeatureAccumulator();
    compute(rule, tailNodes, i, j, sourcePath, sentence, features);
    return features.getFeatures();
  }

  /**
   * This function is called for the final transition. For example, the
   * LanguageModel feature function treats the last rule specially. It needs to
   * return the *weighted* cost of applying the feature. Provided for backward
   * compatibility.
   * 
   * @param tailNode single {@link org.apache.joshua.decoder.hypergraph.HGNode} representing tail node
   * @param i todo
   * @param j todo
   * @param sourcePath information about a path taken through the source {@link org.apache.joshua.lattice.Lattice}
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @return a *weighted* feature cost
   */
  public final float computeFinalCost(HGNode tailNode, int i, int j, SourcePath sourcePath,
      Sentence sentence) {

    final ScoreAccumulator score = new ScoreAccumulator(weights);
    computeFinal(tailNode, i, j, sourcePath, sentence, score);
    return score.getScore();
  }

  /**
   * Returns the *unweighted* feature delta for the final transition (e.g., for
   * the language model feature function). Provided for backward compatibility.
   * 
   * @param tailNode single {@link org.apache.joshua.decoder.hypergraph.HGNode} representing tail node
   * @param i todo
   * @param j todo
   * @param sourcePath information about a path taken through the source {@link org.apache.joshua.lattice.Lattice}
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @return an *weighted* feature vector
   */
  public final FeatureVector computeFinalFeatures(HGNode tailNode, int i, int j,
      SourcePath sourcePath, Sentence sentence) {

    FeatureAccumulator features = new FeatureAccumulator();
    computeFinal(tailNode, i, j, sourcePath, sentence, features);
    return features.getFeatures();
  }

  /**
   * This function is called when sorting rules for cube pruning. It must return
   * the *weighted* estimated cost of applying a feature. This need not be the
   * actual cost of applying the rule in context. Basically, it's the inner
   * product of the weight vector and all features found in the grammar rule,
   * though some features (like LanguageModelFF) can also compute some of their
   * values. This is just an estimate of the cost, which helps do better
   * sorting. Later, the real cost of this feature function is called via
   * compute();
   * 
   * @param rule {@link org.apache.joshua.decoder.ff.tm.Rule} to be utilized within computation
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @return the *weighted* cost of applying the feature.
   */
  public abstract float estimateCost(Rule rule, Sentence sentence);

  /**
   * This feature is called to produce a *weighted estimate* of the future cost
   * of applying this feature. This value is not incorporated into the model
   * score but is used in pruning decisions. Stateless features return 0.0f by
   * default, but Stateful features might want to override this.
   * 
   * @param rule {@link org.apache.joshua.decoder.ff.tm.Rule} to be utilized within computation
   * @param state todo
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @return the *weighted* future cost estimate of applying this rule in
   *         context.
   */
  public abstract float estimateFutureCost(Rule rule, DPState state, Sentence sentence);
}
