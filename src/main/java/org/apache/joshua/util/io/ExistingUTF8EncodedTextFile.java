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
package org.apache.joshua.util.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A class that represents a {@link StandardCharsets#UTF_8} text file. Will
 * throw a {@link FileNotFoundException} upon instantiation if the underlying
 * {@link Path}, or {@link String} representing a Path, is not found.
 */
public class ExistingUTF8EncodedTextFile {
  private static final Predicate<String> emptyStringPredicate = String::isEmpty;

  private final Path p;

  public ExistingUTF8EncodedTextFile(String pathStr) throws FileNotFoundException {
    this(Paths.get(pathStr));
  }

  public ExistingUTF8EncodedTextFile(Path p) throws FileNotFoundException {
    this.p = p;
    if (!Files.exists(p))
      throw new FileNotFoundException("Did not find the file at path: " + p.toString());
  }

  /**
   * @return the {@link Path} representing this object
   */
  public Path getPath() {
    return this.p;
  }

  /**
   * @return the number of lines in the file represented by this object
   * @throws IOException on inability to read file (maybe it's not a text file)
   */
  public int getNumberOfLines() throws IOException {
    try(Stream<String> ls = Files.lines(this.p, StandardCharsets.UTF_8)) {
      return (int) ls.count();
    }
  }

  /**
   * @return the number of non-empty lines in the file represented by this object
   * @throws IOException on inability to read file (maybe it's not a text file)
   */
  public int getNumberOfNonEmptyLines() throws IOException {
    try(Stream<String> ls = Files.lines(this.p, StandardCharsets.UTF_8)) {
      return (int) ls.filter(emptyStringPredicate.negate())
          .count();
    }
  }
}
