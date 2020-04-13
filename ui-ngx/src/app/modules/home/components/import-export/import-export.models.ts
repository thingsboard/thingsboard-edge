///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Widget, WidgetType } from '@app/shared/models/widget.models';
import { DashboardLayoutId } from '@shared/models/dashboard.models';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';

export interface ImportWidgetResult {
  widget: Widget;
  layoutId: DashboardLayoutId;
}

export interface WidgetsBundleItem {
  widgetsBundle: WidgetsBundle;
  widgetTypes: WidgetType[];
}

export interface CsvToJsonConfig {
  delim?: string;
  header?: boolean;
}

export interface CsvToJsonResult {
  headers?: string[];
  rows?: any[][];
}

export enum ImportEntityColumnType {
  name = 'NAME',
  type = 'TYPE',
  label = 'LABEL',
  clientAttribute = 'CLIENT_ATTRIBUTE',
  sharedAttribute = 'SHARED_ATTRIBUTE',
  serverAttribute = 'SERVER_ATTRIBUTE',
  timeseries = 'TIMESERIES',
  entityField = 'ENTITY_FIELD',
  accessToken = 'ACCESS_TOKEN',
  isGateway = 'IS_GATEWAY',
  description = 'DESCRIPTION'
}

export const importEntityObjectColumns =
  [ImportEntityColumnType.name, ImportEntityColumnType.type, ImportEntityColumnType.accessToken];

export const importEntityColumnTypeTranslations = new Map<ImportEntityColumnType, string>(
  [
    [ImportEntityColumnType.name, 'import.column-type.name'],
    [ImportEntityColumnType.type, 'import.column-type.type'],
    [ImportEntityColumnType.label, 'import.column-type.label'],
    [ImportEntityColumnType.clientAttribute, 'import.column-type.client-attribute'],
    [ImportEntityColumnType.sharedAttribute, 'import.column-type.shared-attribute'],
    [ImportEntityColumnType.serverAttribute, 'import.column-type.server-attribute'],
    [ImportEntityColumnType.timeseries, 'import.column-type.timeseries'],
    [ImportEntityColumnType.entityField, 'import.column-type.entity-field'],
    [ImportEntityColumnType.accessToken, 'import.column-type.access-token'],
    [ImportEntityColumnType.isGateway, 'import.column-type.isgateway'],
    [ImportEntityColumnType.description, 'import.column-type.description'],
  ]
);

export interface CsvColumnParam {
  type: ImportEntityColumnType;
  key: string;
  sampleData: any;
}

export interface FileType {
  mimeType: string;
  extension: string;
}

export const JSON_TYPE: FileType = {
  mimeType: 'text/json',
  extension: 'json'
};

export const CSV_TYPE: FileType = {
  mimeType: 'attachament/csv',
  extension: 'csv'
};

export const XLS_TYPE: FileType = {
  mimeType: 'application/vnd.ms-excel',
  extension: 'xls'
};

export const XLSX_TYPE: FileType = {
  mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  extension: 'xlsx'
};

export const ZIP_TYPE: FileType = {
  mimeType: 'application/zip',
  extension: 'zip'
};


export const TEMPLATE_XLS = `
  <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
  <meta http-equiv="content-type" content="application/vnd.ms-excel; charset=UTF-8"/>
  <head><!--[if gte mso 9]><xml>
  <x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>{title}</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml>
  <![endif]--></head>
  <body>{table}</body></html>`;

export function convertCSVToJson(csvdata: string, config: CsvToJsonConfig,
                                 onError: (messageId: string, params?: any) => void): CsvToJsonResult | number {
  config = config || {};
  const delim = config.delim || ',';
  const header = config.header || false;
  const result: CsvToJsonResult = {};
  const csvlines = csvdata.split(/[\r\n]+/);
  const csvheaders = splitCSV(csvlines[0], delim);
  if (csvheaders.length < 2) {
    onError('import.import-csv-number-columns-error');
    return -1;
  }
  const csvrows = header ? csvlines.slice(1, csvlines.length) : csvlines;
  result.headers = csvheaders;
  result.rows = [];
  for (const row of csvrows) {
    if (row.length === 0) {
      break;
    }
    const rowitems: any[] = splitCSV(row, delim);
    if (rowitems.length !== result.headers.length) {
      onError('import.import-csv-invalid-format-error', {line: (header ? result.rows.length + 2 : result.rows.length + 1)});
      return -1;
    }
    for (let i = 0; i < rowitems.length; i++) {
      rowitems[i] = convertStringToJSType(rowitems[i]);
    }
    result.rows.push(rowitems);
  }
  return result;
}

function splitCSV(str: string, sep: string): string[] {
  let foo: string[];
  let x: number;
  let tl: string;
  for (foo = str.split(sep = sep || ','), x = foo.length - 1, tl; x >= 0; x--) {
    if (foo[x].replace(/"\s+$/, '"').charAt(foo[x].length - 1) === '"') {
      if ((tl = foo[x].replace(/^\s+"/, '"')).length > 1 && tl.charAt(0) === '"') {
        foo[x] = foo[x].replace(/^\s*"|"\s*$/g, '').replace(/""/g, '"');
      } else if (x) {
        foo.splice(x - 1, 2, [foo[x - 1], foo[x]].join(sep));
      } else {
        foo = foo.shift().split(sep).concat(foo);
      }
    } else {
      foo[x].replace(/""/g, '"');
    }
  }
  return foo;
}

function isNumeric(str: any): boolean {
  str = str.replace(',', '.');
  return !isNaN(parseFloat(str)) && isFinite(str);
}

function convertStringToJSType(str: string): any {
  if (isNumeric(str.replace(',', '.'))) {
    return parseFloat(str.replace(',', '.'));
  }
  if (str.search(/^(true|false)$/im) === 0) {
    return str.toLowerCase() === 'true';
  }
  if (str === '') {
    return null;
  }
  return str;
}
