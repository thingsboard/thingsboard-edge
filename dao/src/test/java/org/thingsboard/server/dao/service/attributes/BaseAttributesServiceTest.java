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
package org.thingsboard.server.dao.service.attributes;

import com.datastax.driver.core.utils.UUIDs;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
public abstract class BaseAttributesServiceTest extends AbstractServiceTest {

    @Autowired
    private AttributesService attributesService;

    @Before
    public void before() {
    }

    @Test
    public void saveAndFetch() throws Exception {
        DeviceId deviceId = new DeviceId(UUIDs.timeBased());
        KvEntry attrValue = new StringDataEntry("attribute1", "value1");
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        attributesService.save(deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attr)).get();
        Optional<AttributeKvEntry> saved = attributesService.find(deviceId, DataConstants.CLIENT_SCOPE, attr.getKey()).get();
        Assert.assertTrue(saved.isPresent());
        Assert.assertEquals(attr, saved.get());
    }

    @Test
    public void saveMultipleTypeAndFetch() throws Exception {
        DeviceId deviceId = new DeviceId(UUIDs.timeBased());
        KvEntry attrOldValue = new StringDataEntry("attribute1", "value1");
        AttributeKvEntry attrOld = new BaseAttributeKvEntry(attrOldValue, 42L);

        attributesService.save(deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrOld)).get();
        Optional<AttributeKvEntry> saved = attributesService.find(deviceId, DataConstants.CLIENT_SCOPE, attrOld.getKey()).get();

        Assert.assertTrue(saved.isPresent());
        Assert.assertEquals(attrOld, saved.get());

        KvEntry attrNewValue = new StringDataEntry("attribute1", "value2");
        AttributeKvEntry attrNew = new BaseAttributeKvEntry(attrNewValue, 73L);
        attributesService.save(deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrNew)).get();

        saved = attributesService.find(deviceId, DataConstants.CLIENT_SCOPE, attrOld.getKey()).get();
        Assert.assertEquals(attrNew, saved.get());
    }

    @Test
    public void findAll() throws Exception {
        DeviceId deviceId = new DeviceId(UUIDs.timeBased());

        KvEntry attrAOldValue = new StringDataEntry("A", "value1");
        AttributeKvEntry attrAOld = new BaseAttributeKvEntry(attrAOldValue, 42L);
        KvEntry attrANewValue = new StringDataEntry("A", "value2");
        AttributeKvEntry attrANew = new BaseAttributeKvEntry(attrANewValue, 73L);
        KvEntry attrBNewValue = new StringDataEntry("B", "value3");
        AttributeKvEntry attrBNew = new BaseAttributeKvEntry(attrBNewValue, 73L);

        attributesService.save(deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrAOld)).get();
        attributesService.save(deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrANew)).get();
        attributesService.save(deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrBNew)).get();

        List<AttributeKvEntry> saved = attributesService.findAll(deviceId, DataConstants.CLIENT_SCOPE).get();

        Assert.assertNotNull(saved);
        Assert.assertEquals(2, saved.size());

        Assert.assertEquals(attrANew, saved.get(0));
        Assert.assertEquals(attrBNew, saved.get(1));
    }

}
