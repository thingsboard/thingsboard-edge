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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TbResourceInfo extends BaseData<TbResourceId> implements HasName, TenantEntity {

    private static final long serialVersionUID = 7282664529021651736L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the resource can't be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "title")
    @Schema(description = "Resource title.", example = "BinaryAppDataContainer id=19 v1.0")
    private String title;
    @Schema(description = "Resource type.", example = "LWM2M_MODEL", accessMode = Schema.AccessMode.READ_ONLY)
    private ResourceType resourceType;
    @NoXss
    @Length(fieldName = "resourceKey")
    @Schema(description = "Resource key.", example = "19_1.0", accessMode = Schema.AccessMode.READ_ONLY)
    private String resourceKey;
    @Schema(description = "Resource search text.", example = "19_1.0:binaryappdatacontainer", accessMode = Schema.AccessMode.READ_ONLY)
    private String searchText;
    @Schema(description = "Resource etag.", example = "33a64df551425fcc55e4d42a148795d9f25f89d4", accessMode = Schema.AccessMode.READ_ONLY)
    private String etag;

    public TbResourceInfo() {
        super();
    }

    public TbResourceInfo(TbResourceId id) {
        super(id);
    }

    public TbResourceInfo(TbResourceInfo resourceInfo) {
        super(resourceInfo);
        this.tenantId = resourceInfo.getTenantId();
        this.title = resourceInfo.getTitle();
        this.resourceType = resourceInfo.getResourceType();
        this.resourceKey = resourceInfo.getResourceKey();
        this.searchText = resourceInfo.getSearchText();
        this.etag = resourceInfo.getEtag();
    }

    @Schema(description = "JSON object with the Resource Id. " +
            "Specify this field to update the Resource. " +
            "Referencing non-existing Resource Id will cause error. " +
            "Omit this field to create new Resource.")
    @Override
    public TbResourceId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the resource creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    @JsonIgnore
    public String getName() {
        return title;
    }

    @JsonIgnore
    public String getSearchText() {
        return searchText != null ? searchText : title;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ResourceInfo [tenantId=");
        builder.append(tenantId);
        builder.append(", id=");
        builder.append(getUuidId());
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", title=");
        builder.append(title);
        builder.append(", resourceType=");
        builder.append(resourceType);
        builder.append(", resourceKey=");
        builder.append(resourceKey);
        builder.append(", hashCode=");
        builder.append(etag);
        builder.append("]");
        return builder.toString();
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }
}
