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

import { BaseData, HasId } from '@shared/models/base-data';
import { FormBuilder, FormGroup, NgForm } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';

// @dynamic
@Component({})
export abstract class EntityComponent<T extends BaseData<HasId>,
  P extends PageLink = PageLink,
  L extends BaseData<HasId> = T,
  C extends EntityTableConfig<T, P, L> = EntityTableConfig<T, P, L>>
  extends PageComponent implements OnInit {

  entityForm: FormGroup;

  @ViewChild('entityNgForm', {static: true}) entityNgForm: NgForm;

  isEditValue: boolean;

  @Input()
  set entitiesTableConfig(entitiesTableConfig: C) {
    this.entitiesTableConfigValue = entitiesTableConfig;
  }

  get entitiesTableConfig(): C {
    return this.entitiesTableConfigValue;
  }

  @Input()
  set isEdit(isEdit: boolean) {
    this.isEditValue = isEdit;
    this.updateFormState();
  }

  get isEdit() {
    return this.isEditValue;
  }

  get isAdd(): boolean {
    return this.entityValue && !this.entityValue.id;
  }

  @Input()
  set entity(entity: T) {
    this.entityValue = entity;
    if (this.entityForm) {
      this.entityForm.reset(undefined, {emitEvent: false});
      this.entityForm.markAsPristine();
      this.updateForm(entity);
    }
  }

  get entity(): T {
    return this.entityValue;
  }

  @Output()
  entityAction = new EventEmitter<EntityAction<T>>();

  protected constructor(protected store: Store<AppState>,
                        protected fb: FormBuilder,
                        protected entityValue: T,
                        protected entitiesTableConfigValue: C) {
    super(store);
    this.entityForm = this.buildForm(this.entityValue);
  }

  ngOnInit() {
  }

  onEntityAction($event: Event, action: string) {
    const entityAction = {event: $event, action, entity: this.entity} as EntityAction<T>;
    let handled = false;
    if (this.entitiesTableConfig) {
      handled = this.entitiesTableConfig.onEntityAction(entityAction);
    }
    if (!handled) {
      this.entityAction.emit(entityAction);
    }
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  entityFormValue() {
    const formValue = this.entityForm ? {...this.entityForm.getRawValue()} : {};
    return this.prepareFormValue(formValue);
  }

  prepareFormValue(formValue: any): any {
    return formValue;
  }

  abstract buildForm(entity: T): FormGroup;

  abstract updateForm(entity: T);

}
