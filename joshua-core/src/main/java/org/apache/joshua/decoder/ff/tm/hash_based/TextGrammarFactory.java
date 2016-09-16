package org.apache.joshua.decoder.ff.tm.hash_based;

import static java.util.Collections.emptyList;
import static org.apache.joshua.decoder.ff.tm.OwnerMap.UNKNOWN_OWNER;
import static org.apache.joshua.util.Constants.CUSTOM_OWNER;
import static org.apache.joshua.util.Constants.GLUE_OWNER;
import static org.apache.joshua.util.Constants.OOV_OWNER;

import java.util.HashSet;
import java.util.Set;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.DecoderConfig;
import org.apache.joshua.decoder.SearchAlgorithm;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.ff.tm.format.HieroFormatReader;
import org.apache.joshua.decoder.phrase.Hypothesis;
import org.apache.joshua.decoder.phrase.PhraseTable;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.decoder.segment_file.Token;
import org.apache.joshua.lattice.Arc;
import org.apache.joshua.lattice.Node;
import org.apache.joshua.util.FormatUtils;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Provides some static functions to create default/backoff/oov/glue TextGrammars
 * that are dynamically created during decoding.
 */
public class TextGrammarFactory {
  
  public static TextGrammar createGlueTextGrammar(String goalSymbol, String defaultNonTerminal) {
    final Config config = ConfigFactory.parseMap(
        ImmutableMap.of("owner", GLUE_OWNER, "span_limit", "-1"), "Glue Grammar Config");
    final TextGrammar glueGrammar = new TextGrammar(config);
    final String goalNT = FormatUtils.cleanNonTerminal(goalSymbol);
    final String defaultNT = FormatUtils.cleanNonTerminal(defaultNonTerminal);
    
    final String[] ruleStrings = new String[] {
        String.format("[%s] ||| %s ||| %s ||| 0", goalNT, Vocabulary.START_SYM,
            Vocabulary.START_SYM),
        String.format("[%s] ||| [%s,1] [%s,2] ||| [%s,1] [%s,2] ||| -1", goalNT, goalNT, defaultNT,
            goalNT, defaultNT),
        String.format("[%s] ||| [%s,1] %s ||| [%s,1] %s ||| 0", goalNT, goalNT,
            Vocabulary.STOP_SYM, goalNT, Vocabulary.STOP_SYM) };
    
    try(HieroFormatReader reader = new HieroFormatReader(glueGrammar.getOwner());) {
      for (String ruleString : ruleStrings) {
        Rule rule = reader.parseLine(ruleString);
        glueGrammar.addRule(rule);
        // glue rules do not any features
        rule.estimateRuleCost(emptyList());
      }
    }
    return glueGrammar;
  }
  
  public static Grammar createCustomGrammar(SearchAlgorithm searchAlgorithm) {
    final Config config = ConfigFactory.parseMap(
        ImmutableMap.of("owner", CUSTOM_OWNER, "span_limit", "20"), "Custom Grammar Config");
    switch (searchAlgorithm) {
    case stack:
      return new PhraseTable(config);
    case cky:
      return new TextGrammar(config);
    default:
      return null;
    }
  }
  
  public static Grammar addEpsilonDeletingGrammar(String goalSymbol, String defaultNonTerminal) {
    final Config config = ConfigFactory.parseMap(
        ImmutableMap.of("owner", "lattice", "span_limit", "-1"), "Epsilon Grammar Config");
    final TextGrammar latticeGrammar = new TextGrammar(config);
    final String goalNT = FormatUtils.cleanNonTerminal(goalSymbol);
    final String defaultNT = FormatUtils.cleanNonTerminal(defaultNonTerminal);

    //FIXME: arguments changed to match string format on best effort basis.  Author please review.
    final String ruleString = String.format("[%s] ||| [%s,1] <eps> ||| [%s,1] ||| ", goalNT, defaultNT, defaultNT);
    
    try(HieroFormatReader reader = new HieroFormatReader(latticeGrammar.getOwner());) {
      final Rule rule = reader.parseLine(ruleString);
      latticeGrammar.addRule(rule);
      rule.estimateRuleCost(emptyList());
    }
    return latticeGrammar;
  }
  
  public static Grammar createOovGrammarForSentence(final Sentence sentence, DecoderConfig config) {
    final Config grammarConfig = ConfigFactory.parseMap(
        ImmutableMap.of("owner", OOV_OWNER, "span_limit", "20"), "OOV grammar config");
    final TextGrammar oovGrammar = new TextGrammar(grammarConfig);
    final Set<Integer> words = getOovCandidateWords(sentence);
    for (int sourceWord: words) {
      oovGrammar.addOOVRules(sourceWord, sentence.getFlags(), config.getFeatureFunctions());
    }
    // Sort all the rules (not much to actually do, this just marks it as sorted)
    oovGrammar.sortGrammar(config.getFeatureFunctions());
    return oovGrammar;
  }
  
  public static PhraseTable createOovPhraseTable(Sentence sentence, DecoderConfig config) {
    final Config grammarConfig = ConfigFactory.parseMap(
        ImmutableMap.of("owner", OOV_OWNER, "span_limit", "0"), "OOV phrase table config");
    final PhraseTable oovPhraseTable = new PhraseTable(grammarConfig);
    final Set<Integer> words = getOovCandidateWords(sentence);
    for (int sourceWord: words) {
      oovPhraseTable.addOOVRules(sourceWord, sentence.getFlags(), config.getFeatureFunctions());
    }
    // Sort all the rules (not much to actually do, this just marks it as sorted)
    oovPhraseTable.sortGrammar(config.getFeatureFunctions());
    return oovPhraseTable;
  }
  
  /**
   * Returns a set of integer ids for which OOV rules will be created.
   * The set is determined by the flag trueOovsOnly.
   */
  private static Set<Integer> getOovCandidateWords(final Sentence sentence) {
    final Set<Integer> words = new HashSet<>();
    final boolean trueOovsOnly = sentence.getFlags().getBoolean("true_oovs_only");
    for (Node<Token> node : sentence.getLattice()) {
      for (Arc<Token> arc : node.getOutgoingArcs()) {
        int sourceWord = arc.getLabel().getWord();
        if (sourceWord == Vocabulary.id(Vocabulary.START_SYM)
            || sourceWord == Vocabulary.id(Vocabulary.STOP_SYM))
          continue;

        // Determine if word is actual OOV.
        if (trueOovsOnly && ! Vocabulary.hasId(sourceWord))
          continue;

        words.add(sourceWord);
      }
    }
    return words;
  }
  
  public static PhraseTable createEndRulePhraseTable() {
    final Config grammarConfig = ConfigFactory.parseMap(
        ImmutableMap.of("owner", UNKNOWN_OWNER, "span_limit", "0"), "End Rule Phrase Table Config");
    final PhraseTable endRulePhraseTable = new PhraseTable(grammarConfig);
    endRulePhraseTable.addRule(Hypothesis.END_RULE);
    return endRulePhraseTable;
  }

}
