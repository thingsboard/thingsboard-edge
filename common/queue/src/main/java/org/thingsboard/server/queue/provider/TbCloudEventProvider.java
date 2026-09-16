// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.provider;

import org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

/**
 * Responsible for providing various Producers to other services.
 */
public interface TbCloudEventProvider {
    TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> getCloudEventMsgProducer();

    TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> getCloudEventTSMsgProducer();
}
