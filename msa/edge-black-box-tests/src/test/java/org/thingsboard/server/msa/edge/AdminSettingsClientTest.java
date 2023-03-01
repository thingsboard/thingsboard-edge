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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Optional;

@Slf4j
public class AdminSettingsClientTest extends AbstractContainerTest {

    @Test
    public void testTenantAdminSettings() {
        verifyTenantAdminSettingsByKey("general");
        verifyTenantAdminSettingsByKey("mailTemplates");
        verifyTenantAdminSettingsByKey("mail");

        // TODO: @voba - verify admin setting in next release. In the current there is no sysadmin on edge to fetch it
        // login as sysadmin on edge
        // login as sysadmin on cloud
        // verifyAdminSettingsByKey("general");
        // verifyAdminSettingsByKey("mailTemplates");
        // verifyAdminSettingsByKey("mail");
    }

    private void verifyTenantAdminSettingsByKey(String key) {
        Optional<AdminSettings> edgeAdminSettings = edgeRestClient.getAdminSettings(key);
        Assert.assertTrue("Admin settings is not available on edge, key = " + key, edgeAdminSettings.isPresent());
        Optional<AdminSettings> cloudAdminSettings = cloudRestClient.getAdminSettings(key);
        Assert.assertTrue("Admin settings is not available on cloud, key = " + key, cloudAdminSettings.isPresent());
        Assert.assertEquals("Admin settings on cloud and edge are different", edgeAdminSettings.get(), cloudAdminSettings.get());
    }

}

