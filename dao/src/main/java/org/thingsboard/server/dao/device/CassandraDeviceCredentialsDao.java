/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.dao.device;

import com.datastax.driver.core.querybuilder.Select.Where;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.DeviceCredentialsEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractModelDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Component
@Slf4j
@NoSqlDao
public class CassandraDeviceCredentialsDao extends CassandraAbstractModelDao<DeviceCredentialsEntity, DeviceCredentials> implements DeviceCredentialsDao {

    @Override
    protected Class<DeviceCredentialsEntity> getColumnFamilyClass() {
        return DeviceCredentialsEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.DEVICE_CREDENTIALS_COLUMN_FAMILY_NAME;
    }

    @Override
    public DeviceCredentials findByDeviceId(UUID deviceId) {
        log.debug("Try to find device credentials by deviceId [{}] ", deviceId);
        Where query = select().from(ModelConstants.DEVICE_CREDENTIALS_BY_DEVICE_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.DEVICE_CREDENTIALS_DEVICE_ID_PROPERTY, deviceId));
        log.trace("Execute query {}", query);
        DeviceCredentialsEntity deviceCredentialsEntity = findOneByStatement(query);
        log.trace("Found device credentials [{}] by deviceId [{}]", deviceCredentialsEntity, deviceId);
        return DaoUtil.getData(deviceCredentialsEntity);
    }
    
    @Override
    public DeviceCredentials findByCredentialsId(String credentialsId) {
        log.debug("Try to find device credentials by credentialsId [{}] ", credentialsId);
        Where query = select().from(ModelConstants.DEVICE_CREDENTIALS_BY_CREDENTIALS_ID_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.DEVICE_CREDENTIALS_CREDENTIALS_ID_PROPERTY, credentialsId));
        log.trace("Execute query {}", query);
        DeviceCredentialsEntity deviceCredentialsEntity = findOneByStatement(query);
        log.trace("Found device credentials [{}] by credentialsId [{}]", deviceCredentialsEntity, credentialsId);
        return DaoUtil.getData(deviceCredentialsEntity);
    }
}
