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
package org.apache.joshua.decoder.ff.lm.berkeley_lm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.nlp.lm.io.MakeLmBinaryFromArpa;
import org.apache.commons.io.IOUtils;
import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Replacement for test/lm/berkeley/test.sh regression test
 */

public class LMGrammarBerkeleyTest {

  private static final String INPUT = "the chat-rooms";
  private static final String EXPECTED_OUTPUT = "tm_glue_0=2.000 lm_0=-7.153\n";
  private static final String EXPECTED_OUTPUT_WITH_OOV = "tm_glue_0=2.000 lm_0=-7.153 lm_0_oov=0.000\n";
  private static final String[] OPTIONS = "-v 0 -output-format %f".split(" ");

  private static final String lmFile = "src/test/resources/berkeley_lm/lm";
  private static final String compressedLmFile = "target/lm.gz";
  private static final String lmFileBin = "target/lm.berkeleylm";
  private static final String compressedLmFileBin = "target/lm.berkeleylm.gz";

  private JoshuaConfiguration joshuaConfig;
  private Decoder decoder;

  @BeforeClass
  public static void before() throws Exception {
    // generate lm.gz
    FileInputStream lmFileStream = new FileInputStream(new File(lmFile));
    compress(lmFileStream, compressedLmFile);

    // generate lm.berkeleylm
    MakeLmBinaryFromArpa.main(new String[] { lmFile, lmFileBin });

    // generate lm.berkeleylm.gz
    FileInputStream lmFileBinStream = new FileInputStream(new File(lmFileBin));
    compress(lmFileBinStream, compressedLmFileBin);
  }

  private static void compress(FileInputStream lmFileStream, String target) throws IOException {
    try {
      Files.createFile(Paths.get(target));
      GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(target));
      IOUtils.copy(lmFileStream, gzipOutputStream);
      gzipOutputStream.finish();
    } catch (FileAlreadyExistsException fae) {
      // the file already exists, no need to recreate it
    }
  }

  @DataProvider(name = "languageModelFiles")
  public Object[][] lmFiles() {
    return new Object[][] { { lmFile }, { compressedLmFile }, { lmFileBin },
        { compressedLmFileBin } };
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
  }

  @Test(dataProvider = "languageModelFiles")
  public void verifyLM(String lmFile) {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.processCommandLineOptions(OPTIONS);
    joshuaConfig.features.add("LanguageModel -lm_type berkeleylm -lm_order 2 -lm_file " + lmFile);
    decoder = new Decoder(joshuaConfig, null);
    final String translation = decode(INPUT).toString();
    assertEquals(translation, EXPECTED_OUTPUT);
  }

  private Translation decode(String input) {
    final Sentence sentence = new Sentence(input, 0, joshuaConfig);
    return decoder.decode(sentence);
  }

  @Test
  public void givenLmWithOovFeature_whenDecoder_thenCorrectFeaturesReturned() {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.processCommandLineOptions(OPTIONS);
    joshuaConfig.features.add(
        "LanguageModel -lm_type berkeleylm -oov_feature -lm_order 2 -lm_file src/test/resources/berkeley_lm/lm");
    decoder = new Decoder(joshuaConfig, null);
    final String translation = decode(INPUT).toString();
    assertEquals(Decoder.weights.getDenseFeatures().size(), 3);
    assertEquals(translation, EXPECTED_OUTPUT_WITH_OOV);
  }

}
