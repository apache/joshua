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
package org.apache.joshua.decoder;

import org.apache.joshua.decoder.ff.lm.KenLM;

import java.nio.ByteBuffer;

import static org.apache.joshua.util.Constants.LONG_SIZE_IN_BYTES;

/**
 * Class to wrap a KenLM pool of states.  This class is not ThreadSafe.  It should be
 * used in a scoped context, and close must be called to release native resources.  It
 * does implement a custom finalizer that will release these resources if needed, but
 * this should not be relied on.
 *
 * @author Kellen Sunderland
 */

public abstract class LmPool implements AutoCloseable {

  private final long pool;
  private final KenLM languageModel;
  private final ByteBuffer ngramBuffer;
  private boolean released = false;

  public LmPool(long pool, KenLM languageModel, ByteBuffer ngramBuffer) {
    this.pool = pool;
    this.languageModel = languageModel;
    this.ngramBuffer = ngramBuffer;
  }

  public long getPool() {
    return pool;
  }

  @Override
  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }

  @Override
  public synchronized void close() {
    if (!released) {
      released = true;
      languageModel.destroyLMPool(pool);
    }
  }

  /**
   * Write a single id to the KenLM shared buffer.
   * Note: This method must be used in conjunction with setBufferLength.
   *
   * @param index index at which to write id.
   * @param id    word id to write.
   */
  public void writeIdToBuffer(int index, long id) {
    this.ngramBuffer.putLong((index + 1) * LONG_SIZE_IN_BYTES, id);
  }

  /**
   * Manually set the length of the ngram array to be used when calling probRule or estimate on
   * KenLM.
   * Note: Must be used if you are calling writeIdToBuffer.
   *
   * @param length The size of the array of ngrams you would like to use with probRule or estimate.
   */
  public void setBufferLength(long length) {
    ngramBuffer.putLong(0, length);
  }
}
