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
package org.thingsboard.server.dao.sql.job;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.job.JobDao;
import org.thingsboard.server.dao.model.sql.JobEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaJobDao extends JpaAbstractDao<JobEntity, Job> implements JobDao {

    private final JobRepository jobRepository;
    private final EntityService entityService;

    @Override
    public PageData<Job> findByTenantIdAndFilter(TenantId tenantId, JobFilter filter, PageLink pageLink) {
        PageData<Job> jobs = DaoUtil.toPageData(jobRepository.findByTenantIdAndTypesAndStatusesAndEntitiesAndTimeAndSearchText(tenantId.getId(),
                CollectionsUtil.isEmpty(filter.getTypes()) ? null : filter.getTypes(),
                CollectionsUtil.isEmpty(filter.getStatuses()) ? null : filter.getStatuses(),
                CollectionsUtil.isEmpty(filter.getEntities()) ? null : filter.getEntities(),
                filter.getStartTime() != null ? filter.getStartTime() : 0,
                filter.getEndTime() != null ? filter.getEndTime() : 0,
                Strings.emptyToNull(pageLink.getTextSearch()), DaoUtil.toPageable(pageLink)));

        Set<EntityId> entityIds = jobs.getData().stream()
                .map(Job::getEntityId)
                .collect(Collectors.toSet());
        Map<EntityId, EntityInfo> entityInfos = entityService.fetchEntityInfos(tenantId, null, entityIds, MergedUserPermissions.ALL);
        jobs.getData().forEach(job -> {
            EntityInfo entityInfo = entityInfos.get(job.getEntityId());
            if (entityInfo != null) {
                job.setEntityName(entityInfo.getName());
            }
        });
        return jobs;
    }

    @Override
    public Job findByIdForUpdate(TenantId tenantId, JobId jobId) {
        return DaoUtil.getData(jobRepository.findByIdForUpdate(jobId.getId()));
    }

    @Override
    public Job findLatestByTenantIdAndKey(TenantId tenantId, String key) {
        return DaoUtil.getData(jobRepository.findLatestByTenantIdAndKey(tenantId.getId(), key, Limit.of(1)));
    }

    @Override
    public boolean existsByTenantAndKeyAndStatusOneOf(TenantId tenantId, String key, JobStatus... statuses) {
        return jobRepository.existsByTenantIdAndKeyAndStatusIn(tenantId.getId(), key, Arrays.stream(statuses).toList());
    }

    @Override
    public boolean existsByTenantIdAndTypeAndStatusOneOf(TenantId tenantId, JobType type, JobStatus... statuses) {
        return jobRepository.existsByTenantIdAndTypeAndStatusIn(tenantId.getId(), type, Arrays.stream(statuses).toList());
    }

    @Override
    public boolean existsByTenantIdAndEntityIdAndStatusOneOf(TenantId tenantId, EntityId entityId, JobStatus... statuses) {
        return jobRepository.existsByTenantIdAndEntityIdAndStatusIn(tenantId.getId(), entityId.getId(), Arrays.stream(statuses).toList());
    }

    @Override
    public Job findOldestByTenantIdAndTypeAndStatusForUpdate(TenantId tenantId, JobType type, JobStatus status) {
        return DaoUtil.getData(jobRepository.findOldestByTenantIdAndTypeAndStatusForUpdate(tenantId.getId(), type.name(), status.name()));
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        jobRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public int removeByEntityId(TenantId tenantId, EntityId entityId) {
        return jobRepository.deleteByEntityId(entityId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

    @Override
    protected Class<JobEntity> getEntityClass() {
        return JobEntity.class;
    }

    @Override
    protected JpaRepository<JobEntity, UUID> getRepository() {
        return jobRepository;
    }

}
