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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.google.common.base.Throwables;

/**
 * Initializes {@link Decoder} via <code>decoderArgsLine</code> init parameter.
 */
@WebListener
public class DecoderServletContextListener implements ServletContextListener {

    /**
     * Attribute name to locate initialized {@link Decoder} instance in ServletContext.
     */
    public static final String DECODER_CONTEXT_ATTRIBUTE_NAME = Decoder.class.getName();

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String argsLine = sce.getServletContext().getInitParameter("decoderArgsLine");
        try {
            JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
            ArgsParser userArgs = new ArgsParser(argsLine.split(" "), joshuaConfiguration);
            joshuaConfiguration.use_structured_output = true;
            joshuaConfiguration.sanityCheck();
            Decoder decoder = new Decoder(joshuaConfiguration, userArgs.getConfigFile());
            sce.getServletContext().setAttribute(DECODER_CONTEXT_ATTRIBUTE_NAME, decoder);
        } catch (Exception ex) {
            Throwables.propagate(ex);
        }
    }
}
