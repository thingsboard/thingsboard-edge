/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface IntegrationService extends EntityDaoService {

    Integration saveIntegration(Integration integration);

    Integration findIntegrationById(TenantId tenantId, IntegrationId integrationId);

    ListenableFuture<Integration> findIntegrationByIdAsync(TenantId tenantId, IntegrationId integrationId);

    ListenableFuture<List<Integration>> findIntegrationsByIdsAsync(TenantId tenantId, List<IntegrationId> integrationIds);

    Optional<Integration> findIntegrationByRoutingKey(TenantId tenantId, String routingKey);

    List<Integration> findAllIntegrations(TenantId tenantId);

    List<Integration> findIntegrationsByConverterId(TenantId tenantId, ConverterId converterId);

    PageData<Integration> findTenantIntegrations(TenantId tenantId, PageLink pageLink);

    List<Integration> findTenantIntegrationsByName(TenantId tenantId, String name);

    PageData<Integration> findTenantEdgeTemplateIntegrations(TenantId tenantId, PageLink pageLink);

    PageData<IntegrationInfo> findTenantIntegrationInfos(TenantId tenantId, PageLink pageLink, boolean isEdgeTemplate);

    PageData<IntegrationInfo> findTenantIntegrationInfosWithStats(TenantId tenantId, boolean isEdgeTemplate, PageLink pageLink);

    void deleteIntegration(TenantId tenantId, IntegrationId integrationId);

    void deleteIntegrationsByTenantId(TenantId tenantId);

    List<IntegrationInfo> findAllCoreIntegrationInfos(IntegrationType integrationType, boolean remote, boolean enabled);

    Integration assignIntegrationToEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId);

    Integration unassignIntegrationFromEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId, boolean remove);

    PageData<Integration> findIntegrationsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    PageData<IntegrationInfo> findIntegrationInfosByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);
}
