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
package org.thingsboard.server.common.data.edge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class Edge extends SearchTextBasedWithAdditionalInfo<EdgeId>
        implements HasLabel, GroupEntity<EdgeId> {

    private static final long serialVersionUID = 4934987555236873728L;

    private TenantId tenantId;
    private CustomerId customerId;
    private RuleChainId rootRuleChainId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "type")
    private String type;
    @NoXss
    @Length(fieldName = "label")
    private String label;
    @NoXss
    @Length(fieldName = "routingKey")
    private String routingKey;
    @NoXss
    @Length(fieldName = "secret")
    private String secret;
    @NoXss
    @Length(fieldName = "edgeLicenseKey", max = 30)
    private String edgeLicenseKey;
    @NoXss
    @Length(fieldName = "cloudEndpoint")
    private String cloudEndpoint;

    public Edge() {
        super();
    }

    public Edge(EdgeId id) {
        super(id);
    }

    public Edge(Edge edge) {
        super(edge);
        this.tenantId = edge.getTenantId();
        this.customerId = edge.getCustomerId();
        this.type = edge.getType();
        this.name = edge.getName();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
        this.edgeLicenseKey = edge.getEdgeLicenseKey();
        this.cloudEndpoint = edge.getCloudEndpoint();
        this.rootRuleChainId = edge.getRootRuleChainId();
    }

    public void update(Edge edge) {
        this.tenantId = edge.getTenantId();
        this.customerId = edge.getCustomerId();
        this.rootRuleChainId = edge.getRootRuleChainId();
        this.type = edge.getType();
        this.label = edge.getLabel();
        this.name = edge.getName();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
        this.edgeLicenseKey = edge.getEdgeLicenseKey();
        this.cloudEndpoint = edge.getCloudEndpoint();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Edge Id. " +
            "Specify this field to update the Edge. " +
            "Referencing non-existing Edge Id will cause error. " +
            "Omit this field to create new Edge." )
    @Override
    public EdgeId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the edge creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Use 'assignDeviceToTenant' to change the Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return this.tenantId;
    }

    @ApiModelProperty(position = 4, value = "JSON object with Customer Id. Use 'assignEdgeToCustomer' to change the Customer Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public CustomerId getCustomerId() {
        return this.customerId;
    }

    @ApiModelProperty(position = 5, value = "JSON object with Root Rule Chain Id. Use 'setEdgeRootRuleChain' to change the Root Rule Chain Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public RuleChainId getRootRuleChainId() {
        return this.rootRuleChainId;
    }

    @ApiModelProperty(position = 6, required = true, value = "Unique Edge Name in scope of Tenant", example = "Silo_A_Edge")
    @Override
    public String getName() {
        return this.name;
    }

    @ApiModelProperty(position = 7, required = true, value = "Edge type", example = "Silos")
    public String getType() {
        return this.type;
    }

    @ApiModelProperty(position = 8, value = "Label that may be used in widgets", example = "Silo Edge on far field")
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @ApiModelProperty(position = 9, required = true, value = "Edge routing key ('username') to authorize on cloud")
    public String getRoutingKey() {
        return this.routingKey;
    }

    @ApiModelProperty(position = 10, required = true, value = "Edge secret ('password') to authorize on cloud")
    public String getSecret() {
        return this.secret;
    }

    @ApiModelProperty(position = 11, required = true, value = "Edge license key obtained from license portal", example = "AgcnI24Z06XC&m6Sxsdgf")
    public String getEdgeLicenseKey() {
        return this.edgeLicenseKey;
    }

    @ApiModelProperty(position = 12, required = true, value = "Edge uses this cloud URL to activate and periodically check it's license", example = "https://thingsboard.cloud")
    public String getCloudEndpoint() {
        return this.cloudEndpoint;
    }

    @JsonIgnore
    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }
}
