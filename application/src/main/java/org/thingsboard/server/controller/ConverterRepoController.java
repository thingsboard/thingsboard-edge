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


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.converter.TbConverterRepoService;
import org.thingsboard.server.service.converter.TbFileNode;
import org.thingsboard.server.service.converter.TbGitHubContent;

import java.util.List;
import java.util.Map;

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
    public TbGitHubContent[] getVendors(@RequestParam(required = false) String pathDir,
                                                   @PathVariable String integrationType)
                                                   throws ThingsboardException {
        return converterRepoService.getIntegrationVendors(pathDir, integrationType);
    }

    @ApiOperation(value = "Get Arrays from GitHub (getVendorModels)",
            notes = "Returns information about models of Vendor.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/models", method = RequestMethod.GET)
    @ResponseBody
    public TbGitHubContent[] getVendorModels(@RequestParam(required = false) String pathDir,
                                                 @PathVariable String integrationType,
                                                 @PathVariable String vendorName)
                                                 throws ThingsboardException {
        return converterRepoService.getVendorModels(pathDir, integrationType, vendorName);
    }

    @ApiOperation(value = "Get One File json from GitHub (getConverter)",
            notes = "Returns converter.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/uplink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getConverterUplink(@RequestParam String filePath,
                                               @PathVariable String integrationType,
                                               @PathVariable String model,
                                               @PathVariable String vendorName) throws ThingsboardException {
        String content = converterRepoService.getConverterUplink(filePath, integrationType, model, vendorName);
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One File json from GitHub (getMetadata)",
            notes = "Returns metadata.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/uplink/metadata", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getMetadataUplink(@RequestParam String filePath,
                                               @PathVariable String integrationType,
                                               @PathVariable String model,
                                               @PathVariable String vendorName) throws ThingsboardException {
        String content = converterRepoService.getMetadataUplink(filePath, integrationType, model, vendorName);
        return ResponseEntity.ok(content);
    }
    @ApiOperation(value = "Get One File json from GitHub (getPayload)",
            notes = "Returns payload.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = " /api/converter/{integrationType}/{vendorName}/{model}/uplink/payload", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getPayloadDownlink(@RequestParam String filePath,
                                               @PathVariable String integrationType,
                                               @PathVariable String model,
                                               @PathVariable String vendorName) throws ThingsboardException {
        String content = converterRepoService.getPayloadUplink(filePath, integrationType, model, vendorName);
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One File json from GitHub (getConverter)",
            notes = "Returns converter.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/downlink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getConverterDownlink(@RequestParam String filePath,
                                               @PathVariable String integrationType,
                                               @PathVariable String model,
                                               @PathVariable String vendorName) throws ThingsboardException {
        String content = converterRepoService.getConverterDownlink(filePath, integrationType, model, vendorName);
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One File json from GitHub (getMetadata)",
            notes = "Returns metadata.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/{integrationType}/{vendorName}/{model}/downlink/metadata", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getMetadataDownlink(@RequestParam String filePath,
                                               @PathVariable String integrationType,
                                               @PathVariable String model,
                                               @PathVariable String vendorName) throws ThingsboardException {
        String content = converterRepoService.getMetadataDownlink(filePath, integrationType, model, vendorName);
        return ResponseEntity.ok(content);
    }
    @ApiOperation(value = "Get One File json from GitHub (getPayload)",
            notes = "Returns payload.json file from Converters GitHub format: Json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = " /api/converter/{integrationType}/{vendorName}/{model}/downlink/payload", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getPayloadUplink(@RequestParam String filePath,
                                               @PathVariable String integrationType,
                                               @PathVariable String model,
                                               @PathVariable String vendorName) throws ThingsboardException {
        String content = converterRepoService.getPayloadDownlink(filePath, integrationType, model, vendorName);
        return ResponseEntity.ok(content);
    }


    @ApiOperation(value = "Get Converters from GitHub (getListFiles)",
            notes = "Returns list of all files from one Node Converters GitHub")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/node", method = RequestMethod.GET)
    @ResponseBody
    public TbGitHubContent[] getListFiles(@RequestParam(required = false) String pathDir) throws ThingsboardException {
        return converterRepoService.listFiles(pathDir);
    }

    @ApiOperation(value = "Get Converters from GitHub (getListFiles)",
            notes = "Returns list of all files from Node Converters GitHub including child nodes.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/node/list", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> getListFilesNode(@RequestParam(required = false) String pathDir) throws ThingsboardException {
        return converterRepoService.getAllFilesFromDirectory(pathDir);
    }

    @ApiOperation(value = "Get Converters from GitHub (getListFiles)",
            notes = "Returns tree of all files from Node Converters GitHub including child nodes.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/node/tree", method = RequestMethod.GET)
    @ResponseBody
    public TbFileNode buildFileTree(@RequestParam(required = false) String pathDir) throws ThingsboardException {
        return converterRepoService.buildFileTree(pathDir);
    }

    @ApiOperation(value = "Get One Converter from GitHub (getFileContent)",
            notes = "Returns one file from Converters GitHub format: String (Json, base64, md))")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/filecontent", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getFileContentString(@RequestParam String filePath) throws ThingsboardException {
        String content = converterRepoService.getFileContentJson(filePath);
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One Converter from GitHub (getFileContent)",
            notes = "Returns one file from Converters GitHub format: PNG")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/filecontent/png", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<byte[]> getFileContentPng(@RequestParam String filePath) throws ThingsboardException {
        try {
            byte[] content = converterRepoService.getFileContentPng(filePath);
            if (content != null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(content);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error occurred: [{}]", e.getMessage(), e);
            throw new ThingsboardException(e, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }
}
