// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.client.InstanceData;
import org.thingsboard.license.client.OfflineTbLicenseClient;
import org.thingsboard.license.client.TbLicenseClient;
import org.thingsboard.license.client.TbLicenseClientListener;
import org.thingsboard.license.client.TbLicenseCtx;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Version;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.subscription.SubscriptionEntry;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.tenant.TenantService;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Service
@Slf4j
@Profile("!install & !test")
public class BasicSubscriptionService implements SubscriptionService, TbLicenseClientListener {

    public static final String OFFLINE_KEY = "OFFLINE_KEY";
    public static final String OFFLINE_CLOUD_ENDPOINT = "OFFLINE_CLOUD_ENDPOINT";

    private static final int INSTANCE_LICENSE_CHECK_PERIOD_SECONDS = (int) TimeUnit.HOURS.toSeconds(4);

    private static final String MAX_DEVICES_KEY = "maxdevices";
    private static final String MAX_ASSETS_KEY = "maxassets";
    private static final String WHITELABELING_KEY = "whitelabeling";

    private static final int DEFAULT_LICENSE_VERSION = 2;

    @Value("${edge_license.instance_data_file:instance-edge-license.data}")
    private String edgeInstanceDataFilePath;

    private static final Map<String, Predicate<String>> solutionTemplateLevelFilters = new HashMap<>();

    private static final Set<String> makerSolutionTemplateLevelPlans = Set.of("Maker", "Prototype", "Pilot", "Startup", "Business", "Enterprise", "Perpetual");
    private static final Set<String> prototypeSolutionTemplateLevelPlans = Set.of("Prototype", "Pilot", "Startup", "Business", "Enterprise", "Perpetual");
    private static final Set<String> startUpSolutionTemplateLevelPlans = Set.of("Pilot", "Startup", "Business", "Enterprise", "Perpetual");

    static {
        solutionTemplateLevelFilters.put("MAKER", planName -> makerSolutionTemplateLevelPlans.stream().anyMatch(planName::contains));
        solutionTemplateLevelFilters.put("PROTOTYPE", planName -> prototypeSolutionTemplateLevelPlans.stream().anyMatch(planName::contains));
        solutionTemplateLevelFilters.put("STARTUP", planName -> startUpSolutionTemplateLevelPlans.stream().anyMatch(planName::contains));
    }

    @Value("${zk.enabled:false}")
    private boolean zkEnabled;

    private static final String PLAN_KEY = "plan";

    @Autowired
    protected TenantService tenantService;

    @Autowired
    @Lazy
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected EdgeService edgeService;


    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    @Lazy
    private TbLicenseCtx licenseCtx;

    private AbstractTbLicenseClient tbLicenseClient;
    private Integer licenseVersion = DEFAULT_LICENSE_VERSION;
    private boolean isOfflineLicense = false;

    private String currentLicenseSecret;
    private String currentCloudEndpoint;

    private ScheduledExecutorService executor;

    public boolean init(TenantId tenantId, EdgeId edgeId, String licenseSecret, String cloudEndpoint) {
        if (licenseVersion < 2) {
            if (StringUtils.isEmpty(licenseSecret) || StringUtils.isEmpty(cloudEndpoint)) {
                log.error("License secret or cloud endpoint is not provided!");
                doExit(-1, LicenseErrorCode.GENERAL_ERROR, false);
                return false;
            } else {
                if (licenseSecret.equals(currentLicenseSecret) && cloudEndpoint.equals(currentCloudEndpoint)) {
                    log.trace("License client already activated with provided secret and cloud endpoint.");
                    return true;
                }
                int offlineTimeoutInDays = 7;
                if (OFFLINE_KEY.equals(licenseSecret)) {
                    licenseSecret = fetchLicenseSecretFromFile();
                    if (licenseSecret == null) {
                        log.error("Unable to initialize Edge in offline mode: License secret from file is inaccessible.");
                        doExit(-1, LicenseErrorCode.GENERAL_ERROR, false);
                        return false;
                    }
                    offlineTimeoutInDays = 30;
                }
                try {
                    init(licenseSecret, cloudEndpoint, offlineTimeoutInDays);
                } catch (Exception e) {
                    logGeneralError(e);
                    return false;
                }
            }
            this.currentLicenseSecret = licenseSecret;
            this.currentCloudEndpoint = cloudEndpoint;
            if (!whiteLabelingEnabled(new TenantId(EntityId.NULL_UUID))) {
                createEdgeEvaluationAttributeIfNotExits(tenantId, edgeId);
                this.executor = Executors.newSingleThreadScheduledExecutor();
                this.executor.scheduleAtFixedRate(() -> checkEvaluationLicenseTask(tenantId, edgeId), 10, INSTANCE_LICENSE_CHECK_PERIOD_SECONDS, TimeUnit.SECONDS);
            }
        }
        return true;
    }

    private String fetchLicenseSecretFromFile() {
        Path path = FileSystems.getDefault().getPath(this.edgeInstanceDataFilePath);
        if (Files.exists(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
            try {
                byte[] encoded = Files.readAllBytes(path);
                byte[] data = Base64.getDecoder().decode(new String(encoded, StandardCharsets.UTF_8));
                InstanceData instanceData = (new ObjectMapper()).readValue(data, InstanceData.class);
                return instanceData.getLicenseSecret();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void init(String licenseSecret, String cloudEndpoint, int offlineTimeoutInDays) throws Exception {
        stop();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String projectBuildDate = Version.PROJECT_BUILD_DATE;
        log.info("ThingsBoard Edge {} release date: {}", Version.PROJECT_VERSION, projectBuildDate);
        long releaseDate = sdf.parse(projectBuildDate).getTime();
        try {
            tbLicenseClient = OfflineTbLicenseClient.builder()
                    .listener(this)
                    .releaseDate(releaseDate)
                    .encodedLicenseData(licenseSecret)
                    .tbLicenseCtx(licenseCtx)
                    .checkInstanceRequired(zkEnabled) //No need to check instance registry if zk disabled
                    .build();
            this.isOfflineLicense = true;
        } catch (Exception e) {}
        if (tbLicenseClient == null) {
            tbLicenseClient = TbLicenseClient.builder()
                    .listener(this)
                    .licenseServerEndpoint(cloudEndpoint)
                    .licenseSecret(licenseSecret)
                    .licenseDataFilePath(this.edgeInstanceDataFilePath)
                    .releaseDate(releaseDate)
                    .offlineTimeoutMs(TimeUnit.DAYS.toMillis(offlineTimeoutInDays))
                    .instanceLicenseCheckPeriodSeconds(INSTANCE_LICENSE_CHECK_PERIOD_SECONDS)
                    .instanceStatsReportPeriodSeconds(Integer.MAX_VALUE)
                    .build();
        }
        tbLicenseClient.init();
    }

    private void logGeneralError(Exception e) {
        log.error("Failed to init license client", e);
        LicenseErrorCode licenseErrorCode = e instanceof LicenseException ?
                ((LicenseException) e).getErrorCode() : LicenseErrorCode.GENERAL_ERROR;
        doExit(-1, licenseErrorCode, false);
    }

    @PreDestroy
    public void stop() {
        if (this.tbLicenseClient != null) {
            this.tbLicenseClient.stop();
        }
        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
    }

    @Override
    public void setLicenseVersion(Integer licenseVersion) {
        this.licenseVersion = licenseVersion;
    }

    @Override
    public void onError(LicenseException e) {
        log.error("License Error occurred: {}({}) - {}", e.getErrorCode(),
                e.getErrorCode().getErrorCode(), e.getMessage());

        // TODO: voba - remove this after refactoring of license client
        if (LicenseErrorCode.INVALID_LICENSE_CHECK_REQUEST.equals(e.getErrorCode())
                || LicenseErrorCode.INVALID_LICENSE_CHECK_SECRET.equals(e.getErrorCode())) {
            log.error("Ignoring INVALID_LICENSE_CHECK_REQUEST/INVALID_LICENSE_CHECK_SECRET errors at the moment");
            return;
        }

        if (e.isCritical()) {
            doExit(-1, e.getErrorCode(), true);
        }
    }

    @Override
    public LicenseInfo getLicenseInfo() {
        LicenseInfo licenseInfo = new LicenseInfo();
        long maxDevices = 0;
        if (licenseVersion < 2) {
            maxDevices = this.isUnlimited(MAX_DEVICES_KEY) ? 0 : tbLicenseClient.getPlanLongValue(MAX_DEVICES_KEY);
        }
        licenseInfo.setMaxDevices(maxDevices);
        long maxAssets = 0;
        if (licenseVersion < 2) {
            maxAssets = this.isUnlimited(MAX_ASSETS_KEY) ? 0 : tbLicenseClient.getPlanLongValue(MAX_ASSETS_KEY);
        }
        licenseInfo.setMaxAssets(maxAssets);

        //long maxEdges = this.isUnlimited(PlanDataConstants.MAX_EDGES_KEY) ? 0 : tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_EDGES_KEY);
        //licenseInfo.setMaxEdges(maxEdges);

        if (licenseVersion < 2) {
            licenseInfo.setWhiteLabelingEnabled(tbLicenseClient.getPlanBooleanValue(WHITELABELING_KEY));
        } else {
            licenseInfo.setWhiteLabelingEnabled(true);
        }

        /*
        try {
            licenseInfo.setDevelopment(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.DEVELOPMENT_KEY));
        } catch (Exception e) {
            licenseInfo.setDevelopment(false);
        }
         */
        try {
            licenseInfo.setPlan(tbLicenseClient.getPlanStringValue(PLAN_KEY));
        } catch (Exception e) {
            licenseInfo.setPlan("Unknown");
        }
        return licenseInfo;
    }

    @Override
    public int getLicenseVersion() {
        return licenseVersion;
    }

    @Override
    public SubscriptionInfo getSubscriptionInfo() {
        return new SubscriptionInfo();
    }

    @Override
    public SubscriptionInfo refreshLicense() {
        return this.getSubscriptionInfo();
    }

    @Override
    public String getAiToken() {
        // TODO: NM - return null for now. Move to license client version to 2.0.* and then uncomment
        return null;
//        if (tbLicenseClient == null) {
//            return null;
//        }
//        return tbLicenseClient.getAiToken();
    }

    private void doExit(int exitCode, LicenseErrorCode licenseErrorCode, boolean gracefullShutdown) {
        new Thread(() -> {
            log.info("Terminating application due to critical License Error {}({}), exit code [{}]...",
                    licenseErrorCode, licenseErrorCode.getErrorCode(), exitCode);
            int appExitCode = exitCode;
            try {
                if (gracefullShutdown) {
                    appExitCode = SpringApplication.exit(context, () -> exitCode);
                }
            } finally {
                System.exit(appExitCode);
            }
        }, "Shutdown Thread").start();
    }

    private boolean limitReached(long actual, String key) {
        long limit = this.tbLicenseClient.getPlanLongValue(key);
        if (limit > 0) {
            return actual >= limit;
        } else {
            return licenseVersion > 1;
        }
    }

    private boolean isUnlimited(String key) {
        return this.licenseVersion < 2 && this.tbLicenseClient.getPlanLongValue(key) <= 0;
    }

    @Override
    public void createDeviceAllowed(TenantId tenantId) throws SubscriptionException {
        if (licenseVersion < 2) {
            if (isUnlimited(MAX_DEVICES_KEY)) {
                return;
            }
            long actualCount = countDevices();
            if (limitReached(actualCount, MAX_DEVICES_KEY)) {
                log.error("Maximum allowed devices limit reached!");
                throw new SubscriptionException("Maximum allowed devices limit reached!",
                        SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.DEVICE_COUNT, this.tbLicenseClient.getPlanLongValue(MAX_DEVICES_KEY));
            }
        }
    }

    @Override
    public void createAssetAllowed(TenantId tenantId) throws SubscriptionException {
        if (licenseVersion < 2) {
            if (isUnlimited(MAX_ASSETS_KEY)) {
                return;
            }
            long actualCount = countAssets();
            if (limitReached(actualCount, MAX_ASSETS_KEY)) {
                log.error("Maximum allowed assets limit reached!");
                throw new SubscriptionException("Maximum allowed assets limit reached!",
                        SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.ASSET_COUNT, this.tbLicenseClient.getPlanLongValue(MAX_ASSETS_KEY));
            }
        }
    }

    @Override
    public void createEdgeAllowed(TenantId tenantId) throws SubscriptionException {
        if (!isCreateEdgeAllowed(tenantId)) {
            log.error("Maximum allowed edges limit reached!");
            throw new SubscriptionException("Maximum allowed edges limit reached!",
                    SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.EDGE_COUNT, 0);
        }
    }

    @Override
    public boolean isCreateEdgeAllowed(TenantId tenantId) {
        return false;
    }

    @Override
    public void whiteLabelingAllowed(TenantId tenantId) throws SubscriptionException {
        if (licenseVersion < 2) {
            if (!this.tbLicenseClient.getPlanBooleanValue(WHITELABELING_KEY)) {
                throw new SubscriptionException("White Labeling feature is disabled!",
                        SubscriptionErrorCode.FEATURE_DISABLED, SubscriptionEntry.WHITE_LABELING, 0);
            }
        }
    }

    @Override
    public boolean whiteLabelingEnabled(TenantId tenantId) throws SubscriptionException {
        if (licenseVersion < 2) {
            return this.tbLicenseClient.getPlanBooleanValue(WHITELABELING_KEY);
        } else {
            return true;
        }
    }

    @Override
    public boolean edgeEnabled(TenantId tenantId) throws SubscriptionException {
        return false;
    }

    @Override
    public boolean trendzEnabled(TenantId tenantId) throws SubscriptionException {
        return false;
    }

    @Override
    public boolean isDevelopment(TenantId tenantId) throws SubscriptionException {
        return false;
    }

    @Override
    public boolean solutionTemplateLevelAllowed(TenantId tenantId, String solutionTemplateLevel) throws SubscriptionException {
        if (licenseVersion < 2) {
            String planName = this.tbLicenseClient.getPlanStringValue(PLAN_KEY);
            return solutionTemplateLevelFilters.get(solutionTemplateLevel).test(planName);
        }
        return true; // todo: check via planName from the cloud response message?
    }

    private long countDevices() {
        return deviceService.countDevices();
    }

    private long countAssets() {
        return assetService.countAssets();
    }

    private long countEdges() {
        return edgeService.countEdges();
    }

    @Override
    public Long maxDevices() {
        if (licenseVersion < 2) {
            try {
                return this.tbLicenseClient.getPlanLongValue(MAX_DEVICES_KEY);
            } catch (Exception e) {
                log.error("Can't get max devices count");
                return null;
            }
        } else {
            return 0L;
        }
    }

    @Override
    public Long maxAssets() {
        if (licenseVersion < 2) {
            try {
                return this.tbLicenseClient.getPlanLongValue(MAX_ASSETS_KEY);
            } catch (Exception e) {
                log.error("Can't get max assets count");
                return null;
            }
        } else {
            return 0L;
        }
    }

    private void checkEvaluationLicenseTask(TenantId tenantId, EdgeId edgeId) {
        if (whiteLabelingEnabled(new TenantId(EntityId.NULL_UUID))) {
            return;
        }
        try {
            log.info("Checking evaluation license period...");
            ListenableFuture<Optional<AttributeKvEntry>> future =
                    attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, edgeId.toString());
            Optional<AttributeKvEntry> evaluationTsOpt = future.get();
            if (evaluationTsOpt.isEmpty() || evaluationTsOpt.get().getStrValue().isEmpty()) {
                log.error("Evaluation attribute is not available, stopping edge service...");
                doExit(-1, LicenseErrorCode.GENERAL_ERROR, true);
                return;
            }
            Instant now = Instant.now();
            long evaluationTs = evaluationTsOpt.get().getLastUpdateTs();
            Instant createdTime = Instant.ofEpochMilli(evaluationTs);
            long cntOfEvaluationDays = 30;
            if (createdTime.plus(cntOfEvaluationDays, ChronoUnit.DAYS).isBefore(now)) {
                log.error("Evaluation license period for this edge expired. Stopping edge service. " +
                        "Please obtain your license and update license key of edge entity on the cloud.");
                doExit(-1, LicenseErrorCode.GENERAL_ERROR, true);
            }
        } catch (Exception e) {
            log.error("Evaluation license period check can not be performed. Stopping edge service...", e);
            doExit(-1, LicenseErrorCode.GENERAL_ERROR, true);
        }
    }

    private void createEdgeEvaluationAttributeIfNotExits(TenantId tenantId, EdgeId edgeId) {
        try {
            ListenableFuture<Optional<AttributeKvEntry>> future =
                    attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, edgeId.toString());
            Optional<AttributeKvEntry> evaluationTsOpt = future.get();
            if (evaluationTsOpt.isEmpty()) {
                long now = System.currentTimeMillis();
                BaseAttributeKvEntry evaluationStartTsAttr = new BaseAttributeKvEntry(new StringDataEntry(edgeId.toString(), ""), now);
                attributesService.save(tenantId, edgeId, AttributeScope.SERVER_SCOPE, evaluationStartTsAttr);
            }
        } catch (Exception e) {
            log.error("Can not save evaluation attribute. Stopping edge service...", e);
            doExit(-1, LicenseErrorCode.GENERAL_ERROR, true);
        }
    }

    @Override
    public void createAgentAllowed(TenantId tenantId) throws SubscriptionException {
    }

    @Override
    public boolean isFeatureEnabled(TenantId tenantId, PlatformFeature feature) {
        return true;
    }

    @Override
    public void checkFeatureAllowed(TenantId tenantId, PlatformFeature feature) throws SubscriptionException {
    }

    @Override
    public void reconcileLicenseState() {
    }
}
