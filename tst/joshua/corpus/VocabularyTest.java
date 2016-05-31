// Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
package joshua.corpus;

import static joshua.util.FormatUtils.isNonterminal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class VocabularyTest {
  private static final String WORD1 = "word1";
  private static final String WORD2 = "word2";
  private static final String NON_TERMINAL = "[X]";
  private static final String GOAL = "[GOAL]";

  @Before
  public void init() {
    Vocabulary.clear();
  }
  
  @After
  public void deinit() {
    Vocabulary.clear();
  }
  
  @Test
  public void givenVocabulary_whenEmpty_thenOnlyContainsUnknownWord() {
    assertTrue(Vocabulary.hasId(Vocabulary.UNKNOWN_ID));
    assertFalse(Vocabulary.hasId(1));
    assertFalse(Vocabulary.hasId(-1));
    assertEquals(Vocabulary.UNKNOWN_WORD, Vocabulary.word(Vocabulary.UNKNOWN_ID));
    assertEquals(1, Vocabulary.size());
  }
  
  @Test
  public void givenVocabulary_whenNewWord_thenMappingIsAdded() {
    final int FIRST_WORD_ID = 1;
    assertFalse(Vocabulary.hasId(FIRST_WORD_ID));
    assertEquals(FIRST_WORD_ID, Vocabulary.id(WORD1));
    //should return same id after second call:
    assertEquals(FIRST_WORD_ID, Vocabulary.id(WORD1));
    assertTrue(Vocabulary.hasId(FIRST_WORD_ID));
    assertEquals(WORD1, Vocabulary.word(FIRST_WORD_ID));
    assertEquals(2, Vocabulary.size());
  }
  
  @Test
  public void givenVocabulary_whenCheckingStringInBracketsOrNegativeNumber_thenIsNonTerminal() {
    //non-terminals
    assertTrue(isNonterminal(NON_TERMINAL));
    //terminals
    assertFalse(isNonterminal(WORD1));
    assertFalse(isNonterminal("[]"));
    assertFalse(isNonterminal("["));
    assertFalse(isNonterminal("]"));
    assertFalse(isNonterminal(""));
    
    //negative numbers indicate non-terminals
    assertTrue(isNonterminal(-1));
    assertTrue(isNonterminal(-5));
    
    //positive numbers indicate terminals:
    assertFalse(isNonterminal(0));
    assertFalse(isNonterminal(5));

    
  }
  
  @Test
  public void givenVocabulary_whenNonTerminal_thenReturnsStrictlyPositiveNonTerminalIndices() {
    final int FIRST_NON_TERMINAL_INDEX = 1;
    assertTrue(Vocabulary.id(NON_TERMINAL) < 0);
    assertTrue(Vocabulary.hasId(FIRST_NON_TERMINAL_INDEX));
    assertTrue(Vocabulary.hasId(-FIRST_NON_TERMINAL_INDEX));
    
    assertTrue(Vocabulary.id("") > 0);
    assertTrue(Vocabulary.id(WORD1) > 0);
    
    final int SECOND_NON_TERMINAL_INDEX = 4;
    assertTrue(Vocabulary.id(GOAL) < 0);
    assertTrue(Vocabulary.hasId(SECOND_NON_TERMINAL_INDEX));
    assertTrue(Vocabulary.hasId(-SECOND_NON_TERMINAL_INDEX));
    
    assertTrue(Vocabulary.id(WORD2) > 0);
  }
  
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  
  @Test
  public void givenVocabulary_whenWritenAndReading_thenVocabularyStaysTheSame() throws IOException {
    File vocabFile = folder.newFile();
    
    int id1 = Vocabulary.id(WORD1);
    int id2 = Vocabulary.id(NON_TERMINAL);
    int id3 = Vocabulary.id(WORD2);
    
    Vocabulary.write(vocabFile.getAbsolutePath());
    
    Vocabulary.clear();
    
    Vocabulary.read(vocabFile);
    
    assertEquals(4, Vocabulary.size()); //unknown word + 3 other words
    assertTrue(Vocabulary.hasId(id1));
    assertTrue(Vocabulary.hasId(id2));
    assertTrue(Vocabulary.hasId(id3));
    assertEquals(id1, Vocabulary.id(WORD1));
    assertEquals(id2, Vocabulary.id(NON_TERMINAL));
    assertEquals(id3, Vocabulary.id(WORD2));
  }
}
