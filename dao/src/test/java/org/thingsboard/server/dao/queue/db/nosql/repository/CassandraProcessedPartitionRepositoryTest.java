/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.dao.queue.db.nosql.repository;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoNoSqlTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@DaoNoSqlTest
public class CassandraProcessedPartitionRepositoryTest extends AbstractServiceTest {

    @Autowired
    private CassandraProcessedPartitionRepository partitionRepository;

    @Test
    public void lastProcessedPartitionCouldBeFound() {
        UUID nodeId = UUID.fromString("055eee50-1883-11e8-b380-65b5d5335ba9");
        Optional<Long> lastProcessedPartition = partitionRepository.findLastProcessedPartition(nodeId, 101L);
        assertTrue(lastProcessedPartition.isPresent());
        assertEquals((Long) 777L, lastProcessedPartition.get());
    }

    @Test
    public void highestProcessedPartitionReturned() throws ExecutionException, InterruptedException {
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future1 = partitionRepository.partitionProcessed(nodeId, 303L, 100L);
        ListenableFuture<Void> future2 = partitionRepository.partitionProcessed(nodeId, 303L, 200L);
        ListenableFuture<Void> future3 = partitionRepository.partitionProcessed(nodeId, 303L, 10L);
        ListenableFuture<List<Void>> allFutures = Futures.allAsList(future1, future2, future3);
        allFutures.get();
        Optional<Long> actual = partitionRepository.findLastProcessedPartition(nodeId, 303L);
        assertTrue(actual.isPresent());
        assertEquals((Long) 200L, actual.get());
    }

    @Test
    public void expiredPartitionsAreNotReturned() throws ExecutionException, InterruptedException {
        ReflectionTestUtils.setField(partitionRepository, "partitionsTtl", 1);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = partitionRepository.partitionProcessed(nodeId, 404L, 10L);
        future.get();
        Optional<Long> actual = partitionRepository.findLastProcessedPartition(nodeId, 404L);
        assertEquals((Long) 10L, actual.get());
        TimeUnit.SECONDS.sleep(2);
        assertFalse(partitionRepository.findLastProcessedPartition(nodeId, 404L).isPresent());
    }

    @Test
    public void ifNoPartitionsWereProcessedEmptyResultReturned() {
        UUID nodeId = UUIDs.timeBased();
        Optional<Long> actual = partitionRepository.findLastProcessedPartition(nodeId, 505L);
        assertFalse(actual.isPresent());
    }

}