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

import org.glassfish.tyrus.core.monitoring.EndpointEventListener;

/**
 * Listens to endpoint events and collects endpoint-level statistics @see EndpointEventListener.
 * <p/>
 * Creates and registers @EndpointMXBean MXBean that exposes these statistics.
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
abstract class EndpointJmx implements EndpointEventListener, MessageListener {

    protected final EndpointClassNamePathPair endpointClassNamePathPair;
    protected final String applicationName;
    protected final Object maxOpenSessionsCountLock = new Object();
    protected final ApplicationJmx applicationJmx;

    private final ConcurrentMessageStatistics sentTextMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics sentBinaryMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics sentControlMessageStatistics = new ConcurrentMessageStatistics();

    private final ConcurrentMessageStatistics receivedTextMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics receivedBinaryMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics receivedControlMessageStatistics = new ConcurrentMessageStatistics();

    protected volatile int maxOpenSessionsCount = 0;

    EndpointJmx(ApplicationJmx applicationJmx, String applicationName, String endpointPath, String endpointClassName) {
        this.applicationName = applicationName;
        this.endpointClassNamePathPair = new EndpointClassNamePathPair(endpointPath, endpointClassName);
        this.applicationJmx = applicationJmx;

        EndpointMXBeanImpl endpointMXBean = new EndpointMXBeanImpl(new AggregatedMessageStatistics(sentTextMessageStatistics, sentBinaryMessageStatistics, sentControlMessageStatistics),
                new AggregatedMessageStatistics(receivedTextMessageStatistics, receivedBinaryMessageStatistics, receivedControlMessageStatistics), endpointPath, endpointClassName, getOpenSessionsCount(), getMaxOpenSessionsCount());
        MessageStatisticsMXBeanImpl textMessagesMXBean = new MessageStatisticsMXBeanImpl(sentTextMessageStatistics, receivedTextMessageStatistics);
        MessageStatisticsMXBeanImpl binaryMessagesMXBean = new MessageStatisticsMXBeanImpl(sentBinaryMessageStatistics, receivedBinaryMessageStatistics);
        MessageStatisticsMXBeanImpl controlMessagesMXBean = new MessageStatisticsMXBeanImpl(sentControlMessageStatistics, receivedControlMessageStatistics);

        MBeanPublisher.registerEndpointMXBeans(applicationName, endpointPath, endpointMXBean, textMessagesMXBean, binaryMessagesMXBean, controlMessagesMXBean);
    }


    void unregister() {
        MBeanPublisher.unregisterEndpointMXBeans(applicationName, endpointClassNamePathPair.getEndpointPath());
    }

    EndpointClassNamePathPair getEndpointClassNamePathPair() {
        return endpointClassNamePathPair;
    }

    /**
     * Returns a @Callable that will provide current number of open sessions for this endpoint.
     *
     * @return @Callable returning number of currently open sessions.
     */
    abstract protected Callable<Integer> getOpenSessionsCount();

    /**
     * Returns a @Callable that will provide maximal number of open sessions for this endpoint since the start of monitoring.
     *
     * @return @Callable returning a maximal number of open sessions since the start of monitoring.
     */
    private Callable<Integer> getMaxOpenSessionsCount() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                return maxOpenSessionsCount;
            }
        };
    }

    @Override
    public void onTextMessageSent(long length) {
        sentTextMessageStatistics.onMessage(length);
        applicationJmx.onTextMessageSent(length);
    }

    @Override
    public void onBinaryMessageSent(long length) {
        sentBinaryMessageStatistics.onMessage(length);
        applicationJmx.onBinaryMessageSent(length);
    }

    @Override
    public void onControlMessageSent(long length) {
        sentControlMessageStatistics.onMessage(length);
        applicationJmx.onControlMessageSent(length);
    }

    @Override
    public void onTextMessageReceived(long length) {
        receivedTextMessageStatistics.onMessage(length);
        applicationJmx.onTextMessageReceived(length);
    }

    @Override
    public void onBinaryMessageReceived(long length) {
        receivedBinaryMessageStatistics.onMessage(length);
        applicationJmx.onBinaryMessageReceived(length);
    }

    @Override
    public void onControlMessageReceived(long length) {
        receivedControlMessageStatistics.onMessage(length);
        applicationJmx.onControlMessageReceived(length);
    }
}
