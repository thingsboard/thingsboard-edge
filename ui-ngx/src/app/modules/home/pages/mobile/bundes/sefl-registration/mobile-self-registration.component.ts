///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Renderer2, ViewContainerRef } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { EditorPanelComponent } from '@home/pages/mobile/common/editor-panel.component';
import { EntityType } from '@shared/models/entity-type.models';
import { CMAssigneeType, CMScope } from '@app/shared/models/custom-menu.models';
import { GroupPermission } from '@shared/models/group-permission.models';
import {
  defaultSignUpFields,
  MobileSelfRegistrationParams,
  SelfRegistrationType,
  SignUpFieldMap
} from '@shared/models/self-register.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-mobile-self-registration',
  templateUrl: './mobile-self-registration.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MobileSelfRegistrationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MobileSelfRegistrationComponent),
      multi: true
    }
  ],
})
export class MobileSelfRegistrationComponent implements ControlValueAccessor, Validator {

  selfRegistrationForm = this.fb.group({
    enabled: [false],
    title: ['', Validators.required],
    notificationEmail: ['', [Validators.required, Validators.email]],
    redirect: this.fb.group({
      scheme: ['', Validators.required],
      host: ['', Validators.required]
    }),
    signUpFields: [null],
    captcha: this.fb.group({
      version: ['v3'],
      siteKey: ['', Validators.required],
      secretKey: ['', Validators.required],
      logActionName: ['']
    }),
    showPrivacyPolicy: [true],
    showTermsOfUse: [true],
    termsOfUse: [''],
    privacyPolicy: [''],
    permissions: [([] as Array<GroupPermission>)],
    customerGroupId: [null],
    customerTitlePrefix: [''],
    customMenuId: [null],
    defaultDashboard: this.fb.group({
      id: [null],
      fullscreen: [false]
    }),
    homeDashboard: this.fb.group({
      id: [null],
      hideToolbar: [true]
    })
  });

  EntityType = EntityType;
  CMScope = CMScope;
  CMAssigneeType = CMAssigneeType;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private translate: TranslateService) {
    this.selfRegistrationForm.get('enabled').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if(value) {
        this.selfRegistrationForm.enable({emitEvent: false});
      } else {
        this.selfRegistrationForm.disable({emitEvent: false});
        this.selfRegistrationForm.get('enabled').enable({emitEvent: false});
      }
    })

    this.selfRegistrationForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => this.updateModel());
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.selfRegistrationForm.disable({emitEvent: false});
    } else {
      this.selfRegistrationForm.get('enabled').updateValueAndValidity({onlySelf: true});
    }
  }

  writeValue(params: MobileSelfRegistrationParams) {
    if (!isDefinedAndNotNull(params)) {
      if (params === null) {
        params = {} as MobileSelfRegistrationParams;
      }
      params.enabled = true;
      params.signUpFields = deepClone(defaultSignUpFields).map(item => {
        const field = SignUpFieldMap.get(item);
        field.label = this.translate.instant(field.label);
        return field;
      });
    }
    this.selfRegistrationForm.patchValue(params, {emitEvent: false});
    this.selfRegistrationForm.get('enabled').updateValueAndValidity({onlySelf: true});
  }

  validate(): ValidationErrors | null {
    if (!this.selfRegistrationForm.valid) {
      return {
        invalidSelfRegistrationForm: true
      };
    }
    return null;
  }

  editPolicy($event: Event, matButton: MatButton, isPrivacy = false) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        disabled: this.selfRegistrationForm.disabled,
        title: isPrivacy ? 'self-registration.privacy-policy-text' : 'self-registration.terms-of-use-text',
        releaseNotes: isPrivacy
          ? this.selfRegistrationForm.get('privacyPolicy').value
          : this.selfRegistrationForm.get('termsOfUse').value
      };
      const editorPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, EditorPanelComponent, ['leftOnly', 'leftBottomOnly', 'leftTopOnly'], true, null,
        ctx,
        {},
        {}, {}, false, () => {}, {padding: '16px 24px'});
      editorPanelPopover.tbComponentRef.instance.popover = editorPanelPopover;
      editorPanelPopover.tbComponentRef.instance.editorContentApplied.subscribe((content) => {
        editorPanelPopover.hide();
        if (isPrivacy) {
          this.selfRegistrationForm.get('privacyPolicy').setValue(content);
          this.selfRegistrationForm.get('privacyPolicy').markAsDirty();
        } else {
          this.selfRegistrationForm.get('termsOfUse').setValue(content);
          this.selfRegistrationForm.get('termsOfUse').markAsDirty();
        }
      });
    }
  }

  private updateModel() {
    const value =  this.selfRegistrationForm.getRawValue() as MobileSelfRegistrationParams;
    value.type = SelfRegistrationType.MOBILE;
    this.propagateChange(value);
  }
}
