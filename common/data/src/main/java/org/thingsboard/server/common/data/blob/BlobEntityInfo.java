/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.blob;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@EqualsAndHashCode(callSuper = true)
public class BlobEntityInfo extends SearchTextBasedWithAdditionalInfo<BlobEntityId> implements HasName, HasTenantId, HasCustomerId {

    private static final long serialVersionUID = 2807223040519549363L;

    private TenantId tenantId;
    private CustomerId customerId;
    private String name;
    private String type;
    private String contentType;

    public BlobEntityInfo() {
        super();
    }

    public BlobEntityInfo(BlobEntityId id) {
        super(id);
    }

    public BlobEntityInfo(BlobEntityInfo blobEntityInfo) {
        super(blobEntityInfo);
        this.tenantId = blobEntityInfo.getTenantId();
        this.customerId = blobEntityInfo.getCustomerId();
        this.name = blobEntityInfo.getName();
        this.type = blobEntityInfo.getType();
        this.contentType = blobEntityInfo.getContentType();
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

}
