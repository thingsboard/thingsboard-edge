/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.extensions.api.plugins.ws;

import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.api.plugins.PluginApiCallSecurityContext;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

public class BasicPluginWebsocketSessionRef implements PluginWebsocketSessionRef {

    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final PluginApiCallSecurityContext securityCtx;
    private final URI uri;
    private final transient Map<String, Object> attributes;
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;

    public BasicPluginWebsocketSessionRef(String sessionId, PluginApiCallSecurityContext securityCtx, URI uri, Map<String, Object> attributes,
            InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        super();
        this.sessionId = sessionId;
        this.securityCtx = securityCtx;
        this.uri = uri;
        this.attributes = attributes;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public TenantId getPluginTenantId() {
        return securityCtx.getPluginTenantId();
    }

    public PluginId getPluginId() {
        return securityCtx.getPluginId();
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicPluginWebsocketSessionRef other = (BasicPluginWebsocketSessionRef) obj;
        if (sessionId == null) {
            if (other.sessionId != null)
                return false;
        } else if (!sessionId.equals(other.sessionId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BasicPluginWebsocketSessionRef [sessionId=" + sessionId + ", pluginId=" + getPluginId() + "]";
    }

    @Override
    public PluginApiCallSecurityContext getSecurityCtx() {
        return securityCtx;
    }

}
