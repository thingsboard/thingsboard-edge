///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import {
  EntityDataInfo,
  SingleEntityVersionLoadRequest,
  VersionLoadRequestType,
  VersionLoadResult
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { delay } from 'rxjs/operators';
import { SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'tb-entity-version-restore',
  templateUrl: './entity-version-restore.component.html',
  styleUrls: ['./version-control.scss']
})
export class EntityVersionRestoreComponent extends PageComponent implements OnInit {

  @Input()
  branch: string;

  @Input()
  versionName: string;

  @Input()
  versionId: string;

  @Input()
  externalEntityId: EntityId;

  @Input()
  onClose: (result: VersionLoadResult | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  entityDataInfo: EntityDataInfo = null;

  restoreFormGroup: FormGroup;

  errorMessage: SafeHtml;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.restoreFormGroup = this.fb.group({
      loadAttributes: [true, []],
      loadRelations: [true, []],
      loadCredentials: [true, []],
      loadPermissions: [true, []],
      loadGroupEntities: [true, []]
    });
    this.entitiesVersionControlService.getEntityDataInfo(this.externalEntityId, this.versionId).subscribe((data) => {
      this.entityDataInfo = data;
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    });
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null);
    }
  }

  restore(): void {
    const request: SingleEntityVersionLoadRequest = {
      branch: this.branch,
      versionId: this.versionId,
      externalEntityId: this.externalEntityId,
      config: {
        loadRelations: this.entityDataInfo.hasRelations ? this.restoreFormGroup.get('loadRelations').value : false,
        loadAttributes: this.entityDataInfo.hasAttributes ? this.restoreFormGroup.get('loadAttributes').value : false,
        loadCredentials: this.entityDataInfo.hasCredentials ? this.restoreFormGroup.get('loadCredentials').value : false,
        loadPermissions: this.entityDataInfo.hasPermissions ? this.restoreFormGroup.get('loadPermissions').value : false,
        loadGroupEntities: this.entityDataInfo.hasGroupEntities ? this.restoreFormGroup.get('loadGroupEntities').value : false
      },
      type: VersionLoadRequestType.SINGLE_ENTITY
    };
    this.entitiesVersionControlService.loadEntitiesVersion(request).subscribe((result) => {
      if (result.error) {
        this.errorMessage = this.entitiesVersionControlService.entityLoadErrorToMessage(result.error);
        this.cd.detectChanges();
        if (this.popoverComponent) {
          this.popoverComponent.updatePosition();
        }
      } else {
        if (this.onClose) {
          this.onClose(result);
        }
      }
    });
  }
}
