/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.queue.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

@SpringBootTest(classes = TbKafkaSettings.class)
@TestPropertySource(properties = {
        "queue.type=kafka",
        "queue.kafka.bootstrap.servers=localhost:9092",
        "queue.kafka.other-inline=metrics.recording.level:INFO;metrics.sample.window.ms:30000",
})
class TbKafkaSettingsTest {

    @Autowired
    TbKafkaSettings settings;

    @BeforeEach
    void beforeEach() {
        settings = spy(settings); //SpyBean is not aware on @ConditionalOnProperty, that is why the traditional spy in use
    }

    @Test
    void givenToProps_whenConfigureSSL_thenVerifyOnce() {
        Properties props = settings.toProps();

        assertThat(props).as("TB_QUEUE_KAFKA_REQUEST_TIMEOUT_MS").containsEntry("request.timeout.ms", 30000);
        assertThat(props).as("TB_QUEUE_KAFKA_SESSION_TIMEOUT_MS").containsEntry("session.timeout.ms", 10000);

        //other-inline
        assertThat(props).as("metrics.recording.level").containsEntry("metrics.recording.level", "INFO");
        assertThat(props).as("TB_QUEUE_KAFKA_SESSION_TIMEOUT_MS").containsEntry("metrics.sample.window.ms", "30000");

        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

    @Test
    void givenToAdminProps_whenConfigureSSL_thenVerifyOnce() {
        settings.toAdminProps();
        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

    @Test
    void givenToConsumerProps_whenConfigureSSL_thenVerifyOnce() {
        settings.toConsumerProps("main");
        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

    @Test
    void givenTotoProducerProps_whenConfigureSSL_thenVerifyOnce() {
        settings.toProducerProps();
        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

}