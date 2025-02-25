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
package org.thingsboard.server.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.queue.util.TbIntegrationExecutorComponent;
import org.thingsboard.server.service.integration.IntegrationConfigurationService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@TbIntegrationExecutorComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultIntegrationExecutorTenantProfileCache implements IntegrationExecutorTenantProfileCache {

    private final IntegrationConfigurationService integrationConfigurationService;

    private final ConcurrentMap<TenantProfileId, TenantProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TenantProfileId> tenantsProfiles = new ConcurrentHashMap<>();
    private final Lock tenantProfileFetchLock = new ReentrantLock();

    @Override
    public TenantProfile get(TenantId tenantId) {
        TenantProfile profile = null;
        TenantProfileId tenantProfileId = tenantsProfiles.get(tenantId);
        if (tenantProfileId != null) {
            profile = profiles.get(tenantProfileId);
        }
        if (profile == null) {
            tenantProfileFetchLock.lock();
            try {
                tenantProfileId = tenantsProfiles.get(tenantId);
                if (tenantProfileId != null) {
                    profile = profiles.get(tenantProfileId);
                }
                if (profile == null) {
                    profile = integrationConfigurationService.getTenantProfile(tenantId);
                    log.trace("Fetched tenant profile for tenant {}: {}", tenantId, profile);
                    if (profile != null) {
                        profiles.put(profile.getId(), profile);
                        tenantsProfiles.put(tenantId, profile.getId());
                    }
                }
            } finally {
                tenantProfileFetchLock.unlock();
            }
        }
        return profile;
    }

    @Override
    public void evict(TenantProfileId profileId) {
        tenantsProfiles.values().removeIf(profileId::equals);
        profiles.remove(profileId);
        log.debug("Evicted tenant profile by id {}", profileId);
    }

    @Override
    public void evict(TenantId tenantId) {
        tenantsProfiles.remove(tenantId);
        log.debug("Evicted tenant profile for tenant {}", tenantId);
    }

}
