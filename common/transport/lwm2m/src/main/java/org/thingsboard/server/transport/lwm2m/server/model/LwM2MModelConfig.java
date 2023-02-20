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
package org.thingsboard.server.transport.lwm2m.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.common.util.CollectionsUtil.diffSets;

@Data
@NoArgsConstructor
@ToString(exclude = "toCancelRead")
@Slf4j
public class LwM2MModelConfig {
    private String endpoint;
    private Map<String, ObjectAttributes> attributesToAdd;
    private Set<String> attributesToRemove;
    private Set<String> toObserve;
    private Set<String> toCancelObserve;
    private Set<String> toRead;
    @JsonIgnore
    private Set<String> toCancelRead;

    public LwM2MModelConfig(String endpoint) {
        this.endpoint = endpoint;
        this.attributesToAdd = new ConcurrentHashMap<>();
        this.attributesToRemove = ConcurrentHashMap.newKeySet();
        this.toObserve = ConcurrentHashMap.newKeySet();
        this.toCancelObserve = ConcurrentHashMap.newKeySet();
        this.toRead = ConcurrentHashMap.newKeySet();
        this.toCancelRead = new HashSet<>();
    }

    public void merge(LwM2MModelConfig modelConfig) {
        if (modelConfig.isEmpty() && modelConfig.getToCancelRead().isEmpty()) {
            return;
        }

        modelConfig.getAttributesToAdd().forEach((k, v) -> {
            if (this.attributesToRemove.contains(k)) {
                this.attributesToRemove.remove(k);
            } else {
                this.attributesToAdd.put(k, v);
            }
        });

        modelConfig.getAttributesToRemove().forEach(k -> {
            if (this.attributesToAdd.containsKey(k)) {
                this.attributesToRemove.remove(k);
            } else {
                this.attributesToRemove.add(k);
            }
        });

        this.toObserve.addAll(diffSets(this.toCancelObserve, modelConfig.getToObserve()));
        this.toCancelObserve.addAll(diffSets(this.toObserve, modelConfig.getToCancelObserve()));

        this.toObserve.removeAll(modelConfig.getToCancelObserve());
        this.toCancelObserve.removeAll(modelConfig.getToObserve());

        this.toRead.removeAll(modelConfig.getToObserve());
        this.toRead.removeAll(modelConfig.getToCancelRead());
        this.toRead.addAll(modelConfig.getToRead());
    }

    @JsonIgnore
    public boolean isEmpty() {
        return attributesToAdd.isEmpty() && toObserve.isEmpty() && toCancelObserve.isEmpty() && toRead.isEmpty();
    }
}
