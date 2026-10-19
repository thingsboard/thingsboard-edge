// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.event.sender;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cloud.CloudEvent;

import java.util.List;

public interface CloudEventUplinkSender {

    ListenableFuture<Boolean> sendCloudEvents(List<CloudEvent> cloudEvents, boolean isGeneralMsg);
    void init();
    void shutdown();
}
