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

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.cloud.CloudEventDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.service.ttl.cloud.CloudEventsCleanUpService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class BaseCloudEventControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private CloudEventDao cloudEventDao;
    @SpyBean
    private SqlPartitioningRepository partitioningRepository;
    @Autowired
    private CloudEventsCleanUpService cloudEventsCleanUpService;

    @Value("#{${sql.edge_events.partition_size} * 60 * 60 * 1000}")
    private long partitionDurationInMs;
    @Value("${sql.ttl.cloud_events.cloud_events_ttl}")
    private long cloudEventTtlInSec;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void saveCloudEvent_thenCreatePartitionIfNotExist() {
        reset(partitioningRepository);
        CloudEvent cloudEvent = createCloudEvent();
        verify(partitioningRepository).createPartitionIfNotExists(eq("cloud_event"), eq(cloudEvent.getCreatedTime()), eq(partitionDurationInMs));
        List<Long> partitions = partitioningRepository.fetchPartitions("cloud_event");
        assertThat(partitions).singleElement().satisfies(partitionStartTs -> {
            assertThat(partitionStartTs).isEqualTo(partitioningRepository.calculatePartitionStartTime(cloudEvent.getCreatedTime(), partitionDurationInMs));
        });
    }

    @Test
    public void cleanUpCloudEventByTtl_dropOldPartitions() {
        long oldCloudEventTs = LocalDate.of(2020, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long partitionStartTs = partitioningRepository.calculatePartitionStartTime(oldCloudEventTs, partitionDurationInMs);
        partitioningRepository.createPartitionIfNotExists("cloud_event", oldCloudEventTs, partitionDurationInMs);
        List<Long> partitions = partitioningRepository.fetchPartitions("cloud_event");
        assertThat(partitions).contains(partitionStartTs);

        cloudEventsCleanUpService.cleanUp();
        partitions = partitioningRepository.fetchPartitions("cloud_event");
        assertThat(partitions).doesNotContain(partitionStartTs);
        assertThat(partitions).allSatisfy(partitionsStart -> {
            long partitionEndTs = partitionsStart + partitionDurationInMs;
            assertThat(partitionEndTs).isGreaterThan(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(cloudEventTtlInSec));
        });
    }

    private CloudEvent createCloudEvent() {
        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setCreatedTime(System.currentTimeMillis());
        cloudEvent.setTenantId(tenantId);
        cloudEvent.setAction(EdgeEventActionType.ADDED);
        cloudEvent.setEntityId(tenantAdmin.getUuidId());
        cloudEvent.setType(CloudEventType.ALARM);
        try {
            cloudEventDao.saveAsync(cloudEvent).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return cloudEvent;
    }

}
