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

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class TrendzControllerTest extends AbstractControllerTest {

    private final String trendzUrl = "https://some.domain.com:18888/also_necessary_prefix";
    private final String apiKey = "$2a$10$iDjfqYmnrw9gkdw4XhgzFOU.R/pVz3OKgXOdpbR2LuXaKatGcGLiG";

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();

        TrendzSettings trendzSettings = new TrendzSettings();
        trendzSettings.setEnabled(true);
        trendzSettings.setBaseUrl(trendzUrl);
        trendzSettings.setApiKey(apiKey);

        doPost("/api/trendz/settings", trendzSettings).andExpect(status().isOk());
    }

    @Test
    public void testTrendzSettingsWhenTenant() throws Exception {
        loginTenantAdmin();

        TrendzSettings trendzSettings = doGet("/api/trendz/settings", TrendzSettings.class);

        assertThat(trendzSettings).isNotNull();
        assertThat(trendzSettings.isEnabled()).isTrue();
        assertThat(trendzSettings.getBaseUrl()).isEqualTo(trendzUrl);
        trendzSettings.setApiKey(apiKey);

        String updatedUrl = "https://some.domain.com:18888/tenant_trendz";
        String updatedApiKey = "$2a$10$aRR0bHa8rtzP5jRcE72vp.hRFsGQz4MGIs62oogLbfOCFK3.RIESG";
        trendzSettings.setBaseUrl(updatedUrl);
        trendzSettings.setApiKey(updatedApiKey);

        doPost("/api/trendz/settings", trendzSettings).andExpect(status().isOk());

        TrendzSettings updatedTrendzSettings = doGet("/api/trendz/settings", TrendzSettings.class);
        assertThat(updatedTrendzSettings).isEqualTo(trendzSettings);
    }

    @Test
    public void testTrendzSettingsWhenCustomer() throws Exception {
        loginCustomerUser();

        TrendzSettings newTrendzSettings = new TrendzSettings();
        newTrendzSettings.setEnabled(true);
        newTrendzSettings.setBaseUrl("https://some.domain.com:18888/customer_trendz");
        newTrendzSettings.setApiKey("some_api_key");

        doPost("/api/trendz/settings", newTrendzSettings).andExpect(status().isForbidden());

        TrendzSettings fetchedTrendzSettings = doGet("/api/trendz/settings", TrendzSettings.class);
        assertThat(fetchedTrendzSettings).isNotNull();
        assertThat(fetchedTrendzSettings.isEnabled()).isTrue();
        assertThat(fetchedTrendzSettings.getBaseUrl()).isEqualTo(trendzUrl);
        assertThat(fetchedTrendzSettings.getApiKey()).isEqualTo(apiKey);
    }
}
