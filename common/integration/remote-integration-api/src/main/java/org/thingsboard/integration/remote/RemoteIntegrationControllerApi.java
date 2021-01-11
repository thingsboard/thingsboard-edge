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
package org.thingsboard.integration.remote;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.integration.api.IntegrationControllerApi;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.service.RemoteIntegrationManagerService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
@Data
public class RemoteIntegrationControllerApi implements IntegrationControllerApi {

    private ListeningExecutorService service;

    @Value("${executors.thread_pool_size}")
    private int executorThreadPoolSize;

    @Autowired
    private RemoteIntegrationManagerService managerService;

    @PostConstruct
    public void init() {
        this.service = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(executorThreadPoolSize));
    }

    @PreDestroy
    public void destroy() {
        if (this.service != null) {
            this.service.shutdown();
        }
    }

    @Override
    public ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String routingKey) {
        return Futures.immediateFuture(managerService.getIntegration());
    }

    @Override
    public <T> void process(ThingsboardPlatformIntegration<T> integration, T msg) {
        integration.process(msg);
    }

    @Override
    public Executor getCallbackExecutor() {
        return service;
    }
}
