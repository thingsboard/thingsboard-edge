/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.http;

import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.io.IOException;
import java.util.function.Consumer;

class DeviceApiControllerTest {

    @Test
    void deviceAuthCallbackTest() {
        TransportContext transportContext = Mockito.mock(TransportContext.class);
        DeferredResult<ResponseEntity> responseWriter = Mockito.mock(DeferredResult.class);
        Consumer<TransportProtos.SessionInfoProto> onSuccess = x -> {
        };
        var callback = new DeviceApiController.DeviceAuthCallback(transportContext, responseWriter, onSuccess);

        callback.onError(new HttpMessageNotReadableException("JSON incorrect syntax"));

        callback.onError(new JsonParseException("Json ; expected"));

        callback.onError(new IOException("not found"));

        callback.onError(new RuntimeException("oops it is run time error"));
    }

    @Test
    void deviceProvisionCallbackTest() {
        DeferredResult<ResponseEntity> responseWriter = Mockito.mock(DeferredResult.class);
        var callback = new DeviceApiController.DeviceProvisionCallback(responseWriter);

        callback.onError(new HttpMessageNotReadableException("JSON incorrect syntax"));

        callback.onError(new JsonParseException("Json ; expected"));

        callback.onError(new IOException("not found"));

        callback.onError(new RuntimeException("oops it is run time error"));
    }

    @Test
    void getOtaPackageCallback() {
        TransportContext transportContext = Mockito.mock(TransportContext.class);
        DeferredResult<ResponseEntity> responseWriter = Mockito.mock(DeferredResult.class);
        String title = "Title";
        String version = "version";
        int chunkSize = 11;
        int chunk = 3;

        var callback = new DeviceApiController.GetOtaPackageCallback(transportContext, responseWriter, title, version, chunkSize, chunk);

        callback.onError(new HttpMessageNotReadableException("JSON incorrect syntax"));

        callback.onError(new JsonParseException("Json ; expected"));

        callback.onError(new IOException("not found"));

        callback.onError(new RuntimeException("oops it is run time error"));
    }
}
