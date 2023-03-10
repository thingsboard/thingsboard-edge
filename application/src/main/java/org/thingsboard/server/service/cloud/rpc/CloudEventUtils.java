/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud.rpc;

import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;

public final class CloudEventUtils {

    private CloudEventUtils() {
    }

    public static TimePageLink createCloudEventTimePageLink(int pageSize, Long startTs) {
        return new TimePageLink(pageSize,
                0,
                null,
                new SortOrder("createdTime", SortOrder.Direction.ASC),
                startTs,
                System.currentTimeMillis());
    }
}
