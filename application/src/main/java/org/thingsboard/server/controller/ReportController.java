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

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.service.report.ReportService;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
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
            DashboardInfo dashboardInfo = checkDashboardInfoId(dashboardId);
            String baseUrl = constructBaseUrl(request);

            String name = dashboardInfo.getTitle();
            name += "_" + defaultDateFormat.format(new Date());

            reportService.
                    generateDashboardReport(baseUrl, dashboardId, getCurrentUser().getId(), name, reportParams,
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
