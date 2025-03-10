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
  ResourceType,
  ResourceTypeExtension,
  ResourceTypeMIMETypes,
  ResourceTypeTranslationMap
} from '@shared/models/resource.models';
import { startWith, takeUntil } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { isDefinedAndNotNull } from '@core/utils';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Component({
  selector: 'tb-resources-library',
  templateUrl: './resources-library.component.html'
})
export class ResourcesLibraryComponent extends EntityComponent<Resource> implements OnInit, OnDestroy {

  readonly resourceType = ResourceType;
  readonly resourceTypes = [ResourceType.LWM2M_MODEL, ResourceType.PKCS_12, ResourceType.JKS];
  readonly resourceTypesTranslationMap = ResourceTypeTranslationMap;
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
      this.observeResourceTypeChange();
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
      resourceType: [entity?.resourceType ? entity.resourceType : ResourceType.LWM2M_MODEL, Validators.required],
      fileName: [entity ? entity.fileName : null, Validators.required],
      data: [entity ? entity.data : null, this.isAdd ? [Validators.required] : []]
    });
  }

  updateForm(entity: Resource): void {
    this.entityForm.patchValue(entity);
  }

  override updateFormState(): void {
    super.updateFormState();
    if (this.isEdit && this.entityForm && !this.isAdd) {
      this.entityForm.get('resourceType').disable({ emitEvent: false });
      this.entityForm.get('fileName').disable({ emitEvent: false });
    }
  }

  prepareFormValue(formValue: Resource): Resource {
    if (this.isEdit && !isDefinedAndNotNull(formValue.data)) {
      delete formValue.data;
    }
    return super.prepareFormValue(formValue);
  }

  getAllowedExtensions(): string {
    try {
      return ResourceTypeExtension.get(this.entityForm.get('resourceType').value);
    } catch (e) {
      return '';
    }
  }

  getAcceptType(): string {
    try {
      return ResourceTypeMIMETypes.get(this.entityForm.get('resourceType').value);
    } catch (e) {
      return '*/*';
    }
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

  private observeResourceTypeChange(): void {
    this.entityForm.get('resourceType').valueChanges.pipe(
      startWith(ResourceType.LWM2M_MODEL),
      takeUntil(this.destroy$)
    ).subscribe((type: ResourceType) => this.onResourceTypeChange(type));
  }

  private onResourceTypeChange(type: ResourceType): void {
    if (type === this.resourceType.LWM2M_MODEL) {
      this.entityForm.get('title').disable({emitEvent: false});
      this.entityForm.patchValue({title: ''}, {emitEvent: false});
    } else {
      this.entityForm.get('title').enable({emitEvent: false});
    }
    this.entityForm.patchValue({
      data: null,
      fileName: null
    }, {emitEvent: false});
  }
}
