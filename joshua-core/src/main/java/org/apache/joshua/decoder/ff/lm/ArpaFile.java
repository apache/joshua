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
package org.apache.joshua.decoder.ff.lm; 

import java.io.File; 
import java.io.FileInputStream; 
import java.io.FileNotFoundException; 
import java.io.IOException; 
import java.io.InputStream; 
import java.util.Iterator; 
import java.util.NoSuchElementException; 
import java.util.Scanner; 
import java.util.regex.Matcher; 
import java.util.regex.Pattern; 
import java.util.zip.GZIPInputStream; 

import org.apache.joshua.corpus.Vocabulary; 
import org.apache.joshua.util.Regex; 
import org.apache.joshua.util.io.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for reading ARPA language model files. 
 *  
 * @author Lane Schwartz 
 */ 
public class ArpaFile implements Iterable<ArpaNgram> { 

  private static final Logger LOG = LoggerFactory.getLogger(ArpaFile.class);

  /** Regular expression representing a blank line. */ 
  public static final Regex BLANK_LINE  = new Regex("^\\s*$"); 

  /** 
   * Regular expression representing a line  
   * starting a new section of n-grams in an ARPA language model file.  
   */ 
  public static final Regex NGRAM_HEADER = new Regex("^\\\\\\d-grams:\\s*$"); 

  /** 
   * Regular expression representing a line  
   * ending an ARPA language model file.  
   */ 
  public static final Regex NGRAM_END = new Regex("^\\\\end\\\\s*$"); 

  /** ARPA file for this object. */ 
  private final File arpaFile; 

  /** The vocabulary associated with this object. */ 
  private final Vocabulary vocab; 

  /**
   * Constructs an object that represents an ARPA language model file. 
   *  
   * @param arpaFileName File name of an ARPA language model file 
   * @param vocab Symbol table to be used by this object 
   */ 
  public ArpaFile(String arpaFileName, Vocabulary vocab) { 
    this.arpaFile = new File(arpaFileName); 
    this.vocab = vocab; 
  } 

  public ArpaFile(String arpaFileName) throws IOException { 
    this.arpaFile = new File(arpaFileName); 
    this.vocab = new Vocabulary(); 

    //  final Scanner scanner = new Scanner(arpaFile); 

    //  // Eat initial header lines 
    //  while (scanner.hasNextLine()) { 
    //   String line = scanner.nextLine(); 
    //   logger.finest("Discarding line: " + line); 
    //   if (NGRAM_HEADER.matches(line)) { 
    //    break; 
    //   } 
    //  } 

    //  int ngramOrder = 1; 

    LineReader grammarReader = new LineReader(arpaFileName); 

    try { 
      for (String line : grammarReader) { 


        //  while (scanner.hasNext()) { 
        //    
        //   String line = scanner.nextLine(); 

        String[] parts = Regex.spaces.split(line); 
        if (parts.length > 1) { 
          String[] words = Regex.spaces.split(parts[1]); 

          for (String word : words) { 
            LOG.debug("Adding to vocab: {}", word);
            Vocabulary.addAll(word);
          } 
        } else {
          LOG.info(line);
        } 

      } 
    } finally {  
      grammarReader.close();  
    } 

    //    
    //   boolean lineIsHeader = NGRAM_HEADER.matches(line); 
    //    
    //   while (lineIsHeader || BLANK_LINE.matches(line)) { 
    //     
    //    if (lineIsHeader) { 
    //     ngramOrder++; 
    //    } 
    //     
    //    if (scanner.hasNext()) { 
    //     line = scanner.nextLine().trim(); 
    //     lineIsHeader = NGRAM_HEADER.matches(line); 
    //    } else { 
    //     logger.severe("Ran out of lines!"); 
    //     return; 
    //    } 
    //   } 


    //    
    //   // Add word to vocab 
    //   if (logger.isLoggable(Level.FINE)) logger.fine("Adding word to vocab: " + parts[ngramOrder]); 
    //   vocab.addTerminal(parts[ngramOrder]); 
    //    
    //   // Add context words to vocab 
    //   for (int i=1; i<ngramOrder; i++) { 
    //    if (logger.isLoggable(Level.FINE)) logger.fine("Adding context word to vocab: " + parts[i]); 
    //    vocab.addTerminal(parts[i]); 
    //   } 

    //  } 

    LOG.info("Done constructing ArpaFile");

  } 

  /**
   * Gets the {@link org.apache.joshua.corpus.Vocabulary} 
   * associated with this object. 
   *  
   * @return the symbol table associated with this object 
   */ 
  public Vocabulary getVocab() { 
    return vocab; 
  } 

  /**
   * Gets the total number of n-grams  
   * in this ARPA language model file. 
   *  
   * @return total number of n-grams  
   *         in this ARPA language model file 
   */ 
  @SuppressWarnings("unused") 
  public int size() { 

    LOG.debug("Counting n-grams in ARPA file");
    int count=0; 

    for (ArpaNgram ngram : this) { 
      count++; 
    } 
    LOG.debug("Done counting n-grams in ARPA file");

    return count; 
  } 

  public int getOrder() throws FileNotFoundException { 

    Pattern pattern = Pattern.compile("^ngram (\\d+)=\\d+$"); 
    LOG.debug("Pattern is {}", pattern);
    @SuppressWarnings("resource")
    final Scanner scanner = new Scanner(arpaFile); 

    int order = 0; 

    // Eat initial header lines 
    while (scanner.hasNextLine()) { 
      String line = scanner.nextLine(); 

      if (NGRAM_HEADER.matches(line)) { 
        break; 
      } else { 
        Matcher matcher = pattern.matcher(line); 
        if (matcher.matches()) { 
          LOG.debug("DOES  match: '{}'", line);
          order = Integer.valueOf(matcher.group(1)); 
        } else {
          LOG.debug("Doesn't match: '{}'", line);
        } 
      } 
    } 

    return order; 
  } 

  /**
   * Gets an iterator capable of iterating  
   * over all n-grams in the ARPA file. 
   *  
   * @return an iterator capable of iterating  
   *         over all n-grams in the ARPA file 
   */ 
  @SuppressWarnings("resource")
  public Iterator<ArpaNgram> iterator() { 

    try { 
      final Scanner scanner; 

      if (arpaFile.getName().endsWith("gz")) { 
        InputStream in = new GZIPInputStream( 
            new FileInputStream(arpaFile)); 
        scanner = new Scanner(in); 
      } else { 
        scanner = new Scanner(arpaFile); 
      } 

      // Eat initial header lines 
      while (scanner.hasNextLine()) { 
        String line = scanner.nextLine(); 
        LOG.debug("Discarding line: {}", line);
        if (NGRAM_HEADER.matches(line)) { 
          break; 
        } 
      } 

      return new Iterator<ArpaNgram>() { 

        String nextLine = null; 
        int ngramOrder = 1; 
        //    int id = 0; 

        public boolean hasNext() { 

          if (scanner.hasNext()) { 

            String line = scanner.nextLine(); 

            boolean lineIsHeader = NGRAM_HEADER.matches(line) || NGRAM_END.matches(line); 

            while (lineIsHeader || BLANK_LINE.matches(line)) { 

              if (lineIsHeader) { 
                ngramOrder++; 
              } 

              if (scanner.hasNext()) { 
                line = scanner.nextLine().trim(); 
                lineIsHeader = NGRAM_HEADER.matches(line) || NGRAM_END.matches(line); 
              } else { 
                nextLine = null; 
                return false; 
              } 
            } 

            nextLine = line; 
            return true; 

          } else { 
            nextLine = null; 
            return false; 
          } 

        } 

        public ArpaNgram next() { 
          if (nextLine!=null) { 

            String[] parts = Regex.spaces.split(nextLine); 

            float value = Float.valueOf(parts[0]); 

            int word = Vocabulary.id(parts[ngramOrder]); 

            int[] context = new int[ngramOrder-1]; 
            for (int i=1; i<ngramOrder; i++) { 
              context[i-1] = Vocabulary.id(parts[i]); 
            } 

            float backoff; 
            if (parts.length > ngramOrder+1) { 
              backoff = Float.valueOf(parts[parts.length-1]); 
            } else { 
              backoff = ArpaNgram.DEFAULT_BACKOFF; 
            } 

            nextLine = null; 
            return new ArpaNgram(word, context, value, backoff); 

          } else { 
            throw new NoSuchElementException(); 
          } 
        } 

        public void remove() { 
          throw new UnsupportedOperationException(); 
        } 

      }; 
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      return null; 
    } 
  }
}