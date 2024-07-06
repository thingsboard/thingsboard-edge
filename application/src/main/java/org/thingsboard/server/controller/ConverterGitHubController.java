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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.converter.TbConverterGitHubService;
import org.thingsboard.server.service.converter.TbFileNode;
import org.thingsboard.server.service.converter.TbGitHubContent;

import java.util.List;
import java.util.Map;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/githubconverter")
@Slf4j
public class ConverterGitHubController {

    private final TbConverterGitHubService converterGitHubService;


    @ApiOperation(value = "Get Converters from GitHub (getListFiles)",
            notes = "Returns list of all files from one Node Converters GitHub")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/node", method = RequestMethod.GET)
    @ResponseBody
    public TbGitHubContent[] getListFiles(@RequestParam(required = false) String pathDir) throws ThingsboardException {
        return converterGitHubService.listFiles(pathDir);
    }

    @ApiOperation(value = "Get Converters from GitHub (getListFiles)",
            notes = "Returns list of all files from Node Converters GitHub including child nodes.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/node/list", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> getListFilesNode(@RequestParam(required = false) String pathDir) throws ThingsboardException {
        return converterGitHubService.getAllFilesFromDirectory(pathDir);
    }

    @ApiOperation(value = "Get Converters from GitHub (getListFiles)",
            notes = "Returns tree of all files from Node Converters GitHub including child nodes.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/node/tree", method = RequestMethod.GET)
    @ResponseBody
    public TbFileNode buildFileTree(@RequestParam(required = false) String pathDir) throws ThingsboardException {
        return converterGitHubService.buildFileTree(pathDir);
    }

    @ApiOperation(value = "Get One Converter from GitHub (getFileContent)",
            notes = "Returns one file from Converters GitHub format: String (Json, base64, md))")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/filecontent", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> getFileContentString(@RequestParam String filePath) throws ThingsboardException {
        String content = converterGitHubService.getFileContentJson(filePath);
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get One Converter from GitHub (getFileContent)",
            notes = "Returns one file from Converters GitHub format: PNG")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/filecontent/png", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<byte[]> getFileContentPng(@RequestParam String filePath) throws ThingsboardException {
        try {
            byte[] content = converterGitHubService.getFileContentPng(filePath);
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
