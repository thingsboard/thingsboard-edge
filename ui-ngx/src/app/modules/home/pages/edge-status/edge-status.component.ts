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

import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EdgeService } from '@core/http/edge.service';
import { EdgeSettings } from '@shared/models/edge.models';
import { AttributeService } from '@core/http/attribute.service';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'tb-edge-status',
  templateUrl: './edge-status.component.html',
  styleUrls: ['./edge-status.component.scss']
})
export class EdgeStatusComponent extends PageComponent implements OnInit {

  isActive: boolean;
  edgeStatusGroup: FormGroup;

  constructor(public fb: FormBuilder,
              protected store: Store<AppState>,
              private edgeService: EdgeService,
              private attributeService: AttributeService,
              private datePipe: DatePipe,
              private route: ActivatedRoute) {
    super(store);
    this.buildForm();
  }

  ngOnInit(): void {
    this.init();
  }

  buildForm() {
    this.edgeStatusGroup = this.fb.group({
      name: '',
      id: '',
      type: '',
      routingKey: '',
      lastConnectTime: '',
      lastDisconnectTime: ''
    });
    this.edgeStatusGroup.disable();
  }

  private init(): void {
    this.route.data.subscribe(data => {
        const edgeAttributes = data.edgeAttributes.reduce(function (map, attribute) {
          map[attribute.key] = attribute;
          return map;
        }, {});

        const isActive: boolean = edgeAttributes.active.value;
        const lastConnectTime: number = edgeAttributes.lastConnectTime.value;
        const lastDisconnectTime: number = edgeAttributes.lastDisconnectTime?.value;
        const edgeSettings: EdgeSettings = JSON.parse(edgeAttributes.edgeSettings.value);

        this.isActive = isActive;
        this.edgeStatusGroup.setValue({
          name: edgeSettings.name,
          id: edgeSettings.edgeId,
          type: edgeSettings.type,
          routingKey: edgeSettings.routingKey,
          lastConnectTime: this.datePipe.transform(lastConnectTime, 'yyyy-MM-dd HH:mm:ss'),
          lastDisconnectTime: lastDisconnectTime ? this.datePipe.transform(lastDisconnectTime, 'yyyy-MM-dd HH:mm:ss') : 'N/A'
        });
      }
    );
  }

}
