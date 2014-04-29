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

import org.glassfish.tyrus.core.frame.BinaryFrame;
import org.glassfish.tyrus.core.frame.Frame;
import org.glassfish.tyrus.core.frame.TextFrame;
import org.glassfish.tyrus.core.monitoring.MessageEventListener;

/**
 * Listens to message events and collects session-level statistics for sent and received messages.
 * Creates and registers @MessagesStatisticsMXBean MXBean for text, binary control and all messages which expose these statistics.
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 * @see org.glassfish.tyrus.core.monitoring.MessageEventListener
 */
class SessionJmx implements MessageEventListener, MessageStatisticsAggregator.AggregatorStatisticsSource {

    private final MessageStatistics sentTextMessageStatistics = new MessageStatistics();
    private final MessageStatistics sentBinaryMessageStatistics = new MessageStatistics();
    private final MessageStatistics sentControlMessageStatistics = new MessageStatistics();
    private final AggregatedMessageStatistics sentMessageStatistics = new AggregatedMessageStatistics(sentTextMessageStatistics, sentBinaryMessageStatistics, sentControlMessageStatistics);

    private final MessageStatistics receivedTextMessageStatistics = new MessageStatistics();
    private final MessageStatistics receivedBinaryMessageStatistics = new MessageStatistics();
    private final MessageStatistics receivedControlMessageStatistics = new MessageStatistics();
    private final AggregatedMessageStatistics receivedMessageStatistics = new AggregatedMessageStatistics(receivedTextMessageStatistics, receivedBinaryMessageStatistics, receivedControlMessageStatistics);

    private final String applicationName;
    private final String endpointPath;
    private final String sessionId;

    SessionJmx(String applicationName, String endpointPath, String sessionId) {
        this.applicationName = applicationName;
        this.endpointPath = endpointPath;
        this.sessionId = sessionId;
        MessagesStatisticsMXBean sessionMXBean = new MessagesStatisticsMXBeanImpl(sentMessageStatistics, receivedMessageStatistics);
        MessagesStatisticsMXBean textMessagesMXBean = new MessagesStatisticsMXBeanImpl(sentTextMessageStatistics, receivedTextMessageStatistics);
        MessagesStatisticsMXBean binaryMessagesMXBean = new MessagesStatisticsMXBeanImpl(sentBinaryMessageStatistics, receivedBinaryMessageStatistics);
        MessagesStatisticsMXBean controlMessagesMXBean = new MessagesStatisticsMXBeanImpl(sentControlMessageStatistics, receivedControlMessageStatistics);

        MBeanPublisher.registerSessionMXBeans(applicationName, endpointPath, sessionId, sessionMXBean, textMessagesMXBean, binaryMessagesMXBean, controlMessagesMXBean);
    }

    void destroy() {
        MBeanPublisher.unregisterSessionMXBeans(applicationName, endpointPath, sessionId);
    }

    @Override
    public void onMessageSent(Frame frame) {
        if (frame instanceof TextFrame) {
            sentTextMessageStatistics.onMessage(frame.getPayloadLength());
        } else if (frame instanceof BinaryFrame) {
            sentBinaryMessageStatistics.onMessage(frame.getPayloadLength());
        } else {
            sentControlMessageStatistics.onMessage(frame.getPayloadLength());
        }
    }

    @Override
    public void onMessageReceived(Frame frame) {
        if (frame instanceof TextFrame) {
            receivedTextMessageStatistics.onMessage(frame.getPayloadLength());
        } else if (frame instanceof BinaryFrame) {
            receivedBinaryMessageStatistics.onMessage(frame.getPayloadLength());
        } else {
            receivedControlMessageStatistics.onMessage(frame.getPayloadLength());
        }
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

    private class MessageStatistics implements MessageStatisticsSource {

        //TODO comment synchronization
        private volatile long messagesCount = 0;
        private volatile long messagesSize = 0;
        private volatile long minimalMessageSize = Long.MAX_VALUE;
        private volatile long maximalMessageSize = 0;

        //TODO on frame
        void onMessage(long size) {
            messagesCount++;
            messagesSize += size;
            if (minimalMessageSize > size) {
                minimalMessageSize = size;
            }
            if (maximalMessageSize < size) {
                maximalMessageSize = size;
            }
        }

        @Override
        public long getMessagesCount() {
            return messagesCount;
        }

        @Override
        public long getMessagesSize() {
            return messagesSize;
        }

        @Override
        public long getMinimalMessageSize() {
            if (minimalMessageSize == Long.MAX_VALUE) {
                return 0;
            }
            return minimalMessageSize;
        }

        @Override
        public long getMaximalMessageSize() {
            return maximalMessageSize;
        }
    }

    /**
     * Creates total message statistics by aggregating statistics about text, binary and control messages.
     */
    private class AggregatedMessageStatistics implements MessageStatisticsSource {

        private final MessageStatistics textMessageStatistics;
        private final MessageStatistics binaryMessageStatistics;
        private final MessageStatistics controlMessageStatistics;

        private AggregatedMessageStatistics(MessageStatistics textMessageStatistics,
                                            MessageStatistics binaryMessageStatistics,
                                            MessageStatistics controlMessageStatistics) {
            this.textMessageStatistics = textMessageStatistics;
            this.binaryMessageStatistics = binaryMessageStatistics;
            this.controlMessageStatistics = controlMessageStatistics;
        }

        @Override
        public long getMessagesCount() {
            return textMessageStatistics.getMessagesCount()
                    + binaryMessageStatistics.getMessagesCount()
                    + controlMessageStatistics.getMessagesCount();
        }

        @Override
        public long getMessagesSize() {
            return textMessageStatistics.getMessagesSize()
                    + binaryMessageStatistics.getMessagesSize()
                    + controlMessageStatistics.getMessagesSize();
        }

        @Override
        public long getMinimalMessageSize() {
            return Math.min(textMessageStatistics.getMinimalMessageSize(),
                    Math.min(binaryMessageStatistics.getMinimalMessageSize(), controlMessageStatistics.getMinimalMessageSize()));
        }

        @Override
        public long getMaximalMessageSize() {
            return Math.max(textMessageStatistics.getMaximalMessageSize(),
                    Math.max(binaryMessageStatistics.getMaximalMessageSize(), controlMessageStatistics.getMaximalMessageSize()));
        }
    }
}
