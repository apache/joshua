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
 * Unit tests for Counts class.
 * 
 * @author Lane Schwartz
 */
public class CountsTest {

	@Test
	public void verifyCounts() {
		
		Counts<Integer,Integer> counts = new Counts<Integer,Integer>();
		
		int maxA = 100;
		int maxB = 100;
		
		// Increment counts
		for (int a=0; a<maxA; a++) {
			for (int b=0; b<maxB; b++) {
				
				for (int n=0, times=b%10; n<=times; n++) {
					counts.incrementCount(a,b);
					counts.incrementCount(null, b);
				}
				
			}
			
			for (int n=0, times=10-a%10; n<times; n++) {
				counts.incrementCount(a,null);
			}
		}
		
		// Verify co-occurrence counts
		for (int a=0; a<maxA; a++) {
			for (int b=0; b<maxB; b++) {
				int expected = b%10 + 1;
				Assert.assertEquals(counts.getCount(a, b), expected);
				Assert.assertEquals(counts.getCount(null, b), maxA*expected);
			}
			
			int expected = 10 - a%10;
			Assert.assertEquals(counts.getCount(a, null), expected);
		}
		
		// Verify totals for B counts
		for (int b=0; b<maxB; b++) {
			int expected = maxA * 2 * (b%10 + 1);
			Assert.assertEquals(counts.getCount(b), expected);
		}
		
		// Verify probabilities
		for (int a=0; a<maxA; a++) {
			for (int b=0; b<maxB; b++) {
				float expected = 1.0f / (maxA*2);
				Assert.assertEquals(counts.getProbability(a, b), expected);
				Assert.assertEquals(counts.getProbability(null, b), 0.5f);
			}
			
			int aCounter = 0;
			for (int b=0; b<maxB; b++) {
				for (int n=0, times=b%10; n<=times; n++) {
					aCounter++;
				}
			}
			for (int n=0, times=10-a%10; n<times; n++) {
				aCounter++;
			}
				
			float nullExpected = (float) (10-a%10) / (float) (aCounter);
			Assert.assertEquals(counts.getReverseProbability(null, a), nullExpected);
		
		}
			
	}
	
}
