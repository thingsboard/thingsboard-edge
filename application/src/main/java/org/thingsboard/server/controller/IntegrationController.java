/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;

@RestController
@RequestMapping("/api")
public class IntegrationController extends BaseController {

    private static final String INTEGRATION_ID = "integrationId";

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration/{integrationId}", method = RequestMethod.GET)
    @ResponseBody
    public Integration getIntegrationById(@PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            return checkIntegrationId(integrationId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration/routingKey/{routingKey}", method = RequestMethod.GET)
    @ResponseBody
    public Integration getIntegrationByRoutingKey(
            @PathVariable("routingKey") String routingKey) throws ThingsboardException {
        try {
            Integration integration = checkNotNull(integrationService.findIntegrationByRoutingKey(getTenantId(), routingKey));
            checkNotNull(integration);
            accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ, integration.getId(), integration);
            return integration;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration", method = RequestMethod.POST)
    @ResponseBody
    public Integration saveIntegration(@RequestBody Integration integration) throws ThingsboardException {
        try {
            integration.setTenantId(getCurrentUser().getTenantId());
            boolean created = integration.getId() == null;

            Operation operation = created ? Operation.CREATE : Operation.WRITE;

            accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, operation,
                    integration.getId(), integration);

            Integration result = checkNotNull(integrationService.saveIntegration(integration));
            actorService.onEntityStateChange(result.getTenantId(), result.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            logEntityAction(result.getId(), result, null, created ? ActionType.ADDED : ActionType.UPDATED, null);
            return result;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.INTEGRATION), integration,
                    null, integration.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }


    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integrations", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Integration> getIntegrations(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(integrationService.findTenantIntegrations(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration/{integrationId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteIntegration(@PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            Integration integration = checkIntegrationId(integrationId, Operation.DELETE);
            integrationService.deleteIntegration(getTenantId(), integrationId);

            actorService.onEntityStateChange(integration.getTenantId(), integration.getId(), ComponentLifecycleEvent.DELETED);

            logEntityAction(integrationId, integration,
                    null,
                    ActionType.DELETED, null, strIntegrationId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.INTEGRATION),
                    null,
                    null,
                    ActionType.DELETED, e, strIntegrationId);

            throw handleException(e);
        }
    }

}
