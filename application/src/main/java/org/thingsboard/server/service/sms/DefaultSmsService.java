/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.dao.cloud.CloudEventService;

/**
 * On the Edge the SmsService is a thin client. Any send that would rely on admin-configured (tenant or
 * system) SMS provider settings cannot be resolved on the Edge, because the SMS settings are no longer
 * synced to the Edge. Instead of resolving config and opening a provider connection locally, the Edge
 * packages the call into an {@link EdgeSmsRequest} and enqueues a SEND_SMS cloud event; the Cloud resolves
 * the config and transmits via its own provider. The rule-node "own plaintext config" path builds its own
 * {@code SmsSender} directly and never goes through this service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSmsService implements SmsService {

    private final CloudEventService cloudEventService;

    @Override
    public void updateSmsConfiguration() {
        // SMS is sent from the Cloud on the Edge; there is no local SMS configuration to update.
    }

    @Override
    public void sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) throws ThingsboardException {
        enqueue(tenantId, EdgeSmsRequest.builder()
                .method(EdgeSmsRequest.SmsMethod.SEND_SMS)
                .numbers(numbersTo).message(message).build());
    }

    @Override
    public void sendTestSms(TestSmsRequest testSmsRequest) throws ThingsboardException {
        enqueue(TenantId.SYS_TENANT_ID, EdgeSmsRequest.builder()
                .method(EdgeSmsRequest.SmsMethod.SEND_TEST_SMS)
                .testSmsRequest(testSmsRequest).build());
    }

    @Override
    public boolean isConfigured(TenantId tenantId) {
        // SMS sending is delegated to the Cloud, which owns the configuration.
        return true;
    }

    private void enqueue(TenantId tenantId, EdgeSmsRequest request) throws ThingsboardException {
        try {
            cloudEventService.saveCloudEvent(tenantId, CloudEventType.TENANT, EdgeEventActionType.SEND_SMS,
                    tenantId, JacksonUtil.valueToTree(request));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        log.warn("Unable to send SMS: {}", message);
        return new ThingsboardException(String.format("Unable to send SMS: %s", message), ThingsboardErrorCode.GENERAL);
    }

}
