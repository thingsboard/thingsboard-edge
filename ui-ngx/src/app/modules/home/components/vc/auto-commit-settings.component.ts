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

import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import { AutoCommitSettings, AutoVersionCreateConfig } from '@shared/models/settings.models';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import {
  EntityTypeVersionCreateConfig,
  exportableEntityTypes,
  overrideEntityTypeTranslations
} from '@shared/models/vc.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
  selector: 'tb-auto-commit-settings',
  templateUrl: './auto-commit-settings.component.html',
  styleUrls: ['./auto-commit-settings.component.scss', './../../pages/admin/settings-card.scss']
})
export class AutoCommitSettingsComponent extends PageComponent implements OnInit {

  autoCommitSettingsForm: UntypedFormGroup;
  settings: AutoCommitSettings = null;

  entityTypes = EntityType;

  isReadOnly: Observable<boolean>;

  overrideEntityTypeTranslationsMap = overrideEntityTypeTranslations;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.VERSION_CONTROL, Operation.WRITE);
  allowDelete = this.userPermissionsService.hasGenericPermission(Resource.VERSION_CONTROL, Operation.DELETE);

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private dialogService: DialogService,
              private sanitizer: DomSanitizer,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.autoCommitSettingsForm = this.fb.group({
      entityTypes: this.fb.array([], [])
    });
    this.adminService.autoCommitSettingsExists().pipe(
      catchError(() => of(false)),
      mergeMap((hasAutoCommitSettings) => {
        if (hasAutoCommitSettings) {
          return this.adminService.getAutoCommitSettings({ignoreErrors: true}).pipe(
            catchError(() => of(null))
          );
        } else {
          return of(null);
        }
      })
    ).subscribe(
      (settings) => {
        this.settings = settings;
        this.autoCommitSettingsForm.setControl('entityTypes',
          this.prepareEntityTypesFormArray(settings), {emitEvent: false});
        if (this.readonly) {
          this.autoCommitSettingsForm.disable({emitEvent: false});
        }
      });
    this.isReadOnly = this.adminService.getRepositorySettingsInfo().pipe(map(settings => settings.readOnly));
  }

  entityTypesFormGroupArray(): UntypedFormGroup[] {
    return (this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray).controls as UntypedFormGroup[];
  }

  entityTypesFormGroupExpanded(entityTypeControl: AbstractControl): boolean {
    return !!(entityTypeControl as any).expanded;
  }

  public trackByEntityType(index: number, entityTypeControl: AbstractControl): any {
    return entityTypeControl;
  }

  public removeEntityType(index: number) {
    (this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray).removeAt(index);
    this.autoCommitSettingsForm.markAsDirty();
  }

  public addEnabled(): boolean {
    const entityTypesArray = this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray;
    return entityTypesArray.length < exportableEntityTypes.length;
  }

  public addEntityType() {
    const entityTypesArray = this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray;
    const config: AutoVersionCreateConfig = {
      branch: null,
      saveAttributes: true,
      saveRelations: false,
      saveCredentials: true,
      savePermissions: true,
      saveGroupEntities: true
    };
    const allowed = this.allowedEntityTypes();
    let entityType: EntityType = null;
    if (allowed.length) {
      entityType = allowed[0];
    }
    const entityTypeControl = this.createEntityTypeControl(entityType, config);
    (entityTypeControl as any).expanded = true;
    entityTypesArray.push(entityTypeControl);
    this.autoCommitSettingsForm.updateValueAndValidity();
    this.autoCommitSettingsForm.markAsDirty();
  }

  public removeAll() {
    const entityTypesArray = this.autoCommitSettingsForm.get('entityTypes') as UntypedFormArray;
    entityTypesArray.clear();
    this.autoCommitSettingsForm.updateValueAndValidity();
    this.autoCommitSettingsForm.markAsDirty();
  }

  entityTypeText(entityTypeControl: AbstractControl): SafeHtml {
    const entityType: EntityType = entityTypeControl.get('entityType').value;
    const config: AutoVersionCreateConfig = entityTypeControl.get('config').value;
    let message = entityType ? this.translate.instant(entityType === EntityType.USER ? 'entity-group.user-groups'
      : entityTypeTranslations.get(entityType).typePlural) : 'Undefined';
    let branchName;
    if (config.branch) {
      branchName = config.branch;
    } else {
      branchName = this.translate.instant('version-control.default');
    }
    message += ` (<small>${this.translate.instant('version-control.auto-commit-to-branch', {branch: branchName})}</small>)`;
    return this.sanitizer.bypassSecurityTrustHtml(message);
  }

  allowedEntityTypes(entityTypeControl?: AbstractControl): Array<EntityType> {
    let res = [...exportableEntityTypes];
    const currentEntityType: EntityType = entityTypeControl?.get('entityType')?.value;
    const value: [{entityType: string, config: EntityTypeVersionCreateConfig}] =
      this.autoCommitSettingsForm.get('entityTypes').value || [];
    const usedEntityTypes = value.map(val => val.entityType).filter(val => val);
    res = res.filter(entityType => !usedEntityTypes.includes(entityType) || entityType === currentEntityType);
    return res;
  }

  save(): void {
    const value: [{entityType: string, config: AutoVersionCreateConfig}] =
      this.autoCommitSettingsForm.get('entityTypes').value || [];
    const settings: AutoCommitSettings = {};
    if (value && value.length) {
      value.forEach((val) => {
        settings[val.entityType] = val.config;
      });
    }
    this.adminService.saveAutoCommitSettings(settings).subscribe(
      (savedSettings) => {
        this.settings = savedSettings;
        this.autoCommitSettingsForm.setControl('entityTypes',
          this.prepareEntityTypesFormArray(savedSettings), {emitEvent: false});
        this.autoCommitSettingsForm.markAsPristine();
      }
    );
  }

  delete(formDirective: FormGroupDirective): void {
    this.dialogService.confirm(
      this.translate.instant('admin.delete-auto-commit-settings-title', ),
      this.translate.instant('admin.delete-auto-commit-settings-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.adminService.deleteAutoCommitSettings().subscribe(
          () => {
            this.settings = null;
            this.autoCommitSettingsForm.setControl('entityTypes',
              this.prepareEntityTypesFormArray(this.settings), {emitEvent: false});
            this.autoCommitSettingsForm.markAsPristine();
          }
        );
      }
    });
  }

  private prepareEntityTypesFormArray(settings: AutoCommitSettings | null): UntypedFormArray {
    const entityTypesControls: Array<AbstractControl> = [];
    if (settings) {
      for (const entityType of Object.keys(settings)) {
        const config = settings[entityType];
        entityTypesControls.push(this.createEntityTypeControl(entityType as EntityType, config));
      }
    }
    return this.fb.array(entityTypesControls);
  }

  private createEntityTypeControl(entityType: EntityType, config: AutoVersionCreateConfig): AbstractControl {
    const entityTypeControl = this.fb.group(
      {
        entityType: [entityType, [Validators.required]],
        config: this.fb.group({
          branch: [config.branch, []],
          saveRelations: [config.saveRelations, []],
          saveAttributes: [config.saveAttributes, []],
          saveCredentials: [config.saveCredentials, []],
          savePermissions: [config.savePermissions, []],
          saveGroupEntities: [config.saveGroupEntities, []]
        })
      }
    );
    return entityTypeControl;
  }


}
