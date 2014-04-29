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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.glassfish.tyrus.core.monitoring.EndpointEventListener;
import org.glassfish.tyrus.core.monitoring.MessageEventListener;

/**
 * Listens to endpoint events and collects endpoint-level statistics @see EndpointEventListener.
 * The statistics are collected by aggregating statistics from endpoint sessions.
 * <p/>
 * Creates and registers @EndpointMXBean MXBean that exposes these statistics.
 * Also creates and registers @MessagesStatisticsMXBean MXBean for exposing text, binary and control messages statistics.
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class EndpointJmx implements EndpointEventListener, MessageStatisticsAggregator.AggregatorStatisticsSource {

    //TODO better name?
    private final MonitoredEndpointProperties monitoredEndpointProperties;
    private final String applicationName;
    private final Map<String, SessionJmx> sessions = new ConcurrentHashMap<String, SessionJmx>();

    private final MessageStatisticsAggregator sentMessageStatistics = MessageStatisticsAggregator.createSentMessageStatisticsAggregator(sessions);
    private final MessageStatisticsAggregator sentTextMessageStatistics = MessageStatisticsAggregator.createSentTextMessageStatisticsAggregator(sessions);
    private final MessageStatisticsAggregator sentBinaryMessageStatistics = MessageStatisticsAggregator.createSentBinaryMessageStatisticsAggregator(sessions);
    private final MessageStatisticsAggregator sentControlMessageStatistics = MessageStatisticsAggregator.createSentControlMessageStatisticsAggregator(sessions);

    private final MessageStatisticsAggregator receivedMessageStatistics = MessageStatisticsAggregator.createReceivedMessageStatisticsAggregator(sessions);
    private final MessageStatisticsAggregator receivedTextMessageStatistics = MessageStatisticsAggregator.createReceivedTextMessageStatisticsAggregator(sessions);
    private final MessageStatisticsAggregator receivedBinaryMessageStatistics = MessageStatisticsAggregator.createReceivedBinaryMessageStatisticsAggregator(sessions);
    private final MessageStatisticsAggregator receivedControlMessageStatistics = MessageStatisticsAggregator.createReceivedControlMessageStatisticsAggregator(sessions);

    //TODO enough?
    private volatile int maxOpenSessionsCount = 0;

    public EndpointJmx(String applicationName, String endpointPath, String endpointClassName) {
        this.applicationName = applicationName;
        this.monitoredEndpointProperties = new MonitoredEndpointProperties(endpointPath, endpointClassName);

        EndpointMXBeanImpl endpointMXBean = new EndpointMXBeanImpl(getSentMessageStatistics(), getReceivedMessageStatistics(), endpointPath, endpointClassName, getOpenSessionsCount(), getMaxOpenSessionsCount());
        MessagesStatisticsMXBeanImpl textMessagesMXBean = new MessagesStatisticsMXBeanImpl(getSentTextMessageStatistics(), getReceivedTextMessageStatistics());
        MessagesStatisticsMXBeanImpl binaryMessagesMXBean = new MessagesStatisticsMXBeanImpl(getSentBinaryMessageStatistics(), getReceivedBinaryMessageStatistics());
        MessagesStatisticsMXBeanImpl controlMessagesMXBean = new MessagesStatisticsMXBeanImpl(getSentControlMessageStatistics(), getReceivedControlMessageStatistics());

        MBeanPublisher.registerEndpointMXBeans(applicationName, endpointPath, endpointMXBean, textMessagesMXBean, binaryMessagesMXBean, controlMessagesMXBean);
    }

    @Override
    public MessageEventListener onSessionOpened(String sessionId) {
        SessionJmx sessionJmx = new SessionJmx(applicationName, monitoredEndpointProperties.getEndpointPath(), sessionId);
        sessions.put(sessionId, sessionJmx);


        //TODO atomic integer conditional
        synchronized (sessions) {
            if (sessions.size() > maxOpenSessionsCount) {
                maxOpenSessionsCount = sessions.size();
            }
        }
        return sessionJmx;
    }

    @Override
    public void onSessionClosed(String sessionId) {
        SessionJmx sessionJmx = sessions.remove(sessionId);
        if (sessionJmx != null) {
            sessionJmx.destroy();
        }
    }

    void destroy() {
        MBeanPublisher.unregisterEndpointMXBeans(applicationName, monitoredEndpointProperties.getEndpointPath());
    }

    @Override
    public MessageStatisticsSource getSentTextMessageStatistics() {
        return sentTextMessageStatistics;
    }

    @Override
    public MessageStatisticsSource getSentBinaryMessageStatistics() {
        return sentBinaryMessageStatistics;
    }

    @Override
    public MessageStatisticsSource getSentControlMessageStatistics() {
        return sentControlMessageStatistics;
    }

    @Override
    public MessageStatisticsSource getSentMessageStatistics() {
        return sentMessageStatistics;
    }

    @Override
    public MessageStatisticsSource getReceivedTextMessageStatistics() {
        return receivedTextMessageStatistics;
    }

    @Override
    public MessageStatisticsSource getReceivedBinaryMessageStatistics() {
        return receivedBinaryMessageStatistics;
    }

    @Override
    public MessageStatisticsSource getReceivedControlMessageStatistics() {
        return receivedControlMessageStatistics;
    }

    @Override
    public MessageStatisticsSource getReceivedMessageStatistics() {
        return receivedMessageStatistics;
    }

    MonitoredEndpointProperties getMonitoredEndpointProperties() {
        return monitoredEndpointProperties;
    }

    /**
     * Returns a @Callable that will provide current number of open sessions for this endpoint.
     *
     * @return @Callable returning number of currently open sessions.
     */
    private Callable<Integer> getOpenSessionsCount() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                return sessions.size();
            }
        };
    }

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
}
