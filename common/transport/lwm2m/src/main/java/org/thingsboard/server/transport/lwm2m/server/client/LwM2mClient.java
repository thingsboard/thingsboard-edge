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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Data
public class LwM2mClient implements Cloneable {
    private String deviceName;
    private String deviceProfileName;
    private String endpoint;
    private String identity;
    private SecurityInfo securityInfo;
    private UUID deviceId;
    private UUID sessionId;
    private UUID profileId;
    private Registration registration;
    private ValidateDeviceCredentialsResponseMsg credentialsResponse;
    private final Map<String, ResourceValue> resources;
    private final Map<String, TransportProtos.TsKvProto> delayedRequests;
    private final List<String> pendingRequests;
    private boolean init;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public LwM2mClient(String endpoint, String identity, SecurityInfo securityInfo, ValidateDeviceCredentialsResponseMsg credentialsResponse, UUID profileId, UUID sessionId) {
        this.endpoint = endpoint;
        this.identity = identity;
        this.securityInfo = securityInfo;
        this.credentialsResponse = credentialsResponse;
        this.delayedRequests = new ConcurrentHashMap<>();
        this.pendingRequests = new CopyOnWriteArrayList<>();
        this.resources = new ConcurrentHashMap<>();
        this.profileId = profileId;
        this.sessionId = sessionId;
        this.init = false;
    }

    public void updateResourceValue(String pathRez, LwM2mResource rez) {
        if (rez instanceof LwM2mMultipleResource) {
            this.resources.put(pathRez, new ResourceValue(rez.getValues(), null, true));
        } else if (rez instanceof LwM2mSingleResource) {
            this.resources.put(pathRez, new ResourceValue(null, rez.getValue(), false));
        }
    }

    public void initValue(LwM2mTransportServiceImpl lwM2MTransportService, String path) {
        if (path != null) {
            this.pendingRequests.remove(path);
        }
        if (this.pendingRequests.size() == 0) {
            this.init = true;
            lwM2MTransportService.putDelayedUpdateResourcesThingsboard(this);
        }
    }

    public LwM2mClient copy() {
        return new LwM2mClient(this.endpoint, this.identity, this.securityInfo, this.credentialsResponse, this.profileId, this.sessionId);
    }
}

