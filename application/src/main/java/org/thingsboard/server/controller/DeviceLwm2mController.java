/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.ServerSecurityConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class DeviceLwm2mController extends BaseController {


    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/deviceProfile/{objectIds}", method = RequestMethod.GET)
    @ResponseBody
    public List<LwM2mObject> getLwm2mListObjects(@PathVariable("objectIds") int[] objectIds) throws ThingsboardException {
        try {
            return lwM2MModelsRepository.getLwm2mObjects(objectIds, null);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/deviceProfile/objects", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<LwM2mObject> getLwm2mListObjects(@RequestParam int pageSize,
                                                     @RequestParam int page,
                                                     @RequestParam(required = false) String textSearch,
                                                     @RequestParam(required = false) String sortProperty,
                                                     @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(lwM2MModelsRepository.findDeviceLwm2mObjects(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/deviceProfile/bootstrap/{securityMode}/{bootstrapServerIs}", method = RequestMethod.GET)
    @ResponseBody
    public ServerSecurityConfig getLwm2mBootstrapSecurityInfo(@PathVariable("securityMode") String securityMode,
                                                              @PathVariable("bootstrapServerIs") boolean bootstrapServerIs) throws ThingsboardException {
        try {
            return lwM2MModelsRepository.getBootstrapSecurityInfo(securityMode, bootstrapServerIs);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
