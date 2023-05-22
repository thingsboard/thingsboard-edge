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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.common.data.ResourceType.LWM2M_MODEL;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_KEY;

@Slf4j
@Service
@TbLwM2mTransportComponent
public class LwM2mVersionedModelProvider implements LwM2mModelProvider {

    private final LwM2mClientContext lwM2mClientContext;
    private final LwM2mTransportServerHelper helper;
    private final LwM2mTransportContext context;
    private final ConcurrentMap<TenantId, ConcurrentMap<String, ObjectModel>> models;

    public LwM2mVersionedModelProvider(@Lazy LwM2mClientContext lwM2mClientContext, LwM2mTransportServerHelper helper, LwM2mTransportContext context) {
        this.lwM2mClientContext = lwM2mClientContext;
        this.helper = helper;
        this.context = context;
        this.models = new ConcurrentHashMap<>();
    }

    private String getKeyIdVer(Integer objectId, String version) {
        return objectId != null ? objectId + LWM2M_SEPARATOR_KEY + ((version == null || version.isEmpty()) ? ObjectModel.DEFAULT_VERSION : version) : null;
    }

    @Override
    public LwM2mModel getObjectModel(Registration registration) {
        return new DynamicModel(registration);
    }

    public void evict(TenantId tenantId, String key) {
        if (tenantId.isNullUid()) {
            models.values().forEach(m -> m.remove(key));
        } else {
            models.get(tenantId).remove(key);
        }
    }

    private class DynamicModel implements LwM2mModel {
        private final Registration registration;
        private final TenantId tenantId;
        private final Lock modelsLock;

        public DynamicModel(Registration registration) {
            this.registration = registration;
            this.tenantId = lwM2mClientContext.getClientByEndpoint(registration.getEndpoint()).getTenantId();
            this.modelsLock = new ReentrantLock();
            if (tenantId != null) {
                models.computeIfAbsent(tenantId, t -> new ConcurrentHashMap<>());
            }
        }

        @Override
        public ResourceModel getResourceModel(int objectId, int resourceId) {
            try {
                ObjectModel objectModel = getObjectModel(objectId);
                if (objectModel != null)
                    return objectModel.resources.get(resourceId);
                else
                    log.trace("Tenant hasn't such the TbResources: Object model with id [{}/0/{}].", objectId, resourceId);
                return null;
            } catch (Exception e) {
                log.error("", e);
                return null;
            }
        }

        @Override
        public ObjectModel getObjectModel(int objectId) {
            String version = registration.getSupportedVersion(objectId);
            if (version != null) {
                return this.getObjectModelDynamic(objectId, version);
            }
            return null;
        }

        @Override
        public Collection<ObjectModel> getObjectModels() {
            Map<Integer, String> supportedObjects = this.registration.getSupportedObject();
            Collection<ObjectModel> result = new ArrayList<>(supportedObjects.size());
            for (Map.Entry<Integer, String> supportedObject : supportedObjects.entrySet()) {
                ObjectModel objectModel = this.getObjectModelDynamic(supportedObject.getKey(), supportedObject.getValue());
                if (objectModel != null) {
                    result.add(objectModel);
                }
            }
            return result;
        }

        private ObjectModel getObjectModelDynamic(Integer objectId, String version) {
            String key = getKeyIdVer(objectId, version);
            ObjectModel objectModel = tenantId != null ? models.get(tenantId).get(key) : null;
            if (tenantId != null && objectModel == null) {
                modelsLock.lock();
                try {
                    objectModel = models.get(tenantId).get(key);
                    if (objectModel == null) {
                        objectModel = getObjectModel(key);
                    }
                    if (objectModel != null) {
                        models.get(tenantId).put(key, objectModel);
                    } else {
                        log.error("Tenant hasn't such the resource: Object model with id [{}] version [{}].", objectId, version);
                    }
                } finally {
                    modelsLock.unlock();
                }
            }

            return objectModel;
        }

        private ObjectModel getObjectModel(String key) {
            Optional<TbResource> tbResource = context.getTransportResourceCache().get(this.tenantId, LWM2M_MODEL, key);
            return tbResource.map(resource -> helper.parseFromXmlToObjectModel(
                    Base64.getDecoder().decode(resource.getData()),
                    key + ".xml")).orElse(null);
        }
    }
}
