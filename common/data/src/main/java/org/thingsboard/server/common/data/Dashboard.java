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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApiModel
@EqualsAndHashCode(callSuper = true)
public class Dashboard extends SearchTextBased<DashboardId> implements GroupEntity<DashboardId>, HasName, HasTenantId, HasTitle, ExportableEntity<DashboardId> {

    private static final long serialVersionUID = 872682138346187503L;

    private TenantId tenantId;

    private CustomerId customerId;

    @NoXss
    @Length(fieldName = "title")
    private String title;
    @Length(fieldName = "image", max = 1000000)
    private String image;
    @Valid
    private Set<ShortCustomerInfo> assignedCustomers;
    private boolean mobileHide;
    private Integer mobileOrder;

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
        this.tenantId = dashboard.getTenantId();
        this.customerId = dashboard.getCustomerId();
        this.title = dashboard.getTitle();
        this.image = dashboard.getImage();
        this.assignedCustomers = dashboard.getAssignedCustomers();
        this.mobileHide = dashboard.isMobileHide();
        this.mobileOrder = dashboard.getMobileOrder();
        this.configuration = dashboard.getConfiguration();
        this.externalId = dashboard.getExternalId();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the dashboard Id. " +
            "Specify existing dashboard Id to update the dashboard. " +
            "Referencing non-existing dashboard id will cause error. " +
            "Omit this field to create new dashboard.")
    @Override
    public DashboardId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the dashboard creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Tenant Id of the dashboard can't be changed.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(position = 4, value = "JSON object with Customer Id. ")
    public CustomerId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    @ApiModelProperty(position = 5, value = "JSON object with Customer or Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
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

    @ApiModelProperty(position = 6, value = "Title of the dashboard.")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ApiModelProperty(position = 7, value = "Thumbnail picture for rendering of the dashboards in a grid view on mobile devices.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @ApiModelProperty(position = 8, value = "List of assigned customers with their info.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Set<ShortCustomerInfo> getAssignedCustomers() {
        return assignedCustomers;
    }

    public void setAssignedCustomers(Set<ShortCustomerInfo> assignedCustomers) {
        this.assignedCustomers = assignedCustomers;
    }

    @ApiModelProperty(position = 9, value = "Hide dashboard from mobile devices. Useful if the dashboard is not designed for small screens.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public boolean isMobileHide() {
        return mobileHide;
    }

    public void setMobileHide(boolean mobileHide) {
        this.mobileHide = mobileHide;
    }

    @ApiModelProperty(position = 10, value = "Order on mobile devices. Useful to adjust sorting of the dashboards for mobile applications", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Integer getMobileOrder() {
        return mobileOrder;
    }

    public void setMobileOrder(Integer mobileOrder) {
        this.mobileOrder = mobileOrder;
    }


    @ApiModelProperty(position = 11, value = "Same as title of the dashboard. Read-only field. Update the 'title' to change the 'name' of the dashboard.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    @Override
    public String getSearchText() {
        return getTitle();
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

    public boolean isAssignedToCustomer(CustomerId customerId) {
        return this.assignedCustomers != null && this.assignedCustomers.contains(new ShortCustomerInfo(customerId, null, false));
    }

    public ShortCustomerInfo getAssignedCustomerInfo(CustomerId customerId) {
        if (this.assignedCustomers != null) {
            for (ShortCustomerInfo customerInfo : this.assignedCustomers) {
                if (customerInfo.getCustomerId().equals(customerId)) {
                    return customerInfo;
                }
            }
        }
        return null;
    }

    public boolean addAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            return false;
        } else {
            if (this.assignedCustomers == null) {
                this.assignedCustomers = new HashSet<>();
            }
            this.assignedCustomers.add(customerInfo);
            return true;
        }
    }

    public boolean updateAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            this.assignedCustomers.remove(customerInfo);
            this.assignedCustomers.add(customerInfo);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            this.assignedCustomers.remove(customerInfo);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAssignedCustomerInfo(ShortCustomerInfo customerInfo) {
        if (this.assignedCustomers != null && this.assignedCustomers.contains(customerInfo)) {
            this.assignedCustomers.remove(customerInfo);
            return true;
        } else {
            return false;
        }
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
        result = prime * result + ((customerId == null) ? 0 : customerId.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Dashboard that = (Dashboard) o;
        return mobileHide == that.mobileHide
                && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(customerId, that.customerId)
                && Objects.equals(title, that.title)
                && Objects.equals(image, that.image)
                && Objects.equals(assignedCustomers, that.assignedCustomers)
                && Objects.equals(mobileOrder, that.mobileOrder);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Dashboard [tenantId=");
        builder.append(tenantId);
        builder.append(", customerId=");
        builder.append(customerId);
        builder.append(", title=");
        builder.append(title);
        builder.append("]");
        return builder.toString();
    }

}
