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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Random;

import com.google.common.io.ByteSink;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class DecoderServletTest {

    private int port;
    private Server server;

    @BeforeClass
    public void startServer() throws Exception {
        port = 8080 + new Random().nextInt(100);
        System.out.println("Jetty test port is " + port);
        server = new Server(port);
        WebAppContext context = new WebAppContext();
        context.setConfigurations(new Configuration[] {new AnnotationConfiguration()});
        context.setContextPath("/");
        context.setInitParameter("decoderArgsLine", "-mark-oovs true");
        context.getMetaData().addContainerResource(new PathResource(Paths.get("./target/classes")));

        server.setHandler(context);
        server.start();
        server.dump(System.err);
    }

    @AfterClass
    public void stopServer() throws Exception {
        server.stop();
    }

    public void translateWithQueryParam() throws Exception {
        String response = Resources.toString(
                new URL("http://localhost:" + port + "/?q=I%20love%20it%20when%20I%20get%20the%20house%20clean%20before%20the%20weekend"),
                Charset.forName("UTF-8"));
        String expected = Resources.toString(Resources.getResource("server/http/expected"), Charset.forName("UTF-8"));
        Assert.assertEquals(response, expected);
    }

    public void translateWithRawRequest() throws Exception {
        String data = "I love it when I get the house clean before the weekend";
        //Resources.asCharSink(new URL("http://localhost:" + port), Charset.forName("UTF-8")).
        URL requestUrl = new URL("http://localhost:" + port);
        HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
        conn.setDoOutput(true);
        new ByteSink() {
            @Override
            public OutputStream openStream() throws IOException {
                return conn.getOutputStream();
            }
        }.write(data.getBytes("UTF-8"));
        String response = CharStreams.toString(new InputStreamReader(conn.getInputStream()));
        conn.disconnect();

        String expected = Resources.toString(Resources.getResource("server/http/expected"), Charset.forName("UTF-8"));
        Assert.assertEquals(response, expected);
    }

}
