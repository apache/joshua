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

import java.io.IOException;
import java.util.Iterator;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.util.io.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a base class for simple, ASCII line-based grammars that are stored on disk.
 * 
 * @author Juri Ganitkevitch
 * 
 */
public abstract class GrammarReader<R extends Rule> implements Iterable<R>, Iterator<R> {

  private static final Logger LOG = LoggerFactory.getLogger(GrammarReader.class);

  protected static String fieldDelimiter;
  protected static String description;

  protected String fileName;
  protected LineReader reader;
  protected String lookAhead;
  protected int numRulesRead;


  // dummy constructor for
  public GrammarReader() {
    this.fileName = null;
  }

  public GrammarReader(String fileName) throws IOException {
    this.fileName = fileName;
    this.reader = new LineReader(fileName);
    LOG.info("Reading grammar from file {}...", fileName);
    numRulesRead = 0;
    advanceReader();
  }

  // the reader is the iterator itself
  public Iterator<R> iterator() {
    return this;
  }

  /** Unsupported Iterator method. */
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void close() {
    if (null != this.reader) {
      try {
        this.reader.close();
      } catch (IOException e) {
        LOG.warn(e.getMessage(), e);
        LOG.error("Error closing grammar file stream: {}",  this.fileName);
      }
      this.reader = null;
    }
  }

  /**
   * For correct behavior <code>close</code> must be called on every GrammarReader, however this
   * code attempts to avoid resource leaks.
   * 
   * @see org.apache.joshua.util.io.LineReader
   */
  @Override
  protected void finalize() throws Throwable {
    if (this.reader != null) {
      LOG.error("Grammar file stream was not closed, this indicates a coding error: {}",
          this.fileName);
    }

    this.close();
    super.finalize();
  }

  @Override
  public boolean hasNext() {
    return lookAhead != null;
  }

  private void advanceReader() {
    try {
      lookAhead = reader.readLine();
      numRulesRead++;
    } catch (IOException e) {
      LOG.error("Error reading grammar from file: {}", fileName);
      LOG.error(e.getMessage(), e);
    }
    if (lookAhead == null && reader != null) {
      this.close();
    }
  }

  /**
   * Read the next line, and print reader progress.
   */
  @Override
  public R next() {
    String line = lookAhead;

    int oldProgress = reader.progress();
    advanceReader();


    if (Decoder.VERBOSE >= 1) {
      int newProgress = (reader != null) ? reader.progress() : 100;

      //TODO: review this code. It is better to print progress based on time gap (like for every 1s or 2sec) than %!
      if (newProgress > oldProgress) {
        for (int i = oldProgress + 1; i <= newProgress; i++)
          if (i == 97) {
            System.err.print("1");
          } else if (i == 98) {
            System.err.print("0");
          } else if (i == 99) {
            System.err.print("0");
          } else if (i == 100) {
            System.err.println("%");
          } else if (i % 10 == 0) {
            System.err.print(String.format("%d", i));
            System.err.flush();
          } else if ((i - 1) % 10 == 0)
            ; // skip at 11 since 10, 20, etc take two digits
          else {
            System.err.print(".");
            System.err.flush();
          }
      }
    }
    return parseLine(line);
  }

  protected abstract R parseLine(String line);
}
