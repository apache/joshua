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

import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.joshua.decoder.io.TranslationRequestStream;
import org.apache.joshua.server.ServerThread;
import org.apache.joshua.server.TcpServer;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.MapOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

/**
 * Command-line tool for the Joshua Decoder.
 *
 * @author Zhifei Li, zhifei.work@gmail.com
 * @author wren ng thornton wren@users.sourceforge.net
 * @author Lane Schwartz dowobeha@users.sourceforge.net
 * @author Felix Hieber felix.hieber@gmail.com
 */
public class JoshuaDecoder {

  private static final Logger LOG = LoggerFactory.getLogger(JoshuaDecoder.class);
  
  @Option(name="--decoderConfig", aliases={"-c"}, metaVar="DECODER.CFG", required=false, usage="configuration file for the decoder (i.e., joshua.config")
  private File configFile = null;
  
  @Option(name = "-C", handler=MapOptionHandler.class, metaVar = "<property>=<value>", usage = "use value for given key to override flags in the config file, i.e., -C top_n=4", required=false)
  private Map<String, String> overrides = new HashMap<>();
  
  @Option(name="--verbose", aliases={"-v"}, required=false, usage="log level of the decoder")
  private String logLevel = Level.INFO.toString();
  
  @Option(name="--help", aliases={"-h"}, required=false, usage="show configuration options and quit.")
  private boolean help = false;
  
  /**
   * Returns the flags composed of default config, given config, and commandline overrides.
   */
  private Config getFlags() {
    final Config commandLineOverrides = ConfigFactory.parseMap(overrides, "CmdLine overrides");
    if (configFile == null) {
      return commandLineOverrides.withFallback(Decoder.getDefaultFlags()).resolve();
    } else {
      return commandLineOverrides.withFallback(Decoder.getFlagsFromFile(configFile)).resolve();
    }
  }
  
  private static void printFlags(Config flags) {
    System.err.println("Joshua configuration options with default values:");
    System.err.println(
        flags.root().render(ConfigRenderOptions
            .concise()
            .setFormatted(true)
            .setComments(true)));
  }

  private void run() throws IOException {
    
    // set loglevel
    LogManager.getRootLogger().setLevel(Level.toLevel(logLevel));
    
    // load & compose flags
    final Config config = getFlags();
    
    if (help) {
      printFlags(config);
      return;
    }
    
    // initialize the Decoder
    final long initStartTime = System.currentTimeMillis();
    final Decoder decoder = new Decoder(config);
    final float initTime = (System.currentTimeMillis() - initStartTime) / 1000.0f;
    final float usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0f;
    LOG.info("Model loading took {} seconds", initTime);
    LOG.info("Memory used {} MB", usedMemory);
    
    // create a server if requested, which will create TranslationRequest objects
    final Config serverConfig = config.getConfig("serverSettings");
    if (serverConfig.getInt("server_port") > 0) {
      final int port = serverConfig.getInt("server_port");
      final ServerType serverType = ServerType.valueOf(serverConfig.getString("server_type"));
      if (serverType == ServerType.TCP) {
        new TcpServer(decoder, port).start();

      } else if (serverType == ServerType.HTTP) {
        checkState(config.getBoolean("use_structured_output"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        LOG.info("HTTP Server running and listening on port {}.", port);
        server.createContext("/", new ServerThread(null, decoder));
        server.setExecutor(null); // creates a default executor
        server.start();
      } else {
        LOG.error("Unknown server type");
        System.exit(1);
      }
      return;
    }

    // Create a TranslationRequest object, reading from a file if requested, or from STDIN
    InputStream input = (!config.getString("input_file").isEmpty())
      ? new FileInputStream(config.getString("input_file"))
      : System.in;
    
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    TranslationRequestStream fileRequest = new TranslationRequestStream(reader, config);
    TranslationResponseStream translationResponseStream = decoder.decodeAll(fileRequest);

    // Create the n-best output stream
    FileWriter nbest_out = null;
    if (!config.getString("n_best_file").isEmpty()) {
      nbest_out = new FileWriter(config.getString("n_best_file"));
    }

    for (Translation translation: translationResponseStream) {
      /**
       * We need to munge the feature value outputs in order to be compatible with Moses tuners.
       * Whereas Joshua writes to STDOUT whatever is specified in the `output-format` parameter,
       * Moses expects the simple translation on STDOUT and the n-best list in a file with a fixed
       * format.
       */
      if (config.getBoolean("moses")) {
        String text = translation.toString().replaceAll("=", "= ");
        // Write the complete formatted string to STDOUT
        if (!config.getString("n_best_file").isEmpty()) {
          nbest_out.write(text);
        }

        // Extract just the translation and output that to STDOUT
        text = text.substring(0, text.indexOf('\n'));
        String[] fields = text.split(" \\|\\|\\| ");
        text = fields[1];

        System.out.println(text);

      } else {
        System.out.print(translation.toString());
      }
    }

    if (!config.getString("n_best_file").isEmpty()) {
      nbest_out.close();
    }

    LOG.info("Decoding completed.");
    LOG.info("Memory used {} MB", ((Runtime.getRuntime().totalMemory()
        - Runtime.getRuntime().freeMemory()) / 1000000.0));

    /* Step-3: clean up */
    decoder.cleanUp();
    LOG.info("Total running time: {} seconds", (System.currentTimeMillis() - initStartTime) / 1000);
  }
  
  public static void main(String[] args) {
    final JoshuaDecoder cli = new JoshuaDecoder();
    final CmdLineParser parser = new CmdLineParser(cli);
    try {
        parser.parseArgument(args);
        cli.run();
    } catch (CmdLineException e) {
        // handling of wrong arguments
        LOG.error(e.getMessage());
        parser.printUsage(System.err);
        System.exit(1);
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }
}
