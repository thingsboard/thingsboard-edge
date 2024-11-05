/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.queue.provider;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

@Service
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class TbCoreCloudEventProvider implements TbCloudEventProvider {

    private final TbCloudEventQueueFactory tbCloudEventQueueProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> toCloudEventProducer;
    private TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> toCloudEventConsumer;
    private TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> toCloudEventTSProducer;
    private TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> toCloudEventTSConsumer;

    public TbCoreCloudEventProvider(TbCloudEventQueueFactory tbCloudEventQueueProvider) {
        this.tbCloudEventQueueProvider = tbCloudEventQueueProvider;
    }

    @PostConstruct
    public void init() {
        toCloudEventProducer = tbCloudEventQueueProvider.createCloudEventMsgProducer();
        toCloudEventConsumer = tbCloudEventQueueProvider.createCloudEventMsgConsumer();
        toCloudEventTSProducer = tbCloudEventQueueProvider.createCloudEventTSMsgProducer();
        toCloudEventTSConsumer = tbCloudEventQueueProvider.createCloudEventTSMsgConsumer();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> getCloudEventMsgProducer() {
        return toCloudEventProducer;
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> getCloudEventMsgConsumer() {
        return toCloudEventConsumer;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> getCloudEventTSMsgProducer() {
        return toCloudEventTSProducer;
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> getCloudEventTSMsgConsumer() {
        return toCloudEventTSConsumer;
    }
}
