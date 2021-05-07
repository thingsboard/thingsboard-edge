/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStoreListener;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.util.Collection;

@Slf4j
public class TbLwM2mSecurityStore implements EditableSecurityStore {

    private final LwM2mClientContext clientContext;
    private final EditableSecurityStore securityStore;

    public TbLwM2mSecurityStore(LwM2mClientContext clientContext, EditableSecurityStore securityStore) {
        this.clientContext = clientContext;
        this.securityStore = securityStore;
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        return securityStore.getAll();
    }

    @Override
    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
        return securityStore.add(info);
    }

    @Override
    public SecurityInfo remove(String endpoint, boolean infosAreCompromised) {
        return securityStore.remove(endpoint, infosAreCompromised);
    }

    @Override
    public void setListener(SecurityStoreListener listener) {
        securityStore.setListener(listener);
    }

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        SecurityInfo securityInfo = securityStore.getByEndpoint(endpoint);
        if (securityInfo == null) {
            LwM2mClient lwM2mClient = clientContext.getClientByEndpoint(endpoint);
            if (lwM2mClient != null && lwM2mClient.getRegistration() != null && !lwM2mClient.getRegistration().getIdentity().isSecure()) {
                return null;
            }
            securityInfo = clientContext.fetchClientByEndpoint(endpoint).getSecurityInfo();
            try {
                if (securityInfo != null) {
                    add(securityInfo);
                }
            } catch (NonUniqueSecurityInfoException e) {
                log.warn("Failed to add security info: {}", securityInfo, e);
            }
        }
        return securityInfo;
    }

    @Override
    public SecurityInfo getByIdentity(String pskIdentity) {
        SecurityInfo securityInfo = securityStore.getByIdentity(pskIdentity);
        if (securityInfo == null) {
            securityInfo = clientContext.fetchClientByEndpoint(pskIdentity).getSecurityInfo();
            try {
                if (securityInfo != null) {
                    add(securityInfo);
                }
            } catch (NonUniqueSecurityInfoException e) {
                log.warn("Failed to add security info: {}", securityInfo, e);
            }
        }
        return securityInfo;
    }
}
