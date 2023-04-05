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
