// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.provider;

import org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

public interface TbCloudEventQueueFactory {

    TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> createCloudEventMsgConsumer();

    TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> createCloudEventMsgProducer();

    TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> createCloudEventTSMsgConsumer();

    TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> createCloudEventTSMsgProducer();

}
