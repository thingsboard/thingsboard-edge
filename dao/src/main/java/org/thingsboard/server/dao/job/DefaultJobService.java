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
package org.thingsboard.server.dao.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStats;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import java.util.Optional;

import static org.thingsboard.server.common.data.job.JobStatus.CANCELLED;
import static org.thingsboard.server.common.data.job.JobStatus.COMPLETED;
import static org.thingsboard.server.common.data.job.JobStatus.FAILED;
import static org.thingsboard.server.common.data.job.JobStatus.PENDING;
import static org.thingsboard.server.common.data.job.JobStatus.QUEUED;
import static org.thingsboard.server.common.data.job.JobStatus.RUNNING;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultJobService extends AbstractEntityService implements JobService {

    private final JobDao jobDao;

    @Transactional
    @Override
    public Job submitJob(TenantId tenantId, Job job) {
        if (jobDao.existsByTenantAndKeyAndStatusOneOf(tenantId, job.getKey(), QUEUED, PENDING, RUNNING)) {
            throw new IllegalArgumentException("The same job is already queued or running");
        }
        if (jobDao.existsByTenantIdAndTypeAndStatusOneOf(tenantId, job.getType(), PENDING, RUNNING)) {
            job.setStatus(QUEUED);
        } else {
            job.setStatus(PENDING);
        }
        return saveJob(tenantId, job, true, null);
    }

    @Override
    public Job findJobById(TenantId tenantId, JobId jobId) {
        return jobDao.findById(tenantId, jobId.getId());
    }

    @Transactional
    @Override
    public void cancelJob(TenantId tenantId, JobId jobId) {
        Job job = findForUpdate(tenantId, jobId);
        if (!job.getStatus().isOneOf(QUEUED, PENDING, RUNNING)) {
            throw new IllegalArgumentException("Job already " + job.getStatus().name().toLowerCase());
        }
        job.getResult().setCancellationTs(System.currentTimeMillis());
        JobStatus prevStatus = job.getStatus();
        if (job.getStatus() == QUEUED) {
            job.setStatus(CANCELLED); // setting cancelled status right away, because we don't expect stats for cancelled tasks
        } else if (job.getStatus() == PENDING) {
            job.setStatus(RUNNING);
        }
        saveJob(tenantId, job, true, prevStatus);
    }

    @Transactional
    @Override
    public void markAsFailed(TenantId tenantId, JobId jobId, String error) {
        Job job = findForUpdate(tenantId, jobId);
        job.getResult().setGeneralError(error);
        JobStatus prevStatus = job.getStatus();
        job.setStatus(FAILED);
        saveJob(tenantId, job, true, prevStatus);
    }

    @Transactional
    @Override
    public void processStats(TenantId tenantId, JobId jobId, JobStats jobStats) {
        Job job = findForUpdate(tenantId, jobId);
        if (job == null) {
            log.debug("[{}][{}] Got stale stats: {}", tenantId, jobId, jobStats);
            return;
        }
        JobStatus prevStatus = job.getStatus();
        if (job.getStatus() == PENDING) {
            job.setStatus(RUNNING);
        }

        JobResult result = job.getResult();
        if (jobStats.getTotalTasksCount() != null) {
            result.setTotalCount(jobStats.getTotalTasksCount());
        }

        boolean publishEvent = false;
        for (TaskResult taskResult : jobStats.getTaskResults()) {
           result.processTaskResult(taskResult);

            if (result.getCancellationTs() > 0) {
                if (!taskResult.isDiscarded() && System.currentTimeMillis() > result.getCancellationTs()) {
                    log.info("Got task result for cancelled job {}: {}, re-notifying processors about cancellation", jobId, taskResult);
                    // task processor forgot the task is cancelled
                    publishEvent = true;
                }
            }
        }

        if (job.getStatus() == RUNNING) {
            if (result.getTotalCount() != null && result.getCompletedCount() >= result.getTotalCount()) {
                if (result.getCancellationTs() > 0) {
                    job.setStatus(CANCELLED);
                } else if (result.getFailedCount() > 0) {
                    job.setStatus(FAILED);
                } else {
                    job.setStatus(COMPLETED);
                }
            }
        }

        saveJob(tenantId, job, publishEvent, prevStatus);
    }

    private Job saveJob(TenantId tenantId, Job job, boolean publishEvent, JobStatus prevStatus) {
        job = jobDao.save(tenantId, job);
        if (publishEvent) {
            eventPublisher.publishEvent(SaveEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(job.getId())
                    .entity(job)
                    .build());
        }
        log.info("[{}] Saved job: {}", tenantId, job);
        if (prevStatus != null && job.getStatus() != prevStatus) {
            log.info("[{}][{}][{}] New job status: {} -> {}", tenantId, job.getId(), job.getType(), prevStatus, job.getStatus());
            if (job.getStatus().isOneOf(CANCELLED, COMPLETED, FAILED) && prevStatus != QUEUED) { // if prev status is QUEUED - means there are already running jobs with this type, no need to check for waiting job
                checkWaitingJobs(tenantId, job.getType());
            }
        }
        return job;
    }

    private void checkWaitingJobs(TenantId tenantId, JobType jobType) {
        Job queuedJob = jobDao.findOldestByTenantIdAndTypeAndStatusForUpdate(tenantId, jobType, QUEUED);
        if (queuedJob == null) {
            return;
        }
        queuedJob.setStatus(PENDING);
        saveJob(tenantId, queuedJob, true, QUEUED);
    }

    @Override
    public PageData<Job> findJobsByTenantId(TenantId tenantId, PageLink pageLink) {
        return jobDao.findByTenantId(tenantId, pageLink);
    }

    @Override
    public Job findLatestJobByKey(TenantId tenantId, String key) {
        return jobDao.findLatestByKey(tenantId, key);
    }

    private Job findForUpdate(TenantId tenantId, JobId jobId) {
        return jobDao.findByIdForUpdate(tenantId, jobId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findJobById(tenantId, (JobId) entityId));
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        jobDao.removeById(tenantId, id.getId());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        jobDao.deleteByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

}
