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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.thingsboard.rule.engine.api.JobManager;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class JobController extends BaseController {

    private final JobService jobService;
    private final JobManager jobManager;

    @ApiOperation(value = "Get job by id (getJobById)",
            notes = "Fetches job info by id." + NEW_LINE +
                    "Example of a RUNNING CF_REPROCESSING job response:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"id\": {\n" +
                    "    \"entityType\": \"JOB\",\n" +
                    "    \"id\": \"475e94e0-2f2d-11f0-8240-91e99922a704\"\n" +
                    "  },\n" +
                    "  \"createdTime\": 1747053196590,\n" +
                    "  \"tenantId\": {\n" +
                    "    \"entityType\": \"TENANT\",\n" +
                    "    \"id\": \"46859a00-2f2d-11f0-8240-91e99922a704\"\n" +
                    "  },\n" +
                    "  \"type\": \"CF_REPROCESSING\",\n" +
                    "  \"key\": \"474e4130-2f2d-11f0-8240-91e99922a704\",\n" +
                    "  \"description\": \"Reprocessing of calculated field 'Air densityiLscd' for device profile <a href=\\\"/profiles/deviceProfiles/9fcec7f0-31a1-11f0-933e-27998d6db02e\\\">Test Device Profile</a>\",\n" +
                    "  \"status\": \"RUNNING\",\n" +
                    "  \"configuration\": {\n" +
                    "    \"type\": \"CF_REPROCESSING\",\n" +
                    "    \"calculatedFieldId\": {\n" +
                    "      \"entityType\": \"CALCULATED_FIELD\",\n" +
                    "      \"id\": \"474e4130-2f2d-11f0-8240-91e99922a704\"\n" +
                    "    },\n" +
                    "    \"startTs\": 1747051995760,\n" +
                    "    \"endTs\": 1747052895760,\n" +
                    "    \"tasksKey\": \"c3cdbd42-799e-4d3a-9aad-9310f767aa36\",\n" +
                    "    \"toReprocess\": null\n" +
                    "  },\n" +
                    "  \"result\": {\n" +
                    "    \"jobType\": \"CF_REPROCESSING\",\n" +
                    "    \"successfulCount\": 1,\n" +
                    "    \"failedCount\": 0,\n" +
                    "    \"discardedCount\": 0,\n" +
                    "    \"totalCount\": 2,\n" +
                    "    \"results\": [],\n" +
                    "    \"generalError\": null,\n" +
                    "    \"startTs\": 1747323069445,\n" +
                    "    \"finishTs\": 1747323070585,\n" +
                    "    \"cancellationTs\": 0\n" +
                    "  }\n" +
                    "}\n" +
                    MARKDOWN_CODE_BLOCK_END + NEW_LINE +
                    "Example of a CF_REPROCESSING job with failures:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  ...,\n" +
                    "  \"status\": \"FAILED\",\n" +
                    "  ...,\n" +
                    "  \"result\": {\n" +
                    "    \"jobType\": \"CF_REPROCESSING\",\n" +
                    "    \"successfulCount\": 0,\n" +
                    "    \"failedCount\": 2,\n" +
                    "    \"discardedCount\": 0,\n" +
                    "    \"totalCount\": 2,\n" +
                    "    \"results\": [\n" +
                    "      {\n" +
                    "        \"jobType\": \"CF_REPROCESSING\",\n" +
                    "        \"key\": \"c3cdbd42-799e-4d3a-9aad-9310f767aa36\",\n" +
                    "        \"success\": false,\n" +
                    "        \"discarded\": false,\n" +
                    "        \"failure\": {\n" +
                    "          \"error\": \"Failed to fetch temperature: Failed to fetch timeseries data\",\n" +
                    "          \"entityInfo\": {\n" +
                    "            \"id\": {\n" +
                    "              \"entityType\": \"DEVICE\",\n" +
                    "              \"id\": \"9fd41f20-31a1-11f0-933e-27998d6db02e\"\n" +
                    "            },\n" +
                    "            \"name\": \"Test device 1\"\n" +
                    "          }\n" +
                    "        }\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"jobType\": \"CF_REPROCESSING\",\n" +
                    "        \"key\": \"c3cdbd42-799e-4d3a-9aad-9310f767aa36\",\n" +
                    "        \"success\": false,\n" +
                    "        \"discarded\": false,\n" +
                    "        \"failure\": {\n" +
                    "          \"error\": \"Failed to fetch temperature: Failed to fetch timeseries data\",\n" +
                    "          \"entityInfo\": {\n" +
                    "            \"id\": {\n" +
                    "              \"entityType\": \"DEVICE\",\n" +
                    "              \"id\": \"9ffc4090-31a1-11f0-933e-27998d6db02e\"\n" +
                    "            },\n" +
                    "            \"name\": \"Test device 2\"\n" +
                    "          }\n" +
                    "        }\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"generalError\": null,\n" +
                    "    \"startTs\": 1747323069445,\n" +
                    "    \"finishTs\": 1747323070585,\n" +
                    "    \"cancellationTs\": 0\n" +
                    "  }\n" +
                    "}\n" +
                    MARKDOWN_CODE_BLOCK_END + NEW_LINE +
                    "Example of a FAILED job result with general error:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  ...,\n" +
                    "  \"status\": \"FAILED\",\n" +
                    "  ...,\n" +
                    "  \"result\": {\n" +
                    "    \"jobType\": \"CF_REPROCESSING\",\n" +
                    "    \"successfulCount\": 1,\n" +
                    "    \"failedCount\": 0,\n" +
                    "    \"discardedCount\": 0,\n" +
                    "    \"totalCount\": null,\n" +
                    "    \"results\": [],\n" +
                    "    \"generalError\": \"Timeout to find devices by profile\",\n" +
                    "    \"cancellationTs\": 0\n" +
                    "  }\n" +
                    "}\n" +
                    MARKDOWN_CODE_BLOCK_END + NEW_LINE +
                    "Example of a CANCELLED job result:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  ...,\n" +
                    "  \"status\": \"CANCELLED\",\n" +
                    "  ...,\n" +
                    "  \"result\": {\n" +
                    "    \"jobType\": \"CF_REPROCESSING\",\n" +
                    "    \"successfulCount\": 15,\n" +
                    "    \"failedCount\": 0,\n" +
                    "    \"discardedCount\": 85,\n" +
                    "    \"totalCount\": 100,\n" +
                    "    \"results\": [],\n" +
                    "    \"generalError\": null,\n" +
                    "    \"cancellationTs\": 1747065908414\n" +
                    "  }\n" +
                    "}\n" +
                    MARKDOWN_CODE_BLOCK_END +
                    TENANT_AUTHORITY_PARAGRAPH
    )
    @GetMapping("/job/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public Job getJobById(@PathVariable UUID id) throws ThingsboardException {
        // todo check permissions
        return jobService.findJobById(getTenantId(), new JobId(id));
    }

    @ApiOperation(value = "Get jobs (getJobs)",
            notes = "Returns the page of jobs." + NEW_LINE +
                    PAGE_DATA_PARAMETERS +
                    TENANT_AUTHORITY_PARAGRAPH)
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
                                 @Parameter(description = "Comma-separated list of job types to include. If empty - all job types are included.", array = @ArraySchema(schema = @Schema(type = "string")))
                                 @RequestParam(required = false) List<JobType> types,
                                 @Parameter(description = "Comma-separated list of job statuses to include. If empty - all job statuses are included.", array = @ArraySchema(schema = @Schema(type = "string")))
                                 @RequestParam(required = false) List<JobStatus> statuses,
                                 @RequestParam(required = false) List<UUID> entities,
                                 @RequestParam(required = false) Long startTime,
                                 @RequestParam(required = false) Long endTime) throws ThingsboardException {
        // todo check permissions
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        JobFilter filter = JobFilter.builder()
                .types(types)
                .statuses(statuses)
                .entities(entities)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return jobService.findJobsByFilter(getTenantId(), filter, pageLink);
    }

    @ApiOperation(value = "Cancel job (cancelJob)",
            notes = "Cancels the job. The status of the job must be QUEUED, PENDING or RUNNING." + NEW_LINE +
                    "For a running job, all the tasks not yet processed will be discarded." + NEW_LINE +
                    "See the example of a cancelled job result in getJobById method description." +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/job/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public void cancelJob(@PathVariable UUID id) throws ThingsboardException {
        // todo check permissions
        jobManager.cancelJob(getTenantId(), new JobId(id));
    }

    @ApiOperation(value = "Reprocess job (reprocessJob)",
            notes = "Reprocesses the job. Failures are located at job.result.results list. " +
                    "Platform iterates over this list and submits new tasks for them. " +
                    "Doesn't create new job entity but updates the existing one. " +
                    "Successfully reprocessed job will look the same as completed one." +
                    TENANT_AUTHORITY_PARAGRAPH)
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
