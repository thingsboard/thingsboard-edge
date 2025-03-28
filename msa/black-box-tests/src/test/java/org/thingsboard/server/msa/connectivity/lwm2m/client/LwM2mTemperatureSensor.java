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
package org.thingsboard.server.msa.connectivity.lwm2m.client;

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

import javax.security.auth.Destroyable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LwM2mTemperatureSensor extends BaseInstanceEnabler implements Destroyable {

    private static final String UNIT_CELSIUS = "cel";
    private double currentTemp = 20d;
    private double minMeasuredValue = currentTemp;
    private double maxMeasuredValue = currentTemp;

    private LeshanClient leshanClient;
    private List<Double> containingValues;
    protected static final Random RANDOM = new Random();
    private static final List<Integer> supportedResources = Arrays.asList(5601, 5602, 5700, 5701);

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
        log.info("Read on Temperature resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 5601:
                return ReadResponse.success(resourceId, getTwoDigitValue(minMeasuredValue));
            case 5602:
                return ReadResponse.success(resourceId, getTwoDigitValue(maxMeasuredValue));
            case 5700:
                if (identity == LwM2mServer.SYSTEM)  {
                    setTemperature();
                    setData();
                    return ReadResponse.success(resourceId, getTwoDigitValue(currentTemp));
                } else if (this.getId() == 12 && this.leshanClient != null)  {
                    containingValues = new ArrayList<>();
                    sendCollected(5700);
                    return ReadResponse.success(resourceId, getData());
                } else {
                    return ReadResponse.success(resourceId, getTwoDigitValue(currentTemp));
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

    private void setTemperature(){
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

    protected void setLeshanClient(LeshanClient leshanClient){
        this.leshanClient = leshanClient;
    }

    @Override
    public void destroy() {
    }

    private void sendCollected(int resourceId) {
        try {
            LwM2mServer registeredServer = this.leshanClient.getRegisteredServers().values().iterator().next();
            ManualDataSender sender = this.leshanClient.getSendService().getDataSender(ManualDataSender.DEFAULT_NAME,
                    ManualDataSender.class);
            sender.collectData(Arrays.asList(getPathForCollectedValue(resourceId)));
            Thread.sleep(1000);
            sender.collectData(Arrays.asList(getPathForCollectedValue(resourceId)));
            sender.sendCollectedData(registeredServer, ContentFormat.SENML_JSON, 1000, false);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private LwM2mPath getPathForCollectedValue(int resourceId) {
        return new LwM2mPath(3303, this.getId(), resourceId);
    }

    private double getData() {
        if (containingValues.size() > 1) {
            Integer t0 = Math.toIntExact(Math.round(containingValues.get(0) * 100));
            Integer t1 = Math.toIntExact(Math.round(containingValues.get(1) * 100));
            long to_t1 = (((long) t0) << 32) | (t1 & 0xffffffffL);
            return Double.longBitsToDouble(to_t1);
        } else {
            return currentTemp;
        }

    }

    private void setData() {
        if (containingValues == null){
            containingValues = new ArrayList<>();
        }
        containingValues.add(getTwoDigitValue(currentTemp));
    }
}

