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
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;

import java.io.Serializable;

@Data
public class ResourceValue implements Serializable {

    private static final long serialVersionUID = -228268906779089402L;

    private TbLwM2MResource lwM2mResource;
    private TbResourceModel resourceModel;

    public ResourceValue(LwM2mResource lwM2mResource, ResourceModel resourceModel) {
        this.lwM2mResource = toTbLwM2MResource(lwM2mResource);
        this.resourceModel = toTbResourceModel(resourceModel);
    }

    public void setLwM2mResource(LwM2mResource lwM2mResource) {
        this.lwM2mResource = toTbLwM2MResource(lwM2mResource);
    }

    public void setResourceModel(ResourceModel resourceModel) {
        this.resourceModel = toTbResourceModel(resourceModel);
    }

    private static TbLwM2MResource toTbLwM2MResource(LwM2mResource lwM2mResource) {
        if (lwM2mResource.isMultiInstances()) {
            TbLwM2MResourceInstance[] instances = (TbLwM2MResourceInstance[]) lwM2mResource.getInstances().values().stream().map(ResourceValue::toTbLwM2MResourceInstance).toArray();
            return new TbLwM2MMultipleResource(lwM2mResource.getId(), lwM2mResource.getType(), instances);
        } else {
            return new TbLwM2MSingleResource(lwM2mResource.getId(), lwM2mResource.getValue(), lwM2mResource.getType());
        }
    }

    private static TbLwM2MResourceInstance toTbLwM2MResourceInstance(LwM2mResourceInstance instance) {
        return new TbLwM2MResourceInstance(instance.getId(), instance.getValue(), instance.getType());
    }

    private static TbResourceModel toTbResourceModel(ResourceModel resourceModel) {
        return new TbResourceModel(resourceModel.id, resourceModel.name, resourceModel.operations, resourceModel.multiple,
                resourceModel.mandatory, resourceModel.type, resourceModel.rangeEnumeration, resourceModel.units, resourceModel.description);
    }
}
