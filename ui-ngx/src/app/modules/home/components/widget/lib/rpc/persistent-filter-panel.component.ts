///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, Inject, InjectionToken } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { OverlayRef } from '@angular/cdk/overlay';
import { RpcStatus, rpcStatusTranslation } from '@shared/models/rpc.models';
import { TranslateService } from '@ngx-translate/core';

export const PERSISTENT_FILTER_PANEL_DATA = new InjectionToken<any>('AlarmFilterPanelData');

export interface PersistentFilterPanelData {
  rpcStatus: RpcStatus;
}

@Component({
  selector: 'tb-persistent-filter-panel',
  templateUrl: './persistent-filter-panel.component.html',
  styleUrls: ['./persistent-filter-panel.component.scss']
})
export class PersistentFilterPanelComponent {

  public persistentFilterFormGroup: UntypedFormGroup;
  public result: PersistentFilterPanelData;
  public rpcSearchStatusTranslationMap = rpcStatusTranslation;
  public rpcSearchPlaceholder: string;

  public persistentSearchStatuses = Object.keys(RpcStatus);

  constructor(@Inject(PERSISTENT_FILTER_PANEL_DATA)
              public data: PersistentFilterPanelData,
              public overlayRef: OverlayRef,
              private fb: UntypedFormBuilder,
              private translate: TranslateService) {
    this.persistentFilterFormGroup = this.fb.group(
      {
        rpcStatus: this.data.rpcStatus
      }
    );
    this.rpcSearchPlaceholder = this.translate.instant('widgets.persistent-table.any-status');
  }

  update() {
    this.result = {
      rpcStatus: this.persistentFilterFormGroup.get('rpcStatus').value
    };
    this.overlayRef.dispose();
  }

  cancel() {
    this.overlayRef.dispose();
  }
}

