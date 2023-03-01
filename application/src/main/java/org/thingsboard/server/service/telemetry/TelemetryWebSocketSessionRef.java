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
package org.thingsboard.server.service.telemetry;

import lombok.Getter;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 27.03.18.
 */
public class TelemetryWebSocketSessionRef {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String sessionId;
    @Getter
    private final SecurityUser securityCtx;
    @Getter
    private final InetSocketAddress localAddress;
    @Getter
    private final InetSocketAddress remoteAddress;
    @Getter
    private final AtomicInteger sessionSubIdSeq;

    public TelemetryWebSocketSessionRef(String sessionId, SecurityUser securityCtx, InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        this.sessionId = sessionId;
        this.securityCtx = securityCtx;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.sessionSubIdSeq = new AtomicInteger();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelemetryWebSocketSessionRef that = (TelemetryWebSocketSessionRef) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "TelemetryWebSocketSessionRef{" +
                "sessionId='" + sessionId + '\'' +
                ", localAddress=" + localAddress +
                ", remoteAddress=" + remoteAddress +
                '}';
    }
}
