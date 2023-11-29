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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.function.UnaryOperator;

@ApiModel
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TbResourceInfo extends BaseData<TbResourceId> implements HasName, TenantEntity, ExportableEntity<TbResourceId> {

    private static final long serialVersionUID = 7282664529021651736L;

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Tenant Id of the resource can't be changed.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "title")
    @ApiModelProperty(position = 4, value = "Resource title.", example = "BinaryAppDataContainer id=19 v1.0")
    private String title;
    @ApiModelProperty(position = 5, value = "Resource type.", example = "LWM2M_MODEL", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private ResourceType resourceType;
    @NoXss
    @Length(fieldName = "resourceKey")
    @ApiModelProperty(position = 6, value = "Resource key.", example = "19_1.0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String resourceKey;
    @ApiModelProperty(position = 7, value = "Resource search text.", example = "19_1.0:binaryappdatacontainer", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String searchText;

    @ApiModelProperty(position = 8, value = "Resource etag.", example = "33a64df551425fcc55e4d42a148795d9f25f89d4", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String etag;
    @NoXss
    @Length(fieldName = "file name")
    @ApiModelProperty(position = 9, value = "Resource file name.", example = "19.xml", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String fileName;
    private JsonNode descriptor;

    private TbResourceId externalId;

    public TbResourceInfo() {
        super();
    }

    public TbResourceInfo(TbResourceId id) {
        super(id);
    }

    public TbResourceInfo(TbResourceInfo resourceInfo) {
        super(resourceInfo);
        this.tenantId = resourceInfo.tenantId;
        this.title = resourceInfo.title;
        this.resourceType = resourceInfo.resourceType;
        this.resourceKey = resourceInfo.resourceKey;
        this.searchText = resourceInfo.searchText;
        this.etag = resourceInfo.etag;
        this.fileName = resourceInfo.fileName;
        this.descriptor = resourceInfo.descriptor != null ? resourceInfo.descriptor.deepCopy() : null;
        this.externalId = resourceInfo.externalId;
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Resource Id. " +
            "Specify this field to update the Resource. " +
            "Referencing non-existing Resource Id will cause error. " +
            "Omit this field to create new Resource.")
    @Override
    public TbResourceId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the resource creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getLink() {
        if (resourceType == ResourceType.IMAGE) {
            return "/api/images/" + ((tenantId == null || !tenantId.isSysTenantId()) ? "tenant" : "system") + "/" + resourceKey; // tenantId is null in case of export to git
        }
        return null;
    }

    @JsonIgnore
    public String getSearchText() {
        return title;
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }

    public <T> T getDescriptor(Class<T> type) throws JsonProcessingException {
        return descriptor != null ? mapper.treeToValue(descriptor, type) : null;
    }

    public <T> void updateDescriptor(Class<T> type, UnaryOperator<T> updater) throws JsonProcessingException {
        T descriptor = getDescriptor(type);
        descriptor = updater.apply(descriptor);
        setDescriptorValue(descriptor);
    }

    @JsonIgnore
    public void setDescriptorValue(Object value) {
        this.descriptor = value != null ? mapper.valueToTree(value) : null;
    }

}
