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
package org.thingsboard.server.dao.service;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseConverterServiceTest extends AbstractBeforeTest {

    private IdComparator<Converter> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void beforeRun() {
        tenantId = before();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveConverter() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType("default");
        Converter savedConverter = converterService.saveConverter(converter);

        Assert.assertNotNull(savedConverter);
        Assert.assertNotNull(savedConverter.getId());
        Assert.assertTrue(savedConverter.getCreatedTime() > 0);
        Assert.assertEquals(converter.getTenantId(), savedConverter.getTenantId());
        Assert.assertEquals(converter.getName(), savedConverter.getName());

        savedConverter.setName("My new converter");

        converterService.saveConverter(savedConverter);
        Converter foundConverter = converterService.findConverterById(savedConverter.getId());
        Assert.assertEquals(foundConverter.getName(), savedConverter.getName());

        converterService.deleteConverter(savedConverter.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveConverterWithEmptyName() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setType("default");
        converterService.saveConverter(converter);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveConverterWithEmptyTenant() {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType("default");
        converterService.saveConverter(converter);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveConverterWithInvalidTenant() {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType("default");
        converter.setTenantId(new TenantId(UUIDs.timeBased()));
        converterService.saveConverter(converter);
    }

    @Test
    public void testFindConverterById() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType("default");
        Converter savedConverter = converterService.saveConverter(converter);
        Converter foundConverter = converterService.findConverterById(savedConverter.getId());
        Assert.assertNotNull(foundConverter);
        Assert.assertEquals(savedConverter, foundConverter);
        converterService.deleteConverter(savedConverter.getId());
    }

    @Test
    public void testFindConverterTypesByTenantId() throws Exception {
        List<Converter> converters = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Converter converter = new Converter();
                converter.setTenantId(tenantId);
                converter.setName("My converter B" + i);
                converter.setType("typeB");
                converters.add(converterService.saveConverter(converter));
            }
            for (int i = 0; i < 7; i++) {
                Converter converter = new Converter();
                converter.setTenantId(tenantId);
                converter.setName("My converter C" + i);
                converter.setType("typeC");
                converters.add(converterService.saveConverter(converter));
            }
            for (int i = 0; i < 9; i++) {
                Converter converter = new Converter();
                converter.setTenantId(tenantId);
                converter.setName("My converter A" + i);
                converter.setType("typeA");
                converters.add(converterService.saveConverter(converter));
            }
            List<EntitySubtype> converterTypes = converterService.findConverterTypesByTenantId(tenantId).get();
            Assert.assertNotNull(converterTypes);
            Assert.assertEquals(3, converterTypes.size());
            Assert.assertEquals("typeA", converterTypes.get(0).getType());
            Assert.assertEquals("typeB", converterTypes.get(1).getType());
            Assert.assertEquals("typeC", converterTypes.get(2).getType());
        } finally {
            converters.forEach((converter) -> converterService.deleteConverter(converter.getId()));
        }
    }

    @Test
    public void testDeleteConverter() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType("default");
        Converter savedConverter = converterService.saveConverter(converter);
        Converter foundConverter = converterService.findConverterById(savedConverter.getId());
        Assert.assertNotNull(foundConverter);
        converterService.deleteConverter(savedConverter.getId());
        foundConverter = converterService.findConverterById(savedConverter.getId());
        Assert.assertNull(foundConverter);
    }

    @Test
    public void testFindConvertersByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<Converter> converters = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Converter converter = new Converter();
            converter.setTenantId(tenantId);
            converter.setName("Converter" + i);
            converter.setType("default");
            converters.add(converterService.saveConverter(converter));
        }

        List<Converter> loadedConverters;
        loadedConverters = getConvertersList(23, null, null);

        Collections.sort(converters, idComparator);
        Collections.sort(loadedConverters, idComparator);

        Assert.assertEquals(converters, loadedConverters);

        converterService.deleteConvertersByTenantId(tenantId);

        TextPageLink pageLink = new TextPageLink(33);
        TextPageData<Converter> pageData = converterService.findConvertersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    public List<Converter> createConvertersList(int maxInt, String title, String type) {
        List<Converter> converters = new ArrayList<>();
        for (int i = 0; i < maxInt; i++) {
            Converter converter = new Converter();
            converter.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            converter.setName(name);
            converter.setType(type);
            converters.add(converterService.saveConverter(converter));
        }
        return converters;
    }

    public List<Converter> getConvertersList(int limit, String title, String type) {
        List<Converter> loadedConverters = new ArrayList<>();
        TextPageData<Converter> pageData;
        TextPageLink pageLink = new TextPageLink(limit, title);
        if (type == null) {
            do {
                pageData = converterService.findConvertersByTenantId(tenantId, pageLink);
                loadedConverters.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        } else {
            do {
                pageData = converterService.findConvertersByTenantIdAndType(tenantId, type, pageLink);
                loadedConverters.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        }
        return loadedConverters;
    }

    @Test
    public void testFindConvertersByTenantIdAndName() {
        String title1 = "Converter title 1";
        List<Converter> convertersTitle1;
        convertersTitle1 = createConvertersList(143, title1, "default");

        String title2 = "Converter title 2";
        List<Converter> convertersTitle2;
        convertersTitle2 = createConvertersList(175, title2, "default");

        List<Converter> loadedConvertersTitle1;
        loadedConvertersTitle1 = getConvertersList(15, title1, null);
        Collections.sort(convertersTitle1, idComparator);
        Collections.sort(loadedConvertersTitle1, idComparator);
        Assert.assertEquals(convertersTitle1, loadedConvertersTitle1);

        List<Converter> loadedConvertersTitle2;
        loadedConvertersTitle2 = getConvertersList(4, title2, null);
        Collections.sort(convertersTitle2, idComparator);
        Collections.sort(loadedConvertersTitle2, idComparator);
        Assert.assertEquals(convertersTitle2, loadedConvertersTitle2);

        for (Converter converter : loadedConvertersTitle1) {
            converterService.deleteConverter(converter.getId());
        }

        TextPageLink pageLink = new TextPageLink(4, title1);
        TextPageData<Converter> pageData = converterService.findConvertersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Converter converter : loadedConvertersTitle2) {
            converterService.deleteConverter(converter.getId());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = converterService.findConvertersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindConvertersByTenantIdAndType() {
        String title1 = "Converter title 1";
        String type1 = "typeA";
        List<Converter> convertersType1;
        convertersType1 = createConvertersList(143, title1, type1);

        String title2 = "Converter title 2";
        String type2 = "typeB";
        List<Converter> convertersType2;
        convertersType2 = createConvertersList(175, title2, type2);

        List<Converter> loadedConvertersType1;
        loadedConvertersType1 = getConvertersList(15, null, type1);
        Collections.sort(convertersType1, idComparator);
        Collections.sort(loadedConvertersType1, idComparator);
        Assert.assertEquals(convertersType1, loadedConvertersType1);

        List<Converter> loadedConvertersType2;
        loadedConvertersType2 = getConvertersList(4, null, type2);
        Collections.sort(convertersType2, idComparator);
        Collections.sort(loadedConvertersType2, idComparator);
        Assert.assertEquals(convertersType2, loadedConvertersType2);

        for (Converter converter : loadedConvertersType1) {
            converterService.deleteConverter(converter.getId());
        }

        TextPageLink pageLink = new TextPageLink(4);
        TextPageData<Converter>  pageData = converterService.findConvertersByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Converter converter : loadedConvertersType2) {
            converterService.deleteConverter(converter.getId());
        }

        pageLink = new TextPageLink(4);
        pageData = converterService.findConvertersByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
}
