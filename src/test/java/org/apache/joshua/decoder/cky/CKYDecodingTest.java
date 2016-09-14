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
package org.apache.joshua.decoder.cky;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class CKYDecodingTest {

	private final static String N_BEST_SEPARATOR = "\n";

	private JoshuaConfiguration joshuaConfig;
	private Decoder decoder;

	@AfterMethod
	public void tearDown() throws Exception {
		if(decoder != null) {
			decoder.cleanUp();
			decoder = null;
		}
	}

	@Test
	public void givenBnEnInput_whenPhraseDecoding_thenScoreAndTranslationCorrect() throws Exception {
		// Given
		List<String> inputStrings = loadSentencesFromFile("src/test/resources/bn-en/hiero/input.bn");

		// When
		configureDecoder("src/test/resources/bn-en/hiero/joshua.config");
		List<String> decodedStrings = decodeList(inputStrings);

		// Then
		List<String> goldStrings = loadSentencesFromFile("src/test/resources/bn-en/hiero/output.gold");
		assertEquals(decodedStrings, goldStrings);
	}

	@Test
	public void givenBnEnInput_whenPhraseDecodingWithBerkeleyLM_thenScoreAndTranslationCorrect() throws Exception {
		// Given
		List<String> inputStrings = loadSentencesFromFile("src/test/resources/bn-en/hiero/input.bn");

		// When
		configureDecoder("src/test/resources/bn-en/hiero/joshua-berkeleylm.config");
		List<String> decodedStrings = decodeList(inputStrings);

		// Then
		List<String> goldStrings = loadSentencesFromFile("src/test/resources/bn-en/hiero/output-berkeleylm.gold");
		assertEquals(decodedStrings, goldStrings);
	}

	@Test
	public void givenBnEnInput_whenPhraseDecodingWithClassLM_thenScoreAndTranslationCorrect() throws Exception {
		// Given
		List<String> inputStrings = loadSentencesFromFile("src/test/resources/bn-en/hiero/input.bn");

		// When
		configureDecoder("src/test/resources/bn-en/hiero/joshua-classlm.config");
		List<String> decodedStrings = decodeList(inputStrings);

		// Then
		List<String> goldStrings = loadSentencesFromFile("src/test/resources/bn-en/hiero/output-classlm.gold");
		assertEquals(decodedStrings, goldStrings);
	}
	
	@Test
	public void givenBnEnInput_whenPhraseDecodingWithPackedGrammar_thenScoreAndTranslationCorrect() throws Exception {
		// Given
		List<String> inputStrings = loadSentencesFromFile("src/test/resources/bn-en/packed/input.bn");

		// When
		configureDecoder("src/test/resources/bn-en/packed/joshua.config");
		List<String> decodedStrings = decodeList(inputStrings);

		// Then
		List<String> goldStrings = loadSentencesFromFile("src/test/resources/bn-en/packed/output.gold");
		assertEquals(decodedStrings, goldStrings);
	}
	
	@Test
	public void givenBnEnInput_whenPhraseDecodingWithSAMT_thenScoreAndTranslationCorrect() throws Exception {
		// Given
		List<String> inputStrings = loadSentencesFromFile("src/test/resources/bn-en/samt/input.bn");

		// When
		configureDecoder("src/test/resources/bn-en/samt/joshua.config");
		List<String> decodedStrings = decodeList(inputStrings);

		// Then
		List<String> goldStrings = loadSentencesFromFile("src/test/resources/bn-en/samt/output.gold");
		assertEquals(decodedStrings, goldStrings);
	}

	private static List<String> loadSentencesFromFile(String pathToFile) throws IOException {
		List<String> inputLines = Files.lines(Paths.get(pathToFile)).collect(Collectors.toList());
		return inputLines;
	}

	private void configureDecoder(String pathToConfig) throws Exception {
		joshuaConfig = new JoshuaConfiguration();
		joshuaConfig.readConfigFile(pathToConfig);
		KenLmTestUtil.Guard(() -> decoder = new Decoder(joshuaConfig, ""));
	}
	
	/**
	 * @param inputStrings A list of strings that should be decoded
	 * @return A list of decoded strings. If the decoder produces
	 * an n-best list, then each translation of the n-best list
	 * has its own entry in the returned list.
	 */
	private List<String> decodeList(List<String> inputStrings) {
		final List<String> decodedStrings = new ArrayList<>();

		for (String inputString : inputStrings) {
			final Sentence sentence = new Sentence(inputString, 0, joshuaConfig);
			final String[] nBestList = decoder.decode(sentence).toString().split(N_BEST_SEPARATOR);
			decodedStrings.addAll(Arrays.asList(nBestList));
		}

		return decodedStrings;
	}
}
