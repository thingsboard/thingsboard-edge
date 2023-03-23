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
package org.thingsboard.server.dao.sql.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.group.EntityGroupInfoDao;
import org.thingsboard.server.dao.model.sql.EntityGroupInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@SqlDao
public class JpaEntityGroupInfoDao extends JpaAbstractDao<EntityGroupInfoEntity, EntityGroupInfo> implements EntityGroupInfoDao {

    @Autowired
    private EntityGroupInfoRepository entityGroupInfoRepository;

    @Override
    protected Class<EntityGroupInfoEntity> getEntityClass() {
        return EntityGroupInfoEntity.class;
    }

    @Override
    protected JpaRepository<EntityGroupInfoEntity, UUID> getRepository() {
        return entityGroupInfoRepository;
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupsByType(UUID tenantId, UUID parentEntityId,
                                                            EntityType parentEntityType, EntityType groupType, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository
                .findEntityGroupsByType(
                        parentEntityId,
                        parentEntityType,
                        groupType,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupsByOwnerIdsAndType(UUID tenantId, List<UUID> ownerIds, EntityType groupType, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository
                .findEntityGroupsByOwnerIdsAndType(
                        ownerIds,
                        groupType,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupsByIds(UUID tenantId, List<UUID> entityGroupIds, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository
                .findEntityGroupsByIds(
                        entityGroupIds,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupsByTypeOrIds(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType,
                                                                 List<UUID> entityGroupIds, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository
                .findEntityGroupsByTypeOrIds(
                        parentEntityId,
                        parentEntityType,
                        groupType,
                        entityGroupIds,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroupInfo> findEdgeEntityGroupsByOwnerIdAndType(UUID tenantId, UUID edgeId, UUID ownerId, EntityType ownerType, String relationType, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupInfoRepository.findEdgeEntityGroupsByOwnerIdAndType(
                edgeId,
                ownerId.toString(),
                ownerType.name(),
                relationType,
                Objects.toString(pageLink.getTextSearch(), ""),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<EntityGroupInfo> findEntityGroupByTypeAndName(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType, String name) {
        return Optional.ofNullable(DaoUtil.getData(entityGroupInfoRepository.findEntityGroupByTypeAndName(
                parentEntityId,
                parentEntityType,
                groupType,
                name)));
    }
}
