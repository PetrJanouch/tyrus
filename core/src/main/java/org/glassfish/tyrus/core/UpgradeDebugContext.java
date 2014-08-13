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

package org.glassfish.tyrus.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link java.util.logging.Logger} wrapper that temporarily stores log records and postpones their logging
 * until they can be provided with a common ID - either a session ID or randomly generated ID if a session has not been
 * created.
 * <p/>
 * Log records are provided with a common ID, so that log records from a single upgrade request can be easily linked
 * together in a log of a busy server or client.
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class UpgradeDebugContext {

    private static final Logger LOGGER = Logger.getLogger(UpgradeDebugContext.class.getName());
    private static final AtomicLong contextIdGenerator = new AtomicLong(0);

    private final List<LogRecord> logRecords = new ArrayList<LogRecord>();
    private String sessionId = null;

    /**
     * Check if a message of the given level would actually be logged.
     *
     * @param level message level.
     * @return return {@code true} if the given level is currently being logged.
     */
    public boolean isLoggable(Level level) {
        return LOGGER.isLoggable(level);
    }

    /**
     * Append a message to the log, the logging will be postponed until the message can be provided with an ID -
     * either a session ID or randomly generated ID if a session has not been created.
     *
     * @param level   message level.
     * @param message message to be logged.
     */
    public void appendMessage(Level level, String message) {
        appendMessage(level, message, null);
    }

    /**
     * Append a message to the log, the logging will be postponed until the message can be provided with an ID -
     * either a session ID or randomly generated ID if a session has not been created.
     *
     * @param level   message level.
     * @param message message to be logged.
     * @param t       throwable to be logged.
     */
    public void appendMessage(Level level, String message, Throwable t) {
        if (LOGGER.isLoggable(level)) {
            logRecords.add(new LogRecord(level, message, t));
        }
    }

    /**
     * Set a session ID that will be used as a common identifier for logged messages related to the same upgrade request.
     *
     * @param sessionId session ID.
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Write stored messages to the log.
     */
    public void writeToLog() {
        String prefix;
        if (sessionId != null) {
            prefix = "Session " + sessionId + ": ";
        } else {
            // something went wrong before the session could have been initialized, just give all the messages some id.
            prefix = "Debug context id " + String.valueOf(contextIdGenerator.incrementAndGet()) + ": ";
        }

        for (LogRecord logRecord : logRecords) {
            if (logRecord.t != null) {
                LOGGER.log(logRecord.level, prefix + logRecord.message, logRecord.t);
            } else {
                LOGGER.log(logRecord.level, prefix + logRecord.message);
            }
        }
    }

    private static class LogRecord {
        private Level level;
        private String message;
        private Throwable t;

        LogRecord(Level level, String message, Throwable t) {
            this.level = level;
            this.message = message;
            this.t = t;
        }
    }
}
