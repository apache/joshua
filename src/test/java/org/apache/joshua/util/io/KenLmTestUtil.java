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

import org.apache.joshua.decoder.ff.lm.KenLM;
import org.testng.SkipException;

/**
 * Created by kellens on 7/12/16.
 */
public class KenLmTestUtil {

  public static final int STACK_INSPECTION_LIMIT = 20;

  // Catch any exceptions that could be thrown by KenLM, and skip the test if found.
  public static void Guard(Runnable kenLmLoadingCode) {
    try {
      kenLmLoadingCode.run();
    } catch (KenLM.KenLMLoadException e) {
      skip();
    }
    catch (RuntimeException re) {
      Throwable cause = re.getCause();
      int stackCount = 0;

      while (cause != null && stackCount < STACK_INSPECTION_LIMIT) {
        stackCount++;
        if (cause instanceof  KenLM.KenLMLoadException) {
          skip();
          return;
        } else {
          cause = cause.getCause();
        }
      }

      throw re;
    }
  }

  private static void skip() {
    throw new SkipException("Skipping test because KenLM.so/dylib was not found");
  }
}
