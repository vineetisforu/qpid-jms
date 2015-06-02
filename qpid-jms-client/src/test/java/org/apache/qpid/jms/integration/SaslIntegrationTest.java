/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.jms.integration;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.test.QpidJmsTestCase;
import org.apache.qpid.jms.test.testpeer.TestAmqpPeer;
import org.apache.qpid.proton.amqp.Symbol;
import org.junit.Test;

public class SaslIntegrationTest extends QpidJmsTestCase {

    private static final Symbol ANONYMOUS = Symbol.valueOf("ANONYMOUS");
    private static final Symbol PLAIN = Symbol.valueOf("PLAIN");
    private static final Symbol CRAM_MD5 = Symbol.valueOf("CRAM-MD5");

    @Test(timeout = 5000)
    public void testSaslExternalConnection() throws Exception {
        try (TestAmqpPeer testPeer = new TestAmqpPeer();) {

            // Expect an EXTERNAL connection
            testPeer.expectExternalConnect();
            // Each connection creates a session for managing temporary destinations etc
            testPeer.expectBegin(true);

            ConnectionFactory factory = new JmsConnectionFactory("amqp://localhost:" + testPeer.getServerPort());
            Connection connection = factory.createConnection();
            // Set a clientID to provoke the actual AMQP connection process to occur.
            connection.setClientID("clientName");

            testPeer.waitForAllHandlersToComplete(1000);
            assertNull(testPeer.getThrowable());

            testPeer.expectClose();
            connection.close();
        }
    }

    @Test(timeout = 5000)
    public void testSaslPlainConnection() throws Exception {
        try (TestAmqpPeer testPeer = new TestAmqpPeer();) {

            // Expect a PLAIN connection
            String user = "user";
            String pass = "qwerty123456";

            testPeer.expectPlainConnect(user, pass, null, null);
            // Each connection creates a session for managing temporary destinations etc
            testPeer.expectBegin(true);

            ConnectionFactory factory = new JmsConnectionFactory("amqp://localhost:" + testPeer.getServerPort());
            Connection connection = factory.createConnection(user, pass);
            // Set a clientID to provoke the actual AMQP connection process to occur.
            connection.setClientID("clientName");

            testPeer.waitForAllHandlersToComplete(1000);
            assertNull(testPeer.getThrowable());

            testPeer.expectClose();
            connection.close();
        }
    }

    @Test(timeout = 5000)
    public void testSaslAnonymousConnection() throws Exception {
        try (TestAmqpPeer testPeer = new TestAmqpPeer();) {
            // Expect an ANOYMOUS connection
            testPeer.expectAnonymousConnect(true);
            // Each connection creates a session for managing temporary destinations etc
            testPeer.expectBegin(true);

            ConnectionFactory factory = new JmsConnectionFactory("amqp://localhost:" + testPeer.getServerPort());
            Connection connection = factory.createConnection();
            // Set a clientID to provoke the actual AMQP connection process to occur.
            connection.setClientID("clientName");

            testPeer.waitForAllHandlersToComplete(1000);
            assertNull(testPeer.getThrowable());

            testPeer.expectClose();
            connection.close();
        }
    }

    @Test(timeout = 5000)
    public void testAnonymousSelectedWhenNoCredentialsWereSupplied() throws Exception {
        doMechanismSelectedTestImpl(null, null, ANONYMOUS, new Symbol[] {CRAM_MD5, PLAIN, ANONYMOUS});
    }

    @Test(timeout = 5000)
    public void testAnonymousSelectedWhenNoPasswordWasSupplied() throws Exception {
        doMechanismSelectedTestImpl("username", null, ANONYMOUS, new Symbol[] {CRAM_MD5, PLAIN, ANONYMOUS});
    }

    @Test(timeout = 5000)
    public void testCramMd5SelectedWhenCredentialsPresent() throws Exception {
        doMechanismSelectedTestImpl("username", "password", CRAM_MD5, new Symbol[] {CRAM_MD5, PLAIN, ANONYMOUS});
    }

    private void doMechanismSelectedTestImpl(String username, String password, Symbol clientSelectedMech, Symbol[] serverMechs) throws JMSException, InterruptedException, Exception, IOException {
        try (TestAmqpPeer testPeer = new TestAmqpPeer();) {

            testPeer.expectFailingSaslConnect(serverMechs, clientSelectedMech);

            ConnectionFactory factory = new JmsConnectionFactory("amqp://localhost:" + testPeer.getServerPort() + "?jms.clientID=myclientid");
            try {
                factory.createConnection(username, password);
                fail("Excepted exception to be thrown");
            }catch (JMSSecurityException jmsse) {
                // Expected, we deliberately failed the SASL process,
                // we only wanted to verify the correct mechanism
                // was selected, other tests verify the remainder.
            }

            testPeer.waitForAllHandlersToComplete(1000);
        }
    }
}
