/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.ControllerConstants.WIDGET_TYPE_ID_PARAM_DESCRIPTION;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class WidgetTypeController extends AutoCommitController {

    private static final String WIDGET_TYPE_DESCRIPTION = "Widget Type represents the template for widget creation. Widget Type and Widget are similar to class and object in OOP theory.";
    private static final String WIDGET_TYPE_DETAILS_DESCRIPTION = "Widget Type Details extend Widget Type and add image and description properties. " +
            "Those properties are useful to edit the Widget Type but they are not required for Dashboard rendering. ";
    private static final String WIDGET_TYPE_INFO_DESCRIPTION = "Widget Type Info is a lightweight object that represents Widget Type but does not contain the heavyweight widget descriptor JSON";


    @ApiOperation(value = "Get Widget Type Details (getWidgetTypeById)",
            notes = "Get the Widget Type Details based on the provided Widget Type Id. " + WIDGET_TYPE_DETAILS_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.GET)
    @ResponseBody
    public WidgetTypeDetails getWidgetTypeById(
            @ApiParam(value = WIDGET_TYPE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetTypeId") String strWidgetTypeId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            return checkWidgetTypeId(widgetTypeId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Widget Type (saveWidgetType)",
            notes = "Create or update the Widget Type. " + WIDGET_TYPE_DESCRIPTION + " " +
                    "When creating the Widget Type, platform generates Widget Type Id as " + UUID_WIKI_LINK +
                    "The newly created Widget Type Id will be present in the response. " +
                    "Specify existing Widget Type id to update the Widget Type. " +
                    "Referencing non-existing Widget Type Id will cause 'Not Found' error." +
                    "\n\nWidget Type alias is unique in the scope of Widget Bundle. " +
                    "Special Tenant Id '13814000-1dd2-11b2-8080-808080808080' is automatically used if the create request is sent by user with 'SYS_ADMIN' authority." +
                    "Remove 'id', 'tenantId' rom the request body example (below) to create new Widget Type entity." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType", method = RequestMethod.POST)
    @ResponseBody
    public WidgetTypeDetails saveWidgetType(
            @ApiParam(value = "A JSON value representing the Widget Type Details.", required = true)
            @RequestBody WidgetTypeDetails widgetTypeDetails) throws ThingsboardException {
        try {
            var currentUser = getCurrentUser();
            if (Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
                widgetTypeDetails.setTenantId(TenantId.SYS_TENANT_ID);
            } else {
                widgetTypeDetails.setTenantId(currentUser.getTenantId());
            }

            checkEntity(widgetTypeDetails.getId(), widgetTypeDetails, Resource.WIDGET_TYPE, null);

            WidgetTypeDetails savedWidgetTypeDetails = widgetTypeService.saveWidgetType(widgetTypeDetails);

            if (!Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
                WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(widgetTypeDetails.getTenantId(), widgetTypeDetails.getBundleAlias());
                if (widgetsBundle != null) {
                    autoCommit(currentUser, widgetsBundle.getId());
                }
            }

            sendEntityNotificationMsg(getTenantId(), savedWidgetTypeDetails.getId(),
                    widgetTypeDetails.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);

            return checkNotNull(savedWidgetTypeDetails);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete widget type (deleteWidgetType)",
            notes = "Deletes the  Widget Type. Referencing non-existing Widget Type Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteWidgetType(
            @ApiParam(value = WIDGET_TYPE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetTypeId") String strWidgetTypeId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            var currentUser = getCurrentUser();
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            WidgetTypeDetails wtd = checkWidgetTypeId(widgetTypeId, Operation.DELETE);
            widgetTypeService.deleteWidgetType(currentUser.getTenantId(), widgetTypeId);

            if (wtd != null && !Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
                WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(wtd.getTenantId(), wtd.getBundleAlias());
                if (widgetsBundle != null) {
                    autoCommit(currentUser, widgetsBundle.getId());
                }
            }

            sendEntityNotificationMsg(getTenantId(), widgetTypeId, EdgeEventActionType.DELETED);

        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "Get all Widget types for specified Bundle (getBundleWidgetTypes)",
            notes = "Returns an array of Widget Type objects that belong to specified Widget Bundle." + WIDGET_TYPE_DESCRIPTION + " " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetTypes", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetType> getBundleWidgetTypes(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.WIDGET_TYPE, Operation.READ);
            TenantId tenantId;
            if (isSystem) {
                tenantId = TenantId.SYS_TENANT_ID;
            } else {
                tenantId = getCurrentUser().getTenantId();
            }
            return checkNotNull(widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(tenantId, bundleAlias));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get all Widget types details for specified Bundle (getBundleWidgetTypes)",
            notes = "Returns an array of Widget Type Details objects that belong to specified Widget Bundle." + WIDGET_TYPE_DETAILS_DESCRIPTION + " " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypesDetails", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetTypeDetails> getBundleWidgetTypesDetails(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias) throws ThingsboardException {
        try {
            TenantId tenantId;
            if (isSystem) {
                tenantId = TenantId.SYS_TENANT_ID;
            } else {
                tenantId = getCurrentUser().getTenantId();
            }
            return checkNotNull(widgetTypeService.findWidgetTypesDetailsByTenantIdAndBundleAlias(tenantId, bundleAlias));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Widget Type Info objects (getBundleWidgetTypesInfos)",
            notes = "Get the Widget Type Info objects based on the provided parameters. " + WIDGET_TYPE_INFO_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetTypesInfos", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetTypeInfo> getBundleWidgetTypesInfos(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias) throws ThingsboardException {
        try {
            TenantId tenantId;
            if (isSystem) {
                tenantId = TenantId.SYS_TENANT_ID;
            } else {
                tenantId = getCurrentUser().getTenantId();
            }
            return checkNotNull(widgetTypeService.findWidgetTypesInfosByTenantIdAndBundleAlias(tenantId, bundleAlias));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Widget Type (getWidgetType)",
            notes = "Get the Widget Type based on the provided parameters. " + WIDGET_TYPE_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetType", params = {"isSystem", "bundleAlias", "alias"}, method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetType(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias,
            @ApiParam(value = "Widget Type alias", required = true)
            @RequestParam String alias) throws ThingsboardException {
        try {
            TenantId tenantId;
            if (isSystem) {
                tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);
            } else {
                tenantId = getCurrentUser().getTenantId();
            }
            WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdBundleAliasAndAlias(tenantId, bundleAlias, alias);
            checkNotNull(widgetType);
            accessControlService.checkPermission(getCurrentUser(), Resource.WIDGET_TYPE, Operation.READ, widgetType.getId(), widgetType);
            return widgetType;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
