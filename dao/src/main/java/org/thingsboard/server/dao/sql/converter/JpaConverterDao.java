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
package org.thingsboard.server.dao.sql.converter;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.model.sql.ConverterEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaConverterDao extends JpaAbstractSearchTextDao<ConverterEntity, Converter> implements ConverterDao {

    @Autowired
    private ConverterRepository converterRepository;

    @Override
    public PageData<Converter> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                converterRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<Converter> findConverterByTenantIdAndName(UUID tenantId, String name) {
        Converter converter = DaoUtil.getData(converterRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(converter);
    }

    @Override
    public ListenableFuture<List<Converter>> findConvertersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> converterIds) {
        return service.submit(() -> DaoUtil.convertDataList(converterRepository.findConvertersByTenantIdAndIdIn(tenantId, converterIds)));
    }

    @Override
    protected Class<ConverterEntity> getEntityClass() {
        return ConverterEntity.class;
    }

    @Override
    protected CrudRepository<ConverterEntity, UUID> getCrudRepository() {
        return converterRepository;
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return converterRepository.countByTenantId(tenantId.getId());
    }
}
