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
package org.thingsboard.integration.aws.kinesis;

import lombok.Data;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.retrieval.RecordsFetcherFactory;
import software.amazon.kinesis.retrieval.RetrievalFactory;
import software.amazon.kinesis.retrieval.RetrievalSpecificConfig;
import software.amazon.kinesis.retrieval.polling.SimpleRecordsFetcherFactory;
import software.amazon.kinesis.retrieval.polling.SynchronousBlockingRetrievalFactory;

import java.time.Duration;

@Data
public class KinesisPollingConfig implements RetrievalSpecificConfig {

    private final String streamName;

    private final KinesisAsyncClient kinesisClient;

    private int maxRecords = 10000;

    private Duration kinesisRequestTimeout = Duration.ofSeconds(30);

    private RecordsFetcherFactory recordsFetcherFactory = new SimpleRecordsFetcherFactory();

    @Override
    public RetrievalFactory retrievalFactory() {
        return new SynchronousBlockingRetrievalFactory(streamName, kinesisClient, recordsFetcherFactory, maxRecords, kinesisRequestTimeout);
    }
}
