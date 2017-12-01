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

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationSearchQuery;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardException;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class IntegrationController extends BaseController {

    public static final String INTEGRATION_ID = "integrationId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
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

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/integration", method = RequestMethod.POST)
    @ResponseBody
    public Integration saveIntegration(@RequestBody Integration integration) throws ThingsboardException {
        try {
            integration.setTenantId(getCurrentUser().getTenantId());
            if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                if (integration.getId() == null || integration.getId().isNullUid() ||
                        integration.getDefaultConverterId() == null || integration.getDefaultConverterId().isNullUid()) {
                    throw new ThingsboardException("You don't have permission to perform this operation!",
                            ThingsboardErrorCode.PERMISSION_DENIED);
                } else {
                    checkConverterId(integration.getDefaultConverterId());
                }
            }
            return checkNotNull(integrationService.saveIntegration(integration));
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
            checkIntegrationId(integrationId);
            integrationService.deleteIntegration(integrationId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/{converterId}/integration/{integrationId}", method = RequestMethod.POST)
    @ResponseBody
    public Integration assignIntegrationToConverter(@PathVariable("converterId") String strConverterId,
                                       @PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter("converterId", strConverterId);
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            ConverterId converterId = new ConverterId(toUUID(strConverterId));
            checkConverterId(converterId);

            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            checkIntegrationId(integrationId);

            return checkNotNull(integrationService.assignIntegrationToConverter(integrationId, converterId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/integration/{integrationId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Integration unassignIntegrationFromConverter(@PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            checkIntegrationId(integrationId);
            return checkNotNull(integrationService.unassignIntegrationFromConverter(integrationId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/integrations", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Integration> getTenantIntegrations(
            @RequestParam int limit,
            @RequestParam(required = false) IntegrationType type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.toString().trim().length() > 0) {
                return checkNotNull(integrationService.findIntegrationsByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(integrationService.findIntegrationsByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/integrations", params = {"integrationRoutingKey"}, method = RequestMethod.GET)
    @ResponseBody
    public Integration getTenantIntegration(
            @RequestParam String integrationRoutingKey) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(integrationService.findIntegrationByTenantIdAndRoutingKey(tenantId, integrationRoutingKey));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/converter/{converterId}/integrations", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Integration> getConverterIntegrations(
            @PathVariable("converterId") String strConverterId,
            @RequestParam int limit,
            @RequestParam(required = false) IntegrationType type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("converterId", strConverterId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            ConverterId converterId = new ConverterId(toUUID(strConverterId));
            checkConverterId(converterId);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.toString().trim().length() > 0) {
                return checkNotNull(integrationService.findIntegrationsByTenantIdAndConverterIdAndType(tenantId, converterId, type, pageLink));
            } else {
                return checkNotNull(integrationService.findIntegrationsByTenantIdAndConverterId(tenantId, converterId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/integrations", params = {"integrationIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Integration> getIntegrationsByIds(
            @RequestParam("integrationIds") String[] strIntegrationIds) throws ThingsboardException {
        checkArrayParameter("integrationIds", strIntegrationIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<IntegrationId> integrationIds = new ArrayList<>();
            for (String strIntegrationId : strIntegrationIds) {
                integrationIds.add(new IntegrationId(toUUID(strIntegrationId)));
            }
            ListenableFuture<List<Integration>> integrations;
            integrations = integrationService.findIntegrationsByTenantIdAndIdsAsync(tenantId, integrationIds);
            return checkNotNull(integrations.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/integrations", method = RequestMethod.POST)
    @ResponseBody
    public List<Integration> findByQuery(@RequestBody IntegrationSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getIntegrationTypes());
        checkEntityId(query.getParameters().getEntityId());
        try {
            List<Integration> integrations = checkNotNull(integrationService.findIntegrationsByQuery(query).get());
            integrations = integrations.stream().filter(integration -> {
                try {
                    checkIntegration(integration);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return integrations;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/integration/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getIntegrationTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> integrationTypes = integrationService.findIntegrationTypesByTenantId(tenantId);
            return checkNotNull(integrationTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
