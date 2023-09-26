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
package org.thingsboard.server.service.edge.rpc.utils;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;

@Slf4j
public final class EdgeVersionUtils {

    public static boolean isEdgeProtoDeprecated(EdgeVersion edgeVersion) {
        switch (edgeVersion) {
            case V_3_3_0:
            case V_3_3_3:
            case V_3_4_0:
            case V_3_6_0:
                return true;
            case V_3_6_1:
            default:
                return false;
        }
    }
}
