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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.exception.ThingsboardException;
import org.thingsboard.server.service.converter.DataConverterService;

import java.util.Base64;

@RestController
@RequestMapping("/api")
public class ConverterController extends BaseController {

    @Autowired
    private DataConverterService dataConverterService;

    public static final String CONVERTER_ID = "converterId";

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
            return result;
        } catch (Exception e) {
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
            checkConverterId(converterId);
            converterService.deleteConverter(converterId);
            dataConverterService.deleteConverter(converterId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/testCustomUpLink", method = RequestMethod.POST)
    @ResponseBody
    public JsonNode testCustomUpLinkConverter(@RequestBody JsonNode inputParams) throws ThingsboardException {
        try {
            String payloadBase64 = inputParams.get("payload").asText();
            byte[] payload = Base64.getDecoder().decode(payloadBase64);
            JsonNode metadata = inputParams.get("metadata");
            String decoder = inputParams.get("decoder").asText();

            //TODO:

            ObjectNode output = new ObjectMapper().createObjectNode();
            output.put("someAttr", "It works!");
            output.put("payloadBase64", payloadBase64);
            output.set("metadata", metadata);
            output.put("decoder", decoder);

            ObjectNode result = new ObjectMapper().createObjectNode();
            result.set("output", output);
            String errorText = ""; //OK if empty
            result.put("error", errorText);
            return result;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
