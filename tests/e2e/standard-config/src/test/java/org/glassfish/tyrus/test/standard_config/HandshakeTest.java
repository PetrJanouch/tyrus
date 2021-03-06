/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.tyrus.test.standard_config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.TestEndpoint;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests whether the HandShake parameters (sub-protocols, extensions) are sent correctly.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class HandshakeTest extends TestContainer {

    private CountDownLatch messageLatch;

    private String receivedMessage;

    private static final String SENT_MESSAGE = "hello";

    public static class MyClientEndpointConfigurator extends ClientEndpointConfig.Configurator {

        public HandshakeResponse hr;

        @Override
        public void afterResponse(HandshakeResponse hr) {
            this.hr = hr;
        }
    }

    @Test
    public void testClient() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);

            ArrayList<String> subprotocols = new ArrayList<String>();
            subprotocols.add("asd");
            subprotocols.add("ghi");

            final MyClientEndpointConfigurator myClientEndpointConfigurator = new MyClientEndpointConfigurator();

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().
                    preferredSubprotocols(subprotocols).configurator(myClientEndpointConfigurator).build();

            ClientManager client = createClient();
            client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                    receivedMessage = message;
                    messageLatch.countDown();
                    System.out.println("Received message = " + message);
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Sent message: " + SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(TestEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(SENT_MESSAGE, receivedMessage);

            Map<String, List<String>> headers = myClientEndpointConfigurator.hr.getHeaders();

            String supportedSubprotocol = headers.get(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL).get(0);
            Assert.assertEquals("asd", supportedSubprotocol);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
