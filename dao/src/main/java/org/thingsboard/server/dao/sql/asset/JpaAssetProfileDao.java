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
package org.thingsboard.server.dao.sql.asset;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.model.sql.AssetProfileEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaAssetProfileDao extends JpaAbstractSearchTextDao<AssetProfileEntity, AssetProfile> implements AssetProfileDao {

    @Autowired
    private AssetProfileRepository assetProfileRepository;

    @Override
    protected Class<AssetProfileEntity> getEntityClass() {
        return AssetProfileEntity.class;
    }

    @Override
    protected JpaRepository<AssetProfileEntity, UUID> getRepository() {
        return assetProfileRepository;
    }

    @Override
    public AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, UUID assetProfileId) {
        return assetProfileRepository.findAssetProfileInfoById(assetProfileId);
    }

    @Transactional
    @Override
    public AssetProfile saveAndFlush(TenantId tenantId, AssetProfile assetProfile) {
        AssetProfile result = save(tenantId, assetProfile);
        assetProfileRepository.flush();
        return result;
    }

    @Override
    public PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetProfileRepository.findAssetProfiles(
                        tenantId.getId(),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                assetProfileRepository.findAssetProfileInfos(
                        tenantId.getId(),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<AssetProfileInfo>> findAssetProfilesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetProfileIds) {
        return service.submit(() -> assetProfileRepository.findAssetProfileInfosByTenantIdAndIdIn(tenantId, assetProfileIds));
    }

    @Override
    public AssetProfile findDefaultAssetProfile(TenantId tenantId) {
        return DaoUtil.getData(assetProfileRepository.findByDefaultTrueAndTenantId(tenantId.getId()));
    }

    @Override
    public AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId) {
        return assetProfileRepository.findDefaultAssetProfileInfo(tenantId.getId());
    }

    @Override
    public AssetProfile findByName(TenantId tenantId, String profileName) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndName(tenantId.getId(), profileName));
    }

    @Override
    public AssetProfile findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public AssetProfile findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public PageData<AssetProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findAssetProfiles(TenantId.fromUUID(tenantId), pageLink);
    }

    @Override
    public AssetProfileId getExternalIdByInternal(AssetProfileId internalId) {
        return Optional.ofNullable(assetProfileRepository.getExternalIdById(internalId.getId()))
                .map(AssetProfileId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET_PROFILE;
    }

}
