/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
