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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class TenantClientTest extends AbstractContainerTest {

    @SneakyThrows
    @Test
    public void testUpdateTenant() {
        cloudRestClient.login("sysadmin@thingsboard.org", "sysadmin");

        Tenant tenant = edgeRestClient.getTenantById(edge.getTenantId()).get();

        // update tenant
        tenant.setCountry("Edge Update country: Ukraine");
        cloudRestClient.saveTenant(tenant);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Edge Update country: Ukraine".equals(edgeRestClient.getTenantById(tenant.getId()).get().getCountry()));

        // create new tenant profile
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("New Tenant Profile");
        TenantProfile saveTenantProfile = cloudRestClient.saveTenantProfile(tenantProfile);

        // update tenant with new tenant profile
        tenant.setTenantProfileId(saveTenantProfile.getId());
        cloudRestClient.saveTenant(tenant);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> saveTenantProfile.getId().equals(edgeRestClient.getTenantById(tenant.getId()).get().getTenantProfileId()));

        cloudRestClient.login("tenant@thingsboard.org", "tenant");
    }
}
