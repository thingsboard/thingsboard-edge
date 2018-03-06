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

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RuleController extends BaseController {

    public static final String RULE_ID = "ruleId";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/{ruleId}", method = RequestMethod.GET)
    @ResponseBody
    public RuleMetaData getRuleById(@PathVariable(RULE_ID) String strRuleId) throws ThingsboardException {
        checkParameter(RULE_ID, strRuleId);
        try {
            RuleId ruleId = new RuleId(toUUID(strRuleId));
            return checkRule(ruleService.findRuleById(ruleId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/token/{pluginToken}", method = RequestMethod.GET)
    @ResponseBody
    public List<RuleMetaData> getRulesByPluginToken(@PathVariable("pluginToken") String pluginToken) throws ThingsboardException {
        checkParameter("pluginToken", pluginToken);
        try {
            PluginMetaData plugin = checkPlugin(pluginService.findPluginByApiToken(pluginToken));
            return ruleService.findPluginRules(plugin.getApiToken());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule", method = RequestMethod.POST)
    @ResponseBody
    public RuleMetaData saveRule(@RequestBody RuleMetaData source) throws ThingsboardException {
        try {
            boolean created = source.getId() == null;
            source.setTenantId(getCurrentUser().getTenantId());
            RuleMetaData rule = checkNotNull(ruleService.saveRule(source));
            actorService.onRuleStateChange(rule.getTenantId(), rule.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            logEntityAction(rule.getId(), rule,
                    null,
                    created ? ActionType.ADDED : ActionType.UPDATED, null);

            return rule;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.RULE), source,
                    null, source.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/{ruleId}/activate", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void activateRuleById(@PathVariable(RULE_ID) String strRuleId) throws ThingsboardException {
        checkParameter(RULE_ID, strRuleId);
        try {
            RuleId ruleId = new RuleId(toUUID(strRuleId));
            RuleMetaData rule = checkRule(ruleService.findRuleById(ruleId));
            ruleService.activateRuleById(ruleId);
            actorService.onRuleStateChange(rule.getTenantId(), rule.getId(), ComponentLifecycleEvent.ACTIVATED);

            logEntityAction(rule.getId(), rule,
                    null,
                    ActionType.ACTIVATED, null, strRuleId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.RULE),
                    null,
                    null,
                    ActionType.ACTIVATED, e, strRuleId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/{ruleId}/suspend", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void suspendRuleById(@PathVariable(RULE_ID) String strRuleId) throws ThingsboardException {
        checkParameter(RULE_ID, strRuleId);
        try {
            RuleId ruleId = new RuleId(toUUID(strRuleId));
            RuleMetaData rule = checkRule(ruleService.findRuleById(ruleId));
            ruleService.suspendRuleById(ruleId);
            actorService.onRuleStateChange(rule.getTenantId(), rule.getId(), ComponentLifecycleEvent.SUSPENDED);

            logEntityAction(rule.getId(), rule,
                    null,
                    ActionType.SUSPENDED, null, strRuleId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.RULE),
                    null,
                    null,
                    ActionType.SUSPENDED, e, strRuleId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/rule/system", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<RuleMetaData> getSystemRules(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(ruleService.findSystemRules(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/rule/tenant/{tenantId}", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<RuleMetaData> getTenantRules(
            @PathVariable("tenantId") String strTenantId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(ruleService.findTenantRules(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rules", method = RequestMethod.GET)
    @ResponseBody
    public List<RuleMetaData> getRules() throws ThingsboardException {
        try {
            if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
                return checkNotNull(ruleService.findSystemRules());
            } else {
                TenantId tenantId = getCurrentUser().getTenantId();
                return checkNotNull(ruleService.findAllTenantRulesByTenantId(tenantId));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/rule", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<RuleMetaData> getTenantRules(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(ruleService.findTenantRules(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/rule/{ruleId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRule(@PathVariable(RULE_ID) String strRuleId) throws ThingsboardException {
        checkParameter(RULE_ID, strRuleId);
        try {
            RuleId ruleId = new RuleId(toUUID(strRuleId));
            RuleMetaData rule = checkRule(ruleService.findRuleById(ruleId));
            ruleService.deleteRuleById(ruleId);
            actorService.onRuleStateChange(rule.getTenantId(), rule.getId(), ComponentLifecycleEvent.DELETED);

            logEntityAction(ruleId, rule,
                    null,
                    ActionType.DELETED, null, strRuleId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.RULE),
                    null,
                    null,
                    ActionType.DELETED, e, strRuleId);

            throw handleException(e);
        }
    }

}
