/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.cf;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.cf.CalculatedFieldLinkDao;
import org.thingsboard.server.dao.model.sql.CalculatedFieldLinkEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaCalculatedFieldLinkDao extends JpaAbstractDao<CalculatedFieldLinkEntity, CalculatedFieldLink> implements CalculatedFieldLinkDao {

    private final CalculatedFieldLinkRepository calculatedFieldLinkRepository;
    private final NativeCalculatedFieldRepository nativeCalculatedFieldRepository;

    @Override
    public List<CalculatedFieldLink> findCalculatedFieldLinksByCalculatedFieldId(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        return DaoUtil.convertDataList(calculatedFieldLinkRepository.findAllByTenantIdAndCalculatedFieldId(tenantId.getId(), calculatedFieldId.getId()));
    }

    @Override
    public List<CalculatedFieldLink> findCalculatedFieldLinksByEntityId(TenantId tenantId, EntityId entityId) {
        return DaoUtil.convertDataList(calculatedFieldLinkRepository.findAllByTenantIdAndEntityId(tenantId.getId(), entityId.getId()));
    }

    @Override
    public ListenableFuture<List<CalculatedFieldLink>> findCalculatedFieldLinksByCalculatedFieldIdAsync(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        return service.submit(() -> findCalculatedFieldLinksByCalculatedFieldId(tenantId, calculatedFieldId));
    }

    @Override
    public List<CalculatedFieldLink> findAll() {
        return DaoUtil.convertDataList(calculatedFieldLinkRepository.findAll());
    }

    @Override
    public PageData<CalculatedFieldLink> findAll(PageLink pageLink) {
        log.debug("Try to find calculated field links by pageLink [{}]", pageLink);
        return nativeCalculatedFieldRepository.findCalculatedFieldLinks(DaoUtil.toPageable(pageLink));
    }

    @Override
    protected Class<CalculatedFieldLinkEntity> getEntityClass() {
        return CalculatedFieldLinkEntity.class;
    }

    @Override
    protected JpaRepository<CalculatedFieldLinkEntity, UUID> getRepository() {
        return calculatedFieldLinkRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD_LINK;
    }

}
