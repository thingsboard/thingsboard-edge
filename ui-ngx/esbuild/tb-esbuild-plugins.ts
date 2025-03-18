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

import type { Plugin, PluginBuild, OutputFile } from 'esbuild';
import dirTree from 'directory-tree';
import * as packageJson from '../package.json';
import { gzip } from 'node:zlib';
import * as path from 'node:path';

const defineTbVariablesPlugin: Plugin = {
  name: 'tb-define-variables',
  setup(build: PluginBuild) {
    const options = build.initialOptions;

    const langs: string[] = [];

    dirTree("./src/assets/locale/", {extensions: /\.json$/}, (item) => {
      /* It is expected what the name of a locale file has the following format: */
      /* 'locale.constant-LANG_CODE[_REGION_CODE].json', e.g. locale.constant-es.json or locale.constant-zh_CN.json*/
      langs.push(item.name.slice(item.name.lastIndexOf("-") + 1, -5));
    });
    options.define.TB_VERSION = JSON.stringify(packageJson.version);
    options.define.SUPPORTED_LANGS = JSON.stringify(langs);
    options.define.ngJitMode = 'true';
  },
};

const resolveJQueryPlugin: Plugin = {
  name: 'tb-resolve-jquery-plugin',
  setup(build: PluginBuild) {
    if (isProduction()) {
      const jQueryPath = require.resolve('jquery');
      build.onResolve({filter: /^(jquery|\$)$/}, () => {
        return {path: jQueryPath};
      })
    }
  }
};

const compressFileTypes = ['.js', '.css', '.html', '.svg', '.png', '.jpg', '.ttf', '.gif', '.woff', '.woff2', '.eot', '.json'];
const compressThreshold = 10240;

const compressorPlugin: Plugin = {
  name: 'tb-compressor-plugin',
  setup(build) {
    build.onEnd(async result => {
      if (!result.outputFiles || !isProduction()) return;
      const outputExt = '.gz';
      const gzippedFiles: OutputFile[] = [];
      for (const file of result.outputFiles) {
        if (!compressFileTypes.some((ext) => ext === path.extname(file.path))) continue;
        if (file.contents.byteLength <= compressThreshold) continue;
        const compressedContent = await gzipContent(file.contents);
        const compressedFilePath = `${file.path}${outputExt}`;
        gzippedFiles.push(
          {
            path: compressedFilePath,
            hash: file.hash,
            contents: new Uint8Array(compressedContent),
            text: '',
          }
        );
      }
      result.outputFiles.push(...gzippedFiles);
    });
  },
};

async function gzipContent(content): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    gzip(content, (error, result) => {
      if (error) {
        reject(error);
      } else {
        resolve(result);
      }
    });
  });
}

function isProduction(): boolean {
  const configurationIndex = process.argv.indexOf('--configuration');
  let production = false;
  if (configurationIndex > -1) {
    const configurationValue = process.argv[configurationIndex + 1];
    production = configurationValue === 'production';
  }
  return production;
}

export default [defineTbVariablesPlugin, resolveJQueryPlugin, compressorPlugin];
