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
package org.thingsboard.integration.opcua;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Valerii Sosliuk on 4/24/2018.
 */
@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceMapping {

    public static final Pattern TAG_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");

    private final DeviceMappingType mappingType;
    private final String deviceNodePattern;
    private Integer namespace;
    private final List<SubscriptionTag> subscriptionTags;

    @JsonIgnore
    private List<Pattern> mappingPathPatterns;

    void  initMappingPatterns() {
        try {
            if (mappingType == DeviceMappingType.FQN) {
                List<String> splitted = getSplittedRegex(deviceNodePattern);
                mappingPathPatterns = splitted.stream().map(Pattern::compile).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public Set<String> getAllTags() {
        Set<String> tags = new HashSet<>();
        subscriptionTags.forEach(subscriptionTag -> tags.add(subscriptionTag.getPath()));
        return tags;
    }

    private List<String> getSplittedRegex(String pattern) {
        List<String> splitted = new ArrayList<>();
        int startIdx = 0;
        int idx = pattern.indexOf("\\.");
        while (idx != -1) {
            splitted.add(pattern.substring(startIdx, idx));
            startIdx = idx + 2;
            idx = pattern.indexOf("\\.", startIdx);
        }
        splitted.add(pattern.substring(startIdx));
        return splitted;
    }
}
