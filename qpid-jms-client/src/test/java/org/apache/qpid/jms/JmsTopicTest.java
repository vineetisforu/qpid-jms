/*
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
package org.apache.qpid.jms;

import static org.apache.qpid.jms.JmsDestination.NAME_PROP;
import static org.apache.qpid.jms.JmsDestination.TEMPORARY_PROP;
import static org.apache.qpid.jms.JmsDestination.TOPIC_PROP;
import static org.apache.qpid.jms.SerializationTestSupport.roundTripSerializeDestination;
import static org.apache.qpid.jms.SerializationTestSupport.serializeDestination;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;

import org.apache.qpid.jms.test.QpidJmsTestCase;
import org.junit.Test;

public class JmsTopicTest extends QpidJmsTestCase {

    @Test
    public void testIsQueue() {
        JmsTopic topic = new JmsTopic("myTopic");
        assertFalse("should not be a queue", topic.isQueue());
    }

    @Test
    public void testIsTopic() {
        JmsTopic topic = new JmsTopic("myTopic");
        assertTrue("should be a topic", topic.isTopic());
    }

    @Test
    public void testIsTemporary() {
        JmsTopic topic = new JmsTopic("myTopic");
        assertFalse("should not be temporary", topic.isTemporary());
    }

    @Test
    public void testEqualsWithNull() {
        JmsTopic topic = new JmsTopic("myTopic");
        assertFalse("should not be equal", topic.equals(null));
    }

    @Test
    public void testEqualsWithDifferentObjectType() {
        JmsTopic topic = new JmsTopic("name");
        JmsQueue otherObject = new JmsQueue("name");
        assertFalse("should not be equal", topic.equals(otherObject));
    }

    @Test
    public void testEqualsWithSameObject() {
        JmsTopic topic = new JmsTopic("name");
        assertTrue("should be equal to itself", topic.equals(topic));
    }

    @Test
    public void testEqualsWithDifferentObject() {
        JmsTopic topic1 = new JmsTopic("name");
        JmsTopic topic2 = new JmsTopic("name");
        assertTrue("should be equal", topic1.equals(topic2));
        assertTrue("should still be equal", topic2.equals(topic1));
    }

    @Test
    public void testHashcodeWithEqualNamedObjects() {
        JmsTopic topic1 = new JmsTopic("name");
        JmsTopic topic2 = new JmsTopic("name");
        assertEquals("should have same hashcode", topic1.hashCode(), topic2.hashCode());
    }

    @Test
    public void testHashcodeWithDifferentNamedObjects() {
        JmsTopic topic1 = new JmsTopic("name1");
        JmsTopic topic2 = new JmsTopic("name2");

        // Not strictly a requirement, but expected in this case
        assertNotEquals("should not have same hashcode", topic1.hashCode(), topic2.hashCode());
    }

    @Test
    public void testPopulateProperties() throws Exception {
        String name = "myTopic";
        JmsTopic topic = new JmsTopic(name);

        Map<String, String> props = new HashMap<String, String>();
        topic.populateProperties(props);

        assertTrue("Property not found: " + TEMPORARY_PROP, props.containsKey(TEMPORARY_PROP));
        assertEquals("Unexpected value for property: " + TEMPORARY_PROP, "false", props.get(TEMPORARY_PROP));
        assertTrue("Property not found: " + NAME_PROP, props.containsKey(NAME_PROP));
        assertEquals("Unexpected value for property: " + NAME_PROP, name, props.get(NAME_PROP));
        assertTrue("Property not found: " + TOPIC_PROP, props.containsKey(TOPIC_PROP));
        assertEquals("Unexpected value for property: " + TOPIC_PROP, "true", props.get(TOPIC_PROP));
        assertEquals("Unexpected number of properties", 3, props.size());
    }

    @Test
    public void testSerializeThenDeserialize() throws Exception {
        String name = "myTopic";
        JmsTopic topic = new JmsTopic(name);

        Destination roundTripped = roundTripSerializeDestination(topic);

        assertNotNull("Null destination returned", roundTripped);
        assertEquals("Unexpected type", JmsTopic.class, roundTripped.getClass());
        assertEquals("Unexpected name", name, ((JmsTopic)roundTripped).getTopicName());
        assertEquals("Objects were not equal", topic, roundTripped);
    }

    @Test
    public void testSerializeTwoEqualDestinations() throws Exception {
        JmsTopic topic1 = new JmsTopic("myTopic");
        JmsTopic topic2 = new JmsTopic("myTopic");

        assertEquals("Destinations were not equal", topic1, topic2);

        byte[] bytes1 = serializeDestination(topic1);
        byte[] bytes2 = serializeDestination(topic2);

        assertArrayEquals("Serialized bytes were not equal", bytes1, bytes2);
    }

    @Test
    public void testSerializeTwoDifferentDestinations() throws Exception {
        JmsTopic topic1 = new JmsTopic("myTopic1");
        JmsTopic topic2 = new JmsTopic("myTopic2");

        assertNotEquals("Destinations were not expected to be equal", topic1, topic2);

        byte[] bytes1 = serializeDestination(topic1);
        byte[] bytes2 = serializeDestination(topic2);

        try {
            assertArrayEquals(bytes1, bytes2);
            fail("Expected arrays to differ");
        } catch (AssertionError ae) {
            // Expected, pass
        }
    }
}
