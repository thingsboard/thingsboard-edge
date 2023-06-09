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
package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.transport.snmp.AuthenticationProtocol;
import org.thingsboard.server.common.data.transport.snmp.PrivacyProtocol;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;

@Data
@ToString(of = {"host", "port", "protocolVersion"})
public class SnmpDeviceTransportConfiguration implements DeviceTransportConfiguration {
    private String host;
    private Integer port;
    private SnmpProtocolVersion protocolVersion;

    /*
     * For SNMP v1 and v2c
     * */
    private String community;

    /*
     * For SNMP v3
     * */
    private String username;
    private String securityName;
    private String contextName;
    private AuthenticationProtocol authenticationProtocol;
    private String authenticationPassphrase;
    private PrivacyProtocol privacyProtocol;
    private String privacyPassphrase;
    private String engineId;

    public SnmpDeviceTransportConfiguration() {
        this.host = "localhost";
        this.port = 161;
        this.protocolVersion = SnmpProtocolVersion.V2C;
        this.community = "public";
    }

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.SNMP;
    }

    @Override
    public void validate() {
        if (!isValid()) {
            throw new IllegalArgumentException("Transport configuration is not valid");
        }
    }

    @JsonIgnore
    private boolean isValid() {
        boolean isValid = StringUtils.isNotBlank(host) && port != null && protocolVersion != null;
        if (isValid) {
            switch (protocolVersion) {
                case V1:
                case V2C:
                    isValid = StringUtils.isNotEmpty(community);
                    break;
                case V3:
                    isValid = StringUtils.isNotBlank(username) && StringUtils.isNotBlank(securityName)
                            && contextName != null && authenticationProtocol != null
                            && StringUtils.isNotBlank(authenticationPassphrase)
                            && privacyProtocol != null && StringUtils.isNotBlank(privacyPassphrase) && engineId != null;
                    break;
            }
        }
        return isValid;
    }
}
