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
package org.thingsboard.server.service.solutions.data.emulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.service.solutions.data.definition.DeviceEmulatorDefinition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class DeviceEmulatorLauncher {

    private final Device device;
    private final DeviceEmulatorDefinition deviceProfile;
    private final ExecutorService oldTelemetryExecutor;
    private final TbClusterService tbClusterService;
    private final long publishFrequency;

    private final DeviceEmulator deviceEmulator;
    private ScheduledFuture<?> scheduledFuture;

    @Builder
    public DeviceEmulatorLauncher(Device device, DeviceEmulatorDefinition deviceProfile, ExecutorService oldTelemetryExecutor, TbClusterService tbClusterService) throws Exception {
        this.device = device;
        this.deviceProfile = deviceProfile;
        this.oldTelemetryExecutor = oldTelemetryExecutor;
        this.tbClusterService = tbClusterService;
        this.publishFrequency = TimeUnit.SECONDS.toMillis(deviceProfile.getPublishFrequencyInSeconds());
        if (StringUtils.isEmpty(deviceProfile.getClazz())) {
            deviceEmulator = new BasicDeviceEmulator();
        } else {
            deviceEmulator = (DeviceEmulator) Class.forName(deviceProfile.getClazz()).getDeclaredConstructor().newInstance();
        }
        deviceEmulator.init(deviceProfile);
    }

    public void launch() {
        final long latestTs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(deviceProfile.getPublishPeriodInDays()) - publishFrequency;
        oldTelemetryExecutor.submit(() -> {
            try {
                if (latestTs < (System.currentTimeMillis() - publishFrequency)) {
                    pushOldTelemetry(latestTs);
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to upload telemetry for device: ", device.getName(), e);
            }
        });
    }

    private void pushOldTelemetry(long latestTs) throws InterruptedException {
        for (long ts = latestTs; ts < System.currentTimeMillis(); ts += publishFrequency) {
            publishTelemetry(ts);
        }
    }

    private void publishTelemetry(long ts) throws InterruptedException {
        ObjectNode values = deviceEmulator.getValue(ts);
        publishTelemetry(ts, values);
        Thread.sleep(deviceProfile.getPublishPauseInMillis());
    }

    public void stop() {
        scheduledFuture.cancel(true);
    }

    private void publishTelemetry(long ts, ObjectNode value) {
        String msgData = JacksonUtil.toString(value);
        log.debug("[{}] Publishing telemetry: {}", device.getName(), msgData);
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("ts", Long.toString(ts));
        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), device.getId(), md, TbMsgDataType.JSON, msgData);
        tbClusterService.pushMsgToRuleEngine(device.getTenantId(), device.getId(), tbMsg, null);
    }
}
