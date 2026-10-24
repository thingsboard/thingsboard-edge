// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.event.postgres;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.cloud.event.sender.GrpcCloudEventUplinkSender;
import org.thingsboard.server.service.cloud.event.sender.CloudEventUplinkSender;

@Service
@Slf4j
@RequiredArgsConstructor
public class BasePostgresCloudEventUplinkBatchDispatcher extends AbstractPostgresCloudEventUplinkBatchDispatcher {

    private final GrpcCloudEventUplinkSender baseCloudEventUplinkProcessor;

    @Override
    protected CloudEventUplinkSender getCloudEventUplinkSender() {
        return baseCloudEventUplinkProcessor;
    }
}
