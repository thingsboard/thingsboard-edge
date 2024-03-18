/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
