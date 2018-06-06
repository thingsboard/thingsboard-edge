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

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ComponentDescriptorController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/component/{componentDescriptorClazz:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ComponentDescriptor getComponentDescriptorByClazz(@PathVariable("componentDescriptorClazz") String strComponentDescriptorClazz) throws ThingsboardException {
        checkParameter("strComponentDescriptorClazz", strComponentDescriptorClazz);
        try {
            return checkComponentDescriptorByClazz(strComponentDescriptorClazz);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/components/{componentType}", method = RequestMethod.GET)
    @ResponseBody
    public List<ComponentDescriptor> getComponentDescriptorsByType(@PathVariable("componentType") String strComponentType) throws ThingsboardException {
        checkParameter("componentType", strComponentType);
        try {
            return checkComponentDescriptorsByType(ComponentType.valueOf(strComponentType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/components", params = {"componentTypes"}, method = RequestMethod.GET)
    @ResponseBody
    public List<ComponentDescriptor> getComponentDescriptorsByTypes(@RequestParam("componentTypes") String[] strComponentTypes) throws ThingsboardException {
        checkArrayParameter("componentTypes", strComponentTypes);
        try {
            Set<ComponentType> componentTypes = new HashSet<>();
            for (String strComponentType : strComponentTypes) {
                componentTypes.add(ComponentType.valueOf(strComponentType));
            }
            return checkComponentDescriptorsByTypes(componentTypes);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
