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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.RULE_COLUMN_FAMILY_NAME)
public class RuleMetaDataEntity extends BaseSqlEntity<RuleMetaData> implements SearchTextEntity<RuleMetaData> {

    @Column(name = ModelConstants.RULE_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.RULE_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.RULE_STATE_PROPERTY)
    private ComponentLifecycleState state;

    @Column(name = ModelConstants.RULE_WEIGHT_PROPERTY)
    private int weight;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.RULE_PLUGIN_TOKEN_PROPERTY)
    private String pluginToken;

    @Type(type = "json")
    @Column(name = ModelConstants.RULE_FILTERS)
    private JsonNode filters;

    @Type(type = "json")
    @Column(name = ModelConstants.RULE_PROCESSOR)
    private JsonNode processor;

    @Type(type = "json")
    @Column(name = ModelConstants.RULE_ACTION)
    private JsonNode action;

    @Type(type = "json")
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public RuleMetaDataEntity() {
    }

    public RuleMetaDataEntity(RuleMetaData rule) {
        if (rule.getId() != null) {
            this.setId(rule.getUuidId());
        }
        this.tenantId = toString(DaoUtil.getId(rule.getTenantId()));
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
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public RuleMetaData toData() {
        RuleMetaData rule = new RuleMetaData(new RuleId(getId()));
        rule.setTenantId(new TenantId(toUUID(tenantId)));
        rule.setName(name);
        rule.setState(state);
        rule.setWeight(weight);
        rule.setCreatedTime(UUIDs.unixTimestamp(getId()));
        rule.setPluginToken(pluginToken);
        rule.setFilters(filters);
        rule.setProcessor(processor);
        rule.setAction(action);
        rule.setAdditionalInfo(additionalInfo);
        return rule;
    }
}
