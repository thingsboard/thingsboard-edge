/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EdgeController extends BaseController {

    public static final String EDGE_ID = "edgeId";

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    public Edge getEdgeById(@PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            return checkEdgeId(edgeId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge", method = RequestMethod.POST)
    @ResponseBody
    public Edge saveEdge(@RequestBody Edge edge,
                         @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            edge.setTenantId(tenantId);
            boolean created = edge.getId() == null;

            RuleChain defaultRootEdgeRuleChain = null;
            if (created) {
                defaultRootEdgeRuleChain = ruleChainService.getDefaultRootEdgeRuleChain(tenantId);
                if (defaultRootEdgeRuleChain == null) {
                    throw new DataValidationException("Root edge rule chain is not available!");
                }
            }

            EntityGroupId entityGroupId = null;
            if (!StringUtils.isEmpty(strEntityGroupId)) {
                entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            }

            Operation operation = created ? Operation.CREATE : Operation.WRITE;

            accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, operation,
                    edge.getId(), edge, entityGroupId);

            Edge result = checkNotNull(edgeService.saveEdge(edge));

            if (entityGroupId != null && operation == Operation.CREATE) {
                entityGroupService.addEntityToEntityGroup(getTenantId(), entityGroupId, result.getId());
            }

            if (created) {
//                ruleChainService.assignRuleChainToEdgeGroup(tenantId, defaultRootEdgeRuleChain.getId(), result.getId());
//                edgeService.setRootRuleChain(tenantId, result, defaultRootEdgeRuleChain.getId());
            }

            logEntityAction(result.getId(), result, null, created ? ActionType.ADDED : ActionType.UPDATED, null);
            return result;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.EDGE), edge,
                    null, edge.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEdge(@PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.DELETE);
            edgeService.deleteEdge(getTenantId(), edgeId);

            logEntityAction(edgeId, edge,
                    null,
                    ActionType.DELETED, null, strEdgeId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.EDGE),
                    null,
                    null,
                    ActionType.DELETED, e, strEdgeId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edges", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Edge> getEdges(@RequestParam int limit,
                                       @RequestParam(required = false) String textSearch,
                                       @RequestParam(required = false) String idOffset,
                                       @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    @RequestMapping(value = "/customer/{customerId}/edge/{edgeId}", method = RequestMethod.POST)
//    @ResponseBody
//    public Edge assignEdgeToCustomer(@PathVariable("customerId") String strCustomerId,
//                                       @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
//        checkParameter("customerId", strCustomerId);
//        checkParameter(EDGE_ID, strEdgeId);
//        try {
//            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
//            Customer customer = checkCustomerId(customerId, Operation.READ);
//
//            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
//            checkEdgeId(edgeId, Operation.ASSIGN_TO_CUSTOMER);
//
//            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(getCurrentUser().getTenantId(), edgeId, customerId));
//
//            logEntityAction(edgeId, savedEdge,
//                    savedEdge.getCustomerId(),
//                    ActionType.ASSIGNED_TO_CUSTOMER, null, strEdgeId, strCustomerId, customer.getName());
//
//            return savedEdge;
//        } catch (Exception e) {
//            logEntityAction(emptyId(EntityType.EDGE), null,
//                    null,
//                    ActionType.ASSIGNED_TO_CUSTOMER, e, strEdgeId, strCustomerId);
//            throw handleException(e);
//        }
//    }

//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    @RequestMapping(value = "/customer/edge/{edgeId}", method = RequestMethod.DELETE)
//    @ResponseBody
//    public Edge unassignEdgeFromCustomer(@PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
//        checkParameter(EDGE_ID, strEdgeId);
//        try {
//            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
//            Edge edge = checkEdgeId(edgeId, Operation.UNASSIGN_FROM_CUSTOMER);
//            if (edge.getCustomerId() == null || edge.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
//                throw new IncorrectParameterException("Edge isn't assigned to any customer!");
//            }
//            Customer customer = checkCustomerId(edge.getCustomerId(), Operation.READ);
//
//            Edge savedEdge = checkNotNull(edgeService.unassignEdgeFromCustomer(getCurrentUser().getTenantId(), edgeId));
//
//            logEntityAction(edgeId, edge,
//                    edge.getCustomerId(),
//                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strEdgeId, customer.getId().toString(), customer.getName());
//
//            return savedEdge;
//        } catch (Exception e) {
//            logEntityAction(emptyId(EntityType.EDGE), null,
//                    null,
//                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strEdgeId);
//            throw handleException(e);
//        }
//    }

//    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
//    @RequestMapping(value = "/customer/public/edge/{edgeId}", method = RequestMethod.POST)
//    @ResponseBody
//    public Edge assignEdgeToPublicCustomer(@PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
//        checkParameter(EDGE_ID, strEdgeId);
//        try {
//            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
//            Edge edge = checkEdgeId(edgeId, Operation.ASSIGN_TO_CUSTOMER);
//            Customer publicCustomer = customerService.findOrCreatePublicCustomer(edge.getTenantId());
//            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(getCurrentUser().getTenantId(), edgeId, publicCustomer.getId()));
//
//            logEntityAction(edgeId, savedEdge,
//                    savedEdge.getCustomerId(),
//                    ActionType.ASSIGNED_TO_CUSTOMER, null, strEdgeId, publicCustomer.getId().toString(), publicCustomer.getName());
//
//            return savedEdge;
//        } catch (Exception e) {
//            logEntityAction(emptyId(EntityType.EDGE), null,
//                    null,
//                    ActionType.ASSIGNED_TO_CUSTOMER, e, strEdgeId);
//            throw handleException(e);
//        }
//    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edges", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Edge> getTenantEdges(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(edgeService.findEdgesByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edges", params = {"edgeName"}, method = RequestMethod.GET)
    @ResponseBody
    public Edge getTenantEdge(
            @RequestParam String edgeName) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(edgeService.findEdgeByTenantIdAndName(tenantId, edgeName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edgeGroup/{edgeGroupId}/{ruleChainId}/root", method = RequestMethod.POST)
    @ResponseBody
    public EntityGroup setRootRuleChain(@PathVariable("edgeGroupId") String strEdgeGroupId,
                                 @PathVariable("ruleChainId") String strRuleChainId) throws ThingsboardException {
        checkParameter("edgeGroupId", strEdgeGroupId);
        checkParameter("ruleChainId", strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            checkRuleChain(ruleChainId, Operation.WRITE);

            EntityGroupId edgeGroupId = new EntityGroupId(toUUID(strEdgeGroupId));
            EntityGroup edgeGroup = checkEntityGroupId(edgeGroupId, Operation.WRITE);

            EntityGroup updatedEdgeGroup = edgeService.setRootRuleChain(getTenantId(), edgeGroup, ruleChainId);

            logEntityAction(updatedEdgeGroup.getId(), updatedEdgeGroup, null, ActionType.UPDATED, null);

            return updatedEdgeGroup;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.EDGE),
                    null,
                    null,
                    ActionType.UPDATED, e, strEdgeGroupId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/edges", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Edge> getCustomerEdges(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(edgeService.findEdgesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/edges", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Edge> getUserEdges(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            List<Predicate<Edge>> filters = new ArrayList<>();
            if (type != null && type.trim().length() > 0) {
                filters.add((entityView -> entityView.getType().equals(type)));
            }
            return getGroupEntitiesByPageLink(getCurrentUser(), EntityType.ENTITY_VIEW, Operation.READ, entityId -> new EdgeId(entityId.getId()),
                    (entityIds) -> {
                        try {
                            return edgeService.findEdgesByTenantIdAndIdsAsync(getTenantId(), entityIds).get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    filters,
                    pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges", params = {"edgeIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Edge> getEdgesByIds(
            @RequestParam("edgeIds") String[] strEdgeIds) throws ThingsboardException {
        checkArrayParameter("edgeIds", strEdgeIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();
            List<EdgeId> edgeIds = new ArrayList<>();
            for (String strEdgeId : strEdgeIds) {
                edgeIds.add(new EdgeId(toUUID(strEdgeId)));
            }
            ListenableFuture<List<Edge>> edges;
            if (customerId == null || customerId.isNullUid()) {
                edges = edgeService.findEdgesByTenantIdAndIdsAsync(tenantId, edgeIds);
            } else {
                edges = edgeService.findEdgesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, edgeIds);
            }
            return checkNotNull(edges.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges", method = RequestMethod.POST)
    @ResponseBody
    public List<Edge> findByQuery(@RequestBody EdgeSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEdgeTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<Edge> edges = checkNotNull(edgeService.findEdgesByQuery(getCurrentUser().getTenantId(), query).get());
            edges = edges.stream().filter(edge -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, Operation.READ, edge.getId(), edge);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return edges;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getEdgeTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> edgeTypes = edgeService.findEdgeTypesByTenantId(tenantId);
            return checkNotNull(edgeTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
