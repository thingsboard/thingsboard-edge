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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.nosql.CassandraAbstractDao;
import org.thingsboard.server.dao.queue.db.repository.MsgRepository;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@NoSqlDao
public class CassandraMsgRepository extends CassandraAbstractDao implements MsgRepository {

    @Value("${cassandra.queue.msg.ttl}")
    private int msqQueueTtl;

    @Override
    public ListenableFuture<Void> save(TbMsg msg, UUID nodeId, long clusterPartition, long tsPartition, long msgTs) {
        String insert = "INSERT INTO msg_queue (node_id, cluster_partition, ts_partition, ts, msg) VALUES (?, ?, ?, ?, ?) USING TTL ?";
        PreparedStatement statement = prepare(insert);
        BoundStatement boundStatement = statement.bind(nodeId, clusterPartition, tsPartition, msgTs, TbMsg.toBytes(msg), msqQueueTtl);
        ResultSetFuture resultSetFuture = executeAsyncWrite(boundStatement);
        return Futures.transform(resultSetFuture, (Function<ResultSet, Void>) input -> null);
    }

    @Override
    public List<TbMsg> findMsgs(UUID nodeId, long clusterPartition, long tsPartition) {
        String select = "SELECT node_id, cluster_partition, ts_partition, ts, msg FROM msg_queue WHERE " +
                "node_id = ? AND cluster_partition = ? AND ts_partition = ?";
        PreparedStatement statement = prepare(select);
        BoundStatement boundStatement = statement.bind(nodeId, clusterPartition, tsPartition);
        ResultSet rows = executeRead(boundStatement);
        List<TbMsg> msgs = new ArrayList<>();
        for (Row row : rows) {
            msgs.add(TbMsg.fromBytes(row.getBytes("msg")));
        }
        return msgs;
    }

}
