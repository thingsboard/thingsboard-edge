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

import {
  AfterViewInit,
  Component,
  ElementRef,
  HostBinding,
  Inject,
  QueryList,
  SkipSelf,
  ViewChild,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { combineLatest, NEVER, Observable, of } from 'rxjs';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { JsonContentComponent } from '@shared/components/json-content.component';
import { mergeMap, startWith } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ConverterDebugInput, TestConverterInputParams } from '@shared/models/converter.models';
import { base64toString, isEqual, stringToBase64 } from '@core/utils';
import { ConverterService } from '@core/http/converter.service';
import { beautifyJs } from '@shared/models/beautify.models';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { ScriptLanguage } from '@shared/models/rule-node.models';

export interface ConverterTestDialogData {
  isDecoder: boolean;
  funcBody: string;
  scriptLang?: ScriptLanguage;
  debugIn: ConverterDebugInput;
}

@Component({
  selector: 'tb-converter-test-dialog',
  templateUrl: './converter-test-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ConverterTestDialogComponent}],
  styleUrls: ['./converter-test-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ConverterTestDialogComponent extends DialogComponent<ConverterTestDialogComponent,
  string> implements AfterViewInit, ErrorStateMatcher {

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  @ViewChildren('topPanel')
  topPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('topLeftPanel')
  topLeftPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('topRightPanel')
  topRightPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('bottomPanel')
  bottomPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('bottomLeftPanel')
  bottomLeftPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('bottomRightPanel')
  bottomRightPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChild('payloadContent', {static: true}) payloadContent: JsonContentComponent;

  converterTestFormGroup: FormGroup;

  isDecoder: boolean;

  submitted = false;

  contentType = ContentType;

  contentTypes = Object.keys(ContentType);

  contentTypesInfoMap = contentTypesMap;

  scriptLanguage = ScriptLanguage;

  scriptLang = this.data.scriptLang ? this.data.scriptLang : ScriptLanguage.JS;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ConverterTestDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ConverterTestDialogComponent, string>,
              public fb: FormBuilder,
              public wl: WhiteLabelingService,
              private converterService: ConverterService) {
    super(store, router, dialogRef);
    this.isDecoder = this.data.isDecoder;
    this.init();
  }

  init(): void {
    const debugIn = this.data.debugIn;
    let contentType: ContentType;
    let msgType: string;
    let metadata: any;
    let integrationMetadata: any;
    if (debugIn) {
      if (debugIn.inContentType) {
        contentType = debugIn.inContentType;
      }
      if (debugIn.inMetadata) {
        metadata = JSON.parse(debugIn.inMetadata);
      }
      if (!this.isDecoder) {
        if (debugIn.inMsgType) {
          msgType = debugIn.inMsgType;
        }
        if (debugIn.inIntegrationMetadata) {
          integrationMetadata = JSON.parse(debugIn.inIntegrationMetadata);
        }
      }
    }
    if (!contentType) {
      contentType = ContentType.JSON;
    }
    if (this.isDecoder) {
      if (!metadata) {
        metadata = {
          integrationName: 'Test integration'
        };
      }
    } else {
      if (!metadata) {
        metadata = {
          deviceName: 'sensorA',
          deviceType: 'temp-sensor',
          ss_serialNumber: 'SN111'
        };
      }
      if (!integrationMetadata) {
        integrationMetadata = {
          integrationName: 'Test integration'
        };
      }
      if (!msgType) {
        msgType = 'ATTRIBUTES_UPDATED';
      }
    }

    this.converterTestFormGroup = this.fb.group({
      payload: this.fb.group({
        stringContent: [null, []],
        contentType: [contentType, []]
      }),
      metadata: [metadata],
      funcBody: [this.data.funcBody, []],
      output: ['', []]
    });
    const payloadFormGroup: FormGroup = this.converterTestFormGroup.get('payload') as FormGroup;
    if (this.isDecoder) {
      payloadFormGroup.addControl(
        'payload', this.fb.control(null, [])
      );
      payloadFormGroup.get('contentType').valueChanges.subscribe((newVal) => {
        const prevVal = payloadFormGroup.value.contentType;
        if (newVal && !isEqual(newVal, prevVal)) {
          const content = payloadFormGroup.get('stringContent').value;
          if (prevVal === ContentType.BINARY) {
            this.convertContent(content, newVal).subscribe(
              (newContent) => {
                payloadFormGroup.get('stringContent').patchValue(newContent);
              }
            );
          } else if (newVal === ContentType.BINARY) {
            payloadFormGroup.get('stringContent').patchValue(stringToBase64(content));
          } else if (newVal === ContentType.JSON) {
            beautifyJs(content, {indent_size: 4}).subscribe(
              (newContent) => {
                payloadFormGroup.get('stringContent').patchValue(newContent);
              }
            );
          }
        }
      });
    } else {
      payloadFormGroup.addControl(
        'msg', this.fb.control(null, [])
      );
      payloadFormGroup.addControl(
        'msgType', this.fb.control(msgType, [Validators.required])
      );
      this.converterTestFormGroup.addControl(
        'integrationMetadata', this.fb.control(integrationMetadata, [])
      );
    }
    const inputContentTriggers: Observable<any>[] = [
      payloadFormGroup.get('stringContent').valueChanges.pipe(startWith(''))
    ];
    if (this.isDecoder) {
      inputContentTriggers.push(payloadFormGroup.get('contentType').valueChanges.pipe(startWith('')));
    }
    this.updateInputContent();
    combineLatest(inputContentTriggers).subscribe(() => {
      this.updateInputContent();
    });
    this.prepareStringContent(debugIn).subscribe(
      (stringContent) => {
        this.converterTestFormGroup.get('payload').get('stringContent').patchValue(stringContent, {emitEvent: false});
      }
    );
  }

  private prepareStringContent(debugIn: ConverterDebugInput): Observable<string> {
    if (debugIn) {
      if (debugIn.inContentType) {
        if (debugIn.inContent) {
          if (debugIn.inContentType === ContentType.JSON) {
            return beautifyJs(debugIn.inContent, {indent_size: 4});
          } else {
            return of(debugIn.inContent);
          }
        }
      }
    }
    if (this.isDecoder) {
      return beautifyJs(JSON.stringify({devName: 'devA', param1: 1, param2: 'test'}), {indent_size: 4});
    } else {
      const msg = {
        temperatureUploadFrequency: 60,
        humidityUploadFrequency: 30
      };
      return beautifyJs(JSON.stringify(msg), {indent_size: 4});
    }
  }

  private convertContent(content: string, contentType: ContentType): Observable<string> {
    if (contentType && content) {
      if (contentType === ContentType.JSON ||
        contentType === ContentType.TEXT) {
        const stringContent = base64toString(content);
        if (contentType === ContentType.JSON) {
          return beautifyJs(stringContent, {indent_size: 4});
        } else {
          return of(stringContent);
        }
      } else {
        return of(content);
      }
    }
    return of('');
  }

  private updateInputContent() {
    const stringContent = this.converterTestFormGroup.get('payload').get('stringContent').value;
    if (this.isDecoder) {
      const contentType = this.converterTestFormGroup.get('payload').get('contentType').value;
      if (contentType === ContentType.BINARY) {
        this.converterTestFormGroup.get('payload').get('payload').patchValue(stringContent, {emitEvent: false});
      } else {
        this.converterTestFormGroup.get('payload').get('payload').patchValue(stringToBase64(stringContent), {emitEvent: false});
      }
    } else {
      this.converterTestFormGroup.get('payload').get('msg').patchValue(stringContent, {emitEvent: false});
    }
  }

  ngAfterViewInit(): void {
    this.initSplitLayout(this.topPanelElmRef.first.nativeElement,
      this.topLeftPanelElmRef.first.nativeElement,
      this.topRightPanelElmRef.first.nativeElement,
      this.bottomPanelElmRef.first.nativeElement,
      this.bottomLeftPanelElmRef.first.nativeElement,
      this.bottomRightPanelElmRef.first.nativeElement);
  }

  private initSplitLayout(topPanel: any,
                          topLeftPanel: any,
                          topRightPanel: any,
                          bottomPanel: any,
                          bottomLeftPanel: any,
                          bottomRightPanel: any) {

    Split([topPanel, bottomPanel], {
      sizes: [35, 65],
      gutterSize: 8,
      cursor: 'row-resize',
      direction: 'vertical'
    });

    Split([topLeftPanel, topRightPanel], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });

    Split([bottomLeftPanel, bottomRightPanel], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  test(): void {
    this.testConverter().subscribe((output) => {
      beautifyJs(output, {indent_size: 4}).subscribe(
        (newOutput) => {
          this.converterTestFormGroup.get('output').setValue(newOutput);
        }
      );
    });
  }

  private testConverter(): Observable<string> {
    if (this.checkInputParamErrors()) {
      const inputParams = {
        metadata: this.converterTestFormGroup.get('metadata').value
      } as TestConverterInputParams;
      if (this.isDecoder) {
        inputParams.payload = this.converterTestFormGroup.get('payload').get('payload').value;
        inputParams.decoder = this.converterTestFormGroup.get('funcBody').value;
      } else {
        inputParams.msg = this.converterTestFormGroup.get('payload').get('msg').value;
        inputParams.msgType = this.converterTestFormGroup.get('payload').get('msgType').value;
        inputParams.integrationMetadata = this.converterTestFormGroup.get('integrationMetadata').value;
        inputParams.encoder = this.converterTestFormGroup.get('funcBody').value;
      }
      const testObservable = this.isDecoder ? this.converterService.testUpLink(inputParams, this.scriptLang) :
        this.converterService.testDownLink(inputParams, this.scriptLang);
      return testObservable.pipe(
        mergeMap((result) => {
          if (result.error) {
            this.store.dispatch(new ActionNotificationShow(
              {
                message: result.error,
                type: 'error'
              }));
            return NEVER;
          } else {
            return of(result.output);
          }
        })
      );
    } else {
      return NEVER;
    }
  }

  private checkInputParamErrors(): boolean {
    this.payloadContent.validateOnSubmit();
    if (!this.converterTestFormGroup.get('payload').valid) {
      return false;
    }
    return true;
  }

  save(): void {
    this.submitted = true;
    this.testConverter().subscribe(() => {
      this.converterTestFormGroup.markAsPristine();
      this.dialogRef.close(this.converterTestFormGroup.get('funcBody').value);
    });
  }
}
