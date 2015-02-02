/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.qpid.jms.failover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.support.AmqpTestSupport;
import org.apache.qpid.jms.support.Wait;
import org.junit.Test;

/**
 * Basic tests for the FailoverProvider implementation
 */
public class JmsFailoverTest extends AmqpTestSupport {

    @Override
    protected boolean isPersistent() {
        return true;
    }

    @Test(timeout=60000)
    public void testFailoverConnects() throws Exception {
        URI brokerURI = new URI(getAmqpFailoverURI());
        Connection connection = createAmqpConnection(brokerURI);
        connection.start();
        connection.close();
    }

    @Test(timeout=60000)
    public void testFailoverConnectsWithMultipleURIs() throws Exception {
        URI brokerURI = new URI("failover://(amqp://127.0.0.1:61616,amqp://localhost:5777," +
                                getBrokerAmqpConnectionURI() + ")?failover.maxReconnectDelay=500");
        Connection connection = createAmqpConnection(brokerURI);
        connection.start();
        connection.close();
    }

    @Test(timeout=60000)
    public void testStartupReconnectAttempts() throws Exception {
        URI brokerURI = new URI("failover://(amqp://localhost:61616)" +
                                "?failover.maxReconnectDelay=50&failover.startupMaxReconnectAttempts=5");
        JmsConnectionFactory factory = new JmsConnectionFactory(brokerURI);
        Connection connection = factory.createConnection();
        try {
            connection.start();
            fail("Should have thrown an exception of type JMSException");
        } catch (JMSException jmsEx) {
        } catch (Exception unexpected) {
            fail("Should have thrown a JMSException but threw: " + unexpected.getClass().getSimpleName());
        } finally {
            connection.close();
        }
    }

    @Test(timeout=60000)
    public void testStartupReconnectAttemptsMultipleHosts() throws Exception {
        URI brokerURI = new URI("failover://(amqp://localhost:61616,amqp://localhost:61617)" +
                                "?failover.maxReconnectDelay=100&failover.startupMaxReconnectAttempts=5");
        JmsConnectionFactory factory = new JmsConnectionFactory(brokerURI);
        Connection connection = factory.createConnection();
        try {
            connection.start();
            fail("Should have thrown an exception of type JMSException");
        } catch (JMSException jmsEx) {
        } catch (Exception unexpected) {
            fail("Should have thrown a JMSException but threw: " + unexpected.getClass().getSimpleName());
        } finally {
            connection.close();
        }
    }

    @Test(timeout=60000)
    public void testStartFailureWithAsyncExceptionListener() throws Exception {
        URI brokerURI = new URI(getAmqpFailoverURI() +
            "?failover.maxReconnectDelay=100&failover.maxReconnectAttempts=5");

        final CountDownLatch failed = new CountDownLatch(1);
        JmsConnectionFactory factory = new JmsConnectionFactory(brokerURI);
        factory.setExceptionListener(new ExceptionListener() {

            @Override
            public void onException(JMSException exception) {
                LOG.info("Connection got exception: {}", exception.getMessage());
                failed.countDown();
            }
        });
        connection = factory.createConnection();
        connection.start();

        stopPrimaryBroker();

        assertTrue("No async exception", failed.await(15, TimeUnit.SECONDS));
    }

    @SuppressWarnings("unused")
    @Test(timeout=60000)
    public void testBasicStateRestoration() throws Exception {
        URI brokerURI = new URI(getAmqpFailoverURI() + "?failover.maxReconnectDelay=1000");

        connection = createAmqpConnection(brokerURI);
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(name.getMethodName());
        MessageProducer producer = session.createProducer(queue);
        MessageConsumer consumer = session.createConsumer(queue);

        assertEquals(1, brokerService.getAdminView().getQueueSubscribers().length);
        assertEquals(1, brokerService.getAdminView().getQueueProducers().length);

        restartPrimaryBroker();

        assertTrue("Should have a new connection.", Wait.waitFor(new Wait.Condition() {

            @Override
            public boolean isSatisified() throws Exception {
                return brokerService.getAdminView().getCurrentConnectionsCount() == 1;
            }
        }));

        assertEquals(1, brokerService.getAdminView().getQueueSubscribers().length);
        assertEquals(1, brokerService.getAdminView().getQueueProducers().length);
    }

    @SuppressWarnings("unused")
    @Test(timeout=60000)
    public void testDurableSubscriberRestores() throws Exception {
        URI brokerURI = new URI(getAmqpFailoverURI() + "?failover.maxReconnectDelay=200");

        connection = createAmqpConnection(brokerURI);
        connection.setClientID(name.getMethodName());
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(name.getMethodName());
        MessageConsumer consumer = session.createDurableSubscriber(topic, name.getMethodName());

        assertEquals(1, brokerService.getAdminView().getDurableTopicSubscribers().length);

        restartPrimaryBroker();

        assertTrue("Should have a new connection.", Wait.waitFor(new Wait.Condition() {

            @Override
            public boolean isSatisified() throws Exception {
                return brokerService.getAdminView().getCurrentConnectionsCount() == 1;
            }
        }));

        assertTrue("Should have no inactive subscribers.", Wait.waitFor(new Wait.Condition() {

            @Override
            public boolean isSatisified() throws Exception {
                return brokerService.getAdminView().getInactiveDurableTopicSubscribers().length == 0;
            }
        }));

        assertTrue("Should have one durable sub.", Wait.waitFor(new Wait.Condition() {

            @Override
            public boolean isSatisified() throws Exception {
                return brokerService.getAdminView().getDurableTopicSubscribers().length == 1;
            }
        }));
    }

    @Test(timeout=90000)
    public void testBadFirstURIConnectsAndProducerWorks() throws Exception {
        URI brokerURI = new URI("failover://(amqp://localhost:61616," +
            getBrokerAmqpConnectionURI() + ")?failover.maxReconnectDelay=100");

        connection = createAmqpConnection(brokerURI);
        connection.start();

        final int MSG_COUNT = 10;
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(name.getMethodName());
        final MessageProducer producer = session.createProducer(queue);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        final CountDownLatch failed = new CountDownLatch(1);

        assertEquals(1, brokerService.getAdminView().getQueueProducers().length);

        for (int i = 0; i < MSG_COUNT; ++i) {
            producer.send(session.createTextMessage("Message: " + i));
        }

        final QueueViewMBean proxy = getProxyToQueue(name.getMethodName());

        assertTrue("Should have all messages sent.", Wait.waitFor(new Wait.Condition() {

            @Override
            public boolean isSatisified() throws Exception {
                return proxy.getQueueSize() == MSG_COUNT;
            }
        }));

        assertFalse(failed.getCount() == 0);
    }

    @Test(timeout=90000)
    public void testNonTxProducerRecoversAfterFailover() throws Exception {
        URI brokerURI = new URI("failover://("+ getBrokerAmqpConnectionURI() +")?failover.maxReconnectDelay=1000");

        connection = createAmqpConnection(brokerURI);
        connection.start();

        final int MSG_COUNT = 20;
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(name.getMethodName());
        final MessageProducer producer = session.createProducer(queue);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        final CountDownLatch failed = new CountDownLatch(1);
        final CountDownLatch sentSome = new CountDownLatch(3);

        assertEquals(1, brokerService.getAdminView().getQueueProducers().length);

        Thread producerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    for (int i = 0; i < MSG_COUNT; ++i) {
                        LOG.debug("Producer sening message #{}", i + 1);
                        producer.send(session.createTextMessage("Message: " + i));
                        sentSome.countDown();
                        TimeUnit.MILLISECONDS.sleep(50);
                    }
                } catch (Exception e) {
                    failed.countDown();
                }
            }
        });
        producerThread.start();

        // Wait until a couple messages get sent on first broker run.
        assertTrue(sentSome.await(3, TimeUnit.SECONDS));
        stopPrimaryBroker();
        TimeUnit.SECONDS.sleep(3);  // Gives FailoverProvider some CPU time
        restartPrimaryBroker();

        assertTrue("Should have a new connection.", Wait.waitFor(new Wait.Condition() {

            @Override
            public boolean isSatisified() throws Exception {
                return brokerService.getAdminView().getCurrentConnectionsCount() == 1;
            }
        }));

        assertTrue("Should have a recovered producer.", Wait.waitFor(new Wait.Condition() {

            @Override
            public boolean isSatisified() throws Exception {
                return brokerService.getAdminView().getQueueProducers().length == 1;
            }
        }));

        final QueueViewMBean proxy = getProxyToQueue(name.getMethodName());

        assertTrue("Should have all messages sent.", Wait.waitFor(new Wait.Condition() {

            @Override
            public boolean isSatisified() throws Exception {
                return proxy.getQueueSize() == MSG_COUNT;
            }
        }));

        assertFalse(failed.getCount() == 0);
        connection.close();
    }
}
