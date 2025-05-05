/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class SecretControllerTest extends AbstractControllerTest {

    private static final TypeReference<PageData<SecretInfo>> PAGE_DATA_SECRET_TYPE_REF = new TypeReference<>() {};

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void tearDown() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(100, 0));
        for (SecretInfo secretInfo : pageData.getData()) {
            doDelete("/api/secret/" + secretInfo.getId().getId()).andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveSecret() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Create Secret", "CreatePassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        PageData<SecretInfo> pageData2 = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData2.getData()).hasSize(1);
        assertThat(pageData2.getData().get(0)).isEqualTo(new SecretInfo(savedSecret));

        SecretInfo retrievedSecret = doGet("/api/secret/info/{id}", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/info/{id}", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecret() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Update Secret", "UpdatePassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/info/{id}", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        secret = constructSecret(savedSecret);
        secret.setValue("UpdatedPassword".getBytes(StandardCharsets.UTF_8));

        SecretInfo updatedSecret = doPost("/api/secret", secret, SecretInfo.class);
        retrievedSecret = doGet("/api/secret/info/{id}", SecretInfo.class, updatedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(updatedSecret);

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/info/{id}", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecretNameProhibited() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Secret", "Prohibited");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        secret = constructSecret(savedSecret);
        secret.setName("Updated Name");

        doPost("/api/secret", secret)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Can't update secret name!")));

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/info/{id}", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testFindSecretInfos() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        int expectedSize = 10;
        String namePrefix = "Test Create Secret_";
        for (int i = 0; i < expectedSize; i++) {
            doPost("/api/secret", constructSecret(namePrefix + i, "CreatePassword"), SecretInfo.class);
        }

        PageData<SecretInfo> pageData2 = doGetTypedWithPageLink("/api/secret/infos?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(expectedSize, 0));
        assertThat(pageData2.getData()).hasSize(expectedSize);

        List<UUID> toDelete = new ArrayList<>();

        for (int i = 0; i < expectedSize; i++) {
            SecretInfo secretInfo = pageData2.getData().get(i);
            assertThat(secretInfo.getName()).isEqualTo(namePrefix + i);
            toDelete.add(secretInfo.getUuidId());
        }

        toDelete.forEach(secret -> {
            try {
                doDelete("/api/secret/" + secret).andExpect(status().isOk());
                doGet("/api/secret/info/{id}", secret).andExpect(status().isNotFound());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Secret constructSecret(String name, String value) {
        Secret secret = new Secret();
        secret.setName(name);
        secret.setValue(value.getBytes(StandardCharsets.UTF_8));
        return secret;
    }

    private Secret constructSecret(SecretInfo secretInfo) {
        Secret secret = new Secret();
        secret.setId(secretInfo.getId());
        secret.setName(secretInfo.getName());
        secret.setTenantId(secretInfo.getTenantId());
        secret.setCreatedTime(secretInfo.getCreatedTime());
        return secret;
    }

}
