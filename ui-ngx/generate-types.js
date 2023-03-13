/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
const child_process = require("child_process");
const fs = require('fs');
const path = require('path');

const typeDir = path.join('.', 'target', 'types');
const srcDir = path.join('.', 'target', 'types', 'src');
const moduleMapPath = path.join('src', 'app', 'modules', 'common', 'modules-map.ts');
const ngcPath = path.join('.', 'node_modules', '.bin', 'ngc');
const tsconfigPath = path.join('src', 'tsconfig.app.json');

console.log(`Remove directory: ${typeDir}`);
try {
  fs.rmSync(typeDir, {recursive: true, force: true,});
} catch (err) {
  console.error(`Remove directory error: ${err}`);
}

const cliCommand = `${ngcPath} --p ${tsconfigPath} --declaration --outDir ${srcDir}`;
console.log(cliCommand);
try {
  child_process.execSync(cliCommand);
} catch (err) {
  console.error("Build types", err);
  process.exit(1);
}

function fromDir(startPath, filter, callback) {
  if (!fs.existsSync(startPath)) {
    console.log("not dirs", startPath);
    process.exit(1);
  }

  const files = fs.readdirSync(startPath);
  for (let i = 0; i < files.length; i++) {
    const filename = path.join(startPath, files[i]);
    const stat = fs.lstatSync(filename);
    if (stat.isDirectory()) {
      fromDir(filename, filter, callback);
    } else if (filter.test(filename)) {
      callback(filename)
    }
  }
}


fromDir(srcDir, /(\.js|\.js\.map)$/, function (filename) {
  try {
    fs.rmSync(filename);
  } catch (err) {
    console.error(`Remove file error ${filename}: ${err}`);
  }
});
fs.cpSync(moduleMapPath, `${typeDir}/${moduleMapPath}`);
