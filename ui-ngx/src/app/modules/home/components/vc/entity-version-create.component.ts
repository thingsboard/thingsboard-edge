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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import {
  SingleEntityVersionCreateRequest,
  VersionCreateRequestType,
  VersionCreationResult
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of, Subscription } from 'rxjs';
import { EntityType } from '@shared/models/entity-type.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { share } from 'rxjs/operators';
import { parseHttpErrorMessage } from '@core/utils';

@Component({
  selector: 'tb-entity-version-create',
  templateUrl: './entity-version-create.component.html',
  styleUrls: ['./version-control.scss']
})
export class EntityVersionCreateComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  branch: string;

  @Input()
  entityId: EntityId;

  @Input()
  groupType: EntityType;

  @Input()
  entityName: string;

  @Input()
  onClose: (result: VersionCreationResult | null, branch: string | null) => void;

  @Input()
  onBeforeCreateVersion: () => Observable<any>;

  @Input()
  popoverComponent: TbPopoverComponent;

  createVersionFormGroup: UntypedFormGroup;

  entityTypes = EntityType;

  resultMessage: string;

  versionCreateResult$: Observable<VersionCreationResult>;

  private versionCreateResultSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.createVersionFormGroup = this.fb.group({
      branch: [this.branch, [Validators.required]],
      versionName: [this.translate.instant('version-control.default-create-entity-version-name',
        {entityName: this.entityName}), [Validators.required, Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      saveRelations: [false, []],
      saveAttributes: [true, []],
      saveCredentials: [true, []],
      savePermissions: [true, []],
      saveGroupEntities: [true, []]
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    if (this.versionCreateResultSubscription) {
      this.versionCreateResultSubscription.unsubscribe();
    }
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null, null);
    }
  }

  export(): void {
    const before = this.onBeforeCreateVersion ? this.onBeforeCreateVersion() : of(null);
    before.subscribe(() => {
      const request: SingleEntityVersionCreateRequest = {
        entityId: this.entityId,
        branch: this.createVersionFormGroup.get('branch').value,
        versionName: this.createVersionFormGroup.get('versionName').value,
        config: {
          saveRelations: this.createVersionFormGroup.get('saveRelations').value,
          saveAttributes: this.createVersionFormGroup.get('saveAttributes').value,
          saveCredentials: (this.entityId.entityType === EntityType.DEVICE || EntityType.DEVICE === this.groupType) ?
            this.createVersionFormGroup.get('saveCredentials').value : false,
          savePermissions: this.entityId.entityType === EntityType.ENTITY_GROUP && EntityType.USER === this.groupType ?
            this.createVersionFormGroup.get('savePermissions').value : false,
          saveGroupEntities: this.entityId.entityType === EntityType.ENTITY_GROUP && EntityType.USER !== this.groupType ?
            this.createVersionFormGroup.get('saveGroupEntities').value : false,
        },
        type: VersionCreateRequestType.SINGLE_ENTITY
      };
      this.versionCreateResult$ = this.entitiesVersionControlService.saveEntitiesVersion(request, {ignoreErrors: true}).pipe(
        share()
      );
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }

      this.versionCreateResultSubscription = this.versionCreateResult$.subscribe((result) => {
        if (result.done) {
          if (!result.added && !result.modified || result.error) {
            this.resultMessage = result.error ? result.error : this.translate.instant('version-control.nothing-to-commit');
            this.cd.detectChanges();
            if (this.popoverComponent) {
              this.popoverComponent.updatePosition();
            }
          } else if (this.onClose) {
            this.onClose(result, request.branch);
          }
        }
      },
      (error) => {
        this.resultMessage = parseHttpErrorMessage(error, this.translate).message;
        this.cd.detectChanges();
        if (this.popoverComponent) {
          this.popoverComponent.updatePosition();
        }
      });
    });
  }
}
