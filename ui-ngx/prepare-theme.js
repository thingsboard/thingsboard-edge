/*
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const fs = require('fs-extra');
const path = require('path');

const SCSS_DIST_DIR = path.join(__dirname, 'target', 'classes', 'scss');
const SRC_THEME = path.join(__dirname, 'src', 'theme');
const SRC_THIRDPARTY_FILES = [
  path.join(__dirname, 'node_modules', '@angular', 'material', '_theming.scss'),
  path.join(__dirname, 'node_modules', '@mat-datetimepicker', 'core', 'datetimepicker', 'datetimepicker-theme.scss')
];

const REPLACEMENTS = {
  '~@angular/material/theming': '_theming',
  '~@mat-datetimepicker/core/datetimepicker/datetimepicker-theme.scss': 'datetimepicker-theme',
  '\\$primary-extends: \\$mat-tb-primary;': '$primary-extends: $mat-##primary-palette##;',
  '\\$primary-color-map: \\(\\);': '$primary-color-map: (##primary-colors##);',
  '\\$accent-extends: \\$mat-tb-accent;': '$accent-extends: $mat-##accent-palette##;',
  '\\$accent-color-map: \\(\\);': '$accent-color-map: (##accent-colors##);',
  '\\$dark-foreground: false;': '$dark-foreground: ##dark-foreground##;'
};

(async() => {

  console.log(`Preparing theme SCSS files`);

  try {
    await fs.copy(SRC_THEME, SCSS_DIST_DIR, {overwrite: true});
    console.log('Successfully copied Application Theme files!')
  } catch (err) {
    console.error('Failed to copy Application Theme files!');
    console.error(err);
    process.exit(-1);
  }
  try {
    for (const file of SRC_THIRDPARTY_FILES) {
      const destFile = path.join(SCSS_DIST_DIR, path.basename(file));
      await fs.copy(file, destFile, {overwrite: true});
    }
    console.log('Successfully copied ThirdParty Theme files!')
  } catch (err) {
    console.error('Failed to copy ThirdParty Theme files!');
    console.error(err);
    process.exit(-1);
  }

  try {
    const targetFiles = await fs.readdir(SCSS_DIST_DIR);
    for (const file of targetFiles) {
      for (const targetString of Object.keys(REPLACEMENTS)) {
        const replacement = REPLACEMENTS[targetString];
        await replace(path.join(SCSS_DIST_DIR, file), targetString, replacement);
      }
    }
  } catch (err) {
    console.error(err);
    process.exit(-1);
  }

})();

async function replace(targetFile, targetString, replacement) {
  try {
    const data = await fs.readFile(targetFile, 'utf8');
    const result = data.replace(new RegExp(targetString,'g'), replacement);
    await fs.writeFile(targetFile, result, 'utf8');
  } catch (err) {
    console.error(`Failed to made replacement inside file ${targetFile}!`);
    console.error(err);
    process.exit(-1);
  }
}
