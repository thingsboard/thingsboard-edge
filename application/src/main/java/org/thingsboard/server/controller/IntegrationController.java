/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.IntegrationManagerService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_CONFIGURATION_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class IntegrationController extends BaseController {

    @Autowired
    private IntegrationManagerService integrationManagerService;

    private static final String INTEGRATION_ID = "integrationId";

    @ApiOperation(value = "Get Integration (getIntegrationById)",
            notes = "Fetch the Integration object based on the provided Integration Id. " +
                    "The server checks that the integration is owned by the same tenant. "
                    + NEW_LINE + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration/{integrationId}", method = RequestMethod.GET)
    @ResponseBody
    public Integration getIntegrationById(@ApiParam(required = true, value = INTEGRATION_ID_PARAM_DESCRIPTION)
                                          @PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            return checkIntegrationId(integrationId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Integration by Routing Key (getIntegrationByRoutingKey)",
            notes = "Fetch the Integration object based on the provided routing key. " +
                    "The server checks that the integration is owned by the same tenant. "
                    + NEW_LINE + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration/routingKey/{routingKey}", method = RequestMethod.GET)
    @ResponseBody
    public Integration getIntegrationByRoutingKey(
            @ApiParam(required = true, value = "A string value representing the integration routing key. For example, '542047e6-c1b2-112e-a87e-e49247c09d4b'")
            @PathVariable("routingKey") String routingKey) throws ThingsboardException {
        try {
            Integration integration = checkNotNull(integrationService.findIntegrationByRoutingKey(getTenantId(), routingKey));
            accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ, integration.getId(), integration);
            return integration;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Integration (saveIntegration)",
            notes = "Create or update the Integration. When creating integration, platform generates Integration Id as " + UUID_WIKI_LINK +
                    "The newly created integration id will be present in the response. " +
                    "Specify existing Integration id to update the integration. " +
                    "Referencing non-existing integration Id will cause 'Not Found' error. " +
                    "Integration configuration is validated for each type of the integration before it can be created. " +
                    INTEGRATION_CONFIGURATION_DEFINITION
                    + TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration", method = RequestMethod.POST)
    @ResponseBody
    public Integration saveIntegration(@ApiParam(required = true, value = "A JSON value representing the integration.")
                                       @RequestBody Integration integration) throws ThingsboardException {
        try {
            integration.setTenantId(getCurrentUser().getTenantId());
            boolean created = integration.getId() == null;

            checkEntity(integration.getId(), integration, Resource.INTEGRATION, null);

            try {
                integrationManagerService.validateIntegrationConfiguration(integration).get(20, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throwRealCause(e);
            }

            Integration result = checkNotNull(integrationService.saveIntegration(integration));
            tbClusterService.broadcastEntityStateChangeEvent(result.getTenantId(), result.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            logEntityAction(result.getId(), result, null, created ? ActionType.ADDED : ActionType.UPDATED, null);
            return result;
        } catch (TimeoutException e) {
            throw handleException(new ThingsboardRuntimeException("Timeout to validate the configuration!", ThingsboardErrorCode.GENERAL));
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.INTEGRATION), integration,
                    null, integration.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Integrations (getIntegrations)",
            notes = "Returns a page of integrations owned by tenant. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integrations", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Integration> getIntegrations(
            @ApiParam(required = true, value = PAGE_SIZE_DESCRIPTION, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(required = true, value = PAGE_NUMBER_DESCRIPTION, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = INTEGRATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = INTEGRATION_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(integrationService.findTenantIntegrations(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check integration connectivity (checkIntegrationConnection)",
            notes = "Checks if the connection to the integration is established. " +
                    "Throws an error if the connection is not established. Example: Failed to connect to MQTT broker at host:port.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration/check", method = RequestMethod.POST)
    @ResponseBody
    public void checkIntegrationConnection(@ApiParam(required = true, value = "A JSON value representing the integration.")
                                           @RequestBody Integration integration) throws ThingsboardException {
        try {
            checkNotNull(integration);
            integration.setTenantId(getCurrentUser().getTenantId());
            try {
                integrationManagerService.checkIntegrationConnection(integration).get(20, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throwRealCause(e);
            }
        } catch (TimeoutException e) {
            throw handleException(new ThingsboardRuntimeException("Timeout to process the request!", ThingsboardErrorCode.GENERAL));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete integration (deleteIntegration)",
            notes = "Deletes the integration and all the relations (from and to the integration). " +
                    "Referencing non-existing integration Id will cause an error. " +
                    NEW_LINE + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration/{integrationId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteIntegration(@ApiParam(required = true, value = INTEGRATION_ID_PARAM_DESCRIPTION)
                                  @PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            Integration integration = checkIntegrationId(integrationId, Operation.DELETE);
            integrationService.deleteIntegration(getTenantId(), integrationId);

            tbClusterService.broadcastEntityStateChangeEvent(integration.getTenantId(), integration.getId(), ComponentLifecycleEvent.DELETED);

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

    @ApiOperation(value = "Get Integrations By Ids (getIntegrationsByIds)",
            notes = "Requested integrations must be owned by tenant which is performing the request. " +
                    NEW_LINE + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integrations", params = {"integrationIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Integration> getIntegrationsByIds(
            @ApiParam(value = "A list of integration ids, separated by comma ','", required = true)
            @RequestParam("integrationIds") String[] strIntegrationIds) throws ThingsboardException {
        checkArrayParameter("integrationIds", strIntegrationIds);
        try {
            if (!accessControlService.hasPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ)) {
                return Collections.emptyList();
            }
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<IntegrationId> integrationIds = new ArrayList<>();
            for (String strIntegrationId : strIntegrationIds) {
                integrationIds.add(new IntegrationId(toUUID(strIntegrationId)));
            }
            List<Integration> integrations = checkNotNull(integrationService.findIntegrationsByIdsAsync(tenantId, integrationIds).get());
            return filterIntegrationsByReadPermission(integrations);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<Integration> filterIntegrationsByReadPermission(List<Integration> integrations) {
        return integrations.stream().filter(integration -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ, integration.getId(), integration);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

}
