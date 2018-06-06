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

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.queue.MsgQueue;
import org.thingsboard.server.dao.queue.db.MsgAck;
import org.thingsboard.server.dao.queue.db.UnprocessedMsgFilter;
import org.thingsboard.server.dao.queue.db.repository.AckRepository;
import org.thingsboard.server.dao.queue.db.repository.MsgRepository;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "actors.rule.queue", value = "type", havingValue = "db")
@Slf4j
@NoSqlDao
public class CassandraMsgQueue implements MsgQueue {

    @Autowired
    private MsgRepository msgRepository;
    @Autowired
    private AckRepository ackRepository;
    @Autowired
    private UnprocessedMsgFilter unprocessedMsgFilter;
    @Autowired
    private QueuePartitioner queuePartitioner;

    @Override
    public ListenableFuture<Void> put(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        long msgTime = getMsgTime(msg);
        long tsPartition = queuePartitioner.getPartition(msgTime);
        return msgRepository.save(msg, nodeId, clusterPartition, tsPartition, msgTime);
    }

    @Override
    public ListenableFuture<Void> ack(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        long tsPartition = queuePartitioner.getPartition(getMsgTime(msg));
        MsgAck ack = new MsgAck(msg.getId(), nodeId, clusterPartition, tsPartition);
        return ackRepository.ack(ack);
    }

    @Override
    public Iterable<TbMsg> findUnprocessed(TenantId tenantId, UUID nodeId, long clusterPartition) {
        List<TbMsg> unprocessedMsgs = Lists.newArrayList();
        for (Long tsPartition : queuePartitioner.findUnprocessedPartitions(nodeId, clusterPartition)) {
            List<TbMsg> msgs = msgRepository.findMsgs(nodeId, clusterPartition, tsPartition);
            List<MsgAck> acks = ackRepository.findAcks(nodeId, clusterPartition, tsPartition);
            unprocessedMsgs.addAll(unprocessedMsgFilter.filter(msgs, acks));
        }
        return unprocessedMsgs;
    }

    @Override
    public ListenableFuture<Void> cleanUp(TenantId tenantId) {
        return Futures.immediateFuture(null);
    }

    private long getMsgTime(TbMsg msg) {
        return UUIDs.unixTimestamp(msg.getId());
    }

}
