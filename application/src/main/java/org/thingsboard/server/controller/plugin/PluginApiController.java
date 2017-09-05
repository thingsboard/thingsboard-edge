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
package org.thingsboard.server.controller.plugin;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.exception.ThingsboardException;
import org.thingsboard.server.extensions.api.plugins.PluginApiCallSecurityContext;
import org.thingsboard.server.extensions.api.plugins.PluginConstants;
import org.thingsboard.server.extensions.api.plugins.rest.BasicPluginRestMsg;
import org.thingsboard.server.extensions.api.plugins.rest.RestRequest;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(PluginConstants.PLUGIN_URL_PREFIX)
@Slf4j
public class PluginApiController extends BaseController {

    @Autowired
    private ActorService actorService;

    @Autowired
    private PluginService pluginService;

    @SuppressWarnings("rawtypes")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{pluginToken}/**")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(
            @PathVariable("pluginToken") String pluginToken,
            RequestEntity<byte[]> requestEntity,
            HttpServletRequest request)
            throws ThingsboardException {
        log.debug("[{}] Going to process requst uri: {}", pluginToken, requestEntity.getUrl());
        DeferredResult<ResponseEntity> result = new DeferredResult<ResponseEntity>();
        PluginMetaData pluginMd = pluginService.findPluginByApiToken(pluginToken);
        if (pluginMd == null) {
            result.setErrorResult(new PluginNotFoundException("Plugin with token: " + pluginToken + " not found!"));
        } else {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
            if (validatePluginAccess(pluginMd, tenantId, customerId)) {
                if(ModelConstants.NULL_UUID.equals(tenantId.getId())){
                    tenantId = null;
                }
                PluginApiCallSecurityContext securityCtx = new PluginApiCallSecurityContext(pluginMd.getTenantId(), pluginMd.getId(), tenantId, customerId);
                actorService.process(new BasicPluginRestMsg(securityCtx, new RestRequest(requestEntity, request), result));
            } else {
                result.setResult(new ResponseEntity<>(HttpStatus.FORBIDDEN));
            }

        }
        return result;
    }

    public static boolean validatePluginAccess(PluginMetaData pluginMd, TenantId tenantId, CustomerId customerId) {
        boolean systemAdministrator = tenantId == null || ModelConstants.NULL_UUID.equals(tenantId.getId());
        boolean tenantAdministrator = !systemAdministrator && (customerId == null || ModelConstants.NULL_UUID.equals(customerId.getId()));
        boolean systemPlugin = ModelConstants.NULL_UUID.equals(pluginMd.getTenantId().getId());

        boolean validUser = false;
        if (systemPlugin) {
            if (pluginMd.isPublicAccess() || systemAdministrator) {
                // All users can access public system plugins. Only system
                // users can access private system plugins
                validUser = true;
            }
        } else {
            if ((pluginMd.isPublicAccess() || tenantAdministrator) && tenantId.equals(pluginMd.getTenantId())) {
                // All tenant users can access public tenant plugins. Only tenant
                // administrator can access private tenant plugins
                validUser = true;
            }
        }
        return validUser;
    }
}
