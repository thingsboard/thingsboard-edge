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
package org.thingsboard.server.transport.lwm2m.server.secure;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStoreListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MGetSecurityInfo;
import org.thingsboard.server.transport.lwm2m.secure.ReadResultSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler;
import org.thingsboard.server.transport.lwm2m.server.client.AttrTelemetryObserveValue;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClient;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import java.util.Map;
import java.util.UUID;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.*;

@Slf4j
@Component("LwM2mInMemorySecurityStore")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' )|| ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2mInMemorySecurityStore extends InMemorySecurityStore {
    // lock for the two maps
    protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();
    private final boolean infosAreCompromised = false;
    protected Map<String /** registrationId */, LwM2MClient> sessions = new ConcurrentHashMap<>();
    protected Map<UUID /** profileUUid */, AttrTelemetryObserveValue> profiles = new ConcurrentHashMap<>();
    private SecurityStoreListener listener;

    @Autowired
    LwM2MGetSecurityInfo lwM2MGetSecurityInfo;

    @Override
    public SecurityInfo getByEndpoint(String endPoint) {
        readLock.lock();
        try {
            String registrationId = this.getByRegistrationId(endPoint, null);
            return (registrationId != null && sessions.size() > 0 && sessions.get(registrationId) != null) ? sessions.get(registrationId).getInfo() : this.add(endPoint);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        readLock.lock();
        try {
            String integrationId = this.getByRegistrationId(null, identity);
            return (integrationId != null) ? sessions.get(integrationId).getInfo() : add(identity);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        readLock.lock();
        try {
            return Collections.unmodifiableCollection(this.sessions.entrySet().stream().map(model -> model.getValue().getInfo()).collect(Collectors.toList()));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Removed registration Client from sessions and listener
     * @param registrationId if Client
     */
    public void delRemoveSessionAndListener(String registrationId) {
        writeLock.lock();
        try {
            LwM2MClient lwM2MClient = (sessions.get(registrationId) != null) ? sessions.get(registrationId) : null;
            if (lwM2MClient != null) {
                if (listener != null) {
                    listener.securityInfoRemoved(infosAreCompromised, lwM2MClient.getInfo());
                }
                sessions.remove(registrationId);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void setListener(SecurityStoreListener listener) {
        this.listener = listener;
    }

    public LwM2MClient getlwM2MClient(String endPoint, String identity) {
        Map.Entry<String, LwM2MClient> modelClients = (endPoint != null) ?
                this.sessions.entrySet().stream().filter(model -> endPoint.equals(model.getValue().getEndPoint())).findAny().orElse(null) :
                this.sessions.entrySet().stream().filter(model -> identity.equals(model.getValue().getIdentity())).findAny().orElse(null);
        return (modelClients != null) ? modelClients.getValue() : null;
    }

    public LwM2MClient getlwM2MClient(String registrationId) {
        return this.sessions.get(registrationId);
    }

    public LwM2MClient getlwM2MClient(LeshanServer lwServer, Registration registration) {
        writeLock.lock();
        try {
            if (this.sessions.get(registration.getEndpoint()) == null) {
                this.add(registration.getEndpoint());
            }
            LwM2MClient lwM2MClient = this.sessions.get(registration.getEndpoint());
            lwM2MClient.setLwServer(lwServer);
            lwM2MClient.setRegistration(registration);
            lwM2MClient.setAttributes(registration.getAdditionalRegistrationAttributes());
            this.sessions.put(registration.getId(), lwM2MClient);
            this.sessions.remove(registration.getEndpoint());
            return lwM2MClient;
        } finally {
            writeLock.unlock();
        }
    }

    private String getByRegistrationId(String endPoint, String identity) {
        List<String> registrationIds = (endPoint != null) ?
                this.sessions.entrySet().stream().filter(model -> endPoint.equals(model.getValue().getEndPoint())).map(model -> model.getKey()).collect(Collectors.toList()) :
                this.sessions.entrySet().stream().filter(model -> identity.equals(model.getValue().getIdentity())).map(model -> model.getKey()).collect(Collectors.toList());
        return (registrationIds != null && registrationIds.size() > 0) ? registrationIds.get(0) : null;
    }

    public String getByRegistrationId(String credentialsId) {
        List<String> registrationIds = (this.sessions.entrySet().stream().filter(model -> credentialsId.equals(model.getValue().getEndPoint())).map(model -> model.getKey()).collect(Collectors.toList()).size() > 0) ?
                this.sessions.entrySet().stream().filter(model -> credentialsId.equals(model.getValue().getEndPoint())).map(model -> model.getKey()).collect(Collectors.toList()) :
                this.sessions.entrySet().stream().filter(model -> credentialsId.equals(model.getValue().getIdentity())).map(model -> model.getKey()).collect(Collectors.toList());
        return (registrationIds != null && registrationIds.size() > 0) ? registrationIds.get(0) : null;
    }

    public Registration getByRegistration(String registrationId) {
        return this.sessions.get(registrationId).getRegistration();
    }

    private SecurityInfo add(String identity) {
        ReadResultSecurityStore store = lwM2MGetSecurityInfo.getSecurityInfo(identity, TypeServer.CLIENT);
        UUID profileUuid = (addUpdateProfileParameters(store.getDeviceProfile())) ? store.getDeviceProfile().getUuidId() : null;
        if (store.getSecurityInfo() != null) {
            if (store.getSecurityMode() < DEFAULT_MODE.code) {
                String endpoint = store.getSecurityInfo().getEndpoint();
                sessions.put(endpoint, new LwM2MClient(endpoint, store.getSecurityInfo().getIdentity(), store.getSecurityInfo(), store.getMsg(), null, null, profileUuid));
            }
        } else {
            if (store.getSecurityMode() == NO_SEC.code)
                sessions.put(identity, new LwM2MClient(identity, null, null, store.getMsg(), null, null, profileUuid));
            else log.error("Registration failed: FORBIDDEN, endpointId: [{}]", identity);
        }
        return store.getSecurityInfo();
    }

    public Map<String, LwM2MClient> getSession (UUID sessionUuId){
       return this.sessions.entrySet().stream().filter(e -> e.getValue().getSessionUuid().equals(sessionUuId)).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
    }

    public Map<String, LwM2MClient> getSessions() {
        return this.sessions;
    }

    public Map<UUID, AttrTelemetryObserveValue> getProfiles() {
        return this.profiles;
    }

    public Map<UUID, AttrTelemetryObserveValue>setProfiles(Map<UUID, AttrTelemetryObserveValue> profiles) {
        return this.profiles = profiles;
    }

    /**
     *
     * @param deviceProfile
     */
    public boolean addUpdateProfileParameters(DeviceProfile deviceProfile) {
        JsonObject profilesConfigData = LwM2MTransportHandler.getObserveAttrTelemetryFromThingsboard(deviceProfile);
        if (profilesConfigData != null) {
            profiles.put(deviceProfile.getUuidId(), LwM2MTransportHandler.getNewProfileParameters(profilesConfigData));
        }
        return (profilesConfigData != null);
    }

}
