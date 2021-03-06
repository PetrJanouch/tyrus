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

package org.glassfish.tyrus.core;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.core.frame.Frame;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the RemoteEndpointWrapper.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class TyrusRemoteEndpointTest {

    private final byte[] sentBytes = {'a', 'b', 'c'};
    private final byte[] sentBytesComplete = {'a', 'b', 'c', 'a', 'b', 'c'};
    private TyrusEndpointWrapper endpointWrapper;
    private BaseContainer container;

    public TyrusRemoteEndpointTest() {
        try {
            endpointWrapper = new TyrusEndpointWrapper(EchoEndpoint.class, null, ComponentProviderService.create(), new TestContainer(), null, null, null, null, null, null);
        } catch (DeploymentException e) {
            // do nothing.
        }
    }

    @Before
    public void before() {
        container = createContainer();
    }

    @After
    public void after() {
        container.shutdown();
    }

    @Test
    public void testGetSendStream() throws IOException {

        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);
        OutputStream stream = rew.getSendStream();

        for (byte b : sentBytes) {
            stream.write(b);
        }

        stream.flush();

        // Assert.assertArrayEquals("Writing bytes one by one to stream and flushing.", sentBytes, tre.getBytesAndClearBuffer());

        stream.write(sentBytes);
        stream.close();

        Assert.assertArrayEquals("Writing byte[] to stream and flushing.", sentBytesComplete, tre.getBytesAndClearBuffer());
    }

    @Test
    public void testGetSendStreamWriteArrayWhole() throws IOException {

        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);
        OutputStream stream = rew.getSendStream();

        stream.write(sentBytesComplete);
        Assert.assertEquals(6, tre.getLastSentMessageSize());
        stream.close();
        Assert.assertEquals(0, tre.getLastSentMessageSize());

        Assert.assertArrayEquals("Writing byte[] to stream and flushing.", sentBytesComplete, tre.getBytesAndClearBuffer());
    }

    @Test
    public void testGetSendStreamWriteArrayPerPartes() throws IOException {

        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);
        OutputStream stream = rew.getSendStream();

        stream.write(sentBytes);
        Assert.assertEquals(3, tre.getLastSentMessageSize());
        stream.write(sentBytes);
        Assert.assertEquals(3, tre.getLastSentMessageSize());
        stream.close();
        Assert.assertEquals(0, tre.getLastSentMessageSize());

        Assert.assertArrayEquals("Writing byte[] to stream and flushing.", sentBytesComplete, tre.getBytesAndClearBuffer());
    }


    @Test
    public void testGetSendWriter() throws IOException {
        final String sentString = "abc";

        char[] toSend = sentString.toCharArray();
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);
        Writer writer = rew.getSendWriter();

        writer.write(toSend, 0, 3);
        writer.flush();
        Assert.assertEquals("Writing the whole message.", sentString, tre.getStringAndCleanBuilder());

        writer.write(toSend, 0, 1);
        writer.flush();
        Assert.assertEquals("Writing first character.", String.valueOf(toSend[0]), tre.getStringAndCleanBuilder());

        writer.write(toSend, 2, 1);
        writer.flush();
        Assert.assertEquals("Writing first character.", String.valueOf(toSend[2]), tre.getStringAndCleanBuilder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendText() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendText(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendBinary() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendBinary(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendPartialText() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendText(null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendPartialBinary() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendBinary(null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendObject() throws IOException, EncodeException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendObject(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendTextHandler1() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendText(null, new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                // do nothing.
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendTextHandler2() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendText("We are all here to do what we are all here to do...", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendTextFuture() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendText(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendBinaryHandler1() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendBinary(null, new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                // do nothing.
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendBinaryHandler2() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendBinary(ByteBuffer.wrap("We are all here to do what we are all here to do...".getBytes("UTF-8")), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendBinaryFuture() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendBinary(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendObjectHandler1() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendObject(null, new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                // do nothing.
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendObjectHandler2() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendObject("We are all here to do what we are all here to do...", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendObjectFuture() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(container, tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendObject(null);
    }

    private TyrusSession createTestSession(WebSocketContainer container, TyrusWebSocket webSocket, TyrusEndpointWrapper endpointWrapper) {
        return new TyrusSession(container, webSocket, endpointWrapper, null, null, true, null, null, Collections.<String, String>emptyMap(),
                null, new HashMap<String, List<String>>(), null, null, null, null, null, -1, null, null, null, -1, new DebugContext());
    }

    private BaseContainer createContainer() {
        return new BaseContainer() {
            @Override
            public long getDefaultAsyncSendTimeout() {
                return 0;
            }

            @Override
            public void setAsyncSendTimeout(long timeoutmillis) {

            }

            @Override
            public Session connectToServer(Object annotatedEndpointInstance, URI path) throws DeploymentException, IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Session connectToServer(Class<?> annotatedEndpointClass, URI path) throws DeploymentException, IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Session connectToServer(Endpoint endpointInstance, ClientEndpointConfig cec, URI path) throws DeploymentException, IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Session connectToServer(Class<? extends Endpoint> endpointClass, ClientEndpointConfig cec, URI path) throws DeploymentException, IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getDefaultMaxSessionIdleTimeout() {
                return 0;
            }

            @Override
            public void setDefaultMaxSessionIdleTimeout(long timeout) {

            }

            @Override
            public int getDefaultMaxBinaryMessageBufferSize() {
                return 0;
            }

            @Override
            public void setDefaultMaxBinaryMessageBufferSize(int max) {

            }

            @Override
            public int getDefaultMaxTextMessageBufferSize() {
                return 0;
            }

            @Override
            public void setDefaultMaxTextMessageBufferSize(int max) {

            }

            @Override
            public Set<Extension> getInstalledExtensions() {
                return Collections.emptySet();
            }
        };
    }

    private class TestRemoteEndpoint extends TyrusWebSocket {

        private final ArrayList<Byte> bytesToSend = new ArrayList<Byte>();
        StringBuilder builder = new StringBuilder();
        private int lastSentMessageSize;

        private TestRemoteEndpoint() {
            super(new ProtocolHandler(false, null), null);
        }

        @Override
        public Future<Frame> sendText(String fragment, boolean isLast) {
            builder.append(fragment);
            return new Future<Frame>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Frame get() throws InterruptedException, ExecutionException {
                    return null;
                }

                @Override
                public Frame get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return null;
                }
            };
        }

        @Override
        public Future<Frame> sendBinary(byte[] data, int off, int len, boolean isLast) {
            lastSentMessageSize = len;
            for (int i = off; i < len; i++) {
                bytesToSend.add(data[i]);
            }
            return new Future<Frame>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Frame get() throws InterruptedException, ExecutionException {
                    return null;
                }

                @Override
                public Frame get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return null;
                }
            };
        }

        public byte[] getBytesAndClearBuffer() {
            byte[] result = new byte[bytesToSend.size()];

            for (int i = 0; i < bytesToSend.size(); i++) {
                result[i] = bytesToSend.get(i);
            }

            bytesToSend.clear();
            return result;
        }

        public String getStringAndCleanBuilder() {
            String result = builder.toString();
            builder = new StringBuilder();
            return result;
        }

        private int getLastSentMessageSize() {
            return lastSentMessageSize;
        }
    }

    @ServerEndpoint(value = "/echo")
    private static class EchoEndpoint extends Endpoint {

        @Override
        public void onOpen(final Session session, EndpointConfig config) {
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    final String s = EchoEndpoint.this.doThat(message);
                    if (s != null) {
                        try {
                            session.getBasicRemote().sendText(s);
                        } catch (IOException e) {
                            System.out.println("# error");
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        @OnMessage
        public String doThat(String message) {
            return message;
        }
    }

    private static class TestContainer extends BaseContainer {

        @Override
        public long getDefaultAsyncSendTimeout() {
            return 0;
        }

        @Override
        public void setAsyncSendTimeout(long l) {

        }

        @Override
        public Session connectToServer(Object o, URI uri) throws DeploymentException, IOException {
            return null;
        }

        @Override
        public Session connectToServer(Class<?> aClass, URI uri) throws DeploymentException, IOException {
            return null;
        }

        @Override
        public Session connectToServer(Endpoint endpoint, ClientEndpointConfig clientEndpointConfig, URI uri) throws DeploymentException, IOException {
            return null;
        }

        @Override
        public Session connectToServer(Class<? extends Endpoint> aClass, ClientEndpointConfig clientEndpointConfig, URI uri) throws DeploymentException, IOException {
            return null;
        }

        @Override
        public long getDefaultMaxSessionIdleTimeout() {
            return 0;
        }

        @Override
        public void setDefaultMaxSessionIdleTimeout(long l) {

        }

        @Override
        public int getDefaultMaxBinaryMessageBufferSize() {
            return 0;
        }

        @Override
        public void setDefaultMaxBinaryMessageBufferSize(int i) {

        }

        @Override
        public int getDefaultMaxTextMessageBufferSize() {
            return 0;
        }

        @Override
        public void setDefaultMaxTextMessageBufferSize(int i) {

        }

        @Override
        public Set<Extension> getInstalledExtensions() {
            return Collections.emptySet();
        }

        @Override
        public ScheduledExecutorService getScheduledExecutorService() {
            return null;
        }
    }
}
