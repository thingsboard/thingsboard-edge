/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.mobile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.mobile.QrCodeSettingsDao;
import org.thingsboard.server.dao.model.sql.QrCodeSettingsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;


@Component
@Slf4j
@SqlDao
public class JpaQrCodeSettingsDao extends JpaAbstractDao<QrCodeSettingsEntity, QrCodeSettings> implements QrCodeSettingsDao {

    @Autowired
    private QrCodeSettingsRepository qrCodeSettingsRepository;


    @Override
    public QrCodeSettings findByTenantId(TenantId tenantId) {
        return DaoUtil.getData(qrCodeSettingsRepository.findByTenantId(tenantId.getId()));
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        qrCodeSettingsRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    protected Class<QrCodeSettingsEntity> getEntityClass() {
        return QrCodeSettingsEntity.class;
    }

    @Override
    protected JpaRepository<QrCodeSettingsEntity, UUID> getRepository() {
        return qrCodeSettingsRepository;
    }
}
