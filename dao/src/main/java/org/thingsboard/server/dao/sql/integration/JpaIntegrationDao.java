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
package org.thingsboard.server.dao.sql.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
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
    public ListenableFuture<List<Integration>> findIntegrationsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> integrationIds) {
        return service.submit(() -> DaoUtil.convertDataList(integrationRepository.findIntegrationsByTenantIdAndIdIn(tenantId, integrationIds)));
    }

    @Override
    protected Class<IntegrationEntity> getEntityClass() {
        return IntegrationEntity.class;
    }

    @Override
    protected CrudRepository<IntegrationEntity, UUID> getCrudRepository() {
        return integrationRepository;
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return integrationRepository.countByTenantId(tenantId.getId());
    }
}
