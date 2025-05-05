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
package org.thingsboard.server.service.entitiy.dashboard;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.resource.TbResourceRepository;
import org.thingsboard.server.dao.sql.widget.WidgetTypeRepository;
import org.thingsboard.server.dao.sql.widget.WidgetsBundleRepository;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "transport.gateway.dashboard.sync.enabled=true"
})
public class DashboardSyncServiceTest extends AbstractControllerTest {

    @Autowired
    private WidgetTypeRepository widgetTypeRepository;
    @Autowired
    private WidgetsBundleRepository widgetsBundleRepository;
    @Autowired
    private TbResourceRepository resourceRepository;

    @After
    public void after() throws Exception {
        widgetsBundleRepository.deleteAll();
        widgetTypeRepository.deleteAll();
        resourceRepository.deleteAll();
    }

    @Test
    public void testGatewaysDashboardSync() throws Exception {
        loginTenantAdmin();
        await().atMost(45, TimeUnit.SECONDS).untilAsserted(() -> {
            MockHttpServletResponse response = doGet("/api/resource/dashboard/system/gateways_dashboard.json")
                    .andExpect(status().isOk())
                    .andReturn().getResponse();
            String dashboardJson = response.getContentAsString();
            Dashboard dashboard = JacksonUtil.fromString(dashboardJson, Dashboard.class);
            assertThat(dashboard).isNotNull();
            assertThat(dashboard.getTitle()).containsIgnoringCase("gateway");
            assertThat(response.getHeader("ETag")).isNotBlank();
        });
    }

}