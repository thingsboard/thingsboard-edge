/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.job.JobManager;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class JobController extends BaseController {

    private final JobService jobService;
    private final JobManager jobManager;

    @GetMapping("/job/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public Job getJobById(@PathVariable UUID id) throws ThingsboardException {
        // todo check permissions
        return jobService.findJobById(getTenantId(), new JobId(id));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public PageData<Job> getJobs(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                 @RequestParam int pageSize,
                                 @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                 @RequestParam int page,
                                 @Parameter(description = "Case-insensitive 'substring' filter based on job's description")
                                 @RequestParam(required = false) String textSearch,
                                 @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                 @RequestParam(required = false) String sortProperty,
                                 @Parameter(description = SORT_ORDER_DESCRIPTION)
                                 @RequestParam(required = false) String sortOrder,
                                 @RequestParam(required = false) List<JobType> types,
                                 @RequestParam(required = false) List<JobStatus> statuses) throws ThingsboardException {
        // todo check permissions
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        JobFilter filter = JobFilter.builder()
                .types(types)
                .statuses(statuses)
                .build();
        return jobService.findJobsByFilter(getTenantId(), filter, pageLink);
    }

    @PostMapping("/job/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public void cancelJob(@PathVariable UUID id) throws ThingsboardException {
        // todo check permissions
        jobManager.cancelJob(getTenantId(), new JobId(id));
    }

    @PostMapping("/job/{id}/reprocess")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public void reprocessJob(@PathVariable UUID id) throws ThingsboardException {
        // todo check permissions
        jobManager.reprocessJob(getTenantId(), new JobId(id));
    }

    @DeleteMapping("/job/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public void deleteJob(@PathVariable UUID id) throws ThingsboardException {
        // todo check permissions
        jobService.deleteJob(getTenantId(), new JobId(id));
    }

}
