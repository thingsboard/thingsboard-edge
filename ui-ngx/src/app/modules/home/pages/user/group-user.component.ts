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

import { ChangeDetectorRef, Component, Inject, Optional } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { User } from '@shared/models/user.model';
import { selectAuth } from '@core/auth/auth.selectors';
import { map } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { isDefinedAndNotNull, isUndefined } from '@core/utils';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-group-user',
  templateUrl: './group-user.component.html',
  styleUrls: ['./user.component.scss']
})
export class GroupUserComponent extends GroupEntityComponent<User> {

  authority = Authority;

  loginAsUserEnabled$ = this.store.pipe(
    select(selectAuth),
    map((auth) => auth.userTokenAccessEnabled)
  );

  whiteLabelingAllowed$ = this.store.pipe(
    select(selectAuth),
    map((auth) => auth.whiteLabelingAllowed)
  );

  constructor(protected store: Store<AppState>,
              @Optional() @Inject('entity') protected entityValue: User,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: GroupEntityTableConfig<User>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private translate: TranslateService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
   }
  }

  isUserCredentialsEnabled(): boolean {
    if (!this.entity || !this.entity.additionalInfo || isUndefined(this.entity.additionalInfo.userCredentialsEnabled)) {
      return true;
    } else {
      return this.entity.additionalInfo.userCredentialsEnabled === true;
    }
  }

  buildForm(entity: User): UntypedFormGroup {
    return this.fb.group(
      {
        email: [entity ? entity.email : '', [Validators.required, Validators.email]],
        firstName: [entity ? entity.firstName : ''],
        lastName: [entity ? entity.lastName : ''],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
            defaultDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.defaultDashboardId : null],
            defaultDashboardFullscreen: [entity && entity.additionalInfo ? entity.additionalInfo.defaultDashboardFullscreen : false],
            homeDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null],
            homeDashboardHideToolbar: [entity && entity.additionalInfo &&
            isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true]
          }
        )
      }
    );
  }

  updateForm(entity: User) {
    this.entityForm.patchValue({email: entity.email});
    this.entityForm.patchValue({firstName: entity.firstName});
    this.entityForm.patchValue({lastName: entity.lastName});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
    this.entityForm.patchValue({additionalInfo:
        {defaultDashboardId: entity.additionalInfo ? entity.additionalInfo.defaultDashboardId : null}});
    this.entityForm.patchValue({additionalInfo:
        {defaultDashboardFullscreen: entity.additionalInfo ? entity.additionalInfo.defaultDashboardFullscreen : false}});
    this.entityForm.patchValue({additionalInfo:
        {homeDashboardId: entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null}});
    this.entityForm.patchValue({additionalInfo:
        {homeDashboardHideToolbar: entity.additionalInfo &&
          isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true}});
  }

  onUserIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('user.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }
    ));
  }
}
