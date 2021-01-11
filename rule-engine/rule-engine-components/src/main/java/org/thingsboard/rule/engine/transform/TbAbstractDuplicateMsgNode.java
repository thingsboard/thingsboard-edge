/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
public abstract class TbAbstractDuplicateMsgNode implements TbNode {

    private TbDuplicateMsgNodeConfiguration config;

    @Override
    public void init(TbContext context, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDuplicateMsgNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(duplicate(ctx, msg),
                messages -> {
                    if (messages != null && !messages.isEmpty()) {
                        if (messages.size() == 1) {
                            ctx.tellNext(messages.get(0), SUCCESS);
                        } else {
                            ctx.getPeContext().ack(msg);
                            for (TbMsg newMsg : messages) {
                                ctx.tellNext(newMsg, SUCCESS);
                            }
                        }
                    } else {
                        ctx.tellNext(msg, FAILURE);
                    }
                },
                t -> ctx.tellFailure(msg, t));
    }

    protected abstract ListenableFuture<List<TbMsg>> duplicate(TbContext ctx, TbMsg msg);

    public void setConfig(TbDuplicateMsgNodeConfiguration config) {
        this.config = config;
    }
}
