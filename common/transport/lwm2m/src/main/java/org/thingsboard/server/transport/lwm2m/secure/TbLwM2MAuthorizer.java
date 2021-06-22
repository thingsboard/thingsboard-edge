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
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.RequiredArgsConstructor;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;

@Component
@RequiredArgsConstructor
@TbLwM2mTransportComponent
public class TbLwM2MAuthorizer implements Authorizer {

    private final TbLwM2MDtlsSessionStore sessionStorage;
    private final TbSecurityStore securityStore;
    private final SecurityChecker securityChecker = new SecurityChecker();
    private final LwM2mClientContext clientContext;

    @Override
    public Registration isAuthorized(UplinkRequest<?> request, Registration registration, Identity senderIdentity) {
        if (senderIdentity.isX509()) {
            TbX509DtlsSessionInfo sessionInfo = sessionStorage.get(registration.getEndpoint());
            if (sessionInfo != null) {
                if (sessionInfo.getX509CommonName().endsWith(senderIdentity.getX509CommonName())) {
                    clientContext.registerClient(registration, sessionInfo.getCredentials());
                    // X509 certificate is valid and matches endpoint.
                    return registration;
                } else {
                    // X509 certificate is not valid.
                    return null;
                }
            }
            // If session info is not found, this may be the trusted certificate, so we still need to check all other options below.
        }
        SecurityInfo expectedSecurityInfo = null;
        if (securityStore != null) {
            expectedSecurityInfo = securityStore.getByEndpoint(registration.getEndpoint());
        }
        if (securityChecker.checkSecurityInfo(registration.getEndpoint(), senderIdentity, expectedSecurityInfo)) {
            return registration;
        } else {
            return null;
        }
    }
}
