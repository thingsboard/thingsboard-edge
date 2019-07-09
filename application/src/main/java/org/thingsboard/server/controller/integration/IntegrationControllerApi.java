package org.thingsboard.server.controller.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;

import java.util.concurrent.Executor;

public interface IntegrationControllerApi {

    ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String routingKey);

    <T> void process(ThingsboardPlatformIntegration<T> integration, T msg);

    Executor getCallbackExecutor();

}
