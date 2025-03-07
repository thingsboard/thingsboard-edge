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

import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.sync.vc.GitVersionControlQueueService;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class RepositorySettingsTest extends AbstractControllerTest {

    @MockBean
    private GitVersionControlQueueService gitVersionControlQueueService;

    @Test
    public void testFindRepositorySettings() throws Exception {
        loginTenantAdmin();
        doGet("/api/admin/repositorySettings")
                .andExpect(status().isNotFound());

        String testRepositoryUri = "https://github.com/test/version-control-test-repository.git";

        SettableFuture<Void> successFuture = SettableFuture.create();
        successFuture.set(null);
        when(gitVersionControlQueueService.initRepository(any(), any()))
                .thenReturn(successFuture);

        RepositorySettings repositorySettings = new RepositorySettings();
        repositorySettings.setPassword("test");
        repositorySettings.setAuthMethod(RepositoryAuthMethod.USERNAME_PASSWORD);
        repositorySettings.setRepositoryUri(testRepositoryUri);
        repositorySettings.setDefaultBranch("main");
        doPost("/api/admin/repositorySettings", repositorySettings)
                .andExpect(status().isOk());

        // check repository settings
        doGet("/api/admin/repositorySettings")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.repositoryUri", is(testRepositoryUri)));

        // delete settings
        when(gitVersionControlQueueService.clearRepository(any()))
                .thenReturn(successFuture);
        doDelete("/api/admin/repositorySettings")
                .andExpect(status().isOk());

        // check repository settings
        doGet("/api/admin/repositorySettings")
                .andExpect(status().isNotFound());
    }

}
