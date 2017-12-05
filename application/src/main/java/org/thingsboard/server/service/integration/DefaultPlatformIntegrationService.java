/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.converter.ThingsboardDataConverter;
import org.thingsboard.server.service.integration.oc.OceanConnectIntegration;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ashvayka on 02.12.17.
 */
@Service
public class DefaultPlatformIntegrationService implements PlatformIntegrationService {

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private DataConverterService dataConverterService;

    private ConcurrentMap<IntegrationId, ThingsboardPlatformIntegration> integrationsByIdMap;
    private ConcurrentMap<String, ThingsboardPlatformIntegration> integrationsByRoutingKeyMap;

    @PostConstruct
    public void init() {
        integrationsByIdMap = new ConcurrentHashMap<>();
        integrationsByRoutingKeyMap = new ConcurrentHashMap<>();
        //TODO: init integrations maps using information from DB;
    }

    @Override
    public ThingsboardPlatformIntegration createIntegration(Integration integration) {
        return integrationsByIdMap.computeIfAbsent(integration.getId(), i -> {
            ThingsboardPlatformIntegration result = initIntegration(integration);
            integrationsByRoutingKeyMap.putIfAbsent(integration.getRoutingKey(), result);
            return result;
        });
    }

    @Override
    public ThingsboardPlatformIntegration updateIntegration(Integration configuration) {
        ThingsboardPlatformIntegration integration = integrationsByIdMap.get(configuration.getId());
        if (integration != null) {
            integration.update(configuration, getThingsboardDataConverter(configuration));
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
                result = createIntegration(configuration);
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
                result = createIntegration(configuration.get());
            }
        }
        return Optional.ofNullable(result);
    }

    private ThingsboardPlatformIntegration initIntegration(Integration integration) {
        ThingsboardDataConverter converter = getThingsboardDataConverter(integration);
        switch (integration.getType()) {
            case OCEANCONNECT:
                OceanConnectIntegration result = new OceanConnectIntegration();
                result.init(integration, converter);
                return result;
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private ThingsboardDataConverter getThingsboardDataConverter(Integration integration) {
        return dataConverterService.getConverterById(integration.getDefaultConverterId())
                .orElseThrow(() -> new ThingsboardRuntimeException("Converter not found!", ThingsboardErrorCode.ITEM_NOT_FOUND));
    }
}
