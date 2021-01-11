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
package org.thingsboard.integration.http.tmobile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.http.AbstractHttpIntegration;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;

import java.util.List;

/**
 * Created by ashvayka on 02.12.17.
 */
@Slf4j
public class TMobileIotCdpIntegration extends AbstractHttpIntegration<HttpIntegrationMsg> {

    @Override
    protected ResponseEntity doProcess(HttpIntegrationMsg msg) throws Exception {

        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, mapper.writeValueAsBytes(msg.getMsg()), metadataTemplate);
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkData(context, data);
                log.info("[{}] Processing uplink data", data);
            }
        }
        return fromStatus(HttpStatus.OK);
    }

    @Override
    protected String getTypeUplink(HttpIntegrationMsg msg) {
        return "Uplink";
    }

}
