/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.secret.TbSecretService;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SECRET_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class SecretController extends BaseController {

    private final TbSecretService tbSecretService;

    @ApiOperation(value = "Save or Update Secret (saveSecret)",
            notes = "Create or update the Secret. When creating secret, platform generates Secret Id as " + UUID_WIKI_LINK +
                    "The newly created Secret Id will be present in the response. " +
                    "Specify existing Secret Id to update the secret. Secret name is not updatable, only value could be changed. " +
                    "Referencing non-existing Secret Id will cause 'Not Found' error." + NEW_LINE +
                    "Secret name is unique in the scope of tenant.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/secret")
    public SecretInfo saveSecret(
            @Parameter(description = "A JSON value representing the Secret.", required = true)
            @RequestBody @Valid Secret secret) throws Exception {
        secret.setTenantId(getTenantId());
        checkEntity(secret.getId(), secret, Resource.SECRET);
        return tbSecretService.save(secret, getCurrentUser());
    }

    @ApiOperation(value = "Update Secret Description",
            notes = "Updates the description of the existing Secret by secretId. " +
                    "Only the description can be updated. " +
                    "Referencing a non-existing Secret Id will cause a 'Not Found' error.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping("/secret/{id}/description")
    public SecretInfo updateSecretDescription(
            @Parameter(description = "Unique identifier of the Secret to update", required = true)
            @PathVariable UUID id,
            @Parameter(description = "New description for the Secret", example = "Description", required = true)
            @RequestBody String description) throws Exception {
        SecretId secretId = new SecretId(id);
        Secret secret = new Secret(checkSecretId(secretId, Operation.WRITE));
        secret.setDescription(description);
        return tbSecretService.save(secret, getCurrentUser());
    }

    @ApiOperation(value = "Delete secret by ID (deleteSecret)",
            notes = "Deletes the secret. Referencing non-existing Secret Id will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = "/secret/{id}")
    public ResponseEntity<TbSecretDeleteResult> deleteSecret(@PathVariable UUID id,
                                                             @RequestParam(name = "force", required = false) boolean force) throws ThingsboardException {
        SecretId secretId = new SecretId(id);
        SecretInfo secretInfo = checkSecretId(secretId, Operation.DELETE);
        TbSecretDeleteResult result = tbSecretService.delete(secretInfo, force, getCurrentUser());
        return (result.isSuccess() ? ResponseEntity.ok() : ResponseEntity.badRequest()).body(result);
    }

    @ApiOperation(value = "Get Tenant Secret infos (getSecretInfos)",
            notes = "Returns a page of secret infos owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/secret/infos", params = {"pageSize", "page"})
    public PageData<SecretInfo> getSecretInfos(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                               @RequestParam int pageSize,
                                               @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                               @RequestParam int page,
                                               @Parameter(description = SECRET_TEXT_SEARCH_DESCRIPTION)
                                               @RequestParam(required = false) String textSearch,
                                               @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"name", "key"}))
                                               @RequestParam(required = false) String sortProperty,
                                               @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                               @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getTenantId();
        return checkNotNull(secretService.findSecretInfosByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Secret names (getSecretNames)",
            notes = "Returns a page of secret names owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/secret/names")
    public List<String> getSecretNames() throws ThingsboardException {
        TenantId tenantId = getTenantId();
        return checkNotNull(secretService.findSecretNamesByTenantId(tenantId));
    }

    @ApiOperation(value = "Get Secret info by Id (getSecretInfoById)", notes = TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/secret/info/{id}")
    public SecretInfo getSecretInfoById(@PathVariable UUID id) throws ThingsboardException {
        SecretId secretId = new SecretId(id);
        return checkSecretId(secretId, Operation.READ);
    }

}
