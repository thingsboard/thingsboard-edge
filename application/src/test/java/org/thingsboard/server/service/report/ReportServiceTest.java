/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.report;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.util.limits.RateLimitService;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.UUID;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultReportService.class)
public class ReportServiceTest {
    @MockBean
    protected UserService userService;
    @MockBean
    protected CustomerService customerService;
    @MockBean
    protected JwtTokenFactory jwtTokenFactory;
    @MockBean
    protected UserPermissionsService userPermissionsService;
    @MockBean
    protected RateLimitService rateLimitService;

    @SpyBean
    DefaultReportService service;

    @Test
    public void testGenerateReportUsingNonExistingUser() {
        Mockito.when(userService.findUserById(any(), any())).thenReturn(null);

        ReportConfig reportConfig = new ReportConfig();
        reportConfig.setUserId(UUID.randomUUID().toString());
        reportConfig.setType("pdf");
        reportConfig.setBaseUrl("/testUrl");
        reportConfig.setUseCurrentUserCredentials(false);
        reportConfig.setDashboardId(UUID.randomUUID().toString());

        assertThrows(ThingsboardException.class, () -> service.generateReport(TenantId.SYS_TENANT_ID, reportConfig, "/testUrl", null, null));
    }
}
