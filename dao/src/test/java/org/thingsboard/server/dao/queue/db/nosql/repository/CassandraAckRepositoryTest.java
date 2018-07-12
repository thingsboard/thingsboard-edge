/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoNoSqlTest;
import org.thingsboard.server.dao.queue.db.MsgAck;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@DaoNoSqlTest
public class CassandraAckRepositoryTest extends AbstractServiceTest {

    @Autowired
    private CassandraAckRepository ackRepository;

    @Test
    public void acksInPartitionCouldBeFound() {
        UUID nodeId = UUID.fromString("055eee50-1883-11e8-b380-65b5d5335ba9");

        List<MsgAck> extectedAcks = Lists.newArrayList(
                new MsgAck(UUID.fromString("bebaeb60-1888-11e8-bf21-65b5d5335ba9"), nodeId, 101L, 300L),
                new MsgAck(UUID.fromString("12baeb60-1888-11e8-bf21-65b5d5335ba9"), nodeId, 101L, 300L)
        );

        List<MsgAck> actualAcks = ackRepository.findAcks(nodeId, 101L, 300L);
        assertEquals(extectedAcks, actualAcks);
    }

    @Test
    public void ackCanBeSavedAndRead() throws ExecutionException, InterruptedException {
        UUID msgId = UUIDs.timeBased();
        UUID nodeId = UUIDs.timeBased();
        MsgAck ack = new MsgAck(msgId, nodeId, 10L, 20L);
        ListenableFuture<Void> future = ackRepository.ack(ack);
        future.get();
        List<MsgAck> actualAcks = ackRepository.findAcks(nodeId, 10L, 20L);
        assertEquals(1, actualAcks.size());
        assertEquals(ack, actualAcks.get(0));
    }

    @Test
    public void expiredAcksAreNotReturned() throws ExecutionException, InterruptedException {
        ReflectionTestUtils.setField(ackRepository, "ackQueueTtl", 1);
        UUID msgId = UUIDs.timeBased();
        UUID nodeId = UUIDs.timeBased();
        MsgAck ack = new MsgAck(msgId, nodeId, 30L, 40L);
        ListenableFuture<Void> future = ackRepository.ack(ack);
        future.get();
        List<MsgAck> actualAcks = ackRepository.findAcks(nodeId, 30L, 40L);
        assertEquals(1, actualAcks.size());
        TimeUnit.SECONDS.sleep(2);
        assertTrue(ackRepository.findAcks(nodeId, 30L, 40L).isEmpty());
    }


}