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
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.ShortEdgeInfo;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class RuleChain extends SearchTextBasedWithAdditionalInfo<RuleChainId> implements HasName, TenantEntity {

    private static final long serialVersionUID = -5656679015121935465L;

    private TenantId tenantId;
    private String name;
    private RuleChainType type;
    private RuleNodeId firstRuleNodeId;
    private boolean root;
    private boolean debugMode;
    private transient JsonNode configuration;
    private Set<ShortEdgeInfo> assignedEdges;

    @JsonIgnore
    private byte[] configurationBytes;

    public RuleChain() {
        super();
    }

    public RuleChain(RuleChainId id) {
        super(id);
    }

    public RuleChain(RuleChain ruleChain) {
        super(ruleChain);
        this.tenantId = ruleChain.getTenantId();
        this.name = ruleChain.getName();
        this.type = ruleChain.getType();
        this.firstRuleNodeId = ruleChain.getFirstRuleNodeId();
        this.root = ruleChain.isRoot();
        this.assignedEdges = ruleChain.getAssignedEdges();
        this.setConfiguration(ruleChain.getConfiguration());
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    public JsonNode getConfiguration() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> configuration, () -> configurationBytes);
    }

    public void setConfiguration(JsonNode data) {
        setJson(data, json -> this.configuration = json, bytes -> this.configurationBytes = bytes);
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

    public boolean isAssignedToEdge(EdgeId edgeId) {
        return this.assignedEdges != null && this.assignedEdges.contains(new ShortEdgeInfo(edgeId, null, null));
    }

    public ShortEdgeInfo getAssignedEdgeInfo(EdgeId edgeId) {
        if (this.assignedEdges != null) {
            for (ShortEdgeInfo edgeInfo : this.assignedEdges) {
                if (edgeInfo.getEdgeId().equals(edgeId)) {
                    return edgeInfo;
                }
            }
        }
        return null;
    }

    public boolean addAssignedEdge(Edge edge) {
        ShortEdgeInfo edgeInfo = edge.toShortEdgeInfo();
        if (this.assignedEdges != null && this.assignedEdges.contains(edgeInfo)) {
            return false;
        } else {
            if (this.assignedEdges == null) {
                this.assignedEdges = new HashSet<>();
            }
            this.assignedEdges.add(edgeInfo);
            return true;
        }
    }

    public boolean updateAssignedEdge(Edge edge) {
        ShortEdgeInfo edgeInfo = edge.toShortEdgeInfo();
        if (this.assignedEdges != null && this.assignedEdges.contains(edgeInfo)) {
            this.assignedEdges.remove(edgeInfo);
            this.assignedEdges.add(edgeInfo);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAssignedEdge(Edge edge) {
        ShortEdgeInfo edgeInfo = edge.toShortEdgeInfo();
        if (this.assignedEdges != null && this.assignedEdges.contains(edgeInfo)) {
            this.assignedEdges.remove(edgeInfo);
            return true;
        } else {
            return false;
        }
    }

}
