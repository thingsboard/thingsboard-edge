// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.rpc;

import org.thingsboard.server.gen.edge.v1.UplinkMsg;

public interface GrpcClientManager {

    void sendUplinkMsg(UplinkMsg msg);
    void establishRpcConnection();
    boolean isConnected();

}
