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

import org.glassfish.tyrus.core.Beta;

/**
 * MXBean used for exposing message-level statistics.
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
@Beta
public interface MessageStatisticsMXBean {

    /**
     * Returns the total number of messages sent since the start of monitoring.
     *
     * @return the total number of messages sent since the start of monitoring.
     */
    public long getSentMessagesCount();

    /**
     * Returns the size of the smallest message sent since the start of monitoring.
     *
     * @return the size of the smallest message sent since the start of monitoring.
     */
    public long getMinimalSentMessageSize();

    /**
     * Returns the size of the largest message sent since the start of monitoring.
     *
     * @return the size of the largest message sent since the start of monitoring.
     */
    public long getMaximalSentMessageSize();

    /**
     * Returns the average size of all the messages sent since the start of monitoring.
     *
     * @return the average size of all the message sent since the start of monitoring.
     */
    public long getAverageSentMessageSize();

    /**
     * Returns the average number of sent messages per second.
     *
     * @return the average number of sent messages per second.
     */
    public long getSentMessagesCountPerSecond();

    /**
     * Returns the total number of messages received since the start of monitoring.
     *
     * @return the total number of messages received since the start of monitoring.
     */
    public long getReceivedMessagesCount();

    /**
     * Returns the size of the smallest message received since the start of monitoring.
     *
     * @return the size of the smallest message received since the start of monitoring.
     */
    public long getMinimalReceivedMessageSize();

    /**
     * Returns the size of the largest message received since the start of monitoring.
     *
     * @return the size of the largest message received since the start of monitoring.
     */
    public long getMaximalReceivedMessageSize();

    /**
     * Returns the average size of all the messages received since the start of monitoring.
     *
     * @return the average size of all the message received since the start of monitoring.
     */
    public long getAverageReceivedMessageSize();

    /**
     * Returns the average number of received messages per second.
     *
     * @return the average number of received messages per second.
     */
    public long getReceivedMessagesCountPerSecond();
}
