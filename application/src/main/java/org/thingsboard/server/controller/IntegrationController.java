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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.integration.TbIntegrationService;
import org.thingsboard.server.service.integration.IntegrationManagerService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_CONFIGURATION_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_DESCRIPTION;
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
@RequiredArgsConstructor
public class IntegrationController extends AutoCommitController {

    private final IntegrationManagerService integrationManagerService;
    private final TbIntegrationService tbIntegrationService;

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
                                          @PathVariable(INTEGRATION_ID) String strIntegrationId) throws Exception {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
        return checkIntegrationId(integrationId, Operation.READ);
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
            @PathVariable("routingKey") String routingKey) throws Exception {
        Integration integration = checkNotNull(integrationService.findIntegrationByRoutingKey(getTenantId(), routingKey));
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ, integration.getId(), integration);
        return integration;
    }

    @ApiOperation(value = "Create Or Update Integration (saveIntegration)",
            notes = "Create or update the Integration. When creating integration, platform generates Integration Id as " + UUID_WIKI_LINK +
                    "The newly created integration id will be present in the response. " +
                    "Specify existing Integration id to update the integration. " +
                    "Referencing non-existing integration Id will cause 'Not Found' error. " +
                    "Integration configuration is validated for each type of the integration before it can be created. " +
                    INTEGRATION_CONFIGURATION_DEFINITION +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Integration entity. " +
                    TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration", method = RequestMethod.POST)
    @ResponseBody
    public Integration saveIntegration(@ApiParam(required = true, value = "A JSON value representing the integration.")
                                       @RequestBody Integration integration) throws Exception {
        SecurityUser currentUser = getCurrentUser();
        try {
            integration.setTenantId(currentUser.getTenantId());
            boolean created = integration.getId() == null;

            checkEntity(integration.getId(), integration, Resource.INTEGRATION, null);

            if (!integration.isEdgeTemplate()) {
                try {
                    integrationManagerService.validateIntegrationConfiguration(integration).get(20, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    throwRealCause(e);
                }
            }

            Integration result = checkNotNull(integrationService.saveIntegration(integration));

            autoCommit(currentUser, result.getId());

            if (!result.isEdgeTemplate()) {
                tbClusterService.broadcastEntityStateChangeEvent(result.getTenantId(), result.getId(),
                        created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            }

            notificationEntityService.logEntityAction(getTenantId(), result.getId(), result,
                    created ? ActionType.ADDED : ActionType.UPDATED, currentUser);

            if (result.isEdgeTemplate() && !created) {
                sendEntityNotificationMsg(result.getTenantId(), result.getId(), EdgeEventActionType.UPDATED);
            }

            return result;
        } catch (TimeoutException e) {
            throw new ThingsboardRuntimeException("Timeout to validate the configuration!", ThingsboardErrorCode.GENERAL);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION), integration,
                    integration.getId() == null ? ActionType.ADDED : ActionType.UPDATED, currentUser, e);
            throw e;
        }
    }

    @ApiOperation(value = "Get Integrations (getIntegrations)",
            notes = "Returns a page of integrations owned by tenant. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integrations", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Integration> getIntegrations(
            @ApiParam(value = "Fetch edge template integrations")
            @RequestParam(value = "isEdgeTemplate", required = false, defaultValue = "false") boolean isEdgeTemplate,
            @ApiParam(required = true, value = PAGE_SIZE_DESCRIPTION, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(required = true, value = PAGE_NUMBER_DESCRIPTION, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = INTEGRATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = INTEGRATION_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (isEdgeTemplate) {
            return checkNotNull(integrationService.findTenantEdgeTemplateIntegrations(tenantId, pageLink));
        } else {
            return checkNotNull(integrationService.findTenantIntegrations(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Integration Infos (getIntegrationInfos)",
            notes = "Returns a page of integration infos owned by tenant. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integrationInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<IntegrationInfo> getIntegrationInfos(
            @ApiParam(value = "Fetch edge template integrations")
            @RequestParam(value = "isEdgeTemplate", required = false, defaultValue = "false") boolean isEdgeTemplate,
            @ApiParam(required = true, value = PAGE_SIZE_DESCRIPTION, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(required = true, value = PAGE_NUMBER_DESCRIPTION, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = INTEGRATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = INTEGRATION_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return tbIntegrationService.findTenantIntegrationInfos(tenantId, pageLink, isEdgeTemplate);
    }

    @ApiOperation(value = "Check integration connectivity (checkIntegrationConnection)",
            notes = "Checks if the connection to the integration is established. " +
                    "Throws an error if the connection is not established. Example: Failed to connect to MQTT broker at host:port.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/integration/check", method = RequestMethod.POST)
    @ResponseBody
    public void checkIntegrationConnection(@ApiParam(required = true, value = "A JSON value representing the integration.")
                                           @RequestBody Integration integration) throws Exception {
        try {
            checkNotNull(integration);
            integration.setTenantId(getCurrentUser().getTenantId());
            try {
                integrationManagerService.checkIntegrationConnection(integration).get(20, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throwRealCause(e);
            }
        } catch (TimeoutException e) {
            throw new ThingsboardRuntimeException("Timeout to process the request!", ThingsboardErrorCode.GENERAL);
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
                                  @PathVariable(INTEGRATION_ID) String strIntegrationId) throws Exception {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            Integration integration = checkIntegrationId(integrationId, Operation.DELETE);

            List<EdgeId> relatedEdgeIds = null;
            if (integration.isEdgeTemplate()) {
                relatedEdgeIds = findRelatedEdgeIds(getTenantId(), integrationId);
            }

            integrationService.deleteIntegration(getTenantId(), integrationId);

            if (!integration.isEdgeTemplate()) {
                tbClusterService.broadcastEntityStateChangeEvent(integration.getTenantId(), integration.getId(), ComponentLifecycleEvent.DELETED);
            }

            notificationEntityService.logEntityAction(getTenantId(), integrationId, integration, ActionType.DELETED,
                    getCurrentUser(), strIntegrationId);

            if (integration.isEdgeTemplate()) {
                sendDeleteNotificationMsg(integration.getTenantId(), integrationId, relatedEdgeIds);
            }

        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION), ActionType.DELETED,
                    getCurrentUser(), e, strIntegrationId);

            throw e;
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
            @RequestParam("integrationIds") String[] strIntegrationIds) throws Exception {
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
            throw e;
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

    @ApiOperation(value = "Assign integration to edge (assignIntegrationToEdge)",
            notes = "Creates assignment of an existing integration edge template to an instance of The Edge. " +
                    EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment integration " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once integration will be delivered to edge service, it's going to start locally. " +
                    "\n\nOnly integration edge template can be assigned to edge." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/integration/{integrationId}", method = RequestMethod.POST)
    @ResponseBody
    public Integration assignIntegrationToEdge(@PathVariable("edgeId") String strEdgeId,
                                               @PathVariable(INTEGRATION_ID) String strIntegrationId) throws Exception {
        checkParameter("edgeId", strEdgeId);
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.WRITE);

            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            checkIntegrationId(integrationId, Operation.READ);

            Integration savedIntegration = checkNotNull(integrationService.assignIntegrationToEdge(getCurrentUser().getTenantId(), integrationId, edgeId));

            notificationEntityService.logEntityAction(getTenantId(), integrationId, savedIntegration,
                    ActionType.ASSIGNED_TO_EDGE, getCurrentUser(), strIntegrationId, strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, savedIntegration.getId(), EdgeEventActionType.ASSIGNED_TO_EDGE);

            return savedIntegration;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION),
                    ActionType.ASSIGNED_TO_EDGE, getCurrentUser(), e, strIntegrationId, strEdgeId);

            throw e;
        }
    }

    @ApiOperation(value = "Unassign integration from edge (unassignIntegrationFromEdge)",
            notes = "Clears assignment of the integration to the edge. " +
                    EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove integration " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove integration locally." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/integration/{integrationId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Integration unassignIntegrationFromEdge(@PathVariable("edgeId") String strEdgeId,
                                                   @PathVariable(INTEGRATION_ID) String strIntegrationId) throws Exception {
        checkParameter("edgeId", strEdgeId);
        checkParameter(INTEGRATION_ID, strIntegrationId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.WRITE);
            IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
            Integration integration = checkIntegrationId(integrationId, Operation.READ);

            Integration savedIntegration = checkNotNull(integrationService.unassignIntegrationFromEdge(getCurrentUser().getTenantId(), integrationId, edgeId, false));

            notificationEntityService.logEntityAction(getTenantId(), integrationId, integration,
                    ActionType.UNASSIGNED_FROM_EDGE, getCurrentUser(), strIntegrationId, strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, savedIntegration.getId(), EdgeEventActionType.UNASSIGNED_FROM_EDGE);

            return savedIntegration;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION),
                    ActionType.UNASSIGNED_FROM_EDGE, getCurrentUser(), e, strIntegrationId, strEdgeId);

            throw e;
        }
    }

    @ApiOperation(value = "Get Edge Integrations (getEdgeIntegrations)",
            notes = "Returns a page of Integrations assigned to the specified edge. " + INTEGRATION_DESCRIPTION + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/integrations", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Integration> getEdgeIntegrations(
            @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INTEGRATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = INTEGRATION_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(integrationService.findIntegrationsByTenantIdAndEdgeId(tenantId, edgeId, pageLink));
    }

    @ApiOperation(value = "Get Edge Integrations (getEdgeIntegrationInfos)",
            notes = "Returns a page of Integrations assigned to the specified edge. " + INTEGRATION_DESCRIPTION + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/integrationInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<IntegrationInfo> getEdgeIntegrationInfos(
            @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INTEGRATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = INTEGRATION_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tbIntegrationService.findIntegrationInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink));
    }

    @ApiOperation(value = "Find edge missing attributes for assigned integrations (findEdgeMissingAttributes)",
            notes = "Returns list of edge attribute names that are missing in assigned integrations." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/integration/{edgeId}/missingAttributes", params = {"integrationIds"}, method = RequestMethod.GET)
    @ResponseBody
    public String findEdgeMissingAttributes(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                            @PathVariable(EDGE_ID) String strEdgeId,
                                            @ApiParam(value = "A list of assigned integration ids, separated by comma ','", required = true)
                                            @RequestParam("integrationIds") String[] strIntegrationIds) throws Exception {
        checkArrayParameter("integrationIds", strIntegrationIds);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        edgeId = checkNotNull(edgeId);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<IntegrationId> integrationIds = new ArrayList<>();
        for (String strIntegrationId : strIntegrationIds) {
            integrationIds.add(new IntegrationId(toUUID(strIntegrationId)));
        }
        return edgeService.findEdgeMissingAttributes(tenantId, edgeId, integrationIds);
    }

    @ApiOperation(value = "Find missing attributes for all related edges (findAllRelatedEdgesMissingAttributes)",
            notes = "Returns list of attribute names of all related edges that are missing in the integration configuration." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/integration/{integrationId}/allMissingAttributes", method = RequestMethod.GET)
    @ResponseBody
    public String findAllRelatedEdgesMissingAttributes(@ApiParam(value = INTEGRATION_ID_PARAM_DESCRIPTION, required = true)
                                                       @PathVariable("integrationId") String strIntegrationId) throws Exception {
        checkParameter("integrationId", strIntegrationId);
        IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
        integrationId = checkNotNull(integrationId);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        return edgeService.findAllRelatedEdgesMissingAttributes(tenantId, integrationId);
    }

}
