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

import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.apache.joshua.decoder.segment_file.Sentence;
import org.testng.annotations.BeforeTest;
import org.testng.Assert;
import org.testng.annotations.AfterTest;

/**
 * Tests should
 * <ol>
 * <li>Should write translation (using -moses argument) to stdout, output-format info to n-best.txt</li>
 * <li>Should write output-format info to n-best.txt (since no -moses)</li>
 * </ol>
 * We then undertake a simple diff on the outputs to see if Moses compatibility is achieved.
 */
public class TestTranslation {

  private static final String[] MOSES_INPUT = {"-v", "0", "-moses", "-n-best-list", "n-best1.txt", "10", "distinct", ">", "output"};
  private static final String[] STANDARD_INPUT = {"-v", "0", "-n-best-list", "n-best2.txt", "10", "distinct", ">>", "output"};
  private JoshuaConfiguration mosesConfig;
  private JoshuaConfiguration standardConfig;
  private Path tmpFile;

  @BeforeTest
  public void beforeTest() {
    mosesConfig = new JoshuaConfiguration();
    standardConfig = new JoshuaConfiguration();
  }

  @AfterTest
  public void afterTest() {
  }

  /**
   * Should write translation to stdout, output-format info to n-best.txt
   */
  @Test
  public void testMosesTranslationCompatibility() {

    //First execute the MOSES_INPUT
    mosesConfig.processCommandLineOptions(MOSES_INPUT);
    mosesConfig.use_structured_output = true;
    Decoder mosesDecoder = new Decoder(mosesConfig, null);
    Translation mosesTranslations = mosesDecoder.decode(new Sentence("help", 1, mosesConfig));
    getStructuredTranslations(tmpFile, mosesTranslations);

    //Second execute the STANDARD_INPUT
    standardConfig.processCommandLineOptions(STANDARD_INPUT);
    standardConfig.use_structured_output = true;
    Decoder standardDecoder = new Decoder(standardConfig, null);
    Translation standardTranslations = standardDecoder.decode(new Sentence("help", 2, standardConfig));
    getStructuredTranslations(tmpFile, standardTranslations);

    File expectedFile = new File(TestTranslation.class.getClassLoader().getResource("decoder/moses-compat/output.expected").getFile());

    try {
      compareFileContents(tmpFile, expectedFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void compareFileContents(Path newFile, File expectedFile) throws IOException {

    BufferedReader reader1 = new BufferedReader(new FileReader(new File(newFile.toFile().getPath())));
    BufferedReader reader2 = new BufferedReader(new FileReader(new File(expectedFile.getPath())));

    String line1 = null;
    String line2 = null;
    while (((line1 = reader1.readLine()) != null)
        && ((line2 = reader2.readLine()) != null)) {
      if (line1.equals(line2)) {
        Assert.assertTrue(line1.equals(line2), "Contents (each line) of input files should be identical.");
      } else {
        Assert.fail("Contents of input files is not identical.");
      }
    }
    reader1.close();
    reader2.close();
  }

  private void getStructuredTranslations(Path tmpFile, Translation translations) {
    for (StructuredTranslation sTranslation : translations.getStructuredTranslations()) {
      try {
        tmpFile = writeStructuredTranslationString(sTranslation);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private Path writeStructuredTranslationString(StructuredTranslation sTranslation) throws IOException{
    if (tmpFile==null) {
      try {
        tmpFile = java.nio.file.Files.createTempFile("output", null);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    byte[] bTranslation = (Integer.toString(sTranslation.getTranslationWordAlignments().get(0).get(0)) + 
        " ||| " + sTranslation.getTranslationString() + 
        " ||| " + sTranslation.getTranslationFeatures().entrySet().iterator().next().toString() + 
        " ||| " + Float.toString(sTranslation.getTranslationScore()) + "\n").getBytes(Charset.forName("UTF-8"));

    FileOutputStream fos = new FileOutputStream(tmpFile.toFile(), true);
    fos.write(bTranslation);
    fos.close();
    return tmpFile;
  }

}
