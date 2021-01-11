/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.rule.engine.api.ReportService;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.utils.MiscUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class ReportController extends BaseController {

    private SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

    @Autowired
    private ReportService reportService;

    public static final String DASHBOARD_ID = "dashboardId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/report/{dashboardId}/download", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> downloadDashboardReport(@PathVariable(DASHBOARD_ID) String strDashboardId,
                                                                  @RequestBody JsonNode reportParams,
                                                                  HttpServletRequest request) throws ThingsboardException {
        DeferredResult<ResponseEntity> result = new DeferredResult<>();
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            DashboardInfo dashboardInfo = checkDashboardInfoId(dashboardId, Operation.READ);
            String baseUrl = MiscUtils.constructBaseUrl(request);

            String name = dashboardInfo.getTitle();
            name += "-" + defaultDateFormat.format(new Date());

            SecurityUser currentUser = getCurrentUser();
            String publicId = "";
            if (currentUser.getUserPrincipal().getType() == UserPrincipal.Type.PUBLIC_ID) {
                publicId = currentUser.getUserPrincipal().getValue();
            }
            reportService.
                    generateDashboardReport(baseUrl, dashboardId, getTenantId(), currentUser.getId(), publicId, name, reportParams,
                            reportData -> {
                                ByteArrayResource resource = new ByteArrayResource(reportData.getData());
                                ResponseEntity<Resource> response = ResponseEntity.ok().
                                        header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + reportData.getName())
                                        .header("x-filename", reportData.getName())
                                        .contentLength(resource.contentLength())
                                        .contentType(parseMediaType(reportData.getContentType()))
                                        .body(resource);
                                result.setResult(response);
                            },
                            throwable -> {
                                result.setErrorResult(throwable);
                            });
        } catch (Exception e) {
            result.setErrorResult(handleException(e));
        }
        return result;
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/report/test", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> downloadTestReport(@RequestBody ReportConfig reportConfig,
                                                             @RequestParam(required = false) String reportsServerEndpointUrl) {
        DeferredResult<ResponseEntity> result = new DeferredResult<>();
        try {
            String strDashboardId = reportConfig.getDashboardId();
            checkParameter(DASHBOARD_ID, strDashboardId);

            DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            checkDashboardInfoId(dashboardId, Operation.READ);

            reportService.
                    generateReport(
                            getTenantId(),
                            reportConfig,
                            reportsServerEndpointUrl,
                            reportData -> {
                                ByteArrayResource resource = new ByteArrayResource(reportData.getData());
                                ResponseEntity<Resource> response = ResponseEntity.ok().
                                        header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + reportData.getName())
                                        .header("x-filename", reportData.getName())
                                        .contentLength(resource.contentLength())
                                        .contentType(parseMediaType(reportData.getContentType()))
                                        .body(resource);
                                result.setResult(response);
                            },
                            throwable -> {
                                result.setErrorResult(throwable);
                            });
        } catch (Exception e) {
            result.setErrorResult(handleException(e));
        }
        return result;
    }

    private static MediaType parseMediaType(String contentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return mediaType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

}
