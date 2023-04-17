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
package org.thingsboard.server.transport.coap;

import lombok.RequiredArgsConstructor;
import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.EndpointContext;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class TbCoapMessageObserver implements MessageObserver {

    private final int msgId;
    private final Consumer<Integer> onAcknowledge;
    private final Consumer<Integer> onTimeout;

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public void onRetransmission() {

    }

    @Override
    public void onResponse(Response response) {

    }

    @Override
    public void onAcknowledgement() {
        onAcknowledge.accept(msgId);
    }

    @Override
    public void onReject() {

    }

    @Override
    public void onTimeout() {
        if (onTimeout != null) {
            onTimeout.accept(msgId);
        }
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onReadyToSend() {

    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onDtlsRetransmission(int flight) {

    }

    @Override
    public void onSent(boolean retransmission) {

    }

    @Override
    public void onSendError(Throwable error) {

    }

    @Override
    public void onResponseHandlingError(Throwable cause) {

    }

    @Override
    public void onContextEstablished(EndpointContext endpointContext) {

    }

    @Override
    public void onTransferComplete() {

    }
}
