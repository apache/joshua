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
package org.apache.joshua.packed;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.apache.joshua.corpus.Vocabulary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This program reads a packed representation and prints out some
 * basic information about it.
 *
 * Usage: java CountRules PACKED_GRAMMAR_DIR
 */

public class CountRules {

  public static void main(String args[]) {

    String dir = args[0];

    File file = new File(dir + "/chunk_00000.source");
    FileInputStream stream = null;
    FileChannel channel = null;
    try {
      // read the vocabulary
      Vocabulary.read(new File(dir + "/vocabulary"));

      // get the channel etc
      stream = new FileInputStream(file);
      channel = stream.getChannel();
      int size = (int) channel.size();

      MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, size);
      // byte[] bytes = new bytes[size];
      // buffer.get(bytes);

      // read the number of rules
      int numRules = buffer.getInt();
      System.out.println(String.format("There are %d source sides at the root", numRules));

      // read the first symbol and its offset
      for (int i = 0; i < numRules; i++) {
        // String symbol = Vocabulary.word(buffer.getInt());
        int symbol = buffer.getInt();
        String string = Vocabulary.word(symbol);
        int offset = buffer.getInt();
        System.out.println(String.format("-> %s/%d [%d]", string, symbol, offset));
      }

    } catch (IOException e) {

      e.printStackTrace();

    } finally {
      try {
        if (stream != null)
          stream.close();

        if (channel != null)
          channel.close();

      } catch (IOException e) {

        e.printStackTrace();

      }
    }


    // // Read in the bytes
    // int offset = 0;
    // int numRead = 0;
    // while (offset < bytes.length
    // 	   && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
    // 	offset += numRead;
    // }

    // // Ensure all the bytes have been read in
    // if (offset < bytes.length) {
    // 	throw new IOException("Could not completely read file "+file.getName());
    // }

    // // Close the input stream and return bytes
    // is.close();
    // return bytes;
  }
}
