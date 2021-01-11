/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.dao.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Optional;

public interface IntegrationService {

    Integration saveIntegration(Integration integration);

    Integration findIntegrationById(TenantId tenantId, IntegrationId integrationId);

    ListenableFuture<Integration> findIntegrationByIdAsync(TenantId tenantId, IntegrationId integrationId);

    ListenableFuture<List<Integration>> findIntegrationsByIdsAsync(TenantId tenantId, List<IntegrationId> integrationIds);

    Optional<Integration> findIntegrationByRoutingKey(TenantId tenantId, String routingKey);

    List<Integration> findAllIntegrations(TenantId tenantId);

    List<Integration> findIntegrationsByConverterId(TenantId tenantId, ConverterId converterId);

    PageData<Integration> findTenantIntegrations(TenantId tenantId, PageLink pageLink);

    void deleteIntegration(TenantId tenantId, IntegrationId integrationId);

    void deleteIntegrationsByTenantId(TenantId tenantId);

}
