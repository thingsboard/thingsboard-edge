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

import { AfterViewInit, Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  getIntegrationHelpLink,
  Integration,
  IntegrationType,
  integrationTypeInfoMap
} from '@shared/models/integration.models';
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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Converter, ConverterType } from '@shared/models/converter.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { deepTrim, guid } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ConverterService } from '@core/http/converter.service';
import { IntegrationService } from '@core/http/integration.service';
import { ConverterId } from '@shared/models/id/converter-id';

export interface IntegrationWizardData<T> extends AddEntityDialogData<T>{
  edgeTemplate: boolean;
}

@Component({
  selector: 'tb-integration-wizard',
  templateUrl: './integration-wizard-dialog.component.html',
  styleUrls: ['./integration-wizard-dialog.component.scss'],
  providers: []
})
export class IntegrationWizardDialogComponent extends
  DialogComponent<IntegrationWizardDialogComponent, Integration> implements AfterViewInit, OnDestroy {

  @ViewChild('addIntegrationWizardStepper', {static: true}) addIntegrationWizardStepper: MatStepper;
  @ViewChild('uplinkDataConverter', {static: true}) uplinkDataConverterComponent: ConverterComponent;
  @ViewChild('downlinkDataConverter') downlinkDataConverterComponent: ConverterComponent;

  selectedIndex = 0;
  converterType = ConverterType;
  isEdgeTemplate = false;
  showCheckConnection = false;
  integrationType = '';
  showCheckSuccess = false;
  checkErrMsg = '';
  showDownlinkStep = true;

  stepperOrientation: Observable<StepperOrientation>;

  integrationWizardForm: UntypedFormGroup;
  uplinkConverterForm: UntypedFormGroup;
  downlinkConverterForm: UntypedFormGroup;
  integrationConfigurationForm: UntypedFormGroup;

  uplinkConverter = {
    type: ConverterType.UPLINK
  } as Converter;

  downlinkConverter = {
    type: ConverterType.DOWNLINK
  } as Converter;

  private checkConnectionAllow = false;
  private destroy$ = new Subject();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: IntegrationWizardData<Integration>,
              public dialogRef: MatDialogRef<IntegrationWizardDialogComponent, Integration>,
              private breakpointObserver: BreakpointObserver,
              private converterService: ConverterService,
              private integrationService: IntegrationService,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.isEdgeTemplate = this.data.edgeTemplate;

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-xs'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.integrationWizardForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255), Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      type: [null, [Validators.required]],
      enabled: [true],
      debugMode: [true],
      allowCreateDevicesOrAssets: [true],
    });

    this.integrationWizardForm.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: IntegrationType) => {
      if (integrationTypeInfoMap.has(value)) {
        this.integrationType = this.translate.instant(integrationTypeInfoMap.get(value).name);
        this.checkConnectionAllow = integrationTypeInfoMap.get(value).checkConnection || false;
        this.showDownlinkStep = !integrationTypeInfoMap.get(value).hideDownlink;
        if (integrationTypeInfoMap.get(value).remote) {
          this.integrationConfigurationForm.get('remote').disable({emitEvent: true});
          this.integrationConfigurationForm.get('remote').setValue(true, {emitEvent: true});
        } else {
          this.integrationConfigurationForm.get('remote').enable({emitEvent: true});
          this.integrationConfigurationForm.get('remote').setValue(false, {emitEvent: true});
        }
      } else {
        this.integrationType = '';
      }
      this.integrationConfigurationForm.get('configuration').setValue(null);
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
      routingKey: [{value: guid(), disabled: true}],
      secret: [{value: this.generateSecret(20), disabled: true}],
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

  get helpLinkId(): string {
    return getIntegrationHelpLink(this.integrationWizardForm.value);
  }

  private createUplinkConverter(): Observable<ConverterId> {
    if (this.uplinkConverterForm.get('converterType').value === 'exist') {
      return of(this.uplinkConverterForm.get('uplinkConverterId').value);
    } else {
      const converterConfig: Converter = deepTrim(this.uplinkConverterForm.get('newUplinkConverter').value);
      converterConfig.edgeTemplate = this.data.edgeTemplate;
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
    if (!this.showDownlinkStep || this.downlinkConverterForm.get('downlinkConverterId').pristine &&
      this.downlinkConverterForm.get('newDownlinkConverter').pristine) {
      return of(null);
    } else if (this.downlinkConverterForm.get('converterType').value === 'exist') {
      return of(this.downlinkConverterForm.get('downlinkConverterId').value);
    } else {
      const converterConfig: Converter = deepTrim(this.downlinkConverterForm.get('newDownlinkConverter').value);
      converterConfig.edgeTemplate = this.data.edgeTemplate;
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

  private getIntegrationData(uplinkConverterId?: ConverterId, downlinkConverterId?: ConverterId): Integration {
    const integrationData: Integration = {
      configuration: {
        metadata: this.integrationConfigurationForm.value.metadata,
        ...this.integrationConfigurationForm.value.configuration,
      },
      routingKey: this.integrationConfigurationForm.getRawValue().routingKey,
      secret: this.integrationConfigurationForm.getRawValue().secret,
      remote: this.integrationConfigurationForm.getRawValue().remote,
      defaultConverterId: uplinkConverterId,
      downlinkConverterId,
      name: this.integrationWizardForm.value.name.trim(),
      type: this.integrationWizardForm.value.type,
      enabled: this.integrationWizardForm.value.enabled,
      debugMode: this.integrationWizardForm.value.debugMode,
      allowCreateDevicesOrAssets: this.integrationWizardForm.value.allowCreateDevicesOrAssets,
      edgeTemplate: this.data.edgeTemplate
    };
    if (this.integrationConfigurationForm.value.additionalInfo.description) {
      integrationData.additionalInfo = {
        description: this.integrationConfigurationForm.value.additionalInfo.description
      };
    }
    return integrationData;
  }

  private createdIntegration(uplinkConverterId: ConverterId, downlinkConverterId: ConverterId): Observable<Integration> {
    const integrationData = this.getIntegrationData(uplinkConverterId, downlinkConverterId);
    return this.integrationService.saveIntegration(integrationData);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  private get maxStep(): number {
    return this.showDownlinkStep ? 3 : 2;
  }

  changeStep($event: StepperSelectionEvent) {
    this.selectedIndex = $event.selectedIndex;
    if (this.isConnectionStep) {
      this.showCheckConnection = false;
    }
  }

  nextStep() {
    if (this.selectedIndex >= this.maxStep) {
      this.add();
    } else {
      this.addIntegrationWizardStepper.next();
    }
  }

  nextStepLabel(): string {
    if (this.showDownlinkStep && this.selectedIndex === 2 && this.downlinkConverterForm.get('downlinkConverterId').pristine &&
      this.downlinkConverterForm.get('newDownlinkConverter').pristine) {
      return 'action.skip';
    }
    if (this.selectedIndex >= this.maxStep) {
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
        horizontalPosition: 'right',
        target: 'integrationRoot'
      }));
  }

  onIntegrationCheck(): void {
    if (this.allValid()) {
      this.showCheckConnection = true;
      this.showCheckSuccess = false;
      this.checkErrMsg = '';
      setTimeout(() => {
        this.addIntegrationWizardStepper.next();
      }, 0);
      const integrationData = this.getIntegrationData(null, null);
      this.integrationService.checkIntegrationConnection(integrationData, {
        ignoreErrors: true,
        ignoreLoading: true
      }).subscribe(() => {
        this.showCheckSuccess = true;
      }, (error) => {
        this.checkErrMsg = error.error.message;
      });
    }
  }

  get isCheckConnectionAvailable(): boolean {
    return !this.isEdgeTemplate && this.checkConnectionAllow && !this.isRemoteIntegration && this.isConnectionStep;
  }

  private get isRemoteIntegration(): boolean {
    return this.integrationConfigurationForm.value.remote;
  }

  private get isConnectionStep(): boolean {
    return this.selectedIndex === this.maxStep;
  }

  private generateSecret(length: number = 1): string {
    const l = length > 10 ? 10 : length;
    const str = Math.random().toString(36).substr(2, l);
    if (str.length >= length) {
      return str;
    }
    return str.concat(this.generateSecret(length - str.length));
  }

  private allValid(): boolean {
    if (this.addIntegrationWizardStepper.steps.find((item, index) => {
      if (item.stepControl?.invalid) {
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
