/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.type.DeviceCredentialsTypeCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = DEVICE_CREDENTIALS_COLUMN_FAMILY_NAME)
public final class DeviceCredentialsEntity implements BaseEntity<DeviceCredentials> {

    @Transient
    private static final long serialVersionUID = -2667310560260623272L;
    
    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;
    
    @Column(name = DEVICE_CREDENTIALS_DEVICE_ID_PROPERTY)
    private UUID deviceId;
    
    @Column(name = DEVICE_CREDENTIALS_CREDENTIALS_TYPE_PROPERTY, codec = DeviceCredentialsTypeCodec.class)
    private DeviceCredentialsType credentialsType;

    @Column(name = DEVICE_CREDENTIALS_CREDENTIALS_ID_PROPERTY)
    private String credentialsId;

    @Column(name = DEVICE_CREDENTIALS_CREDENTIALS_VALUE_PROPERTY)
    private String credentialsValue;

    public DeviceCredentialsEntity() {
        super();
    }

    public DeviceCredentialsEntity(DeviceCredentials deviceCredentials) {
        if (deviceCredentials.getId() != null) {
            this.id = deviceCredentials.getId().getId();
        }
        if (deviceCredentials.getDeviceId() != null) {
            this.deviceId = deviceCredentials.getDeviceId().getId();
        }
        this.credentialsType = deviceCredentials.getCredentialsType();
        this.credentialsId = deviceCredentials.getCredentialsId();
        this.credentialsValue = deviceCredentials.getCredentialsValue(); 
    }
    
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceCredentialsType getCredentialsType() {
        return credentialsType;
    }

    public void setCredentialsType(DeviceCredentialsType credentialsType) {
        this.credentialsType = credentialsType;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getCredentialsValue() {
        return credentialsValue;
    }

    public void setCredentialsValue(String credentialsValue) {
        this.credentialsValue = credentialsValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((credentialsId == null) ? 0 : credentialsId.hashCode());
        result = prime * result + ((credentialsType == null) ? 0 : credentialsType.hashCode());
        result = prime * result + ((credentialsValue == null) ? 0 : credentialsValue.hashCode());
        result = prime * result + ((deviceId == null) ? 0 : deviceId.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeviceCredentialsEntity other = (DeviceCredentialsEntity) obj;
        if (credentialsId == null) {
            if (other.credentialsId != null)
                return false;
        } else if (!credentialsId.equals(other.credentialsId))
            return false;
        if (credentialsType != other.credentialsType)
            return false;
        if (credentialsValue == null) {
            if (other.credentialsValue != null)
                return false;
        } else if (!credentialsValue.equals(other.credentialsValue))
            return false;
        if (deviceId == null) {
            if (other.deviceId != null)
                return false;
        } else if (!deviceId.equals(other.deviceId))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DeviceCredentialsEntity [id=");
        builder.append(id);
        builder.append(", deviceId=");
        builder.append(deviceId);
        builder.append(", credentialsType=");
        builder.append(credentialsType);
        builder.append(", credentialsId=");
        builder.append(credentialsId);
        builder.append(", credentialsValue=");
        builder.append(credentialsValue);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public DeviceCredentials toData() {
        DeviceCredentials deviceCredentials = new DeviceCredentials(new DeviceCredentialsId(id));
        deviceCredentials.setCreatedTime(UUIDs.unixTimestamp(id));
        if (deviceId != null) {
            deviceCredentials.setDeviceId(new DeviceId(deviceId));
        }
        deviceCredentials.setCredentialsType(credentialsType);
        deviceCredentials.setCredentialsId(credentialsId);
        deviceCredentials.setCredentialsValue(credentialsValue);
        return deviceCredentials;
    }

}