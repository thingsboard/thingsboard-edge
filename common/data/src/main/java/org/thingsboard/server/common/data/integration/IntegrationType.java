/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.common.data.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum IntegrationType {
    OCEANCONNECT(false), SIGFOX(false), THINGPARK(false), TPE(false), TMOBILE_IOT_CDP(false), HTTP(false), MQTT(true),
    AWS_IOT(true), AWS_SQS(true), AWS_KINESIS(false), IBM_WATSON_IOT(true), TTN(true), TTI(true), AZURE_EVENT_HUB(true), OPC_UA(true),
    CUSTOM(false, true), UDP(false, true), TCP(false, true), KAFKA(false, false), AZURE_IOT_HUB(true), APACHE_PULSAR(false), RABBITMQ(false, false), LORIOT(false);

    IntegrationType(boolean singleton) {
        this.singleton = singleton;
        this.remoteOnly = false;
    }

    //Identifies if the Integration instance is one per cluster.
    @Getter
    private final boolean singleton;

    @Getter
    private final boolean remoteOnly;


}
