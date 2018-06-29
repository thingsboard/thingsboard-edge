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
package org.thingsboard.server.dao.user;

import com.datastax.driver.core.querybuilder.Select.Where;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.UserEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Component
@Slf4j
@NoSqlDao
public class CassandraUserDao extends CassandraAbstractSearchTextDao<UserEntity, User> implements UserDao {

    @Override
    protected Class<UserEntity> getColumnFamilyClass() {
        return UserEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.USER_COLUMN_FAMILY_NAME;
    }

    @Override
    public User findByEmail(String email) {
        log.debug("Try to find user by email [{}] ", email);
        Where query = select().from(ModelConstants.USER_BY_EMAIL_COLUMN_FAMILY_NAME).where(eq(ModelConstants.USER_EMAIL_PROPERTY, email));
        log.trace("Execute query {}", query);
        UserEntity userEntity = findOneByStatement(query);
        log.trace("Found user [{}] by email [{}]", userEntity, email);
        return DaoUtil.getData(userEntity);
    }

    @Override
    public List<User> findTenantAdmins(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find tenant admin users by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<UserEntity> userEntities = findPageWithTextSearch(ModelConstants.USER_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.USER_TENANT_ID_PROPERTY, tenantId),
                              eq(ModelConstants.USER_CUSTOMER_ID_PROPERTY, ModelConstants.NULL_UUID),
                              eq(ModelConstants.USER_AUTHORITY_PROPERTY, Authority.TENANT_ADMIN.name())),
                pageLink); 
        log.trace("Found tenant admin users [{}] by tenantId [{}] and pageLink [{}]", userEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(userEntities);
    }

    @Override
    public List<User> findCustomerUsers(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        log.debug("Try to find customer users by tenantId [{}], customerId [{}] and pageLink [{}]", tenantId, customerId, pageLink);
        List<UserEntity> userEntities = findPageWithTextSearch(ModelConstants.USER_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.USER_TENANT_ID_PROPERTY, tenantId),
                              eq(ModelConstants.USER_CUSTOMER_ID_PROPERTY, customerId),
                              eq(ModelConstants.USER_AUTHORITY_PROPERTY, Authority.CUSTOMER_USER.name())),
                pageLink); 
        log.trace("Found customer users [{}] by tenantId [{}], customerId [{}] and pageLink [{}]", userEntities, tenantId, customerId, pageLink);
        return DaoUtil.convertDataList(userEntities);
    }

    @Override
    public List<User> findAllCustomerUsers(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find customer users by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<UserEntity> userEntities = findPageWithTextSearch(ModelConstants.USER_BY_TENANT_AUTHORITY_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.USER_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.USER_AUTHORITY_PROPERTY, Authority.CUSTOMER_USER.name())),
                pageLink);
        log.trace("Found customer users [{}] by tenantId [{}] and pageLink [{}]", userEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(userEntities);
    }

}
