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
package org.apache.joshua.decoder.ff.fragmentlm;

import static org.apache.joshua.decoder.ff.FeatureMap.hashFeature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.StatefulFF;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Feature function that reads in a list of language model fragments and matches them against the
 * hypergraph. This allows for language model fragment "glue" features, which fire when LM fragments
 * (supplied as input) are assembled. These LM fragments are presumably useful in ensuring
 * grammaticality and can be independent of the translation model fragments.</p>
 * 
 * <p>Usage: in the Joshua Configuration file, put</p>
 * 
 * <code>feature-function = FragmentLM -lm LM_FRAGMENTS_FILE -map RULE_FRAGMENTS_MAP_FILE</code>
 * 
 * <p>LM_FRAGMENTS_FILE is a pointer to a file containing a list of fragments that it should look for.
 * The format of the file is one fragment per line in PTB format, e.g.:</p>
 * 
 * <code>(S NP (VP (VBD said) SBAR) (. .))</code>
 * 
 * <p>RULE_FRAGMENTS_MAP_FILE points to a file that maps fragments to the flattened SCFG rule format
 * that Joshua uses. This mapping is necessary because Joshua's rules have been flattened, meaning
 * that their internal structure has been removed, yet this structure is needed for matching LM
 * fragments. The format of the file is</p>
 * 
 * <code>FRAGMENT ||| RULE-TARGET-SIDE</code>
 * 
 * <p>for example,</p>
 * 
 * <code>(S (NP (DT the) (NN man)) VP .) ||| the man [VP,1] [.,2] (SBAR (IN that) (S (NP (PRP he)) (VP
 * (VBD was) (VB done)))) ||| that he was done (VP (VBD said) SBAR) ||| said SBAR</code>
 * 
 * @author Matt Post post@cs.jhu.edu
 */
public class FragmentLMFF extends StatefulFF {

  private static final Logger LOG = LoggerFactory.getLogger(FragmentLMFF.class);

  /*
   * When building a fragment from a rule rooted in the hypergraph, this parameter determines how
   * deep we'll go. Smaller values mean less hypergraph traversal but may also limit the LM
   * fragments that can be fired.
   */
  private int BUILD_DEPTH = 1;

  /*
   * The maximum depth of a fragment, defined as the longest path from the fragment root to any of
   * its leaves.
   */
  private int MAX_DEPTH = 0;

  /*
   * This is the minimum depth for lexicalized LM fragments. This allows you to easily exclude small
   * depth-one fragments that may be overfit to the training data. A depth of 1 (the default) does
   * not exclude any fragments.
   */
  private int MIN_LEX_DEPTH = 1;

  /*
   * Set to true to activate meta-features.
   */
  private boolean OPTS_DEPTH = false;

  /*
   * This contains a list of the language model fragments, indexed by LHS.
   */
  private HashMap<String, ArrayList<Tree>> lmFragments = null;

  private int numFragments = 0;

  /* The location of the file containing the language model fragments */
  private String fragmentLMFile = "";

  /**
   * @param weights a {@link org.apache.joshua.decoder.ff.FeatureVector} with weights
   * @param args arguments passed to the feature function
   * @param config the {@link org.apache.joshua.decoder.JoshuaConfiguration}
   */
  public FragmentLMFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "FragmentLMFF", args, config);

    lmFragments = new HashMap<String, ArrayList<Tree>>();

    fragmentLMFile = parsedArgs.get("lm");
    BUILD_DEPTH = Integer.parseInt(parsedArgs.get("build-depth"));
    MAX_DEPTH = Integer.parseInt(parsedArgs.get("max-depth"));
    MIN_LEX_DEPTH = Integer.parseInt(parsedArgs.get("min-lex-depth"));

    /* Read in the language model fragments */
    try {
      Collection<Tree> trees = PennTreebankReader.readTrees(fragmentLMFile);
      for (Tree fragment : trees) {
        addLMFragment(fragment);

        // System.err.println(String.format("Read fragment: %s",
        // lmFragments.get(lmFragments.size()-1)));
      }
    } catch (IOException e) {
      throw new RuntimeException(String.format("* WARNING: couldn't read fragment LM file '%s'",
          fragmentLMFile), e);
    }
    LOG.info("FragmentLMFF: Read {} LM fragments from '{}'", numFragments, fragmentLMFile);
  }

  /**
   * Add the provided fragment to the language model, subject to some filtering.
   * 
   * @param fragment a {@link org.apache.joshua.decoder.ff.fragmentlm.Tree} fragment
   */
  public void addLMFragment(Tree fragment) {
    if (lmFragments == null)
      return;

    int fragmentDepth = fragment.getDepth();

    if (MAX_DEPTH != 0 && fragmentDepth > MAX_DEPTH) {
      LOG.warn("Skipping fragment {} (depth {} > {})", fragment, fragmentDepth, MAX_DEPTH);
      return;
    }

    if (MIN_LEX_DEPTH > 1 && fragment.isLexicalized() && fragmentDepth < MIN_LEX_DEPTH) {
      LOG.warn("Skipping fragment {} (lex depth {} < {})", fragment, fragmentDepth, MIN_LEX_DEPTH);
      return;
    }

    if (lmFragments.get(fragment.getRule()) == null) {
      lmFragments.put(fragment.getRule(), new ArrayList<Tree>());
    }
    lmFragments.get(fragment.getRule()).add(fragment);
    numFragments++;
  }
  
  /**
   * This function computes the features that fire when the current rule is applied. The features
   * that fire are any LM fragments that match the fragment associated with the current rule. LM
   * fragments may recurse over the tail nodes, following 1-best backpointers until the fragment
   * either matches or fails.
   * 
   * @param rule {@link org.apache.joshua.decoder.ff.tm.Rule} to be utilized within computation
   * @param tailNodes {@link java.util.List} of {@link org.apache.joshua.decoder.hypergraph.HGNode} tail nodes
   * @param i todo
   * @param j todo
   * @param sourcePath information about a path taken through the source {@link org.apache.joshua.lattice.Lattice}
   * @param sentence {@link org.apache.joshua.lattice.Lattice} input
   * @param acc {@link org.apache.joshua.decoder.ff.FeatureFunction.Accumulator} object permitting generalization of feature computation
   * @return the new dynamic programming state (null for stateless features)
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath, 
      Sentence sentence, Accumulator acc) {

    /*
     * Get the fragment associated with the target side of this rule.
     * 
     * This could be done more efficiently. For example, just build the tree fragment once and then
     * pattern match against it. This would circumvent having to build the tree possibly once every
     * time you try to apply a rule.
     */
    Tree baseTree = Tree.buildTree(rule, tailNodes, BUILD_DEPTH);

    Stack<Tree> nodeStack = new Stack<Tree>();
    nodeStack.add(baseTree);
    while (!nodeStack.empty()) {
      Tree tree = nodeStack.pop();
      if (tree == null)
        continue;

      if (lmFragments.get(tree.getRule()) != null) {
        for (Tree fragment : lmFragments.get(tree.getRule())) {
//           System.err.println(String.format("Does\n  %s match\n  %s??\n  -> %s", fragment, tree,
//           match(fragment, tree)));

          if (fragment.getLabel() == tree.getLabel() && match(fragment, tree)) {
//             System.err.println(String.format("  FIRING: matched %s against %s", fragment, tree));
            acc.add(hashFeature(fragment.escapedString()), 1);
            if (OPTS_DEPTH)
              if (fragment.isLexicalized())
                acc.add(hashFeature(String.format("FragmentFF_lexdepth%d", fragment.getDepth())), 1);
              else
                acc.add(hashFeature(String.format("FragmentFF_depth%d", fragment.getDepth())), 1);
          }
        }
      }

      // We also need to try matching rules against internal nodes of the fragment corresponding to
      // this
      // rule
      if (tree.getChildren() != null)
        for (Tree childNode : tree.getChildren()) {
          if (!childNode.isBoundary())
            nodeStack.add(childNode);
        }
    }

    return new FragmentState(baseTree);
  }

  /**
   * Matches the fragment against the (possibly partially-built) tree. Assumption
   * 
   * @param fragment the language model fragment
   * @param tree the tree to match against (expanded from the hypergraph)
   * @return
   */
  private boolean match(Tree fragment, Tree tree) {
    // System.err.println(String.format("MATCH(%s,%s)", fragment, tree));

    /* Make sure the root labels match. */
    if (fragment.getLabel() != tree.getLabel()) {
      return false;
    }

    /* Same number of kids? */
    List<Tree> fkids = fragment.getChildren();
    if (fkids.size() > 0) {
      List<Tree> tkids = tree.getChildren();
      if (fkids.size() != tkids.size()) {
        return false;
      }

      /* Do the kids match on all labels? */
      for (int i = 0; i < fkids.size(); i++)
        if (fkids.get(i).getLabel() != tkids.get(i).getLabel())
          return false;

      /* Recursive match. */
      for (int i = 0; i < fkids.size(); i++) {
        if (!match(fkids.get(i), tkids.get(i)))
          return false;
      }
    }

    return true;
  }

  @Override
  public DPState computeFinal(HGNode tailNodes, int i, int j, SourcePath sourcePath, Sentence sentence,
      Accumulator acc) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public float estimateFutureCost(Rule rule, DPState state, Sentence sentence) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float estimateCost(Rule rule, Sentence sentence) {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * Maintains a state pointer used by KenLM to implement left-state minimization. 
   * 
   * @author Matt Post post@cs.jhu.edu
   * @author Juri Ganitkevitch juri@cs.jhu.edu
   */
  public class FragmentState extends DPState {

    private Tree tree = null;

    public FragmentState(Tree tree) {
      this.tree = tree;
    }

    /**
     * Every tree is unique.
     * 
     * Some savings could be had here if we grouped together items with the same string.
     */
    @Override
    public int hashCode() {
      return tree.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      return (other instanceof FragmentState && this == other);
    }

    @Override
    public String toString() {
      return String.format("[FragmentState %s]", tree);
    }
  }
}
