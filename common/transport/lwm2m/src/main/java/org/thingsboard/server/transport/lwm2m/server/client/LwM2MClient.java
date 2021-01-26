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
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportService;
import org.thingsboard.server.transport.lwm2m.server.ResultIds;

import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Data
public class LwM2MClient implements Cloneable {
    private String deviceName;
    private String deviceProfileName;
    private String endPoint;
    private String identity;
    private SecurityInfo info;
    private UUID deviceUuid;
    private UUID sessionUuid;
    private UUID profileUuid;
    private LeshanServer lwServer;
    private LwM2MTransportService lwM2MTransportService;
    private Registration registration;
    private ValidateDeviceCredentialsResponseMsg credentialsResponse;
    private Map<String, String> attributes;
    private Map<Integer, ModelObject> modelObjects;
    private Set<String> pendingRequests;
    private Map<String, LwM2mResponse> responses;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public LwM2MClient(String endPoint, String identity, SecurityInfo info, ValidateDeviceCredentialsResponseMsg credentialsResponse, Map<String, String> attributes, Map<Integer, ModelObject> modelObjects, UUID profileUuid) {
        this.endPoint = endPoint;
        this.identity = identity;
        this.info = info;
        this.credentialsResponse = credentialsResponse;
        this.attributes = (attributes != null && attributes.size() > 0) ? attributes : new ConcurrentHashMap<String, String>();
        this.modelObjects = (modelObjects != null && modelObjects.size() > 0) ? modelObjects : new ConcurrentHashMap<Integer, ModelObject>();
        this.pendingRequests = ConcurrentHashMap.newKeySet();
        this.profileUuid = profileUuid;
        /**
         * Key <objectId>, response<Value -> instance -> resources: value...>
         */
        this.responses = new ConcurrentHashMap<>();
    }

    /**
     *  Fill with data -> Model client
     * @param path -
     * @param response -
     */
    public void onSuccessHandler(String path, LwM2mResponse response) {
        this.responses.put(path, response);
        this.pendingRequests.remove(path);
        if (this.pendingRequests.size() == 0) {
            this.initValue();
            this.lwM2MTransportService.updatesAndSentModelParameter(this.lwServer, this.registration);
        }
    }

    private void initValue() {
        this.responses.forEach((key, resp) -> {
            ResultIds pathIds = new ResultIds(key);
            if (pathIds.getObjectId() > -1) {
                ObjectModel objectModel = ((Collection<ObjectModel>) this.lwServer.getModelProvider().getObjectModel(registration).getObjectModels()).stream().filter(v -> v.id == pathIds.getObjectId()).collect(Collectors.toList()).get(0);
                if (this.modelObjects.get(pathIds.getObjectId()) != null) {
                    this.modelObjects.get(pathIds.getObjectId()).getInstances().put(((ReadResponse) resp).getContent().getId(), (LwM2mObjectInstance) ((ReadResponse) resp).getContent());
                } else {
                    Map<Integer, LwM2mObjectInstance> instances = new ConcurrentHashMap<>();
                    instances.put(((ReadResponse) resp).getContent().getId(), (LwM2mObjectInstance) ((ReadResponse) resp).getContent());
                    ModelObject modelObject = new ModelObject(objectModel, instances);
                    this.modelObjects.put(pathIds.getObjectId(), modelObject);
                }
            }
        });
    }
}
