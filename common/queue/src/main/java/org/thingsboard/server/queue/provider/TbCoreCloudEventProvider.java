// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.provider;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class TbCoreCloudEventProvider implements TbCloudEventProvider {

    private final TbCloudEventQueueFactory tbCloudEventQueueProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> toCloudEventProducer;
    private TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> toCloudEventTSProducer;

    public TbCoreCloudEventProvider(TbCloudEventQueueFactory tbCloudEventQueueProvider) {
        this.tbCloudEventQueueProvider = tbCloudEventQueueProvider;
    }

    @PostConstruct
    public void init() {
        toCloudEventProducer = tbCloudEventQueueProvider.createCloudEventMsgProducer();
        toCloudEventTSProducer = tbCloudEventQueueProvider.createCloudEventTSMsgProducer();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> getCloudEventMsgProducer() {
        return toCloudEventProducer;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> getCloudEventTSMsgProducer() {
        return toCloudEventTSProducer;
    }
}

