///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { PageComponent } from "@shared/components/page.component";
import { FormBuilder, FormGroup } from "@angular/forms";
import { Store } from "@ngrx/store";
import { AppState } from "@core/core.state";
import { EdgeService } from "@core/http/edge.service";
import { CloudStatus } from "@shared/models/edge.models";
import { AttributeService } from "@core/http/attribute.service";
import { AttributeScope } from "@shared/models/telemetry/telemetry.models";
import { getCurrentAuthUser } from "@core/auth/auth.selectors";
import { EntityId } from "@shared/models/id/entity-id";
import { EntityType } from "@shared/models/entity-type.models";
import { DatePipe } from "@angular/common";

@Component({
  selector: 'tb-edge-status',
  templateUrl: './edge-status.component.html',
  styleUrls: ['./edge-status.component.scss']
})
export class EdgeStatusComponent extends PageComponent implements OnInit {

  edgeStatusGroup: FormGroup;
  cloudStatus: CloudStatus = {
    label: '',
    isActive: false
  }

  constructor(protected store: Store<AppState>,
              private edgeService: EdgeService,
              private attributeService: AttributeService,
              private datePipe: DatePipe,
              public fb: FormBuilder,
              private cd: ChangeDetectorRef) {
    super(store);
    this.buildEdgeStatusForm();
  }

  ngOnInit(): void {
    this.loadEdgeStatus();
  }

  buildEdgeStatusForm() {
    this.edgeStatusGroup = this.fb.group({
      id: '',
      type: '',
      routingKey: '',
      cloudType: '',
      lastConnectTime: '',
      lastDisconnectTime: ''
    });
    this.edgeStatusGroup.disable();
  }

  loadEdgeStatus() {
    const authUser = getCurrentAuthUser(this.store);
    const currentTenant: EntityId = {
      id: authUser.tenantId,
      entityType: EntityType.TENANT
    }
    this.attributeService.getEntityAttributes(currentTenant, AttributeScope.SERVER_SCOPE)
      .subscribe(attributes => {
        const edge: any = attributes.reduce(function (map, attribute) {
          map[attribute.key] = attribute;
          return map;
        }, {});
        const edgeSettings = JSON.parse(edge.edgeSettings.value);
        this.cloudStatus = {
          label: edge.active.value ? "edge.connected" : "edge.disconnected",
          isActive: edge.active.value
        }
        const lastDisconnectTime = edge.lastDisconnectTime ?
          this.datePipe.transform(edge.lastDisconnectTime?.value, 'yyyy-MM-dd HH:mm:ss') : 'N/A';
        this.edgeStatusGroup.setValue({
          id: edgeSettings.edgeId,
          type: edgeSettings.type,
          routingKey: edgeSettings.routingKey,
          cloudType: edgeSettings.cloudType,
          lastConnectTime: this.datePipe.transform(edge.lastConnectTime.value, 'yyyy-MM-dd HH:mm:ss'),
          lastDisconnectTime: lastDisconnectTime
        })

        this.cd.detectChanges();
      })
  }

}
