// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Data
@Component
public class TbQueueCloudEventTSSettings {

    @Value("${queue.cloud-event-ts.topic}")
    private String topic;
    @Value("${queue.cloud-event-ts.poll-interval}")
    private Long pollInterval;
}
