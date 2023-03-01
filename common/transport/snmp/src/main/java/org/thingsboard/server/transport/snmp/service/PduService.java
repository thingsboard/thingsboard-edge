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
package org.thingsboard.server.transport.snmp.service;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@TbSnmpTransportComponent
@Service
@Slf4j
public class PduService {
    public PDU createPdu(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig, Map<String, String> values) {
        PDU pdu = setUpPdu(sessionContext);

        pdu.setType(communicationConfig.getMethod().getCode());
        pdu.addAll(communicationConfig.getAllMappings().stream()
                .filter(mapping -> values.isEmpty() || values.containsKey(mapping.getKey()))
                .map(mapping -> Optional.ofNullable(values.get(mapping.getKey()))
                        .map(value -> {
                            Variable variable = toSnmpVariable(value, mapping.getDataType());
                            return new VariableBinding(new OID(mapping.getOid()), variable);
                        })
                        .orElseGet(() -> new VariableBinding(new OID(mapping.getOid()))))
                .collect(Collectors.toList()));

        return pdu;
    }

    public PDU createSingleVariablePdu(DeviceSessionContext sessionContext, SnmpMethod snmpMethod, String oid, String value, DataType dataType) {
        PDU pdu = setUpPdu(sessionContext);
        pdu.setType(snmpMethod.getCode());

        Variable variable = value == null ? Null.instance : toSnmpVariable(value, dataType);
        pdu.add(new VariableBinding(new OID(oid), variable));

        return pdu;
    }

    private Variable toSnmpVariable(String value, DataType dataType) {
        dataType = dataType == null ? DataType.STRING : dataType;
        Variable variable;
        switch (dataType) {
            case LONG:
                try {
                    variable = new Integer32(Integer.parseInt(value));
                    break;
                } catch (NumberFormatException ignored) {
                }
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case JSON:
            default:
                variable = new OctetString(value);
        }
        return variable;
    }

    private PDU setUpPdu(DeviceSessionContext sessionContext) {
        PDU pdu;
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = sessionContext.getDeviceTransportConfiguration();
        SnmpProtocolVersion snmpVersion = deviceTransportConfiguration.getProtocolVersion();
        switch (snmpVersion) {
            case V1:
            case V2C:
                pdu = new PDU();
                break;
            case V3:
                ScopedPDU scopedPdu = new ScopedPDU();
                scopedPdu.setContextName(new OctetString(deviceTransportConfiguration.getContextName()));
                scopedPdu.setContextEngineID(new OctetString(deviceTransportConfiguration.getEngineId()));
                pdu = scopedPdu;
                break;
            default:
                throw new UnsupportedOperationException("SNMP version " + snmpVersion + " is not supported");
        }
        return pdu;
    }


    public JsonObject processPdu(PDU pdu, List<SnmpMapping> responseMappings) {
        Map<OID, String> values = processPdu(pdu);

        Map<OID, SnmpMapping> mappings = new HashMap<>();
        if (responseMappings != null) {
            for (SnmpMapping mapping : responseMappings) {
                OID oid = new OID(mapping.getOid());
                mappings.put(oid, mapping);
            }
        }

        JsonObject data = new JsonObject();
        values.forEach((oid, value) -> {
            log.trace("Processing variable binding: {} - {}", oid, value);

            SnmpMapping mapping = mappings.get(oid);
            if (mapping == null) {
                log.debug("No SNMP mapping for oid {}", oid);
                return;
            }

            processValue(mapping.getKey(), mapping.getDataType(), value, data);
        });

        return data;
    }

    public Map<OID, String> processPdu(PDU pdu) {
        return IntStream.range(0, pdu.size())
                .mapToObj(pdu::get)
                .filter(Objects::nonNull)
                .filter(variableBinding -> !(variableBinding.getVariable() instanceof Null))
                .collect(Collectors.toMap(VariableBinding::getOid, VariableBinding::toValueString));
    }

    public void processValue(String key, DataType dataType, String value, JsonObject result) {
        switch (dataType) {
            case LONG:
                result.addProperty(key, Long.parseLong(value));
                break;
            case BOOLEAN:
                result.addProperty(key, Boolean.parseBoolean(value));
                break;
            case DOUBLE:
                result.addProperty(key, Double.parseDouble(value));
                break;
            case STRING:
            case JSON:
            default:
                result.addProperty(key, value);
        }
    }
}
