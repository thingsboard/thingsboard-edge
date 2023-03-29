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

import { Component, Inject, OnDestroy, SkipSelf, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormGroupDirective,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  createDeviceProfileConfiguration,
  createDeviceProfileTransportConfiguration,
  Device,
  DeviceProfile,
  DeviceProfileInfo,
  DeviceProfileType,
  DeviceProvisionConfiguration,
  DeviceProvisionType,
  DeviceTransportType,
  deviceTransportTypeHintMap,
  deviceTransportTypeTranslationMap
} from '@shared/models/device.models';
import { MatStepper } from '@angular/material/stepper';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable, of, Subscription, throwError } from 'rxjs';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';
import { DeviceService } from '@core/http/device.service';
import { ErrorStateMatcher } from '@angular/material/core';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { CustomerId } from '@shared/models/id/customer-id';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { ServiceType } from '@shared/models/queue.models';
import { deepTrim } from '@core/utils';
import { EntityGroup } from '@shared/models/entity-group.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { OwnerAndGroupsData } from '@shared/components/group/owner-and-groups.component';

export interface DeviceWizardDialogData {
  customerId?: string;
  entityGroup?: EntityGroup;
}

@Component({
  selector: 'tb-device-wizard',
  templateUrl: './device-wizard-dialog.component.html',
  providers: [],
  styleUrls: ['./device-wizard-dialog.component.scss']
})
export class DeviceWizardDialogComponent extends
  DialogComponent<DeviceWizardDialogComponent, Device> implements OnDestroy, ErrorStateMatcher {

  @ViewChild('addDeviceWizardStepper', {static: true}) addDeviceWizardStepper: MatStepper;

  resource = Resource;

  operation = Operation;

  selectedIndex = 0;

  showNext = true;

  createProfile = false;

  entityType = EntityType;

  deviceTransportTypes = Object.values(DeviceTransportType);

  deviceTransportTypeTranslations = deviceTransportTypeTranslationMap;

  deviceTransportTypeHints = deviceTransportTypeHintMap;

  deviceWizardFormGroup: UntypedFormGroup;

  transportConfigFormGroup: UntypedFormGroup;

  alarmRulesFormGroup: UntypedFormGroup;

  provisionConfigFormGroup: UntypedFormGroup;

  credentialsFormGroup: UntypedFormGroup;

  ownerAndGroupsFormGroup: UntypedFormGroup;

  labelPosition: MatStepper['labelPosition'] = 'end';

  entityGroup = this.data.entityGroup;

  customerId = this.data.customerId;

  initialOwnerId: EntityId;
  initialGroups: EntityInfoData[];

  serviceType = ServiceType.TB_RULE_ENGINE;

  private subscriptions: Subscription[] = [];
  private currentDeviceProfileTransportType = DeviceTransportType.DEFAULT;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceWizardDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DeviceWizardDialogComponent, Device>,
              private deviceProfileService: DeviceProfileService,
              private deviceService: DeviceService,
              private userPermissionsService: UserPermissionsService,
              private breakpointObserver: BreakpointObserver,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.deviceWizardFormGroup = this.fb.group({
        name: ['', [Validators.required, Validators.maxLength(255)]],
        label: ['', Validators.maxLength(255)],
        gateway: [false],
        overwriteActivityTime: [false],
        addProfileType: [0],
        deviceProfileId: [null, Validators.required],
        newDeviceProfileTitle: [{value: null, disabled: true}],
        defaultRuleChainId: [{value: null, disabled: true}],
        defaultQueueName: [{value: null, disabled: true}],
        description: ['']
      }
    );

    this.subscriptions.push(this.deviceWizardFormGroup.get('addProfileType').valueChanges.subscribe(
      (addProfileType: number) => {
        if (addProfileType === 0) {
          this.deviceWizardFormGroup.get('deviceProfileId').setValidators([Validators.required]);
          this.deviceWizardFormGroup.get('deviceProfileId').enable();
          this.deviceWizardFormGroup.get('newDeviceProfileTitle').setValidators(null);
          this.deviceWizardFormGroup.get('newDeviceProfileTitle').disable();
          this.deviceWizardFormGroup.get('defaultRuleChainId').disable();
          this.deviceWizardFormGroup.get('defaultQueueName').disable();
          this.deviceWizardFormGroup.updateValueAndValidity();
          this.createProfile = false;
        } else {
          this.deviceWizardFormGroup.get('deviceProfileId').setValidators(null);
          this.deviceWizardFormGroup.get('deviceProfileId').disable();
          this.deviceWizardFormGroup.get('newDeviceProfileTitle').setValidators([Validators.required]);
          this.deviceWizardFormGroup.get('newDeviceProfileTitle').enable();
          this.deviceWizardFormGroup.get('defaultRuleChainId').enable();
          this.deviceWizardFormGroup.get('defaultQueueName').enable();

          this.deviceWizardFormGroup.updateValueAndValidity();
          this.createProfile = true;
        }
      }
    ));

    this.transportConfigFormGroup = this.fb.group(
      {
        transportType: [DeviceTransportType.DEFAULT, Validators.required],
        transportConfiguration: [createDeviceProfileTransportConfiguration(DeviceTransportType.DEFAULT), Validators.required]
      }
    );

    this.subscriptions.push(this.transportConfigFormGroup.get('transportType').valueChanges.subscribe((transportType) => {
      this.deviceProfileTransportTypeChanged(transportType);
    }));

    this.alarmRulesFormGroup = this.fb.group({
        alarms: [null]
      }
    );

    this.provisionConfigFormGroup = this.fb.group(
      {
        provisionConfiguration: [{
          type: DeviceProvisionType.DISABLED
        } as DeviceProvisionConfiguration, [Validators.required]]
      }
    );

    this.credentialsFormGroup  = this.fb.group({
        setCredential: [false],
        credential: [{value: null, disabled: true}]
      }
    );

    this.subscriptions.push(this.credentialsFormGroup.get('setCredential').valueChanges.subscribe((value) => {
      if (value) {
        this.credentialsFormGroup.get('credential').enable();
      } else {
        this.credentialsFormGroup.get('credential').disable();
      }
    }));

    this.initialGroups = [];
    if (this.entityGroup) {
      this.initialOwnerId = this.entityGroup.ownerId;
      if (!this.entityGroup.groupAll) {
        this.initialGroups = [{id: this.entityGroup.id, name: this.entityGroup.name}];
      }
    } else {
      if (this.customerId) {
        this.initialOwnerId = new CustomerId(this.customerId);
      } else {
        this.initialOwnerId = this.userPermissionsService.getUserOwnerId();
      }
    }

    const ownerAndGroups: OwnerAndGroupsData = {
      owner: this.initialOwnerId,
      groups: this.initialGroups
    };

    this.ownerAndGroupsFormGroup = this.fb.group({
      ownerAndGroups: [ownerAndGroups, [Validators.required]]
    });

    this.labelPosition = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']) ? 'end' : 'bottom';

    this.subscriptions.push(this.breakpointObserver
      .observe(MediaBreakpoints['gt-sm'])
      .subscribe((state: BreakpointState) => {
          if (state.matches) {
            this.labelPosition = 'end';
          } else {
            this.labelPosition = 'bottom';
          }
        }
      ));
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep(): void {
    this.addDeviceWizardStepper.previous();
  }

  nextStep(): void {
    this.addDeviceWizardStepper.next();
  }

  getFormLabel(index: number): string {
    if (index > 0) {
      if (!this.createProfile) {
        index += 3;
      }
    }
    switch (index) {
      case 0:
        return 'device.wizard.device-details';
      case 1:
        return 'device-profile.transport-configuration';
      case 2:
        return 'device-profile.alarm-rules';
      case 3:
        return 'device-profile.device-provisioning';
      case 4:
        return 'device.credentials';
      case 5:
        return 'entity-group.owner-and-groups';
    }
  }

  get maxStepperIndex(): number {
    return this.addDeviceWizardStepper?._steps?.length - 1;
  }

  private deviceProfileTransportTypeChanged(deviceTransportType: DeviceTransportType): void {
    this.transportConfigFormGroup.patchValue(
      {transportConfiguration: createDeviceProfileTransportConfiguration(deviceTransportType)});
    const setCredentialBox = this.credentialsFormGroup.get('setCredential');
    if (deviceTransportType === DeviceTransportType.LWM2M) {
      setCredentialBox.patchValue(true);
      setCredentialBox.disable();
    } else {
      setCredentialBox.patchValue(false);
      setCredentialBox.enable();
    }
  }

  add(): void {
    if (this.allValid()) {
      this.createDeviceProfile().pipe(
        mergeMap(profileId => this.createDevice(profileId)),
        mergeMap(device => this.saveCredentials(device))
      ).subscribe(
        (device) => {
          this.dialogRef.close(device);
        }
      );
    }
  }

  get deviceTransportType(): DeviceTransportType {
    if (this.deviceWizardFormGroup.get('addProfileType').value) {
      return this.transportConfigFormGroup.get('transportType').value;
    } else {
      return this.currentDeviceProfileTransportType;
    }
  }

  deviceProfileChanged(deviceProfile: DeviceProfileInfo) {
    if (deviceProfile) {
      this.currentDeviceProfileTransportType = deviceProfile.transportType;
    }
  }

  private createDeviceProfile(): Observable<EntityId> {
    if (this.deviceWizardFormGroup.get('addProfileType').value) {
      const deviceProvisionConfiguration: DeviceProvisionConfiguration = this.provisionConfigFormGroup.get('provisionConfiguration').value;
      const provisionDeviceKey = deviceProvisionConfiguration.provisionDeviceKey;
      delete deviceProvisionConfiguration.provisionDeviceKey;
      const deviceProfile: DeviceProfile = {
        name: this.deviceWizardFormGroup.get('newDeviceProfileTitle').value,
        type: DeviceProfileType.DEFAULT,
        defaultQueueName: this.deviceWizardFormGroup.get('defaultQueueName').value,
        transportType: this.transportConfigFormGroup.get('transportType').value,
        provisionType: deviceProvisionConfiguration.type,
        provisionDeviceKey,
        profileData: {
          configuration: createDeviceProfileConfiguration(DeviceProfileType.DEFAULT),
          transportConfiguration: this.transportConfigFormGroup.get('transportConfiguration').value,
          alarms: this.alarmRulesFormGroup.get('alarms').value,
          provisionConfiguration: deviceProvisionConfiguration
        }
      };
      if (this.deviceWizardFormGroup.get('defaultRuleChainId').value) {
        deviceProfile.defaultRuleChainId = new RuleChainId(this.deviceWizardFormGroup.get('defaultRuleChainId').value);
      }
      return this.deviceProfileService.saveDeviceProfile(deepTrim(deviceProfile)).pipe(
        tap((profile) => {
          this.currentDeviceProfileTransportType = profile.transportType;
          this.deviceWizardFormGroup.patchValue({
            deviceProfileId: profile.id,
            addProfileType: 0
          });
        }),
        map(profile => profile.id)
      );
    } else {
      return of(this.deviceWizardFormGroup.get('deviceProfileId').value);
    }
  }

  private createDevice(profileId): Observable<Device> {
    const device = {
      name: this.deviceWizardFormGroup.get('name').value,
      label: this.deviceWizardFormGroup.get('label').value,
      deviceProfileId: profileId,
      additionalInfo: {
        gateway: this.deviceWizardFormGroup.get('gateway').value,
        overwriteActivityTime: this.deviceWizardFormGroup.get('overwriteActivityTime').value,
        description: this.deviceWizardFormGroup.get('description').value
      },
      customerId: null
    } as Device;
    const targetOwnerAndGroups: OwnerAndGroupsData = this.ownerAndGroupsFormGroup.get('ownerAndGroups').value;
    const targetOwner = targetOwnerAndGroups.owner;
    let targetOwnerId: EntityId;
    if ((targetOwner as EntityInfoData).name) {
      targetOwnerId = (targetOwner as EntityInfoData).id;
    } else {
      targetOwnerId = targetOwner as EntityId;
    }
    if (targetOwnerId.entityType === EntityType.CUSTOMER) {
      device.customerId = targetOwnerId as CustomerId;
    }
    const entityGroupIds = targetOwnerAndGroups.groups.map(group => group.id.id);
    return this.deviceService.saveDevice(deepTrim(device), entityGroupIds).pipe(
      catchError(e => {
        this.addDeviceWizardStepper.selectedIndex = 0;
        return throwError(e);
      })
    );
  }

  private saveCredentials(device: Device): Observable<Device> {
    if (this.credentialsFormGroup.get('setCredential').value) {
      return this.deviceService.getDeviceCredentials(device.id.id).pipe(
        mergeMap(
          (deviceCredentials) => {
            const deviceCredentialsValue = {...deviceCredentials, ...this.credentialsFormGroup.value.credential};
            return this.deviceService.saveDeviceCredentials(deviceCredentialsValue).pipe(
              catchError(e => {
                this.addDeviceWizardStepper.selectedIndex = 1;
                return this.deviceService.deleteDevice(device.id.id).pipe(
                  mergeMap(() => throwError(e)
                ));
              })
            );
          }
        ),
        map(() => device));
    }
    return of(device);
  }

  allValid(): boolean {
    if (this.addDeviceWizardStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addDeviceWizardStepper.selectedIndex = index;
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

  changeStep($event: StepperSelectionEvent): void {
    this.selectedIndex = $event.selectedIndex;
    this.showNext = this.selectedIndex !== this.maxStepperIndex;
  }
}
