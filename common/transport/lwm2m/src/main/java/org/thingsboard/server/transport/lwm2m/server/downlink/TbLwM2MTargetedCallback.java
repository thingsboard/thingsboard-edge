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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;

import java.util.Arrays;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

@Slf4j
public abstract class TbLwM2MTargetedCallback<R, T> extends AbstractTbLwM2MRequestCallback<R, T> {

    protected final String versionedId;
    protected final String[] versionedIds;

    public TbLwM2MTargetedCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String versionedId) {
        super(logService, client);
        this.versionedId = versionedId;
        this.versionedIds = null;
    }

    public TbLwM2MTargetedCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String[] versionedIds) {
        super(logService, client);
        this.versionedId = null;
        this.versionedIds = versionedIds;
    }

    @Override
    public void onSuccess(R request, T response) {
        //TODO convert camelCase to "camel case" using .split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")
        if (response instanceof LwM2mResponse) {
            logForBadResponse(((LwM2mResponse) response).getCode().getCode(), response.toString(), request.getClass().getSimpleName());
        }
    }

    public void logForBadResponse(int code, String responseStr, String requestName) {
        if (code > ResponseCode.CONTENT_CODE) {
            log.error("[{}] [{}] [{}] failed to process successful response [{}] ", client.getEndpoint(), requestName,
                    versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr);
            logService.log(client, String.format("[%s]: %s [%s] failed to process successful. Result: %s", LOG_LWM2M_ERROR,
                    requestName, versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr));
        } else {
            log.trace("[{}] {} [{}] successful: {}", client.getEndpoint(), requestName, versionedId != null ?
                    versionedId : versionedIds, responseStr);
            logService.log(client, String.format("[%s]: %s [%s] successful. Result: %s", LOG_LWM2M_INFO, requestName,
                    versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr));
        }
    }
}
