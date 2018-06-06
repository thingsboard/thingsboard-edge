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
package org.thingsboard.server.dao.queue.db.nosql;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.dao.queue.db.repository.ProcessedPartitionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueuePartitionerTest {

    private QueuePartitioner queuePartitioner;

    @Mock
    private ProcessedPartitionRepository partitionRepo;

    private Instant startInstant;
    private Instant endInstant;

    @Before
    public void init() {
        queuePartitioner = new QueuePartitioner("MINUTES", partitionRepo);
        startInstant = Instant.now();
        endInstant = startInstant.plus(2, ChronoUnit.MINUTES);
        queuePartitioner.setClock(Clock.fixed(endInstant, ZoneOffset.UTC));
    }

    @Test
    public void partitionCalculated() {
        long time = 1519390191425L;
        long partition = queuePartitioner.getPartition(time);
        assertEquals(1519390140000L, partition);
    }

    @Test
    public void unprocessedPartitionsReturned() {
        UUID nodeId = UUID.randomUUID();
        long clusteredHash = 101L;
        when(partitionRepo.findLastProcessedPartition(nodeId, clusteredHash)).thenReturn(Optional.of(startInstant.toEpochMilli()));
        List<Long> actual = queuePartitioner.findUnprocessedPartitions(nodeId, clusteredHash);
        assertEquals(3, actual.size());
    }

    @Test
    public void defaultShiftUsedIfNoPartitionWasProcessed() {
        UUID nodeId = UUID.randomUUID();
        long clusteredHash = 101L;
        when(partitionRepo.findLastProcessedPartition(nodeId, clusteredHash)).thenReturn(Optional.empty());
        List<Long> actual = queuePartitioner.findUnprocessedPartitions(nodeId, clusteredHash);
        assertEquals(10083, actual.size());
    }

}