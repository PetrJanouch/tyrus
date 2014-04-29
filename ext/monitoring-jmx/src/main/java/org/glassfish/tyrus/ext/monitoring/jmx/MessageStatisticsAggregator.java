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

/**
 * Aggregates message statistics from the child nodes.
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class MessageStatisticsAggregator<T extends MessageStatisticsAggregator.AggregatorStatisticsSource> implements MessageStatisticsSource {

    private final Map<String, T> providers;
    private final StatisticsSourceSelector selector;

    private MessageStatisticsAggregator(Map<String, T> providers, StatisticsSourceSelector selector) {
        this.providers = providers;
        this.selector = selector;
    }

    @Override
    public long getMessagesCount() {
        long result = 0;
        for (AggregatorStatisticsSource provider : providers.values()) {
            result += selector.getMessageStatisticsSource(provider).getMessagesCount();
        }
        return result;
    }

    @Override
    public long getMessagesSize() {
        long result = 0;
        for (AggregatorStatisticsSource provider : providers.values()) {
            result += selector.getMessageStatisticsSource(provider).getMessagesSize();
        }
        return result;
    }

    @Override
    public long getMinimalMessageSize() {
        long result = Integer.MAX_VALUE;
        for (AggregatorStatisticsSource provider : providers.values()) {
            result = Math.min(result, selector.getMessageStatisticsSource(provider).getMinimalMessageSize());
        }
        if (result == Integer.MAX_VALUE) {
            return 0;
        }
        return result;
    }

    @Override
    public long getMaximalMessageSize() {
        long result = 0;
        for (AggregatorStatisticsSource provider : providers.values()) {
            result = Math.max(result, selector.getMessageStatisticsSource(provider).getMaximalMessageSize());
        }
        return result;
    }

    static <T extends AggregatorStatisticsSource> MessageStatisticsAggregator createSentTextMessageStatisticsAggregator(Map<String, T> statisticsProviders) {
        return new MessageStatisticsAggregator<T>(statisticsProviders, new StatisticsSourceSelector() {

            @Override
            public MessageStatisticsSource getMessageStatisticsSource(AggregatorStatisticsSource statisticsProvider) {
                return statisticsProvider.getSentTextMessageStatistics();
            }
        });
    }

    static <T extends AggregatorStatisticsSource> MessageStatisticsAggregator createSentBinaryMessageStatisticsAggregator(Map<String, T> statisticsProviders) {
        return new MessageStatisticsAggregator<T>(statisticsProviders, new StatisticsSourceSelector() {

            @Override
            public MessageStatisticsSource getMessageStatisticsSource(AggregatorStatisticsSource statisticsProvider) {
                return statisticsProvider.getSentBinaryMessageStatistics();
            }
        });
    }

    static <T extends AggregatorStatisticsSource> MessageStatisticsAggregator createSentControlMessageStatisticsAggregator(Map<String, T> statisticsProviders) {
        return new MessageStatisticsAggregator<T>(statisticsProviders, new StatisticsSourceSelector() {

            @Override
            public MessageStatisticsSource getMessageStatisticsSource(AggregatorStatisticsSource statisticsProvider) {
                return statisticsProvider.getSentControlMessageStatistics();
            }
        });
    }

    static <T extends AggregatorStatisticsSource> MessageStatisticsAggregator createSentMessageStatisticsAggregator(Map<String, T> statisticsProviders) {
        return new MessageStatisticsAggregator<T>(statisticsProviders, new StatisticsSourceSelector() {

            @Override
            public MessageStatisticsSource getMessageStatisticsSource(AggregatorStatisticsSource statisticsProvider) {
                return statisticsProvider.getSentMessageStatistics();
            }
        });
    }

    static <T extends AggregatorStatisticsSource> MessageStatisticsAggregator createReceivedTextMessageStatisticsAggregator(Map<String, T> statisticsProviders) {
        return new MessageStatisticsAggregator<T>(statisticsProviders, new StatisticsSourceSelector() {

            @Override
            public MessageStatisticsSource getMessageStatisticsSource(AggregatorStatisticsSource statisticsProvider) {
                return statisticsProvider.getReceivedTextMessageStatistics();
            }
        });
    }

    static <T extends AggregatorStatisticsSource> MessageStatisticsAggregator createReceivedBinaryMessageStatisticsAggregator(Map<String, T> statisticsProviders) {
        return new MessageStatisticsAggregator<T>(statisticsProviders, new StatisticsSourceSelector() {

            @Override
            public MessageStatisticsSource getMessageStatisticsSource(AggregatorStatisticsSource statisticsProvider) {
                return statisticsProvider.getReceivedBinaryMessageStatistics();
            }
        });
    }

    static <T extends AggregatorStatisticsSource> MessageStatisticsAggregator createReceivedControlMessageStatisticsAggregator(Map<String, T> statisticsProviders) {
        return new MessageStatisticsAggregator<T>(statisticsProviders, new StatisticsSourceSelector() {

            @Override
            public MessageStatisticsSource getMessageStatisticsSource(AggregatorStatisticsSource statisticsProvider) {
                return statisticsProvider.getReceivedControlMessageStatistics();
            }
        });
    }

    static <T extends AggregatorStatisticsSource> MessageStatisticsAggregator createReceivedMessageStatisticsAggregator(Map<String, T> statisticsProviders) {
        return new MessageStatisticsAggregator<T>(statisticsProviders, new StatisticsSourceSelector() {

            @Override
            public MessageStatisticsSource getMessageStatisticsSource(AggregatorStatisticsSource statisticsProvider) {
                return statisticsProvider.getReceivedMessageStatistics();
            }
        });
    }


    private interface StatisticsSourceSelector {

        MessageStatisticsSource getMessageStatisticsSource(AggregatorStatisticsSource statisticsProvider);
    }

    /**
     * Interface that allows both {@link org.glassfish.tyrus.ext.monitoring.jmx.EndpointJmx} and {@link org.glassfish.tyrus.ext.monitoring.jmx.SessionJmx} as a source
     * of message statistics {@link org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsAggregator}.
     *
     * @author Petr Janouch (petr.janouch at oracle.com)
     */
    interface AggregatorStatisticsSource {

        MessageStatisticsSource getSentTextMessageStatistics();

        MessageStatisticsSource getSentBinaryMessageStatistics();

        MessageStatisticsSource getSentControlMessageStatistics();

        MessageStatisticsSource getSentMessageStatistics();

        MessageStatisticsSource getReceivedTextMessageStatistics();

        MessageStatisticsSource getReceivedBinaryMessageStatistics();

        MessageStatisticsSource getReceivedControlMessageStatistics();

        MessageStatisticsSource getReceivedMessageStatistics();

    }

}
