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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  NgZone,
  OnDestroy,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { Subject } from 'rxjs';
import { Overlay } from '@angular/cdk/overlay';
import { UtilsService } from '@core/services/utils.service';
import { EntityService } from '@core/http/entity.service';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  BrokerSecurityType,
  BrokerSecurityTypeTranslationsMap,
  noLeadTrailSpacesRegex
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-broker-security',
  templateUrl: './broker-security.component.html',
  styleUrls: ['./broker-security.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => BrokerSecurityComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => BrokerSecurityComponent),
      multi: true
    }
  ]
})
export class BrokerSecurityComponent extends PageComponent implements ControlValueAccessor, Validator, OnDestroy {

  BrokerSecurityType = BrokerSecurityType;

  securityTypes = Object.values(BrokerSecurityType);

  SecurityTypeTranslationsMap = BrokerSecurityTypeTranslationsMap;

  securityFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => {};

  constructor(protected store: Store<AppState>,
              public translate: TranslateService,
              public dialog: MatDialog,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private dialogService: DialogService,
              private entityService: EntityService,
              private utils: UtilsService,
              private zone: NgZone,
              private cd: ChangeDetectorRef,
              private elementRef: ElementRef,
              private fb: FormBuilder) {
    super(store);
    this.securityFormGroup = this.fb.group({
      type: [BrokerSecurityType.ANONYMOUS, []],
      username: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      password: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToCACert: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToPrivateKey: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToClientCert: ['', [Validators.pattern(noLeadTrailSpacesRegex)]]
    });
    this.securityFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
    this.securityFormGroup.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      this.updateValidators(type);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  writeValue(deviceInfo: any) {
    if (!deviceInfo.type) {
      deviceInfo.type = BrokerSecurityType.ANONYMOUS;
    }
    this.securityFormGroup.reset(deviceInfo);
    this.updateView(deviceInfo);
  }

  validate(): ValidationErrors | null {
    return this.securityFormGroup.valid ? null : {
      securityForm: { valid: false }
    };
  }

  updateView(value: any) {
    this.propagateChange(value);
  }

  private updateValidators(type) {
    if (type) {
      this.securityFormGroup.get('username').disable({emitEvent: false});
      this.securityFormGroup.get('password').disable({emitEvent: false});
      this.securityFormGroup.get('pathToCACert').disable({emitEvent: false});
      this.securityFormGroup.get('pathToPrivateKey').disable({emitEvent: false});
      this.securityFormGroup.get('pathToClientCert').disable({emitEvent: false});
      if (type === BrokerSecurityType.BASIC) {
        this.securityFormGroup.get('username').enable({emitEvent: false});
        this.securityFormGroup.get('password').enable({emitEvent: false});
      } else if (type === BrokerSecurityType.CERTIFICATES) {
        this.securityFormGroup.get('pathToCACert').enable({emitEvent: false});
        this.securityFormGroup.get('pathToPrivateKey').enable({emitEvent: false});
        this.securityFormGroup.get('pathToClientCert').enable({emitEvent: false});
      }
    }
  }
}
