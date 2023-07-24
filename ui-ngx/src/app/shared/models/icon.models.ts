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

import { ResourcesService } from '@core/services/resources.service';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { isNotEmptyStr } from '@core/utils';

export interface MaterialIcon {
  name: string;
  displayName?: string;
  tags: string[];
}

export const iconByName = (icons: Array<MaterialIcon>, name: string): MaterialIcon => icons.find(i => i.name === name);

const searchIconTags = (icon: MaterialIcon, searchText: string): boolean =>
  !!icon.tags.find(t => t.toUpperCase().includes(searchText.toUpperCase()));

const searchIcons = (_icons: Array<MaterialIcon>, searchText: string): Array<MaterialIcon> => _icons.filter(
  i => i.name.toUpperCase().includes(searchText.toUpperCase()) ||
    i.displayName.toUpperCase().includes(searchText.toUpperCase()) ||
    searchIconTags(i, searchText)
);

const getCommonMaterialIcons = (icons: Array<MaterialIcon>, chunkSize: number): Array<MaterialIcon> => icons.slice(0, chunkSize * 4);

export const getMaterialIcons = (resourcesService: ResourcesService,  chunkSize = 11,
                                 all = false, searchText: string): Observable<MaterialIcon[][]> =>
  resourcesService.loadJsonResource<Array<MaterialIcon>>('/assets/metadata/material-icons.json',
    (icons) => {
      for (const icon of icons) {
        const words = icon.name.replace(/_/g, ' ').split(' ');
        for (let i = 0; i < words.length; i++) {
          words[i] = words[i].charAt(0).toUpperCase() + words[i].slice(1);
        }
        icon.displayName = words.join(' ');
      }
      return icons;
    }
  ).pipe(
    map((icons) => {
      if (isNotEmptyStr(searchText)) {
        return searchIcons(icons, searchText);
      } else if (!all) {
        return getCommonMaterialIcons(icons, chunkSize);
      } else {
        return icons;
      }
    }),
    map((icons) => {
      const iconChunks: MaterialIcon[][] = [];
      for (let i = 0; i < icons.length; i += chunkSize) {
        const chunk = icons.slice(i, i + chunkSize);
        iconChunks.push(chunk);
      }
      return iconChunks;
    })
  );
