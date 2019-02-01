/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.converter;

import com.datastax.driver.core.utils.UUIDs;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.converter.ConverterDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JpaConverterDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private ConverterDao converterDao;

    @Test
    public void testFindConvertersByTenantId() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID converterId = UUIDs.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            saveConverter(converterId, tenantId, "CONVERTER_" + i, ConverterType.UPLINK);
        }
        assertEquals(60, converterDao.find(TenantId.SYS_TENANT_ID).size());

        TextPageLink pageLink1 = new TextPageLink(20, "CONVERTER_");
        List<Converter> converters1 = converterDao.findByTenantIdAndPageLink(tenantId1, pageLink1);
        assertEquals(20, converters1.size());

        TextPageLink pageLink2 = new TextPageLink(20, "CONVERTER_", converters1.get(19).getId().getId(), null);
        List<Converter> converters2 = converterDao.findByTenantIdAndPageLink(tenantId1, pageLink2);
        assertEquals(10, converters2.size());

        TextPageLink pageLink3 = new TextPageLink(20, "CONVERTER_", converters2.get(9).getId().getId(), null);
        List<Converter> converters3 = converterDao.findByTenantIdAndPageLink(tenantId1, pageLink3);
        assertEquals(0, converters3.size());
    }

    @Test
    public void testFindAssetsByTenantIdAndName() {
        UUID converterId1 = UUIDs.timeBased();
        UUID converterId2 = UUIDs.timeBased();
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        String name = "TEST_CONVERTER";
        saveConverter(converterId1, tenantId1, name, ConverterType.UPLINK);
        saveConverter(converterId2, tenantId2, name, ConverterType.UPLINK);

        Optional<Converter> converterOpt1 = converterDao.findConverterByTenantIdAndName(tenantId2, name);
        assertTrue("Optional expected to be non-empty", converterOpt1.isPresent());
        assertEquals(converterId2, converterOpt1.get().getId().getId());

        Optional<Converter> converterOpt2 = converterDao.findConverterByTenantIdAndName(tenantId2, "NON_EXISTENT_NAME");
        assertFalse("Optional expected to be empty", converterOpt2.isPresent());
    }

    private void saveConverter(UUID id, UUID tenantId, String name, ConverterType type) {
        Converter converter = new Converter();
        converter.setId(new ConverterId(id));
        converter.setTenantId(new TenantId(tenantId));
        converter.setName(name);
        converter.setType(type);
        converterDao.save(new TenantId(tenantId), converter);
    }
}
