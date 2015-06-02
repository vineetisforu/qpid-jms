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
package org.apache.qpid.jms.sasl;

import static org.junit.Assert.*;

import org.junit.Test;

public class PlainMechanismTest {

    @Test
    public void testIsNotApplicableWithNoCredentials() {
        PlainMechanism mech = new PlainMechanism();

        assertFalse("Should not be applicable with no credentials", mech.isApplicable(null, null));
    }

    @Test
    public void testIsNotApplicableWithNoUser() {
        PlainMechanism mech = new PlainMechanism();

        assertFalse("Should not be applicable with no username", mech.isApplicable(null, "pass"));
    }

    @Test
    public void testIsNotApplicableWithNoPassword() {
        PlainMechanism mech = new PlainMechanism();

        assertFalse("Should not be applicable with no password", mech.isApplicable("user", null));
    }

    @Test
    public void testIsNotApplicableWithEmtpyUser() {
        PlainMechanism mech = new PlainMechanism();

        assertFalse("Should not be applicable with empty username", mech.isApplicable("", "pass"));
    }

    @Test
    public void testIsNotApplicableWithEmtpyPassword() {
        PlainMechanism mech = new PlainMechanism();

        assertFalse("Should not be applicable with empty password", mech.isApplicable("user", ""));
    }

    @Test
    public void testIsNotApplicableWithEmtpyUserAndPassword() {
        PlainMechanism mech = new PlainMechanism();

        assertFalse("Should not be applicable with empty user and password", mech.isApplicable("", ""));
    }

    @Test
    public void testIsApplicableWithUserAndPassword() {
        PlainMechanism mech = new PlainMechanism();

        assertTrue("Should be applicable with user and password", mech.isApplicable("user", "password"));
    }
}
