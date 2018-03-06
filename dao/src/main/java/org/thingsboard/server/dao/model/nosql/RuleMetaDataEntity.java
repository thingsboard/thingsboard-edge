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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.ComponentLifecycleStateCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = RULE_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public class RuleMetaDataEntity implements SearchTextEntity<RuleMetaData> {

    @PartitionKey
    @Column(name = ID_PROPERTY)
    private UUID id;
    @ClusteringColumn
    @Column(name = RULE_TENANT_ID_PROPERTY)
    private UUID tenantId;
    @Column(name = RULE_NAME_PROPERTY)
    private String name;
    @Column(name = RULE_STATE_PROPERTY, codec = ComponentLifecycleStateCodec.class)
    private ComponentLifecycleState state;
    @Column(name = RULE_WEIGHT_PROPERTY)
    private int weight;
    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;
    @Column(name = RULE_PLUGIN_TOKEN_PROPERTY)
    private String pluginToken;
    @Column(name = RULE_FILTERS, codec = JsonCodec.class)
    private JsonNode filters;
    @Column(name = RULE_PROCESSOR, codec = JsonCodec.class)
    private JsonNode processor;
    @Column(name = RULE_ACTION, codec = JsonCodec.class)
    private JsonNode action;
    @Column(name = ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public RuleMetaDataEntity() {
    }

    public RuleMetaDataEntity(RuleMetaData rule) {
        if (rule.getId() != null) {
            this.id = rule.getUuidId();
        }
        this.tenantId = DaoUtil.getId(rule.getTenantId());
        this.name = rule.getName();
        this.pluginToken = rule.getPluginToken();
        this.state = rule.getState();
        this.weight = rule.getWeight();
        this.searchText = rule.getName();
        this.filters = rule.getFilters();
        this.processor = rule.getProcessor();
        this.action = rule.getAction();
        this.additionalInfo = rule.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return getSearchText();
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

    public ComponentLifecycleState getState() {
        return state;
    }

    public void setState(ComponentLifecycleState state) {
        this.state = state;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getPluginToken() {
        return pluginToken;
    }

    public void setPluginToken(String pluginToken) {
        this.pluginToken = pluginToken;
    }

    public String getSearchText() {
        return searchText;
    }

    public JsonNode getFilters() {
        return filters;
    }

    public void setFilters(JsonNode filters) {
        this.filters = filters;
    }

    public JsonNode getProcessor() {
        return processor;
    }

    public void setProcessor(JsonNode processor) {
        this.processor = processor;
    }

    public JsonNode getAction() {
        return action;
    }

    public void setAction(JsonNode action) {
        this.action = action;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public RuleMetaData toData() {
        RuleMetaData rule = new RuleMetaData(new RuleId(id));
        rule.setTenantId(new TenantId(tenantId));
        rule.setName(name);
        rule.setState(state);
        rule.setWeight(weight);
        rule.setCreatedTime(UUIDs.unixTimestamp(id));
        rule.setPluginToken(pluginToken);
        rule.setFilters(filters);
        rule.setProcessor(processor);
        rule.setAction(action);
        rule.setAdditionalInfo(additionalInfo);
        return rule;
    }

}
