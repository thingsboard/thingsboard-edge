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
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Ace } from 'ace-builds';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { ResizeObserver } from '@juggle/resize-observer';
import { guid } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getAce } from '@shared/models/ace/ace.models';
import { beautifyJs } from '@shared/models/beautify.models';

@Component({
  selector: 'tb-protobuf-content',
  templateUrl: './protobuf-content.component.html',
  styleUrls: ['./protobuf-content.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ProtobufContentComponent),
      multi: true
    }
  ]
})
export class ProtobufContentComponent implements OnInit, ControlValueAccessor, OnDestroy {

  @ViewChild('protobufEditor', {static: true})
  protobufEditorElmRef: ElementRef;

  private protobufEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  toastTargetId = `protobufContentEditor-${guid()}`;

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() editorStyle: {[klass: string]: any};

  @Input() tbPlaceholder: string;

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }
  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  fullscreen = false;

  contentBody: string;

  errorShowed = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>,
              private raf: RafService) {
  }

  ngOnInit(): void {
    const editorElement = this.protobufEditorElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/protobuf`,
      showGutter: true,
      showPrintMargin: false,
      readOnly: this.disabled || this.readonly,
    };

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce().subscribe(
      (ace) => {
        this.protobufEditor = ace.edit(editorElement, editorOptions);
        this.protobufEditor.session.setUseWrapMode(true);
        this.protobufEditor.setValue(this.contentBody ? this.contentBody : '', -1);
        this.protobufEditor.setReadOnly(this.disabled || this.readonly);
        this.protobufEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.updateView();
          }
        });
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
    if (this.protobufEditor) {
      this.protobufEditor.destroy();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.protobufEditor) {
      this.protobufEditor.setReadOnly(this.disabled || this.readonly);
    }
  }

  writeValue(value: string): void {
    this.contentBody = value;
    if (this.protobufEditor) {
      this.ignoreChange = true;
      this.protobufEditor.setValue(this.contentBody ? this.contentBody : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.protobufEditor.getValue();
    if (this.contentBody !== editorValue) {
      this.contentBody = editorValue;
      this.propagateChange(this.contentBody);
    }
  }

  beautifyProtobuf() {
    beautifyJs(this.contentBody, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        this.protobufEditor.setValue(res ? res : '', -1);
        this.updateView();
      }
    );
  }

  onFullscreen() {
    if (this.protobufEditor) {
      setTimeout(() => {
        this.protobufEditor.resize();
      }, 0);
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.protobufEditor.resize();
      this.protobufEditor.renderer.updateFull();
    });
  }

}
