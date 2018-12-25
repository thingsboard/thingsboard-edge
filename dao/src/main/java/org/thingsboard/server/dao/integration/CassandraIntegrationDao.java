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
package org.thingsboard.server.dao.integration;

import com.datastax.driver.core.querybuilder.Select;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.IntegrationEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.*;

@Component
@Slf4j
@NoSqlDao
public class CassandraIntegrationDao extends CassandraAbstractSearchTextDao<IntegrationEntity, Integration> implements IntegrationDao {

    @Override
    protected Class<IntegrationEntity> getColumnFamilyClass() {
        return IntegrationEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return INTEGRATION_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<Integration> findByTenantIdAndPageLink(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find integrations by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<IntegrationEntity> integrationEntities = findPageWithTextSearch(new TenantId(tenantId), INTEGRATION_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(INTEGRATION_TENANT_ID_PROPERTY, tenantId)), pageLink);

        log.trace("Found integrations [{}] by tenantId [{}] and pageLink [{}]", integrationEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(integrationEntities);
    }

    @Override
    public Optional<Integration> findByRoutingKey(UUID tenantId, String routingKey) {
        log.debug("Search integration by routing key [{}]", routingKey);
        Select.Where query = select().from(INTEGRATION_BY_ROUTING_KEY_VIEW_NAME).where(eq(INTEGRATION_ROUTING_KEY_PROPERTY, routingKey));
        IntegrationEntity integrationEntity = findOneByStatement(new TenantId(tenantId), query);
        log.trace("Found integration [{}] by routing key [{}]", integrationEntity, routingKey);
        return Optional.ofNullable(DaoUtil.getData(integrationEntity));
    }

    @Override
    public List<Integration> findByConverterId(UUID tenantId, UUID converterId) {
        log.debug("Search integrations by converterId [{}]", converterId);
        Select.Where query = select().from(INTEGRATION_BY_CONVERTER_ID_VIEW_NAME).where(eq(INTEGRATION_CONVERTER_ID_PROPERTY, converterId));
        List<IntegrationEntity> integrationEntities = findListByStatement(new TenantId(tenantId), query);
        log.trace("Found integrations [{}] by converterId [{}]", integrationEntities, converterId);
        return DaoUtil.convertDataList(integrationEntities);
    }

    @Override
    public ListenableFuture<List<Integration>> findIntegrationsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> integrationIds) {
        log.debug("Try to find integrations by tenantId [{}] and integration Ids [{}]", tenantId, integrationIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(INTEGRATION_TENANT_ID_PROPERTY, tenantId));
        query.and(in(ID_PROPERTY, integrationIds));
        return findListByStatementAsync(new TenantId(tenantId), query);
    }

}
