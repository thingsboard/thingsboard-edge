/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.security.diffPort;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.SocketIdentity;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.junit.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;

@DaoSqlTest
@Slf4j
public abstract class AbstractLwM2MIntegrationDiffPortTest extends AbstractSecurityLwM2MIntegrationTest {

    @SpyBean
    private RegistrationStore registrationStoreTest;

    protected void basicTestConnectionDifferentPort(Lwm2mDeviceProfileTransportConfiguration transportConfiguration,
                                                    String awaitAlias) throws Exception {

        createDeviceProfile(transportConfiguration);
        createDevice(deviceCredentials, clientEndpoint);
        createNewClient(security, null, COAP_CONFIG, true, clientEndpoint);
        lwM2MTestClient.start(true);
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_SUCCESS) || lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_STARTED));
        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatusesRegistrationLwm2mSuccess));

        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    doAnswer(new Answer<Object>() {
                        @Override
                        public Object answer(InvocationOnMock invocation) throws Throwable {
                            Object[] arguments = invocation.getArguments();
                            if (arguments.length > 0 && arguments[0] instanceof RegistrationUpdate) {
                                int portOld = ((RegistrationUpdate) arguments[0]).getPort();
                                int portValueChange = 5;
                                arguments[0] = registrationUpdateNewPort((RegistrationUpdate) arguments[0], portValueChange);
                                int portNew =  ((RegistrationUpdate) arguments[0]).getPort();
                                Assert.assertEquals((portNew - portOld), portValueChange);
                            }
                            return invocation.callRealMethod();
                        }
                    }).when(registrationStoreTest).updateRegistration(any(RegistrationUpdate.class));
                    return  lwM2MTestClient.getClientStates().contains(ON_UPDATE_SUCCESS);
                });
        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatusesRegistrationLwm2mSuccessUpdate));
    }

    private RegistrationUpdate registrationUpdateNewPort (RegistrationUpdate update, int portValueChange) {
        Integer portOld = update.getPort();
        Integer portNew = portOld + portValueChange;
        log.warn("portOld: [{}], portNew: [{}]", portOld, portNew);
        InetAddress addressOld = update.getAddress();
        InetSocketAddress socketAddressUpdate = new InetSocketAddress(addressOld, portNew);
        SocketIdentity socketIdentity = new SocketIdentity(socketAddressUpdate);
        LwM2mPeer sender = new IpPeer(new InetSocketAddress(addressOld, portNew), socketIdentity);
        return new RegistrationUpdate(update.getRegistrationId(), sender,
                update.getLifeTimeInSec(), update.getSmsNumber(), update.getBindingMode(),
                update.getObjectLinks(), update.getAlternatePath(),
                update.getSupportedContentFormats(), update.getSupportedObjects(),
                update.getAvailableInstances(), update.getAdditionalAttributes(),
                update.getApplicationData());
    }
}
