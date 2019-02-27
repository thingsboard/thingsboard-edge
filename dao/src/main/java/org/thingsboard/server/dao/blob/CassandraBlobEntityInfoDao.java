/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.blob;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.BlobEntityInfoEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.*;

@Component
@Slf4j
@NoSqlDao
public class CassandraBlobEntityInfoDao extends CassandraAbstractSearchTimeDao<BlobEntityInfoEntity, BlobEntityInfo> implements BlobEntityInfoDao {

    @Override
    protected Class<BlobEntityInfoEntity> getColumnFamilyClass() {
        return BlobEntityInfoEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return BLOB_ENTITY_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<BlobEntityInfo> findBlobEntitiesByTenantId(UUID tenantId, TimePageLink pageLink) {
        log.trace("Try to find blob entities by tenant [{}] and pageLink [{}]", tenantId, pageLink);
        List<BlobEntityInfoEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), BLOB_ENTITY_BY_TENANT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.BLOB_ENTITY_TENANT_ID_PROPERTY, tenantId)),
                pageLink);
        log.trace("Found blob entities by tenant [{}] and pageLink [{}]", tenantId, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<BlobEntityInfo> findBlobEntitiesByTenantIdAndType(UUID tenantId, String type, TimePageLink pageLink) {
        log.trace("Try to find blob entities by tenant [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        List<BlobEntityInfoEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), BLOB_ENTITY_BY_TENANT_AND_TYPE_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.BLOB_ENTITY_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.BLOB_ENTITY_TYPE_PROPERTY, type)),
                pageLink.isAscOrder() ? QueryBuilder.asc(ModelConstants.BLOB_ENTITY_TYPE_PROPERTY) :
                        QueryBuilder.desc(ModelConstants.BLOB_ENTITY_TYPE_PROPERTY),
                pageLink);
        log.trace("Found blob entities by tenant [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<BlobEntityInfo> findBlobEntitiesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TimePageLink pageLink) {
        log.trace("Try to find blob entities by tenant [{}], customer [{}] and pageLink [{}]", tenantId, customerId, pageLink);
        List<BlobEntityInfoEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), BLOB_ENTITY_BY_CUSTOMER_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.BLOB_ENTITY_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.BLOB_ENTITY_CUSTOMER_ID_PROPERTY, customerId)),
                pageLink);
        log.trace("Found blob entities by tenant [{}], customer [{}] and pageLink [{}]", tenantId, customerId, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<BlobEntityInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TimePageLink pageLink) {
        log.trace("Try to find blob entities by tenant [{}], customer [{}], type [{}] and pageLink [{}]", tenantId, customerId, type, pageLink);
        List<BlobEntityInfoEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), BLOB_ENTITY_BY_CUSTOMER_AND_TYPE_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.BLOB_ENTITY_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.BLOB_ENTITY_CUSTOMER_ID_PROPERTY, customerId),
                        eq(ModelConstants.BLOB_ENTITY_TYPE_PROPERTY, type)),
                pageLink.isAscOrder() ? QueryBuilder.asc(ModelConstants.BLOB_ENTITY_TYPE_PROPERTY) :
                        QueryBuilder.desc(ModelConstants.BLOB_ENTITY_TYPE_PROPERTY),
                pageLink);
        log.trace("Found blob entities by tenant [{}], customer [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public ListenableFuture<List<BlobEntityInfo>> findBlobEntitiesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> blobEntityIds) {
        log.debug("Try to find blob entities by tenantId [{}] and blob entity Ids [{}]", tenantId, blobEntityIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(BLOB_ENTITY_TENANT_ID_PROPERTY, tenantId));
        query.and(in(ID_PROPERTY, blobEntityIds));
        return findListByStatementAsync(new TenantId(tenantId), query);
    }
}
