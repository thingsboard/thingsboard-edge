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

import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from "@core/core.state";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { EntityType } from "@shared/models/entity-type.models";
import { Edge } from "@shared/models/edge.models";
import { TranslateService } from "@ngx-translate/core";
import { ActionNotificationShow } from "@core/notification/notification.actions";
import { guid, isUndefined } from "@core/utils";
import { GroupEntityTableConfig } from "@home/models/group/group-entities-table-config.models";
import { WINDOW } from "@core/services/window.service";
import { GroupEntityComponent } from "@home/components/group/group-entity.component";

@Component({
  selector: 'tb-edge',
  templateUrl: './edge.component.html',
  styleUrls: ['./edge.component.scss']
})
export class EdgeComponent extends GroupEntityComponent<Edge> {

  entityType = EntityType;

  // edgeScope: 'tenant' | 'customer' | 'customer_user';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Edge,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: GroupEntityTableConfig<Edge>,
              protected fb: FormBuilder,
              @Inject(WINDOW) protected window: Window) {
    super(store, fb, entityValue, entitiesTableConfigValue, window);
  }

  ngOnInit() {
    // this.edgeScope = this.entitiesTableConfig.componentsData.edgeScope;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideAssignmentActions() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.assignmentEnabled(this.entity);
    } else {
      return false;
    }
  }

  /* isAssignedToCustomer(entity: EdgeInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  } */

  buildForm(entity: Edge): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        type: [entity ? entity.type : null, [Validators.required]],
        label: [entity ? entity.label : ''],
        cloudEndpoint: [this.window.location.origin, [Validators.required]],
        edgeLicenseKey: ['', [Validators.required]],
        routingKey: guid(),
        secret: this.generateSecret(20),
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : '']
          }
        )
      }
    );
  }

  updateForm(entity: Edge) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type});
    this.entityForm.patchValue({label: entity.label});
    this.entityForm.patchValue({cloudEndpoint: entity.cloudEndpoint});
    this.entityForm.patchValue({edgeLicenseKey: entity.edgeLicenseKey});
    this.entityForm.patchValue({routingKey: entity.routingKey});
    this.entityForm.patchValue({secret: entity.secret});
    this.entityForm.patchValue({additionalInfo: {
      description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }

  onEdgeIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.id-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

  onEdgeKeyCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.edge-key-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

  onEdgeSecretCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.edge-secret-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

  generateSecret(length): string {
    if (isUndefined(length) || length == null) {
      length = 1;
    }
    var l = length > 10 ? 10 : length;
    var str =  Math.random().toString(36).substr(2, l);
    if (str.length >= length) {
      return str;
    }
    return str.concat(this.generateSecret(length - str.length));
  }
}
