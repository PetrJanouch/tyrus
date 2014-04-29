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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.core.monitoring.EndpointEventListener;
import org.glassfish.tyrus.core.monitoring.MessageEventListener;
import static org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsAggregator.createReceivedBinaryMessageStatisticsAggregator;
import static org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsAggregator.createReceivedControlMessageStatisticsAggregator;
import static org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsAggregator.createReceivedMessageStatisticsAggregator;
import static org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsAggregator.createReceivedTextMessageStatisticsAggregator;
import static org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsAggregator.createSentBinaryMessageStatisticsAggregator;
import static org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsAggregator.createSentControlMessageStatisticsAggregator;
import static org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsAggregator.createSentMessageStatisticsAggregator;
import static org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsAggregator.createSentTextMessageStatisticsAggregator;

/**
 * Listens to application events and collects application-level statistics @see ApplicationEventListener.
 * The statistics are collected by aggregating statistics from application endpoints.
 * <p/>
 * Creates and registers @ApplicationMXBeanImpl MXBean that exposes these application statistics.
 * Also creates and registers @MessagesStatisticsMXBean MXBean for exposing text, binary and control messages statistics.
 * <p/>
 * For monitoring in Grizzly server an instance should be passed to the server in server properties.
 * <p/>
 * <pre>
 *     serverProperties.put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new ApplicationJmx());
 * </pre>
 * <p/>
 * For use in servlet container the class name should be passed as context parameter in web.xml.
 * <p/>
 * <pre>
 *      {@code
 *
 *      <context-param>
 *         <param-name>org.glassfish.tyrus.core.monitoring.ApplicationEventListener</param-name>
 *         <param-value>org.glassfish.tyrus.ext.monitoring.jmx.ApplicationJmx</param-value>
 *      </context-param>
 *      }
 *  <pre/>
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class ApplicationJmx implements ApplicationEventListener {

    private final Map<String, EndpointJmx> endpoints = new ConcurrentHashMap<String, EndpointJmx>();
    private volatile String applicationName;
    private int openSessionsCount = 0;
    private int maxOpenSessionCount = 0;

    @Override
    public void onApplicationInitialized(String applicationName) {
        this.applicationName = applicationName;

        ApplicationMXBeanImpl applicationMXBean = new ApplicationMXBeanImpl(createSentMessageStatisticsAggregator(endpoints), createReceivedMessageStatisticsAggregator(endpoints), getEndpoints(), getEndpointPaths(), getOpenSessionsCount(), getMaxOpenSessionsCount());
        MessagesStatisticsMXBeanImpl textMessagesMXBean = new MessagesStatisticsMXBeanImpl(createSentTextMessageStatisticsAggregator(endpoints), createReceivedTextMessageStatisticsAggregator(endpoints));
        MessagesStatisticsMXBeanImpl controlMessagesMXBean = new MessagesStatisticsMXBeanImpl(createSentControlMessageStatisticsAggregator(endpoints), createReceivedControlMessageStatisticsAggregator(endpoints));
        MessagesStatisticsMXBeanImpl binaryMessagesMXBean = new MessagesStatisticsMXBeanImpl(createSentBinaryMessageStatisticsAggregator(endpoints), createReceivedBinaryMessageStatisticsAggregator(endpoints));

        MBeanPublisher.registerApplicationMXBeans(applicationName, applicationMXBean, textMessagesMXBean, binaryMessagesMXBean, controlMessagesMXBean);
    }

    @Override
    public void onApplicationDestroyed() {
        MBeanPublisher.unregisterApplicationMXBeans(applicationName);
    }

    @Override
    public EndpointEventListener onEndpointRegistered(String endpointPath, Class<?> endpointClass) {
        EndpointJmx endpoint = new EndpointJmx(applicationName, endpointPath, endpointClass.getName());
        endpoints.put(endpointPath, endpoint);
        return createEndpointEventListener(endpoint);
    }

    @Override
    public void onEndpointUnregistered(String endpointPath) {
        EndpointJmx endpoint = endpoints.remove(endpointPath);
        if (endpoint != null) {
            endpoint.destroy();
        }
    }

    /**
     * Returns a @Callable that will provide list of endpoint paths and endpoint class names for currently registered endpoints.
     *
     * @return @Callable returning list of endpoint paths and class names.
     */
    private Callable<List<MonitoredEndpointProperties>> getEndpoints() {
        return new Callable<List<MonitoredEndpointProperties>>() {
            @Override
            public List<MonitoredEndpointProperties> call() {
                List<MonitoredEndpointProperties> result = new ArrayList<MonitoredEndpointProperties>(endpoints.size());
                for (EndpointJmx endpoint : endpoints.values()) {
                    result.add(endpoint.getMonitoredEndpointProperties());
                }
                return result;
            }
        };
    }

    /**
     * Returns a @Callable that will provide set of endpoint paths for currently registered endpoints.
     *
     * @return @Callable returning set of endpoint paths.
     */
    private Callable<List<String>> getEndpointPaths() {
        return new Callable<List<String>>() {
            @Override
            public List<String> call() {
                return new ArrayList<String>(endpoints.keySet());
            }
        };

    }

    /**
     * Returns a @Callable that will provide number of currently open sessions.
     *
     * @return @Callable returning a current number of open sessions.
     */
    private Callable<Integer> getOpenSessionsCount() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                return openSessionsCount;
            }
        };
    }

    /**
     * Returns a @Callable that will provide a maximal number of open sessions since the start of monitoring.
     *
     * @return @Callable returning a maximal number of open sessions since the start of monitoring.
     */
    private Callable<Integer> getMaxOpenSessionsCount() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                return maxOpenSessionCount;
            }
        };
    }

    private EndpointEventListener createEndpointEventListener(final EndpointJmx endpointJmx) {
        return new EndpointEventListener() {
            @Override
            public MessageEventListener onSessionOpened(String sessionId) {
                synchronized (ApplicationJmx.this) {
                    openSessionsCount++;
                    if (openSessionsCount > maxOpenSessionCount) {
                        maxOpenSessionCount = openSessionsCount;
                    }
                }
                return endpointJmx.onSessionOpened(sessionId);
            }

            @Override
            public void onSessionClosed(String sessionId) {
                synchronized (ApplicationJmx.this) {
                    openSessionsCount--;
                }
                endpointJmx.onSessionClosed(sessionId);
            }
        };
    }

}
