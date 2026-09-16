// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cloud;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
public class DefaultCloudSynchronizationManager implements CloudSynchronizationManager {

    private final ThreadLocal<Boolean> sync = new ThreadLocal<>();

    @Override
    public boolean isSync() {
        Boolean sync = this.sync.get();
        return sync != null && sync;
    }

}
