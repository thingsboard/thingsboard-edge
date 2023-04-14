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
package org.thingsboard.server.common.data.objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Victor Basanets on 9/05/2017.
 */
@Data
@ApiModel
@NoArgsConstructor
public class AttributesEntityView implements Serializable {

    @ApiModelProperty(position = 1, required = true, value = "List of client-side attribute keys to expose", example = "currentConfiguration")
    private List<String> cs = new ArrayList<>();
    @ApiModelProperty(position = 3, required = true, value = "List of server-side attribute keys to expose", example = "model")
    private List<String> ss = new ArrayList<>();
    @ApiModelProperty(position = 2, required = true, value = "List of shared attribute keys to expose", example = "targetConfiguration")
    private List<String> sh = new ArrayList<>();

    public AttributesEntityView(List<String> cs,
                                List<String> ss,
                                List<String> sh) {

        this.cs = new ArrayList<>(cs);
        this.ss = new ArrayList<>(ss);
        this.sh = new ArrayList<>(sh);
    }

    public AttributesEntityView(AttributesEntityView obj) {
        this(obj.getCs(), obj.getSs(), obj.getSh());
    }
}
