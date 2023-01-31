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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, Sanitizer } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import {
  EntityDataInfo,
  SingleEntityVersionLoadRequest, VersionCreationResult,
  VersionLoadRequestType,
  VersionLoadResult
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { EntityType } from '@shared/models/entity-type.models';
import { delay, share } from 'rxjs/operators';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Observable, Subscription } from 'rxjs';
import { parseHttpErrorMessage } from '@core/utils';

@Component({
  selector: 'tb-entity-version-restore',
  templateUrl: './entity-version-restore.component.html',
  styleUrls: ['./version-control.scss']
})
export class EntityVersionRestoreComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  versionName: string;

  @Input()
  versionId: string;

  @Input()
  internalEntityId: EntityId;

  @Input()
  externalEntityId: EntityId;

  @Input()
  groupType: EntityType;

  @Input()
  onClose: (result: VersionLoadResult | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  entityTypes = EntityType;

  entityDataInfo: EntityDataInfo = null;

  restoreFormGroup: FormGroup;

  errorMessage: SafeHtml;

  versionLoadResult$: Observable<VersionLoadResult>;

  private versionLoadResultSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.restoreFormGroup = this.fb.group({
      loadAttributes: [true, []],
      loadRelations: [true, []],
      loadCredentials: [true, []],
      loadPermissions: [true, []],
      loadGroupEntities: [true, []],
      autoGenerateIntegrationKey: [false, []]
    });
    this.entitiesVersionControlService.getEntityDataInfo(this.externalEntityId, this.internalEntityId, this.versionId).subscribe((data) => {
      this.entityDataInfo = data;
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    if (this.versionLoadResultSubscription) {
      this.versionLoadResultSubscription.unsubscribe();
    }
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null);
    }
  }

  restore(): void {
    const request: SingleEntityVersionLoadRequest = {
      versionId: this.versionId,
      internalEntityId: this.internalEntityId,
      externalEntityId: this.externalEntityId,
      config: {
        loadRelations: this.entityDataInfo.hasRelations ? this.restoreFormGroup.get('loadRelations').value : false,
        loadAttributes: this.entityDataInfo.hasAttributes ? this.restoreFormGroup.get('loadAttributes').value : false,
        loadCredentials: (this.entityDataInfo.hasCredentials || EntityType.DEVICE === this.groupType) ? this.restoreFormGroup.get('loadCredentials').value : false,
        loadPermissions: this.entityDataInfo.hasPermissions ? this.restoreFormGroup.get('loadPermissions').value : false,
        autoGenerateIntegrationKey: this.internalEntityId.entityType === EntityType.INTEGRATION ? this.restoreFormGroup.get('loadPermissions').value : false,
        loadGroupEntities: this.entityDataInfo.hasGroupEntities ? this.restoreFormGroup.get('loadGroupEntities').value : false
      },
      type: VersionLoadRequestType.SINGLE_ENTITY
    };
    this.versionLoadResult$ = this.entitiesVersionControlService.loadEntitiesVersion(request, {ignoreErrors: true}).pipe(
      share()
    );
    this.cd.detectChanges();
    if (this.popoverComponent) {
      this.popoverComponent.updatePosition();
    }
    this.versionLoadResultSubscription = this.versionLoadResult$.subscribe((result) => {
      if (result.done) {
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
      }
    },
    (error) => {
      this.errorMessage = this.sanitizer.bypassSecurityTrustHtml(parseHttpErrorMessage(error, this.translate).message);
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    });
  }
}
