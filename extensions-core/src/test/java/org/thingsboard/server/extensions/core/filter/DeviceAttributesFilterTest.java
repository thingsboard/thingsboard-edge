/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.extensions.core.filter;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.extensions.api.device.DeviceAttributes;
import org.thingsboard.server.extensions.api.device.DeviceMetaData;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@RunWith(MockitoJUnitRunner.class)
public class DeviceAttributesFilterTest {

    @Mock
    RuleContext ruleCtx;

    private static JsFilterConfiguration wrap(String filterBody) {
        return new JsFilterConfiguration(filterBody);
    }

    @Test
    public void basicMissingAttributesTest() {
        DeviceAttributesFilter filter = new DeviceAttributesFilter();
        filter.init(wrap("((typeof nonExistingVal === 'undefined') || nonExistingVal == true) && booleanValue == false"));
        List<AttributeKvEntry> clientAttributes = new ArrayList<>();
        clientAttributes.add(new BaseAttributeKvEntry(new BooleanDataEntry("booleanValue", false), 42));
        DeviceAttributes attributes = new DeviceAttributes(clientAttributes, new ArrayList<>(), new ArrayList<>());

        Mockito.when(ruleCtx.getDeviceMetaData()).thenReturn(new DeviceMetaData(new DeviceId(UUID.randomUUID()), "A", "A", attributes));
        Assert.assertTrue(filter.filter(ruleCtx, null));
        filter.stop();
    }

    @Test
    public void basicClientAttributesTest() {
        DeviceAttributesFilter filter = new DeviceAttributesFilter();
        filter.init(wrap("doubleValue == 1.0 && booleanValue == false"));
        List<AttributeKvEntry> clientAttributes = new ArrayList<>();
        clientAttributes.add(new BaseAttributeKvEntry(new DoubleDataEntry("doubleValue", 1.0), 42));
        clientAttributes.add(new BaseAttributeKvEntry(new BooleanDataEntry("booleanValue", false), 42));
        DeviceAttributes attributes = new DeviceAttributes(clientAttributes, new ArrayList<>(), new ArrayList<>());

        Mockito.when(ruleCtx.getDeviceMetaData()).thenReturn(new DeviceMetaData(new DeviceId(UUID.randomUUID()), "A", "A", attributes));
        Assert.assertTrue(filter.filter(ruleCtx, null));
        filter.stop();
    }

    @Test(timeout = 30000)
    public void basicClientAttributesStressTest() {
        DeviceAttributesFilter filter = new DeviceAttributesFilter();
        filter.init(wrap("doubleValue == 1.0 && booleanValue == false"));

        List<AttributeKvEntry> clientAttributes = new ArrayList<>();
        clientAttributes.add(new BaseAttributeKvEntry(new DoubleDataEntry("doubleValue", 1.0), 42));
        clientAttributes.add(new BaseAttributeKvEntry(new BooleanDataEntry("booleanValue", false), 42));
        DeviceAttributes attributes = new DeviceAttributes(clientAttributes, new ArrayList<>(), new ArrayList<>());

        Mockito.when(ruleCtx.getDeviceMetaData()).thenReturn(new DeviceMetaData(new DeviceId(UUID.randomUUID()), "A", "A", attributes));

        for (int i = 0; i < 10000; i++) {
            Assert.assertTrue(filter.filter(ruleCtx, null));
        }
        filter.stop();
    }

    @Test
    public void basicServerAttributesTest() {
        DeviceAttributesFilter filter = new DeviceAttributesFilter();
        filter.init(wrap("doubleValue == 1.0 && booleanValue == false"));

        List<AttributeKvEntry> serverAttributes = new ArrayList<>();
        serverAttributes.add(new BaseAttributeKvEntry(new DoubleDataEntry("doubleValue", 1.0), 42));
        serverAttributes.add(new BaseAttributeKvEntry(new BooleanDataEntry("booleanValue", false), 42));
        DeviceAttributes attributes = new DeviceAttributes(new ArrayList<>(), serverAttributes, new ArrayList<>());

        Mockito.when(ruleCtx.getDeviceMetaData()).thenReturn(new DeviceMetaData(new DeviceId(UUID.randomUUID()), "A", "A", attributes));
        Assert.assertTrue(filter.filter(ruleCtx, null));
        filter.stop();
    }

    @Test
    public void basicConflictServerAttributesTest() {
        DeviceAttributesFilter filter = new DeviceAttributesFilter();
        filter.init(wrap("cs.doubleValue == 1.0 && cs.booleanValue == true && ss.doubleValue == 0.0 && ss.booleanValue == false"));

        List<AttributeKvEntry> clientAttributes = new ArrayList<>();
        clientAttributes.add(new BaseAttributeKvEntry(new DoubleDataEntry("doubleValue", 1.0), 42));
        clientAttributes.add(new BaseAttributeKvEntry(new BooleanDataEntry("booleanValue", true), 42));

        List<AttributeKvEntry> serverAttributes = new ArrayList<>();
        serverAttributes.add(new BaseAttributeKvEntry(new DoubleDataEntry("doubleValue", 0.0), 42));
        serverAttributes.add(new BaseAttributeKvEntry(new BooleanDataEntry("booleanValue", false), 42));
        DeviceAttributes attributes = new DeviceAttributes(clientAttributes, serverAttributes, new ArrayList<>());

        Mockito.when(ruleCtx.getDeviceMetaData()).thenReturn(new DeviceMetaData(new DeviceId(UUID.randomUUID()), "A", "A", attributes));
        Assert.assertTrue(filter.filter(ruleCtx, null));
        filter.stop();
    }

}
