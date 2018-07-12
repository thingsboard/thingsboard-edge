/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.dao.customer;

import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.CustomerEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_BY_TENANT_AND_TITLE_VIEW_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_TITLE_PROPERTY;
@Component
@Slf4j
@NoSqlDao
public class CassandraCustomerDao extends CassandraAbstractSearchTextDao<CustomerEntity, Customer> implements CustomerDao {

    @Override
    protected Class<CustomerEntity> getColumnFamilyClass() {
        return CustomerEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.CUSTOMER_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<Customer> findCustomersByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find customers by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<CustomerEntity> customerEntities = findPageWithTextSearch(ModelConstants.CUSTOMER_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.CUSTOMER_TENANT_ID_PROPERTY, tenantId)),
                pageLink); 
        log.trace("Found customers [{}] by tenantId [{}] and pageLink [{}]", customerEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(customerEntities);
    }

    @Override
    public Optional<Customer> findCustomersByTenantIdAndTitle(UUID tenantId, String title) {
        Select select = select().from(CUSTOMER_BY_TENANT_AND_TITLE_VIEW_NAME);
        Select.Where query = select.where();
        query.and(eq(CUSTOMER_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(CUSTOMER_TITLE_PROPERTY, title));
        CustomerEntity customerEntity = findOneByStatement(query);
        Customer customer = DaoUtil.getData(customerEntity);
        return Optional.ofNullable(customer);
    }

}
