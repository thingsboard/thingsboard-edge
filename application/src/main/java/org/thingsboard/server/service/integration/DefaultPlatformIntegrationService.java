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
package org.thingsboard.server.service.integration;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.converter.ThingsboardDataConverter;
import org.thingsboard.server.service.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.server.service.integration.oc.OceanConnectIntegration;
import org.thingsboard.server.service.integration.thingpark.ThingParkIntegration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 02.12.17.
 */
@Service
public class DefaultPlatformIntegrationService implements PlatformIntegrationService {

    public static EventLoopGroup EVENT_LOOP_GROUP;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private DataConverterService dataConverterService;

    @Autowired
    protected IntegrationContext context;

    private ConcurrentMap<IntegrationId, ThingsboardPlatformIntegration> integrationsByIdMap;
    private ConcurrentMap<String, ThingsboardPlatformIntegration> integrationsByRoutingKeyMap;

    @PostConstruct
    public void init() {
        integrationsByIdMap = new ConcurrentHashMap<>();
        integrationsByRoutingKeyMap = new ConcurrentHashMap<>();
        EVENT_LOOP_GROUP = new NioEventLoopGroup();
        //TODO: init integrations maps using information from DB;
    }

    @PreDestroy
    public void destroy() {
        EVENT_LOOP_GROUP.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        integrationsByIdMap.values().forEach(ThingsboardPlatformIntegration::destroy);
        integrationsByIdMap.clear();
        integrationsByRoutingKeyMap.clear();
    }

    @Override
    public ThingsboardPlatformIntegration createIntegration(Integration integration) throws Exception {
        return integrationsByIdMap.computeIfAbsent(integration.getId(), i -> {
            try {
                ThingsboardPlatformIntegration result = initIntegration(integration);
                integrationsByRoutingKeyMap.putIfAbsent(integration.getRoutingKey(), result);
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public ThingsboardPlatformIntegration updateIntegration(Integration configuration) throws Exception {
        ThingsboardPlatformIntegration integration = integrationsByIdMap.get(configuration.getId());
        if (integration != null) {
            integration.update(context, configuration, getThingsboardDataConverter(configuration));
            return integration;
        } else {
            return createIntegration(configuration);
        }
    }

    @Override
    public void deleteIntegration(IntegrationId integrationId) {
        ThingsboardPlatformIntegration integration = integrationsByIdMap.remove(integrationId);
        if (integration != null) {
            integrationsByRoutingKeyMap.remove(integration.getConfiguration().getRoutingKey());
            integration.destroy();
        }
    }

    @Override
    public Optional<ThingsboardPlatformIntegration> getIntegrationById(IntegrationId id) {
        ThingsboardPlatformIntegration result = integrationsByIdMap.get(id);
        if (result == null) {
            Integration configuration = integrationService.findIntegrationById(id);
            if (configuration != null) {
                try {
                    result = createIntegration(configuration);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String key) {
        ThingsboardPlatformIntegration result = integrationsByRoutingKeyMap.get(key);
        if (result == null) {
            Optional<Integration> configuration = integrationService.findIntegrationByRoutingKey(key);
            if (configuration.isPresent()) {
                try {
                    result = createIntegration(configuration.get());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return Optional.ofNullable(result);
    }

    private ThingsboardPlatformIntegration initIntegration(Integration integration) throws Exception {
        ThingsboardDataConverter converter = getThingsboardDataConverter(integration);
        ThingsboardPlatformIntegration result = createThingsboardPlatformIntegration(integration);
        result.init(context, integration, converter);
        return result;
    }

    private ThingsboardPlatformIntegration createThingsboardPlatformIntegration(Integration integration) {
        switch (integration.getType()) {
            case OCEANCONNECT:
                return new OceanConnectIntegration();
            case THINGPARK:
                return new ThingParkIntegration();
            case MQTT:
                return new BasicMqttIntegration();
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private ThingsboardDataConverter getThingsboardDataConverter(Integration integration) {
        return dataConverterService.getConverterById(integration.getDefaultConverterId())
                .orElseThrow(() -> new ThingsboardRuntimeException("Converter not found!", ThingsboardErrorCode.ITEM_NOT_FOUND));
    }
}
