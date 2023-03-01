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
package org.thingsboard.server.dao.sql.rpc;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RpcEntity;
import org.thingsboard.server.dao.rpc.RpcDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaRpcDao extends JpaAbstractDao<RpcEntity, Rpc> implements RpcDao {

    private final RpcRepository rpcRepository;

    @Override
    protected Class<RpcEntity> getEntityClass() {
        return RpcEntity.class;
    }

    @Override
    protected JpaRepository<RpcEntity, UUID> getRepository() {
        return rpcRepository;
    }

    @Override
    public PageData<Rpc> findAllByDeviceId(TenantId tenantId, DeviceId deviceId, PageLink pageLink) {
        return DaoUtil.toPageData(rpcRepository.findAllByTenantIdAndDeviceId(tenantId.getId(), deviceId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        return DaoUtil.toPageData(rpcRepository.findAllByTenantIdAndDeviceIdAndStatus(tenantId.getId(), deviceId.getId(), rpcStatus, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Rpc> findAllRpcByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(rpcRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Long deleteOutdatedRpcByTenantId(TenantId tenantId, Long expirationTime) {
        return rpcRepository.deleteOutdatedRpcByTenantId(tenantId.getId(), expirationTime);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.RPC;
    }

}
