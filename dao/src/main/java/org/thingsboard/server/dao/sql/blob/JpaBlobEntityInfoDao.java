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
package org.thingsboard.server.dao.sql.blob;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
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
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Component
@SqlDao
public class JpaBlobEntityInfoDao extends JpaAbstractDao<BlobEntityInfoEntity, BlobEntityInfo> implements BlobEntityInfoDao {

    @Autowired
    BlobEntityInfoRepository blobEntityInfoRepository;

    @Override
    protected Class<BlobEntityInfoEntity> getEntityClass() {
        return BlobEntityInfoEntity.class;
    }

    @Override
    protected JpaRepository<BlobEntityInfoEntity, UUID> getRepository() {
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
