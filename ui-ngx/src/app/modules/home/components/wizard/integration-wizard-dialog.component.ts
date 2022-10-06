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

import { AfterViewInit, Component, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Integration } from '@shared/models/integration.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { MatStepper, StepperOrientation } from '@angular/material/stepper';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver } from '@angular/cdk/layout';
import { map, mergeMap, takeUntil, tap } from 'rxjs/operators';
import { forkJoin, Observable, of, Subject } from 'rxjs';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Converter, ConverterType } from '@shared/models/converter.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { guid, isUndefined } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ConverterService } from '@core/http/converter.service';
import { IntegrationService } from '@core/http/integration.service';
import { ConverterId } from '@shared/models/id/converter-id';

@Component({
  selector: 'tb-integration-wizard',
  templateUrl: './integration-wizard-dialog.component.html',
  styleUrls: ['./integration-wizard-dialog.component.scss'],
  providers: []
})
export class IntegrationWizardDialogComponent extends
  DialogComponent<IntegrationWizardDialogComponent, Integration> implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('addIntegrationWizardStepper', {static: true}) addIntegrationWizardStepper: MatStepper;
  @ViewChild('uplinkDataConverter', {static: true}) uplinkDataConverterComponent: ConverterComponent;
  @ViewChild('downlinkDataConverter', {static: true}) downlinkDataConverterComponent: ConverterComponent;

  selectedIndex = 0;
  showNext = true;
  converterType = ConverterType;

  stepperOrientation: Observable<StepperOrientation>;

  integrationWizardForm: FormGroup;
  uplinkConverterForm: FormGroup;
  downlinkConverterForm: FormGroup;
  integrationConfigurationForm: FormGroup;

  uplinkConverter = {
    type: ConverterType.UPLINK
  } as Converter;

  downlinkConverter = {
    type: ConverterType.DOWNLINK
  } as Converter;

  private destroy$ = new Subject();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEntityDialogData<Integration>,
              public dialogRef: MatDialogRef<IntegrationWizardDialogComponent, Integration>,
              private breakpointObserver: BreakpointObserver,
              private converterService: ConverterService,
              private integrationService: IntegrationService,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.integrationWizardForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      type: [null, [Validators.required]],
      enabled: [true],
      debugMode: [true],
      allowCreateDevicesOrAssets: [true],
    });

    this.uplinkConverterForm = this.fb.group({
      uplinkConverterId: [null, [Validators.required]],
      converterType: ['exist'],
      newUplinkConverter: [{
        value: {
          type: ConverterType.UPLINK
        },
        disable: true
      }]
    });

    this.downlinkConverterForm = this.fb.group({
      downlinkConverterId: [null],
      converterType: ['exist'],
      newDownlinkConverter: [{
        value: {
          type: ConverterType.DOWNLINK
        },
        disable: true
      }]
    });

    this.integrationConfigurationForm = this.fb.group({
      configuration: [{}, Validators.required],
      metadata: [{}],
      remote: [false],
      routingKey: this.fb.control({ value: guid(), disabled: true }),
      secret: this.fb.control({ value: this.generateSecret(20), disabled: true }),
      additionalInfo: this.fb.group(
        {
          description: ['']
        }
      )
    });

    this.uplinkConverterForm.get('converterType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (value === 'exist') {
        this.uplinkConverterForm.get('uplinkConverterId').enable({emitEvent: false});
        this.uplinkConverterForm.get('newUplinkConverter').disable({emitEvent: false});
      } else {
        this.uplinkConverterForm.get('uplinkConverterId').disable({emitEvent: false});
        this.uplinkConverterForm.get('newUplinkConverter').enable({emitEvent: false});
      }
    });

    this.downlinkConverterForm.get('converterType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (value === 'exist') {
        this.downlinkConverterForm.get('downlinkConverterId').enable({emitEvent: false});
        this.downlinkConverterForm.get('newDownlinkConverter').disable({emitEvent: false});
      } else {
        this.downlinkConverterForm.get('downlinkConverterId').disable({emitEvent: false});
        this.downlinkConverterForm.get('newDownlinkConverter').enable({emitEvent: false});
      }
    });
  }

  ngOnInit() {

  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.uplinkConverterForm.setControl('newUplinkConverter', this.uplinkDataConverterComponent.entityForm, {emitEvent: false});
      this.uplinkConverterForm.get('newUplinkConverter').disable({emitEvent: false});
      this.downlinkConverterForm.setControl('newDownlinkConverter', this.downlinkDataConverterComponent.entityForm, {emitEvent: false});
      this.downlinkConverterForm.get('newDownlinkConverter').disable({emitEvent: false});
    }, 0);
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  add(): void {
    if (this.allValid()) {
      forkJoin([
        this.createUplinkConverter(),
        this.createDownlinkConverter()
      ]).pipe(
        mergeMap(([uplinkId, downlinkId]) => this.createdIntegration(uplinkId, downlinkId))
      ).subscribe(
        (device) => {
          this.dialogRef.close(device);
        }
      );
    }
  }

  private createUplinkConverter(): Observable<ConverterId> {
    if (this.uplinkConverterForm.get('converterType').value === 'exist') {
      return of(this.uplinkConverterForm.get('uplinkConverterId').value);
    } else {
      const converterConfig: Converter = this.uplinkConverterForm.get('newUplinkConverter').value;
      return this.converterService.saveConverter(converterConfig).pipe(
        tap(converter => {
          this.uplinkConverterForm.patchValue({
            converterType: 'exist',
            uplinkConverterId: converter.id
          });
        }),
        map(converter => converter.id)
      );
    }
  }

  private createDownlinkConverter(): Observable<ConverterId> {
    if (this.downlinkConverterForm.get('downlinkConverterId').pristine &&
      this.downlinkConverterForm.get('newDownlinkConverter').pristine) {
      return of(null);
    } else if (this.downlinkConverterForm.get('converterType').value === 'exist') {
      return of(this.downlinkConverterForm.get('downlinkConverterId').value);
    } else {
      const converterConfig: Converter = this.downlinkConverterForm.get('newDownlinkConverter').value;
      return this.converterService.saveConverter(converterConfig).pipe(
        tap(converter => {
          this.downlinkConverterForm.patchValue({
            converterType: 'exist',
            downlinkConverterId: converter.id
          });
        }),
        map(converter => converter.id)
      );
    }
  }

  private createdIntegration(uplinkConverterId: ConverterId, downlinkConverterId: ConverterId): Observable<Integration> {
    const integrationData: Integration = {
      ...this.integrationConfigurationForm.value,
      routingKey: this.integrationConfigurationForm.getRawValue().routingKey,
      secret: this.integrationConfigurationForm.getRawValue().secret,
      defaultConverterId: uplinkConverterId,
      downlinkConverterId,
      name: this.integrationWizardForm.value.name,
      type: this.integrationWizardForm.value.type,
      enabled: this.integrationWizardForm.value.enabled,
      debugMode: this.integrationWizardForm.value.debugMode,
      allowCreateDevicesOrAssets: this.integrationWizardForm.value.allowCreateDevicesOrAssets,
    };
    return this.integrationService.saveIntegration(integrationData);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  changeStep($event: StepperSelectionEvent) {
    this.selectedIndex = $event.selectedIndex;
  }

  nextStep() {
    if (this.selectedIndex === this.maxStepperIndex) {
      this.add();
    } else {
      this.addIntegrationWizardStepper.next();
    }
  }

  nextStepLabel(): string {
    if (this.selectedIndex === 2 && this.downlinkConverterForm.get('downlinkConverterId').pristine &&
      this.downlinkConverterForm.get('newDownlinkConverter').pristine) {
      return 'action.skip';
    }
    if (this.selectedIndex === 3) {
      return 'action.add';
    }
    return 'action.next';
  }

  backStep() {
    this.addIntegrationWizardStepper.previous();
  }

  onIntegrationInfoCopied(type: string) {
    const message = type === 'key' ? 'integration.integration-key-copied-message'
      : 'integration.integration-secret-copied-message';
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant(message),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  private get maxStepperIndex(): number {
    return this.addIntegrationWizardStepper?._steps?.length - 1;
  }

  private generateSecret(length?: number): string {
    if (isUndefined(length) || length == null) {
      length = 1;
    }
    const l = length > 10 ? 10 : length;
    const str = Math.random().toString(36).substr(2, l);
    if (str.length >= length) {
      return str;
    }
    return str.concat(this.generateSecret(length - str.length));
  }

  private allValid(): boolean {
    if (this.addIntegrationWizardStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addIntegrationWizardStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    } )) {
      return false;
    } else {
      return true;
    }
  }
}
