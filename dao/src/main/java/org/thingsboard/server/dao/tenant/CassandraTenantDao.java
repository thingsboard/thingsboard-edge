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
package org.thingsboard.server.dao.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.TenantEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_BY_REGION_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_REGION_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraTenantDao extends CassandraAbstractSearchTextDao<TenantEntity, Tenant> implements TenantDao {

    @Override
    protected Class<TenantEntity> getColumnFamilyClass() {
        return TenantEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return TENANT_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<Tenant> findTenantsByRegion(String region, TextPageLink pageLink) {
        log.debug("Try to find tenants by region [{}] and pageLink [{}]", region, pageLink);
        List<TenantEntity> tenantEntities = findPageWithTextSearch(TENANT_BY_REGION_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME, 
                Arrays.asList(eq(TENANT_REGION_PROPERTY, region)), 
                pageLink); 
        log.trace("Found tenants [{}] by region [{}] and pageLink [{}]", tenantEntities, region, pageLink);
        return DaoUtil.convertDataList(tenantEntities);
    }

}
