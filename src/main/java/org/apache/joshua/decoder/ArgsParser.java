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
package org.apache.joshua.decoder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.joshua.util.Constants;
import org.apache.joshua.util.io.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author orluke
 *
 */
public class ArgsParser {

  private static final Logger LOG = LoggerFactory.getLogger(ArgsParser.class);

  private String configFile = null;

  /**
   * Parse the arguments passed from the command line when the JoshuaDecoder application was
   * executed from the command line.
   *
   * @param args string array of input arguments
   * @param config the {@link org.apache.joshua.decoder.JoshuaConfiguration}
   * @throws IOException if there is an error wit the input arguments
   */
  public ArgsParser(String[] args, JoshuaConfiguration config) throws IOException {

    /*
     * Look for a verbose flag, -v.
     *
     * Look for an argument to the "-config" flag to find the config file, if any.
     */
    if (args.length >= 1) {
      // Search for a verbose flag
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-v")) {
          Decoder.VERBOSE = Integer.parseInt(args[i + 1].trim());
          config.setVerbosity(Decoder.VERBOSE);
        }

        if (args[i].equals("-version")) {
          System.out.println(String.format("The Apache Joshua Machine Translator, version %s", Constants.VERSION));
          System.out.println("http://joshua.apache.org/");
          System.exit(0);
        } else if (args[i].equals("-license")) {
          try {
            Files.readAllLines(Paths.get(String.format("%s/../LICENSE",
                JoshuaConfiguration.class.getProtectionDomain().getCodeSource().getLocation()
                    .getPath())), Charset.defaultCharset()).forEach(System.out::println);
          } catch (IOException e) {
            throw new RuntimeException("FATAL: missing license file!", e);
          }
          System.exit(0);
        }
      }

      // Search for the configuration file from the end (so as to take the last one)
      for (int i = args.length-1; i >= 0; i--) {
        if (args[i].equals("-c") || args[i].equals("-config")) {

          setConfigFile(args[i + 1].trim());
          try {
            LOG.info("Parameters read from configuration file: {}", getConfigFile());
            config.readConfigFile(getConfigFile());

            // Now set the parent directory
            config.setConfigFilePath(new File(getConfigFile()).getCanonicalFile().getParent());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          break;
        }
      }

      // Now process all the command-line args
      config.processCommandLineOptions(args);
    }
  }

  /**
   * @return the configFile
   */
  public String getConfigFile() {
    return configFile;
  }

  /**
   * @param configFile the configFile to set
   */
  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }
}
