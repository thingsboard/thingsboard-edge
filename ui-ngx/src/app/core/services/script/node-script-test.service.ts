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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RuleChainService } from '@core/http/rule-chain.service';
import { switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import {
  NodeScriptTestDialogComponent,
  NodeScriptTestDialogData
} from '@shared/components/dialog/node-script-test-dialog.component';
import { sortObjectKeys } from '@core/utils';
import { ScriptLanguage } from '@shared/models/rule-node.models';

@Injectable({
  providedIn: 'root'
})
export class NodeScriptTestService {

  constructor(private ruleChainService: RuleChainService,
              public dialog: MatDialog) {
  }

  testNodeScript(script: string, scriptType: string, functionTitle: string,
                 functionName: string, argNames: string[], ruleNodeId: string, helpId?: string,
                 scriptLang?: ScriptLanguage): Observable<string> {
    if (ruleNodeId) {
      return this.ruleChainService.getLatestRuleNodeDebugInput(ruleNodeId).pipe(
        switchMap((debugIn) => {
          let msg: any;
          let metadata: {[key: string]: string};
          let msgType: string;
          if (debugIn) {
            if (debugIn.data) {
              msg = JSON.parse(debugIn.data);
            }
            if (debugIn.metadata) {
              metadata = JSON.parse(debugIn.metadata);
            }
            msgType = debugIn.msgType;
          }
          return this.openTestScriptDialog(script, scriptType, functionTitle,
            functionName, argNames, msg, metadata, msgType, helpId, scriptLang);
        })
      );
    } else {
      return this.openTestScriptDialog(script, scriptType, functionTitle,
        functionName, argNames, null, null, null, helpId, scriptLang);
    }
  }

  private openTestScriptDialog(script: string, scriptType: string,
                               functionTitle: string, functionName: string, argNames: string[],
                               msg?: any, metadata?: {[key: string]: string}, msgType?: string, helpId?: string,
                               scriptLang?: ScriptLanguage): Observable<string> {
    if (!msg) {
      msg = {
        temperature: 22.4,
        humidity: 78
      };
    }
    if (!metadata) {
      metadata = {
        deviceName: 'Test Device',
        deviceType: 'default',
        ts: new Date().getTime() + ''
      };
    } else {
      metadata = sortObjectKeys(metadata);
    }
    if (!msgType) {
      msgType = 'POST_TELEMETRY_REQUEST';
    }
    return this.dialog.open<NodeScriptTestDialogComponent, NodeScriptTestDialogData, string>(NodeScriptTestDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
        data: {
          msg,
          metadata,
          msgType,
          functionTitle,
          functionName,
          script,
          scriptType,
          argNames,
          helpId,
          scriptLang
        }
      }).afterClosed();
  }

}
