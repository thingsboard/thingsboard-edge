package org.thingsboard.server.common.data.permission;

import lombok.Data;
import org.thingsboard.server.common.data.role.Role;

@Data
public class GroupPermissionInfo extends GroupPermission {

    private static final long serialVersionUID = 2807343092519543363L;

    private Role role;
    private String entityGroupName;
    private boolean isReadOnly;

    public GroupPermissionInfo() {
        super();
    }

    public GroupPermissionInfo(GroupPermission groupPermission) {
        super(groupPermission);
    }

}
