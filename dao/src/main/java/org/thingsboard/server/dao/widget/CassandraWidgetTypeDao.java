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
package org.thingsboard.server.dao.widget;

import com.datastax.driver.core.querybuilder.Select.Where;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.WidgetTypeEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractModelDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_ALIAS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_BUNDLE_ALIAS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_BY_TENANT_AND_ALIASES_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_TENANT_ID_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraWidgetTypeDao extends CassandraAbstractModelDao<WidgetTypeEntity, WidgetType> implements WidgetTypeDao {

    @Override
    protected Class<WidgetTypeEntity> getColumnFamilyClass() {
        return WidgetTypeEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return WIDGET_TYPE_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias) {
        log.debug("Try to find widget types by tenantId [{}] and bundleAlias [{}]", tenantId, bundleAlias);
        Where query = select().from(WIDGET_TYPE_BY_TENANT_AND_ALIASES_COLUMN_FAMILY_NAME)
                .where()
                .and(eq(WIDGET_TYPE_TENANT_ID_PROPERTY, tenantId))
                .and(eq(WIDGET_TYPE_BUNDLE_ALIAS_PROPERTY, bundleAlias));
        List<WidgetTypeEntity> widgetTypesEntities = findListByStatement(new TenantId(tenantId), query);
        log.trace("Found widget types [{}] by tenantId [{}] and bundleAlias [{}]", widgetTypesEntities, tenantId, bundleAlias);
        return DaoUtil.convertDataList(widgetTypesEntities);
    }

    @Override
    public WidgetType findByTenantIdBundleAliasAndAlias(UUID tenantId, String bundleAlias, String alias) {
        log.debug("Try to find widget type by tenantId [{}], bundleAlias [{}] and alias [{}]", tenantId, bundleAlias, alias);
        Where query = select().from(WIDGET_TYPE_BY_TENANT_AND_ALIASES_COLUMN_FAMILY_NAME)
                .where()
                .and(eq(WIDGET_TYPE_TENANT_ID_PROPERTY, tenantId))
                .and(eq(WIDGET_TYPE_BUNDLE_ALIAS_PROPERTY, bundleAlias))
                .and(eq(WIDGET_TYPE_ALIAS_PROPERTY, alias));
        log.trace("Execute query {}", query);
        WidgetTypeEntity widgetTypeEntity = findOneByStatement(new TenantId(tenantId), query);
        log.trace("Found widget type [{}] by tenantId [{}], bundleAlias [{}] and alias [{}]",
                widgetTypeEntity, tenantId, bundleAlias, alias);
        return DaoUtil.getData(widgetTypeEntity);
    }

}
