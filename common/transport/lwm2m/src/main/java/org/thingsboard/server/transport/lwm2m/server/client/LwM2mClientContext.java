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
package org.thingsboard.server.transport.lwm2m.server.client;

import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface LwM2mClientContext {

    void removeClientByRegistrationId(String registrationId);

    LwM2mClient getClientByEndpoint(String endpoint);

    LwM2mClient getClientByRegistrationId(String registrationId);

    LwM2mClient getClient(TransportProtos.SessionInfoProto sessionInfo);

    LwM2mClient getOrRegister(Registration registration);

    LwM2mClient registerOrUpdate(Registration registration);

    LwM2mClient fetchClientByEndpoint(String endpoint);

    Registration getRegistration(String registrationId);

    Collection<LwM2mClient> getLwM2mClients();

    Map<UUID, LwM2mClientProfile> getProfiles();

    LwM2mClientProfile getProfile(UUID profileUuId);

    LwM2mClientProfile getProfile(Registration registration);

    Map<UUID, LwM2mClientProfile> setProfiles(Map<UUID, LwM2mClientProfile> profiles);

    LwM2mClientProfile profileUpdate(DeviceProfile deviceProfile);

    Set<String> getSupportedIdVerInClient(Registration registration);

    LwM2mClient getClientByDeviceId(UUID deviceId);

    void registerClient(Registration registration, ValidateDeviceCredentialsResponse credentials);
}
