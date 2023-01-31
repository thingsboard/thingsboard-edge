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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Data
public class CoapTestCallback implements CoapHandler {

    protected final CountDownLatch latch;
    protected Integer observe;
    protected byte[] payloadBytes;
    protected CoAP.ResponseCode responseCode;

    public CoapTestCallback() {
        this.latch = new CountDownLatch(1);
    }

    public CoapTestCallback(int subscribeCount) {
        this.latch = new CountDownLatch(subscribeCount);
    }

    public Integer getObserve() {
        return observe;
    }

    public byte[] getPayloadBytes() {
        return payloadBytes;
    }

    public CoAP.ResponseCode getResponseCode() {
        return responseCode;
    }

    @Override
    public void onLoad(CoapResponse response) {
        observe = response.getOptions().getObserve();
        payloadBytes = response.getPayload();
        responseCode = response.getCode();
        latch.countDown();
    }

    @Override
    public void onError() {
        log.warn("Command Response Ack Error, No connect");
    }

}
