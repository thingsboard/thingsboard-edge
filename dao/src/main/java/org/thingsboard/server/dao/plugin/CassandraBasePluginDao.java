/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.dao.plugin;

import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.PluginMetaDataEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@Slf4j
@NoSqlDao
public class CassandraBasePluginDao extends CassandraAbstractSearchTextDao<PluginMetaDataEntity, PluginMetaData> implements PluginDao {

    @Override
    protected Class<PluginMetaDataEntity> getColumnFamilyClass() {
        return PluginMetaDataEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.PLUGIN_COLUMN_FAMILY_NAME;
    }

    @Override
    public PluginMetaData findById(PluginId pluginId) {
        log.debug("Search plugin meta-data entity by id [{}]", pluginId);
        PluginMetaData pluginMetaData = super.findById(pluginId.getId());
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for plugin entity [{}]", pluginMetaData != null, pluginMetaData);
        } else {
            log.debug("Search result: [{}]", pluginMetaData != null);
        }
        return pluginMetaData;
    }

    @Override
    public PluginMetaData findByApiToken(String apiToken) {
        log.debug("Search plugin meta-data entity by api token [{}]", apiToken);
        Select.Where query = select().from(ModelConstants.PLUGIN_BY_API_TOKEN_COLUMN_FAMILY_NAME).where(eq(ModelConstants.PLUGIN_API_TOKEN_PROPERTY, apiToken));
        log.trace("Execute query [{}]", query);
        PluginMetaDataEntity entity = findOneByStatement(query);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for plugin entity [{}]", entity != null, entity);
        } else {
            log.debug("Search result: [{}]", entity != null);
        }
        return DaoUtil.getData(entity);
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Delete plugin meta-data entity by id [{}]", id);
        boolean result = removeById(id);
        log.debug("Delete result: [{}]", result);
    }

    @Override
    public void deleteById(PluginId pluginId) {
        deleteById(pluginId.getId());
    }

    @Override
    public List<PluginMetaData> findByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.debug("Try to find plugins by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<PluginMetaDataEntity> entities = findPageWithTextSearch(ModelConstants.PLUGIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.PLUGIN_TENANT_ID_PROPERTY, tenantId.getId())), pageLink);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<PluginMetaData> findAllTenantPluginsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find all tenant plugins by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<PluginMetaDataEntity> pluginEntities = findPageWithTextSearch(ModelConstants.PLUGIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(in(ModelConstants.PLUGIN_TENANT_ID_PROPERTY, Arrays.asList(NULL_UUID, tenantId))),
                pageLink);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(pluginEntities.toArray()));
        } else {
            log.debug("Search result: [{}]", pluginEntities.size());
        }
        return DaoUtil.convertDataList(pluginEntities);
    }

}
