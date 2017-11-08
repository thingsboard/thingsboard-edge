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
package org.thingsboard.server.dao.sql.converter;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.converter.ConverterDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

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
            saveConverter(converterId, tenantId, "CONVERTER_" + i, ConverterType.JS);
        }
        assertEquals(60, converterDao.find().size());

        TextPageLink pageLink1 = new TextPageLink(20, "CONVERTER_");
        List<Converter> converters1 = converterDao.findConvertersByTenantId(tenantId1, pageLink1);
        assertEquals(20, converters1.size());

        TextPageLink pageLink2 = new TextPageLink(20, "CONVERTER_", converters1.get(19).getId().getId(), null);
        List<Converter> converters2 = converterDao.findConvertersByTenantId(tenantId1, pageLink2);
        assertEquals(10, converters2.size());

        TextPageLink pageLink3 = new TextPageLink(20, "CONVERTER_", converters2.get(9).getId().getId(), null);
        List<Converter> converters3 = converterDao.findConvertersByTenantId(tenantId1, pageLink3);
        assertEquals(0, converters3.size());
    }

    @Test
    public void testFindConvertersByTenantIdAndIdsAsync() throws ExecutionException, InterruptedException {
        UUID tenantId = UUIDs.timeBased();
        List<UUID> searchIds = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            UUID converterId = UUIDs.timeBased();
            saveConverter(converterId, tenantId, "CONVERTER_" + i, ConverterType.JS);
            if (i % 3 == 0) {
                searchIds.add(converterId);
            }
        }

        ListenableFuture<List<Converter>> convertersFuture = converterDao
                .findConvertersByTenantIdAndIdsAsync(tenantId, searchIds);
        List<Converter> converters = convertersFuture.get();
        assertNotNull(converters);
        assertEquals(10, converters.size());
    }

    @Test
    public void testFindAssetsByTenantIdAndName() {
        UUID converterId1 = UUIDs.timeBased();
        UUID converterId2 = UUIDs.timeBased();
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        String name = "TEST_CONVERTER";
        saveConverter(converterId1, tenantId1, name, ConverterType.JS);
        saveConverter(converterId2, tenantId2, name, ConverterType.JS);

        Optional<Converter> converterOpt1 = converterDao.findConvertersByTenantIdAndName(tenantId2, name);
        assertTrue("Optional expected to be non-empty", converterOpt1.isPresent());
        assertEquals(converterId2, converterOpt1.get().getId().getId());

        Optional<Converter> converterOpt2 = converterDao.findConvertersByTenantIdAndName(tenantId2, "NON_EXISTENT_NAME");
        assertFalse("Optional expected to be empty", converterOpt2.isPresent());
    }

    @Test
    public void testFindTenantAssetTypesAsync() throws ExecutionException, InterruptedException {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        saveConverter(UUIDs.timeBased(), tenantId1, "TEST_CONVERTER_1", ConverterType.JS);
        saveConverter(UUIDs.timeBased(), tenantId1, "TEST_CONVERTER_2", ConverterType.JS);
        saveConverter(UUIDs.timeBased(), tenantId1, "TEST_CONVERTER_3", ConverterType.GENERIC);
        saveConverter(UUIDs.timeBased(), tenantId1, "TEST_CONVERTER_4", ConverterType.GENERIC);
        saveConverter(UUIDs.timeBased(), tenantId1, "TEST_CONVERTER_5", ConverterType.GENERIC);

        saveConverter(UUIDs.timeBased(), tenantId2, "TEST_CONVERTER_6", ConverterType.JS);
        saveConverter(UUIDs.timeBased(), tenantId2, "TEST_CONVERTER_7", ConverterType.JS);

        List<EntitySubtype> tenant1Types = converterDao.findTenantConverterTypesAsync(tenantId1).get();
        assertNotNull(tenant1Types);
        List<EntitySubtype> tenant2Types = converterDao.findTenantConverterTypesAsync(tenantId2).get();
        assertNotNull(tenant2Types);

        assertEquals(2, tenant1Types.size());
        assertTrue(tenant1Types.stream().anyMatch(t -> t.getType().equals(ConverterType.JS.toString())));
        assertTrue(tenant1Types.stream().anyMatch(t -> t.getType().equals(ConverterType.GENERIC.toString())));
        assertFalse(tenant1Types.stream().anyMatch(t -> t.getType().equals("TYPE_1")));

        assertEquals(1, tenant2Types.size());
        assertTrue(tenant2Types.stream().anyMatch(t -> t.getType().equals(ConverterType.JS.toString())));
        assertFalse(tenant2Types.stream().anyMatch(t -> t.getType().equals("TYPE_2")));
    }

    private void saveConverter(UUID id, UUID tenantId, String name, ConverterType type) {
        Converter converter = new Converter();
        converter.setId(new ConverterId(id));
        converter.setTenantId(new TenantId(tenantId));
        converter.setName(name);
        converter.setType(type);
        converterDao.save(converter);
    }
}
