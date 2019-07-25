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
package org.thingsboard.integration.ophardt.converter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.ophardt.data.ConverterResult;
import org.thingsboard.integration.ophardt.data.DeduplicationData;
import org.thingsboard.integration.ophardt.data.DeviceTypes;
import org.thingsboard.integration.ophardt.data.ErrorCodes;
import org.thingsboard.integration.ophardt.data.EventProcessingData;
import org.thingsboard.integration.ophardt.data.EventTypes;
import org.thingsboard.integration.ophardt.data.OphardtData;
import org.thingsboard.integration.ophardt.data.SensorTypes;
import org.thingsboard.integration.ophardt.data.UUIDVersion5;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.gen.transport.KeyValueProto;
import org.thingsboard.server.gen.transport.KeyValueType;
import org.thingsboard.server.gen.transport.PostAttributeMsg;
import org.thingsboard.server.gen.transport.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TsKvListProto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class OphardtConverter {

    private static final String RAW_DATA_UUID = "__rawDataUUID";
    private static final String UNDERSCORE_SYMBOLS = "__";
    private static final String MEASUREMENT_ERROR_EVENT_TYPE = "MEASUREMENTERROR";
    private static final String SYSTEM_ERROR_EVENT_TYPE = "SYSTEM_ERROR";
    private static final String TYPE_FIELD = "type";
    private static final String CONTENT_FIELD = "content";
    private static final String CHILDREN_FIELD = "children";
    private static final int RESET_EVENT_CODE = 25;
    private static final long TS_FOR_2010_01_01 = 1262304000000L;
    private static final int FIVE_MINUTES = 5 * 60 * 1000;

    private final ConcurrentMap<String, Long> tsCorrectionPartsMap;
    private final ConcurrentMap<String, Long> lastCorrectedEventTsPerDeviceMap;

    private long deviceTime;
    private long lastEventTsFromMsg;
    private long lastValidEventTsBeforeReset;

    public OphardtConverter() {
        this.tsCorrectionPartsMap = new ConcurrentHashMap<>();
        this.lastCorrectedEventTsPerDeviceMap = new ConcurrentHashMap<>();
    }

    public ConverterResult convert(IntegrationContext context, JsonNode rawData, String uuidString) {
        ConverterResult converterResult = new ConverterResult();

        UplinkData.UplinkDataBuilder builder = UplinkData.builder();
        Map<UUID, JsonNode> eventsMap = new HashMap<>();
        String deviceName = null;
        String deviceType = null;
        long msgReceiveTime = rawData.get("submitterMessageReceiveTime").asLong();
        JsonNode message = rawData.get("message");

        if (message.isArray()) {
            for (JsonNode node : message) {
                switch (node.get(TYPE_FIELD).asText()) {
                    case "DeviceId":
                        deviceName = node.get(CONTENT_FIELD).asText();
                        builder.deviceName(deviceName);
                        break;
                    case "DeviceType":
                        deviceType = DeviceTypes.getDeviceTypeByValue(node.get(CONTENT_FIELD).asLong());
                        builder.deviceType(deviceType);
                        break;
                    case "Message":
                        PostAttributeMsg.Builder attributesBuilder = PostAttributeMsg.newBuilder();
                        PostTelemetryMsg.Builder telemetryBuilder = PostTelemetryMsg.newBuilder();
                        List<KeyValueProto> attributesResult = new ArrayList<>();
                        List<KeyValueProto> telemetryResult = new ArrayList<>();
                        long firmwareVersion = 0L;

                        boolean stopProcessingMsg = false;
                        for (JsonNode child : node.get(CHILDREN_FIELD)) {
                            if (child.get(TYPE_FIELD).asText().equals("Header")) {
                                long headerTs = getHeaderTs(msgReceiveTime, telemetryResult, child);
                                for (JsonNode headerChild : child.get(CHILDREN_FIELD)) {
                                    switch (headerChild.get(TYPE_FIELD).asText()) {
                                        case "MessageId":
                                            telemetryResult.add(KeyValueProto.newBuilder().setKey("deviceMessageId").setType(KeyValueType.LONG_V)
                                                    .setLongV(headerChild.get(CONTENT_FIELD).asLong()).build());
                                            break;
                                        case "SendAttempt":
                                            telemetryResult.add(KeyValueProto.newBuilder().setKey("sendAttempt").setType(KeyValueType.LONG_V)
                                                    .setLongV(headerChild.get(CONTENT_FIELD).asLong()).build());
                                            break;
                                        case "LastRoundTripTime":
                                            telemetryResult.add(KeyValueProto.newBuilder().setKey("lastRoundTripTime").setType(KeyValueType.LONG_V)
                                                    .setLongV(headerChild.get(CONTENT_FIELD).asLong()).build());
                                            break;
                                        case "RSSIOfAccessPoint":
                                            telemetryResult.add(KeyValueProto.newBuilder().setKey("rssiOfAccessPoint").setType(KeyValueType.LONG_V)
                                                    .setLongV(headerChild.get(CONTENT_FIELD).asLong()).build());
                                            break;
                                        case "MACOfAccessPoint":
                                            telemetryResult.add(KeyValueProto.newBuilder().setKey("macOfAccessPoint").setType(KeyValueType.LONG_V)
                                                    .setLongV(headerChild.get(CONTENT_FIELD).asLong()).build());
                                            break;
                                        case "DeviceInfo":
                                            for (JsonNode deviceInfoChild : headerChild.get(CHILDREN_FIELD)) {
                                                switch (deviceInfoChild.get(TYPE_FIELD).asText()) {
                                                    case "FirmwareVersion":
                                                        firmwareVersion = deviceInfoChild.get(CONTENT_FIELD).asLong();
                                                        break;
                                                    case "EventsInMemory":
                                                        telemetryResult.add(KeyValueProto.newBuilder().setKey("eventsInMemory").setType(KeyValueType.LONG_V)
                                                                .setLongV(deviceInfoChild.get(CONTENT_FIELD).asLong()).build());
                                                        break;
                                                    case "Memory":
                                                        telemetryResult.add(KeyValueProto.newBuilder().setKey("memory").setType(KeyValueType.LONG_V)
                                                                .setLongV(deviceInfoChild.get(CONTENT_FIELD).asLong()).build());
                                                        break;
                                                    case "TotalUptime":
                                                        telemetryResult.add(KeyValueProto.newBuilder().setKey("totalUptime").setType(KeyValueType.LONG_V)
                                                                .setLongV(deviceInfoChild.get(CONTENT_FIELD).asLong()).build());
                                                        break;
                                                    case "StoredEvents":
                                                        telemetryResult.add(KeyValueProto.newBuilder().setKey("storedEvents").setType(KeyValueType.LONG_V)
                                                                .setLongV(deviceInfoChild.get(CONTENT_FIELD).asLong()).build());
                                                        break;
                                                    case "AgeOfOldestEvent":
                                                        telemetryResult.add(KeyValueProto.newBuilder().setKey("ageOfOldestEvent").setType(KeyValueType.LONG_V)
                                                                .setLongV(deviceInfoChild.get(CONTENT_FIELD).asLong()).build());
                                                        break;
                                                }
                                            }
                                            break;
                                    }
                                }
                                telemetryResult.add(KeyValueProto.newBuilder().setKey("submitterMessageReceiveTime").setType(KeyValueType.LONG_V)
                                        .setLongV(msgReceiveTime).build());
                                telemetryResult.add(KeyValueProto.newBuilder().setKey("submitterUUID").setType(KeyValueType.STRING_V)
                                        .setStringV(rawData.get("submitterUUID").asText()).build());
                                telemetryResult.add(KeyValueProto.newBuilder().setKey("submitterDeviceType").setType(KeyValueType.STRING_V)
                                        .setStringV(rawData.get("submitterDeviceType").asText()).build());

                                telemetryBuilder.addTsKvList(buildTelemetryList(headerTs, telemetryResult));
                            } else {
                                int lastResetEventIndex = getLastResetEventIndex(child);
                                for (JsonNode bodyChild : child.get(CHILDREN_FIELD)) {
                                    if (bodyChild.get(TYPE_FIELD).asText().equals("EventList")) {
                                        JsonNode eventsArray = bodyChild.get(CHILDREN_FIELD);
                                        int eventsCount = eventsArray.size();
                                        int eventWithWrongTsIndex = 0;

                                        List<EventProcessingData> eventProcessingDataList = new ArrayList<>();
                                        List<String> eventsUids = new ArrayList<>();

                                        for (JsonNode eventNode : eventsArray) {
                                            if (lastResetEventIndex != 0) {
                                                lastResetEventIndex--;
                                                continue;
                                            }
                                            if (eventNode.get(TYPE_FIELD).asText().equals("Event")) {
                                                long eventTs = 0L;
                                                OphardtData event = new OphardtData();
                                                List<OphardtData> sensors = new ArrayList<>();

                                                DeduplicationData deduplicationData = new DeduplicationData();
                                                deduplicationData.setDeviceId(deviceName);
                                                deduplicationData.setDeviceType(deviceType);

                                                for (JsonNode eventChild : eventNode.get(CHILDREN_FIELD)) {
                                                    switch (eventChild.get(TYPE_FIELD).asText()) {
                                                        case "EventId":
                                                            deduplicationData.setEventId(eventChild.get(CONTENT_FIELD).asLong());
                                                            break;
                                                        case "EventTime":
                                                            eventTs = eventChild.get(CONTENT_FIELD).asLong();
                                                            if (eventTs < TS_FOR_2010_01_01) {
                                                                long tsCorrectionPart;
                                                                if (deviceTime < TS_FOR_2010_01_01) {
                                                                    tsCorrectionPart = msgReceiveTime - deviceTime;
                                                                    eventTs += tsCorrectionPart;
                                                                    tsCorrectionPartsMap.put(deviceName, tsCorrectionPart);
                                                                } else {
                                                                    if (tsCorrectionPartsMap.containsKey(deviceName)) {
                                                                        eventTs += tsCorrectionPartsMap.get(deviceName);
                                                                    } else {
                                                                        eventWithWrongTsIndex++;
                                                                        eventTs = findCorrectionWhenDeviceTimeIsValid(bodyChild, eventWithWrongTsIndex);
                                                                    }
                                                                }
                                                            } else if (eventTs - msgReceiveTime <= FIVE_MINUTES) {
                                                                lastValidEventTsBeforeReset = eventTs;
                                                            }
                                                            deduplicationData.setEventTime(eventTs);
                                                            break;
                                                        case "EventType":
                                                            String eventType = EventTypes.getEventTypeByValue(eventChild.get(CONTENT_FIELD).asLong());
                                                            event.setType(eventType);
                                                            deduplicationData.setEventType(eventType);
                                                            break;
                                                        case "EventValue":
                                                            if (eventChild.has(CONTENT_FIELD)) {
                                                                long eventValue = eventChild.get(CONTENT_FIELD).asLong();
                                                                event.setValue(eventValue);
                                                                deduplicationData.setEventValue(eventValue);
                                                            }
                                                            break;
                                                        case "SensorList":
                                                            processSensors(sensors, eventChild);
                                                            break;
                                                    }
                                                }
                                                if (isValidTs(msgReceiveTime, eventNode, eventTs)) {
                                                    continue;
                                                }

                                                UUID uuid = UUIDVersion5.generateUUID(UUIDVersion5.NAMESPACE_URL,
                                                        deduplicationData.toString());

                                                eventProcessingDataList.add(new EventProcessingData(uuid, eventNode, event,
                                                        sensors, eventTs, deviceName));
                                                eventsUids.add(findEvent(context, deviceName, uuid));
                                            }
                                        }
                                        stopProcessingMsg = getMsgProcessingFuture(context, uuidString, eventsMap,
                                                telemetryBuilder, eventsCount, eventProcessingDataList, eventsUids);
                                    }
                                }
                            }
                        }
                        context.saveDeviceAttributeValueInCache(deviceName, DataConstants.CLIENT_SCOPE, "lastEventTs", lastEventTsFromMsg);
                        attributesResult.add(KeyValueProto.newBuilder().setKey("lastEventTs")
                                .setType(KeyValueType.LONG_V).setLongV(lastEventTsFromMsg).build());
                        attributesResult.add(KeyValueProto.newBuilder().setKey("firmwareVersion")
                                .setType(KeyValueType.LONG_V).setLongV(firmwareVersion).build());
                        attributesBuilder.addAllKv(attributesResult);

                        if (!stopProcessingMsg) {
                            builder.attributesUpdate(attributesBuilder.build());
                            builder.telemetry(telemetryBuilder.build());
                        } else {
                            builder.attributesUpdate(PostAttributeMsg.getDefaultInstance());
                            builder.telemetry(PostTelemetryMsg.getDefaultInstance());
                        }
                        converterResult.setUplinkData(builder.build());
                        converterResult.setEventsMap(eventsMap);
                        break;
                }
            }
        }
        return converterResult;
    }

    private Boolean getMsgProcessingFuture(IntegrationContext context, String uuidString, Map<UUID, JsonNode> eventsMap,
                                           PostTelemetryMsg.Builder telemetryBuilder, int eventsCount,
                                           List<EventProcessingData> eventProcessingDataList, List<String> eventsUids) {
        int duplicatedEventsCount = 0;
        int index = 0;
        for (String eventUid : eventsUids) {
            if (eventUid != null && StringUtils.isEmpty(eventUid)) {
                log.error("[{}] Event is duplicated!", eventUid);
                duplicatedEventsCount++;
                if (duplicatedEventsCount == eventsCount) {
                    return true;
                }
            } else {
                EventProcessingData eventProcessingData = eventProcessingDataList.get(index);

                eventsMap.put(eventProcessingData.getUuid(), eventProcessingData.getNode());
                List<KeyValueProto> telemetryEventsResult = new ArrayList<>();
                saveSensors(telemetryEventsResult, eventProcessingData.getSensors(),
                        processAndSaveEvent(telemetryEventsResult, eventProcessingData.getEvent(), uuidString));
                lateEventsDetecting(context, telemetryEventsResult, eventProcessingData.getDeviceName(), eventProcessingData.getEventTs());

                telemetryBuilder.addTsKvList(buildTelemetryList(eventProcessingData.getEventTs(), telemetryEventsResult));
            }
            index++;
        }
        return false;
    }

    private TsKvListProto buildTelemetryList(long ts, List<KeyValueProto> telemetryListResult) {
        TsKvListProto.Builder tsKvListBuilder = TsKvListProto.newBuilder();
        tsKvListBuilder.setTs(ts);
        tsKvListBuilder.addAllKv(telemetryListResult);
        return tsKvListBuilder.build();
    }

    private String findEvent(IntegrationContext context, String deviceName, UUID uuid) {
        return context.findEventUid(deviceName, DataConstants.RAW_DATA, uuid.toString());
    }

    private void lateEventsDetecting(IntegrationContext context, List<KeyValueProto> telemetryEventsResult,
                                     String deviceName, long eventTs) {
        if (lastCorrectedEventTsPerDeviceMap.containsKey(deviceName)) {
            compareEventTsAndLastTs(telemetryEventsResult, eventTs, lastCorrectedEventTsPerDeviceMap.get(deviceName));
        } else {
            compareEventTsAndLastTs(telemetryEventsResult, eventTs, context.findDeviceAttributeValue(deviceName, DataConstants.CLIENT_SCOPE, "lastEventTs"));
        }
        lastEventTsFromMsg = eventTs;
        lastCorrectedEventTsPerDeviceMap.put(deviceName, lastEventTsFromMsg);
    }

    private void compareEventTsAndLastTs(List<KeyValueProto> telemetryEventsResult, long eventTs, long lastTs) {
        if (eventTs < lastTs) {
            telemetryEventsResult.add(KeyValueProto.newBuilder().setKey("ophardtLastEventTs")
                    .setType(KeyValueType.STRING_V).setStringV("true").build());
        }
    }

    private void processSensors(List<OphardtData> sensors, JsonNode eventChild) {
        for (JsonNode sensorNode : eventChild.get(CHILDREN_FIELD)) {
            if (sensorNode.get(TYPE_FIELD).asText().equals("Sensor")) {
                OphardtData sensor = new OphardtData();
                for (JsonNode sensorChild : sensorNode.get(CHILDREN_FIELD)) {
                    if (sensorChild.get(TYPE_FIELD).asText().equals("SensorType")) {
                        sensor.setType(SensorTypes.getSensorTypeByValue(sensorChild.get(CONTENT_FIELD).asLong()));
                    } else {
                        sensor.setValue(sensorChild.get(CONTENT_FIELD).asLong());
                    }
                }
                sensors.add(sensor);
            }
        }
    }

    private void saveSensors(List<KeyValueProto> telemetryEventsResult, List<OphardtData> sensors, String eventType) {
        for (OphardtData sensor : sensors) {
            if (sensor.getType() != null && sensor.getValue() != null) {
                telemetryEventsResult.add(KeyValueProto.newBuilder().setKey(eventType + UNDERSCORE_SYMBOLS + sensor.getType())
                        .setType(KeyValueType.LONG_V).setLongV(sensor.getValue()).build());
            }
        }
    }

    private String processAndSaveEvent(List<KeyValueProto> telemetryEventsResult, OphardtData event, String uuidString) {
        String eventType = event.getType();
        if (event.getValue() == null) {
            if (eventType.equals(MEASUREMENT_ERROR_EVENT_TYPE) ||
                    eventType.equals(SYSTEM_ERROR_EVENT_TYPE)) {
                telemetryEventsResult.add(KeyValueProto.newBuilder().setKey(eventType)
                        .setType(KeyValueType.STRING_V).setStringV("").build());
            } else {
                telemetryEventsResult.add(KeyValueProto.newBuilder().setKey(eventType)
                        .setType(KeyValueType.BOOLEAN_V).setBoolV(true).build());
            }
        } else {
            if (eventType.equals(MEASUREMENT_ERROR_EVENT_TYPE) || eventType.equals(SYSTEM_ERROR_EVENT_TYPE)) {
                telemetryEventsResult.add(KeyValueProto.newBuilder().setKey(eventType)
                        .setType(KeyValueType.STRING_V).setStringV(ErrorCodes.getErrorCodeByValue(event.getValue())).build());
            } else {
                telemetryEventsResult.add(KeyValueProto.newBuilder().setKey(eventType)
                        .setType(KeyValueType.LONG_V).setLongV(event.getValue()).build());
            }
        }
        telemetryEventsResult.add(KeyValueProto.newBuilder().setKey(eventType + RAW_DATA_UUID)
                .setType(KeyValueType.STRING_V).setStringV(uuidString).build());
        return eventType;
    }

    private long getHeaderTs(long msgReceiveTime, List<KeyValueProto> telemetryResult, JsonNode child) {
        long headerTs = 0L;
        for (JsonNode headerChild : child.get(CHILDREN_FIELD)) {
            if (headerChild.get(TYPE_FIELD).asText().equals("DeviceTime")) {
                deviceTime = headerChild.get(CONTENT_FIELD).asLong();
                if (deviceTime < TS_FOR_2010_01_01) {
                    headerTs = msgReceiveTime;
                } else {
                    headerTs = deviceTime;
                }
                telemetryResult.add(KeyValueProto.newBuilder().setKey("deviceTime")
                        .setType(KeyValueType.LONG_V).setLongV(deviceTime).build());
                break;
            }
        }
        return headerTs;
    }

    private boolean isValidTs(long msgReceiveTime, JsonNode eventNode, long ts) {
        if (ts - msgReceiveTime > FIVE_MINUTES || ts < TS_FOR_2010_01_01) {
            log.trace("Ts: [{}]. Discard event in the future (more than 5 minutes) and past - {}", ts, eventNode);
            return true;
        }
        return false;
    }

    private int getLastResetEventIndex(JsonNode child) {
        int resetEventsCount = 0;
        int lastResetEventIndex = 0;
        int index = 0;
        for (JsonNode bodyChild : child.get(CHILDREN_FIELD)) {
            if (bodyChild.get(TYPE_FIELD).asText().equals("EventList")) {
                for (JsonNode eventNode : bodyChild.get(CHILDREN_FIELD)) {
                    if (eventNode.get(TYPE_FIELD).asText().equals("Event")) {
                        for (JsonNode eventChild : eventNode.get(CHILDREN_FIELD)) {
                            if (eventChild.get(TYPE_FIELD).asText().equals("EventType") &&
                                    eventChild.get(CONTENT_FIELD).asLong() == RESET_EVENT_CODE) {
                                resetEventsCount++;
                                lastResetEventIndex = index;
                            }
                        }
                        index++;
                    }
                }
            }
        }
        if (resetEventsCount > 1) {
            return lastResetEventIndex;
        }
        return 0;
    }

    private long findCorrectionWhenDeviceTimeIsValid(JsonNode bodyChild, int eventWithWrongTsIndex) {
        int eventCount = 0;
        for (JsonNode eventNode : bodyChild.get(CHILDREN_FIELD)) {
            if (eventNode.get(TYPE_FIELD).asText().equals("Event")) {
                for (JsonNode eventChild : eventNode.get(CHILDREN_FIELD)) {
                    if (eventChild.get(TYPE_FIELD).asText().equals("EventTime")) {
                        long eventTs = eventChild.get(CONTENT_FIELD).asLong();
                        if (eventTs > TS_FOR_2010_01_01) {
                            return eventTs - 1 - (eventTs - lastValidEventTsBeforeReset) / eventCount * (eventCount - eventWithWrongTsIndex);
                        } else {
                            eventCount++;
                        }
                    }
                }
            }
        }
        return 0L;
    }
}
