// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.event.postgres;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.cloud.event.sender.CloudEventUplinkSender;
import org.thingsboard.server.service.cloud.event.sender.KafkaCloudEventUplinkSender;

@Service
@RequiredArgsConstructor
public class PostgresToKafkaCloudEventUplinkMigrationDispatcher extends AbstractPostgresCloudEventUplinkBatchDispatcher {

    private final KafkaCloudEventUplinkSender kafkaCloudEventUplinkMigrationSender;

    @Override
    protected CloudEventUplinkSender getCloudEventUplinkSender() {
        return kafkaCloudEventUplinkMigrationSender;
    }
}
