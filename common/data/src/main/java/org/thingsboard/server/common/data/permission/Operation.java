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
package org.thingsboard.server.common.data.permission;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum Operation {
    ALL(true), CREATE(true, true, true), READ(true), WRITE(true, false, true), DELETE(true, true, true), RPC_CALL(true),
    READ_CREDENTIALS(true), WRITE_CREDENTIALS(true), READ_ATTRIBUTES(true), WRITE_ATTRIBUTES(true, false, true), READ_TELEMETRY(true), WRITE_TELEMETRY(true, false, true),
    ADD_TO_GROUP, REMOVE_FROM_GROUP, CHANGE_OWNER, IMPERSONATE, CLAIM_DEVICES, SHARE_GROUP(true), ASSIGN_TO_TENANT;

    public static Set<Operation> defaultEntityOperations = new HashSet<>(Arrays.asList(ALL, READ, WRITE,
            CREATE, DELETE, READ_ATTRIBUTES, WRITE_ATTRIBUTES, READ_TELEMETRY, WRITE_TELEMETRY, CHANGE_OWNER));

    public static Set<Operation> defaultEntityGroupOperations = new HashSet<>(Arrays.asList(ALL, READ, WRITE,
            CREATE, DELETE, READ_ATTRIBUTES, WRITE_ATTRIBUTES, READ_TELEMETRY, WRITE_TELEMETRY, ADD_TO_GROUP, REMOVE_FROM_GROUP, SHARE_GROUP));

    public static Set<Operation> crudOperations = new HashSet<>(Arrays.asList(ALL, READ, WRITE,
            CREATE, DELETE));

    public static Set<Operation> allowedForGroupRoleOperations = new HashSet<>();
    static {
        for (Operation operation : Operation.values()) {
            if (operation.isAllowedForGroupRole()) {
                allowedForGroupRoleOperations.add(operation);
            }
        }
    }

    public static Set<Operation> allowedForGroupOwnerOnlyOperations = new HashSet<>();
    static {
        for (Operation operation : Operation.values()) {
            if (operation.isAllowedForGroupOwnerOnly()) {
                allowedForGroupOwnerOnlyOperations.add(operation);
            }
        }
    }

    public static Set<Operation> allowedForGroupOwnerOnlyGroupOperations = new HashSet<>();
    static {
        for (Operation operation : Operation.values()) {
            if (operation.isGroupOperationAllowedForGroupOwnerOnly()) {
                allowedForGroupOwnerOnlyGroupOperations.add(operation);
            }
        }
    }

    @Getter
    private boolean allowedForGroupRole;

    @Getter
    private boolean allowedForGroupOwnerOnly;

    @Getter
    private boolean groupOperationAllowedForGroupOwnerOnly;

    Operation() {
        this(false, false, false);
    }

    Operation(boolean allowedForGroupRole) {
        this(allowedForGroupRole, false, false);
    }

    Operation(boolean allowedForGroupRole, boolean allowedForGroupOwnerOnly) {
        this(allowedForGroupRole, allowedForGroupOwnerOnly, false);
    }

    Operation(boolean allowedForGroupRole, boolean allowedForGroupOwnerOnly, boolean groupOperationAllowedForGroupOwnerOnly) {
        this.allowedForGroupRole = allowedForGroupRole;
        this.allowedForGroupOwnerOnly = allowedForGroupOwnerOnly;
        this.groupOperationAllowedForGroupOwnerOnly = groupOperationAllowedForGroupOwnerOnly;
    }
}
