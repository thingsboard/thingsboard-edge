// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge.stats;

import lombok.Getter;

@Getter
public enum CloudStatsKey {
    UPLINK_MSGS_ADDED("uplinkMsgsAdded"),
    UPLINK_MSGS_PUSHED("uplinkMsgsPushed"),
    UPLINK_MSGS_PERMANENTLY_FAILED("uplinkMsgsPermanentlyFailed"),
    UPLINK_MSGS_TMP_FAILED("uplinkMsgsTmpFailed"),
    UPLINK_MSGS_LAG("uplinkMsgsLag");

    private final String key;

    CloudStatsKey(String key) {
        this.key = key;
    }

}
