/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
