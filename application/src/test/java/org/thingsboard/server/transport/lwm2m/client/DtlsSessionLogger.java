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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.dtls.ClientHandshaker;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.SessionAdapter;
import org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState;

import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_READ_CONNECTION_ID;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_WRITE_CONNECTION_ID;

@Slf4j
public class DtlsSessionLogger extends SessionAdapter {

    private final Set<LwM2MClientState> clientStates;
    private final Map<LwM2MClientState, Integer> clientDtlsCid;

    public DtlsSessionLogger(Set<LwM2MClientState> clientStates, Map<LwM2MClientState, Integer> clientDtlsCid) {
        this.clientStates = clientStates;
        this.clientDtlsCid = clientDtlsCid;
    }

    @Override
    public void handshakeStarted(Handshaker handshaker) throws HandshakeException {
        if (handshaker instanceof ClientHandshaker) {
            log.info("DTLS Full Handshake initiated by client : STARTED ...");
        }
    }

    @Override
    public void contextEstablished(Handshaker handshaker, DTLSContext establishedContext) throws HandshakeException {
        if (handshaker instanceof ClientHandshaker) {
            log.warn("DTLS initiated by client: SUCCEED, WriteConnectionId: [{}], ReadConnectionId: [{}]", establishedContext.getWriteConnectionId(), establishedContext.getReadConnectionId());
            clientStates.add(ON_WRITE_CONNECTION_ID);
            clientStates.add(ON_READ_CONNECTION_ID);
            Integer lenWrite = establishedContext.getWriteConnectionId() == null ? null : establishedContext.getWriteConnectionId().getBytes().length;
            Integer lenRead = establishedContext.getReadConnectionId() == null ? null : establishedContext.getReadConnectionId().getBytes().length;
            clientDtlsCid.put(ON_WRITE_CONNECTION_ID, lenWrite);
            clientDtlsCid.put(ON_READ_CONNECTION_ID, lenRead);
        }
    }

    @Override
    public void handshakeFailed(Handshaker handshaker, Throwable error) {
        // get cause
        String cause;
        if (error != null) {
            if (error.getMessage() != null) {
                cause = error.getMessage();
            } else {
                cause = error.getClass().getName();
            }
        } else {
            cause = "unknown cause";
        }

        if (handshaker instanceof ClientHandshaker) {
            log.info("DTLS Full Handshake initiated by client : FAILED ({})", cause);
        }
    }
}
