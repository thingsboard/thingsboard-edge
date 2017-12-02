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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseConverterControllerTest extends AbstractControllerTest {

    private IdComparator<Converter> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveConverter() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.GENERIC);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        Assert.assertNotNull(savedConverter);
        Assert.assertNotNull(savedConverter.getId());
        Assert.assertTrue(savedConverter.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedConverter.getTenantId());
        Assert.assertEquals(converter.getName(), savedConverter.getName());

        savedConverter.setName("My new converter");
        doPost("/api/converter", savedConverter, Converter.class);

        Converter foundConverter = doGet("/api/converter/" + savedConverter.getId().getId().toString(), Converter.class);
        Assert.assertEquals(foundConverter.getName(), savedConverter.getName());
    }

    @Test
    public void testFindConverterById() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.GENERIC);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);
        Converter foundConverter = doGet("/api/converter/" + savedConverter.getId().getId().toString(), Converter.class);
        Assert.assertNotNull(foundConverter);
        Assert.assertEquals(savedConverter, foundConverter);
    }

    @Test
    public void testDeleteConverter() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.GENERIC);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        doDelete("/api/converter/" + savedConverter.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/converter/" + savedConverter.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveConverterWithEmptyType() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Converter type should be specified")));
    }

    @Test
    public void testSaveConverterWithEmptyName() throws Exception {
        Converter converter = new Converter();
        converter.setType(ConverterType.GENERIC);
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Converter name should be specified")));
    }

    @Test
    public void testFindTenantConverters() throws Exception {
        List<Converter> converters = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Converter converter = new Converter();
            converter.setName("Converter" + i);
            converter.setType(ConverterType.GENERIC);
            converters.add(doPost("/api/converter", converter, Converter.class));
        }
        List<Converter> loadedConverters = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Converter> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<TextPageData<Converter>>() {
                    }, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters, idComparator);
        Collections.sort(loadedConverters, idComparator);

        Assert.assertEquals(converters, loadedConverters);
    }

    @Test
    public void testFindTenantConvertersBySearchText() throws Exception {
        String title1 = "Converter title 1";
        List<Converter> converters = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Converter converter = new Converter();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            converter.setName(name);
            converter.setType(ConverterType.GENERIC);
            converters.add(doPost("/api/converter", converter, Converter.class));
        }
        String title2 = "Converter title 2";
        List<Converter> converters1 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Converter converter = new Converter();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            converter.setName(name);
            converter.setType(ConverterType.GENERIC);
            converters1.add(doPost("/api/converter", converter, Converter.class));
        }

        List<Converter> loadedConverters = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Converter> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<TextPageData<Converter>>() {
                    }, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters, idComparator);
        Collections.sort(loadedConverters, idComparator);

        Assert.assertEquals(converters, loadedConverters);

        List<Converter> loadedConverters1 = new ArrayList<>();
        pageLink = new TextPageLink(4, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<TextPageData<Converter>>() {
                    }, pageLink);
            loadedConverters1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters1, idComparator);
        Collections.sort(loadedConverters1, idComparator);

        Assert.assertEquals(converters1, loadedConverters1);

        for (Converter converter : loadedConverters) {
            doDelete("/api/converter/" + converter.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title1);
        pageData = doGetTypedWithPageLink("/api/converters?",
                new TypeReference<TextPageData<Converter>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Converter converter : loadedConverters1) {
            doDelete("/api/converter/" + converter.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = doGetTypedWithPageLink("/api/converters?",
                new TypeReference<TextPageData<Converter>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

}
