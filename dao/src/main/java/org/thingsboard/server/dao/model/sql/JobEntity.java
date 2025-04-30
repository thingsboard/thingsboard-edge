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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobConfiguration;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.JOB_TABLE_NAME)
public class JobEntity extends BaseSqlEntity<Job> {

    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.JOB_TYPE_PROPERTY, nullable = false)
    private JobType type;

    @Column(name = ModelConstants.JOB_KEY_PROPERTY, nullable = false)
    private String key;

    @Column(name = ModelConstants.JOB_DESCRIPTION_PROPERTY, nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.JOB_STATUS_PROPERTY, nullable = false)
    private JobStatus status;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.JOB_CONFIGURATION_PROPERTY, nullable = false)
    private JsonNode configuration;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.JOB_RESULT_PROPERTY)
    private JsonNode result;

    public JobEntity(Job job) {
        super(job);
        this.tenantId = getTenantUuid(job.getTenantId());
        this.type = job.getType();
        this.key = job.getKey();
        this.description = job.getDescription();
        this.status = job.getStatus();
        this.configuration = toJson(job.getConfiguration());
        this.result = toJson(job.getResult());
    }

    @Override
    public Job toData() {
        Job job = new Job();
        job.setId(new JobId(id));
        job.setCreatedTime(createdTime);
        job.setTenantId(getTenantId(tenantId));
        job.setType(type);
        job.setKey(key);
        job.setDescription(description);
        job.setStatus(status);
        job.setConfiguration(fromJson(configuration, JobConfiguration.class));
        job.setResult(fromJson(result, JobResult.class));
        return job;
    }

}
