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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParseException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.AbstractDownlinkDataConverter;
import org.thingsboard.integration.api.converter.ScriptDownlinkEvaluator;
import org.thingsboard.integration.api.converter.ScriptUplinkEvaluator;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkContentType;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.converter.TbConverterService;
import org.thingsboard.server.service.script.RuleNodeJsScriptEngine;
import org.thingsboard.server.service.script.RuleNodeTbelScriptEngine;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_CONFIGURATION_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_DEBUG_INPUT_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TEST_DOWNLINK_CONVERTER_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.TEST_UPLINK_CONVERTER_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class ConverterController extends AutoCommitController {

    private final EventService eventService;
    private final JsInvokeService jsInvokeService;
    private final Optional<TbelInvokeService> tbelInvokeService;
    private final TbConverterService tbConverterService;

    public static final String CONVERTER_ID = "converterId";

    @ApiOperation(value = "Get Converter (getConverterById)",
            notes = "Fetch the Converter object based on the provided Converter Id. " +
                    "The server checks that the converter is owned by the same tenant. "
                    + NEW_LINE + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/{converterId}", method = RequestMethod.GET)
    @ResponseBody
    public Converter getConverterById(@ApiParam(required = true, value = CONVERTER_ID_PARAM_DESCRIPTION)
                                      @PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        ConverterId converterId = new ConverterId(toUUID(strConverterId));
        return checkConverterId(converterId, Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Converter (saveConverter)",
            notes = "Create or update the Converter. When creating converter, platform generates Converter Id as " + UUID_WIKI_LINK +
                    "The newly created converter id will be present in the response. " +
                    "Specify existing Converter id to update the converter. " +
                    "Referencing non-existing converter Id will cause 'Not Found' error. " +
                    "Converter name is unique in the scope of tenant. " + NEW_LINE +
                    CONVERTER_CONFIGURATION_DESCRIPTION +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new converter entity. " +
                    TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter", method = RequestMethod.POST)
    @ResponseBody
    public Converter saveConverter(@ApiParam(required = true, value = "A JSON value representing the converter.") @RequestBody Converter converter) throws Exception {
        converter.setTenantId(getCurrentUser().getTenantId());
        checkEntity(converter.getId(), converter, Resource.CONVERTER, null);
        return tbConverterService.save(converter, getCurrentUser());
    }

    @ApiOperation(value = "Get Converters (getConverters)",
            notes = "Returns a page of converters owned by tenant. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converters", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Converter> getConverters(
            @ApiParam(value = "Fetch edge template converters")
            @RequestParam(value = "isEdgeTemplate", required = false, defaultValue = "false") boolean isEdgeTemplate,
            @ApiParam(required = true, value = PAGE_SIZE_DESCRIPTION, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(required = true, value = PAGE_NUMBER_DESCRIPTION, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = CONVERTER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = CONVERTER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (isEdgeTemplate) {
            return checkNotNull(converterService.findTenantEdgeTemplateConverters(tenantId, pageLink));
        } else {
            return checkNotNull(converterService.findTenantConverters(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Delete converter (deleteConverter)",
            notes = "Deletes the converter and all the relations (from and to the converter). " +
                    "Referencing non-existing converter Id will cause an error. " +
                    "If the converter is associated with the integration, it will not be allowed for deletion." + NEW_LINE + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/{converterId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteConverter(@ApiParam(required = true, value = CONVERTER_ID_PARAM_DESCRIPTION) @PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        ConverterId converterId = new ConverterId(toUUID(strConverterId));
        Converter converter = checkConverterId(converterId, Operation.DELETE);
        tbConverterService.delete(converter, getCurrentUser());
    }

    @ApiOperation(value = "Get latest debug input event (getLatestConverterDebugInput)",
            notes = "Returns a JSON object of the latest debug event representing the input message the converter processed. " + NEW_LINE +
                    CONVERTER_DEBUG_INPUT_DEFINITION +
                    NEW_LINE + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/{converterId}/debugIn", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getLatestConverterDebugInput(@ApiParam(required = true, value = CONVERTER_ID_PARAM_DESCRIPTION)
                                                 @PathVariable(CONVERTER_ID) String strConverterId) throws Exception {
        checkParameter(CONVERTER_ID, strConverterId);
        ConverterId converterId = new ConverterId(toUUID(strConverterId));
        checkConverterId(converterId, Operation.READ);
        List<EventInfo> events = eventService.findLatestEvents(getTenantId(), converterId, EventType.DEBUG_CONVERTER, 1);
        JsonNode result = null;
        if (events != null && !events.isEmpty()) {
            EventInfo event = events.get(0);
            JsonNode body = event.getBody();
            if (body.has("type")) {
                String type = body.get("type").asText();
                if (type.equals("Uplink") || type.equals("Downlink")) {
                    ObjectNode debugIn = JacksonUtil.newObjectNode();
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
                        JsonNode inJson = JacksonUtil.toJsonNode(in);
                        if (inJson.isArray() && inJson.size() > 0) {
                            JsonNode msgJson = inJson.get(inJson.size() - 1);
                            JsonNode msg = msgJson.get("msg");
                            if (msg.isTextual()) {
                                inContent = "";
                            } else if (msg.isObject()) {
                                inContent = JacksonUtil.toString(msg);
                            }
                            inMsgType = msgJson.get("msgType").asText();
                            inMetadata = JacksonUtil.toString(msgJson.get("metadata"));
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
    }

    @ApiOperation(value = "Test converter function (testUpLinkConverter)",
            notes = "Returns a JSON object representing the result of the processed incoming message. " + NEW_LINE +
                    TEST_UPLINK_CONVERTER_DEFINITION
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/testUpLink", method = RequestMethod.POST)
    @ResponseBody
    public JsonNode testUpLinkConverter(
            @ApiParam(value = "Script language: JS or TBEL")
            @RequestParam(required = false) ScriptLanguage scriptLang,
            @ApiParam(required = true, value = "A JSON value representing the input to the converter function.")
            @RequestBody JsonNode inputParams) throws ThingsboardException {
        String payloadBase64 = inputParams.get("payload").asText();
        byte[] payload = Base64.getDecoder().decode(payloadBase64);
        JsonNode metadata = inputParams.get("metadata");
        String decoder = inputParams.get("decoder").asText();

        Map<String, String> metadataMap = JacksonUtil.convertValue(metadata, new TypeReference<>() {
        });
        UplinkMetaData uplinkMetaData = new UplinkMetaData(UplinkContentType.JSON, metadataMap);

        String output = "";
        String errorText = "";
        ScriptUplinkEvaluator scriptUplinkEvaluator = null;
        try {
            scriptUplinkEvaluator = new ScriptUplinkEvaluator(getTenantId(), getScriptInvokeService(scriptLang), getCurrentUser().getId(), decoder);
            output = scriptUplinkEvaluator.execute(payload, uplinkMetaData).get();
        } catch (Exception e) {
            log.error("Error evaluating JS UpLink Converter function", e);
            errorText = e.getMessage();
        } finally {
            if (scriptUplinkEvaluator != null) {
                scriptUplinkEvaluator.destroy();
            }
        }
        ObjectNode result = JacksonUtil.newObjectNode();
        result.put("output", output);
        result.put("error", errorText);
        return result;
    }

    @ApiOperation(value = "Test converter function (testDownLinkConverter)",
            notes = "Returns a JSON object representing the result of the processed incoming message. " + NEW_LINE +
                    TEST_DOWNLINK_CONVERTER_DEFINITION
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/testDownLink", method = RequestMethod.POST)
    @ResponseBody
    public JsonNode testDownLinkConverter(
            @ApiParam(value = "Script language: JS or TBEL")
            @RequestParam(required = false) ScriptLanguage scriptLang,
            @ApiParam(required = true, value = "A JSON value representing the input to the converter function.")
            @RequestBody JsonNode inputParams) throws Exception {
        String data = inputParams.get("msg").asText();
        JsonNode metadata = inputParams.get("metadata");
        String msgType = inputParams.get("msgType").asText();
        JsonNode integrationMetadata = inputParams.get("integrationMetadata");
        String encoder = inputParams.get("encoder").asText();

        Map<String, String> metadataMap = JacksonUtil.convertValue(metadata, new TypeReference<Map<String, String>>() {
        });

        Map<String, String> integrationMetadataMap = JacksonUtil.convertValue(integrationMetadata, new TypeReference<Map<String, String>>() {
        });
        IntegrationMetaData integrationMetaData = new IntegrationMetaData(integrationMetadataMap);

        JsonNode output = null;
        String errorText = "";
        ScriptDownlinkEvaluator scriptDownlinkEvaluator = null;
        try {
            TbMsg inMsg = TbMsg.newMsg(msgType, null, new TbMsgMetaData(metadataMap), data);
            scriptDownlinkEvaluator = new ScriptDownlinkEvaluator(getTenantId(), getScriptInvokeService(scriptLang), getCurrentUser().getId(), encoder);
            output = scriptDownlinkEvaluator.execute(inMsg, integrationMetaData);
            validateDownLinkOutput(output);
        } catch (Exception e) {
            log.error("Error evaluating JS Downlink Converter function", e);
            errorText = e.getMessage();
        } finally {
            if (scriptDownlinkEvaluator != null) {
                scriptDownlinkEvaluator.destroy();
            }
        }
        ObjectNode result = JacksonUtil.newObjectNode();
        result.put("output", JacksonUtil.toString(output));
        result.put("error", errorText);
        return result;
    }

    private ScriptInvokeService getScriptInvokeService(ScriptLanguage scriptLang) {
        ScriptInvokeService scriptInvokeService;
        if (scriptLang == null) {
            scriptLang = ScriptLanguage.JS;
        }
        if (ScriptLanguage.JS.equals(scriptLang)) {
            scriptInvokeService = jsInvokeService;
        } else {
            if (tbelInvokeService.isEmpty()) {
                throw new IllegalArgumentException("TBEL script engine is disabled!");
            }
            scriptInvokeService = tbelInvokeService.get();
        }
        return scriptInvokeService;
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

    @ApiOperation(value = "Get Converters By Ids (getConvertersByIds)",
            notes = "Requested converters must be owned by tenant which is performing the request. " +
                    NEW_LINE + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converters", params = {"converterIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Converter> getConvertersByIds(
            @ApiParam(value = "A list of converter ids, separated by comma ','", required = true)
            @RequestParam("converterIds") String[] strConverterIds) throws Exception {
        checkArrayParameter("converterIds", strConverterIds);
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
