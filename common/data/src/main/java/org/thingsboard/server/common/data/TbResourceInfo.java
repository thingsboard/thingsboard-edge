/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.NoXss;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TbResourceInfo extends SearchTextBased<TbResourceId> implements TenantEntity {

    private TenantId tenantId;
    @NoXss
    private String title;
    private ResourceType resourceType;
    private String resourceKey;
    private String searchText;

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
    }

    @Override
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
        builder.append("]");
        return builder.toString();
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }
}
