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
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.Translations;
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
        Translations translations = decoder.decodeAll(request);
        
        OutputStream out = socket.getOutputStream();
        
        for (Translation translation: translations) {
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
  
  public HashMap<String, String> queryToMap(String query){
    HashMap<String, String> result = new HashMap<String, String>();
    for (String param : query.split("&")) {
        String pair[] = param.split("=");
        if (pair.length > 1) {
            result.put(pair[0], pair[1]);
        } else {
            result.put(pair[0], "");
        }
    }
    return result;
  } 

  private class HttpWriter extends OutputStream {

    private HttpExchange client = null;
    private OutputStream out = null;
    
    public HttpWriter(HttpExchange client) {
      this.client = client;
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
   * @param client the client connection
   */
  @Override
  public void handle(HttpExchange client) throws IOException {

    HashMap<String, String> params = queryToMap(URLDecoder.decode(client.getRequestURI().getQuery(), "UTF-8"));
    String query = params.get("q");
    String meta = params.get("meta");
    
    BufferedReader reader = new BufferedReader(new StringReader(query));
    TranslationRequestStream request = new TranslationRequestStream(reader, joshuaConfiguration);
    
    Translations translations = decoder.decodeAll(request);
    JSONMessage message = new JSONMessage();
    if (meta != null && ! meta.isEmpty())
      message.setMetaData(handleMetadata(meta));

    for (Translation translation: translations) {
      LOG.info("TRANSLATION: '{}' with {} k-best items", translation, translation.getStructuredTranslations().size());
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
   * @return result string (for some commands)
   */
  private String handleMetadata(String meta) {
    String[] tokens = meta.split("\\s+", 2);
    String type = tokens[0];
    String args = tokens.length > 1 ? tokens[1] : "";
    String response = "";
    
    if (type.equals("get_weight")) {
      String weight = tokens[1];
      LOG.info("WEIGHT: %s = %.3f", weight, Decoder.weights.getWeight(weight));

    } else if (type.equals("set_weights")) {
      // Change a decoder weight
      String[] argTokens = args.split("\\s+");
      for (int i = 0; i < argTokens.length; i += 2) {
        String feature = argTokens[i];
        String newValue = argTokens[i+1];
        float old_weight = Decoder.weights.getWeight(feature);
        Decoder.weights.set(feature, Float.parseFloat(newValue));
        LOG.info("set_weights: {} {} -> {}", feature, old_weight, Decoder.weights.getWeight(feature));
      }
      
      response = "weights " + Decoder.weights.toString();
      
    } else if (type.equals("get_weights")) {
      response = "weights " + Decoder.weights.toString();
      
    } else if (type.equals("add_rule")) {
      String argTokens[] = args.split(" ,,, ");
  
      if (argTokens.length != 2) {
        LOG.error("* INVALID RULE '{}'", meta);
        return "";
      }
      
      String source = argTokens[0];
      String target = argTokens[1];
      String featureStr = "";
      if (argTokens.length > 2) 
        featureStr = argTokens[2];
          
      /* Prepend source and target side nonterminals for phrase-based decoding. Probably better
       * handled in each grammar type's addRule() function.
       */
      String ruleString = (joshuaConfiguration.search_algorithm.equals("stack"))
          ? String.format("[X] ||| [X,1] %s ||| [X,1] %s ||| custom=1 %s", source, target, featureStr)
          : String.format("[X] ||| %s ||| %s ||| custom=1 %s", source, target, featureStr);
      
      Rule rule = new HieroFormatReader().parseLine(ruleString);
      decoder.addCustomRule(rule);
      
      LOG.info("Added custom rule {}", rule.toString());
  
    } else if (type.equals("list_rules")) {
  
      LOG.info("list_rules");
      
      JSONMessage message = new JSONMessage();
  
      // Walk the the grammar trie
      ArrayList<Trie> nodes = new ArrayList<Trie>();
      nodes.add(decoder.getCustomPhraseTable().getTrieRoot());
  
      while (nodes.size() > 0) {
        Trie trie = nodes.remove(0);
  
        if (trie == null)
          continue;
  
        if (trie.hasRules()) {
          for (Rule rule: trie.getRuleCollection().getRules()) {
            message.addRule(rule.toString());
            LOG.info("Found rule: " + rule);
          }
        }
  
        if (trie.getExtensions() != null)
          nodes.addAll(trie.getExtensions());
      }
  
    } else if (type.equals("remove_rule")) {
      // Remove a rule from a custom grammar, if present
      String[] argTokens = args.split(" ,,, ");
      if (argTokens.length != 2) {
        return "";
      }
  
      // Search for the rule in the trie
      int nt_i = Vocabulary.id(joshuaConfiguration.default_non_terminal);
      Trie trie = decoder.getCustomPhraseTable().getTrieRoot().match(nt_i);
  
      for (String word: argTokens[0].split("\\s+")) {
        int id = Vocabulary.id(word);
        Trie nextTrie = trie.match(id);
        if (nextTrie != null)
          trie = nextTrie;
      }
  
      if (trie.hasRules()) {
        Rule matched = null;
        for (Rule rule: trie.getRuleCollection().getRules()) {
          String target = rule.getEnglishWords();
          target = target.substring(target.indexOf(' ') + 1);
  
          if (argTokens[1].equals(target)) {
            matched = rule;
            break;
          }
        }
        trie.getRuleCollection().getRules().remove(matched);
        return "";
      }
    }
    return response;
  }
}
