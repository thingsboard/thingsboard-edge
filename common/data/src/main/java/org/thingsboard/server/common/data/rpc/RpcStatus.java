/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.rpc;

import lombok.Getter;

public enum RpcStatus {

    QUEUED(true),
    SENT(true),
    DELIVERED(true),
    SUCCESSFUL(false),
    TIMEOUT(false),
    EXPIRED(false),
    FAILED(false),
    DELETED(false);

    /**
     * {@code true} while the RPC is still pending in the device actor (not yet delivered/answered).
     * Note: TIMEOUT is not intermediate, but it is not final either - the device actor may retry
     * the delivery (TIMEOUT -> SENT/DELIVERED/QUEUED) until retries are exhausted (then FAILED).
     */
    @Getter
    private final boolean intermediate;

    RpcStatus(boolean intermediate) {
        this.intermediate = intermediate;
    }

}
