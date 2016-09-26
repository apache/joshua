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
package org.apache.joshua.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * utility functions for file operations
 *
 * @author Zhifei Li, zhifei.work@gmail.com
 * @author wren ng thornton wren@users.sourceforge.net
 * @since 28 February 2009
 */
public class FileUtility {

  /**
   * Warning, will truncate/overwrite existing files
   * @param filename a file for which to obtain a writer
   * @return the buffered writer object
   * @throws IOException if there is a problem reading the inout file
   */
  public static BufferedWriter getWriteFileStream(String filename) throws IOException {
    return new BufferedWriter(new OutputStreamWriter(
    // TODO: add GZIP
        filename.equals("-") ? new FileOutputStream(FileDescriptor.out) : new FileOutputStream(
            filename, false), StandardCharsets.UTF_8));
  }

  /**
   * Returns the base directory of the file. For example, dirname('/usr/local/bin/emacs') -&gt;
   * '/usr/local/bin'
   * @param fileName the input path
   * @return the parent path
   */
  static public String dirname(String fileName) {
    if (fileName.contains(File.separator))
      return fileName.substring(0, fileName.lastIndexOf(File.separator));

    return ".";
  }
}
