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
package org.thingsboard.server.common.data.lwm2m;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.stream.Stream;

@ApiModel
@Data
@AllArgsConstructor
public class LwM2mResourceObserve {
    @ApiModelProperty(position = 1, value = "LwM2M Resource Observe id.", example = "0")
    int id;
    @ApiModelProperty(position = 2, value = "LwM2M Resource Observe name.", example = "Data")
    String name;
    @ApiModelProperty(position = 3, value = "LwM2M Resource Observe observe.", example = "false")
    boolean observe;
    @ApiModelProperty(position = 4, value = "LwM2M Resource Observe attribute.", example = "false")
    boolean attribute;
    @ApiModelProperty(position = 5, value = "LwM2M Resource Observe telemetry.", example = "false")
    boolean telemetry;
    @ApiModelProperty(position = 6, value = "LwM2M Resource Observe key name.", example = "data")
    String keyName;

    public LwM2mResourceObserve(int id, String name, boolean observe, boolean attribute, boolean telemetry) {
        this.id = id;
        this.name = name;
        this.observe = observe;
        this.attribute = attribute;
        this.telemetry = telemetry;
        this.keyName = getCamelCase (this.name);
    }

    private String getCamelCase (String name) {
        name = name.replaceAll("-", " ");
        name = name.replaceAll("_", " ");
        String [] nameCamel1 = name.split(" ");
        String [] nameCamel2 = new String[nameCamel1.length];
        int[] idx = { 0 };
        Stream.of(nameCamel1).forEach((s -> {
            nameCamel2[idx[0]] = toProperCase(idx[0]++,  s);
        }));
        return String.join("", nameCamel2);
    }

    private String toProperCase(int idx, String s) {
        if (!s.isEmpty() && s.length()> 0) {
            String s1 = (idx == 0) ? s.substring(0, 1).toLowerCase() : s.substring(0, 1).toUpperCase();
            String s2 = "";
            if (s.length()> 1) s2 = s.substring(1).toLowerCase();
            s = s1 + s2;
        }
        return s;
    }
}
