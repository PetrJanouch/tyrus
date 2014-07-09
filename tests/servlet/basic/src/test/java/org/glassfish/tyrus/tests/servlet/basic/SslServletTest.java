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
package org.glassfish.tyrus.tests.servlet.basic;

import java.io.IOException;

import javax.websocket.DeploymentException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

/**
 * Secured (using SSL) run of tests in {@link org.glassfish.tyrus.tests.servlet.basic.ServletTestBase}.
 * <p/>
 * The test will be run only if {@code tyrus.test.port.ssl} is set.
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class SslServletTest extends ServletTestBase {

    private String testPort = null;

    /**
     * If the {@code tyrus.test.port.ssl} is set the tests will run and the value of {@code tyrus.test.port.ssl} will
     * replace {@code tyrus.test.port} for the duration of this tests.
     */
    @Before
    public void before() {
        String sslPort = System.getProperty("tyrus.test.port.ssl");
        // if sslPort is not set the test will be skipped
        assumeTrue(sslPort != null);

        if (sslPort != null) {
            testPort = System.getProperty("tyrus.test.port");
            System.setProperty("tyrus.test.port", sslPort);
        }
    }

    @After
    public void after() {
        if (testPort != null) {
            System.setProperty("tyrus.test.port", testPort);
        }
    }

    @Test
    public void testPlainEchoShort() throws DeploymentException, InterruptedException, IOException {
        super.testPlainEchoShort("wss");
    }

    @Test
    public void testPlainEchoShort100() throws DeploymentException, InterruptedException, IOException {
        super.testPlainEchoShort100("wss");
    }

    @Test
    public void testPlainEchoShort10Sequence() throws DeploymentException, InterruptedException, IOException {
        super.testPlainEchoShort10Sequence("wss");
    }

    @Test
    public void testPlainEchoShort10SequenceReturnedSession() throws DeploymentException, InterruptedException, IOException {
        super.testPlainEchoShort10SequenceReturnedSession("wss");
    }

    @Test
    public void testPlainEchoLong() throws DeploymentException, InterruptedException, IOException {
        super.testPlainEchoLong("wss");
    }

    @Test
    public void testPlainEchoLong10() throws DeploymentException, InterruptedException, IOException {
        super.testPlainEchoLong10("wss");
    }

    @Test
    public void testPlainEchoLong10Sequence() throws DeploymentException, InterruptedException, IOException {
        super.testPlainEchoLong10Sequence("wss");
    }

    @Test
    public void testGetRequestURI() throws DeploymentException, InterruptedException, IOException {
        super.testGetRequestURI("wss");
    }

    @Test
    public void testOnOpenClose() throws DeploymentException, InterruptedException, IOException {
        super.testOnOpenClose("wss");
    }

    @Test
    public void testMultiEcho() throws IOException, DeploymentException, InterruptedException {
        super.testMultiEcho("wss");
    }

    @Test
    public void testTyrusBroadcastString() throws IOException, DeploymentException, InterruptedException {
        super.testTyrusBroadcastString("wss");
    }

    @Test
    public void testTyrusBroadcastBinary() throws IOException, DeploymentException, InterruptedException {
        super.testTyrusBroadcastBinary("wss");
    }

    @Test
    public void testWebSocketBroadcast() throws IOException, DeploymentException, InterruptedException {
        super.testWebSocketBroadcast("wss");
    }

    @Test
    public void testTyrusBroadcastStringSharedClientContainer() throws IOException, DeploymentException, InterruptedException {
        super.testTyrusBroadcastStringSharedClientContainer("wss");
    }

    @Test
    public void testTyrusBroadcastBinarySharedClientContainer() throws IOException, DeploymentException, InterruptedException {
        super.testTyrusBroadcastBinarySharedClientContainer("wss");
    }

    @Test
    public void testWebSocketBroadcastSharedClientContainer() throws IOException, DeploymentException, InterruptedException {
        super.testWebSocketBroadcastSharedClientContainer("wss");
    }
}
