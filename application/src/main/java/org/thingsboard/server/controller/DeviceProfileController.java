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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_DATA;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TRANSPORT_TYPE_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class DeviceProfileController extends BaseController {


    @Autowired
    private TimeseriesService timeseriesService;

    @ApiOperation(value = "Get Device Profile (getDeviceProfileById)",
            notes = "Fetch the Device Profile object based on the provided Device Profile Id. " +
                    "The server checks that the device profile is owned by the same tenant. " + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public DeviceProfile getDeviceProfileById(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            return checkDeviceProfileId(deviceProfileId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Device Profile Info (getDeviceProfileInfoById)",
            notes = "Fetch the Device Profile Info object based on the provided Device Profile Id. "
                    + DEVICE_PROFILE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfo/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public DeviceProfileInfo getDeviceProfileInfoById(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            return new DeviceProfileInfo(checkDeviceProfileId(deviceProfileId, Operation.READ));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Default Device Profile (getDefaultDeviceProfileInfo)",
            notes = "Fetch the Default Device Profile Info object. " +
                    DEVICE_PROFILE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfo/default", method = RequestMethod.GET)
    @ResponseBody
    public DeviceProfileInfo getDefaultDeviceProfileInfo() throws ThingsboardException {
        try {
            return checkNotNull(deviceProfileService.findDefaultDeviceProfileInfo(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get time-series keys (getTimeseriesKeys)",
            notes = "Get a set of unique time-series keys used by devices that belong to specified profile. " +
                    "If profile is not set returns a list of unique keys among all profiles. " +
                    "The call is used for auto-complete in the UI forms. " +
                    "The implementation limits the number of devices that participate in search to 100 as a trade of between accurate results and time-consuming queries. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/devices/keys/timeseries", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getTimeseriesKeys(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(name = DEVICE_PROFILE_ID, required = false) String deviceProfileIdStr) throws ThingsboardException {
        DeviceProfileId deviceProfileId;
        if (StringUtils.isNotEmpty(deviceProfileIdStr)) {
            deviceProfileId = new DeviceProfileId(UUID.fromString(deviceProfileIdStr));
            checkDeviceProfileId(deviceProfileId, Operation.READ);
        } else {
            deviceProfileId = null;
        }

        try {
            return timeseriesService.findAllKeysByDeviceProfileId(getTenantId(), deviceProfileId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get attribute keys (getAttributesKeys)",
            notes = "Get a set of unique attribute keys used by devices that belong to specified profile. " +
                    "If profile is not set returns a list of unique keys among all profiles. " +
                    "The call is used for auto-complete in the UI forms. " +
                    "The implementation limits the number of devices that participate in search to 100 as a trade of between accurate results and time-consuming queries. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/devices/keys/attributes", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getAttributesKeys(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(name = DEVICE_PROFILE_ID, required = false) String deviceProfileIdStr) throws ThingsboardException {
        DeviceProfileId deviceProfileId;
        if (StringUtils.isNotEmpty(deviceProfileIdStr)) {
            deviceProfileId = new DeviceProfileId(UUID.fromString(deviceProfileIdStr));
            checkDeviceProfileId(deviceProfileId, Operation.READ);
        } else {
            deviceProfileId = null;
        }

        try {
            return attributesService.findAllKeysByDeviceProfileId(getTenantId(), deviceProfileId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Device Profile (saveDeviceProfile)",
            notes = "Create or update the Device Profile. When creating device profile, platform generates device profile id as " + UUID_WIKI_LINK +
                    "The newly created device profile id will be present in the response. " +
                    "Specify existing device profile id to update the device profile. " +
                    "Referencing non-existing device profile Id will cause 'Not Found' error. " + NEW_LINE +
                    "Device profile name is unique in the scope of tenant. Only one 'default' device profile may exist in scope of tenant." + DEVICE_PROFILE_DATA +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json",
            consumes = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile", method = RequestMethod.POST)
    @ResponseBody
    public DeviceProfile saveDeviceProfile(
            @ApiParam(value = "A JSON value representing the device profile.")
            @RequestBody DeviceProfile deviceProfile) throws ThingsboardException {
        try {
            boolean created = deviceProfile.getId() == null;
            deviceProfile.setTenantId(getTenantId());

            checkEntity(deviceProfile.getId(), deviceProfile, Resource.DEVICE_PROFILE, null);

            boolean isFirmwareChanged = false;
            boolean isSoftwareChanged = false;

            if (!created) {
                DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(getTenantId(), deviceProfile.getId());
                if (!Objects.equals(deviceProfile.getFirmwareId(), oldDeviceProfile.getFirmwareId())) {
                    isFirmwareChanged = true;
                }
                if (!Objects.equals(deviceProfile.getSoftwareId(), oldDeviceProfile.getSoftwareId())) {
                    isSoftwareChanged = true;
                }
            }
            DeviceProfile savedDeviceProfile = checkNotNull(deviceProfileService.saveDeviceProfile(deviceProfile));

            tbClusterService.onDeviceProfileChange(savedDeviceProfile, null);
            tbClusterService.broadcastEntityStateChangeEvent(deviceProfile.getTenantId(), savedDeviceProfile.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            logEntityAction(savedDeviceProfile.getId(), savedDeviceProfile,
                    null,
                    created ? ActionType.ADDED : ActionType.UPDATED, null);

            otaPackageStateService.update(savedDeviceProfile, isFirmwareChanged, isSoftwareChanged);

            sendEntityNotificationMsg(getTenantId(), savedDeviceProfile.getId(),
                    deviceProfile.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);
            return savedDeviceProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE_PROFILE), deviceProfile,
                    null, deviceProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete device profile (deleteDeviceProfile)",
            notes = "Deletes the device profile. Referencing non-existing device profile Id will cause an error. " +
                    "Can't delete the device profile if it is referenced by existing devices." + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDeviceProfile(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.DELETE);
            deviceProfileService.deleteDeviceProfile(getTenantId(), deviceProfileId);

            tbClusterService.onDeviceProfileDelete(deviceProfile, null);
            tbClusterService.broadcastEntityStateChangeEvent(deviceProfile.getTenantId(), deviceProfile.getId(), ComponentLifecycleEvent.DELETED);

            logEntityAction(deviceProfileId, deviceProfile,
                    null,
                    ActionType.DELETED, null, strDeviceProfileId);

            sendEntityNotificationMsg(getTenantId(), deviceProfile.getId(), EdgeEventActionType.DELETED);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE_PROFILE),
                    null,
                    null,
                    ActionType.DELETED, e, strDeviceProfileId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Make Device Profile Default (setDefaultDeviceProfile)",
            notes = "Marks device profile as default within a tenant scope." + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    public DeviceProfile setDefaultDeviceProfile(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.WRITE);
            DeviceProfile previousDefaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(getTenantId());
            if (deviceProfileService.setDefaultDeviceProfile(getTenantId(), deviceProfileId)) {
                if (previousDefaultDeviceProfile != null) {
                    previousDefaultDeviceProfile = deviceProfileService.findDeviceProfileById(getTenantId(), previousDefaultDeviceProfile.getId());

                    logEntityAction(previousDefaultDeviceProfile.getId(), previousDefaultDeviceProfile,
                            null, ActionType.UPDATED, null);
                }
                deviceProfile = deviceProfileService.findDeviceProfileById(getTenantId(), deviceProfileId);

                logEntityAction(deviceProfile.getId(), deviceProfile,
                        null, ActionType.UPDATED, null);
            }
            return deviceProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE_PROFILE),
                    null,
                    null,
                    ActionType.UPDATED, e, strDeviceProfileId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Device Profiles (getDeviceProfiles)",
            notes = "Returns a page of devices profile objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceProfile> getDeviceProfiles(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE_PROFILE, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(deviceProfileService.findDeviceProfiles(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Device Profiles for transport type (getDeviceProfileInfos)",
            notes = "Returns a page of devices profile info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + DEVICE_PROFILE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceProfileInfo> getDeviceProfileInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "Type of the transport", allowableValues = TRANSPORT_TYPE_ALLOWABLE_VALUES)
            @RequestParam(required = false) String transportType) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE_PROFILE, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(deviceProfileService.findDeviceProfileInfos(getTenantId(), pageLink, transportType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
