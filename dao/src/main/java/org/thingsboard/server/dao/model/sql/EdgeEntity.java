/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_LABEL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_ROOT_RULE_CHAIN_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_ROUTING_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_SECRET_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = EDGE_COLUMN_FAMILY_NAME)
public class EdgeEntity extends BaseSqlEntity<Edge> implements SearchTextEntity<Edge> {

    @Column(name = EDGE_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = EDGE_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Column(name = EDGE_ROOT_RULE_CHAIN_ID_PROPERTY)
    private String rootRuleChainId;

    @Column(name = EDGE_TYPE_PROPERTY)
    private String type;

    @Column(name = EDGE_NAME_PROPERTY)
    private String name;

    @Column(name = EDGE_LABEL_PROPERTY)
    private String label;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = EDGE_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Column(name = EDGE_SECRET_PROPERTY)
    private String secret;

    @Type(type = "json")
    @Column(name = ModelConstants.EDGE_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Type(type = "json")
    @Column(name = ModelConstants.EDGE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public EdgeEntity() {
        super();
    }

    public EdgeEntity(Edge edge) {
        if (edge.getId() != null) {
            this.setUuid(edge.getId().getId());
        }
        if (edge.getTenantId() != null) {
            this.tenantId = UUIDConverter.fromTimeUUID(edge.getTenantId().getId());
        }
        if (edge.getCustomerId() != null) {
            this.customerId = UUIDConverter.fromTimeUUID(edge.getCustomerId().getId());
        }
        if (edge.getRootRuleChainId() != null) {
            this.rootRuleChainId = UUIDConverter.fromTimeUUID(edge.getRootRuleChainId().getId());
        }
        this.type = edge.getType();
        this.name = edge.getName();
        this.label = edge.getLabel();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
        this.configuration = edge.getConfiguration();
        this.additionalInfo = edge.getAdditionalInfo();
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public Edge toData() {
        Edge edge = new Edge(new EdgeId(UUIDConverter.fromString(id)));
        edge.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
        if (tenantId != null) {
            edge.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        if (customerId != null) {
            edge.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        }
        if (rootRuleChainId != null) {
            edge.setRootRuleChainId(new RuleChainId(UUIDConverter.fromString(rootRuleChainId)));
        }
        edge.setType(type);
        edge.setName(name);
        edge.setLabel(label);
        edge.setRoutingKey(routingKey);
        edge.setSecret(secret);
        edge.setConfiguration(configuration);
        edge.setAdditionalInfo(additionalInfo);
        return edge;
    }
}
