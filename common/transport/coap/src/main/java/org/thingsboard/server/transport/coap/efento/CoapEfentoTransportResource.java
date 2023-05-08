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
package org.thingsboard.server.transport.coap.efento;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.EfentoCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos.ProtoChannel;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos.ProtoMeasurements;
import org.thingsboard.server.transport.coap.AbstractCoapTransportResource;
import org.thingsboard.server.transport.coap.CoapTransportContext;
import org.thingsboard.server.transport.coap.callback.CoapDeviceAuthCallback;
import org.thingsboard.server.transport.coap.callback.CoapEfentoCallback;
import org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.transport.coap.CoapTransportService.CONFIGURATION;
import static org.thingsboard.server.transport.coap.CoapTransportService.CURRENT_TIMESTAMP;
import static org.thingsboard.server.transport.coap.CoapTransportService.DEVICE_INFO;
import static org.thingsboard.server.transport.coap.CoapTransportService.MEASUREMENTS;

@Slf4j
public class CoapEfentoTransportResource extends AbstractCoapTransportResource {

    private static final int CHILD_RESOURCE_POSITION = 2;

    public CoapEfentoTransportResource(CoapTransportContext context, String name) {
        super(context, name);
        this.setObservable(true); // enable observing
        this.setObserveType(CoAP.Type.CON); // configure the notification type to CONs
//        this.getAttributes().setObservable(); // mark observable in the Link-Format
    }

    @Override
    protected void processHandleGet(CoapExchange exchange) {
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();
        List<String> uriPath = request.getOptions().getUriPath();
        boolean validPath = uriPath.size() == CHILD_RESOURCE_POSITION && uriPath.get(1).equals(CURRENT_TIMESTAMP);
        if (!validPath) {
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else {
            int dateInSec = (int) (System.currentTimeMillis() / 1000);
            byte[] bytes = ByteBuffer.allocate(4).putInt(dateInSec).array();
            exchange.respond(CoAP.ResponseCode.CONTENT, bytes);
        }
    }

    @Override
    protected void processHandlePost(CoapExchange exchange) {
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() != CHILD_RESOURCE_POSITION) {
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
            return;
        }
        String requestType = uriPath.get(1);
        switch (requestType) {
            case MEASUREMENTS:
                processMeasurementsRequest(exchange, request);
                break;
            case DEVICE_INFO:
            case CONFIGURATION:
                //We respond only to confirmed requests in order to reduce battery consumption for Efento devices.
                if (exchange.advanced().getRequest().isConfirmable()) {
                    exchange.respond(new Response(CoAP.ResponseCode.CREATED));
                }
                break;
            default:
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                break;
        }
    }

    private void processMeasurementsRequest(CoapExchange exchange, Request request) {
        byte[] bytes = request.getPayload();
        try {
            ProtoMeasurements protoMeasurements = ProtoMeasurements.parseFrom(bytes);
            log.trace("Successfully parsed Efento ProtoMeasurements: [{}]", protoMeasurements.getCloudToken());
            String token = protoMeasurements.getCloudToken();
            transportService.process(DeviceTransportType.COAP, ValidateDeviceTokenRequestMsg.newBuilder().setToken(token).build(),
                    new CoapDeviceAuthCallback(exchange, (msg, deviceProfile) -> {
                        TransportProtos.SessionInfoProto sessionInfo = SessionInfoCreator.create(msg, transportContext, UUID.randomUUID());
                        UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
                        try {
                            validateEfentoTransportConfiguration(deviceProfile);
                            List<EfentoMeasurements> efentoMeasurements = getEfentoMeasurements(protoMeasurements, sessionId);
                            transportService.process(sessionInfo,
                                    transportContext.getEfentoCoapAdaptor().convertToPostTelemetry(sessionId, efentoMeasurements),
                                    new CoapEfentoCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
                            reportSubscriptionInfo(sessionInfo, false, false);
                        } catch (AdaptorException e) {
                            log.error("[{}] Failed to decode Efento ProtoMeasurements: ", sessionId, e);
                            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                        }
                    }));
        } catch (Exception e) {
            log.error("Failed to decode Efento ProtoMeasurements: ", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Resource getChild(String name) {
        return this;
    }

    private void validateEfentoTransportConfiguration(DeviceProfile deviceProfile) throws AdaptorException {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration instanceof CoapDeviceProfileTransportConfiguration) {
            CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration =
                    (CoapDeviceProfileTransportConfiguration) transportConfiguration;
            if (!(coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration() instanceof EfentoCoapDeviceTypeConfiguration)) {
                throw new AdaptorException("Invalid CoapDeviceTypeConfiguration type: " + coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration().getClass().getSimpleName() + "!");
            }
        } else {
            throw new AdaptorException("Invalid DeviceProfileTransportConfiguration type" + transportConfiguration.getClass().getSimpleName() + "!");
        }
    }

    private List<EfentoMeasurements> getEfentoMeasurements(ProtoMeasurements protoMeasurements, UUID sessionId) {
        String serialNumber = CoapEfentoUtils.convertByteArrayToString(protoMeasurements.getSerialNum().toByteArray());
        boolean batteryStatus = protoMeasurements.getBatteryStatus();
        int measurementPeriodBase = protoMeasurements.getMeasurementPeriodBase();
        int measurementPeriodFactor = protoMeasurements.getMeasurementPeriodFactor();
        int signal = protoMeasurements.getSignal();
        List<ProtoChannel> channelsList = protoMeasurements.getChannelsList();
        Map<Long, JsonObject> valuesMap = new TreeMap<>();
        if (!CollectionUtils.isEmpty(channelsList)) {
            int channel = 0;
            JsonObject values;
            for (ProtoChannel protoChannel : channelsList) {
                channel++;
                boolean isBinarySensor = false;
                MeasurementType measurementType = protoChannel.getType();
                String measurementTypeName = measurementType.name();
                if (measurementType.equals(MeasurementType.OK_ALARM)
                        || measurementType.equals(MeasurementType.FLOODING)) {
                    isBinarySensor = true;
                }
                if (measurementPeriodFactor == 0 && isBinarySensor) {
                    measurementPeriodFactor = 14;
                } else {
                    measurementPeriodFactor = 1;
                }
                int measurementPeriod = measurementPeriodBase * measurementPeriodFactor;
                long measurementPeriodMillis = TimeUnit.SECONDS.toMillis(measurementPeriod);
                long nextTransmissionAtMillis = TimeUnit.SECONDS.toMillis(protoMeasurements.getNextTransmissionAt());
                int startPoint = protoChannel.getStartPoint();
                int startTimestamp = protoChannel.getTimestamp();
                long startTimestampMillis = TimeUnit.SECONDS.toMillis(startTimestamp);
                List<Integer> sampleOffsetsList = protoChannel.getSampleOffsetsList();
                if (!CollectionUtils.isEmpty(sampleOffsetsList)) {
                    int sampleOfssetsListSize = sampleOffsetsList.size();
                    for (int i = 0; i < sampleOfssetsListSize; i++) {
                        int sampleOffset = sampleOffsetsList.get(i);
                        Integer previousSampleOffset = isBinarySensor && i > 0 ? sampleOffsetsList.get(i - 1) : null;
                        if (sampleOffset == -32768) {
                            log.warn("[{}],[{}] Sensor error value! Ignoring.", sessionId, sampleOffset);
                        } else {
                            switch (measurementType) {
                                case TEMPERATURE:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("temperature_" + channel, ((double) (startPoint + sampleOffset)) / 10f);
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case WATER_METER:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("pulse_counter_water_" + channel, ((double) (startPoint + sampleOffset)));
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case HUMIDITY:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("humidity_" + channel, (double) (startPoint + sampleOffset));
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case ATMOSPHERIC_PRESSURE:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("pressure_" + channel, (double) (startPoint + sampleOffset) / 10f);
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case DIFFERENTIAL_PRESSURE:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("pressure_diff_" + channel, (double) (startPoint + sampleOffset));
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case OK_ALARM:
                                    boolean currentIsOk = sampleOffset < 0;
                                    if (previousSampleOffset != null) {
                                        boolean previousIsOk = previousSampleOffset < 0;
                                        boolean isOk = previousIsOk && currentIsOk;
                                        boolean isAlarm = !previousIsOk && !currentIsOk;
                                        if (isOk || isAlarm) {
                                            break;
                                        }
                                    }
                                    String data = currentIsOk ? "OK" : "ALARM";
                                    long sampleOffsetMillis = TimeUnit.SECONDS.toMillis(sampleOffset);
                                    long measurementTimestamp = startTimestampMillis + Math.abs(sampleOffsetMillis);
                                    values = valuesMap.computeIfAbsent(measurementTimestamp - 1000, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("ok_alarm_" + channel, data);
                                    break;
                                case NO_SENSOR:
                                case UNRECOGNIZED:
                                    log.trace("[{}][{}] Sensor error value! Ignoring.", sessionId, measurementTypeName);
                                    break;
                                default:
                                    log.trace("[{}],[{}] Unsupported measurementType! Ignoring.", sessionId, measurementTypeName);
                                    break;
                            }
                        }
                    }
                } else {
                    log.trace("[{}][{}] sampleOffsetsList list is empty!", sessionId, measurementTypeName);
                }
            }
        } else {
            throw new IllegalStateException("[" + sessionId + "]: Failed to get Efento measurements, reason: channels list is empty!");
        }
        if (!CollectionUtils.isEmpty(valuesMap)) {
            List<EfentoMeasurements> efentoMeasurements = new ArrayList<>();
            for (Long ts : valuesMap.keySet()) {
                EfentoMeasurements measurement = new EfentoMeasurements(ts, valuesMap.get(ts));
                efentoMeasurements.add(measurement);
            }
            return efentoMeasurements;
        } else {
            throw new IllegalStateException("[" + sessionId + "]: Failed to collect Efento measurements, reason, values map is empty!");
        }
    }

    @Data
    @AllArgsConstructor
    public static class EfentoMeasurements {

        private long ts;
        private JsonObject values;

    }
}
