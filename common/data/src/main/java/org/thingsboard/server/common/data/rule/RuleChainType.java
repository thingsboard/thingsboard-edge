// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

public enum RuleChainType {
    CORE,
    // Edge-only:  merge comment -
    // on edge EDGE type replaced by CORE to avoid updates in multiple rule chain core classes
    // EDGE type not removed for test possibility
    EDGE
}
