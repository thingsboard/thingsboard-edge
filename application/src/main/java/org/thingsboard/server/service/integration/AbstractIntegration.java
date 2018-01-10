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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.session.AdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicAdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicToDeviceActorSessionMsg;
import org.thingsboard.server.service.converter.ThingsboardDataConverter;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.http.IntegrationHttpSessionCtx;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Created by ashvayka on 25.12.17.
 */
public abstract class AbstractIntegration<T> implements ThingsboardPlatformIntegration<T> {

    protected final ObjectMapper mapper = new ObjectMapper();
    protected Integration configuration;
    protected ThingsboardDataConverter converter;
    protected UplinkMetaData metadataTemplate;
    protected IntegrationStatistics integrationStatistics;

    @Override
    public void init(IntegrationContext context, Integration dto, ThingsboardDataConverter converter) throws Exception {
        this.configuration = dto;
        this.converter = converter;
        Map<String, String> mdMap = new HashMap<>();
        mdMap.put("integrationName", configuration.getName());
        JsonNode metadata = configuration.getConfiguration().get("metadata");
        for (Iterator<Map.Entry<String, JsonNode>> it = metadata.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> md = it.next();
            mdMap.put(md.getKey(), md.getValue().asText());
        }
        this.metadataTemplate = new UplinkMetaData(getUplinkContentType(), mdMap);
        this.integrationStatistics = new IntegrationStatistics();
    }

    protected String getUplinkContentType() {
        return "JSON";
    }

    @Override
    public void update(IntegrationContext context, Integration dto, ThingsboardDataConverter converter) throws Exception {
        init(context, dto, converter);
    }

    @Override
    public Integration getConfiguration() {
        return configuration;
    }

    @Override
    public void destroy() {

    }

    @Override
    public IntegrationStatistics popStatistics() {
        IntegrationStatistics statistics = this.integrationStatistics;
        this.integrationStatistics = new IntegrationStatistics();
        return statistics;
    }

    protected void processUplinkData(IntegrationContext context, UplinkData data) {
        Device device = getOrCreateDevice(context, data);

        if (data.getTelemetry() != null) {
            AdaptorToSessionActorMsg msg = new BasicAdaptorToSessionActorMsg(new IntegrationHttpSessionCtx(), data.getTelemetry());
            context.getSessionMsgProcessor().process(new BasicToDeviceActorSessionMsg(device, msg));
        }

        if (data.getAttributesUpdate() != null) {
            AdaptorToSessionActorMsg msg = new BasicAdaptorToSessionActorMsg(new IntegrationHttpSessionCtx(), data.getAttributesUpdate());
            context.getSessionMsgProcessor().process(new BasicToDeviceActorSessionMsg(device, msg));
        }
    }

    private Device getOrCreateDevice(IntegrationContext context, UplinkData data) {
        Optional<Device> deviceOptional = context.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), data.getDeviceName());
        Device device;
        if (!deviceOptional.isPresent()) {
            device = new Device();
            device.setName(data.getDeviceName());
            device.setType(data.getDeviceType());
            device.setTenantId(configuration.getTenantId());
            device = context.getDeviceService().saveDevice(device);
            EntityRelation relation = new EntityRelation();
            relation.setFrom(configuration.getId());
            relation.setTo(device.getId());
            relation.setTypeGroup(RelationTypeGroup.COMMON);
            relation.setType(EntityRelation.CONTAINS_TYPE);
            context.getRelationService().saveRelation(relation);
        } else {
            device = deviceOptional.get();
        }
        return device;
    }

    protected void persistDebug(IntegrationContext context, String type, String messageType, String message, String status, Exception exception) {
        Event event = new Event();
        event.setTenantId(configuration.getTenantId());
        event.setEntityId(configuration.getId());
        event.setType(DataConstants.DEBUG_INTEGRATION);

        ObjectNode node = mapper.createObjectNode()
                .put("server", context.getDiscoveryService().getCurrentServer().getServerAddress().toString())
                .put("type", type)
                .put("messageType", messageType)
                .put("message", message)
                .put("status", status);

        if (exception != null) {
            node = node.put("error", toString(exception));
        }

        event.setBody(node);
        context.getEventService().save(event);
    }

    private String toString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    protected List<UplinkData> convertToUplinkDataList(IntegrationContext context, byte[] data, UplinkMetaData md) throws Exception {
        return this.converter.convertUplink(context.getConverterContext(), data, md);
    }
}
