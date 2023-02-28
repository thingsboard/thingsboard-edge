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
import { FormBuilder, FormGroup } from '@angular/forms';
import {
  createDefaultEntityTypesVersionLoad, EntityTypeLoadResult,
  EntityTypeVersionLoadRequest,
  VersionLoadRequestType,
  VersionLoadResult
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { TranslateService } from '@ngx-translate/core';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Observable, Subscription } from 'rxjs';
import { share } from 'rxjs/operators';
import { parseHttpErrorMessage } from '@core/utils';

@Component({
  selector: 'tb-complex-version-load',
  templateUrl: './complex-version-load.component.html',
  styleUrls: ['./version-control.scss']
})
export class ComplexVersionLoadComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  versionName: string;

  @Input()
  versionId: string;

  @Input()
  onClose: (result: VersionLoadResult | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  loadVersionFormGroup: FormGroup;

  versionLoadResult: VersionLoadResult = null;

  entityTypeLoadResults: Array<EntityTypeLoadResult> = null;

  errorMessage: SafeHtml;

  hasError = false;

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
    this.loadVersionFormGroup = this.fb.group({
      entityTypes: [createDefaultEntityTypesVersionLoad(), []],
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    if (this.versionLoadResultSubscription) {
      this.versionLoadResultSubscription.unsubscribe();
    }
  }

  entityTypeLoadResultMessage(result: EntityTypeLoadResult): string {
    const entityType = result.entityType;
    let message = this.translate.instant(entityTypeTranslations.get(entityType).typePlural) + ': ';
    const resultMessages: string[] = [];
    if (result.created) {
      resultMessages.push(this.translate.instant('version-control.created', {created: result.created}));
    }
    if (result.updated) {
      resultMessages.push(this.translate.instant('version-control.updated', {updated: result.updated}));
    }
    if (result.deleted) {
      resultMessages.push(this.translate.instant('version-control.deleted', {deleted: result.deleted}));
    }
    if (result.groupsCreated) {
      resultMessages.push(this.translate.instant('version-control.groups-created', {created: result.groupsCreated}));
    }
    if (result.groupsUpdated) {
      resultMessages.push(this.translate.instant('version-control.groups-updated', {updated: result.groupsUpdated}));
    }
    if (result.groupsDeleted) {
      resultMessages.push(this.translate.instant('version-control.groups-deleted', {deleted: result.groupsDeleted}));
    }
    message += resultMessages.join(', ') + '.';
    return message;
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(this.versionLoadResult);
    }
  }

  restore(): void {
    const request: EntityTypeVersionLoadRequest = {
      versionId: this.versionId,
      entityTypes: this.loadVersionFormGroup.get('entityTypes').value,
      type: VersionLoadRequestType.ENTITY_TYPE
    };
    this.versionLoadResult$ = this.entitiesVersionControlService.loadEntitiesVersion(request, {ignoreErrors: true}).pipe(
      share()
    );
    this.cd.detectChanges();
    if (this.popoverComponent) {
      this.popoverComponent.updatePosition();
    }
    this.versionLoadResultSubscription = this.versionLoadResult$.subscribe((result) => {
      this.versionLoadResult = result;
      this.entityTypeLoadResults = (result.result || []).filter(res => res.created || res.updated || res.deleted);
      if (result.error) {
        this.errorMessage = this.entitiesVersionControlService.entityLoadErrorToMessage(result.error);
      }
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    },
    (error) => {
      this.hasError = true;
      this.errorMessage = this.sanitizer.bypassSecurityTrustHtml(parseHttpErrorMessage(error, this.translate).message);
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    });
  }
}
