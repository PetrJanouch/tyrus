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

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that open sessions statistics (number of currently open sessions and maximal number of open sessions since
 * the beginning of monitoring) are correctly collected.
 *
 * @author Petr Janouch (petr.janouch at oracle.com
 */
public class SessionStatisticsTest extends TestContainer {


    @javax.websocket.server.ServerEndpoint("/jmxSessionStatisticsEndpoint")
    public static class ServerEndpoint {

        public static final CountDownLatch closeCountDownLatch = new CountDownLatch(1);
        public static final CountDownLatch openCountDownLatch = new CountDownLatch(3);

        @OnOpen
        public void onClose(Session session) {
            openCountDownLatch.countDown();
        }

        @OnClose
        public void onClose() {
            closeCountDownLatch.countDown();
        }
    }

    @javax.websocket.ClientEndpoint
    public static class ClientEndpoint {

    }

    @Test
    public void test() {
        ClientManager client = createClient();
        setContextPath("/jmxSessionTestApp");
        getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new ApplicationJmx());
        Server server = null;
        try {
            server = startServer(ServerEndpoint.class);
            client.connectToServer(ClientEndpoint.class, getURI(ServerEndpoint.class));
            Session session2 = client.connectToServer(ClientEndpoint.class, getURI(ServerEndpoint.class));
            client.connectToServer(ClientEndpoint.class, getURI(ServerEndpoint.class));
            assertTrue(ServerEndpoint.openCountDownLatch.await(1, TimeUnit.SECONDS));
            session2.close();
            assertTrue(ServerEndpoint.closeCountDownLatch.await(1, TimeUnit.SECONDS));
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            String fullApplicationMxBeanName = "org.glassfish.tyrus:type=application,appName=/jmxSessionTestApp";
            try {
                ApplicationMXBean applicationBean = JMX.newMXBeanProxy(mBeanServer, new ObjectName(fullApplicationMxBeanName), ApplicationMXBean.class);

                assertEquals(2, applicationBean.getOpenSessionsCount());
                assertEquals(3, applicationBean.getMaxOpenSessionsCount());
            } catch (Exception e) {
                // do nothing false will be returned
                e.printStackTrace();
                fail();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }


}
