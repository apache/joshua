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
import java.util.HashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.Translations;
import org.apache.joshua.decoder.JoshuaConfiguration.INPUT_TYPE;
import org.apache.joshua.decoder.JoshuaConfiguration.SERVER_TYPE;
import org.apache.joshua.decoder.io.JSONMessage;
import org.apache.joshua.decoder.io.TranslationRequestStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles a concurrent request for translations from a newly opened socket.
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
      
      
  @Override
  public void handle(HttpExchange client) throws IOException {

    HashMap<String, String> params = queryToMap(URLDecoder.decode(client.getRequestURI().getQuery(), "UTF-8"));
    String query = params.get("q");
    
    BufferedReader reader = new BufferedReader(new StringReader(query));
    TranslationRequestStream request = new TranslationRequestStream(reader, joshuaConfiguration);
    
    Translations translations = decoder.decodeAll(request);
    OutputStream out = new HttpWriter(client);
    
    for (Translation translation: translations) {
      if (joshuaConfiguration.input_type == INPUT_TYPE.json || joshuaConfiguration.server_type == SERVER_TYPE.HTTP) {
        JSONMessage message = JSONMessage.buildMessage(translation);
        out.write(message.toString().getBytes());
      }
    }
    out.close();
    reader.close();
  }
}
