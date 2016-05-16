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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for doing bit twiddling.
 *
 * @author Lane Schwartz
 */
public class BitsTest {

	@Test
	public void positiveLowBitsLongEncoding() {
		
		int[] highs = {Integer.MIN_VALUE, -1234567890, -1, 0, 1, 1234567890, Integer.MAX_VALUE};
		
		for (int high : highs) {
			for (int low=0, step=(Integer.MAX_VALUE/754); low>=0 && low<=Integer.MAX_VALUE; low+=step) {
				
				Assert.assertTrue(step > 0);
				Assert.assertTrue(low >= 0);

				long encoded = Bits.encodeAsLong(high, low);

				Assert.assertEquals(Bits.decodeHighBits(encoded), high);
				Assert.assertEquals(Bits.decodeLowBits(encoded), low);
			}
		}
		
	}
	
	@Test
	public void negativeLowBitsLongEncoding() {

		int[] highs = {Integer.MIN_VALUE, -1234567890, -1, 0, 1, 1234567890, Integer.MAX_VALUE};

		for (int high : highs) {
			for (int low=Integer.MIN_VALUE, step=(Integer.MAX_VALUE/754); low<=0 && low>=Integer.MIN_VALUE; low-=step) {

				Assert.assertTrue(step > 0);
				Assert.assertTrue(low <= 0);

				long encoded = Bits.encodeAsLong(high, low);

				Assert.assertEquals(Bits.decodeHighBits(encoded), high);
				Assert.assertEquals(Bits.decodeLowBits(encoded), low);
			}
		}
	}
	
	
	@Test
	public void positiveHighBitsLongEncoding() {
		
		int[] lows = {Integer.MIN_VALUE, -1234567890, -1, 0, 1, 1234567890, Integer.MAX_VALUE};
		
		for (int low : lows) {
			for (int high=0, step=(Integer.MAX_VALUE/754); high>=0 && high<=Integer.MAX_VALUE; high+=step) {
				
				Assert.assertTrue(step > 0);
				Assert.assertTrue(high >= 0);

				long encoded = Bits.encodeAsLong(high, low);

				Assert.assertEquals(Bits.decodeHighBits(encoded), high);
				Assert.assertEquals(Bits.decodeLowBits(encoded), low);
			}
		}
	}
	
	@Test
	public void negativeHighBitsLongEncoding() {

		int[] lows = {Integer.MIN_VALUE, -1234567890, -1, 0, 1, 1234567890, Integer.MAX_VALUE};

		for (int low : lows) {
			for (int high=Integer.MIN_VALUE, step=(Integer.MAX_VALUE/754); high<=0 && high>=Integer.MIN_VALUE; high-=step) {

				Assert.assertTrue(step > 0);
				Assert.assertTrue(high <= 0);

				long encoded = Bits.encodeAsLong(high, low);

				Assert.assertEquals(Bits.decodeHighBits(encoded), high);
				Assert.assertEquals(Bits.decodeLowBits(encoded), low);
			}
		}
	}
	
	
	@Test
	public void positiveLowBitsIntEncoding() {
		
		short[] highs = {Short.MIN_VALUE, -12345, -1, 0, 1, 12345, Short.MAX_VALUE};
		
		for (short high : highs) {
			for (short low=0, step=(Short.MAX_VALUE/75); low>=0 && low<=Short.MAX_VALUE; low+=step) {
				
				Assert.assertTrue(step > 0);
				Assert.assertTrue(low >= 0);

				int encoded = Bits.encodeAsInt(high, low);

				Assert.assertEquals(Bits.decodeHighBits(encoded), high);
				Assert.assertEquals(Bits.decodeLowBits(encoded), low);
			}
		}
		
	}
	
	@Test
	public void negativeLowBitsIntEncoding() {

		short[] highs = {Short.MIN_VALUE, -12345, -1, 0, 1, 12345, Short.MAX_VALUE};

		for (short high : highs) {
			for (short low=0, step=(Short.MAX_VALUE/75); low>=0 && low>=Short.MIN_VALUE; low-=step) {

				Assert.assertTrue(step > 0);
				Assert.assertTrue(low <= 0);

				int encoded = Bits.encodeAsInt(high, low);

				Assert.assertEquals(Bits.decodeHighBits(encoded), high);
				Assert.assertEquals(Bits.decodeLowBits(encoded), low);
			}
		}
	}
	
	
	@Test
	public void positiveHighBitsIntEncoding() {
		
		short[] lows = {Short.MIN_VALUE, -12345, -1, 0, 1, 12345, Short.MAX_VALUE};
		
		for (short low : lows) {
			for (short high=0, step=(Short.MAX_VALUE/75); high>=0 && high<=Short.MAX_VALUE; high+=step) {
				
				Assert.assertTrue(step > 0);
				Assert.assertTrue(high >= 0);

				int encoded = Bits.encodeAsInt(high, low);

				Assert.assertEquals(Bits.decodeHighBits(encoded), high);
				Assert.assertEquals(Bits.decodeLowBits(encoded), low);
			}
		}
	}
	
	@Test
	public void negativeHighBitsIntEncoding() {

		short[] lows = {Short.MIN_VALUE, -12345, -1, 0, 1, 12345, Short.MAX_VALUE};
		
		for (short low : lows) {
			for (short high=0, step=(Short.MAX_VALUE/75); high>=0 && high>=Short.MIN_VALUE; high-=step) {

				Assert.assertTrue(step > 0);
				Assert.assertTrue(high <= 0);

				int encoded = Bits.encodeAsInt(high, low);

				Assert.assertEquals(Bits.decodeHighBits(encoded), high);
				Assert.assertEquals(Bits.decodeLowBits(encoded), low);
			}
		}
	}
}
