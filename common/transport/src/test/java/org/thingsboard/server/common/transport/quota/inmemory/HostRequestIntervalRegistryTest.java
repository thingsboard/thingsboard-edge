/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.transport.quota.inmemory;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.transport.quota.host.HostRequestIntervalRegistry;

import static org.junit.Assert.assertEquals;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
public class HostRequestIntervalRegistryTest {

    private HostRequestIntervalRegistry registry;

    @Before
    public void init() {
        registry = new HostRequestIntervalRegistry(10000L, 100L,"g1,g2", "b1");
    }

    @Test
    public void newHostCreateNewInterval() {
        assertEquals(1L, registry.tick("host1"));
    }

    @Test
    public void existingHostUpdated() {
        registry.tick("aaa");
        assertEquals(1L, registry.tick("bbb"));
        assertEquals(2L, registry.tick("aaa"));
    }

    @Test
    public void expiredIntervalsCleaned() throws InterruptedException {
        registry.tick("aaa");
        Thread.sleep(150L);
        registry.tick("bbb");
        registry.clean();
        assertEquals(1L, registry.tick("aaa"));
        assertEquals(2L, registry.tick("bbb"));
    }

    @Test
    public void domainFromWhitelistNotCounted(){
        assertEquals(0L, registry.tick("g1"));
        assertEquals(0L, registry.tick("g1"));
        assertEquals(0L, registry.tick("g2"));
    }

    @Test
    public void domainFromBlackListReturnMaxValue(){
        assertEquals(Long.MAX_VALUE, registry.tick("b1"));
        assertEquals(Long.MAX_VALUE, registry.tick("b1"));
    }

    @Test
    public void emptyWhitelistParsedOk(){
        registry = new HostRequestIntervalRegistry(10000L, 100L,"", "b1");
        assertEquals(1L, registry.tick("aaa"));
    }

    @Test
    public void emptyBlacklistParsedOk(){
        registry = new HostRequestIntervalRegistry(10000L, 100L,"", "");
        assertEquals(1L, registry.tick("aaa"));
    }
}