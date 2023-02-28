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

import { AfterViewInit, Component, Inject, OnInit, QueryList, ViewChildren } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { FormControl } from '@angular/forms';
import { merge, Observable, of } from 'rxjs';
import { delay, map, mapTo, mergeMap, share, startWith, tap } from 'rxjs/operators';

export interface MaterialIconsDialogData {
  icon: string;
}

@Component({
  selector: 'tb-material-icons-dialog',
  templateUrl: './material-icons-dialog.component.html',
  providers: [],
  styleUrls: ['./material-icons-dialog.component.scss']
})
export class MaterialIconsDialogComponent extends DialogComponent<MaterialIconsDialogComponent, string>
  implements OnInit, AfterViewInit {

  @ViewChildren('iconButtons') iconButtons: QueryList<HTMLElement>;

  selectedIcon: string;
  icons$: Observable<Array<string>>;
  loadingIcons$: Observable<boolean>;

  showAllControl: FormControl;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MaterialIconsDialogData,
              private utils: UtilsService,
              public dialogRef: MatDialogRef<MaterialIconsDialogComponent, string>) {
    super(store, router, dialogRef);
    this.selectedIcon = data.icon;
    this.showAllControl = new FormControl(false);
  }

  ngOnInit(): void {
    this.icons$ = this.showAllControl.valueChanges.pipe(
      map((showAll) => {
        return {firstTime: false, showAll};
      }),
      startWith<{firstTime: boolean, showAll: boolean}>({firstTime: true, showAll: false}),
      mergeMap((data) => {
        if (data.showAll) {
          return this.utils.getMaterialIcons().pipe(delay(100));
        } else {
          const res = of(this.utils.getCommonMaterialIcons());
          return data.firstTime ? res : res.pipe(delay(50));
        }
      }),
      share()
    );
  }

  ngAfterViewInit(): void {
    this.loadingIcons$ = merge(
      this.showAllControl.valueChanges.pipe(
        mapTo(true),
      ),
      this.iconButtons.changes.pipe(
        delay(100),
        mapTo( false),
      )
    ).pipe(
      tap((loadingIcons) => {
        if (loadingIcons) {
          this.showAllControl.disable({emitEvent: false});
        } else {
          this.showAllControl.enable({emitEvent: false});
        }
      }),
      share()
    );
  }

  selectIcon(icon: string) {
    this.dialogRef.close(icon);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

}
