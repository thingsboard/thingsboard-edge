// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.lts;

import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class V4_2_2_4Migration implements Migration {

    @Override
    public String getVersion() {
        return "4.2.2.4";
    }

}
