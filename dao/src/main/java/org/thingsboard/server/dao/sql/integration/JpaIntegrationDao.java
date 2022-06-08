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
package org.thingsboard.server.dao.sql.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.dao.model.sql.IntegrationEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Component
public class JpaIntegrationDao extends JpaAbstractSearchTextDao<IntegrationEntity, Integration> implements IntegrationDao {

    @Autowired
    private IntegrationRepository integrationRepository;

    @Override
    public PageData<Integration> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                integrationRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<Integration> findByRoutingKey(UUID tenantId, String routingKey) {
        Integration integration = DaoUtil.getData(integrationRepository.findByRoutingKey(routingKey));
        return Optional.ofNullable(integration);
    }

    @Override
    public List<Integration> findByConverterId(UUID tenantId, UUID converterId) {
        return DaoUtil.convertDataList(integrationRepository.findByConverterId(converterId));
    }

    @Override
    public boolean existsByUplinkOrDownlinkConverterId(UUID tenantId, UUID converterId) {
        return integrationRepository.existsByConverterIdOrDownlinkConverterId(converterId, converterId);
    }

    @Override
    public ListenableFuture<List<Integration>> findIntegrationsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> integrationIds) {
        return service.submit(() -> DaoUtil.convertDataList(integrationRepository.findIntegrationsByTenantIdAndIdIn(tenantId, integrationIds)));
    }

    @Override
    protected Class<IntegrationEntity> getEntityClass() {
        return IntegrationEntity.class;
    }

    @Override
    protected JpaRepository<IntegrationEntity, UUID> getRepository() {
        return integrationRepository;
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return integrationRepository.countByTenantId(tenantId.getId());
    }
}
