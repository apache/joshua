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
package org.apache.joshua.pro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import org.apache.joshua.util.StreamGobbler;
import org.apache.joshua.util.io.LineReader;

// sparse feature representation version
public class ClassifierMegaM implements ClassifierInterface {
  @Override
  public double[] runClassifier(Vector<String> samples, double[] initialLambda, int featDim) {
    double[] lambda = new double[featDim + 1];
    System.out.println("------- MegaM training starts ------");

    try {
      // prepare training file for MegaM
      PrintWriter prt = new PrintWriter(new FileOutputStream(trainingFilePath));
      String[] feat;
      String[] featInfo;

      for (String line : samples) {
        feat = line.split("\\s+");

        if (feat[feat.length - 1].equals("1"))
          prt.print("1 ");
        else
          prt.print("0 ");

        // only for dense representation
        // for(int i=0; i<feat.length-1; i++)
        // prt.print( (i+1) + " " + feat[i]+" "); //feat id starts from 1!

        for (int i = 0; i < feat.length - 1; i++) {
          featInfo = feat[i].split(":");
          prt.print(featInfo[0] + " " + featInfo[1] + " ");
        }
        prt.println();
      }
      prt.close();

      // start running MegaM
      Runtime rt = Runtime.getRuntime();
      Process p = rt.exec(commandFilePath);

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), 1);
      StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), 1);

      errorGobbler.start();
      outputGobbler.start();

      int decStatus = p.waitFor();
      if (decStatus != 0) {
        throw new RuntimeException("Call to decoder returned " + decStatus + "; was expecting " + 0 + ".");
      }

      // read the weights
      for (String line: new LineReader(weightFilePath)) {
        String val[] = line.split("\\s+");
        lambda[Integer.parseInt(val[0])] = Double.parseDouble(val[1]);
      }

      File file = new File(trainingFilePath);
      file.delete();
      file = new File(weightFilePath);
      file.delete();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    System.out.println("------- MegaM training ends ------");

    /*
     * try { Thread.sleep(20000); } catch(InterruptedException e) { }
     */

    return lambda;
  }

  @Override
  /*
   * for MegaM classifier: param[0] = MegaM command file path param[1] = MegaM training data
   * file(generated on the fly) path param[2] = MegaM weight file(generated after training) path
   * note that the training and weight file path should be consistent with that specified in the
   * command file
   */
  public void setClassifierParam(String[] param) {
    if (param == null) {
      throw new RuntimeException("ERROR: must provide parameters for MegaM classifier!");
    } else {
      commandFilePath = param[0];
      trainingFilePath = param[1];
      weightFilePath = param[2];
    }
  }

  String commandFilePath;
  String trainingFilePath;
  String weightFilePath;
}
