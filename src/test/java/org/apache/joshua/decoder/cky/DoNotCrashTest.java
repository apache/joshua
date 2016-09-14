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

import java.io.IOException;
import java.util.List;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DoNotCrashTest {

	private JoshuaConfiguration joshuaConfig = null;
	private Decoder decoder = null;

	@BeforeMethod
	public void setUp() throws Exception {
		joshuaConfig = new JoshuaConfiguration();
		decoder = new Decoder(joshuaConfig, "");
	}

	@AfterMethod
	public void tearDown() throws Exception {
		decoder.cleanUp();
		decoder = null;
	}

	@Test
	public void givenProblematicInput_whenDecoding_thenNoCrash() throws IOException {
		// Given
		List<String> inputStrings = loadStringsFromFile("src/test/resources/decoder/dont-crash/input");
		
		// When
		decodeList(inputStrings, decoder, joshuaConfig);
		
		// Then
		// Did not crash
	}

}
