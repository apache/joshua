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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Random;

/**
 * This program runs a little benchmark to check reading speed on various data
 * representations.
 * 
 * Usage: java Benchmark PACKED_GRAMMAR_DIR TIMES
 */

public class Benchmark {

  
  private static final Logger LOG = LoggerFactory.getLogger(Benchmark.class);

  private IntBuffer intBuffer;
  private MappedByteBuffer byteBuffer;
  private int[] intArray;

  public Benchmark(String dir) throws IOException {
    File file = new File(dir + "/slice_00000.source");

    FileChannel source_channel = new FileInputStream(file).getChannel();
    int byte_size = (int) source_channel.size();
    int int_size = byte_size / 4;

    byteBuffer = source_channel.map(MapMode.READ_ONLY, 0, byte_size); 
    intBuffer = byteBuffer.asIntBuffer();

    intArray = new int[int_size];
    intBuffer.get(intArray);
  }

  public void benchmark(int times) {
    LOG.info("Beginning benchmark.");

    Random r = new Random();
    r.setSeed(1234567890);
    int[] positions = new int[1000];
    for (int i = 0; i < positions.length; i++)
      positions[i] = r.nextInt(intArray.length);

    long sum;

    long start_time = System.currentTimeMillis();

    sum = 0;
    for (int t = 0; t < times; t++)
      for (int i = 0; i < positions.length; i++)
        sum += byteBuffer.getInt(positions[i] * 4);
    LOG.info("Sum: {}", sum);
    long byte_time = System.currentTimeMillis();

    sum = 0;
    for (int t = 0; t < times; t++)
      for (int i = 0; i < positions.length; i++)
        sum += intBuffer.get(positions[i]);
    LOG.info("Sum: {}", sum);
    long int_time = System.currentTimeMillis();

    sum = 0;
    for (int t = 0; t < times; t++)
      for (int i = 0; i < positions.length; i++)
        sum += intArray[positions[i]];
    LOG.info("Sum: {}", sum);
    long array_time = System.currentTimeMillis();

    sum = 0;
    for (int t = 0; t < times; t++)
      for (int i = 0; i < (intArray.length / 8); i++)
        sum += intArray[i * 6] + intArray[i * 6 + 2];
    LOG.info("Sum: {}", sum);
    long mult_time = System.currentTimeMillis();

    sum = 0;
    for (int t = 0; t < times; t++) {
      int index = 0;
      for (int i = 0; i < (intArray.length / 8); i++) {
        sum += intArray[index] + intArray[index + 2];
        index += 6;
      }
    }
    LOG.info("Sum: {}", sum);
    long add_time = System.currentTimeMillis();

    LOG.info("ByteBuffer: {}", (byte_time - start_time));
    LOG.info("IntBuffer:  {}", (int_time - byte_time));
    LOG.info("Array:      {}", (array_time - int_time));
    LOG.info("Multiply:   {}", (mult_time - array_time));
    LOG.info("Add:        {}", (add_time - mult_time));
  }

  public static void main(String args[]) throws IOException {
    Benchmark pr = new Benchmark(args[0]);
    pr.benchmark( Integer.parseInt(args[1]));
  }
}
