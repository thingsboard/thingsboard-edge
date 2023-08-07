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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { Converter, ConverterDebugInput, ConverterType } from '@shared/models/converter.models';
import { DebugConverterEventBody } from '@shared/models/event.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-converter-tabs',
  templateUrl: './converter-tabs.component.html',
  styleUrls: []
})
export class ConverterTabsComponent extends EntityTabsComponent<Converter> {

  constructor(protected store: Store<AppState>,
              private translate: TranslateService) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  getConverterFunctionName() {
    return this.translate.instant(this.entity.type === ConverterType.UPLINK ?
      'converter.test-decoder-fuction' : 'converter.test-encoder-fuction')
  }

  onDebugEventSelected(event: DebugConverterEventBody) {
    let metadata = '';
    let msgContent = '';
    let msgType = '';
    let inIntegrationMetadata = '';
    if (this.entity.type === ConverterType.DOWNLINK) {
      const msg = JSON.parse(event.in)[0];
      if (msg?.metadata) {
        metadata = JSON.stringify(msg.metadata);
      }
      if (msg?.msg) {
        msgContent = JSON.stringify(msg.msg);
      }
      if (msg?.msgType) {
        msgType = msg.msgType;
      }
      inIntegrationMetadata = event.metadata;
    } else {
      msgContent = event.in;
      metadata = event.metadata;
      msgType = event.inMessageType;
    }
    const debugIn: ConverterDebugInput = {
      inContentType: event.inMessageType,
      inContent: msgContent,
      inMetadata: metadata,
      inMsgType: msgType,
      inIntegrationMetadata: inIntegrationMetadata
    };
    const convertersTable = this.entitiesTableConfig.getTable();
    const converterComponent = convertersTable ? convertersTable.entityDetailsPanel.entityComponent :
      this.entitiesTableConfig.getEntityDetailsPage().entityComponent;
    (converterComponent as ConverterComponent).showConverterTestDialog(debugIn, true);
  }

}
