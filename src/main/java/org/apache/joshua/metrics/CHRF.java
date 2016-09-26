/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.joshua.metrics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;


/**
 *
 * An implementation of the chrF evaluation metric for tuning.
 * It is based on the original code by Maja Popovic [1] with the following main modifications:
 * - Adapted to extend Joshua's EvaluationMetric class
 * - Use of a length penalty to prevent chrF to prefer too long (with beta %gt; 1) or too short (with beta &lt; 1) translations
 * - Use of hash tables for efficient n-gram matching
 *
 * The metric has 2 parameters:
 * - Beta. It assigns beta times more weight to recall than to precision. By default 1.
 *   Although for evaluation the best correlation was found with beta=3, we've found the
 *   best results for tuning so far with beta=1
 * - Max-ngram. Maximum n-gram length (characters). By default 6.
 *
 * If you use this metric in your research please cite [2].
 *
 * [1] Maja Popovic. 2015. chrF: character n-gram F-score for automatic MT evaluation.
 * In Proceedings of the Tenth Workshop on Statistical Machine Translation. Lisbon, Portugal, pages 392–395.
 * [2] Víctor Sánchez Cartagena and Antonio Toral. 2016.
 * Abu-MaTran at WMT 2016 Translation Task: Deep Learning, Morphological Segmentation and Tuning on Character Sequences.
 * In Proceedings of the First Conference on Machine Translation (WMT16). Berlin, Germany.

 * @author Antonio Toral
 */
public class CHRF extends EvaluationMetric {
    private static final Logger logger = Logger.getLogger(CHRF.class.getName());

    protected double beta = 1;
    protected double factor;
    protected int maxGramLength = 6; // The maximum n-gram we care about
    //private double[] nGramWeights; //TODO to weight them differently

    //private String metricName;
    //private boolean toBeMinimized;
    //private int suffStatsCount;


  public CHRF()
  {
      this(1, 6);
  }

  public CHRF(String[] CHRF_options)
  {
    //
    //
    // process the Metric_options array
    //
    //
    this(Double.parseDouble(CHRF_options[0]), Integer.parseInt(CHRF_options[1]));
  }

  public CHRF(double bt, int mxGrmLn){
    if (bt > 0) {
      beta = bt;
    } else {
      logger.severe("Beta must be positive");
      System.exit(1);
    }

    if (mxGrmLn >= 1) {
      maxGramLength = mxGrmLn;
    } else {
      logger.severe("Maximum gram length must be positive");
      System.exit(1);
    }

    initialize(); // set the data members of the metric
  }

  @Override
  protected void initialize()
  {
    metricName = "CHRF";
    toBeMinimized = false;
    suffStatsCount = 4 * maxGramLength;
    factor = Math.pow(beta, 2);
  }

  @Override
  public double bestPossibleScore() { return 100.0; }

  @Override
  public double worstPossibleScore() { return 0.0; }

  protected String separateCharacters(String s)
  {
    String s_chars = "";
    //alternative implementation (the one below seems more robust)
    /*for (int i = 0; i < s.length(); i++) {
        if (s.charAt(i) == ' ') continue;
        s_chars += s.charAt(i) + " ";
    }
    System.out.println("CHRF separate chars1: " + s_chars);*/

    String[] words = s.split("\\s+");
    for (String w: words) {
        for (int i = 0; i<w.length(); i++)
            s_chars += w.charAt(i);
    }

    //System.out.println("CHRF separate chars: " + s_chars);
    return s_chars;
  }


  protected HashMap<String, Integer>[] getGrams(String s)
  {
    HashMap<String, Integer>[] grams = new HashMap[1 + maxGramLength];
    grams[0] = null;
    for (int n = 1; n <= maxGramLength; ++n) {
      grams[n] = new HashMap<>();
    }


    for (int n=1; n<=maxGramLength; n++){
      String gram;
      for (int i = 0; i < s.length() - n + 1; i++){
          gram = s.substring(i, i+n);
          if(grams[n].containsKey(gram)){
            int old_count = grams[n].get(gram);
            grams[n].put(gram, old_count+1);
          } else {
            grams[n].put(gram, 1);
          }
      }

    }

    /* debugging
    String key, value;
    for (int n=1; n<=maxGramLength; n++){
      System.out.println("Grams of order " + n);
      for (String gram: grams[n].keySet()){
        key = gram.toString();
        value = grams[n].get(gram).toString();
        System.out.println(key + " " + value);
      }
    }*/

    return grams;
  }


  protected int[] candRefErrors(HashMap<String, Integer> ref, HashMap<String, Integer> cand)
  {
      int[] to_return = {0,0};
      String gram;
      int cand_grams = 0;
      int candGramCount, refGramCount;
      int errors = 0;

    for (String s : (cand.keySet())) {
      gram = s;
      candGramCount = cand.get(gram);
      cand_grams += candGramCount;
      if (ref.containsKey(gram)) {
        refGramCount = ref.get(gram);
        if (candGramCount > refGramCount) {
          int error_here = candGramCount - refGramCount;
          errors += error_here;
        }
      } else {
        errors += candGramCount;
      }
    }

      //System.out.println("  Ngrams not found: " + not_found);

      to_return[0] = cand_grams;
      to_return[1] = errors;

      return to_return;
  }

  @Override
  public int[] suffStats(String cand_str, int i) //throws Exception
  {
    int[] stats = new int[suffStatsCount];

    //TODO check unicode chars correctly split
    String cand_char = separateCharacters(cand_str);
    String ref_char = separateCharacters(refSentences[i][0]);

    HashMap<String, Integer>[] grams_cand = getGrams(cand_char);
    HashMap<String, Integer>[] grams_ref = getGrams(ref_char);

    for (int n = 1; n <= maxGramLength; ++n) {
        //System.out.println("Calculating precision...");
        int[] precision_vals = candRefErrors(grams_ref[n], grams_cand[n]);
        //System.out.println("  length: " + precision_vals[0] + ", errors: " + precision_vals[1]);
        //System.out.println("Calculating recall...");
        int[] recall_vals = candRefErrors(grams_cand[n], grams_ref[n]);
        //System.out.println("  length: " + recall_vals[0] + ", errors: " + recall_vals[1]);

        stats[4*(n-1)] = precision_vals[0]; //cand_grams
        stats[4*(n-1)+1] = precision_vals[1]; //errors (precision)
        stats[4*(n-1)+2] = recall_vals[0]; //ref_grams
        stats[4*(n-1)+3] = recall_vals[1]; //errors (recall)
    }

    return stats;
  }


  @Override
  public double score(int[] stats)
  {
    int precision_ngrams, recall_ngrams, precision_errors, recall_errors;
    double[] precisions = new double[maxGramLength+1];
    double[] recalls = new double[maxGramLength+1];
    double[] fs = new double[maxGramLength+1];
    //double[] scs = new double[maxGramLength+1];
    double totalF = 0, totalSC;
    double lp = 1;

    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ") in NewMetric.score(int[])");
      System.exit(1);
    }

    for (int n = 1; n <= maxGramLength; n++) {
      precision_ngrams = stats[4 * (n - 1)];
      precision_errors = stats[4 * (n - 1) + 1];
      recall_ngrams = stats[4 * (n - 1) + 2];
      recall_errors = stats[4 * (n - 1) + 3];

      if (precision_ngrams != 0)
        precisions[n] = 100 - 100*precision_errors/ (double)precision_ngrams;
      else precisions[n] = 0;

      if (recall_ngrams != 0)
        recalls[n] = 100 - 100*recall_errors/ (double)recall_ngrams;
      else
        recalls[n] = 0;

      if(precisions[n] != 0 || recalls[n] != 0)
        fs[n] = (1+factor) * recalls[n] * precisions[n] / (factor * precisions[n] + recalls[n]);
      else
        fs[n] = 0;

      //System.out.println("Precision (n=" + n + "): " + precisions[n]);
      //System.out.println("Recall (n=" + n + "): " + recalls[n]);
      //System.out.println("F (n=" + n + "): " + fs[n]);

      totalF += (1/(double)maxGramLength) * fs[n];
    }

    //length penalty
    if (beta>1){ //penalise long translations
        lp = Math.min(1, stats[2]/(double)stats[0]);
    } else if (beta < 1){ //penalise short translations
        lp = Math.min(1, stats[0]/(double)stats[2]);
    }
    totalSC = totalF*lp;

    //System.out.println("Precision (total): " + totalPrecision);
    //System.out.println("Recall (total):" + totalRecall);
    //System.out.println("F (total): " + totalF);

    return totalSC;
  }


  @Override
  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner)
  {
    System.out.println(metricName + " = " + score(stats));

    //
    //
    // optional (for debugging purposes)
    //
    //
  }

}

