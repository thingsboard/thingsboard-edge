/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.DEBUG_MODE;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_CONFIGURATION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_FIRST_RULE_NODE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_ROOT_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Table(name = RULE_CHAIN_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public class RuleChainEntity implements SearchTextEntity<RuleChain> {

    @PartitionKey
    @Column(name = ID_PROPERTY)
    private UUID id;
    @ClusteringColumn
    @Column(name = RULE_CHAIN_TENANT_ID_PROPERTY)
    private UUID tenantId;
    @Column(name = RULE_CHAIN_NAME_PROPERTY)
    private String name;
    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;
    @Column(name = RULE_CHAIN_FIRST_RULE_NODE_ID_PROPERTY)
    private UUID firstRuleNodeId;
    @Column(name = RULE_CHAIN_ROOT_PROPERTY)
    private boolean root;
    @Getter
    @Setter
    @Column(name = DEBUG_MODE)
    private boolean debugMode;
    @Column(name = RULE_CHAIN_CONFIGURATION_PROPERTY, codec = JsonCodec.class)
    private JsonNode configuration;
    @Column(name = ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public RuleChainEntity() {
    }

    public RuleChainEntity(RuleChain ruleChain) {
        if (ruleChain.getId() != null) {
            this.id = ruleChain.getUuidId();
        }
        this.tenantId = DaoUtil.getId(ruleChain.getTenantId());
        this.name = ruleChain.getName();
        this.searchText = ruleChain.getName();
        this.firstRuleNodeId = DaoUtil.getId(ruleChain.getFirstRuleNodeId());
        this.root = ruleChain.isRoot();
        this.debugMode = ruleChain.isDebugMode();
        this.configuration = ruleChain.getConfiguration();
        this.additionalInfo = ruleChain.getAdditionalInfo();
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

    public UUID getFirstRuleNodeId() {
        return firstRuleNodeId;
    }

    public void setFirstRuleNodeId(UUID firstRuleNodeId) {
        this.firstRuleNodeId = firstRuleNodeId;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public String getSearchText() {
        return searchText;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public RuleChain toData() {
        RuleChain ruleChain = new RuleChain(new RuleChainId(id));
        ruleChain.setCreatedTime(UUIDs.unixTimestamp(id));
        ruleChain.setTenantId(new TenantId(tenantId));
        ruleChain.setName(name);
        if (this.firstRuleNodeId != null) {
            ruleChain.setFirstRuleNodeId(new RuleNodeId(this.firstRuleNodeId));
        }
        ruleChain.setRoot(this.root);
        ruleChain.setDebugMode(this.debugMode);
        ruleChain.setConfiguration(this.configuration);
        ruleChain.setAdditionalInfo(this.additionalInfo);
        return ruleChain;
    }

}
