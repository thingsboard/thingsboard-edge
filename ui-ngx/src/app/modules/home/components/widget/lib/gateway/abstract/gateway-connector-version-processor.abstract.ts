///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import { GatewayConnector, GatewayVersion } from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isString } from '@core/utils';

export abstract class GatewayConnectorVersionProcessor<BasicConfig> {
  gatewayVersion: number;
  configVersion: number;

  protected constructor(protected gatewayVersionStr: string, protected connector: GatewayConnector<BasicConfig>) {
    this.gatewayVersion = this.parseVersion(this.gatewayVersionStr);
    this.configVersion = this.parseVersion(connector.configVersion);
  }

  getProcessedByVersion(): GatewayConnector<BasicConfig> {
    if (this.isVersionUpdateNeeded()) {
      return this.isVersionUpgradeNeeded()
        ? this.getUpgradedVersion()
        : this.getDowngradedVersion();
    }

    return this.connector;
  }

  private isVersionUpdateNeeded(): boolean {
    if (!this.gatewayVersion) {
      return false;
    }

    return this.configVersion !== this.gatewayVersion;
  }

  private isVersionUpgradeNeeded(): boolean {
    return this.gatewayVersionStr === GatewayVersion.Current && (!this.configVersion || this.configVersion < this.gatewayVersion);
  }

  private parseVersion(version: string): number {
    return isString(version) ? Number(version.replace(/\./g, '')) : 0;
  }

  protected abstract getDowngradedVersion(): GatewayConnector<BasicConfig>;
  protected abstract getUpgradedVersion(): GatewayConnector<BasicConfig>;
}
