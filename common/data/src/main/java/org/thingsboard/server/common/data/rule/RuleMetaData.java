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
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class RuleMetaData extends SearchTextBasedWithAdditionalInfo<RuleId> implements HasName {

    private static final long serialVersionUID = -5656679015122935465L;

    private TenantId tenantId;
    private String name;
    private ComponentLifecycleState state;
    private int weight;
    private String pluginToken;
    private transient JsonNode filters;
    private transient JsonNode processor;
    private transient JsonNode action;
    @JsonIgnore
    private byte[] filtersBytes;
    @JsonIgnore
    private byte[] processorBytes;
    @JsonIgnore
    private byte[] actionBytes;


    public RuleMetaData() {
        super();
    }

    public RuleMetaData(RuleId id) {
        super(id);
    }

    public RuleMetaData(RuleMetaData rule) {
        super(rule);
        this.tenantId = rule.getTenantId();
        this.name = rule.getName();
        this.state = rule.getState();
        this.weight = rule.getWeight();
        this.pluginToken = rule.getPluginToken();
        this.setFilters(rule.getFilters());
        this.setProcessor(rule.getProcessor());
        this.setAction(rule.getAction());
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    public JsonNode getFilters() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> filters, () -> filtersBytes);
    }

    public JsonNode getProcessor() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> processor, () -> processorBytes);
    }

    public JsonNode getAction() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> action, () -> actionBytes);
    }

    public void setFilters(JsonNode data) {
        setJson(data, json -> this.filters = json, bytes -> this.filtersBytes = bytes);
    }

    public void setProcessor(JsonNode data) {
        setJson(data, json -> this.processor = json, bytes -> this.processorBytes = bytes);
    }

    public void setAction(JsonNode data) {
        setJson(data, json -> this.action = json, bytes -> this.actionBytes = bytes);
    }


}
