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

import { ChangeDetectionStrategy, Component, forwardRef } from '@angular/core';
import { FormGroup, NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  MappingType,
  OPCBasicConfig_v3_5_2,
  ServerConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { MappingTableComponent } from '@home/components/widget/lib/gateway/connectors-configuration/mapping-table/mapping-table.component';
import {
  SecurityConfigComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/security-config/security-config.component';
import {
  OpcServerConfigComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/opc/opc-server-config/opc-server-config.component';
import {
  GatewayConnectorBasicConfigDirective
} from '@home/components/widget/lib/gateway/abstract/gateway-connector-basic-config.abstract';

@Component({
  selector: 'tb-opc-ua-basic-config',
  templateUrl: './opc-ua-basic-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OpcUaBasicConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => OpcUaBasicConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
    MappingTableComponent,
    OpcServerConfigComponent,
  ],
  styleUrls: ['./opc-ua-basic-config.component.scss']
})
export class OpcUaBasicConfigComponent extends GatewayConnectorBasicConfigDirective<OPCBasicConfig_v3_5_2, OPCBasicConfig_v3_5_2> {

  mappingTypes = MappingType;
  isLegacy = false;

  protected override initBasicFormGroup(): FormGroup {
    return this.fb.group({
      mapping: [],
      server: [],
    });
  }

  protected override mapConfigToFormValue(config: OPCBasicConfig_v3_5_2): OPCBasicConfig_v3_5_2 {
    return {
      server: config.server ?? {} as ServerConfig,
      mapping: config.mapping ?? [],
    };
  }

  protected override getMappedValue(value: OPCBasicConfig_v3_5_2): OPCBasicConfig_v3_5_2 {
    return {
      server: value.server,
      mapping: value.mapping,
    };
  }
}
