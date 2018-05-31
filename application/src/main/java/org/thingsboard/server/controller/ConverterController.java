/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.service.converter.AbstractDownlinkDataConverter;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.converter.IntegrationMetaData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.converter.js.JSDownlinkEvaluator;
import org.thingsboard.server.service.converter.js.JSUplinkEvaluator;
import org.thingsboard.server.service.script.JsSandboxService;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class ConverterController extends BaseController {

    @Autowired
    private DataConverterService dataConverterService;

    @Autowired
    private JsSandboxService jsSandboxService;

    public static final String CONVERTER_ID = "converterId";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/{converterId}", method = RequestMethod.GET)
    @ResponseBody
    public Converter getConverterById(@PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        try {
            ConverterId converterId = new ConverterId(toUUID(strConverterId));
            return checkConverterId(converterId);
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
            boolean create = converter.getId() == null;
            Converter result = checkNotNull(converterService.saveConverter(converter));
            if (create) {
                dataConverterService.createConverter(result);
            } else {
                dataConverterService.updateConverter(result);
            }

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
            Converter converter = checkConverterId(converterId);
            converterService.deleteConverter(converterId);
            dataConverterService.deleteConverter(converterId);

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
                jsUplinkEvaluator = new JSUplinkEvaluator(jsSandboxService, decoder);
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
                TbMsg inMsg = new TbMsg(UUIDs.timeBased(), msgType, null, new TbMsgMetaData(metadataMap), data, null, null, 0L);
                jsDownlinkEvaluator = new JSDownlinkEvaluator(jsSandboxService, encoder);
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
}
