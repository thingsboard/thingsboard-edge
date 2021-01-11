///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatDialog } from '@angular/material/dialog';
import { genericRolePermissionsValidator, Role } from '@shared/models/role.models';
import { RoleType, roleTypeTranslationMap } from '@shared/models/security.models';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-role',
  templateUrl: './role.component.html',
  styleUrls: ['./role.component.scss']
})
export class RoleComponent extends EntityComponent<Role> {

  roleType = RoleType;

  roleTypes = Object.keys(RoleType);

  roleTypeTranslations = roleTypeTranslationMap;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private dialog: MatDialog,
              @Inject('entity') protected entityValue: Role,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Role>,
              protected fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Role): FormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        type: [entity ? entity.type : null, [Validators.required]],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        ),
        genericPermissions: [entity && entity.type === RoleType.GENERIC ? entity.permissions : null, []],
        groupPermissions: [entity && entity.type === RoleType.GROUP ? entity.permissions : null, []]
      }
    );
    this.updateValidators(form);
    form.get('type').valueChanges.subscribe((newVal) => {
      this.roleTypeChanged(form, newVal);
    });
    return form;
  }

  private roleTypeChanged(form: FormGroup, newVal: RoleType) {
    if (this.isEdit) {
      const prevVal: RoleType = form.value.type;
      if (!isEqual(newVal, prevVal)) {
          form.get('genericPermissions').patchValue({}, {emitEvent: false});
          form.get('groupPermissions').patchValue([], {emitEvent: false});
          this.updateValidators(form);
      }
    }
  }

  private updateValidators(form: FormGroup) {
    const roleType: RoleType = form.get('type').value;
    form.get('genericPermissions').setValidators([genericRolePermissionsValidator(roleType && roleType === RoleType.GENERIC)]);
    form.get('groupPermissions').setValidators(roleType && roleType === RoleType.GROUP ? [Validators.required] : []);
    form.get('genericPermissions').updateValueAndValidity();
    form.get('groupPermissions').updateValueAndValidity();
  }

  updateForm(entity: Role) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type}, {emitEvent: false});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
    this.entityForm.patchValue({genericPermissions: entity.type === RoleType.GENERIC ? entity.permissions : null});
    this.entityForm.patchValue({groupPermissions: entity.type === RoleType.GROUP ? entity.permissions : null});
    this.updateValidators(this.entityForm);
  }

  prepareFormValue(formValue: any): any {
    const roleType: RoleType = formValue.type;
    if (roleType === RoleType.GENERIC) {
      formValue.permissions = formValue.genericPermissions;
    } else if (roleType === RoleType.GROUP) {
      formValue.permissions = formValue.groupPermissions;
    }
    delete formValue.genericPermissions;
    delete formValue.groupPermissions;
    return super.prepareFormValue(formValue);
  }

  onRoleIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('role.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
