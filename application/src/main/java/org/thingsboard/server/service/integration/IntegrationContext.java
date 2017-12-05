package org.thingsboard.server.service.integration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.dao.device.DeviceService;

/**
 * Created by ashvayka on 05.12.17.
 */
@Component
@Data
public class IntegrationContext {

    @Lazy
    @Autowired
    private SessionMsgProcessor sessionMsgProcessor;

    @Lazy
    @Autowired
    private DeviceService deviceService;

    @Value("${http.request_timeout}")
    private long defaultHttpTimeout;


}
