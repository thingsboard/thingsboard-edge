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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class ComponentDescriptorController extends BaseController {

    private static final String COMPONENT_DESCRIPTOR_DEFINITION = "Each Component Descriptor represents configuration of specific rule node (e.g. 'Save Timeseries' or 'Send Email'.). " +
            "The Component Descriptors are used by the rule chain Web UI to build the configuration forms for the rule nodes. " +
            "The Component Descriptors are discovered at runtime by scanning the class path and searching for @RuleNode annotation. " +
            "Once discovered, the up to date list of descriptors is persisted to the database.";

    @ApiOperation(value = "Get Component Descriptor (getComponentDescriptorByClazz)",
            notes = "Gets the Component Descriptor object using class name from the path parameters. " +
                    COMPONENT_DESCRIPTOR_DEFINITION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/component/{componentDescriptorClazz:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ComponentDescriptor getComponentDescriptorByClazz(
            @ApiParam(value = "Component Descriptor class name", required = true)
            @PathVariable("componentDescriptorClazz") String strComponentDescriptorClazz) throws ThingsboardException {
        checkParameter("strComponentDescriptorClazz", strComponentDescriptorClazz);
        try {
            return checkComponentDescriptorByClazz(strComponentDescriptorClazz);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Component Descriptors (getComponentDescriptorsByType)",
            notes = "Gets the Component Descriptors using rule node type and optional rule chain type request parameters. " +
                    COMPONENT_DESCRIPTOR_DEFINITION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/components/{componentType}", method = RequestMethod.GET)
    @ResponseBody
    public List<ComponentDescriptor> getComponentDescriptorsByType(
            @ApiParam(value = "Type of the Rule Node", allowableValues = "ENRICHMENT,FILTER,TRANSFORMATION,ACTION,EXTERNAL", required = true)
            @PathVariable("componentType") String strComponentType,
            @ApiParam(value = "Type of the Rule Chain", allowableValues = "CORE,EDGE")
            @RequestParam(value = "ruleChainType", required = false) String strRuleChainType) throws ThingsboardException {
        checkParameter("componentType", strComponentType);
        try {
            return checkComponentDescriptorsByType(ComponentType.valueOf(strComponentType), getRuleChainType(strRuleChainType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Component Descriptors (getComponentDescriptorsByTypes)",
            notes = "Gets the Component Descriptors using coma separated list of rule node types and optional rule chain type request parameters. " +
                    COMPONENT_DESCRIPTOR_DEFINITION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/components", params = {"componentTypes"}, method = RequestMethod.GET)
    @ResponseBody
    public List<ComponentDescriptor> getComponentDescriptorsByTypes(
            @ApiParam(value = "List of types of the Rule Nodes, (ENRICHMENT, FILTER, TRANSFORMATION, ACTION or EXTERNAL)", required = true)
            @RequestParam("componentTypes") String[] strComponentTypes,
            @ApiParam(value = "Type of the Rule Chain", allowableValues = "CORE,EDGE")
            @RequestParam(value = "ruleChainType", required = false) String strRuleChainType) throws ThingsboardException {
        checkArrayParameter("componentTypes", strComponentTypes);
        try {
            Set<ComponentType> componentTypes = new HashSet<>();
            for (String strComponentType : strComponentTypes) {
                componentTypes.add(ComponentType.valueOf(strComponentType));
            }
            return checkComponentDescriptorsByTypes(componentTypes, getRuleChainType(strRuleChainType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private RuleChainType getRuleChainType(String strRuleChainType) {
        RuleChainType ruleChainType;
        if (StringUtils.isEmpty(strRuleChainType)) {
            ruleChainType = RuleChainType.CORE;
        } else {
            ruleChainType = RuleChainType.valueOf(strRuleChainType);
        }
        return ruleChainType;
    }

}
