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
package org.thingsboard.server.dao.resource;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Resource;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mInstance;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.LwM2mResource;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.device.DeviceServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseResourceService implements ResourceService {

    private final ResourceDao resourceDao;

    public BaseResourceService(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    @Override
    public Resource saveResource(Resource resource) {
        log.trace("Executing saveResource [{}]", resource);
        validate(resource);
        return resourceDao.saveResource(resource);
    }

    @Override
    public Resource getResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        log.trace("Executing getResource [{}] [{}] [{}]", tenantId, resourceType, resourceId);
        validate(tenantId, resourceType, resourceId);
        return resourceDao.getResource(tenantId, resourceType, resourceId);
    }

    @Override
    public void deleteResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        log.trace("Executing deleteResource [{}] [{}] [{}]", tenantId, resourceType, resourceId);
        validate(tenantId, resourceType, resourceId);
        resourceDao.deleteResource(tenantId, resourceType, resourceId);
    }

    @Override
    public PageData<Resource> findResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findAllByTenantId(tenantId, pageLink);
    }


    @Override
    public List<Resource> findAllByTenantIdAndResourceType(TenantId tenantId, ResourceType resourceType) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findAllByTenantIdAndResourceType(tenantId, resourceType);
    }

    @Override
    public List<LwM2mObject> findLwM2mObjectPage(TenantId tenantId, String sortProperty, String sortOrder, PageLink pageLink) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        PageData<Resource> resourcePageData = resourceDao.findResourcesByTenantIdAndResourceType(
                                                                        tenantId,
                                                                        ResourceType.LWM2M_MODEL, pageLink);
        List<LwM2mObject> lwM2mObjects = resourcePageData.getData().stream().map(this::toLwM2mObject).collect(Collectors.toList());
        return lwM2mObjects.size() > 1 ? this.sortList (lwM2mObjects, sortProperty, sortOrder) : lwM2mObjects;
    }

    @Override
    public List<LwM2mObject> findLwM2mObject(TenantId tenantId, String sortOrder,
                                             String sortProperty,
                                             String[] objectIds,
                                             String searchText) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<Resource> resources = resourceDao.findResourcesByTenantIdAndResourceType(tenantId, ResourceType.LWM2M_MODEL,
                                                                                        objectIds,
                                                                                        searchText);
        List<LwM2mObject> lwM2mObjects = resources.stream().map(this::toLwM2mObject).collect(Collectors.toList());
        return lwM2mObjects.size() > 1 ? this.sortList (lwM2mObjects, sortProperty, sortOrder) : lwM2mObjects;
    }

    @Override
    public void deleteResourcesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        resourceDao.removeAllByTenantId(tenantId);
    }

    protected void validate(Resource resource) {
        if (resource == null) {
            throw new DataValidationException("Resource should be specified!");
        }
        if (resource.getValue() == null) {
            throw new DataValidationException("Resource value should be specified!");
        }
        validate(resource.getTenantId(), resource.getResourceType(), resource.getResourceId());
        if (resource.getResourceType().equals(ResourceType.LWM2M_MODEL) && resource.toLwM2mObject() == null) {
            throw new DataValidationException(String.format("Could not parse the XML of objectModel with name %s", resource.getTextSearch()));
        }
    }

    protected void validate(TenantId tenantId, ResourceType resourceType, String resourceId) {
        if (resourceType == null) {
            throw new DataValidationException("Resource type should be specified!");
        }
        if (resourceId == null) {
            throw new DataValidationException("Resource id should be specified!");
        }
        validateId(tenantId, "Incorrect tenantId ");
    }

    private LwM2mObject toLwM2mObject(Resource resource) {
        try {
            DDFFileParser ddfFileParser = new DDFFileParser(new DefaultDDFFileValidator());
            List<ObjectModel> objectModels =
                    ddfFileParser.parseEx(new ByteArrayInputStream(Base64.getDecoder().decode(resource.getValue())), resource.getTextSearch());
            if (objectModels.size() == 0) {
                return null;
            } else {
                ObjectModel obj = objectModels.get(0);
                LwM2mObject lwM2mObject = new LwM2mObject();
                lwM2mObject.setId(obj.id);
                lwM2mObject.setKeyId(resource.getResourceId());
                lwM2mObject.setName(obj.name);
                lwM2mObject.setMultiple(obj.multiple);
                lwM2mObject.setMandatory(obj.mandatory);
                LwM2mInstance instance = new LwM2mInstance();
                instance.setId(0);
                List<LwM2mResource> resources = new ArrayList<>();
                obj.resources.forEach((k, v) -> {
                    if (!v.operations.isExecutable()) {
                        LwM2mResource lwM2mResource = new LwM2mResource(k, v.name, false, false, false);
                        resources.add(lwM2mResource);
                    }
                });
                instance.setResources(resources.stream().toArray(LwM2mResource[]::new));
                lwM2mObject.setInstances(new LwM2mInstance[]{instance});
                return lwM2mObject;
            }
        } catch (IOException | InvalidDDFFileException e) {
            log.error("Could not parse the XML of objectModel with name [{}]", resource.getTextSearch(), e);
            return null;
        }
    }

    private List<LwM2mObject> sortList (List<LwM2mObject> lwM2mObjects, String sortProperty, String sortOrder) {
        switch (sortProperty) {
            case "name":
                switch (sortOrder) {
                    case "ASC":
                        lwM2mObjects.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
                        break;
                    case "DESC":
                        lwM2mObjects.stream().sorted(Comparator.comparing(LwM2mObject::getName).reversed());
                        break;
                }
            case "id":
                switch (sortOrder) {
                    case "ASC":
                        lwM2mObjects.sort((o1, o2) -> Long.compare(o1.getId(), o2.getId()));
                        break;
                    case "DESC":
                        lwM2mObjects.sort((o1, o2) -> Long.compare(o2.getId(), o1.getId()));
                }
        }
        return lwM2mObjects;
    }

}
