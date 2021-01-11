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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.integration.api.IntegrationControllerApi;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import java.util.concurrent.Executor;

@TbCoreComponent
@Component
public class LocalIntegrationControllerApi implements IntegrationControllerApi {

    @Autowired
    private DbCallbackExecutorService callbackExecutorService;

    @Autowired
    private PlatformIntegrationService integrationService;

    @Override
    public ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String routingKey) {
        return integrationService.getIntegrationByRoutingKey(routingKey);
    }

    public <T> void process(ThingsboardPlatformIntegration<T> integration, T msg) {
        integration.process(msg);
    }

    @Override
    public Executor getCallbackExecutor() {
        return callbackExecutorService;
    }

}
