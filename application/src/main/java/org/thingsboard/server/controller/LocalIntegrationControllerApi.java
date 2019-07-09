package org.thingsboard.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.server.controller.integration.IntegrationControllerApi;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import java.util.concurrent.Executor;

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
