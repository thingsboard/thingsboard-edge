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
package org.thingsboard.server.common.transport.session;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentialsFilter;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.session.SessionContext;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.auth.DeviceAuthResult;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public abstract class DeviceAwareSessionContext implements SessionContext {

    protected final DeviceAuthService authService;
    protected final SessionMsgProcessor processor;

    protected volatile Device device;

    public DeviceAwareSessionContext(SessionMsgProcessor processor, DeviceAuthService authService) {
        this.processor = processor;
        this.authService = authService;
    }

    public DeviceAwareSessionContext(SessionMsgProcessor processor, DeviceAuthService authService, Device device) {
        this(processor, authService);
        this.device = device;
    }


    public boolean login(DeviceCredentialsFilter credentials) {
        DeviceAuthResult result = authService.process(credentials);
        if (result.isSuccess()) {
            Optional<Device> deviceOpt = authService.findDeviceById(result.getDeviceId());
            if (deviceOpt.isPresent()) {
                device = deviceOpt.get();
            }
            return true;
        } else {
            log.debug("Can't find device using credentials [{}] due to {}", credentials, result.getErrorMsg());
            return false;
        }
    }

    public DeviceAuthService getAuthService() {
        return authService;
    }

    public SessionMsgProcessor getProcessor() {
        return processor;
    }

    public Device getDevice() {
        return device;
    }
}
