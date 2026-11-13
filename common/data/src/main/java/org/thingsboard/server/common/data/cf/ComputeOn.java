// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.cf;

/**
 * Defines which side of an edge deployment computes a calculated field.
 * <p>
 * {@link #DEFAULT} and {@link #isComputedHere(ComputeOn)} are deliberately inverted in the
 * thingsboard-edge fork (EDGE instead of CLOUD). Keep them as the single seam between the two
 * builds - inlining {@code isComputedHere} into its call sites, or rewriting it as
 * {@code computeOn != EDGE}, would silently break the edge with nothing here to catch it.
 */
public enum ComputeOn {

    CLOUD,
    EDGE;

    public static final ComputeOn DEFAULT = EDGE; // edge only

    public static ComputeOn orDefault(ComputeOn computeOn) {
        return computeOn == null ? DEFAULT : computeOn;
    }

    public static boolean isComputedHere(ComputeOn computeOn) {
        return orDefault(computeOn) == EDGE; // edge only
    }

}
