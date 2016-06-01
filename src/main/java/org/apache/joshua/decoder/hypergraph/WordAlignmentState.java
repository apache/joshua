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
package org.apache.joshua.decoder.hypergraph;

import static java.lang.Integer.MAX_VALUE;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.joshua.decoder.ff.tm.Rule;

/**
 * This class encodes a derivation state in terms of a list of alignment points.
 * Whenever a child instance is substituted into the parent instance, we need to
 * adjust source indexes of the alignments.
 * 
 * @author fhieber
 */
public class WordAlignmentState {

  /**
   * each element in this list corresponds to a token on the target side of the
   * rule. The values of the elements correspond to the aligned source token on
   * the source side of the rule.
   */
  private List<AlignedSourceTokens> trgPoints;
  private final int srcStart;
  /** number of NTs we need to substitute. */
  private int numNT;
  /** grows with substitutions of child rules. Reaches original Rule span if substitutions are complete */
  private int srcLength;

  /**
   * construct AlignmentState object from a virgin Rule and its source span.
   * Determines if state is complete (if no NT present)
   * 
   * @param rule the input Rule
   * @param start the start index
   */
  public WordAlignmentState(final Rule rule, final int start) {
    trgPoints = new LinkedList<AlignedSourceTokens>();
    srcLength = rule.getFrench().length;
    numNT = rule.getArity();
    srcStart = start;
    final Map<Integer, List<Integer>> alignmentMap = rule.getAlignmentMap();
    final int[] nonTerminalSourcePositions = rule.getNonTerminalSourcePositions();
    final int[] trg = rule.getEnglish();
    // for each target index, create a TargetAlignmentPoint
    for (int trgIndex = 0; trgIndex < trg.length; trgIndex++) {
      final AlignedSourceTokens trgPoint = new AlignedSourceTokens();

      if (trg[trgIndex] >= 0) { // this is a terminal symbol, check for alignment
        if (alignmentMap.containsKey(trgIndex)) {
          // add source indexes to TargetAlignmentPoint
          for (int srcIdx : alignmentMap.get(trgIndex)) {
            trgPoint.add(srcStart + srcIdx);
          }
        } else { // this target word is NULL-aligned
          trgPoint.setNull();
        }
      } else { // this is a nonterminal ([X]) [actually its the (negative) index of the NT in the source]
        trgPoint.setNonTerminal(); // mark as non-terminal
        final int absoluteNonTerminalSourcePosition = srcStart + nonTerminalSourcePositions[Math.abs(trg[trgIndex]) - 1];
        trgPoint.add(absoluteNonTerminalSourcePosition);
      }
      trgPoints.add(trgPoint);
    }
  }

  /**
   * if there are no more NonTerminals to substitute,
   * this state is said to be complete
   * @return true if complete
   */
  public boolean isComplete() {
    return numNT == 0;
  }

  /**
   * builds the final alignment string in the standard alignment format: src -
   * trg. Sorted by trg indexes. Disregards the sentence markers.
   * @return result string
   */
  public String toFinalString() {
    final StringBuilder sb = new StringBuilder();
    int t = 0;
    for (AlignedSourceTokens pt : trgPoints) {
      for (int s : pt) {
        sb.append(String.format(" %d-%d", s-1, t-1)); // disregard sentence markers
      }
      t++;
    }
    final String result = sb.toString();
    if (!result.isEmpty()) {
      return result.substring(1);
    }
    return result;
  }
  
  /**
   * builds the final alignment list.
   * each entry in the list corresponds to a list of aligned source tokens.
   * First and last item in trgPoints is skipped.
   * @return a final alignment list
   */
  public List<List<Integer>> toFinalList() {
    final List<List<Integer>> alignment = new ArrayList<List<Integer>>(trgPoints.size());
    if (trgPoints.isEmpty()) {
      return alignment;
    }
    final ListIterator<AlignedSourceTokens> it = trgPoints.listIterator();
    it.next(); // skip first item (sentence marker)
    while (it.hasNext()) {
      final AlignedSourceTokens alignedSourceTokens = it.next();
      if (it.hasNext()) { // if not last element in trgPoints
        final List<Integer> newAlignedSourceTokens = new ArrayList<Integer>();
        for (Integer sourceIndex : alignedSourceTokens) {
          newAlignedSourceTokens.add(sourceIndex - 1); // shift by one to disregard sentence marker
        }
        alignment.add(newAlignedSourceTokens);
      }
    }
    return alignment;
  }

  /**
   * String representation for debugging.
   */
  @Override
  public String toString() {
    return String.format("%s , len=%d start=%d, isComplete=%s",
        trgPoints.toString(), srcLength, srcStart, this.isComplete());
  }

  /**
   * Substitutes a child WorldAlignmentState into this instance at the next
   * nonterminal slot. Also shifts the indeces in this instance by the span/width of the
   * child that is to be substituted.
   * Substitution order is determined by the source-first traversal through the hypergraph.
   * 
   * @param child The child
   */
  public void substituteIn(WordAlignmentState child) {
    // find the index of the NonTerminal where we substitute the child targetPoints into.
    // The correct NT is the first one on the SOURCE side.
    // Also shift all trgPoints by the child length.
    int substitutionIndex = 0;
    int sourcePosition = MAX_VALUE;
    for (final ListIterator<AlignedSourceTokens> trgPointsIterator = trgPoints.listIterator(); trgPointsIterator.hasNext();) {
      final AlignedSourceTokens trgPoint = trgPointsIterator.next();
      trgPoint.shiftBy(child.srcStart, child.srcLength - 1);
      if (trgPoint.isNonTerminal() && trgPoint.get(0) < sourcePosition) {
        sourcePosition = trgPoint.get(0);
        substitutionIndex = trgPointsIterator.previousIndex();
      }
    }
    
    // point and remove NT element determined from above
    final ListIterator<AlignedSourceTokens> insertionIterator = trgPoints.listIterator(substitutionIndex);
    insertionIterator.next();
    insertionIterator.remove();
    
    // insert child target points and set them to final.
    for (AlignedSourceTokens childElement : child.trgPoints) {
      childElement.setFinal();
      insertionIterator.add(childElement);
    }
    
    // update length and number of non terminal slots
    this.srcLength += child.srcLength - 1; // -1 (NT)
    this.numNT--;
  }
}
