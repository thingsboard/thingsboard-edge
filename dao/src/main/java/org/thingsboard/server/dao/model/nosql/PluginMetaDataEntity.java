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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.ComponentLifecycleStateCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.Objects;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = PLUGIN_COLUMN_FAMILY_NAME)
public class PluginMetaDataEntity implements SearchTextEntity<PluginMetaData> {

    @Transient
    private static final long serialVersionUID = -5231612734979707866L;

    @PartitionKey
    @Column(name = ID_PROPERTY)
    private UUID id;

    @Column(name = PLUGIN_API_TOKEN_PROPERTY)
    private String apiToken;

    @ClusteringColumn
    @Column(name = PLUGIN_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = PLUGIN_NAME_PROPERTY)
    private String name;

    @Column(name = PLUGIN_CLASS_PROPERTY)
    private String clazz;

    @Column(name = PLUGIN_ACCESS_PROPERTY)
    private boolean publicAccess;

    @Column(name = PLUGIN_STATE_PROPERTY, codec = ComponentLifecycleStateCodec.class)
    private ComponentLifecycleState state;

    @Column(name = PLUGIN_CONFIGURATION_PROPERTY, codec = JsonCodec.class)
    private JsonNode configuration;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public PluginMetaDataEntity() {
    }

    public PluginMetaDataEntity(PluginMetaData pluginMetaData) {
        if (pluginMetaData.getId() != null) {
            this.id = pluginMetaData.getId().getId();
        }
        this.tenantId = pluginMetaData.getTenantId().getId();
        this.apiToken = pluginMetaData.getApiToken();
        this.clazz = pluginMetaData.getClazz();
        this.name = pluginMetaData.getName();
        this.publicAccess = pluginMetaData.isPublicAccess();
        this.state = pluginMetaData.getState();
        this.configuration = pluginMetaData.getConfiguration();
        this.searchText = pluginMetaData.getName();
        this.additionalInfo = pluginMetaData.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
    }

    public ComponentLifecycleState getState() {
        return state;
    }

    public void setState(ComponentLifecycleState state) {
        this.state = state;
    }

    public String getSearchText() {
        return searchText;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public PluginMetaData toData() {
        PluginMetaData data = new PluginMetaData(new PluginId(id));
        data.setTenantId(new TenantId(tenantId));
        data.setCreatedTime(UUIDs.unixTimestamp(id));
        data.setName(name);
        data.setConfiguration(configuration);
        data.setClazz(clazz);
        data.setPublicAccess(publicAccess);
        data.setState(state);
        data.setApiToken(apiToken);
        data.setAdditionalInfo(additionalInfo);
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PluginMetaDataEntity entity = (PluginMetaDataEntity) o;
        return Objects.equals(id, entity.id) && Objects.equals(apiToken, entity.apiToken) && Objects.equals(tenantId, entity.tenantId)
                && Objects.equals(name, entity.name) && Objects.equals(clazz, entity.clazz) && Objects.equals(state, entity.state)
                && Objects.equals(configuration, entity.configuration)
                && Objects.equals(searchText, entity.searchText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, apiToken, tenantId, name, clazz, state, configuration, searchText);
    }

    @Override
    public String toString() {
        return "PluginMetaDataEntity{" + "id=" + id + ", apiToken='" + apiToken + '\'' + ", tenantId=" + tenantId + ", name='" + name + '\'' + ", clazz='"
                + clazz + '\'' + ", state=" + state + ", configuration=" + configuration + ", searchText='" + searchText + '\'' + '}';
    }
}
