/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Firmware;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseFirmwareControllerTest extends AbstractControllerTest {

    private IdComparator<FirmwareInfo> idComparator = new IdComparator<>();

    public static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final String CHECKSUM_ALGORITHM = "sha256";
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

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
    public void testSaveFirmware() throws Exception {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);

        FirmwareInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(savedFirmwareInfo);

        FirmwareInfo foundFirmwareInfo = doGet("/api/firmware/info/" + savedFirmwareInfo.getId().getId().toString(), FirmwareInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
    }

    @Test
    public void testSaveFirmwareData() throws Exception {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);

        FirmwareInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(savedFirmwareInfo);

        FirmwareInfo foundFirmwareInfo = doGet("/api/firmware/info/" + savedFirmwareInfo.getId().getId().toString(), FirmwareInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        Firmware savedFirmware = savaData("/api/firmware/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        Assert.assertEquals(FILE_NAME, savedFirmware.getFileName());
        Assert.assertEquals(CONTENT_TYPE, savedFirmware.getContentType());
    }

    @Test
    public void testUpdateFirmwareFromDifferentTenant() throws Exception {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);

        FirmwareInfo savedFirmwareInfo = save(firmwareInfo);

        loginDifferentTenant();
        doPost("/api/firmware", savedFirmwareInfo, FirmwareInfo.class, status().isForbidden());
        deleteDifferentTenant();
    }

    @Test
    public void testFindFirmwareInfoById() throws Exception {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);

        FirmwareInfo savedFirmwareInfo = save(firmwareInfo);

        FirmwareInfo foundFirmware = doGet("/api/firmware/info/" + savedFirmwareInfo.getId().getId().toString(), FirmwareInfo.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmwareInfo, foundFirmware);
    }

    @Test
    public void testFindFirmwareById() throws Exception {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);

        FirmwareInfo savedFirmwareInfo = save(firmwareInfo);

        MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        Firmware savedFirmware = savaData("/api/firmware/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        Firmware foundFirmware = doGet("/api/firmware/" + savedFirmwareInfo.getId().getId().toString(), Firmware.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
    }

    @Test
    public void testDeleteFirmware() throws Exception {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);

        FirmwareInfo savedFirmwareInfo = save(firmwareInfo);

        doDelete("/api/firmware/" + savedFirmwareInfo.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/firmware/info/" + savedFirmwareInfo.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindTenantFirmwares() throws Exception {
        List<FirmwareInfo> firmwares = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            FirmwareInfo firmwareInfo = new FirmwareInfo();
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);

            FirmwareInfo savedFirmwareInfo = save(firmwareInfo);

            if (i > 100) {
                MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                Firmware savedFirmware = savaData("/api/firmware/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                firmwares.add(new FirmwareInfo(savedFirmware));
            } else {
                firmwares.add(savedFirmwareInfo);
            }
        }

        List<FirmwareInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<FirmwareInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/firmwares?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(firmwares, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(firmwares, loadedFirmwares);
    }

    @Test
    public void testFindTenantFirmwaresByHasData() throws Exception {
        List<FirmwareInfo> firmwaresWithData = new ArrayList<>();
        List<FirmwareInfo> firmwaresWithoutData = new ArrayList<>();

        for (int i = 0; i < 165; i++) {
            FirmwareInfo firmwareInfo = new FirmwareInfo();
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);

            FirmwareInfo savedFirmwareInfo = save(firmwareInfo);

            if (i > 100) {
                MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                Firmware savedFirmware = savaData("/api/firmware/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                firmwaresWithData.add(new FirmwareInfo(savedFirmware));
            } else {
                firmwaresWithoutData.add(savedFirmwareInfo);
            }
        }

        List<FirmwareInfo> loadedFirmwaresWithData = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<FirmwareInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/firmwares/true?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedFirmwaresWithData.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        List<FirmwareInfo> loadedFirmwaresWithoutData = new ArrayList<>();
        pageLink = new PageLink(24);
        do {
            pageData = doGetTypedWithPageLink("/api/firmwares/false?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedFirmwaresWithoutData.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(firmwaresWithData, idComparator);
        Collections.sort(firmwaresWithoutData, idComparator);
        Collections.sort(loadedFirmwaresWithData, idComparator);
        Collections.sort(loadedFirmwaresWithoutData, idComparator);

        Assert.assertEquals(firmwaresWithData, loadedFirmwaresWithData);
        Assert.assertEquals(firmwaresWithoutData, loadedFirmwaresWithoutData);
    }


    private FirmwareInfo save(FirmwareInfo firmwareInfo) throws Exception {
        return doPost("/api/firmware", firmwareInfo, FirmwareInfo.class);
    }

    protected Firmware savaData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), Firmware.class);
    }

}
