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
package org.thingsboard.server.dao.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface IntegrationDao.
 *
 */
public interface IntegrationDao extends Dao<Integration>, TenantEntityDao, ExportableEntityDao<IntegrationId, Integration> {

    /**
     * Find all (core and edge template) integrations by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of integration objects
     */
    PageData<Integration> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find core integrations by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of integration objects
     */
    PageData<Integration> findCoreIntegrationsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find edge template integrations by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of integration objects
     */
    PageData<Integration> findEdgeTemplateIntegrationsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find integrations by routing Key.
     *
     * @param routingKey the integration routingKey
     * @return the optional integration object
     */
    Optional<Integration> findByRoutingKey(UUID tenantId, String routingKey);

    /**
     * Find integrations by converterId.
     *
     * @param converterId the converterId
     * @return the list of integration objects
     */
    List<Integration> findByConverterId(UUID tenantId, UUID converterId);

    /**
     * Find integrations by tenantId and integration Ids.
     *
     * @param tenantId the tenantId
     * @param integrationIds the integration Ids
     * @return the list of integration objects
     */
    ListenableFuture<List<Integration>> findIntegrationsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> integrationIds);

    List<Integration> findTenantIntegrationsByName(UUID tenantId, String name);

    /**
     * Find integrations by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param pageLink the page link
     * @return the list of integration objects
     */
    PageData<Integration> findIntegrationsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    Long countCoreIntegrations();
}
