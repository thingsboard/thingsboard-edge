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

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PluginController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/{pluginId}", method = RequestMethod.GET)
    @ResponseBody
    public PluginMetaData getPluginById(@PathVariable("pluginId") String strPluginId) throws ThingsboardException {
        checkParameter("pluginId", strPluginId);
        try {
            PluginId pluginId = new PluginId(toUUID(strPluginId));
            return checkPlugin(pluginService.findPluginById(pluginId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/token/{pluginToken}", method = RequestMethod.GET)
    @ResponseBody
    public PluginMetaData getPluginByToken(@PathVariable("pluginToken") String pluginToken) throws ThingsboardException {
        checkParameter("pluginToken", pluginToken);
        try {
            return checkPlugin(pluginService.findPluginByApiToken(pluginToken));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin", method = RequestMethod.POST)
    @ResponseBody
    public PluginMetaData savePlugin(@RequestBody PluginMetaData source) throws ThingsboardException {
        try {
            boolean created = source.getId() == null;
            source.setTenantId(getCurrentUser().getTenantId());
            PluginMetaData plugin = checkNotNull(pluginService.savePlugin(source));
            actorService.onPluginStateChange(plugin.getTenantId(), plugin.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return plugin;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/{pluginId}/activate", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void activatePluginById(@PathVariable("pluginId") String strPluginId) throws ThingsboardException {
        checkParameter("pluginId", strPluginId);
        try {
            PluginId pluginId = new PluginId(toUUID(strPluginId));
            PluginMetaData plugin = checkPlugin(pluginService.findPluginById(pluginId));
            pluginService.activatePluginById(pluginId);
            actorService.onPluginStateChange(plugin.getTenantId(), plugin.getId(), ComponentLifecycleEvent.ACTIVATED);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/{pluginId}/suspend", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void suspendPluginById(@PathVariable("pluginId") String strPluginId) throws ThingsboardException {
        checkParameter("pluginId", strPluginId);
        try {
            PluginId pluginId = new PluginId(toUUID(strPluginId));
            PluginMetaData plugin = checkPlugin(pluginService.findPluginById(pluginId));
            pluginService.suspendPluginById(pluginId);
            actorService.onPluginStateChange(plugin.getTenantId(), plugin.getId(), ComponentLifecycleEvent.SUSPENDED);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/plugin/system", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<PluginMetaData> getSystemPlugins(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(pluginService.findSystemPlugins(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/plugin/tenant/{tenantId}", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<PluginMetaData> getTenantPlugins(
            @PathVariable("tenantId") String strTenantId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(pluginService.findTenantPlugins(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugins", method = RequestMethod.GET)
    @ResponseBody
    public List<PluginMetaData> getPlugins() throws ThingsboardException {
        try {
            if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
                return checkNotNull(pluginService.findSystemPlugins());
            } else {
                TenantId tenantId = getCurrentUser().getTenantId();
                List<PluginMetaData> plugins = checkNotNull(pluginService.findAllTenantPluginsByTenantId(tenantId));
                plugins.stream()
                        .filter(plugin -> plugin.getTenantId().getId().equals(ModelConstants.NULL_UUID))
                        .forEach(plugin -> plugin.setConfiguration(null));
                return plugins;
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/plugin", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<PluginMetaData> getTenantPlugins(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(pluginService.findTenantPlugins(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/plugin/{pluginId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deletePlugin(@PathVariable("pluginId") String strPluginId) throws ThingsboardException {
        checkParameter("pluginId", strPluginId);
        try {
            PluginId pluginId = new PluginId(toUUID(strPluginId));
            PluginMetaData plugin = checkPlugin(pluginService.findPluginById(pluginId));
            pluginService.deletePluginById(pluginId);
            actorService.onPluginStateChange(plugin.getTenantId(), plugin.getId(), ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


}
