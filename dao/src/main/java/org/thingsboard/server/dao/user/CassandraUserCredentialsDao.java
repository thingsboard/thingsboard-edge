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
package org.thingsboard.server.dao.user;

import com.datastax.driver.core.querybuilder.Select.Where;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.UserCredentialsEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractModelDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Component
@Slf4j
@NoSqlDao
public class CassandraUserCredentialsDao extends CassandraAbstractModelDao<UserCredentialsEntity, UserCredentials> implements UserCredentialsDao {

    public static final String EXECUTE_QUERY = "Execute query {}";

    @Override
    protected Class<UserCredentialsEntity> getColumnFamilyClass() {
        return UserCredentialsEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.USER_CREDENTIALS_COLUMN_FAMILY_NAME;
    }

    @Override
    public UserCredentials findByUserId(UUID userId) {
        log.debug("Try to find user credentials by userId [{}] ", userId);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_USER_COLUMN_FAMILY_NAME).where(eq(ModelConstants.USER_CREDENTIALS_USER_ID_PROPERTY, userId));
        log.trace(EXECUTE_QUERY, query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(query);
        log.trace("Found user credentials [{}] by userId [{}]", userCredentialsEntity, userId);
        return DaoUtil.getData(userCredentialsEntity);
    }

    @Override
    public UserCredentials findByActivateToken(String activateToken) {
        log.debug("Try to find user credentials by activateToken [{}] ", activateToken);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_ACTIVATE_TOKEN_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY, activateToken));
        log.trace(EXECUTE_QUERY, query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(query);
        log.trace("Found user credentials [{}] by activateToken [{}]", userCredentialsEntity, activateToken);
        return DaoUtil.getData(userCredentialsEntity);
    }

    @Override
    public UserCredentials findByResetToken(String resetToken) {
        log.debug("Try to find user credentials by resetToken [{}] ", resetToken);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_RESET_TOKEN_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.USER_CREDENTIALS_RESET_TOKEN_PROPERTY, resetToken));
        log.trace(EXECUTE_QUERY, query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(query);
        log.trace("Found user credentials [{}] by resetToken [{}]", userCredentialsEntity, resetToken);
        return DaoUtil.getData(userCredentialsEntity);
    }

}
