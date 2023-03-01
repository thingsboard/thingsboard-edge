/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.DashboardId;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
public class Dashboard extends DashboardInfo implements ExportableEntity<DashboardId> {

    private static final long serialVersionUID = 872682138346187503L;

    private transient JsonNode configuration;

    @Getter
    @Setter
    private DashboardId externalId;

    public Dashboard() {
        super();
    }

    public Dashboard(DashboardId id) {
        super(id);
    }

    public Dashboard(DashboardInfo dashboardInfo) {
        super(dashboardInfo);
    }

    public Dashboard(Dashboard dashboard) {
        super(dashboard);
        this.configuration = dashboard.getConfiguration();
        this.externalId = dashboard.getExternalId();
    }

    @ApiModelProperty(position = 12, value = "JSON object with main configuration of the dashboard: layouts, widgets, aliases, etc. " +
            "The JSON structure of the dashboard configuration is quite complex. " +
            "The easiest way to learn it is to export existing dashboard to JSON."
            , dataType = "com.fasterxml.jackson.databind.JsonNode")
    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    @JsonIgnore
    public List<ObjectNode> getEntityAliasesConfig() {
        return getChildObjects("entityAliases");
    }

    @JsonIgnore
    public List<ObjectNode> getWidgetsConfig() {
        return getChildObjects("widgets");
    }

    @JsonIgnore
    private List<ObjectNode> getChildObjects(String propertyName) {
        return Optional.ofNullable(configuration)
                .map(config -> config.get(propertyName))
                .filter(node -> !node.isEmpty() && (node.isObject() || node.isArray()))
                .map(node -> Streams.stream(node.elements())
                        .filter(JsonNode::isObject)
                        .map(jsonNode -> (ObjectNode) jsonNode)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Dashboard [tenantId=");
        builder.append(getTenantId());
        builder.append(", title=");
        builder.append(getTitle());
        builder.append(", customerId=");
        builder.append(getCustomerId());
        builder.append(", configuration=");
        builder.append(configuration);
        builder.append("]");
        return builder.toString();
    }
}
