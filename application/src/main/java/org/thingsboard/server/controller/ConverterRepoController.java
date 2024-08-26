/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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


import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.converter.TbConverterRepoService;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/converter")
@Slf4j
public class ConverterRepoController {

    private final TbConverterRepoService converterRepoService;

    @ApiOperation(value = "Get Arrays from GitHub (getIntegrationVendors)",
            notes = "Returns information about Vendors of Integration: a list of all  vendors...")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/vendors", method = RequestMethod.GET)
    @ResponseBody
    public ArrayNode getVendors(@PathVariable String integrationType)
                                                   throws ThingsboardException {
        return converterRepoService.getVendorsByIntegrationType(integrationType);
    }

    @ApiOperation(value = "Get Arrays from GitHub (getVendorModels)",
            notes = "Returns information about models of Vendor.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/models", method = RequestMethod.GET)
    @ResponseBody
    public ArrayNode ThingsStackIndustries(@PathVariable String integrationType,
                                           @PathVariable String vendorName)
                                                 throws ThingsboardException {
        return converterRepoService.getVendorModelsByIntegrationType(integrationType, vendorName);
    }

    @ApiOperation(value = "Get One File json from GitHub (getConverter)",
            notes = "Returns uplink converter.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/uplink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getConverterUplink(@PathVariable String integrationType,
                                                     @PathVariable String vendorName,
                                                     @PathVariable String model) throws ThingsboardException {
        String content = converterRepoService.getFileString(integrationType, vendorName, model, "uplink", "converter.json");;
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One File json from GitHub (getConverter)",
            notes = "Returns uplink metadata.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/uplink/metadata", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getMetadataUplink(@PathVariable String integrationType,
                                                    @PathVariable String vendorName,
                                                    @PathVariable String model) throws ThingsboardException {
        String content = converterRepoService.getFileString(integrationType, vendorName, model, "uplink", "metadata.json");;
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One File json from GitHub (getConverter)",
            notes = "Returns uplink Payload.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/uplink/payload", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getPayloadUplink(@PathVariable String integrationType,
                                                   @PathVariable String vendorName,
                                                   @PathVariable String model) throws ThingsboardException {
        String content = converterRepoService.getFileString(integrationType, vendorName, model, "uplink", "payload.json");;
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One File json from GitHub (getConverter)",
            notes = "Returns downlink converter.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/downlink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getConverterDownlink(@PathVariable String integrationType,
                                                       @PathVariable String vendorName,
                                                       @PathVariable String model) throws ThingsboardException {
        String content = converterRepoService.getFileString(integrationType, vendorName, model, "downlink", "converter.json");;
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One File json from GitHub (getConverter)",
            notes = "Returns downlink metadata.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/downlink/metadata", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getMetadataDownlink(@PathVariable String integrationType,
                                                      @PathVariable String vendorName,
                                                      @PathVariable String model) throws ThingsboardException {
        String content = converterRepoService.getFileString(integrationType, vendorName, model, "downlink", "metadata.json");;
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One File json from GitHub (getConverter)",
            notes = "Returns downlink Payload.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/downlink/payload", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getPayloadDownlink(@PathVariable String integrationType,
                                                     @PathVariable String vendorName,
                                                     @PathVariable String model) throws ThingsboardException {
        String content = converterRepoService.getFileString(integrationType, vendorName, model, "downlink", "payload.json");;
        return ResponseEntity.ok(content);
    }
}

