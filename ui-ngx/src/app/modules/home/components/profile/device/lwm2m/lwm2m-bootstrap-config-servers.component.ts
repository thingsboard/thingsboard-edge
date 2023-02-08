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

import { Component, EventEmitter, forwardRef, Input, OnDestroy, OnInit, Output } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder, UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR
} from '@angular/forms';
import { of, Subject } from 'rxjs';
import { ServerSecurityConfig } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { Lwm2mBootstrapAddConfigServerDialogComponent } from '@home/components/profile/device/lwm2m/lwm2m-bootstrap-add-config-server-dialog.component';
import { mergeMap, takeUntil } from 'rxjs/operators';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { Lwm2mSecurityType } from '@shared/models/lwm2m-security-config.models';

@Component({
  selector: 'tb-profile-lwm2m-bootstrap-config-servers',
  templateUrl: './lwm2m-bootstrap-config-servers.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mBootstrapConfigServersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mBootstrapConfigServersComponent),
      multi: true,
    }
  ]
})
export class Lwm2mBootstrapConfigServersComponent implements OnInit, ControlValueAccessor, OnDestroy {

  bootstrapConfigServersFormGroup: UntypedFormGroup;

  @Input()
  disabled: boolean;

  @Input()
  isTransportWasRunWithBootstrap: boolean;

  @Output()
  isTransportWasRunWithBootstrapChange = new EventEmitter<boolean>();

  public isBootstrapServerUpdateEnableValue: boolean;
  @Input()
  set isBootstrapServerUpdateEnable(value: boolean) {
    this.isBootstrapServerUpdateEnableValue = value;
    if (!value) {
      this.removeBootstrapServerConfig();
    }
  }

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(public translate: TranslateService,
              public matDialog: MatDialog,
              private dialogService: DialogService,
              private deviceProfileService: DeviceProfileService,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.bootstrapConfigServersFormGroup = this.fb.group({
      serverConfigs: this.fb.array([])
    });
    this.bootstrapConfigServersFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get serverConfigsFromArray(): UntypedFormArray {
    return this.bootstrapConfigServersFormGroup.get('serverConfigs') as UntypedFormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.bootstrapConfigServersFormGroup.disable({emitEvent: false});
    } else {
      this.bootstrapConfigServersFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(serverConfigs: Array<ServerSecurityConfig> | null): void {
    if (serverConfigs?.length === this.serverConfigsFromArray.length) {
      this.serverConfigsFromArray.patchValue(serverConfigs, {emitEvent: false});
    } else {
      const serverConfigsControls: Array<AbstractControl> = [];
      if (serverConfigs) {
        serverConfigs.forEach((serverConfig) => {
          serverConfigsControls.push(this.fb.control(serverConfig));
        });
      }
      this.bootstrapConfigServersFormGroup.setControl('serverConfigs', this.fb.array(serverConfigsControls), {emitEvent: false});
      if (this.disabled) {
        this.bootstrapConfigServersFormGroup.disable({emitEvent: false});
      } else {
        this.bootstrapConfigServersFormGroup.enable({emitEvent: false});
      }
    }
  }

  trackByParams(index: number): number {
    return index;
  }

  removeServerConfig($event: Event, index: number) {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.dialogService.confirm(
      this.translate.instant('device-profile.lwm2m.delete-server-title'),
      this.translate.instant('device-profile.lwm2m.delete-server-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.serverConfigsFromArray.removeAt(index);
      }
    });
  }

  addServerConfig(): void {
    const addDialogObs = this.isBootstrapServerNotAvailable() ? of(false) :
      this.matDialog.open<Lwm2mBootstrapAddConfigServerDialogComponent>(Lwm2mBootstrapAddConfigServerDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
      }).afterClosed();
    const addServerConfigObs = addDialogObs.pipe(
      mergeMap((isBootstrap) => {
        if (isBootstrap === null) {
          return of(null);
        }
        return this.deviceProfileService.getLwm2mBootstrapSecurityInfoBySecurityType(isBootstrap, Lwm2mSecurityType.NO_SEC);
      })
    );
    addServerConfigObs.subscribe((serverConfig) => {
      if (serverConfig) {
        serverConfig.securityMode = Lwm2mSecurityType.NO_SEC;
        this.serverConfigsFromArray.push(this.fb.control(serverConfig));
        this.updateModel();
      } else {
        this.isTransportWasRunWithBootstrap = false;
        this.isTransportWasRunWithBootstrapChange.emit(this.isTransportWasRunWithBootstrap);
      }
    });
  }

  updateIsTransportWasRunWithBootstrap(newValue: boolean): void {
    this.isTransportWasRunWithBootstrap = newValue;
    this.isTransportWasRunWithBootstrapChange.emit(this.isTransportWasRunWithBootstrap);
  }

  public validate(c: UntypedFormControl) {
    return (this.bootstrapConfigServersFormGroup.valid) ? null : {
      serverConfigs: {
        valid: false,
      },
    };
  }

  public isBootstrapServerNotAvailable(): boolean {
    return this.isBootstrapAdded() || !this.isBootstrapServerUpdateEnableValue || !this.isTransportWasRunWithBootstrap;
  }

  private isBootstrapAdded(): boolean {
    const serverConfigsArray =  this.serverConfigsFromArray.getRawValue();
    for (let i = 0; i < serverConfigsArray.length; i++) {
      if (serverConfigsArray[i].bootstrapServerIs) {
        return true;
      }
    }
    return false;
  }

  private removeBootstrapServerConfig(): void {
    if (this.bootstrapConfigServersFormGroup) {
      const bootstrapServerIndex = this.serverConfigsFromArray.getRawValue().findIndex(server => server.bootstrapServerIs === true);
      if (bootstrapServerIndex !== -1) {
        this.serverConfigsFromArray.removeAt(bootstrapServerIndex);
      }
    }
  }

  private updateModel() {
    const serverConfigs: Array<ServerSecurityConfig> = this.serverConfigsFromArray.value;
    this.propagateChange(serverConfigs);
  }
}
