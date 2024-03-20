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
package org.thingsboard.server.transport.lwm2m.client;

import org.eclipse.leshan.client.resource.BaseInstanceEnablerFactory;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TbObjectsInitializer extends ObjectsInitializer {


    public TbObjectsInitializer(LwM2mModel model) {
        super(model);
    }

    public List<LwM2mObjectEnabler> create(int... objectId) {
        List<LwM2mObjectEnabler> enablers = new ArrayList<>();
        for (int anObjectId : objectId) {
            LwM2mObjectEnabler objectEnabler = create(anObjectId);
            if (objectEnabler != null)
                enablers.add(objectEnabler);
        }
        return enablers;
    }

    public LwM2mObjectEnabler create(int objectId) {
        ObjectModel objectModel = model.getObjectModel(objectId);
        if (objectModel == null) {
            throw new IllegalArgumentException(
                    "Cannot create object for id " + objectId + " because no model is defined for this id.");
        }
        return createNodeEnabler(objectModel);
    }

    protected LwM2mObjectEnabler createNodeEnabler(ObjectModel objectModel) {
        Map<Integer, LwM2mInstanceEnabler> instances = new HashMap<>();
        LwM2mInstanceEnabler[] newInstances = createInstances(objectModel);
        for (LwM2mInstanceEnabler instance : newInstances) {
            // set id if not already set
            if (instance.getId() == null) {
                int id = BaseInstanceEnablerFactory.generateNewInstanceId(instances.keySet());
                instance.setId(id);
            }
            instance.setModel(objectModel);
            instances.put(instance.getId(), instance);
        }
        return new TbLwm2mObjectEnabler(objectModel.id, objectModel, instances, getFactoryFor(objectModel),
                getContentFormat(objectModel.id));
    }
}
