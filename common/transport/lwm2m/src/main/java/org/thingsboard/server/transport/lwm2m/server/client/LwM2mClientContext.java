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
package org.thingsboard.server.transport.lwm2m.server.client;

import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LwM2mClientContext {

    LwM2mClient getClientByEndpoint(String endpoint);

    LwM2mClient getClientBySessionInfo(TransportProtos.SessionInfoProto sessionInfo);

    Optional<TransportProtos.SessionInfoProto> register(LwM2mClient lwM2MClient, Registration registration) throws LwM2MClientStateException;

    void updateRegistration(LwM2mClient client, Registration registration) throws LwM2MClientStateException;

    void unregister(LwM2mClient client, Registration registration) throws LwM2MClientStateException;

    Collection<LwM2mClient> getLwM2mClients();

    //TODO: replace UUID with DeviceProfileId
    Lwm2mDeviceProfileTransportConfiguration getProfile(UUID profileUuId);

    Lwm2mDeviceProfileTransportConfiguration getProfile(Registration registration);

    Lwm2mDeviceProfileTransportConfiguration profileUpdate(DeviceProfile deviceProfile);

    Set<String> getSupportedIdVerInClient(LwM2mClient registration);

    LwM2mClient getClientByDeviceId(UUID deviceId);

    String getObjectIdByKeyNameFromProfile(LwM2mClient lwM2mClient, String keyName);

    void registerClient(Registration registration, ValidateDeviceCredentialsResponse credentials);

    void update(LwM2mClient lwM2MClient);

    void sendMsgsAfterSleeping(LwM2mClient lwM2MClient);

    void onUplink(LwM2mClient client);

    Long getRequestTimeout(LwM2mClient client);

    boolean asleep(LwM2mClient client);

    boolean awake(LwM2mClient client);

    boolean isDownlinkAllowed(LwM2mClient client);

}
