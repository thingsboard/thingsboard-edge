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
package org.thingsboard.server.service.cf.ctx.state;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.service.cf.AbstractCalculatedFieldStateService;
import org.thingsboard.server.service.cf.CfRocksDb;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='in-memory'")
public class RocksDBCalculatedFieldStateService extends AbstractCalculatedFieldStateService {

    private final CfRocksDb cfRocksDb;

    private Set<TopicPartitionInfo> partitions;

    @Override
    protected void doPersist(CalculatedFieldEntityCtxId stateId, CalculatedFieldStateProto stateMsgProto, TbCallback callback) {
        cfRocksDb.put(stateId.toKey(), stateMsgProto.toByteArray());
        callback.onSuccess();
    }

    @Override
    protected void doRemove(CalculatedFieldEntityCtxId stateId, TbCallback callback) {
        cfRocksDb.delete(stateId.toKey());
        callback.onSuccess();
    }

    @Override
    public void restore(Set<TopicPartitionInfo> partitions) {
        if (this.partitions == null) {
            cfRocksDb.forEach((key, value) -> {
                try {
                    processRestoredState(CalculatedFieldStateProto.parseFrom(value));
                } catch (InvalidProtocolBufferException e) {
                    log.error("[{}] Failed to process restored state", key, e);
                }
            });
        }

        eventConsumer.update(partitions);
        this.partitions = partitions;
    }

    @Override
    public void stop() {
    }

}
