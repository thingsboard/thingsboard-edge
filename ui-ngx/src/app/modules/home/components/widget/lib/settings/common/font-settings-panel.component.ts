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
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import {
  commonFonts,
  ComponentStyle,
  Font,
  fontStyles,
  fontStyleTranslations,
  fontWeights,
  fontWeightTranslations, isFontPartiallySet,
  textStyle
} from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable } from 'rxjs';
import { map, startWith, tap } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-font-settings-panel',
  templateUrl: './font-settings-panel.component.html',
  providers: [],
  styleUrls: ['./font-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class FontSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  font: Font;

  @Input()
  previewText = 'AaBbCcDd';

  @Input()
  initialPreviewStyle: ComponentStyle;

  @Input()
  @coerceBoolean()
  clearButton = false;

  @Input()
  popover: TbPopoverComponent<FontSettingsPanelComponent>;

  @Output()
  fontApplied = new EventEmitter<Font>();

  @ViewChild('familyInput', {static: true}) familyInput: ElementRef;

  fontWeightsList = fontWeights;

  fontWeightTranslationsMap = fontWeightTranslations;

  fontStylesList = fontStyles;

  fontStyleTranslationsMap = fontStyleTranslations;

  fontFormGroup: UntypedFormGroup;

  filteredFontFamilies: Observable<Array<string>>;

  familySearchText = '';

  previewStyle: ComponentStyle = {};

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.fontFormGroup = this.fb.group(
      {
        size: [this.font?.size, [Validators.min(0)]],
        sizeUnit: [(this.font?.sizeUnit || 'px'), []],
        family: [this.font?.family, []],
        weight: [this.font?.weight, []],
        style: [this.font?.style, []],
        lineHeight: [this.font?.lineHeight, []]
      }
    );
    this.updatePreviewStyle(this.font);
    this.fontFormGroup.valueChanges.subscribe((font: Font) => {
      if (this.fontFormGroup.valid) {
        this.updatePreviewStyle(font);
        setTimeout(() => {this.popover?.updatePosition();}, 0);
      }
    });
    this.filteredFontFamilies = this.fontFormGroup.get('family').valueChanges
      .pipe(
        startWith<string>(''),
        tap((searchText) => { this.familySearchText = searchText || ''; }),
        map(() => commonFonts.filter(f => f.toUpperCase().includes(this.familySearchText.toUpperCase())))
      );
  }

  clearFamily() {
    this.fontFormGroup.get('family').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.familyInput.nativeElement.blur();
      this.familyInput.nativeElement.focus();
    }, 0);
  }

  cancel() {
    this.popover?.hide();
  }

  applyFont() {
    const font = this.fontFormGroup.value;
    this.fontApplied.emit(font);
  }

  clearDisabled(): boolean {
    return !isFontPartiallySet(this.fontFormGroup.value);
  }

  clearFont() {
    this.fontFormGroup.reset({sizeUnit: 'px'});
    this.fontFormGroup.markAsDirty();
  }

  private updatePreviewStyle(font: Font) {
    this.previewStyle = {...(this.initialPreviewStyle || {}), ...textStyle(font)};
  }

}
