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
package org.glassfish.tyrus.ext.monitoring.jmx;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.fail;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class MessageStatisticsJConsoleTest extends TestContainer {

    @ServerEndpoint("/jmxServerEndpoint1")
    public static class AnnotatedServerEndpoint1 {

        @OnMessage
        public void messageReceived(Session session, String text) throws IOException {
            session.getBasicRemote().sendText(text);
            session.getBasicRemote().sendText(text);
        }

        @OnMessage
        public void messageReceived(Session session, ByteBuffer data) throws IOException {
            session.getBasicRemote().sendBinary(data);
            session.getBasicRemote().sendBinary(data);
        }

        @OnMessage
        public void messageReceived(Session session, PongMessage pong) throws IOException {
            session.getBasicRemote().sendPong(pong.getApplicationData());
            session.getBasicRemote().sendPong(pong.getApplicationData());
        }

    }

    @ServerEndpoint("/jmxServerEndpoint2")
    public static class AnnotatedServerEndpoint2 {

        @OnMessage
        public void messageReceived(Session session, String text) throws IOException {
            session.getBasicRemote().sendText(text);
            session.getBasicRemote().sendText(text);
        }

        @OnMessage
        public void messageReceived(Session session, ByteBuffer data) throws IOException {
            session.getBasicRemote().sendBinary(data);
            session.getBasicRemote().sendBinary(data);
        }

        @OnMessage
        public void messageReceived(Session session, PongMessage pong) throws IOException {
            session.getBasicRemote().sendPong(pong.getApplicationData());
            session.getBasicRemote().sendPong(pong.getApplicationData());
        }
    }

    @ServerEndpoint("/jmxServerEndpoint3")
    public static class AnnotatedServerEndpoint3 {

        @OnMessage
        public void messageReceived(Session session, String text) throws IOException {
            session.getBasicRemote().sendText(text);
            session.getBasicRemote().sendText(text);
        }

        @OnMessage
        public void messageReceived(Session session, ByteBuffer data) throws IOException {
            session.getBasicRemote().sendBinary(data);
            session.getBasicRemote().sendBinary(data);
        }

        @OnMessage
        public void messageReceived(Session session, PongMessage pong) throws IOException {
            session.getBasicRemote().sendPong(pong.getApplicationData());
            session.getBasicRemote().sendPong(pong.getApplicationData());
        }
    }

    @ClientEndpoint
    public static class DummyClientEndpoint {

        @OnMessage
        public void messageReceived(Session session, String text) {
        }

        @OnMessage
        public void messageReceived(Session session, ByteBuffer data) {
        }

        @OnMessage
        public void messageReceived(Session session, PongMessage pong) {
        }

    }

    @Test
    public void test() {
        Server server1 = null;
        Server server2 = null;
        try {
            Map<String, Object> server1Properties = new HashMap<String, Object>();
            ApplicationEventListener application1EventListener = new ApplicationJmx();
            server1Properties.put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, application1EventListener);
            server1 = new Server("localhost", 8025, "/jmxTestApp", server1Properties, AnnotatedServerEndpoint1.class, AnnotatedServerEndpoint2.class);
            server1.start();

            Map<String, Object> server2Properties = new HashMap<String, Object>();
            server2Properties.put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new ApplicationJmx());
            server2 = new Server("localhost", 8026, "/jmxTestApp2", server2Properties, AnnotatedServerEndpoint2.class, AnnotatedServerEndpoint3.class);
            server2.start();

            ClientManager client1 = createClient();
            Session session1 = client1.connectToServer(DummyClientEndpoint.class, new URI("ws", null, "localhost", 8025, "/jmxTestApp/jmxServerEndpoint1", null, null));

            ClientManager client2 = createClient();
            Session session2 = client2.connectToServer(DummyClientEndpoint.class, new URI("ws", null, "localhost", 8025, "/jmxTestApp/jmxServerEndpoint2", null, null));

            ClientManager client3 = createClient();
            Session session3 = client3.connectToServer(DummyClientEndpoint.class, new URI("ws", null, "localhost", 8025, "/jmxTestApp/jmxServerEndpoint2", null, null));

            ClientManager client4 = createClient();
            Session session4 = client4.connectToServer(DummyClientEndpoint.class, new URI("ws", null, "localhost", 8026, "/jmxTestApp2/jmxServerEndpoint2", null, null));

            ClientManager client5 = createClient();
            Session session5 = client5.connectToServer(DummyClientEndpoint.class, new URI("ws", null, "localhost", 8026, "/jmxTestApp2/jmxServerEndpoint3", null, null));

            ClientManager client6 = createClient();
            Session session6 = client6.connectToServer(DummyClientEndpoint.class, new URI("ws", null, "localhost", 8026, "/jmxTestApp2/jmxServerEndpoint3", null, null));

            while (true) {
                int messageType = 0 + (int) (Math.random() * 3);
                if (messageType == 0) {
                    session1.getBasicRemote().sendText(getRandomText());
                    session2.getBasicRemote().sendText(getRandomText());
                    session3.getBasicRemote().sendText(getRandomText());
                    session4.getBasicRemote().sendText(getRandomText());
                    session5.getBasicRemote().sendText(getRandomText());
                    session6.getBasicRemote().sendText(getRandomText());
                } else if (messageType == 1) {
                    session1.getBasicRemote().sendBinary(getRandomBytes());
                    session2.getBasicRemote().sendBinary(getRandomBytes());
                    session3.getBasicRemote().sendBinary(getRandomBytes());
                    session4.getBasicRemote().sendBinary(getRandomBytes());
                    session5.getBasicRemote().sendBinary(getRandomBytes());
                    session6.getBasicRemote().sendBinary(getRandomBytes());
                } else {
                    session1.getBasicRemote().sendPing(getRandomBytes());
                    session2.getBasicRemote().sendPing(getRandomBytes());
                    session3.getBasicRemote().sendPing(getRandomBytes());
                    session4.getBasicRemote().sendPing(getRandomBytes());
                    session5.getBasicRemote().sendPing(getRandomBytes());
                    session6.getBasicRemote().sendPing(getRandomBytes());
                }
                System.out.println("Sent");

                Thread.sleep(500);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            if (server1 != null) {
                server1.stop();
            }

            if (server2 != null) {
                server2.stop();
            }
        }
    }

    private String getRandomText() {
        int length = 0 + (int) (Math.random() * 10);
        String s = "AAAAAAAAAA";
        return s.substring(0, length);
    }

    private ByteBuffer getRandomBytes() {
        return ByteBuffer.wrap(getRandomText().getBytes());
    }

}
