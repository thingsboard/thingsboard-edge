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
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.DeviceInactivityNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceInactivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

@Service
@RequiredArgsConstructor
public class DeviceInactivityTriggerProcessor implements NotificationRuleTriggerProcessor<TbMsg, DeviceInactivityNotificationRuleTriggerConfig> {

    private final TbDeviceProfileCache deviceProfileCache;

    @Override
    public boolean matchesFilter(TbMsg ruleEngineMsg, DeviceInactivityNotificationRuleTriggerConfig triggerConfig) {
        DeviceId deviceId = (DeviceId) ruleEngineMsg.getOriginator();
        if (CollectionUtils.isNotEmpty(triggerConfig.getDevices())) {
            return triggerConfig.getDevices().contains(deviceId.getId());
        } else if (CollectionUtils.isNotEmpty(triggerConfig.getDeviceProfiles())) {
            DeviceProfile deviceProfile = deviceProfileCache.get(TenantId.SYS_TENANT_ID, deviceId);
            return deviceProfile != null && triggerConfig.getDeviceProfiles().contains(deviceProfile.getUuidId());
        } else {
            return true;
        }
    }

    @Override
    public NotificationInfo constructNotificationInfo(TbMsg ruleEngineMsg, DeviceInactivityNotificationRuleTriggerConfig triggerConfig) {
        return DeviceInactivityNotificationInfo.builder()
                .deviceId(ruleEngineMsg.getOriginator().getId())
                .deviceName(ruleEngineMsg.getMetaData().getValue("deviceName"))
                .deviceType(ruleEngineMsg.getMetaData().getValue("deviceType"))
                .deviceCustomerId(ruleEngineMsg.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.DEVICE_INACTIVITY;
    }

}
