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

import vm, { Script } from 'vm';
import atob from 'atob';
import btoa from 'btoa';
import {TextDecoder} from "util";

export type TbScript = Script | Function;

export class JsExecutor {
    useSandbox: boolean;

    constructor(useSandbox: boolean) {
        this.useSandbox = useSandbox;
    }

    compileScript(code: string): Promise<TbScript> {
        if (this.useSandbox) {
            return this.createScript(code);
        } else {
            return this.createFunction(code);
        }
    }

    executeScript(script: TbScript, args: string[], timeout?: number): Promise<any> {
        if (this.useSandbox) {
            return this.invokeScript(script as Script, args, timeout);
        } else {
            return this.invokeFunction(script as Function, args);
        }
    }

    private createScript(code: string): Promise<Script> {
        return new Promise((resolve, reject) => {
            try {
                code = "("+code+")(...args)";
                const script = new vm.Script(code);
                resolve(script);
            } catch (err) {
                reject(err);
            }
        });
    }

    private invokeScript(script: Script, args: string[], timeout: number | undefined): Promise<any> {
        return new Promise((resolve, reject) => {
            try {
                const sandbox = Object.create(null);
                sandbox.args = args;
                sandbox.btoa = btoa;
                sandbox.atob = atob;
                sandbox.TextDecoder = TextDecoder;
                const result = script.runInNewContext(sandbox, {timeout: timeout});
                resolve(result);
            } catch (err) {
                reject(err);
            }
        });
    }


    private createFunction(code: string): Promise<Function> {
        return new Promise((resolve, reject) => {
            try {
                code = "return ("+code+")(...args)";
                const parsingContext = vm.createContext({btoa: btoa, atob: atob, TextDecoder: TextDecoder});
                const func = vm.compileFunction(code, ['args'], {parsingContext: parsingContext});
                resolve(func);
            } catch (err) {
                reject(err);
            }
        });
    }

    private invokeFunction(func: Function, args: string[]): Promise<any> {
        return new Promise((resolve, reject) => {
            try {
                const result = func(args);
                resolve(result);
            } catch (err) {
                reject(err);
            }
        });
    }
}
