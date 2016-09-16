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

import static org.apache.joshua.decoder.cky.TestUtil.decodeList;
import static org.apache.joshua.decoder.cky.TestUtil.loadStringsFromFile;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class LeftStateTest {

	private Decoder decoder;

	@AfterMethod
	public void tearDown() throws Exception {
		if(decoder != null) {
			decoder.cleanUp();
			decoder = null;
		}
	}

	@Test
	public void givenInput_whenLeftStateDecoding_thenScoreAndTranslationCorrect() throws Exception {
		// Given
		List<String> inputStrings = loadStringsFromFile("src/test/resources/decoder/left-state/input.bn");

		// When
		configureDecoder(new File("src/test/resources/decoder/left-state/joshua.config"));
		List<String> decodedStrings = decodeList(inputStrings, decoder);

		// Then
		List<String> goldStrings = loadStringsFromFile("src/test/resources/decoder/left-state/output.gold");

		assertEquals(decodedStrings, goldStrings);
	}
	
	public void configureDecoder(File pathToConfig) throws Exception {
		KenLmTestUtil.Guard(() -> decoder = new Decoder(Decoder.getFlagsFromFile(pathToConfig)));
	}
}
