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
package org.apache.joshua.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.TranslationResponseStream;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.ff.tm.Trie;
import org.apache.joshua.decoder.ff.tm.format.HieroFormatReader;
import org.apache.joshua.decoder.io.JSONMessage;
import org.apache.joshua.decoder.io.TranslationRequestStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles a concurrent request for translations from a newly opened socket, for
 * both raw TCP/IP connections and for HTTP connections.
 * 
 */
public class ServerThread extends Thread implements HttpHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ServerThread.class);
  private static final Charset FILE_ENCODING = Charset.forName("UTF-8");
  
  private final JoshuaConfiguration joshuaConfiguration;
  private Socket socket = null;
  private final Decoder decoder;

  /**
   * Creates a new TcpServerThread that can run a set of translations.
   * 
   * @param socket the socket representing the input/output streams
   * @param decoder the configured decoder that handles performing translations
   * @param joshuaConfiguration a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   */
  public ServerThread(Socket socket, Decoder decoder, JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
    this.socket = socket;
    this.decoder = decoder;
  }

  /**
   * Reads the input from the socket, submits the input to the decoder, transforms the resulting
   * translations into the required output format, writes out the formatted output, then closes the
   * socket.
   */
  @Override
  public void run() {

    //TODO: use try-with-resources block
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), FILE_ENCODING));

      TranslationRequestStream request = new TranslationRequestStream(reader, joshuaConfiguration);

      try {
        TranslationResponseStream translationResponseStream = decoder.decodeAll(request);
        
        OutputStream out = socket.getOutputStream();
        
        for (Translation translation: translationResponseStream) {
          out.write(translation.toString().getBytes());
        }
        
      } catch (SocketException e) {
        LOG.error(" Socket interrupted", e);
        request.shutdown();
      } finally {
        reader.close();
        socket.close();
      }
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }
  
  /**
   * Transforms an HTTP query string into a dictionary of lists of values. The lists are necessary 
   * because the RESTful spec permits multiple keys of the same name.
   * 
   * @param query the query string
   * @return a map of lists of values found for each key in the query string
   * @throws UnsupportedEncodingException if there is a bad encoding
   */
  public HashMap<String, ArrayList<String>> queryToMap(String query) throws UnsupportedEncodingException {
    HashMap<String, ArrayList<String>> result = new HashMap<>();
    if (LOG.isDebugEnabled())
      LOG.debug("Got RESTful query: " + query);
    for (String param : query.split("&")) {
      int pos = param.indexOf('=');
      String key = URLDecoder.decode((pos != -1) ? param.substring(0,  pos) : param, "UTF-8");
      String val = URLDecoder.decode((pos != -1) ? param.substring(pos+1) : "", "UTF-8");
      if (LOG.isDebugEnabled())
        LOG.debug("  -> Got {} = {}", key, val);
      if (! result.containsKey(key))
        result.put(key, new ArrayList<String>());
      result.get(key).add(val);
    }
    return result;
  } 

  private class HttpWriter extends OutputStream {

    private HttpExchange client = null;
    private OutputStream out = null;
    
    public HttpWriter(HttpExchange client) {
      this.client = client;
      client.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    }
    
    @Override
    public void write(byte[] response) throws IOException {
      client.sendResponseHeaders(200, response.length);
      out = client.getResponseBody();
      out.write(response);
      out.close();
    }

    @Override
    public void write(int b) throws IOException {
      out.write(b);
    }
  }

  /**
   * Called to handle an HTTP connection. This looks for metadata in the URL string, which is processed
   * if present. It also then handles returning a JSON-formatted object to the caller. 
   * 
   * URL query strings can have multiple keys of the same name. These are accumulated into arrays,
   * but only multiple "q=" keys are permitted. If multiple keys of other names are found, only the 
   * last one is used. So for the query string:
   * 
   * ?q=a&amp;q=b&amp;meta=c&amp;meta=d
   * 
   * handle() will use q = {a, b} and meta = {d}
   * 
   * @param client the client connection
   */
  @Override
  public synchronized void handle(HttpExchange client) throws IOException {

    HashMap<String, ArrayList<String>> params = queryToMap(client.getRequestURI().getRawQuery());
    ArrayList<String> queryList = params.get("q");
    ArrayList<String> metaList = params.get("meta");
    String meta = (metaList != null && ! metaList.isEmpty()) ? metaList.get(metaList.size() - 1) : null;
    
    /* Join together multiple sentence queries as distinct sentences. */
    BufferedReader reader = new BufferedReader(new StringReader(String.join("\n", queryList)));
    TranslationRequestStream request = new TranslationRequestStream(reader, joshuaConfiguration);
    
    TranslationResponseStream translationResponseStream = decoder.decodeAll(request);
    JSONMessage message = new JSONMessage();
    if (meta != null && ! meta.isEmpty())
      handleMetadata(meta, message);

    for (Translation translation: translationResponseStream) {
      LOG.info("TRANSLATION: '{}' with {} k-best items, score {}", 
          translation, translation.getStructuredTranslations().size());
      message.addTranslation(translation);
    }

    OutputStream out = new HttpWriter(client);
    out.write(message.toString().getBytes());
    if (LOG.isDebugEnabled())
      LOG.debug(message.toString());
    out.close();
    
    reader.close();
  }
  
  /**
   * Processes metadata commands received in the HTTP request. Some commands result in sending data back.
   *
   * @param meta the metadata request
   * @param message the JSON message template that will be filled out.
   */
  private void handleMetadata(String meta, JSONMessage message) {
    String[] tokens = meta.split("\\s+", 2);
    String type = tokens[0];
    String args = tokens.length > 1 ? tokens[1] : "";
    
    LOG.info("META: {}", type);

    switch (type) {
    case "get_weight":
      String weight = tokens[1];
      LOG.info("WEIGHT: %s = %.3f", weight, Decoder.weights.getWeight(weight));

      break;
    case "set_weights": {
      // Change a decoder weight
      String[] argTokens = args.split("\\s+");
      for (int i = 0; i < argTokens.length; i += 2) {
        String feature = argTokens[i];
        String newValue = argTokens[i + 1];
        float old_weight = Decoder.weights.getWeight(feature);
        Decoder.weights.set(feature, Float.parseFloat(newValue));
        LOG.info("set_weights: {} {} -> {}", feature, old_weight,
            Decoder.weights.getWeight(feature));
      }

      message.addMetaData("weights " + Decoder.weights.toString());

      break;
    }
    case "get_weights": {
      message.addMetaData("weights " + Decoder.weights.toString());

      break;
    }
    case "add_rule": {
    
      String argTokens[] = args.split(" \\|\\|\\| ");

      if (argTokens.length < 3) {
        LOG.warn("* INVALID RULE '{}'", meta);
        return;
      }
      
      String lhs = argTokens[0];
      String source = argTokens[1];
      String target = argTokens[2];
      String featureStr = "";
      String alignmentStr = "";
      if (argTokens.length > 3)
        featureStr = argTokens[3].trim();
      if (argTokens.length > 4)
        alignmentStr = argTokens[4].trim();

      /* Prepend source and target side nonterminals for phrase-based decoding. Probably better
       * handled in each grammar type's addRule() function.
       */
      String ruleString = String.format("%s ||| %s ||| %s ||| -1", lhs, source, target);
      if (! featureStr.equals(""))
        ruleString += featureStr;
      if (! alignmentStr.equals(""))
        ruleString += " ||| " + alignmentStr;

      Rule rule = new HieroFormatReader().parseLine(ruleString);
      decoder.addCustomRule(rule);

      LOG.info("Added custom rule {}", rule.toString());

      break;
    }
    case "list_rules": {

      // Walk the the grammar trie
      ArrayList<Trie> nodes = new ArrayList<>();
      nodes.add(decoder.getCustomPhraseTable().getTrieRoot());

      while (nodes.size() > 0) {
        Trie trie = nodes.remove(0);

        if (trie == null)
          continue;

        if (trie.hasRules()) {
          for (Rule rule : trie.getRuleCollection().getRules()) {
            message.addRule(rule.toString());
            LOG.debug("Found rule: " + rule);
          }
        }

        if (trie.getExtensions() != null)
          nodes.addAll(trie.getExtensions());
      }

      break;
    }
    case "remove_rule": {

      Rule rule = new HieroFormatReader().parseLine(args);

      Trie trie = decoder.getCustomPhraseTable().getTrieRoot();
      int[] sourceTokens = rule.getFrench();
      for (int sourceToken : sourceTokens) {
        Trie nextTrie = trie.match(sourceToken);
        if (nextTrie == null)
          return;

        trie = nextTrie;
      }

      if (trie.hasRules()) {
        for (Rule ruleCand : trie.getRuleCollection().getRules()) {
          if (Arrays.equals(rule.getEnglish(), ruleCand.getEnglish())) {
            trie.getRuleCollection().getRules().remove(ruleCand);
            break;
          }
        }
      }
      
      decoder.saveCustomPhraseTable();
      
      break;
    }
    default: {
      LOG.warn("INVALID metadata command '{}'", type);
      break;
    }
    }
  }
}
