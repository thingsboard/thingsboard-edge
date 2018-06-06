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
package org.thingsboard.server.dao.widget;

import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.WidgetsBundleEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGETS_BUNDLE_ALIAS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGETS_BUNDLE_BY_TENANT_AND_ALIAS_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGETS_BUNDLE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGETS_BUNDLE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGETS_BUNDLE_TENANT_ID_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraWidgetsBundleDao extends CassandraAbstractSearchTextDao<WidgetsBundleEntity, WidgetsBundle> implements WidgetsBundleDao {

    @Override
    protected Class<WidgetsBundleEntity> getColumnFamilyClass() {
        return WidgetsBundleEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return WIDGETS_BUNDLE_COLUMN_FAMILY_NAME;
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias) {
        log.debug("Try to find widgets bundle by tenantId [{}] and alias [{}]", tenantId, alias);
        Select.Where query = select().from(WIDGETS_BUNDLE_BY_TENANT_AND_ALIAS_COLUMN_FAMILY_NAME)
                .where()
                .and(eq(WIDGETS_BUNDLE_TENANT_ID_PROPERTY, tenantId))
                .and(eq(WIDGETS_BUNDLE_ALIAS_PROPERTY, alias));
        log.trace("Execute query {}", query);
        WidgetsBundleEntity widgetsBundleEntity = findOneByStatement(query);
        log.trace("Found widgets bundle [{}] by tenantId [{}] and alias [{}]",
                widgetsBundleEntity, tenantId, alias);
        return DaoUtil.getData(widgetsBundleEntity);
    }

    @Override
    public List<WidgetsBundle> findSystemWidgetsBundles(TextPageLink pageLink) {
        log.debug("Try to find system widgets bundles by pageLink [{}]", pageLink);
        List<WidgetsBundleEntity> widgetsBundlesEntities = findPageWithTextSearch(WIDGETS_BUNDLE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(WIDGETS_BUNDLE_TENANT_ID_PROPERTY, NULL_UUID)),
                pageLink);
        log.trace("Found system widgets bundles [{}] by pageLink [{}]", widgetsBundlesEntities, pageLink);
        return DaoUtil.convertDataList(widgetsBundlesEntities);
    }

    @Override
    public List<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find tenant widgets bundles by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<WidgetsBundleEntity> widgetsBundlesEntities = findPageWithTextSearch(WIDGETS_BUNDLE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(WIDGETS_BUNDLE_TENANT_ID_PROPERTY, tenantId)),
                pageLink);
        log.trace("Found tenant widgets bundles [{}] by tenantId [{}] and pageLink [{}]", widgetsBundlesEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(widgetsBundlesEntities);
    }

    @Override
    public List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find all tenant widgets bundles by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<WidgetsBundleEntity> widgetsBundlesEntities = findPageWithTextSearch(WIDGETS_BUNDLE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(in(WIDGETS_BUNDLE_TENANT_ID_PROPERTY, Arrays.asList(NULL_UUID, tenantId))),
                pageLink);
        log.trace("Found all tenant widgets bundles [{}] by tenantId [{}] and pageLink [{}]", widgetsBundlesEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(widgetsBundlesEntities);
    }

}
