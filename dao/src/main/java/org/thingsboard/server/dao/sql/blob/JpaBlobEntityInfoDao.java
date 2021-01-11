/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.blob;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.blob.BlobEntityInfoDao;
import org.thingsboard.server.dao.model.sql.BlobEntityInfoEntity;
import org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Component
public class JpaBlobEntityInfoDao extends JpaAbstractDao<BlobEntityInfoEntity, BlobEntityInfo> implements BlobEntityInfoDao {

    @Autowired
    BlobEntityInfoRepository blobEntityInfoRepository;

    @Override
    protected Class<BlobEntityInfoEntity> getEntityClass() {
        return BlobEntityInfoEntity.class;
    }

    @Override
    protected CrudRepository<BlobEntityInfoEntity, UUID> getCrudRepository() {
        return blobEntityInfoRepository;
    }

    @Override
    public BlobEntityWithCustomerInfo findBlobEntityWithCustomerInfoById(UUID tenantId, UUID blobEntityId) {
        return DaoUtil.getData(blobEntityInfoRepository.findBlobEntityWithCustomerInfoById(blobEntityId));
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantId(UUID tenantId, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                blobEntityInfoRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        DaoUtil.toPageable(pageLink, BlobEntityWithCustomerInfoEntity.blobEntityWithCustomerInfoColumnMap)));
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndType(UUID tenantId, String type, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                blobEntityInfoRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        DaoUtil.toPageable(pageLink, BlobEntityWithCustomerInfoEntity.blobEntityWithCustomerInfoColumnMap)));
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                blobEntityInfoRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        DaoUtil.toPageable(pageLink, BlobEntityWithCustomerInfoEntity.blobEntityWithCustomerInfoColumnMap)));
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                blobEntityInfoRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        DaoUtil.toPageable(pageLink, BlobEntityWithCustomerInfoEntity.blobEntityWithCustomerInfoColumnMap)));
    }

    @Override
    public ListenableFuture<List<BlobEntityInfo>> findBlobEntitiesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> blobEntityIds) {
        return service.submit(() -> DaoUtil.convertDataList(blobEntityInfoRepository.findBlobEntitiesByTenantIdAndIdIn(
                tenantId, blobEntityIds)));
    }
}
