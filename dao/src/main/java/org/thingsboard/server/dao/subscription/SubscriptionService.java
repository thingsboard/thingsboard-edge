// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;

public interface SubscriptionService {

    // Edge specific
    boolean init(TenantId tenantId, EdgeId edgeId, String licenseSecret, String cloudEndpoint);

    void createDeviceAllowed(TenantId tenantId) throws SubscriptionException;

    void createAssetAllowed(TenantId tenantId) throws SubscriptionException;

    void createEdgeAllowed(TenantId tenantId) throws SubscriptionException;

    boolean isCreateEdgeAllowed(TenantId tenantId);

    void createAgentAllowed(TenantId tenantId) throws SubscriptionException;

    void whiteLabelingAllowed(TenantId tenantId) throws SubscriptionException;

    boolean whiteLabelingEnabled(TenantId tenantId) throws SubscriptionException;

    boolean edgeEnabled(TenantId tenantId) throws SubscriptionException;

    boolean trendzEnabled(TenantId tenantId) throws SubscriptionException;

    /**
     * Whether the licence grants this platform feature; a licence that says nothing about it grants it, see
     * {@link PlatformFeature}. Deployment-wide: {@code tenantId} is not consulted.
     */
    boolean isFeatureEnabled(TenantId tenantId, PlatformFeature feature);

    /** Throws unless {@link #isFeatureEnabled(TenantId, PlatformFeature)} grants the feature. */
    void checkFeatureAllowed(TenantId tenantId, PlatformFeature feature) throws SubscriptionException;

    /** Whether this deployment runs in development (non-production) mode; {@code tenantId} is not consulted. */
    boolean isDevelopment(TenantId tenantId) throws SubscriptionException;

    Long maxDevices();

    Long maxAssets();

    boolean solutionTemplateLevelAllowed(TenantId tenantId, String solutionTemplateLevel) throws SubscriptionException;

    LicenseInfo getLicenseInfo();

    int getLicenseVersion();

    SubscriptionInfo getSubscriptionInfo();

    SubscriptionInfo refreshLicense();

    void setLicenseVersion(Integer version);

    /**
     * Re-reads the cluster's licence state from {@code tb_cluster} and brings this node onto it, whichever way
     * it changed: a secret replaced, a secret removed, or a locked or keyless node picking up one the cluster
     * now holds. Bypasses the per-secret and per-node throttles, which exist to cap the unauthenticated request
     * path and would otherwise delay an operator-initiated change by up to five minutes.
     * <p>
     * A reader only. It never writes {@code tb_cluster} and never consults the environment secret: the node
     * that made the change is the writer, and resolving the environment here would let a peer answer a cleared
     * licence by re-seeding it for the whole cluster.
     */
    void reconcileLicenseState();

    default String getAiToken() {
        return null;
    }

    /**
     * Whether the licence this instance runs on is one the additional device- and asset-count enforcement applies to.
     * False whenever there is no answer, so losing the licence can never turn the enforcement on.
     */
    default boolean isCommunityGrantLicense() {
        return false;
    }

}
