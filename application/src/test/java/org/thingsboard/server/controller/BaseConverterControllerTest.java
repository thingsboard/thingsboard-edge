/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
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

    private static final JsonNode CUSTOM_CONVERTER_CONFIGURATION = new ObjectMapper()
            .createObjectNode().put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

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
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
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
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);
        Converter foundConverter = doGet("/api/converter/" + savedConverter.getId().getId().toString(), Converter.class);
        Assert.assertNotNull(foundConverter);
        Assert.assertEquals(savedConverter, foundConverter);
    }

    @Test
    public void testDeleteConverter() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
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
        converter.setType(ConverterType.UPLINK);
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
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
            converters.add(doPost("/api/converter", converter, Converter.class));
        }
        List<Converter> loadedConverters = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Converter> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<PageData<Converter>>() {
                    }, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
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
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
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
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
            converters1.add(doPost("/api/converter", converter, Converter.class));
        }

        List<Converter> loadedConverters = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Converter> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<PageData<Converter>>() {
                    }, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters, idComparator);
        Collections.sort(loadedConverters, idComparator);

        Assert.assertEquals(converters, loadedConverters);

        List<Converter> loadedConverters1 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<PageData<Converter>>() {
                    }, pageLink);
            loadedConverters1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters1, idComparator);
        Collections.sort(loadedConverters1, idComparator);

        Assert.assertEquals(converters1, loadedConverters1);

        for (Converter converter : loadedConverters) {
            doDelete("/api/converter/" + converter.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/converters?",
                new TypeReference<PageData<Converter>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Converter converter : loadedConverters1) {
            doDelete("/api/converter/" + converter.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/converters?",
                new TypeReference<PageData<Converter>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

}
