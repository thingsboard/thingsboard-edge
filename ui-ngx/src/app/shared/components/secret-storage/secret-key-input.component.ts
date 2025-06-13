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

import {
  booleanAttribute,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import {
  SecretStorageData,
  SecretStorageDialogComponent
} from '@shared/components/secret-storage/secret-storage-dialog.component';
import { parseSecret, SecretStorageType } from '@shared/models/secret-storage.models';
import { SecretStorageService } from '@core/http/secret-storage.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
  selector: 'tb-secret-key-input',
  templateUrl: './secret-key-input.component.html',
  styleUrls: ['./secret-key-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecretKeyInputComponent),
      multi: true
    }
  ]
})
export class SecretKeyInputComponent extends PageComponent implements OnInit, ControlValueAccessor, OnChanges {

  @Input()
  label: string;

  @Input()
  requiredText: string;

  @Input({transform: booleanAttribute})
  required: boolean = false;

  @Input()
  disabled: boolean;

  @Input({transform: booleanAttribute})
  readonly = false;

  secretStorageKey: string;

  private modelValue: string;

  private propagateChange = null;

  public secretKeyFormGroup: UntypedFormGroup;

  constructor(private secretStorageService: SecretStorageService,
              private userPermissionsService: UserPermissionsService,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.readonly = this.readonly || !this.userPermissionsService.hasGenericPermission(Resource.SECRET, Operation.WRITE);
    this.secretKeyFormGroup = this.fb.group({
      secretKey: [null, this.required ? [Validators.required] : []]
    });

    this.secretKeyFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.required) {
      const requiredChanges = changes.required;
      if (!requiredChanges.firstChange && requiredChanges.currentValue !== requiredChanges.previousValue) {
        this.updateValidators();
      }
    }
  }

  private updateValidators() {
    if (this.secretKeyFormGroup) {
      this.secretKeyFormGroup.get('secretKey').setValidators(this.required ? [Validators.required] : []);
      this.secretKeyFormGroup.get('secretKey').updateValueAndValidity();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.secretKeyFormGroup.disable({emitEvent: false});
    } else {
      this.secretKeyFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    const parsedSecret = parseSecret(value);
    this.modelValue = value;
    if (parsedSecret) {
      this.secretStorageService.getSecretByName(parsedSecret, {ignoreErrors: true}).subscribe({
        next: () => {
          this.secretStorageKey = parsedSecret;
          this.secretKeyFormGroup.patchValue(
            {secretKey: this.modelValue}, {emitEvent: false}
          );
        },
        error: () => {
          this.secretStorageKey = null;
          this.secretKeyFormGroup.patchValue(
            {secretKey: null}, {emitEvent: true}
          );
          this.secretKeyFormGroup.get('secretKey').markAsTouched();
        }
      })
    } else {
      this.secretStorageKey = null;
      this.secretKeyFormGroup.patchValue(
        { secretKey: this.modelValue }, {emitEvent: false}
      );
    }
  }

  private updateModel() {
    const secretKey: string = this.secretKeyFormGroup.get('secretKey').value;
    this.secretStorageKey = parseSecret(secretKey);
    if (this.modelValue !== secretKey) {
      this.modelValue = secretKey;
      this.propagateChange(this.modelValue);
    }
  }

  remove($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.secretKeyFormGroup.get('secretKey').patchValue(null, {emitEvent: true});
    this.secretKeyFormGroup.get('secretKey').markAsTouched();
  }

  openSecretKeyDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SecretStorageDialogComponent, SecretStorageData, string>(SecretStorageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        type: SecretStorageType.TEXT,
        value: this.secretKeyFormGroup.get('secretKey').value,
        hideType: true
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.secretKeyFormGroup.get('secretKey').patchValue(res, {emitEvent: true});
        }
      });
  }
}
