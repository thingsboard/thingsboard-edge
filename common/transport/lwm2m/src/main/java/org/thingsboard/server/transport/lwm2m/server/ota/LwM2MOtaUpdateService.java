/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.transport.lwm2m.server.ota;

import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Optional;

public interface LwM2MOtaUpdateService {

    void init(LwM2mClient client);

    void forceFirmwareUpdate(LwM2mClient client);

    void onTargetFirmwareUpdate(LwM2mClient client, String newFwTitle, String newFwVersion, Optional<String> newFwUrl, Optional<String> newFwTag);

    void onTargetSoftwareUpdate(LwM2mClient client, String newSwTitle, String newSwVersion, Optional<String> newSwUrl, Optional<String> newSwTag);

    void onCurrentFirmwareNameUpdate(LwM2mClient client, String name);

    void onFirmwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration);

    void onCurrentSoftwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration);

    void onCurrentFirmwareVersion3Update(LwM2mClient client, String version);

    void onCurrentFirmwareVersionUpdate(LwM2mClient client, String version);

    void onCurrentFirmwareStateUpdate(LwM2mClient client, Long state);

    void onCurrentFirmwareResultUpdate(LwM2mClient client, Long result);

    void onCurrentFirmwareDeliveryMethodUpdate(LwM2mClient lwM2MClient, Long value);

    void onCurrentSoftwareNameUpdate(LwM2mClient lwM2MClient, String name);

    void onCurrentSoftwareVersion3Update(LwM2mClient lwM2MClient, String version);

    void onCurrentSoftwareVersionUpdate(LwM2mClient client, String version);

    void onCurrentSoftwareStateUpdate(LwM2mClient lwM2MClient, Long value);

    void onCurrentSoftwareResultUpdate(LwM2mClient client, Long result);

    boolean isOtaDownloading(LwM2mClient client);
}
