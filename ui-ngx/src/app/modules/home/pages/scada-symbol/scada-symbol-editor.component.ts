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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  ScadaSymbolEditObject,
  ScadaSymbolEditObjectCallbacks
} from '@home/pages/scada-symbol/scada-symbol-editor.models';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { FormControl } from '@angular/forms';
import {
  parseScadaSymbolsTagsFromContent,
  removeScadaSymbolMetadata
} from '@home/components/widget/lib/scada/scada-symbol.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface ScadaSymbolEditorData {
  scadaSymbolContent: string;
}

type editorModeType = 'svg' | 'xml';

@Component({
  selector: 'tb-scada-symbol-editor',
  templateUrl: './scada-symbol-editor.component.html',
  styleUrls: ['./scada-symbol-editor.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolEditorComponent implements OnInit, OnDestroy, AfterViewInit, OnChanges {

  @ViewChild('scadaSymbolShape', {static: false})
  scadaSymbolShape: ElementRef<HTMLElement>;

  @ViewChild('tooltipsContainer', {static: false})
  tooltipsContainer: ElementRef<HTMLElement>;

  @ViewChild('tooltipsContainerComponent', {static: true})
  tooltipsContainerComponent: TbAnchorComponent;

  @Input()
  data: ScadaSymbolEditorData;

  @Input()
  editObjectCallbacks: ScadaSymbolEditObjectCallbacks;

  @Input()
  readonly: boolean;

  @Output()
  updateScadaSymbol = new EventEmitter();

  @Output()
  downloadScadaSymbol = new EventEmitter();

  scadaSymbolEditObject: ScadaSymbolEditObject;

  zoomInDisabled = false;
  zoomOutDisabled = false;

  @Input()
  showHiddenElements = false;

  @Output()
  showHiddenElementsChange = new EventEmitter<boolean>();

  displayShowHidden = false;

  svgContentFormControl = new FormControl();

  svgContent: string;

  private editorModeValue: editorModeType = 'svg';

  get editorMode(): editorModeType {
    return this.editorModeValue;
  }

  set editorMode(value: editorModeType) {
    this.updateEditorMode(value);
  }

  constructor(private cd: ChangeDetectorRef,
              private zone: NgZone,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    if (this.readonly) {
      this.svgContentFormControl.disable({emitEvent: false});
    } else {
      this.svgContentFormControl.enable({emitEvent: false});
    }
    this.svgContentFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((svgContent) => {
      if (this.svgContent !== svgContent) {
        this.svgContent = svgContent;
        this.editObjectCallbacks.onSymbolEditObjectDirty(true);
      }
      this.editObjectCallbacks.onSymbolEditObjectValid(this.svgContentFormControl.valid);
    });
  }

  ngAfterViewInit() {
    this.editObjectCallbacks.onZoom = () => {
      this.updateZoomButtonsState();
    };
    this.editObjectCallbacks.hasHiddenElements = (hasHidden) => {
      this.displayShowHidden = hasHidden;
      if (hasHidden) {
        this.scadaSymbolEditObject.showHiddenElements(this.showHiddenElements);
      }
      this.cd.markForCheck();
    };
    this.scadaSymbolEditObject = new ScadaSymbolEditObject(this.scadaSymbolShape.nativeElement,
      this.tooltipsContainer.nativeElement,
      this.tooltipsContainerComponent.viewContainerRef, this.zone, this.editObjectCallbacks, this.readonly);
    if (this.data) {
      this.updateContent(this.data.scadaSymbolContent);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'data') {
          setTimeout(() => {
            this.updateContent(this.data.scadaSymbolContent);
          });
        } else if (propName === 'readonly') {
          this.scadaSymbolEditObject.setReadOnly(this.readonly);
          if (this.readonly) {
            this.svgContentFormControl.disable({emitEvent: false});
          } else {
            this.svgContentFormControl.enable({emitEvent: false});
          }
        }
      }
    }
  }

  ngOnDestroy() {
    this.scadaSymbolEditObject.destroy();
  }

  getContent(): string {
    if (this.editorMode === 'svg') {
      return this.scadaSymbolEditObject?.getContent();
    } else {
      return this.svgContent;
    }
  }

  getTags(): string[] {
    if (this.editorMode === 'svg') {
      return this.scadaSymbolEditObject?.getTags();
    } else {
      return parseScadaSymbolsTagsFromContent(this.svgContent);
    }
  }

  zoomIn() {
    this.scadaSymbolEditObject.zoomIn();
  }

  zoomOut() {
    this.scadaSymbolEditObject.zoomOut();
  }

  toggleShowHidden() {
    this.showHiddenElements = !this.showHiddenElements;
    this.showHiddenElementsChange.emit(this.showHiddenElements);
    this.scadaSymbolEditObject.showHiddenElements(this.showHiddenElements);
  }

  private updateEditorMode(mode: editorModeType) {
    this.editorModeValue = mode;
    if (mode === 'xml') {
      this.svgContent = this.scadaSymbolEditObject.getContent();
      this.svgContentFormControl.setValue(this.svgContent, {emitEvent: false});
    } else {
      this.updateEditObjectContent(this.svgContent);
    }
  }

  private updateContent(content: string) {
    this.svgContent = removeScadaSymbolMetadata(content);
    if (this.editorMode === 'xml') {
      this.svgContentFormControl.setValue(this.svgContent, {emitEvent: false});
    } else {
      this.updateEditObjectContent(this.svgContent);
    }
  }

  private updateEditObjectContent(content: string) {
    if (this.scadaSymbolEditObject) {
      this.displayShowHidden = false;
      this.scadaSymbolEditObject.setContent(content);
      setTimeout(() => {
        this.updateZoomButtonsState();
      });
    }
  }

  private updateZoomButtonsState() {
    this.zoomInDisabled = this.scadaSymbolEditObject.zoomInDisabled();
    this.zoomOutDisabled = this.scadaSymbolEditObject.zoomOutDisabled();
    this.cd.markForCheck();
  }
}

