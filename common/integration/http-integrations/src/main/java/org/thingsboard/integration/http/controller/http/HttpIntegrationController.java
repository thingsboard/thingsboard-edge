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
package org.thingsboard.integration.http.controller.http;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationHttpMsgProcessor;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.controller.BaseIntegrationController;
import org.thingsboard.integration.api.controller.BinaryHttpIntegrationMsg;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;
import org.thingsboard.integration.api.controller.StringHttpIntegrationMsg;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/integrations/http")
@Slf4j
@SuppressWarnings("rawtypes")
public class HttpIntegrationController extends BaseIntegrationController {

    @ApiOperation(value = "Process request from HTTP integrations", hidden = true)
    @RequestMapping(value = {"/{routingKey}", "/{routingKey}/{suffix}"}, method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(@PathVariable("routingKey") String routingKey,
                                                         @PathVariable("suffix") Optional<String> suffix,
                                                         @RequestHeader Map<String, String> requestHeaders,
                                                         InputStream request) throws ThingsboardException {
        log.debug("[{}] Received binary form request", routingKey);

        try {
            return processRequest(routingKey, suffix, new BinaryHttpIntegrationMsg(requestHeaders, IOUtils.toByteArray(request), new DeferredResult<>()));
        } catch (IOException e) {
            throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }


    @ApiOperation(value = "Process request from HTTP integrations", hidden = true)
    @RequestMapping(value = {"/{routingKey}", "/{routingKey}/{suffix}"}, method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(@PathVariable("routingKey") String routingKey,
                                                         @PathVariable("suffix") Optional<String> suffix,
                                                         MultipartHttpServletRequest request) {
        log.debug("[{}] Received multipart form request: {}", routingKey, request.getMultiFileMap().keySet());

        Map<String, String> requestHeaders = request.getRequestHeaders().toSingleValueMap();
        ObjectNode msg = JacksonUtil.newObjectNode();
        request.getMultiFileMap().forEach((fieldName, multipartFiles) -> {
            ArrayNode fileArrayNode = convertFilesToJsonFormat(multipartFiles);
            if (fileArrayNode.size() == 1) {
                msg.set(fieldName, fileArrayNode.get(0));
            } else {
                msg.set(fieldName, fileArrayNode);
            }
        });

        return processRequest(routingKey, suffix, new JsonHttpIntegrationMsg(requestHeaders, msg, new DeferredResult<>()));
    }

    @ApiOperation(value = "Process request from HTTP integrations", hidden = true)
    @RequestMapping(value = {"/{routingKey}", "/{routingKey}/{suffix}"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(@PathVariable("routingKey") String routingKey,
                                                         @PathVariable("suffix") Optional<String> suffix,
                                                         @RequestBody JsonNode msg,
                                                         @RequestHeader Map<String, String> requestHeaders) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        return processRequest(routingKey, suffix, new JsonHttpIntegrationMsg(requestHeaders, msg, new DeferredResult<>()));
    }

    @ApiOperation(value = "Process request from HTTP integrations", hidden = true)
    @RequestMapping(value = {"/{routingKey}", "/{routingKey}/{suffix}"}, method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {MediaType.TEXT_PLAIN_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(@PathVariable("routingKey") String routingKey,
                                                         @PathVariable("suffix") Optional<String> suffix,
                                                         @RequestBody String msg,
                                                         @RequestHeader Map<String, String> requestHeaders) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        return processRequest(routingKey, suffix, new StringHttpIntegrationMsg(requestHeaders, msg, new DeferredResult<>()));
    }

    @ApiOperation(value = "Process request from HTTP integrations", hidden = true)
    @RequestMapping(value = {"/{routingKey}", "/{routingKey}/{suffix}"}, method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(@PathVariable("routingKey") String routingKey,
                                                         @PathVariable("suffix") Optional<String> suffix,
                                                         @RequestParam Map<String, String> requestParams,
                                                         @RequestHeader Map<String, String> requestHeaders) {
        log.debug("[{}] Received status check request", routingKey);
        return processRequest(routingKey, suffix,
                new JsonHttpIntegrationMsg(requestHeaders, JacksonUtil.convertValue(requestParams, JsonNode.class), new DeferredResult<>()));
    }

    @SuppressWarnings("unchecked")
    private DeferredResult<ResponseEntity> processRequest(String routingKey,
                                                          Optional<String> suffix,
                                                          HttpIntegrationMsg msg) {
        DeferredResult<ResponseEntity> result = msg.getCallback();
        suffix.ifPresent(suffixStr -> msg.getRequestHeaders().put("suffix", suffixStr));
        api.process(IntegrationType.HTTP, routingKey, result, msg);
        return result;
    }

    private ArrayNode convertFilesToJsonFormat(List<MultipartFile> multipartFiles) {
        ArrayNode fileArrayNode = JacksonUtil.newArrayNode();
        for (MultipartFile multipartFile : multipartFiles) {
            try (InputStream fileIS = multipartFile.getInputStream();) {
                byte[] fileBytes = new byte[(int) multipartFile.getSize()];
                fileIS.read(fileBytes, 0, fileBytes.length);
                try {
                    JsonNode jsonInterpretation = JacksonUtil.fromBytes(fileBytes);
                    fileArrayNode.add(jsonInterpretation);
                } catch (IllegalArgumentException e) {
                    String stringInterpretation = Base64Utils.encodeToString(fileBytes);
                    fileArrayNode.add(stringInterpretation);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file " + multipartFile.getName() + ".");
            }
        }
        return fileArrayNode;
    }

    @ApiOperation(value = "Process request from HTTP integrations", hidden = true)
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{routingKey}", method = {RequestMethod.GET})
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> checkStatus(@PathVariable("routingKey") String routingKey,
                                                      @RequestParam Map<String, String> requestParams,
                                                      @RequestHeader Map<String, String> requestHeaders) {
        log.debug("[{}] Received status check request", routingKey);
        DeferredResult<ResponseEntity> result = new DeferredResult<>();

        ObjectNode json = JacksonUtil.newObjectNode();
        if (requestParams.size() > 0) {
            requestParams.forEach(json::put);
        }

        JsonHttpIntegrationMsg msg = new JsonHttpIntegrationMsg(requestHeaders, json, result);

        api.process(IntegrationType.HTTP, routingKey, result, msg, (integration, tmpResult, tmpMsg) -> {
            if (requestParams.size() > 0) {
                integration.process(tmpMsg);
            } else {
                 tmpResult.setResult(new ResponseEntity<>(HttpStatus.OK));
            }
        });

        return result;
    }

}
