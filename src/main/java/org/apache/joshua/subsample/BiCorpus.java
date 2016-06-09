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
package org.apache.joshua.subsample;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.joshua.corpus.Phrase;

/**
 * Class for representing a sentence-aligned bi-corpus (with optional word-alignments).
 * <p>
 * In order to avoid memory crashes we no longer extend an ArrayList, which tries to cache the
 * entire file in memory at once. This means we'll re-read through each file (1 +
 * {@link Subsampler#MAX_SENTENCE_LENGTH} / binsize) times where binsize is determined by the
 * <code>subsample(String, float, PhraseWriter, BiCorpusFactory)</code> method.
 * 
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton wren@users.sourceforge.net
 * @version $LastChangedDate$
 */
public class BiCorpus implements Iterable<PhrasePair> {
  protected final String foreignFileName;
  protected final String nativeFileName;
  protected final String alignmentFileName;

  /**
   * Constructor for unaligned BiCorpus.
   * @param foreignFileName todo
   * @param nativeFileName todo
   * @throws IOException todo
   */
  public BiCorpus(String foreignFileName, String nativeFileName) throws IOException {
    this(foreignFileName, nativeFileName, null);
  }

  /**
   * Constructor for word-aligned BiCorpus.
   * @param foreignFileName todo
   * @param nativeFileName todo
   * @param alignmentFileName todo
   * @throws IOException todo
   * @throws IllegalArgumentException todo
   * @throws IndexOutOfBoundsException todo
   */
  public BiCorpus(String foreignFileName, String nativeFileName, String alignmentFileName)
      throws IOException, IllegalArgumentException, IndexOutOfBoundsException {
    this.foreignFileName = foreignFileName;
    this.nativeFileName = nativeFileName;
    this.alignmentFileName = alignmentFileName;

    // Check for fileLengthMismatchException
    // Of course, that will be checked for in each iteration
    //
    // We write it this way to avoid warnings from the foreach style loop
    Iterator<PhrasePair> it = iterator();
    while (it.hasNext()) {
      it.next();
    }
  }


  // ===============================================================
  // Methods
  // ===============================================================
  // BUG: We don't close file handles. The other reader classes apparently have finalizers to handle
  // this well enough for our purposes, but we should migrate to using joshua.util.io.LineReader and
  // be sure to close it in the end.

  // We're not allowed to throw exceptions from Iterator/Iterable
  // so we have evil boilerplate to crash the system
  /**
   * Iterate through the files represented by this <code>BiCorpus</code>, returning a
   * {@link PhrasePair} for each pair (or triple) of lines.
   */
  @SuppressWarnings("resource")
  public Iterator<PhrasePair> iterator() {
    PhraseReader closureRF = null;
    PhraseReader closureRE = null;
    BufferedReader closureRA = null;
    try {
      closureRF = new PhraseReader(new FileReader(this.foreignFileName), (byte) 1);
      closureRE = new PhraseReader(new FileReader(this.nativeFileName), (byte) 0);
      closureRA =
          (null == this.alignmentFileName ? null : new BufferedReader(new FileReader(
              this.alignmentFileName)));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("File not found", e);
    }
    // Making final for closure capturing in the local class definition
    final PhraseReader rf = closureRF;
    final PhraseReader re = closureRE;
    final BufferedReader ra = closureRA;

    return new Iterator<PhrasePair>() { /* Local class definition */
      private Phrase nextForeignPhrase = null;

      public void remove() {
        throw new UnsupportedOperationException();
      }

      public boolean hasNext() {
        if (null == this.nextForeignPhrase) {
          try {
            this.nextForeignPhrase = rf.readPhrase();
          } catch (IOException e) {
            throw new RuntimeException("IOException", e);
          }
        }
        return null != this.nextForeignPhrase;
      }

      public PhrasePair next() {
        if (this.hasNext()) {
          Phrase f = this.nextForeignPhrase;

          Phrase e = null;
          try {
            e = re.readPhrase();
          } catch (IOException ioe) {
            throw new RuntimeException("IOException", ioe);
          }
          if (null == e) {
            fileLengthMismatchException();
            return null; // Needed to make javac happy
          } else {
            if (e.size() != 0 && f.size() != 0) {
              if (null != ra) {
                String line = null;
                try {
                  line = ra.readLine();
                } catch (IOException ioe) {
                  throw new RuntimeException("IOException", ioe);
                }

                if (null == line) {
                  fileLengthMismatchException();
                  return null; // Needed to make javac happy
                } else {
                  Alignment a = new Alignment((short) f.size(), (short) e.size(), line);

                  this.nextForeignPhrase = null;
                  return new PhrasePair(f, e, a);
                }
              } else {
                this.nextForeignPhrase = null;
                return new PhrasePair(f, e);
              }
            } else {
              // Inverted while loop
              this.nextForeignPhrase = null;
              return this.next();
            }
          }
        } else {
          throw new NoSuchElementException();
        }
      }
    }; /* End local class definition */
  } /* end iterator() */


  private static void fileLengthMismatchException() throws RuntimeException {
    throw new RuntimeException("Mismatched file lengths!");
  }
}
