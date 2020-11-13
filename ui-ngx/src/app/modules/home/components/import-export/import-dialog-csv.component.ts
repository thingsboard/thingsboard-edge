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

import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { MatVerticalStepper } from '@angular/material/stepper';
import {
  convertCSVToJson,
  CsvColumnParam,
  CsvToJsonConfig,
  CsvToJsonResult,
  ImportEntityColumnType
} from '@home/components/import-export/import-export.models';
import { ImportEntitiesResultInfo, ImportEntityData } from '@app/shared/models/entity.models';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { CustomerId } from '@shared/models/id/customer-id';

export interface ImportDialogCsvData {
  entityType: EntityType;
  customerId: CustomerId;
  importTitle: string;
  importFileLabel: string;
  entityGroupId: string;
}

@Component({
  selector: 'tb-import-csv-dialog',
  templateUrl: './import-dialog-csv.component.html',
  providers: [],
  styleUrls: ['./import-dialog-csv.component.scss']
})
export class ImportDialogCsvComponent extends DialogComponent<ImportDialogCsvComponent, boolean>
  implements OnInit {

  @ViewChild('importStepper', {static: true}) importStepper: MatVerticalStepper;

  entityType: EntityType;
  importTitle: string;
  importFileLabel: string;
  customerId: CustomerId;
  entityGroupId: string;

  delimiters: {key: string, value: string}[] = [{
    key: ',',
    value: ','
  }, {
    key: ';',
    value: ';'
  }, {
    key: '|',
    value: '|'
  }, {
    key: '\t',
    value: 'Tab'
  }];

  selectedIndex = 0;

  selectFileFormGroup: FormGroup;
  importParametersFormGroup: FormGroup;
  columnTypesFormGroup: FormGroup;

  isImportData = false;
  progressCreate = 0;
  statistical: ImportEntitiesResultInfo;

  private parseData: CsvToJsonResult;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ImportDialogCsvData,
              public dialogRef: MatDialogRef<ImportDialogCsvComponent, boolean>,
              public translate: TranslateService,
              private importExport: ImportExportService,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.entityType = data.entityType;
    this.importTitle = data.importTitle;
    this.importFileLabel = data.importFileLabel;
    this.customerId = data.customerId;
    this.entityGroupId = data.entityGroupId;

    this.selectFileFormGroup = this.fb.group(
      {
        importData: [null, [Validators.required]]
      }
    );
    this.importParametersFormGroup = this.fb.group({
      delim: [',', [Validators.required]],
      isHeader: [true, []],
      isUpdate: [true, []],
    });
    this.columnTypesFormGroup = this.fb.group({
      columnsParam: [[], []]
    });
  }

  ngOnInit(): void {
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  previousStep() {
    this.importStepper.previous();
  }

  nextStep(step: number) {
    switch (step) {
      case 2:
        this.importStepper.next();
        break;
      case 3:
        const importData: string = this.selectFileFormGroup.get('importData').value;
        const parseData = this.parseCSV(importData);
        if (parseData === -1) {
          this.importStepper.previous();
          this.importStepper.selected.reset();
        } else {
          this.parseData = parseData as CsvToJsonResult;
          const columnsParam = this.createColumnsData();
          this.columnTypesFormGroup.patchValue({columnsParam}, {emitEvent: true});
          this.importStepper.next();
        }
        break;
      case 4:
        this.importStepper.next();
        this.isImportData = true;
        this.addEntities();
        break;
      case 6:
        this.dialogRef.close(true);
        break;
    }
  }

  private parseCSV(importData: string): CsvToJsonResult | number {
    const config: CsvToJsonConfig = {
      delim: this.importParametersFormGroup.get('delim').value,
      header: this.importParametersFormGroup.get('isHeader').value
    };
    return convertCSVToJson(importData, config,
      (messageId, params) => {
        this.store.dispatch(new ActionNotificationShow(
          {message: this.translate.instant(messageId, params),
            type: 'error'}));
      }
    );
  }

  private createColumnsData(): CsvColumnParam[] {
    const columnsParam: CsvColumnParam[] = [];
    const isHeader: boolean = this.importParametersFormGroup.get('isHeader').value;
    for (let i = 0; i < this.parseData.headers.length; i++) {
      let columnParam: CsvColumnParam;
      if (isHeader && this.parseData.headers[i].search(/^(name|type|label)$/im) === 0) {
        columnParam = {
          type: ImportEntityColumnType[this.parseData.headers[i].toLowerCase()],
          key: this.parseData.headers[i].toLowerCase(),
          sampleData: this.parseData.rows[0][i]
        };
      } else {
        columnParam = {
          type: ImportEntityColumnType.serverAttribute,
          key: isHeader ? this.parseData.headers[i] : '',
          sampleData: this.parseData.rows[0][i]
        };
      }
      columnsParam.push(columnParam);
    }
    return columnsParam;
  }

  private addEntities() {
    const importData = this.parseData;
    const parameterColumns: CsvColumnParam[] = this.columnTypesFormGroup.get('columnsParam').value;
    const entitiesData: ImportEntityData[] = [];
    let sentDataLength = 0;
    for (let row = 0; row < importData.rows.length; row++) {
      const entityData: ImportEntityData = {
        name: '',
        type: '',
        description: '',
        gateway: null,
        label: '',
        accessToken: '',
        attributes: {
          server: [],
          shared: []
        },
        timeseries: []
      };
      const i = row;
      for (let j = 0; j < parameterColumns.length; j++) {
        switch (parameterColumns[j].type) {
          case ImportEntityColumnType.serverAttribute:
            entityData.attributes.server.push({
              key: parameterColumns[j].key,
              value: importData.rows[i][j]
            });
            break;
          case ImportEntityColumnType.timeseries:
            entityData.timeseries.push({
              key: parameterColumns[j].key,
              value: importData.rows[i][j]
            });
            break;
          case ImportEntityColumnType.sharedAttribute:
            entityData.attributes.shared.push({
              key: parameterColumns[j].key,
              value: importData.rows[i][j]
            });
            break;
          case ImportEntityColumnType.accessToken:
            entityData.accessToken = importData.rows[i][j];
            break;
          case ImportEntityColumnType.name:
            entityData.name = importData.rows[i][j];
            break;
          case ImportEntityColumnType.type:
            entityData.type = importData.rows[i][j];
            break;
          case ImportEntityColumnType.label:
            entityData.label = importData.rows[i][j];
            break;
          case ImportEntityColumnType.isGateway:
            entityData.gateway = importData.rows[i][j];
            break;
          case ImportEntityColumnType.description:
            entityData.description = importData.rows[i][j];
            break;
        }
      }
      entitiesData.push(entityData);
    }
    const createImportEntityCompleted = () => {
      sentDataLength++;
      this.progressCreate = Math.round((sentDataLength / importData.rows.length) * 100);
    };

    const isUpdate: boolean = this.importParametersFormGroup.get('isUpdate').value;

    this.importExport.importEntities(entitiesData, this.customerId, this.entityType, this.entityGroupId, isUpdate,
      createImportEntityCompleted, {ignoreErrors: true, resendRequest: true}).subscribe(
      (result) => {
        this.statistical = result;
        this.isImportData = false;
        this.importStepper.next();
      }
    );
  }

}
