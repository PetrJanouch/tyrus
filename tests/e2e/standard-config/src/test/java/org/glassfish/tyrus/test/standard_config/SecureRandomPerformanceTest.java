/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class SecureRandomPerformanceTest extends TestContainer {

    private static final int THREAD_COUNT = 10;

    @Test
    public void manyClientsEchoTest() {
        Server server = null;
        try {
            server = startServer(EchoEndpoint.class);
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        ClientManager client = ClientManager.createClient();
                        client.getProperties().put(ClientProperties.SHARED_CONTAINER, true);
                        for (int j = 0; j < 3000; j++) {
                            try {
                                CountDownLatch messageLatch = new CountDownLatch(1);
                                Session session = client.connectToServer(new EchoClientEndpoint(messageLatch), getURI(EchoEndpoint.class));
                                session.getAsyncRemote().sendText("Hello");
                                messageLatch.await(3, TimeUnit.SECONDS);
                                session.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                                fail();
                            }
                        }
                        System.out.println("Stopped");
                        completionLatch.countDown();
                    }
                });
            }

            assertTrue(completionLatch.await(3, TimeUnit.MINUTES));
            System.out.println("Duration:" + (System.currentTimeMillis() - startTime) / 1000);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void manyClientsConnectTest() {
        Server server = null;
        try {
            server = startServer(BlackHoleEndpoint.class);
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        ClientManager client = ClientManager.createClient();
                        client.getProperties().put(ClientProperties.SHARED_CONTAINER, true);
                        for (int j = 0; j < 3000; j++) {
                            try {
                                final CountDownLatch latch = new CountDownLatch(1);
                                client.connectToServer(new Endpoint() {
                                    @Override
                                    public void onOpen(Session session, EndpointConfig config) {
                                        try {
                                            session.close();
                                            latch.countDown();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            fail();
                                        }
                                    }
                                }, getURI(BlackHoleEndpoint.class));

                                assertTrue(latch.await(3, TimeUnit.SECONDS));
                            } catch (Exception e) {
                                e.printStackTrace();
                                fail();
                            }
                        }

                        System.out.println("Stopped");
                        completionLatch.countDown();
                    }
                });
            }

            assertTrue(completionLatch.await(3, TimeUnit.MINUTES));
            System.out.println("Duration:" + (System.currentTimeMillis() - startTime) / 1000);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void fewClientsEchoTest() {
        Server server = null;
        try {
            server = startServer(EchoEndpoint.class);
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
                            ClientManager client = ClientManager.createClient();
                            client.getProperties().put(ClientProperties.SHARED_CONTAINER, true);
                            CountDownLatch messageLatch = new CountDownLatch(300000);
                            Session session = client.connectToServer(new EchoClientEndpoint(messageLatch), getURI(EchoEndpoint.class));
                            for (int j = 0; j < 300000; j++) {
                                session.getBasicRemote().sendText("Hello");
                            }
                            assertTrue(messageLatch.await(2, TimeUnit.MINUTES));
                            session.close();


                            completionLatch.countDown();
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail();
                        }
                    }
                });
            }

            assertTrue(completionLatch.await(3, TimeUnit.MINUTES));
            System.out.println("Duration:" + (System.currentTimeMillis() - startTime) / 1000);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint("/srEchoEndpoint")
    public static class EchoEndpoint {

        @OnMessage
        public void onMessage(String message, Session session) throws IOException {
            session.getBasicRemote().sendText(message);
        }
    }

    @ServerEndpoint("/srBlackHoleEndpoint")
    public static class BlackHoleEndpoint {

        @OnMessage
        public void onMessage(String message, Session session) {
        }
    }

    @ClientEndpoint
    public static class EchoClientEndpoint {

        private final CountDownLatch messageLatch;

        public EchoClientEndpoint(CountDownLatch messageLatch) {
            this.messageLatch = messageLatch;
        }

        @OnMessage
        public void onMessage(String message, Session session) throws IOException {
            messageLatch.countDown();
        }
    }
}
