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
package org.thingsboard.server.controller;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.integration.api.converter.AbstractDownlinkDataConverter;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.converter.JSDownlinkEvaluator;
import org.thingsboard.integration.api.converter.JSUplinkEvaluator;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
public class ConverterController extends BaseController {
    
    @Autowired
    private EventService eventService;

    @Autowired
    private JsInvokeService jsSandboxService;

    public static final String CONVERTER_ID = "converterId";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/{converterId}", method = RequestMethod.GET)
    @ResponseBody
    public Converter getConverterById(@PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        try {
            ConverterId converterId = new ConverterId(toUUID(strConverterId));
            return checkConverterId(converterId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter", method = RequestMethod.POST)
    @ResponseBody
    public Converter saveConverter(@RequestBody Converter converter) throws ThingsboardException {
        try {
            converter.setTenantId(getCurrentUser().getTenantId());
            boolean created = converter.getId() == null;

            Operation operation = created ? Operation.CREATE : Operation.WRITE;

            accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, operation,
                    converter.getId(), converter);

            Converter result = checkNotNull(converterService.saveConverter(converter));
            actorService.onEntityStateChange(result.getTenantId(), result.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            logEntityAction(result.getId(), result,
                    null,
                    converter.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return result;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.CONVERTER), converter,
                    null, converter.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converters", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Converter> getConverters(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(converterService.findTenantConverters(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/{converterId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteConverter(@PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        try {
            ConverterId converterId = new ConverterId(toUUID(strConverterId));
            Converter converter = checkConverterId(converterId, Operation.DELETE);
            converterService.deleteConverter(getTenantId(), converterId);
            actorService.onEntityStateChange(getTenantId(), converterId, ComponentLifecycleEvent.DELETED);

            logEntityAction(converterId, converter,
                    null,
                    ActionType.DELETED, null, strConverterId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.CONVERTER),
                    null,
                    null,
                    ActionType.DELETED, e, strConverterId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/{converterId}/debugIn", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getLatestConverterDebugInput(@PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        try {
            ConverterId converterId = new ConverterId(toUUID(strConverterId));
            checkConverterId(converterId, Operation.READ);
            List<Event> events = eventService.findLatestEvents(getTenantId(), converterId, DataConstants.DEBUG_CONVERTER, 1);
            JsonNode result = null;
            if (events != null && !events.isEmpty()) {
                Event event = events.get(0);
                JsonNode body = event.getBody();
                if (body.has("type")) {
                    String type = body.get("type").asText();
                    if (type.equals("Uplink") || type.equals("Downlink")) {
                        ObjectNode debugIn = objectMapper.createObjectNode();
                        String inContentType = body.get("inMessageType").asText();
                        debugIn.put("inContentType", inContentType);
                        if (type.equals("Uplink")) {
                            String inContent = body.get("in").asText();
                            debugIn.put("inContent", inContent);
                            String inMetadata = body.get("metadata").asText();
                            debugIn.put("inMetadata", inMetadata);
                        } else { //Downlink
                            String inContent = "";
                            String inMsgType = "";
                            String inMetadata = "";
                            String in = body.get("in").asText();
                            JsonNode inJson = objectMapper.readTree(in);
                            if (inJson.isArray() && inJson.size() > 0) {
                                JsonNode msgJson = inJson.get(inJson.size()-1);
                                JsonNode msg = msgJson.get("msg");
                                if (msg.isTextual()) {
                                    inContent = "";
                                } else if (msg.isObject()) {
                                    inContent = objectMapper.writeValueAsString(msg);
                                }
                                inMsgType = msgJson.get("msgType").asText();
                                inMetadata = objectMapper.writeValueAsString(msgJson.get("metadata"));
                            }
                            debugIn.put("inContent", inContent);
                            debugIn.put("inMsgType", inMsgType);
                            debugIn.put("inMetadata", inMetadata);
                            String inIntegrationMetadata = body.get("metadata").asText();
                            debugIn.put("inIntegrationMetadata", inIntegrationMetadata);
                        }
                        result = debugIn;
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/testUpLink", method = RequestMethod.POST)
    @ResponseBody
    public JsonNode testUpLinkConverter(@RequestBody JsonNode inputParams) throws ThingsboardException {
        try {
            String payloadBase64 = inputParams.get("payload").asText();
            byte[] payload = Base64.getDecoder().decode(payloadBase64);
            JsonNode metadata = inputParams.get("metadata");
            String decoder = inputParams.get("decoder").asText();

            Map<String, String> metadataMap = objectMapper.convertValue(metadata, new TypeReference<Map<String, String>>() {
            });
            UplinkMetaData uplinkMetaData = new UplinkMetaData("JSON", metadataMap);

            String output = "";
            String errorText = "";
            JSUplinkEvaluator jsUplinkEvaluator = null;
            try {
                jsUplinkEvaluator = new JSUplinkEvaluator(jsSandboxService, getCurrentUser().getId(), decoder);
                output = jsUplinkEvaluator.execute(payload, uplinkMetaData);
            } catch (Exception e) {
                log.error("Error evaluating JS UpLink Converter function", e);
                errorText = e.getMessage();
            } finally {
                if (jsUplinkEvaluator != null) {
                    jsUplinkEvaluator.destroy();
                }
            }
            ObjectNode result = objectMapper.createObjectNode();
            result.put("output", output);
            result.put("error", errorText);
            return result;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/testDownLink", method = RequestMethod.POST)
    @ResponseBody
    public JsonNode testDownLinkConverter(@RequestBody JsonNode inputParams) throws ThingsboardException {
        try {
            String data = inputParams.get("msg").asText();
            JsonNode metadata = inputParams.get("metadata");
            String msgType = inputParams.get("msgType").asText();
            JsonNode integrationMetadata = inputParams.get("integrationMetadata");
            String encoder = inputParams.get("encoder").asText();

            Map<String, String> metadataMap = objectMapper.convertValue(metadata, new TypeReference<Map<String, String>>() {});

            Map<String, String> integrationMetadataMap = objectMapper.convertValue(integrationMetadata, new TypeReference<Map<String, String>>() {});
            IntegrationMetaData integrationMetaData = new IntegrationMetaData(integrationMetadataMap);

            JsonNode output = null;
            String errorText = "";
            JSDownlinkEvaluator jsDownlinkEvaluator = null;
            try {
                TbMsg inMsg = TbMsg.createNewMsg(UUIDs.timeBased(), msgType, null, new TbMsgMetaData(metadataMap), data);
                jsDownlinkEvaluator = new JSDownlinkEvaluator(jsSandboxService, getCurrentUser().getId(), encoder);
                output = jsDownlinkEvaluator.execute(inMsg, integrationMetaData);
                validateDownLinkOutput(output);
            } catch (Exception e) {
                log.error("Error evaluating JS Downlink Converter function", e);
                errorText = e.getMessage();
            } finally {
                if (jsDownlinkEvaluator != null) {
                    jsDownlinkEvaluator.destroy();
                }
            }
            ObjectNode result = objectMapper.createObjectNode();
            result.put("output", objectMapper.writeValueAsString(output));
            result.put("error", errorText);
            return result;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void validateDownLinkOutput(JsonNode output) throws Exception {
        if (output.isArray()) {
            for (JsonNode downlinkJson : output) {
                if (downlinkJson.isObject()) {
                    validateDownLinkObject(downlinkJson);
                } else {
                    throw new JsonParseException("Invalid downlink output format!");
                }
            }
        } else if (output.isObject()) {
            validateDownLinkObject(output);
        } else {
            throw new JsonParseException("Invalid downlink output format!");
        }
    }

    private void validateDownLinkObject(JsonNode src) throws Exception {
        AbstractDownlinkDataConverter.parseDownlinkData(src);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converters", params = {"converterIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Converter> getConvertersByIds(
            @RequestParam("converterIds") String[] strConverterIds) throws ThingsboardException {
        checkArrayParameter("converterIds", strConverterIds);
        try {
            if (!accessControlService.hasPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ)) {
                return Collections.emptyList();
            }
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<ConverterId> converterIds = new ArrayList<>();
            for (String strConverterId : strConverterIds) {
                converterIds.add(new ConverterId(toUUID(strConverterId)));
            }
            List<Converter> converters = checkNotNull(converterService.findConvertersByIdsAsync(tenantId, converterIds).get());
            return filterConvertersByReadPermission(converters);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<Converter> filterConvertersByReadPermission(List<Converter> converters) {
        return converters.stream().filter(converter -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ, converter.getId(), converter);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }
}
