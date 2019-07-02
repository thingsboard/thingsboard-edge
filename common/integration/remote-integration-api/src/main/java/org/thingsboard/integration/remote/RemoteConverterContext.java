/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.cluster.ServerType;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@Slf4j
public class RemoteConverterContext implements ConverterContext {

    private final EventStorage eventStorage;
    private final TenantId tenantId;
    private final ConverterId converterId;
    private final boolean isUplink;
    private final ObjectMapper mapper;
    private final String clientId;
    private final int port;

    private AtomicInteger index = new AtomicInteger(0);

    @Override
    public ServerAddress getServerAddress() {
        return new ServerAddress(clientId, port, ServerType.CORE);
    }

    @Override
    public void saveEvent(String type, JsonNode body, IntegrationCallback<Void> callback) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(converterId);
        event.setType(type);
        event.setBody(body);

        TbEventSource source;
        if (isUplink) {
            source = TbEventSource.UPLINK_CONVERTER;
        } else {
            source = TbEventSource.DOWNLINK_CONVERTER;
        }

        String eventData = "";
        try {
            eventData = mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to convert event!", event, e);
        }

        eventStorage.write(UplinkMsg.newBuilder().setEventsData(index.getAndIncrement(), TbEventProto.newBuilder()
                .setSource(source)
                .setType("type") // TODO: 7/2/19 what type?
                .setData(eventData)
                .build()
        ).build(), callback);
    }
}
