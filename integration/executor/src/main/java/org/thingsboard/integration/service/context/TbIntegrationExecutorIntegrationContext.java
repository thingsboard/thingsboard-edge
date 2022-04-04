/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.service.context;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.service.api.IntegrationApiService;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.service.integration.IntegrationProtoUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class TbIntegrationExecutorIntegrationContext implements IntegrationContext {

    private final String serviceId;
    private final IntegrationApiService apiService;
    private final TbIntegrationExecutorContextComponent contextComponent;
    private final Integration configuration;
    private final IntegrationInfoProto integrationInfoProto;

    public TbIntegrationExecutorIntegrationContext(String serviceId, IntegrationApiService apiService, TbIntegrationExecutorContextComponent contextComponent, Integration configuration) {
        this.serviceId = serviceId;
        this.apiService = apiService;
        this.contextComponent = contextComponent;
        this.configuration = configuration;
        this.integrationInfoProto = IntegrationProtoUtil.toProto(configuration);
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public ConverterContext getUplinkConverterContext() {
        return new ConverterContext() {
            @Override
            public String getServiceId() {
                return serviceId;
            }

            @Override
            public void saveEvent(String type, JsonNode body, IntegrationCallback<Void> callback) {
                // TODO: ashvayka integration executor
            }
        };
    }

    @Override
    public ConverterContext getDownlinkConverterContext() {
        return new ConverterContext() {
            @Override
            public String getServiceId() {
                return serviceId;
            }

            @Override
            public void saveEvent(String type, JsonNode body, IntegrationCallback<Void> callback) {
                // TODO: ashvayka integration executor
            }
        };
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto uplinkData, IntegrationCallback<Void> callback) {
        log.info("Received uplink: {}", uplinkData);
        apiService.sendUplinkData(configuration, integrationInfoProto, uplinkData, callback);
    }

    @Override
    public void processUplinkData(AssetUplinkDataProto uplinkData, IntegrationCallback<Void> callback) {
        log.info("Received uplink: {}", uplinkData);
        apiService.sendUplinkData(configuration, integrationInfoProto, uplinkData, callback);
    }

    @Override
    public void createEntityView(EntityViewDataProto entityViewDataProto, IntegrationCallback<Void> callback) {

    }

    @Override
    public void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback) {

    }

    @Override
    public void saveEvent(String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {

    }

    @Override
    public void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {

    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return contextComponent.getEventLoopGroup();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return contextComponent.getScheduledExecutorService();
    }

    @Override
    public ExecutorService getCallBackExecutorService() {
        return contextComponent.getCallBackExecutorService();
    }

    @Override
    public DownLinkMsg getDownlinkMsg(String deviceName) {
        return null;
    }

    @Override
    public DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg) {
        return null;
    }

    @Override
    public void removeDownlinkMsg(String deviceName) {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isExceptionStackTraceEnabled() {
        return false;
    }
}
