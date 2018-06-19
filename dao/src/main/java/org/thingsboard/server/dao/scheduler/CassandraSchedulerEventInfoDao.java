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
package org.thingsboard.server.dao.scheduler;

import com.datastax.driver.core.querybuilder.Select;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.SchedulerEventInfoEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.*;

@Component
@NoSqlDao
public class CassandraSchedulerEventInfoDao extends CassandraAbstractSearchTextDao<SchedulerEventInfoEntity, SchedulerEventInfo> implements SchedulerEventInfoDao {

    @Override
    protected Class<SchedulerEventInfoEntity> getColumnFamilyClass() {
        return SchedulerEventInfoEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return SCHEDULER_EVENT_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantId(UUID tenantId) {
        Select select = select().from(SCHEDULER_EVENT_BY_TENANT_COLUMN_FAMILY_NAME);
        Select.Where query = select.where();
        query.and(eq(SCHEDULER_EVENT_TENANT_ID_PROPERTY, tenantId));
        return DaoUtil.convertDataList(findListByStatement(query));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndType(UUID tenantId, String type) {
        Select select = select().from(SCHEDULER_EVENT_BY_TENANT_AND_TYPE_COLUMN_FAMILY_NAME);
        Select.Where query = select.where();
        query.and(eq(SCHEDULER_EVENT_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(SCHEDULER_EVENT_TYPE_PROPERTY, type));
        return DaoUtil.convertDataList(findListByStatement(query));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndCustomerId(UUID tenantId, UUID customerId) {
        Select select = select().from(SCHEDULER_EVENT_BY_CUSTOMER_COLUMN_FAMILY_NAME);
        Select.Where query = select.where();
        query.and(eq(SCHEDULER_EVENT_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY, customerId));
        return DaoUtil.convertDataList(findListByStatement(query));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type) {
        Select select = select().from(SCHEDULER_EVENT_BY_CUSTOMER_AND_TYPE_COLUMN_FAMILY_NAME);
        Select.Where query = select.where();
        query.and(eq(SCHEDULER_EVENT_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY, customerId));
        query.and(eq(SCHEDULER_EVENT_TYPE_PROPERTY, type));
        return DaoUtil.convertDataList(findListByStatement(query));
    }
}
