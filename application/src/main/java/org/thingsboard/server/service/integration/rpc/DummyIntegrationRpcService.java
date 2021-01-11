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
package org.thingsboard.server.service.integration.rpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.integration.Integration;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "integrations.rpc", value = "enabled", havingValue = "false")
public class DummyIntegrationRpcService implements IntegrationRpcService {

    @Override
    public void updateIntegration(Integration integration) {

    }

    @Override
    public void updateConverter(Converter converter) {

    }

    @Override
    public boolean handleRemoteDownlink(IntegrationDownlinkMsg msg) {
        return false;
    }
}
