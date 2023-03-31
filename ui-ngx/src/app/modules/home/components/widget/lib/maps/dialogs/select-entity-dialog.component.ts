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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { FormattedData } from '@shared/models/widget.models';
import { GenericFunction } from '@home/components/widget/lib/maps/map-models';
import { fillDataPattern, processDataPattern, safeExecute } from '@core/utils';
import { parseWithTranslation } from '@home/components/widget/lib/maps/common-maps-utils';

export interface SelectEntityDialogData {
  entities: FormattedData[];
  labelSettings: {
    showLabel: boolean;
    useLabelFunction: boolean;
    parsedLabelFunction: GenericFunction;
    label: string;
  };
}

@Component({
  selector: 'tb-select-entity-dialog',
  templateUrl: './select-entity-dialog.component.html',
  styleUrls: ['./select-entity-dialog.component.scss']
})
export class SelectEntityDialogComponent extends DialogComponent<SelectEntityDialogComponent, FormattedData> {
  selectEntityFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SelectEntityDialogData,
              public dialogRef: MatDialogRef<SelectEntityDialogComponent, FormattedData>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.selectEntityFormGroup = this.fb.group(
      {
        entity: ['', Validators.required]
      }
    );
  }

  public parseName(entity) {
    let name;
    if (this.data.labelSettings?.showLabel) {
      const pattern = this.data.labelSettings.useLabelFunction ? safeExecute(this.data.labelSettings.parsedLabelFunction,
        [entity, this.data.entities, entity.dsIndex]) : this.data.labelSettings.label;
      const markerLabelText = parseWithTranslation.prepareProcessPattern(pattern, true);
      const replaceInfoLabelMarker = processDataPattern(pattern, entity);
      const div = document.createElement('div');
      div.innerHTML = fillDataPattern(markerLabelText, replaceInfoLabelMarker, entity);
      name = div.textContent || div.innerText || '';
    } else {
      name = entity.entityName;
    }
    return name;
  }

  save(): void {
    const entity = this.selectEntityFormGroup.value.entity;
    entity.parseName = this.parseName(this.selectEntityFormGroup.value.entity);
    this.dialogRef.close(entity);
  }
}
