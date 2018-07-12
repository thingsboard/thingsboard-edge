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

//import static org.junit.jupiter.api.Assertions.*;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoNoSqlTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@DaoNoSqlTest
public class CassandraMsgRepositoryTest extends AbstractServiceTest {

    @Autowired
    private CassandraMsgRepository msgRepository;

    @Test
    public void msgCanBeSavedAndRead() throws ExecutionException, InterruptedException {
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "type", new DeviceId(UUIDs.timeBased()), null, TbMsgDataType.JSON, "0000",
                new RuleChainId(UUIDs.timeBased()), new RuleNodeId(UUIDs.timeBased()), 0L);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = msgRepository.save(msg, nodeId, 1L, 1L, 1L);
        future.get();
        List<TbMsg> msgs = msgRepository.findMsgs(nodeId, 1L, 1L);
        assertEquals(1, msgs.size());
    }

    @Test
    public void expiredMsgsAreNotReturned() throws ExecutionException, InterruptedException {
        ReflectionTestUtils.setField(msgRepository, "msqQueueTtl", 1);
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "type", new DeviceId(UUIDs.timeBased()), null, TbMsgDataType.JSON, "0000",
                new RuleChainId(UUIDs.timeBased()), new RuleNodeId(UUIDs.timeBased()), 0L);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = msgRepository.save(msg, nodeId, 2L, 2L, 2L);
        future.get();
        TimeUnit.SECONDS.sleep(2);
        assertTrue(msgRepository.findMsgs(nodeId, 2L, 2L).isEmpty());
    }

    @Test
    public void protoBufConverterWorkAsExpected() throws ExecutionException, InterruptedException {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("key", "value");
        String dataStr = "someContent";
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "type", new DeviceId(UUIDs.timeBased()), metaData, TbMsgDataType.JSON, dataStr,
                new RuleChainId(UUIDs.timeBased()), new RuleNodeId(UUIDs.timeBased()), 0L);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = msgRepository.save(msg, nodeId, 1L, 1L, 1L);
        future.get();
        List<TbMsg> msgs = msgRepository.findMsgs(nodeId, 1L, 1L);
        assertEquals(1, msgs.size());
        assertEquals(msg, msgs.get(0));
    }


}