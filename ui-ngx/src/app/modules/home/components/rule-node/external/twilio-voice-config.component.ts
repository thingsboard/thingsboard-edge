///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Language, ProviderSource, voiceConfiguration, Voices } from './twilio-voice-config.models';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-external-node-twilio-voice-config',
  templateUrl: './twilio-voice-config.component.html',
  styleUrls: ['./twilio-voice-config.component.scss', './twilio-config.component.scss']
})
export class TwilioVoiceConfigComponent extends RuleNodeConfigurationComponent {

  twilioVoiceConfigForm: FormGroup;
  voiceConfiguration = voiceConfiguration;
  providers = ProviderSource;
  languages: Language[] = [];
  voices: Voices[] = [];

  get startPauseErrorText(): string {
    const startPauseControl = this.twilioVoiceConfigForm.get('startPause');

    if (startPauseControl.hasError('required')) {
      return this.translate.instant('rule-node-config.twilio.start-pause-required');
    } else if (startPauseControl.hasError('min')) {
      return this.translate.instant('rule-node-config.twilio.start-pause-min', {min: 0});
    }

    return '';
  }

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    super();
  }

  protected configForm(): FormGroup {
    return this.twilioVoiceConfigForm;
  }

  protected updateConfiguration(configuration: RuleNodeConfiguration) {
    super.updateConfiguration(configuration);
    if (this.configuration.provider !== null) {
      this.languages = Array.from(this.voiceConfiguration.get(configuration?.provider)?.values());
      this.voices = this.voiceConfiguration.get(configuration?.provider)?.get(configuration?.language)?.voices;
    }
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.twilioVoiceConfigForm = this.fb.group({
      numberFrom: [configuration ? configuration.numberFrom : null, [Validators.required]],
      numbersTo: [configuration ? configuration.numbersTo : null, [Validators.required]],
      accountSid: [configuration ? configuration.accountSid : null, [Validators.required]],
      accountToken: [configuration ? configuration.accountToken : null, [Validators.required]],
      provider: [configuration ? configuration.provider : null, [Validators.required]],
      language: [configuration ? configuration.language : null, [Validators.required]],
      voice: [configuration ? configuration.voice : null, [Validators.required]],
      pitch: [configuration ? configuration.pitch : null, [Validators.required, Validators.min(0)]],
      rate: [configuration ? configuration.rate : null, [Validators.required, Validators.min(0)]],
      volume: [configuration ? configuration.volume : null, [Validators.required]],
      startPause: [configuration ? configuration.startPause : null, [Validators.required, Validators.min(0)]]
    });

    this.twilioVoiceConfigForm.get('provider').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(provider => {
      this.languages = Array.from(this.voiceConfiguration.get(provider)?.values());
      this.voices = provider === ProviderSource.ALICE ? [Voices.ALICE] : [];
        this.twilioVoiceConfigForm.patchValue({
          language: null,
          voice: provider === ProviderSource.ALICE ? Voices.ALICE : null
        }, {emitEvent: false});
    })
    this.twilioVoiceConfigForm.get('language').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((language) => {
      this.voices = Array.from(this.voiceConfiguration.get(this.twilioVoiceConfigForm.get('provider').value)?.get(language).voices);
      this.twilioVoiceConfigForm.patchValue({
        voice: this.twilioVoiceConfigForm.get('provider').value === ProviderSource.ALICE ? Voices.ALICE : null
      }, {emitEvent: false});
    })
  }

}
