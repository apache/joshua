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

/**
 * A set of word alignments between an F phrase and an E phrase. The implementation uses a
 * two-dimensional bit vector, though for our purposes we could just keep the original string around
 * (which would save lots of time parsing and reconstructing the string).
 * 
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton wren@users.sourceforge.net
 * @version $LastChangedDate$
 */
public class Alignment {
  private short eLength;
  private short fLength;
  private M2 aligned;

  public Alignment(short fLength, short eLength, String alignments) {
    this.eLength = eLength;
    this.fLength = fLength;
    this.aligned = new M2(fLength, eLength);

    if (alignments == null || alignments.length() == 0) {
      return;
    }
    String[] als = alignments.split("\\s+"); // TODO: joshua.util.Regex
    for (String al : als) {
      String[] pair = al.split("-");
      if (pair.length != 2)
        throw new IllegalArgumentException("Malformed alignment string: " + alignments);
      short f = Short.parseShort(pair[0]);
      short e = Short.parseShort(pair[1]);
      if (f >= fLength || e >= eLength)
        throw new IndexOutOfBoundsException("out of bounds: " + f + "," + e);
      aligned.set(f, e);
    }
  }


  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (short i = 0; i < fLength; i++)
      for (short j = 0; j < eLength; j++)
        if (aligned.get(i, j)) sb.append(i).append('-').append(j).append(' ');

    // Remove trailing space
    if (sb.length() > 0) sb.delete(sb.length() - 1, sb.length());

    return sb.toString();
  }


  /** A (short,short)->boolean map for storing alignments. */
  private final static class M2 {
    private short width;
    private boolean[] bits;

    public M2(short f, short e) {
      width = f;
      bits = new boolean[f * e];
    }

    public boolean get(short f, short e) {
      return bits[width * e + f];
    }

    public void set(short f, short e) {
      try {
        bits[width * e + f] = true;
      } catch (ArrayIndexOutOfBoundsException ee) {
        throw new RuntimeException("Set(" + f + ", " + e + "): caught " + ee);
      }
    }
  }
}
