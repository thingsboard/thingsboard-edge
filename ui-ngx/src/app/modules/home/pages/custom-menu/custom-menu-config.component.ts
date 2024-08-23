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

import { ChangeDetectorRef, Component, OnInit, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { CustomMenuConfig, CustomMenuInfo } from '@shared/models/custom-menu.models';
import { FormControl, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';

@Component({
  selector: 'tb-custom-menu-config',
  templateUrl: './custom-menu-config.component.html',
  styleUrls: ['./custom-menu-config.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CustomMenuConfigComponent extends PageComponent implements OnInit, HasDirtyFlag {

  private forcePristine = false;

  get isDirty(): boolean {
    return this.customMenuFormGroup.dirty && !this.forcePristine;
  }

  set isDirty(value: boolean) {
    this.forcePristine = !value;
  }

  customMenu: CustomMenuInfo;
  customMenuConfig: CustomMenuConfig;

  readonly: boolean;

  showHiddenItems: FormControl = new FormControl<boolean>(true);

  customMenuFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private router: Router,
              private route: ActivatedRoute,
              private customMenuService: CustomMenuService,
              private userPermissionsService: UserPermissionsService,
              private cd: ChangeDetectorRef,
              private dialog: MatDialog) {
    super(store);
    this.customMenu = this.route.snapshot.data.customMenu;
    this.customMenuConfig = this.route.snapshot.data.customMenuConfig;
  }

  ngOnInit(): void {
    this.customMenuFormGroup = this.fb.group({
      items: []
    });
    const authUser = getCurrentAuthUser(this.store);
    this.readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);
    if (this.readonly) {
      this.customMenuFormGroup.disable({emitEvent: false});
    }
  }

  goBack() {
    this.router.navigate(['..'], { relativeTo: this.route });
  }

  save() {
    // toto get value
    const config = this.customMenuConfig;
    this.customMenuService.updateCustomMenuConfig(this.customMenu.id.id, config).subscribe(
      () => {
        this.customMenuFormGroup.markAsPristine();
      }
    );
  }

}
