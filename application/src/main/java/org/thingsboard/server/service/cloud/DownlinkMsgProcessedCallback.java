/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud;

import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;

import java.util.List;

@Slf4j
public class DownlinkMsgProcessedCallback implements FutureCallback<List<Void>> {

    private final EdgeRpcClient client;
    private final EdgeInfoHolder edgeInfo;
    private final DownlinkMsg downlinkMsg;

    public DownlinkMsgProcessedCallback(EdgeRpcClient client, EdgeInfoHolder edgeInfo, DownlinkMsg downlinkMsg) {
        this.client = client;
        this.edgeInfo = edgeInfo;
        this.downlinkMsg = downlinkMsg;
    }

    @Override
    public void onSuccess(List<Void> result) {
        log.trace("[{}] DownlinkMsg has been processed successfully! DownlinkMsgId {}", edgeInfo.getRoutingKey(), downlinkMsg.getDownlinkMsgId());
        DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                .setSuccess(true).build();

        client.sendDownlinkResponseMsg(downlinkResponseMsg);
    }

    @Override
    public void onFailure(Throwable t) {
        log.error("[{}] Failed to process DownlinkMsg! DownlinkMsgId {}", edgeInfo.getRoutingKey(), downlinkMsg.getDownlinkMsgId());
        String errorMsg = EdgeUtils.createErrorMsgFromRootCauseAndStackTrace(t);
        DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                .setSuccess(false).setErrorMsg(errorMsg).build();

        client.sendDownlinkResponseMsg(downlinkResponseMsg);
    }
}
