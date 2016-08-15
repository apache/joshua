/**
 * Copyright 2016  Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except
 * in compliance with the License. A copy of the License is located at
 * http://aws.amazon.com/apache-2-0/
 * or in the "license" file accompanying this file. This file is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.apache.joshua.decoder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.joshua.decoder.io.JSONMessage;
import org.apache.joshua.decoder.io.TranslationRequestStream;

/**
 * Simple servlet implementation to handle translation request via <code>q</code> parameter.
 */
@WebServlet(urlPatterns = "/")
public class DecoderServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Decoder decoder = (Decoder)getServletContext().getAttribute(DecoderServletContextListener.DECODER_CONTEXT_ATTRIBUTE_NAME);

        String param = req.getParameter("q");
        try (InputStream in = new ByteArrayInputStream(param.getBytes());
            OutputStream out = resp.getOutputStream()) {
            resp.setHeader("Content-Type", "application/json");
            handleRequest(decoder, in, out);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Decoder decoder = (Decoder)getServletContext().getAttribute(DecoderServletContextListener.DECODER_CONTEXT_ATTRIBUTE_NAME);

        try (InputStream in = req.getInputStream();
            OutputStream out = resp.getOutputStream()) {
            resp.setHeader("Content-Type", "application/json");
            handleRequest(decoder, in, out);
        }
    }

    private void handleRequest(Decoder decoder, InputStream in, OutputStream out) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
        TranslationRequestStream request = new TranslationRequestStream(reader, decoder.getJoshuaConfiguration());

        Translations translations = decoder.decodeAll(request);

        JSONMessage message = new JSONMessage();
        for (Translation translation : translations) {
            message.addTranslation(translation);
        }
        out.write(message.toString().getBytes());
    }
}
