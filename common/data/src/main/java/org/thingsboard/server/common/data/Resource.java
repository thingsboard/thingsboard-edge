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
package org.thingsboard.server.common.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mInstance;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.LwM2mResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Data
public class Resource implements HasTenantId, Serializable {

    private static final long serialVersionUID = 7379609705527272306L;

    private TenantId tenantId;
    private ResourceType resourceType;
    private String resourceId;
    private String textSearch;
    private String value;

    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder("Resource{");
        res.append("tenantId=").append(tenantId);
        res.append(", resourceType='").append(resourceType).append('\'');
        res.append(", resourceId='").append(resourceId).append('\'');
        res.append(", textSearch='").append(textSearch).append('\'');
        res.append('}');
        return res.toString();
    }

    public LwM2mObject toLwM2mObject () {
            try {
                DDFFileParser ddfFileParser = new DDFFileParser(new DefaultDDFFileValidator());
                List<ObjectModel> objectModels = ddfFileParser.parseEx(new ByteArrayInputStream(Base64.getDecoder().decode(this.value)), this.textSearch);
                if (objectModels.size() == 0) {
                    return null;
                }
                else {
                    ObjectModel obj = objectModels.get(0);
                    LwM2mObject lwM2mObject = new LwM2mObject();
                    lwM2mObject.setId(obj.id);
                    lwM2mObject.setKeyId(this.resourceId);
                    lwM2mObject.setName(obj.name);
                    lwM2mObject.setMultiple(obj.multiple);
                    lwM2mObject.setMandatory(obj.mandatory);
                    LwM2mInstance instance = new LwM2mInstance();
                    instance.setId(0);
                    List<LwM2mResource> resources = new ArrayList<>();
                    obj.resources.forEach((k, v) -> {
                        if (!v.operations.isExecutable()) {
                            LwM2mResource resource = new LwM2mResource(k, v.name, false, false, false);
                            resources.add(resource);
                        }
                    });
                    instance.setResources(resources.stream().toArray(LwM2mResource[]::new));
                    lwM2mObject.setInstances(new LwM2mInstance[]{instance});
                   return lwM2mObject;
                }
            } catch (IOException | InvalidDDFFileException e) {
                log.error("Could not parse the XML of objectModel with name [{}]", this.textSearch, e);
                return  null;
            }
    }
}
