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

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.role.Role;

@Data
public class GroupPermissionInfo extends GroupPermission {

    private static final long serialVersionUID = 2807343092519543363L;

    private Role role;

    private String entityGroupName;
    private EntityId entityGroupOwnerId;
    private String entityGroupOwnerName;

    private String userGroupName;
    private EntityId userGroupOwnerId;
    private String userGroupOwnerName;

    private boolean isReadOnly;

    public GroupPermissionInfo() {
        super();
    }

    public GroupPermissionInfo(GroupPermission groupPermission) {
        super(groupPermission);
    }

}
