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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.solutions.SolutionService;
import org.thingsboard.server.service.solutions.data.solution.SolutionInstallResponse;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateDetails;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInfo;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.ASSET_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SOLUTION_TEMPLATE_ID_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api/solutions")
public class SolutionController extends BaseController {

    @Autowired
    private SolutionService solutionService;

    @ApiOperation(value = "Get Solution templates (getSolutionTemplateInfos)",
            notes = "Get a list of solution template descriptors" + "\n\n" + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/templates/infos", method = RequestMethod.GET)
    @ResponseBody
    public List<TenantSolutionTemplateInfo> getSolutionTemplateInfos() throws ThingsboardException {
        try {
            checkAllPermissions();
            return checkNotNull(solutionService.getSolutionInfos(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Solution template details (getSolutionTemplateDetails)",
            notes = "Get a solution template details based on the provided id" + "\n\n" + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/templates/details/{solutionTemplateId}", method = RequestMethod.GET)
    @ResponseBody
    public TenantSolutionTemplateDetails getSolutionTemplateDetails(
            @ApiParam(value = SOLUTION_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("solutionTemplateId") String solutionTemplateId) throws ThingsboardException {
        checkParameter("solutionTemplateId", solutionTemplateId);
        try {
            checkAllPermissions();
            return checkNotNull(solutionService.getSolutionDetails(getTenantId(), solutionTemplateId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Solution Template Instructions (getSolutionTemplateInstructions)",
            notes = "Get a solution template instructions based on the provided id" + "\n\n" + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/templates/instructions/{solutionTemplateId}", method = RequestMethod.GET)
    @ResponseBody
    public TenantSolutionTemplateInstructions getSolutionTemplateInstructions(
            @ApiParam(value = SOLUTION_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("solutionTemplateId") String solutionTemplateId) throws ThingsboardException {
        checkParameter("solutionTemplateId", solutionTemplateId);
        try {
            checkAllPermissions();
            return checkNotNull(solutionService.getSolutionInstructions(getTenantId(), solutionTemplateId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Install Solution Template (installSolutionTemplate)",
            notes = "Install solution template based on the provided id" + "\n\n" + RBAC_WRITE_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/templates/{solutionTemplateId}/install", method = RequestMethod.POST)
    @ResponseBody
    public SolutionInstallResponse installSolutionTemplate(
            @ApiParam(value = SOLUTION_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(name = "solutionTemplateId") String solutionTemplateId, HttpServletRequest request) throws ThingsboardException {
        checkParameter("solutionTemplateId", solutionTemplateId);
        try {
            checkAllPermissions();
            return checkNotNull(solutionService.installSolution(getCurrentUser(), getTenantId(), solutionTemplateId, request));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Uninstall Solution Template (uninstallSolutionTemplate)",
            notes = "Uninstall solution template based on the provided id" + "\n\n" + RBAC_DELETE_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/templates/{solutionTemplateId}/delete", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void uninstallSolutionTemplate(
            @ApiParam(value = SOLUTION_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(name = "solutionTemplateId") String solutionTemplateId) throws ThingsboardException {
        checkParameter("solutionTemplateId", solutionTemplateId);
        try {
            checkAllPermissions();
            solutionService.deleteSolution(getTenantId(), solutionTemplateId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void checkAllPermissions() throws ThingsboardException {
        if (!getCurrentUser().getUserPermissions().hasGenericPermission(Resource.ALL, Operation.ALL)) {
            throw new ThingsboardException("You don't have permissions to use solution templates!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

}
