///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntityComponent } from '@home/components/entity/entity.component';
import {
  Resource,
  ResourceSubType,
  ResourceSubTypeTranslationMap,
  ResourceType,
  ResourceTypeExtension,
  ResourceTypeMIMETypes
} from '@shared/models/resource.models';
import { startWith, takeUntil } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { isDefinedAndNotNull } from '@core/utils';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { scadaSymbolGeneralStateHighlightRules } from '@home/pages/scada-symbol/scada-symbol-editor.models';

@Component({
  selector: 'tb-js-resource',
  templateUrl: './js-resource.component.html'
})
export class JsResourceComponent extends EntityComponent<Resource> implements OnInit, OnDestroy {

  readonly ResourceSubType = ResourceSubType;
  readonly jsResourceSubTypes: ResourceSubType[] = [ResourceSubType.EXTENSION, ResourceSubType.MODULE];
  readonly ResourceSubTypeTranslationMap = ResourceSubTypeTranslationMap;
  readonly maxResourceSize = getCurrentAuthState(this.store).maxResourceSize;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Resource,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Resource>,
              public fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    if (this.isAdd) {
      this.observeResourceSubTypeChange();
    }
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  hideDelete(): boolean {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Resource): FormGroup {
    return this.fb.group({
      title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
      resourceSubType: [entity?.resourceSubType ? entity.resourceSubType : ResourceSubType.EXTENSION, Validators.required],
      fileName: [entity ? entity.fileName : null, Validators.required],
      data: [entity ? entity.data : null, this.isAdd ? [Validators.required] : []],
      content: [entity?.data?.length ? window.atob(entity.data) : '', Validators.required]
    });
  }

  updateForm(entity: Resource): void {
    this.entityForm.patchValue(entity);
    const content = entity.resourceSubType === ResourceSubType.MODULE && entity?.data?.length ? window.atob(entity.data) : '';
    this.entityForm.get('content').patchValue(content);
  }

  override updateFormState(): void {
    super.updateFormState();
    if (this.isEdit && this.entityForm && !this.isAdd) {
      this.entityForm.get('resourceSubType').disable({ emitEvent: false });
      this.updateResourceSubTypeFieldsState(this.entityForm.get('resourceSubType').value);
    }
  }

  prepareFormValue(formValue: Resource): Resource {
    if (this.isEdit && !isDefinedAndNotNull(formValue.data)) {
      delete formValue.data;
    }
    if (formValue.resourceSubType === ResourceSubType.MODULE) {
      if (!formValue.fileName) {
        formValue.fileName = formValue.title + '.js';
      }
      formValue.data = window.btoa((formValue as any).content);
      delete (formValue as any).content;
    }
    return super.prepareFormValue(formValue);
  }

  getAllowedExtensions(): string {
    return ResourceTypeExtension.get(ResourceType.JS_MODULE);
  }

  getAcceptType(): string {
    return ResourceTypeMIMETypes.get(ResourceType.JS_MODULE);
  }

  convertToBase64File(data: string): string {
    return window.btoa(data);
  }

  onResourceIdCopied(): void {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('resource.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  uploadContentFromFile(content: string) {
    this.entityForm.get('content').patchValue(content);
    this.entityForm.markAsDirty();
  }

  private observeResourceSubTypeChange(): void {
    this.entityForm.get('resourceSubType').valueChanges.pipe(
      startWith(ResourceSubType.EXTENSION),
      takeUntil(this.destroy$)
    ).subscribe((subType: ResourceSubType) => this.onResourceSubTypeChange(subType));
  }

  private onResourceSubTypeChange(subType: ResourceSubType): void {
    this.updateResourceSubTypeFieldsState(subType);
    this.entityForm.patchValue({
      data: null,
      fileName: null
    }, {emitEvent: false});
  }

  private updateResourceSubTypeFieldsState(subType: ResourceSubType) {
    if (subType === ResourceSubType.EXTENSION) {
      this.entityForm.get('data').enable({ emitEvent: false });
      this.entityForm.get('fileName').enable({ emitEvent: false });
      this.entityForm.get('content').disable({ emitEvent: false });
    } else {
      this.entityForm.get('data').disable({ emitEvent: false });
      this.entityForm.get('fileName').disable({ emitEvent: false });
      this.entityForm.get('content').enable({ emitEvent: false });
    }
  }

  protected readonly highlightRules = scadaSymbolGeneralStateHighlightRules;
}
