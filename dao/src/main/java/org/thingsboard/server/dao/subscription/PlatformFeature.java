// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import lombok.Getter;
import org.thingsboard.server.common.data.subscription.SubscriptionEntry;

/**
 * The professional feature set: platform features a licence may withhold, each identified by the plan-data
 * key that carries it. All three read as <b>enabled when the key is absent</b>; white labeling is not a
 * member because it uses the opposite convention and its own methods.
 */
@Getter
public enum PlatformFeature {

    INTEGRATIONS("integrations", "Integrations", SubscriptionEntry.INTEGRATIONS),
    SCHEDULER("scheduler", "Scheduler", SubscriptionEntry.SCHEDULER),
    REPORTING("reporting", "Reporting", SubscriptionEntry.REPORTING);

    private final String planDataKey;
    private final String displayName;
    private final SubscriptionEntry subscriptionEntry;

    PlatformFeature(String planDataKey, String displayName, SubscriptionEntry subscriptionEntry) {
        this.planDataKey = planDataKey;
        this.displayName = displayName;
        this.subscriptionEntry = subscriptionEntry;
    }
}
