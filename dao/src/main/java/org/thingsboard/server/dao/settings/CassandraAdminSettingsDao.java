/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.settings;

import com.datastax.driver.core.querybuilder.Select.Where;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.AdminSettingsEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractModelDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.ADMIN_SETTINGS_BY_KEY_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ADMIN_SETTINGS_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ADMIN_SETTINGS_KEY_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraAdminSettingsDao extends CassandraAbstractModelDao<AdminSettingsEntity, AdminSettings> implements AdminSettingsDao {

    @Override
    protected Class<AdminSettingsEntity> getColumnFamilyClass() {
        return AdminSettingsEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ADMIN_SETTINGS_COLUMN_FAMILY_NAME;
    }

    @Override
    public AdminSettings findByKey(String key) {
        log.debug("Try to find admin settings by key [{}] ", key);
        Where query = select().from(ADMIN_SETTINGS_BY_KEY_COLUMN_FAMILY_NAME).where(eq(ADMIN_SETTINGS_KEY_PROPERTY, key));
        log.trace("Execute query {}", query);
        AdminSettingsEntity adminSettingsEntity = findOneByStatement(query);
        log.trace("Found admin settings [{}] by key [{}]", adminSettingsEntity, key);
        return DaoUtil.getData(adminSettingsEntity);
    }

}
