import * as i0 from '@angular/core';
import { Pipe, Component, NgModule } from '@angular/core';
import { RuleNodeConfigurationComponent, SharedModule } from '@shared/public-api';
import * as i2 from '@angular/forms';
import { Validators } from '@angular/forms';
import * as i1$1 from '@ngrx/store';
import * as i3 from '@angular/material/form-field';
import * as i6 from '@angular/flex-layout/flex';
import * as i5 from '@ngx-translate/core';
import * as i6$1 from '@angular/material/input';
import * as i9 from '@angular/common';
import { CommonModule } from '@angular/common';
import * as i1 from '@angular/platform-browser';
import * as i4 from '@angular/material/select';
import * as i5$1 from '@angular/material/core';
import { HomeComponentsModule } from '@home/components/public-api';

class SafeHtmlPipe {
    constructor(sanitizer) {
        this.sanitizer = sanitizer;
    }
    transform(html) {
        return this.sanitizer.bypassSecurityTrustHtml(html);
    }
}
SafeHtmlPipe.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SafeHtmlPipe, deps: [{ token: i1.DomSanitizer }], target: i0.ɵɵFactoryTarget.Pipe });
SafeHtmlPipe.ɵpipe = i0.ɵɵngDeclarePipe({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SafeHtmlPipe, name: "safeHtml" });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SafeHtmlPipe, decorators: [{
            type: Pipe,
            args: [{
                    name: 'safeHtml',
                }]
        }], ctorParameters: function () { return [{ type: i1.DomSanitizer }]; } });

class TwilioSmsConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.twilioSmsConfigForm;
    }
    onConfigurationSet(configuration) {
        this.twilioSmsConfigForm = this.fb.group({
            numberFrom: [configuration ? configuration.numberFrom : null, [Validators.required]],
            numbersTo: [configuration ? configuration.numbersTo : null, [Validators.required]],
            accountSid: [configuration ? configuration.accountSid : null, [Validators.required]],
            accountToken: [configuration ? configuration.accountToken : null, [Validators.required]]
        });
    }
}
TwilioSmsConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TwilioSmsConfigComponent, deps: [{ token: i1$1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
TwilioSmsConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: TwilioSmsConfigComponent, selector: "tb-action-node-twilio-sms-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"twilioSmsConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.twilio.number-from</mat-label>\n    <input required matInput formControlName=\"numberFrom\">\n    <mat-error *ngIf=\"twilioSmsConfigForm.get('numberFrom').hasError('required')\">\n      {{ 'tb.twilio.number-from-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.twilio.numbers-to</mat-label>\n    <input required matInput formControlName=\"numbersTo\">\n    <mat-error *ngIf=\"twilioSmsConfigForm.get('numbersTo').hasError('required')\">\n      {{ 'tb.twilio.numbers-to-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.twilio.numbers-to-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.twilio.account-sid</mat-label>\n    <input required matInput formControlName=\"accountSid\">\n    <mat-error *ngIf=\"twilioSmsConfigForm.get('accountSid').hasError('required')\">\n      {{ 'tb.twilio.account-sid-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.twilio.account-token</mat-label>\n    <input required type=\"password\" matInput formControlName=\"accountToken\">\n    <mat-error *ngIf=\"twilioSmsConfigForm.get('accountToken').hasError('required')\">\n      {{ 'tb.twilio.account-token-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }], directives: [{ type: i6.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i5.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i6$1.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i9.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }], pipes: { "translate": i5.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TwilioSmsConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-twilio-sms-config',
                    templateUrl: './twilio-sms-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1$1.Store }, { type: i2.FormBuilder }]; } });

var Voices;
(function (Voices) {
    Voices["MAN"] = "man";
    Voices["WOMAN"] = "woman";
    Voices["ALICE"] = "alice";
    Voices["POLLY_ADITI"] = "Polly.Aditi";
    Voices["POLLY_AMY"] = "Polly.Amy";
    Voices["POLLY_ASTRID"] = "Polly.Astrid";
    Voices["POLLY_BIANCA"] = "Polly.Bianca";
    Voices["POLLY_BRIAN"] = "Polly.Brian";
    Voices["POLLY_CAMILA"] = "Polly.Camila";
    Voices["POLLY_CARLA"] = "Polly.Carla";
    Voices["POLLY_CARMEN"] = "Polly.Carmen";
    Voices["POLLY_CELINE"] = "Polly.Celine";
    Voices["POLLY_CHANTAL"] = "Polly.Chantal";
    Voices["POLLY_CONCHITA"] = "Polly.Conchita";
    Voices["POLLY_CRISTIANO"] = "Polly.Cristiano";
    Voices["POLLY_DORA"] = "Polly.Dora";
    Voices["POLLY_EMMA"] = "Polly.Emma";
    Voices["POLLY_ENRIQUE"] = "Polly.Enrique";
    Voices["POLLY_EWA"] = "Polly.Ewa";
    Voices["POLLY_FILIZ"] = "Polly.Filiz";
    Voices["POLLY_GERAINT"] = "Polly.Geraint";
    Voices["POLLY_GIORGIO"] = "Polly.Giorgio";
    Voices["POLLY_GWYNETH"] = "Polly.Gwyneth";
    Voices["POLLY_HANS"] = "Polly.Hans";
    Voices["POLLY_INES"] = "Polly.Ines";
    Voices["POLLY_IVY"] = "Polly.Ivy";
    Voices["POLLY_JACEK"] = "Polly.Jacek";
    Voices["POLLY_JAN"] = "Polly.Jan";
    Voices["POLLY_JOANNA"] = "Polly.Joanna";
    Voices["POLLY_JOEY"] = "Polly.Joey";
    Voices["POLLY_JUSTIN"] = "Polly.Justin";
    Voices["POLLY_KARL"] = "Polly.Karl";
    Voices["POLLY_KENDRA"] = "Polly.Kendra";
    Voices["POLLY_KIMBERLY"] = "Polly.Kimberly";
    Voices["POLLY_LEA"] = "Polly.Lea";
    Voices["POLLY_LIV"] = "Polly.Liv";
    Voices["POLLY_LOTTE"] = "Polly.Lotte";
    Voices["POLLY_LUCIA"] = "Polly.Lucia";
    Voices["POLLY_LUPE"] = "Polly.Lupe";
    Voices["POLLY_MADS"] = "Polly.Mads";
    Voices["POLLY_MAJA"] = "Polly.Maja";
    Voices["POLLY_MARLENE"] = "Polly.Marlene";
    Voices["POLLY_MATHIEU"] = "Polly.Mathieu";
    Voices["POLLY_MATTHEW"] = "Polly.Matthew";
    Voices["POLLY_MAXIM"] = "Polly.Maxim";
    Voices["POLLY_MIA"] = "Polly.Mia";
    Voices["POLLY_MIGUEL"] = "Polly.Miguel";
    Voices["POLLY_MIZUKI"] = "Polly.Mizuki";
    Voices["POLLY_NAJA"] = "Polly.Naja";
    Voices["POLLY_NICOLE"] = "Polly.Nicole";
    Voices["POLLY_PENELOPE"] = "Polly.Penelope";
    Voices["POLLY_RAVEENA"] = "Polly.Raveena";
    Voices["POLLY_RICARDO"] = "Polly.Ricardo";
    Voices["POLLY_RUBEN"] = "Polly.Ruben";
    Voices["POLLY_RUSSELL"] = "Polly.Russell";
    Voices["POLLY_SALLI"] = "Polly.Salli";
    Voices["POLLY_SEOYEON"] = "Polly.Seoyeon";
    Voices["POLLY_TAKUMI"] = "Polly.Takumi";
    Voices["POLLY_TATYANA"] = "Polly.Tatyana";
    Voices["POLLY_VICKI"] = "Polly.Vicki";
    Voices["POLLY_VITORIA"] = "Polly.Vitoria";
    Voices["POLLY_ZEINA"] = "Polly.Zeina";
    Voices["POLLY_ZHIYU"] = "Polly.Zhiyu";
    Voices["POLLY_AMY_NEURAL"] = "Polly.Amy-Neural";
    Voices["POLLY_EMMA_NEURAL"] = "Polly.Emma-Neural";
    Voices["POLLY_BRIAN_NEURAL"] = "Polly.Brian-Neural";
    Voices["POLLY_SALLI_NEURAL"] = "Polly.Salli-Neural";
    Voices["POLLY_IVY_NEURAL"] = "Polly.Ivy-Neural";
    Voices["POLLY_JOANNA_NEURAL"] = "Polly.Joanna-Neural";
    Voices["POLLY_KENDRA_NEURAL"] = "Polly.Kendra-Neural";
    Voices["POLLY_KIMBERLY_NEURAL"] = "Polly.Kimberly-Neural";
    Voices["POLLY_JOEY_NEURAL"] = "Polly.Joey-Neural";
    Voices["POLLY_JUSTIN_NEURAL"] = "Polly.Justin-Neural";
    Voices["POLLY_MATTHEW_NEURAL"] = "Polly.Matthew-Neural";
    Voices["POLLY_CAMILA_NEURAL"] = "Polly.Camila-Neural";
    Voices["POLLY_LUPE_NEURAL"] = "Polly.Lupe-Neural";
})(Voices || (Voices = {}));
var ProviderSource;
(function (ProviderSource) {
    ProviderSource["BASIC"] = "Basic";
    ProviderSource["ALICE"] = "Alice";
    ProviderSource["AMAZON_POLLY"] = "Amazon Polly";
})(ProviderSource || (ProviderSource = {}));
const BasicLanguages = new Map([
    ['en', { viewValue: 'English, United States', value: 'en', voices: [Voices.MAN, Voices.WOMAN] }],
    ['en-gb', { viewValue: 'English, British', value: 'en-gb', voices: [Voices.MAN, Voices.WOMAN] }],
    ['es', { viewValue: 'Spanish, Spain', value: 'es', voices: [Voices.MAN, Voices.WOMAN] }],
    ['fr', { viewValue: 'French', value: 'fr', voices: [Voices.MAN, Voices.WOMAN] }],
    ['de', { viewValue: 'German', value: 'de', voices: [Voices.MAN, Voices.WOMAN] }]
]);
const AliceLanguages = new Map([
    ['da-DK', { viewValue: 'Danish, Denmark', value: 'da-DK', voices: [Voices.ALICE] }],
    ['de-DE', { viewValue: 'German, Germany', value: 'de-DE', voices: [Voices.ALICE] }],
    ['en-AU', { viewValue: 'English, Australia', value: 'en-AU', voices: [Voices.ALICE] }],
    ['en-CA', { viewValue: 'English, Canada', value: 'en-CA', voices: [Voices.ALICE] }],
    ['en-GB', { viewValue: 'English, UK', value: 'en-GB', voices: [Voices.ALICE] }],
    ['en-IN', { viewValue: 'English, India', value: 'en-IN', voices: [Voices.ALICE] }],
    ['en-US', { viewValue: 'English, United States', value: 'en-US', voices: [Voices.ALICE] }],
    ['ca-ES', { viewValue: 'Catalan, Spain', value: 'ca-ES', voices: [Voices.ALICE] }],
    ['es-ES', { viewValue: 'Spanish, Spain', value: 'es-ES', voices: [Voices.ALICE] }],
    ['es-MX', { viewValue: 'Spanish, Mexico', value: 'es-MX', voices: [Voices.ALICE] }],
    ['fi-FI', { viewValue: 'Finnish, Finland', value: 'fi-FI', voices: [Voices.ALICE] }],
    ['fr-CA', { viewValue: 'French, Canada', value: 'fr-CA', voices: [Voices.ALICE] }],
    ['fr-FR', { viewValue: 'French, France', value: 'fr-FR', voices: [Voices.ALICE] }],
    ['it-IT', { viewValue: 'Italian, Italy', value: 'it-IT', voices: [Voices.ALICE] }],
    ['ja-JP', { viewValue: 'Japanese, Japan', value: 'ja-JP', voices: [Voices.ALICE] }],
    ['ko-KR', { viewValue: 'Korean, Korea', value: 'ko-KR', voices: [Voices.ALICE] }],
    ['nb-NO', { viewValue: 'Norwegian, Norway', value: 'nb-NO', voices: [Voices.ALICE] }],
    ['nl-NL', { viewValue: 'Dutch, Netherlands', value: 'nl-NL', voices: [Voices.ALICE] }],
    ['pl-PL', { viewValue: 'Polish-Poland', value: 'pl-PL', voices: [Voices.ALICE] }],
    ['pt-BR', { viewValue: 'Portuguese, Brazil', value: 'pt-BR', voices: [Voices.ALICE] }],
    ['pt-PT', { viewValue: 'Portuguese, Portugal', value: 'pt-PT', voices: [Voices.ALICE] }],
    ['ru-RU', { viewValue: 'Russian, Russia', value: 'ru-RU', voices: [Voices.ALICE] }],
    ['sv-SE', { viewValue: 'Swedish, Sweden', value: 'sv-SE', voices: [Voices.ALICE] }],
    ['zh-CN', { viewValue: 'Chinese (Mandarin)', value: 'zh-CN', voices: [Voices.ALICE] }],
    ['zh-HK', { viewValue: 'Chinese (Cantonese)', value: 'zh-HK', voices: [Voices.ALICE] }],
    ['zh-TW', { viewValue: 'Chinese (Taiwanese Mandarin)', value: 'zh-TW', voices: [Voices.ALICE] }]
]);
const AmazonPollyLanguages = new Map([
    ['arb', { viewValue: 'Arabic', value: 'arb', voices: [Voices.POLLY_ZEINA] }],
    ['cy-GB', { viewValue: 'Welsh', value: 'cy-GB', voices: [Voices.POLLY_GWYNETH] }],
    ['da-DK', { viewValue: 'Danish', value: 'da-DK', voices: [Voices.POLLY_NAJA, Voices.POLLY_MADS] }],
    ['de-DE', { viewValue: 'German', value: 'de-DE', voices: [Voices.POLLY_MARLENE, Voices.POLLY_VICKI, Voices.POLLY_HANS] }],
    ['en-AU', { viewValue: 'English (Australian)', value: 'en-AU', voices: [Voices.POLLY_NICOLE, Voices.POLLY_RUSSELL] }],
    ['en-GB', {
            viewValue: 'English (British)',
            value: 'en-GB',
            voices: [Voices.POLLY_AMY, Voices.POLLY_EMMA, Voices.POLLY_BRIAN, Voices.POLLY_AMY_NEURAL, Voices.POLLY_EMMA_NEURAL,
                Voices.POLLY_BRIAN_NEURAL]
        }],
    ['en-GB', { viewValue: 'English (Welsh)', value: 'en-GB-WLS', voices: [Voices.POLLY_GERAINT] }],
    ['en-IN', { viewValue: 'English (Indian)', value: 'en-IN', voices: [Voices.POLLY_ADITI, Voices.POLLY_RAVEENA] }],
    ['en-US', {
            viewValue: 'English (US)',
            value: 'en-US',
            voices: [Voices.POLLY_SALLI, Voices.POLLY_IVY, Voices.POLLY_JOANNA, Voices.POLLY_KENDRA, Voices.POLLY_KIMBERLY, Voices.POLLY_JOEY,
                Voices.POLLY_JUSTIN, Voices.POLLY_MATTHEW, Voices.POLLY_SALLI_NEURAL, Voices.POLLY_IVY_NEURAL, Voices.POLLY_JOANNA_NEURAL,
                Voices.POLLY_KENDRA_NEURAL, Voices.POLLY_KIMBERLY_NEURAL, Voices.POLLY_JOEY_NEURAL, Voices.POLLY_JUSTIN_NEURAL,
                Voices.POLLY_MATTHEW_NEURAL]
        }],
    ['es-ES', {
            viewValue: 'Spanish (Castilian)',
            value: 'es-ES',
            voices: [Voices.POLLY_CONCHITA, Voices.POLLY_ENRIQUE, Voices.POLLY_LUCIA]
        }],
    ['es-MX', { viewValue: 'Spanish (Mexico)', value: 'es-MX', voices: [Voices.POLLY_MIA] }],
    ['es-US', {
            viewValue: 'Spanish (Latin American)',
            value: 'es-US',
            voices: [Voices.POLLY_LUPE, Voices.POLLY_PENELOPE, Voices.POLLY_MIGUEL, Voices.POLLY_LUPE_NEURAL]
        }],
    ['fr-CA', { viewValue: 'French (Canadian)', value: 'fr-CA', voices: [Voices.POLLY_CHANTAL] }],
    ['fr-FR', { viewValue: 'French', value: 'fr-FR', voices: [Voices.POLLY_CELINE, Voices.POLLY_MATHIEU, Voices.POLLY_LEA] }],
    ['hi-IN', { viewValue: 'Hindi', value: 'hi-IN', voices: [Voices.POLLY_ADITI] }],
    ['is-IS', { viewValue: 'Icelandic', value: 'is-IS', voices: [Voices.POLLY_DORA, Voices.POLLY_KARL] }],
    ['it-IT', { viewValue: 'Italian', value: 'it-IT', voices: [Voices.POLLY_CARLA, Voices.POLLY_GIORGIO, Voices.POLLY_BIANCA] }],
    ['ja-JP', { viewValue: 'Japanese', value: 'ja-JP', voices: [Voices.POLLY_MIZUKI, Voices.POLLY_TAKUMI] }],
    ['ko-KR', { viewValue: 'Korean', value: 'ko-KR', voices: [Voices.POLLY_SEOYEON] }],
    ['nb-NO', { viewValue: 'Norwegian', value: 'nb-NO', voices: [Voices.POLLY_LIV] }],
    ['nl-NL', { viewValue: 'Dutch', value: 'nl-NL', voices: [Voices.POLLY_LOTTE, Voices.POLLY_RUBEN] }],
    ['pl-PL', { viewValue: 'Polish', value: 'pl-PL', voices: [Voices.POLLY_EWA, Voices.POLLY_MAJA, Voices.POLLY_JAN, Voices.POLLY_JACEK] }],
    ['pt-BR', {
            viewValue: 'Portuguese (Brazilian)',
            value: 'pt-BR',
            voices: [Voices.POLLY_CAMILA, Voices.POLLY_VITORIA, Voices.POLLY_RICARDO, Voices.POLLY_CAMILA_NEURAL]
        }],
    ['pt-PT', { viewValue: 'Portuguese (European)', value: 'pt-PT', voices: [Voices.POLLY_INES, Voices.POLLY_CRISTIANO] }],
    ['ro-RO', { viewValue: 'Romanian', value: 'ro-RO', voices: [Voices.POLLY_CARMEN] }],
    ['ru-RU', { viewValue: 'Russian', value: 'ru-RU', voices: [Voices.POLLY_TATYANA, Voices.POLLY_MAXIM] }],
    ['sv-SE', { viewValue: 'Swedish', value: 'sv-SE', voices: [Voices.POLLY_ASTRID] }],
    ['tr-TR', { viewValue: 'Turkish', value: 'tr-TR', voices: [Voices.POLLY_FILIZ] }],
    ['zh-CN', { viewValue: 'Chinese (Mandarin)', value: 'zh-CN', voices: [Voices.POLLY_ZHIYU] }]
]);
const voiceConfiguration = new Map([
    [ProviderSource.BASIC, BasicLanguages],
    [ProviderSource.ALICE, AliceLanguages],
    [ProviderSource.AMAZON_POLLY, AmazonPollyLanguages]
]);

class TwilioVoiceConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.voiceConfiguration = voiceConfiguration;
        this.providers = ProviderSource;
        this.languages = [];
        this.voices = [];
    }
    configForm() {
        return this.twilioVoiceConfigForm;
    }
    updateConfiguration(configuration) {
        var _a, _b, _c;
        super.updateConfiguration(configuration);
        if (this.configuration.provider !== null) {
            this.languages = Array.from((_a = this.voiceConfiguration.get(configuration === null || configuration === void 0 ? void 0 : configuration.provider)) === null || _a === void 0 ? void 0 : _a.values());
            this.voices = (_c = (_b = this.voiceConfiguration.get(configuration === null || configuration === void 0 ? void 0 : configuration.provider)) === null || _b === void 0 ? void 0 : _b.get(configuration === null || configuration === void 0 ? void 0 : configuration.language)) === null || _c === void 0 ? void 0 : _c.voices;
        }
    }
    onConfigurationSet(configuration) {
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
        this.twilioVoiceConfigForm.get('provider').valueChanges.subscribe(provider => {
            var _a;
            this.languages = Array.from((_a = this.voiceConfiguration.get(provider)) === null || _a === void 0 ? void 0 : _a.values());
            this.voices = provider === ProviderSource.ALICE ? [Voices.ALICE] : [];
            this.twilioVoiceConfigForm.patchValue({
                language: null,
                voice: provider === ProviderSource.ALICE ? Voices.ALICE : null
            }, { emitEvent: false });
        });
        this.twilioVoiceConfigForm.get('language').valueChanges.subscribe((language) => {
            var _a;
            this.voices = Array.from((_a = this.voiceConfiguration.get(this.twilioVoiceConfigForm.get('provider').value)) === null || _a === void 0 ? void 0 : _a.get(language).voices);
            this.twilioVoiceConfigForm.patchValue({
                voice: this.twilioVoiceConfigForm.get('provider').value === ProviderSource.ALICE ? Voices.ALICE : null
            }, { emitEvent: false });
        });
    }
}
TwilioVoiceConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TwilioVoiceConfigComponent, deps: [{ token: i1$1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
TwilioVoiceConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: TwilioVoiceConfigComponent, selector: "tb-action-node-twilio-voice-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"twilioVoiceConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.twilio.number-from</mat-label>\n    <input required matInput formControlName=\"numberFrom\">\n    <mat-error *ngIf=\"twilioVoiceConfigForm.get('numberFrom').hasError('required')\">\n      {{ 'tb.twilio.number-from-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.twilio.numbers-to</mat-label>\n    <input required matInput formControlName=\"numbersTo\">\n    <mat-error *ngIf=\"twilioVoiceConfigForm.get('numbersTo').hasError('required')\">\n      {{ 'tb.twilio.numbers-to-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.twilio.numbers-to-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.twilio.account-sid</mat-label>\n    <input required matInput formControlName=\"accountSid\">\n    <mat-error *ngIf=\"twilioVoiceConfigForm.get('accountSid').hasError('required')\">\n      {{ 'tb.twilio.account-sid-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.twilio.account-token</mat-label>\n    <input required type=\"password\" autocomplete=\"new-password\" matInput formControlName=\"accountToken\">\n    <mat-error *ngIf=\"twilioVoiceConfigForm.get('accountToken').hasError('required')\">\n      {{ 'tb.twilio.account-token-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <section>\n    <mat-form-field class=\"mat-block\">\n      <mat-label translate>tb.twilio.provider</mat-label>\n      <mat-select formControlName=\"provider\">\n        <mat-option *ngFor=\"let provider of providers | keyvalue\" [value]=\"provider.value\">\n          {{ provider.value }}\n        </mat-option>\n      </mat-select>\n      <mat-error *ngIf=\"twilioVoiceConfigForm.get('provider').hasError('required')\">\n        {{ 'tb.twilio.provider-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field class=\"mat-block\">\n      <mat-label translate>tb.twilio.language</mat-label>\n      <mat-select required formControlName=\"language\">\n        <mat-option *ngFor=\"let language of languages\" [value]=\"language.value\">\n          {{ language.viewValue }}\n        </mat-option>\n      </mat-select>\n      <mat-error *ngIf=\"twilioVoiceConfigForm.get('language').hasError('required')\">\n        {{ 'tb.twilio.language-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field class=\"mat-block\" *ngIf=\"this.configuration.provider!='Alice'\" disabled=\"this.twilioVoiceConfigForm.get('language').value\">\n      <mat-label translate>tb.twilio.voice</mat-label>\n      <mat-select required formControlName=\"voice\">\n        <mat-option *ngFor=\"let voice of voices\" [value]=\"voice\">\n          {{ voice }}\n        </mat-option>\n      </mat-select>\n      <mat-error *ngIf=\"twilioVoiceConfigForm.get('voice').hasError('required')\">\n        {{ 'tb.twilio.voice-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n  </section>\n  <section fxLayout=\"row\" fxLayoutGap=\"8px\">\n    <mat-form-field fxFlex class=\"mat-block\">\n      <mat-label translate>tb.twilio.pitch</mat-label>\n      <input required matInput type=\"number\" formControlName=\"pitch\" min=\"0\">\n      <span matSuffix> %</span>\n      <mat-error *ngIf=\"twilioVoiceConfigForm.get('pitch').hasError('required')\">\n        {{ 'tb.twilio.pitch-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field fxFlex class=\"mat-block\">\n      <mat-label translate>tb.twilio.rate</mat-label>\n      <input required matInput type=\"number\" formControlName=\"rate\" min=\"0\">\n      <span matSuffix> %</span>\n      <mat-error *ngIf=\"twilioVoiceConfigForm.get('rate').hasError('required')\">\n        {{ 'tb.twilio.rate-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field fxFlex class=\"mat-block\">\n      <mat-label translate>tb.twilio.volume</mat-label>\n      <input required matInput type=\"number\" formControlName=\"volume\">\n      <span matSuffix> dB</span>\n      <mat-error *ngIf=\"twilioVoiceConfigForm.get('volume').hasError('required')\">\n        {{ 'tb.twilio.volume-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n  </section>\n  <section>\n    <mat-form-field class=\"mat-block\">\n      <mat-label translate>tb.twilio.start-pause</mat-label>\n      <span matSuffix> second</span>\n      <input required matInput type=\"number\" formControlName=\"startPause\" min=\"0\">\n    </mat-form-field>\n  </section>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5$1.MatOption, selector: "mat-option", exportAs: ["matOption"] }], directives: [{ type: i6.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i5.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i6$1.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i9.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i9.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i6.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i6.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i3.MatSuffix, selector: "[matSuffix]" }], pipes: { "translate": i5.TranslatePipe, "safeHtml": SafeHtmlPipe, "keyvalue": i9.KeyValuePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TwilioVoiceConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-twilio-voice-config',
                    templateUrl: './twilio-voice-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1$1.Store }, { type: i2.FormBuilder }]; } });

function addTwilioLocaleEnglish(translate) {
    const enUS = {
        tb: {
            'twilio': {
                'number-from': 'Phone Number From',
                'number-from-required': 'Phone Number From is required.',
                'numbers-to': 'Phone Numbers To',
                'numbers-to-required': 'Phone Numbers To is required.',
                'numbers-to-hint': 'Comma separated Phone Numbers, use <code><span style="color: #000;">$&#123;</span>' +
                    'metadataKey<span style="color: #000;">&#125;</span></code> for value from metadata, <code><span style="color: #000;">' +
                    '$[</span>messageKey<span style="color: #000;">]</span></code> for value from message body',
                'account-sid': 'Twilio Account SID',
                'account-sid-required': 'Twilio Account SID is required',
                'account-token': 'Twilio Account Token',
                'account-token-required': 'Twilio Account Token is required',
                'provider': 'Voice provider',
                'provider-required': 'Voice provider is required',
                'language': 'Language',
                'language-required': 'Language is required',
                'voice': 'Voice',
                'voice-required': 'Voice is required',
                'pitch': 'Pitch',
                'pitch-required': 'Pitch is required',
                'volume': 'Volume',
                'volume-required': 'Volume is required',
                'rate': 'Rate',
                'rate-required': 'Rate is required',
                'start-pause': 'Pause before talking'
            }
        }
    };
    translate.setTranslation('en_US', enUS, true);
}

class TwilioConfigModule {
    constructor(translate) {
        addTwilioLocaleEnglish(translate);
    }
}
TwilioConfigModule.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TwilioConfigModule, deps: [{ token: i5.TranslateService }], target: i0.ɵɵFactoryTarget.NgModule });
TwilioConfigModule.ɵmod = i0.ɵɵngDeclareNgModule({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TwilioConfigModule, declarations: [TwilioSmsConfigComponent,
        TwilioVoiceConfigComponent,
        SafeHtmlPipe], imports: [CommonModule,
        SharedModule,
        HomeComponentsModule], exports: [TwilioSmsConfigComponent,
        TwilioVoiceConfigComponent] });
TwilioConfigModule.ɵinj = i0.ɵɵngDeclareInjector({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TwilioConfigModule, imports: [[
            CommonModule,
            SharedModule,
            HomeComponentsModule
        ]] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TwilioConfigModule, decorators: [{
            type: NgModule,
            args: [{
                    declarations: [
                        TwilioSmsConfigComponent,
                        TwilioVoiceConfigComponent,
                        SafeHtmlPipe
                    ],
                    imports: [
                        CommonModule,
                        SharedModule,
                        HomeComponentsModule
                    ],
                    exports: [
                        TwilioSmsConfigComponent,
                        TwilioVoiceConfigComponent
                    ]
                }]
        }], ctorParameters: function () { return [{ type: i5.TranslateService }]; } });

/*
 * Public API Surface of rule-core-config
 */

/**
 * Generated bundle index. Do not edit.
 */

export { TwilioConfigModule, TwilioSmsConfigComponent, TwilioVoiceConfigComponent };
//# sourceMappingURL=twilio-config.js.map
