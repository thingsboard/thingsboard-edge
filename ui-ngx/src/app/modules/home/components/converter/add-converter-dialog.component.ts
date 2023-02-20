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

import { AfterViewInit, Component, Inject, OnInit, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { BaseData, HasId } from '@shared/models/base-data';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Converter, ConverterType, getConverterHelpLink } from '@shared/models/converter.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { ConverterService } from '@core/http/converter.service';

export interface AddConverterDialogData  {
  name: string;
  edgeTemplate?: boolean;
  type: ConverterType;
}

@Component({
  selector: 'tb-add-converter-dialog',
  templateUrl: './add-converter-dialog.component.html',
  styleUrls: ['./add-converter-dialog.component.scss']
})
export class AddConverterDialogComponent extends DialogComponent<AddConverterDialogComponent, BaseData<HasId>>
  implements OnInit, AfterViewInit {

  dialogTitle = entityTypeTranslations.get(EntityType.CONVERTER).add;
  converter: Converter;

  @ViewChild('converterComponent', {static: true}) converterComponent: ConverterComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddConverterDialogData,
              public dialogRef: MatDialogRef<AddConverterDialogComponent, BaseData<HasId>>,
              private converterService: ConverterService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.converter = {...this.data as Converter};
  }

  ngAfterViewInit() {
    this.converterComponent.entityForm.get('type').disable({emitEvent: false});
    this.converterComponent.entityForm.patchValue(this.converter);
  }

  helpLinkId(): string {
    return getConverterHelpLink(this.data as Converter);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.converterComponent.entityForm.valid) {
      this.converter = {...this.converter, ...this.converterComponent.entityFormValue()};
      this.converterService.saveConverter(this.converter).subscribe(
        (entity) => {
          this.dialogRef.close(entity);
        }
      );
    }
  }
}
