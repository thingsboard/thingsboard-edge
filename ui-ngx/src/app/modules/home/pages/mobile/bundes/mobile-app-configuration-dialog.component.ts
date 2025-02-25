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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';
import { MobileApp } from '@shared/models/mobile-app.models';

export interface MobileAppConfigurationDialogData {
  afterAdd: boolean;
  androidApp: MobileApp;
  iosApp: MobileApp;
}

@Component({
  selector: 'tb-mobile-app-configuration-dialog',
  templateUrl: './mobile-app-configuration-dialog.component.html',
  styleUrls: ['./mobile-app-configuration-dialog.component.scss']
})
export class MobileAppConfigurationDialogComponent extends DialogComponent<MobileAppConfigurationDialogComponent> {

  notShowAgain = false;
  setApplication = false;

  showDontShowAgain: boolean;

  gitRepositoryLink = 'git clone -b master https://github.com/thingsboard/flutter_thingsboard_pe_app.git';
  pathToConstants = 'lib/constants/app_constants.dart';
  flutterRunCommand = 'flutter run';
  flutterInstallRenameCommand = 'flutter pub global activate rename';

  configureApi: string;

  renameCommands: string[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: MobileAppConfigurationDialogData,
              protected dialogRef: MatDialogRef<MobileAppConfigurationDialogComponent>,
              ) {
    super(store, router, dialogRef);

    this.showDontShowAgain = this.data.afterAdd;

    this.setApplication = !!this.data.androidApp || !!this.data.iosApp;

    this.configureApi = `static const thingsBoardApiEndpoint = '${window.location.origin}';`;
    if (this.setApplication) {
      this.configureApi += '\n';
      if (!!this.data.androidApp) {
        this.configureApi += `\nstatic const thingsboardAndroidAppSecret = '${this.data.androidApp.appSecret}';`;
      }
      if (!!this.data.iosApp) {
        this.configureApi += `\nstatic const thingsboardIOSAppSecret = '${this.data.iosApp.appSecret}';`;
      }
    }
    if (this.setApplication) {
      if (this.data.androidApp?.pkgName === this.data.iosApp?.pkgName) {
        this.renameCommands.push(`rename setBundleId --targets android, ios --value "${this.data.androidApp.pkgName}"`);
      } else {
        if (!!this.data.androidApp) {
          this.renameCommands.push(`rename setBundleId --targets android --value "${this.data.androidApp.pkgName}"`);
        }
        if (!!this.data.iosApp) {
          this.renameCommands.push(`rename setBundleId --targets ios --value "${this.data.iosApp.pkgName}"`);
        }
      }
    }
  }

  close(): void {
    if (this.notShowAgain && this.showDontShowAgain) {
      this.store.dispatch(new ActionPreferencesPutUserSettings({ notDisplayConfigurationAfterAddMobileBundle: true }));
      this.dialogRef.close(null);
    } else {
      this.dialogRef.close(null);
    }
  }

  createMarkDownCommand(commands: string | string[]): string {
    if (Array.isArray(commands)) {
      const formatCommands: Array<string> = [];
      commands.forEach(command => formatCommands.push(this.createMarkDownSingleCommand(command)));
      return formatCommands.join(`\n<br />\n\n`);
    } else {
      return this.createMarkDownSingleCommand(commands);
    }
  }

  private createMarkDownSingleCommand(command: string): string {
    return '```bash\n' +
      command +
      '{:copy-code}\n' +
      '```';
  }
}
