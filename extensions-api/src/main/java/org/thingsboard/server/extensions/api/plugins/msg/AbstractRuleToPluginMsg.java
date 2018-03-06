/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.extensions.api.plugins.msg;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;
import java.util.UUID;

public abstract class AbstractRuleToPluginMsg<T extends Serializable> implements RuleToPluginMsg<T> {

    private static final long serialVersionUID = 1L;

    private final UUID uid;
    private final TenantId tenantId;
    private final CustomerId customerId;
    private final DeviceId deviceId;
    private final T payload;

    public AbstractRuleToPluginMsg(TenantId tenantId, CustomerId customerId, DeviceId deviceId, T payload) {
        super();
        this.uid = UUID.randomUUID();
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.deviceId = deviceId;
        this.payload = payload;
    }

    @Override
    public UUID getUid() {
        return uid;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    @Override
    public DeviceId getDeviceId() {
        return deviceId;
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "AbstractRuleToPluginMsg [uid=" + uid + ", tenantId=" + tenantId + ", customerId=" + customerId
                + ", deviceId=" + deviceId + ", payload=" + payload + "]";
    }

}
