///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *  http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//package org.apache.joshua.decoder.ff;
//
//import org.apache.joshua.decoder.ff.tm.BilingualRule;
//import org.apache.joshua.decoder.ff.tm.MonolingualRule;
//import org.apache.joshua.decoder.ff.tm.Rule;
//
//import org.testng.Assert;
//import org.testng.annotations.Test;
//
///**
// * Unit tests for ArityPhrasePenaltyFF.
// * 
// * @author Lane Schwartz
// * @version $LastChangedDate$
// */
//public class ArityPhrasePenaltyFFTest {
//
//  @Test
//  public void alpha() {
//    Assert.assertEquals(ArityPhrasePenaltyFF.ALPHA, - Math.log10(Math.E));
//  }
//
//  @Test
//  public void estimate() {
//
//    int featureID = 0;
//    double weight = 0.0;
//    int owner = MonolingualRule.DUMMY_OWNER;
//    int min = 1;
//    int max = 5;
//
//    ArityPhrasePenaltyFF featureFunction = new ArityPhrasePenaltyFF(featureID, weight, owner, min, max);
//
//    int lhs = -1;
//    int[] sourceRHS = {24, -1, 42, 738};
//    int[] targetRHS = {-1, 7, 8};
//    float[] featureScores = {-2.35f, -1.78f, -0.52f};
//    int arity = 1;
//
//    Rule dummyRule = new BilingualRule(lhs, sourceRHS, targetRHS, featureScores, arity);
//
//    Assert.assertEquals(featureFunction.estimateLogP(dummyRule, -1), ArityPhrasePenaltyFF.ALPHA);
//
//  }
//
//}
