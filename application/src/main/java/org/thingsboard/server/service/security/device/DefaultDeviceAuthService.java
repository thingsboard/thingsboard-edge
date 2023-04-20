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
package org.thingsboard.server.service.security.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsFilter;
import org.thingsboard.server.common.transport.auth.DeviceAuthResult;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
@Slf4j
public class DefaultDeviceAuthService implements DeviceAuthService {

    private final DeviceService deviceService;

    private final DeviceCredentialsService deviceCredentialsService;

    public DefaultDeviceAuthService(DeviceService deviceService, DeviceCredentialsService deviceCredentialsService) {
        this.deviceService = deviceService;
        this.deviceCredentialsService = deviceCredentialsService;
    }

    @Override
    public DeviceAuthResult process(DeviceCredentialsFilter credentialsFilter) {
        log.trace("Lookup device credentials using filter {}", credentialsFilter);
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(credentialsFilter.getCredentialsId());
        if (credentials != null) {
            log.trace("Credentials found {}", credentials);
            if (credentials.getCredentialsType() == credentialsFilter.getCredentialsType()) {
                switch (credentials.getCredentialsType()) {
                    case ACCESS_TOKEN:
                        // Credentials ID matches Credentials value in this
                        // primitive case;
                        return DeviceAuthResult.of(credentials.getDeviceId());
                    case X509_CERTIFICATE:
                        return DeviceAuthResult.of(credentials.getDeviceId());
                    case LWM2M_CREDENTIALS:
                        return DeviceAuthResult.of(credentials.getDeviceId());
                    default:
                        return DeviceAuthResult.of("Credentials Type is not supported yet!");
                }
            } else {
                return DeviceAuthResult.of("Credentials Type mismatch!");
            }
        } else {
            log.trace("Credentials not found!");
            return DeviceAuthResult.of("Credentials Not Found!");
        }
    }

}
