package org.apache.joshua.decoder;

import org.apache.joshua.decoder.ff.lm.KenLM;

import java.nio.ByteBuffer;

/**
 * Class to wrap a KenLM pool of states.  This class is not ThreadSafe.  It should be
 * used in a scoped context, and close must be called to release native resources.  It
 * does implement a custom finalizer that will release these resources if needed, but
 * this should not be relied on.
 *
 * @author Kellen Sunderland
 */

public class KenLMPool implements AutoCloseable {

  private final long pool;
  private final KenLM languageModel;
  private final ByteBuffer ngramBuffer;
  private boolean released = false;

  public KenLMPool(long pool, KenLM languageModel, ByteBuffer ngramBuffer) {
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
  public void close() {
    if (!released) {
      released = true;
      languageModel.destroyLMPool(pool);
    }
  }

  public ByteBuffer getNgramBuffer() {
    return ngramBuffer;
  }
}
