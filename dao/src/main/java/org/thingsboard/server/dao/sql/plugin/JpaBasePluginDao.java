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
package org.thingsboard.server.dao.sql.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.PluginMetaDataEntity;
import org.thingsboard.server.dao.plugin.PluginDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 5/1/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaBasePluginDao extends JpaAbstractSearchTextDao<PluginMetaDataEntity, PluginMetaData> implements PluginDao {

    public static final String SEARCH_RESULT = "Search result: [{}]";
    @Autowired
    private PluginMetaDataRepository pluginMetaDataRepository;

    @Override
    protected Class<PluginMetaDataEntity> getEntityClass() {
        return PluginMetaDataEntity.class;
    }

    @Override
    protected CrudRepository<PluginMetaDataEntity, String> getCrudRepository() {
        return pluginMetaDataRepository;
    }

    @Override
    public PluginMetaData findById(PluginId pluginId) {
        log.debug("Search plugin meta-data entity by id [{}]", pluginId);
        PluginMetaData pluginMetaData = super.findById(pluginId.getId());
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for plugin entity [{}]", pluginMetaData != null, pluginMetaData);
        } else {
            log.debug(SEARCH_RESULT, pluginMetaData != null);
        }
        return pluginMetaData;
    }

    @Override
    public PluginMetaData findByApiToken(String apiToken) {
        log.debug("Search plugin meta-data entity by api token [{}]", apiToken);
        PluginMetaDataEntity entity = pluginMetaDataRepository.findByApiToken(apiToken);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for plugin entity [{}]", entity != null, entity);
        } else {
            log.debug(SEARCH_RESULT, entity != null);
        }
        return DaoUtil.getData(entity);
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Delete plugin meta-data entity by id [{}]", id);
        pluginMetaDataRepository.delete(UUIDConverter. fromTimeUUID(id));
    }

    @Override
    public void deleteById(PluginId pluginId) {
        deleteById(pluginId.getId());
    }

    @Override
    public List<PluginMetaData> findByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.debug("Try to find plugins by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<PluginMetaDataEntity> entities = pluginMetaDataRepository
                .findByTenantIdAndPageLink(
                        UUIDConverter.fromTimeUUID(tenantId.getId()),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR :  UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit()));
        if (log.isTraceEnabled()) {
            log.trace(SEARCH_RESULT, Arrays.toString(entities.toArray()));
        } else {
            log.debug(SEARCH_RESULT, entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<PluginMetaData> findAllTenantPluginsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find all tenant plugins by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<PluginMetaDataEntity> entities = pluginMetaDataRepository
                .findAllTenantPluginsByTenantId(
                        UUIDConverter.fromTimeUUID(tenantId),
                        NULL_UUID_STR,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR :  UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit()));
        if (log.isTraceEnabled()) {
            log.trace(SEARCH_RESULT, Arrays.toString(entities.toArray()));
        } else {
            log.debug(SEARCH_RESULT, entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }
}
