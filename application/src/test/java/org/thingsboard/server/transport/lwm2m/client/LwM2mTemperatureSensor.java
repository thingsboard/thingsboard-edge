/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper;

import javax.security.auth.Destroyable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3303_12_5700_VALUE_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3303_12_5700_VALUE_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_VALUE_3303_12_5700_DELTA_TS;

@Slf4j
public class LwM2mTemperatureSensor extends BaseInstanceEnabler implements Destroyable {

    private static final String UNIT_CELSIUS = "cel";
    private double currentTemp = 20d;
    private double minMeasuredValue = currentTemp;
    private double maxMeasuredValue = currentTemp;

    private LeshanClient leshanClient;
    private int cntIdentitySystem;

    protected static final Random RANDOM = new Random();
    private static final List<Integer> supportedResources = Arrays.asList(5601, 5602, 5700, 5701);

    private LwM2mServer registeredServer;
    private ManualDataSender sender;

    private int resourceIdForSendCollected = 5700;

    public LwM2mTemperatureSensor() {
    }

    public LwM2mTemperatureSensor(ScheduledExecutorService executorService, Integer id) {
        try {
            if (id != null) this.setId(id);
            executorService.scheduleWithFixedDelay(this::adjustTemperature, 2000, 2000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            log.error("[{}]Throwable", e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public synchronized ReadResponse read(LwM2mServer identity, int resourceId) {
        log.trace("Read on Temperature resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        if (this.registeredServer == null && this.leshanClient != null && getId() == 12) {
            try {
                Lwm2mTestHelper.RESOURCE_ID_3303_12_5700_TS_0 = Instant.now().toEpochMilli();
                this.registeredServer = this.leshanClient.getRegisteredServers().values().iterator().next();
                this.sender = (ManualDataSender) this.leshanClient.getSendService().getDataSender(ManualDataSender.DEFAULT_NAME);
                this.sender.collectData(Arrays.asList(getPathForCollectedValue(resourceIdForSendCollected)));
            } catch (Exception e) {
                log.error("[{}] Sender for SendCollected", e.toString());
                e.printStackTrace();
            }
        }
        switch (resourceId) {
            case 5601:
                return ReadResponse.success(resourceId, getTwoDigitValue(minMeasuredValue));
            case 5602:
                return ReadResponse.success(resourceId, getTwoDigitValue(maxMeasuredValue));
            case 5700:
                if (identity == LwM2mServer.SYSTEM) {
                    double val5700 = cntIdentitySystem == 0 ? RESOURCE_ID_3303_12_5700_VALUE_0 : RESOURCE_ID_3303_12_5700_VALUE_1;
                    cntIdentitySystem++;
                    return ReadResponse.success(resourceId, val5700);
                } else {
                    if (cntIdentitySystem == 1 && this.getId() == 12 && this.leshanClient != null) {
                        sendCollected();
                    }
                    return super.read(identity, resourceId);
                }
            case 5701:
                return ReadResponse.success(resourceId, UNIT_CELSIUS);
            default:
                return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(LwM2mServer identity, int resourceId, Arguments arguments) {
        log.info("Execute on Temperature resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 5605:
                resetMinMaxMeasuredValues();
                return ExecuteResponse.success();
            default:
                return super.execute(identity, resourceId, arguments);
        }
    }

    private double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void adjustTemperature() {
        setTemperature();
        Integer changedResource = adjustMinMaxMeasuredValue(currentTemp);
        fireResourceChange(5700);
        if (changedResource != null) {
            fireResourceChange(changedResource);
        }
    }

    private void setTemperature() {
        float delta = (RANDOM.nextInt(20) - 10) / 10f;
        currentTemp += delta;
    }

    private synchronized Integer adjustMinMaxMeasuredValue(double newTemperature) {
        if (newTemperature > maxMeasuredValue) {
            maxMeasuredValue = newTemperature;
            return 5602;
        } else if (newTemperature < minMeasuredValue) {
            minMeasuredValue = newTemperature;
            return 5601;
        } else {
            return null;
        }
    }

    private void resetMinMaxMeasuredValues() {
        minMeasuredValue = currentTemp;
        maxMeasuredValue = currentTemp;
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    protected void setLeshanClient(LeshanClient leshanClient) {
        this.leshanClient = leshanClient;
    }

    @Override
    public void destroy() {
    }

    private void sendCollected() {
        try {
            if ((Instant.now().toEpochMilli() - Lwm2mTestHelper.RESOURCE_ID_3303_12_5700_TS_0) < RESOURCE_ID_VALUE_3303_12_5700_DELTA_TS) {
                Thread.sleep(RESOURCE_ID_VALUE_3303_12_5700_DELTA_TS);
            }
            sender.collectData(Arrays.asList(getPathForCollectedValue(resourceIdForSendCollected)));
            Lwm2mTestHelper.RESOURCE_ID_3303_12_5700_TS_1 = Instant.now().toEpochMilli();
            sender.sendCollectedData(registeredServer, ContentFormat.SENML_JSON, 1000, false);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private LwM2mPath getPathForCollectedValue(int resourceId) {
        return new LwM2mPath(3303, this.getId(), resourceId);
    }
}


