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
package org.thingsboard.server.service.update;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.msg.notification.trigger.NewPlatformVersionTrigger;
import org.thingsboard.server.queue.notification.NotificationRuleProcessor;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
public class DefaultUpdateService implements UpdateService {

    private static final String INSTANCE_ID_FILE = ".instance_id";
    // private static final String UPDATE_SERVER_BASE_URL = "https://updates.thingsboard.io";
    private static final String UPDATE_SERVER_BASE_URL = "https://updates.thingsboard.io";

    private static final String PLATFORM_PARAM = "platform";
    private static final String VERSION_PARAM = "version";
    private static final String INSTANCE_ID_PARAM = "instanceId";

    @Value("${updates.enabled}")
    private boolean updatesEnabled;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Autowired
    private NotificationRuleProcessor notificationRuleProcessor;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, ThingsBoardThreadFactory.forName("tb-update-service"));

    private ScheduledFuture<?> checkUpdatesFuture = null;
    private final RestTemplate restClient = new RestTemplate();

    private UpdateMessage updateMessage;

    private String platform;
    private String version;
    private UUID instanceId = null;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() {
        version = buildProperties != null ? buildProperties.getVersion() : "unknown";
        updateMessage = new UpdateMessage(false, version, "", "",
                "https://thingsboard.io/docs/pe/reference/releases",
                "https://thingsboard.io/docs/pe/reference/releases");
        if (updatesEnabled) {
            try {
                platform = System.getProperty("platform", "unknown");
                instanceId = parseInstanceId();
                checkUpdatesFuture = scheduler.scheduleAtFixedRate(checkUpdatesRunnable, 0, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                //Do nothing
            }
        }
    }

    private UUID parseInstanceId() throws IOException {
        UUID result = null;
        Path instanceIdPath = Paths.get(INSTANCE_ID_FILE);
        if (instanceIdPath.toFile().exists()) {
            byte[] data = Files.readAllBytes(instanceIdPath);
            if (data.length > 0) {
                try {
                    result = UUID.fromString(new String(data));
                } catch (IllegalArgumentException e) {
                    //Do nothing
                }
            }
        }
        if (result == null) {
            result = UUID.randomUUID();
            Files.write(instanceIdPath, result.toString().getBytes());
        }
        return result;
    }

    @PreDestroy
    private void destroy() {
        try {
            if (checkUpdatesFuture != null) {
                checkUpdatesFuture.cancel(true);
            }
            scheduler.shutdownNow();
        } catch (Exception e) {
            //Do nothing
        }
    }

    Runnable checkUpdatesRunnable = () -> {
        try {
            log.trace("Executing check update method for instanceId [{}], platform [{}] and version [{}]", instanceId, platform, version);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectNode request = JacksonUtil.newObjectNode();
            request.put(PLATFORM_PARAM, platform);
            request.put(VERSION_PARAM, version);
            request.put(INSTANCE_ID_PARAM, instanceId.toString());
            UpdateMessage prevUpdateMessage = updateMessage;
            updateMessage = restClient.postForObject(UPDATE_SERVER_BASE_URL + "/api/v2/thingsboard/updates", new HttpEntity<>(request.toString(), headers), UpdateMessage.class);
            if (updateMessage != null && updateMessage.isUpdateAvailable() && !updateMessage.equals(prevUpdateMessage)) {
                notificationRuleProcessor.process(NewPlatformVersionTrigger.builder()
                        .updateInfo(updateMessage)
                        .build());
            }
        } catch (Exception e) {
            log.trace(e.getMessage());
        }
    };

    @Override
    public UpdateMessage checkUpdates() {
        return updateMessage;
    }

}
