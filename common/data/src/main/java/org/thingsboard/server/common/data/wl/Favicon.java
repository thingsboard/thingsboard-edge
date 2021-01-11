/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.common.data.wl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

@Data
@EqualsAndHashCode
public class Favicon {

    private String url;
    private String type;

    public Favicon() {
    }

    public Favicon(String url) {
        this(url, extractTypeFromDataUrl(url));
    }

    public Favicon(String url, String type) {
        this.url = url;
        this.type = type;
    }

    private static String extractTypeFromDataUrl(String dataUrl) {
        String type = null;
        if (!StringUtils.isEmpty(dataUrl)) {
            String[] parts = dataUrl.split(";");
            if (parts != null && parts.length > 0) {
                String part = parts[0];
                String[] typeParts = part.split(":");
                if (typeParts != null && typeParts.length > 1) {
                    type = typeParts[1];
                }
            }
        }
        return type;
    }

}
