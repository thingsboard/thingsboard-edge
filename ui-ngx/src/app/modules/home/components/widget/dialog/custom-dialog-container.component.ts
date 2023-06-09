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

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  Component,
  ComponentFactory,
  ComponentRef, HostBinding,
  Inject,
  Injector,
  OnDestroy,
  ViewContainerRef
} from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import {
  CUSTOM_DIALOG_DATA,
  CustomDialogComponent,
  CustomDialogData
} from '@home/components/widget/dialog/custom-dialog.component';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';

export interface CustomDialogContainerData {
  controller: (instance: CustomDialogComponent) => void;
  data?: any;
  customComponentFactory: ComponentFactory<CustomDialogComponent>;
}

@Component({
  selector: 'tb-custom-dialog-container-component',
  template: ''
})
export class CustomDialogContainerComponent extends DialogComponent<CustomDialogContainerComponent> implements OnDestroy {

  @HostBinding('style.height') height = '0px';

  private readonly customComponentRef: ComponentRef<CustomDialogComponent>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public viewContainerRef: ViewContainerRef,
              public dialogRef: MatDialogRef<CustomDialogContainerComponent>,
              private dialogService: DialogService,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: CustomDialogContainerData) {
    super(store, router, dialogRef);
    let customDialogData: CustomDialogData = {
      controller: this.data.controller
    };
    if (this.data.data) {
      customDialogData = {...customDialogData, ...this.data.data};
    }
    const injector: Injector = Injector.create({
      providers: [{
        provide: CUSTOM_DIALOG_DATA,
        useValue: customDialogData
      },
        {
          provide: MatDialogRef,
          useValue: dialogRef
        }]
    });
    try {
      this.customComponentRef = this.viewContainerRef.createComponent(this.data.customComponentFactory, 0, injector);
    } catch (e: any) {
      let message;
      if (e.message?.startsWith('NG0')) {
        message = this.translate.instant('widget-action.custom-pretty-template-error');
      } else {
        message = this.translate.instant('widget-action.custom-pretty-controller-error');
      }
      dialogRef.close();
      console.error(e);
      this.dialogService.errorAlert(this.translate.instant('widget-action.custom-pretty-error-title'), message, e);
    }
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.customComponentRef) {
      this.customComponentRef.destroy();
    }
  }

}
