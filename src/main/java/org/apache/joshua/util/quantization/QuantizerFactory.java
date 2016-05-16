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
package org.apache.joshua.util.quantization;
 
 
public class QuantizerFactory { 
 
  public static Quantizer get(String key) { 
    if ("boolean".equals(key)) { 
      return new BooleanQuantizer(); 
 
//    } else if ("byte".equals(key)) { 
//      return new ByteQuantizer(); 
// 
//    } else if ("char".equals(key)) { 
//      return new CharQuantizer(); 
// 
//    } else if ("short".equals(key)) { 
//      return new ShortQuantizer(); 
// 
//    } else if ("float".equals(key)) { 
//      return new FloatQuantizer(); 
// 
//    } else if ("int".equals(key)) { 
//      return new IntQuantizer(); 
// 
//    } else if ("8bit".equals(key)) { 
//      return new EightBitQuantizer(); 
 
    } else { 
      throw new RuntimeException("Unknown quantizer type: " + key); 
    } 
  } 
}