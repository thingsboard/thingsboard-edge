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

import { Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, Renderer2, ViewChild } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { Ace } from 'ace-builds';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { RafService } from '@core/services/raf.service';
import { isDefinedAndNotNull, isUndefined } from '@core/utils';
import { getAce } from '@shared/models/ace/ace.models';

@Component({
  selector: 'tb-json-object-view',
  templateUrl: './json-object-view.component.html',
  styleUrls: ['./json-object-view.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsonObjectViewComponent),
      multi: true
    }
  ]
})
export class JsonObjectViewComponent implements OnInit, OnDestroy {

  @ViewChild('jsonViewer', {static: true})
  jsonViewerElmRef: ElementRef;

  private jsonViewer: Ace.Editor;
  private viewerElement: Ace.Editor;
  private propagateChange = null;
  private modelValue: any;
  private contentValue: string;

  @Input() label: string;

  @Input() fillHeight: boolean;

  @Input() editorStyle: { [klass: string]: any };

  @Input() sort: (key: string, value: any) => any;

  private widthValue: boolean;

  get autoWidth(): boolean {
    return this.widthValue;
  }

  @Input()
  set autoWidth(value: boolean) {
    this.widthValue = coerceBooleanProperty(value);
  }

  private heigthValue: boolean;

  get autoHeight(): boolean {
    return this.heigthValue;
  }

  @Input()
  set autoHeight(value: boolean) {
    this.heigthValue = coerceBooleanProperty(value);
  }

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>,
              private raf: RafService,
              private renderer: Renderer2) {
  }

  ngOnInit(): void {
    this.viewerElement = this.jsonViewerElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/java',
      theme: 'ace/theme/github',
      showGutter: false,
      showPrintMargin: false,
      readOnly: true
    };

    const advancedOptions = {
      enableSnippets: false,
      enableBasicAutocompletion: false,
      enableLiveAutocompletion: false
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce().subscribe(
      (ace) => {
        this.jsonViewer = ace.edit(this.viewerElement, editorOptions);
        this.jsonViewer.session.setUseWrapMode(false);
        this.jsonViewer.setValue(this.contentValue ? this.contentValue : '', -1);
        if (this.contentValue && (this.widthValue || this.heigthValue)) {
          this.updateEditorSize(this.viewerElement, this.contentValue, this.jsonViewer);
        }
      }
    );
  }

  ngOnDestroy(): void {
    if (this.jsonViewer) {
      this.jsonViewer.destroy();
    }
  }

  updateEditorSize(editorElement: any, content: string, editor: Ace.Editor) {
    let newHeight = 200;
    let newWidth = 600;
    if (content && content.length > 0) {
      const lines = content.split('\n');
      newHeight = 17 * lines.length + 17;
      let maxLineLength = 0;
      lines.forEach((row) => {
        const line = row.replace(/\t/g, '    ').replace(/\n/g, '');
        const lineLength = line.length;
        maxLineLength = Math.max(maxLineLength, lineLength);
      });
      newWidth = 8 * maxLineLength + 16;
    }
    if (this.heigthValue) {
      this.renderer.setStyle(editorElement, 'height', newHeight.toString() + 'px');
    }
    if (this.widthValue) {
      this.renderer.setStyle(editorElement, 'width', newWidth.toString() + 'px');
    }
    editor.resize();
  }
  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: any): void {
    this.modelValue = value;
    this.contentValue = '';
    try {
      if (isDefinedAndNotNull(this.modelValue)) {
        this.contentValue = JSON.stringify(this.modelValue, isUndefined(this.sort) ? undefined :
          (key, objectValue) => {
            return this.sort(key, objectValue);
          }, 2);
      }
    } catch (e) {
      console.error(e);
    }
    if (this.jsonViewer) {
      this.jsonViewer.setValue(this.contentValue ? this.contentValue : '', -1);
      if (this.contentValue && (this.widthValue || this.heigthValue)) {
        this.updateEditorSize(this.viewerElement, this.contentValue, this.jsonViewer);
      }
    }
  }

}
