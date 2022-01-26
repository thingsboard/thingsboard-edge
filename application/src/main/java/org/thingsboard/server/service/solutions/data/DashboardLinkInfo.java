package org.thingsboard.server.service.solutions.data;

import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;

@Data
public class DashboardLinkInfo {

    private final String name;
    private final EntityGroupId entityGroupId;
    private final DashboardId dashboardId;

}
