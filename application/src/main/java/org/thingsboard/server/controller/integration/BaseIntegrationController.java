package org.thingsboard.server.controller.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.PlatformIntegrationService;
import org.thingsboard.server.service.integration.ThingsboardPlatformIntegration;

/**
 * Created by ashvayka on 18.12.17.
 */
public class BaseIntegrationController extends BaseController {

    @Autowired
    protected PlatformIntegrationService integrationService;

    @Autowired
    protected IntegrationContext context;

    protected <T> void process(ThingsboardPlatformIntegration<T> integration, T msg) {
        integration.process(context, msg);
    }

}
