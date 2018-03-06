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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardException;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

@RestController
@RequestMapping("/api")
public class IntegrationController extends BaseController {

    private static final String INTEGRATION_ID = "integrationId";

    @Autowired
    private PlatformIntegrationService platformIntegrationService;


    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration/{integrationId}", method = RequestMethod.GET)
    @ResponseBody
    public Integration getIntegrationById(@PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            return checkIntegrationId(integrationId);
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
            Integration integration = checkNotNull(integrationService.findIntegrationByRoutingKey(routingKey));
            return checkIntegration(integration);
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
            boolean create = integration.getId() == null;
            Integration old = null;
            if (!create) {
                old = checkNotNull(integrationService.findIntegrationById(integration.getId()));
            }
            Integration result = checkNotNull(integrationService.saveIntegration(integration));
            try {
                if (create) {
                    platformIntegrationService.createIntegration(result);
                } else {
                    platformIntegrationService.updateIntegration(result);
                }
            } catch (Exception e) {
                if (create) {
                    integrationService.deleteIntegration(result.getId()); e.printStackTrace();
                } else {
                    integrationService.saveIntegration(old);
                    platformIntegrationService.updateIntegration(old);
                }
                throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }

            logEntityAction(result.getId(), result,
                    null,
                    integration.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

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
            Integration integration = checkIntegrationId(integrationId);
            integrationService.deleteIntegration(integrationId);
            platformIntegrationService.deleteIntegration(integrationId);

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
