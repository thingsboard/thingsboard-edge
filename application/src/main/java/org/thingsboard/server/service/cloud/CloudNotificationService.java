// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud;

import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;

public interface CloudNotificationService {

    void pushNotificationToCloud(TransportProtos.CloudNotificationMsgProto cloudNotificationMsg, TbCallback callback);

}
