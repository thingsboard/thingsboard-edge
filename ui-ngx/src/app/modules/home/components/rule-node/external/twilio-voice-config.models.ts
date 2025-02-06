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

export enum Voices {
  MAN = 'man',
  WOMAN = 'woman',
  ALICE = 'alice',
  POLLY_ADITI = 'Polly.Aditi',
  POLLY_AMY = 'Polly.Amy',
  POLLY_ASTRID = 'Polly.Astrid',
  POLLY_BIANCA = 'Polly.Bianca',
  POLLY_BRIAN = 'Polly.Brian',
  POLLY_CAMILA = 'Polly.Camila',
  POLLY_CARLA = 'Polly.Carla',
  POLLY_CARMEN = 'Polly.Carmen',
  POLLY_CELINE = 'Polly.Celine',
  POLLY_CHANTAL = 'Polly.Chantal',
  POLLY_CONCHITA = 'Polly.Conchita',
  POLLY_CRISTIANO = 'Polly.Cristiano',
  POLLY_DORA = 'Polly.Dora',
  POLLY_EMMA = 'Polly.Emma',
  POLLY_ENRIQUE = 'Polly.Enrique',
  POLLY_EWA = 'Polly.Ewa',
  POLLY_FILIZ = 'Polly.Filiz',
  POLLY_GERAINT = 'Polly.Geraint',
  POLLY_GIORGIO = 'Polly.Giorgio',
  POLLY_GWYNETH = 'Polly.Gwyneth',
  POLLY_HANS = 'Polly.Hans',
  POLLY_INES = 'Polly.Ines',
  POLLY_IVY = 'Polly.Ivy',
  POLLY_JACEK = 'Polly.Jacek',
  POLLY_JAN = 'Polly.Jan',
  POLLY_JOANNA = 'Polly.Joanna',
  POLLY_JOEY = 'Polly.Joey',
  POLLY_JUSTIN = 'Polly.Justin',
  POLLY_KARL = 'Polly.Karl',
  POLLY_KENDRA = 'Polly.Kendra',
  POLLY_KIMBERLY = 'Polly.Kimberly',
  POLLY_LEA = 'Polly.Lea',
  POLLY_LIV = 'Polly.Liv',
  POLLY_LOTTE = 'Polly.Lotte',
  POLLY_LUCIA = 'Polly.Lucia',
  POLLY_LUPE = 'Polly.Lupe',
  POLLY_MADS = 'Polly.Mads',
  POLLY_MAJA = 'Polly.Maja',
  POLLY_MARLENE = 'Polly.Marlene',
  POLLY_MATHIEU = 'Polly.Mathieu',
  POLLY_MATTHEW = 'Polly.Matthew',
  POLLY_MAXIM = 'Polly.Maxim',
  POLLY_MIA = 'Polly.Mia',
  POLLY_MIGUEL = 'Polly.Miguel',
  POLLY_MIZUKI = 'Polly.Mizuki',
  POLLY_NAJA = 'Polly.Naja',
  POLLY_NICOLE = 'Polly.Nicole',
  POLLY_PENELOPE = 'Polly.Penelope',
  POLLY_RAVEENA = 'Polly.Raveena',
  POLLY_RICARDO = 'Polly.Ricardo',
  POLLY_RUBEN = 'Polly.Ruben',
  POLLY_RUSSELL = 'Polly.Russell',
  POLLY_SALLI = 'Polly.Salli',
  POLLY_SEOYEON = 'Polly.Seoyeon',
  POLLY_TAKUMI = 'Polly.Takumi',
  POLLY_TATYANA = 'Polly.Tatyana',
  POLLY_VICKI = 'Polly.Vicki',
  POLLY_VITORIA = 'Polly.Vitoria',
  POLLY_ZEINA = 'Polly.Zeina',
  POLLY_ZHIYU = 'Polly.Zhiyu',
  POLLY_AMY_NEURAL = 'Polly.Amy-Neural',
  POLLY_EMMA_NEURAL = 'Polly.Emma-Neural',
  POLLY_BRIAN_NEURAL = 'Polly.Brian-Neural',
  POLLY_SALLI_NEURAL = 'Polly.Salli-Neural',
  POLLY_IVY_NEURAL = 'Polly.Ivy-Neural',
  POLLY_JOANNA_NEURAL = 'Polly.Joanna-Neural',
  POLLY_KENDRA_NEURAL = 'Polly.Kendra-Neural',
  POLLY_KIMBERLY_NEURAL = 'Polly.Kimberly-Neural',
  POLLY_JOEY_NEURAL = 'Polly.Joey-Neural',
  POLLY_JUSTIN_NEURAL = 'Polly.Justin-Neural',
  POLLY_MATTHEW_NEURAL = 'Polly.Matthew-Neural',
  POLLY_CAMILA_NEURAL = 'Polly.Camila-Neural',
  POLLY_LUPE_NEURAL = 'Polly.Lupe-Neural'
}

export enum ProviderSource {
  BASIC = 'Basic',
  ALICE = 'Alice',
  AMAZON_POLLY = 'Amazon Polly'
}

export interface Language {
  viewValue: string;
  value: string;
  voices: Voices[];
}

export const BasicLanguages = new Map<string, Language>(
  [
    ['en', {viewValue: 'English, United States', value: 'en', voices: [Voices.MAN, Voices.WOMAN]}],
    ['en-gb', {viewValue: 'English, British', value: 'en-gb', voices: [Voices.MAN, Voices.WOMAN]}],
    ['es', {viewValue: 'Spanish, Spain', value: 'es', voices: [Voices.MAN, Voices.WOMAN]}],
    ['fr', {viewValue: 'French', value: 'fr', voices: [Voices.MAN, Voices.WOMAN]}],
    ['de', {viewValue: 'German', value: 'de', voices: [Voices.MAN, Voices.WOMAN]}]
  ]
)

export const AliceLanguages = new Map<string, Language>(
  [
    ['da-DK', {viewValue: 'Danish, Denmark', value: 'da-DK', voices: [Voices.ALICE]}],
    ['de-DE', {viewValue: 'German, Germany', value: 'de-DE', voices: [Voices.ALICE]}],
    ['en-AU', {viewValue: 'English, Australia', value: 'en-AU', voices: [Voices.ALICE]}],
    ['en-CA', {viewValue: 'English, Canada', value: 'en-CA', voices: [Voices.ALICE]}],
    ['en-GB', {viewValue: 'English, UK', value: 'en-GB', voices: [Voices.ALICE]}],
    ['en-IN', {viewValue: 'English, India', value: 'en-IN', voices: [Voices.ALICE]}],
    ['en-US', {viewValue: 'English, United States', value: 'en-US', voices: [Voices.ALICE]}],
    ['ca-ES', {viewValue: 'Catalan, Spain', value: 'ca-ES', voices: [Voices.ALICE]}],
    ['es-ES', {viewValue: 'Spanish, Spain', value: 'es-ES', voices: [Voices.ALICE]}],
    ['es-MX', {viewValue: 'Spanish, Mexico', value: 'es-MX', voices: [Voices.ALICE]}],
    ['fi-FI', {viewValue: 'Finnish, Finland', value: 'fi-FI', voices: [Voices.ALICE]}],
    ['fr-CA', {viewValue: 'French, Canada', value: 'fr-CA', voices: [Voices.ALICE]}],
    ['fr-FR', {viewValue: 'French, France', value: 'fr-FR', voices: [Voices.ALICE]}],
    ['it-IT', {viewValue: 'Italian, Italy', value: 'it-IT', voices: [Voices.ALICE]}],
    ['ja-JP', {viewValue: 'Japanese, Japan', value: 'ja-JP', voices: [Voices.ALICE]}],
    ['ko-KR', {viewValue: 'Korean, Korea', value: 'ko-KR', voices: [Voices.ALICE]}],
    ['nb-NO', {viewValue: 'Norwegian, Norway', value: 'nb-NO', voices: [Voices.ALICE]}],
    ['nl-NL', {viewValue: 'Dutch, Netherlands', value: 'nl-NL', voices: [Voices.ALICE]}],
    ['pl-PL', {viewValue: 'Polish-Poland', value: 'pl-PL', voices: [Voices.ALICE]}],
    ['pt-BR', {viewValue: 'Portuguese, Brazil', value: 'pt-BR', voices: [Voices.ALICE]}],
    ['pt-PT', {viewValue: 'Portuguese, Portugal', value: 'pt-PT', voices: [Voices.ALICE]}],
    ['ru-RU', {viewValue: 'Russian, Russia', value: 'ru-RU', voices: [Voices.ALICE]}],
    ['sv-SE', {viewValue: 'Swedish, Sweden', value: 'sv-SE', voices: [Voices.ALICE]}],
    ['zh-CN', {viewValue: 'Chinese (Mandarin)', value: 'zh-CN', voices: [Voices.ALICE]}],
    ['zh-HK', {viewValue: 'Chinese (Cantonese)', value: 'zh-HK', voices: [Voices.ALICE]}],
    ['zh-TW', {viewValue: 'Chinese (Taiwanese Mandarin)', value: 'zh-TW', voices: [Voices.ALICE]}]
  ]
)

export const AmazonPollyLanguages = new Map<string, Language>(
  [
    ['arb', {viewValue: 'Arabic', value: 'arb', voices: [Voices.POLLY_ZEINA]}],
    ['cy-GB', {viewValue: 'Welsh', value: 'cy-GB', voices: [Voices.POLLY_GWYNETH]}],
    ['da-DK', {viewValue: 'Danish', value: 'da-DK', voices: [Voices.POLLY_NAJA, Voices.POLLY_MADS]}],
    ['de-DE', {viewValue: 'German', value: 'de-DE', voices: [Voices.POLLY_MARLENE, Voices.POLLY_VICKI, Voices.POLLY_HANS]}],
    ['en-AU', {viewValue: 'English (Australian)', value: 'en-AU', voices: [Voices.POLLY_NICOLE, Voices.POLLY_RUSSELL]}],
    ['en-GB', {
      viewValue: 'English (British)',
      value: 'en-GB',
      voices: [Voices.POLLY_AMY, Voices.POLLY_EMMA, Voices.POLLY_BRIAN, Voices.POLLY_AMY_NEURAL, Voices.POLLY_EMMA_NEURAL,
        Voices.POLLY_BRIAN_NEURAL]
    }],
    ['en-GB', {viewValue: 'English (Welsh)', value: 'en-GB-WLS', voices: [Voices.POLLY_GERAINT]}],
    ['en-IN', {viewValue: 'English (Indian)', value: 'en-IN', voices: [Voices.POLLY_ADITI, Voices.POLLY_RAVEENA]}],
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
    ['es-MX', {viewValue: 'Spanish (Mexico)', value: 'es-MX', voices: [Voices.POLLY_MIA]}],
    ['es-US', {
      viewValue: 'Spanish (Latin American)',
      value: 'es-US',
      voices: [Voices.POLLY_LUPE, Voices.POLLY_PENELOPE, Voices.POLLY_MIGUEL, Voices.POLLY_LUPE_NEURAL]
    }],
    ['fr-CA', {viewValue: 'French (Canadian)', value: 'fr-CA', voices: [Voices.POLLY_CHANTAL]}],
    ['fr-FR', {viewValue: 'French', value: 'fr-FR', voices: [Voices.POLLY_CELINE, Voices.POLLY_MATHIEU, Voices.POLLY_LEA]}],
    ['hi-IN', {viewValue: 'Hindi', value: 'hi-IN', voices: [Voices.POLLY_ADITI]}],
    ['is-IS', {viewValue: 'Icelandic', value: 'is-IS', voices: [Voices.POLLY_DORA, Voices.POLLY_KARL]}],
    ['it-IT', {viewValue: 'Italian', value: 'it-IT', voices: [Voices.POLLY_CARLA, Voices.POLLY_GIORGIO, Voices.POLLY_BIANCA]}],
    ['ja-JP', {viewValue: 'Japanese', value: 'ja-JP', voices: [Voices.POLLY_MIZUKI, Voices.POLLY_TAKUMI]}],
    ['ko-KR', {viewValue: 'Korean', value: 'ko-KR', voices: [Voices.POLLY_SEOYEON]}],
    ['nb-NO', {viewValue: 'Norwegian', value: 'nb-NO', voices: [Voices.POLLY_LIV]}],
    ['nl-NL', {viewValue: 'Dutch', value: 'nl-NL', voices: [Voices.POLLY_LOTTE, Voices.POLLY_RUBEN]}],
    ['pl-PL', {viewValue: 'Polish', value: 'pl-PL', voices: [Voices.POLLY_EWA, Voices.POLLY_MAJA, Voices.POLLY_JAN, Voices.POLLY_JACEK]}],
    ['pt-BR', {
      viewValue: 'Portuguese (Brazilian)',
      value: 'pt-BR',
      voices: [Voices.POLLY_CAMILA, Voices.POLLY_VITORIA, Voices.POLLY_RICARDO, Voices.POLLY_CAMILA_NEURAL]
    }],
    ['pt-PT', {viewValue: 'Portuguese (European)', value: 'pt-PT', voices: [Voices.POLLY_INES, Voices.POLLY_CRISTIANO]}],
    ['ro-RO', {viewValue: 'Romanian', value: 'ro-RO', voices: [Voices.POLLY_CARMEN]}],
    ['ru-RU', {viewValue: 'Russian', value: 'ru-RU', voices: [Voices.POLLY_TATYANA, Voices.POLLY_MAXIM]}],
    ['sv-SE', {viewValue: 'Swedish', value: 'sv-SE', voices: [Voices.POLLY_ASTRID]}],
    ['tr-TR', {viewValue: 'Turkish', value: 'tr-TR', voices: [Voices.POLLY_FILIZ]}],
    ['zh-CN', {viewValue: 'Chinese (Mandarin)', value: 'zh-CN', voices: [Voices.POLLY_ZHIYU]}]
  ]
)

export const voiceConfiguration = new Map<ProviderSource, Map<string, Language>>(
  [
    [ProviderSource.BASIC, BasicLanguages],
    [ProviderSource.ALICE, AliceLanguages],
    [ProviderSource.AMAZON_POLLY, AmazonPollyLanguages]
  ]
)
