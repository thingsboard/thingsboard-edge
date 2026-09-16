// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.rpc;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class CloudEventStorageSettings {
    @Value("${cloud.rpc.storage.max_read_records_count}")
    private int maxReadRecordsCount;
    @Value("${cloud.rpc.storage.no_read_records_sleep}")
    private long noRecordsSleepInterval;
    @Value("${cloud.rpc.storage.sleep_between_batches}")
    private long sleepIntervalBetweenBatches;
    @Value("${cloud.rpc.storage.misordering_compensation_millis:60000}")
    private long misorderingCompensationMillis;
}
