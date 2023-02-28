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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
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
              protected fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
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
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
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
