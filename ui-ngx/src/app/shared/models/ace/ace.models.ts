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

import { Ace } from 'ace-builds';
import { Observable } from 'rxjs/internal/Observable';
import { forkJoin, from, of } from 'rxjs';
import { map, mergeMap, tap } from 'rxjs/operators';

let aceDependenciesLoaded = false;
let aceModule: any;
let aceDiffModule: any;

function loadAceDependencies(): Observable<any> {
  if (aceDependenciesLoaded) {
    return of(null);
  } else {
    const aceObservables: Observable<any>[] = [];
    aceObservables.push(from(import('ace-builds/src-noconflict/ext-language_tools')));
    aceObservables.push(from(import('ace-builds/src-noconflict/ext-searchbox')));
    aceObservables.push(from(import('ace-builds/src-noconflict/mode-java')));
    aceObservables.push(from(import('ace-builds/src-noconflict/mode-css')));
    aceObservables.push(from(import('ace-builds/src-noconflict/mode-json')));
    aceObservables.push(from(import('ace-builds/src-noconflict/mode-javascript')));
    aceObservables.push(from(import('ace-builds/src-noconflict/mode-text')));
    aceObservables.push(from(import('ace-builds/src-noconflict/mode-markdown')));
    aceObservables.push(from(import('ace-builds/src-noconflict/mode-html')));
    aceObservables.push(from(import('ace-builds/src-noconflict/mode-c_cpp')));
    aceObservables.push(from(import('ace-builds/src-noconflict/mode-protobuf')));
    aceObservables.push(from(import('ace-builds/src-noconflict/snippets/java')));
    aceObservables.push(from(import('ace-builds/src-noconflict/snippets/css')));
    aceObservables.push(from(import('ace-builds/src-noconflict/snippets/json')));
    aceObservables.push(from(import('ace-builds/src-noconflict/snippets/javascript')));
    aceObservables.push(from(import('ace-builds/src-noconflict/snippets/text')));
    aceObservables.push(from(import('ace-builds/src-noconflict/snippets/markdown')));
    aceObservables.push(from(import('ace-builds/src-noconflict/snippets/html')));
    aceObservables.push(from(import('ace-builds/src-noconflict/snippets/c_cpp')));
    aceObservables.push(from(import('ace-builds/src-noconflict/snippets/protobuf')));
    aceObservables.push(from(import('ace-builds/src-noconflict/theme-textmate')));
    aceObservables.push(from(import('ace-builds/src-noconflict/theme-github')));
    return forkJoin(aceObservables).pipe(
      tap(() => {
        aceDependenciesLoaded = true;
      })
    );
  }
}

export function getAce(): Observable<any> {
  if (aceModule) {
    return of(aceModule);
  } else {
    return from(import('ace')).pipe(
      mergeMap((module) => {
        return loadAceDependencies().pipe(
         map(() => module)
        );
      }),
      tap((module) => {
        aceModule = module;
      })
    );
  }
}

export function getAceDiff(): Observable<any> {
  if (aceDiffModule) {
    return of(aceDiffModule);
  } else {
    return getAce().pipe(
      mergeMap((ace) => {
        return from(import('ace-diff'));
      }),
      tap((module) => {
        aceDiffModule = module;
      })
    );
  }
}

export class Range implements Ace.Range {

  public start: Ace.Point;
  public end: Ace.Point;

  constructor(startRow: number, startColumn: number, endRow: number, endColumn: number) {
    this.start = {
      row: startRow,
      column: startColumn
    };

    this.end = {
      row: endRow,
      column: endColumn
    };
  }

  static fromPoints(start: Ace.Point, end: Ace.Point): Ace.Range {
    return new Range(start.row, start.column, end.row, end.column);
  }

  clipRows(firstRow: number, lastRow: number): Ace.Range {
    let end: Ace.Point;
    let start: Ace.Point;
    if (this.end.row > lastRow) {
      end = {row: lastRow + 1, column: 0};
    } else if (this.end.row < firstRow) {
      end = {row: firstRow, column: 0};
    }

    if (this.start.row > lastRow) {
      start = {row: lastRow + 1, column: 0};
    } else if (this.start.row < firstRow) {
      start = {row: firstRow, column: 0};
    }
    return Range.fromPoints(start || this.start, end || this.end);
  }

  clone(): Ace.Range {
    return Range.fromPoints(this.start, this.end);
  }

  collapseRows(): Ace.Range {
    if (this.end.column === 0) {
      return new Range(this.start.row, 0, Math.max(this.start.row, this.end.row - 1), 0);
    } else {
      return new Range(this.start.row, 0, this.end.row, 0);
    }
  }

  compare(row: number, column: number): number {
    if (!this.isMultiLine()) {
      if (row === this.start.row) {
        return column < this.start.column ? -1 : (column > this.end.column ? 1 : 0);
      }
    }

    if (row < this.start.row) {
      return -1;
    }

    if (row > this.end.row) {
      return 1;
    }

    if (this.start.row === row) {
      return column >= this.start.column ? 0 : -1;
    }

    if (this.end.row === row) {
      return column <= this.end.column ? 0 : 1;
    }

    return 0;
  }

  compareEnd(row: number, column: number): number {
    if (this.end.row === row && this.end.column === column) {
      return 1;
    } else {
      return this.compare(row, column);
    }
  }

  compareInside(row: number, column: number): number {
    if (this.end.row === row && this.end.column === column) {
      return 1;
    } else if (this.start.row === row && this.start.column === column) {
      return -1;
    } else {
      return this.compare(row, column);
    }
  }

  comparePoint(p: Ace.Point): number {
    return this.compare(p.row, p.column);
  }

  compareRange(range: Ace.Range): number {
    let cmp: number;
    const end = range.end;
    const start = range.start;

    cmp = this.compare(end.row, end.column);
    if (cmp === 1) {
      cmp = this.compare(start.row, start.column);
      if (cmp === 1) {
        return 2;
      } else if (cmp === 0) {
        return 1;
      } else {
        return 0;
      }
    } else if (cmp === -1) {
      return -2;
    } else {
      cmp = this.compare(start.row, start.column);
      if (cmp === -1) {
        return -1;
      } else if (cmp === 1) {
        return 42;
      } else {
        return 0;
      }
    }
  }

  compareStart(row: number, column: number): number {
    if (this.start.row === row && this.start.column === column) {
      return -1;
    } else {
      return this.compare(row, column);
    }
  }

  contains(row: number, column: number): boolean {
    return this.compare(row, column) === 0;
  }

  containsRange(range: Ace.Range): boolean {
    return this.comparePoint(range.start) === 0 && this.comparePoint(range.end) === 0;
  }

  extend(row: number, column: number): Ace.Range {
    const cmp = this.compare(row, column);
    let end: Ace.Point;
    let start: Ace.Point;
    if (cmp === 0) {
      return this;
    } else if (cmp === -1) {
      start = {row, column};
    } else {
      end = {row, column};
    }
    return Range.fromPoints(start || this.start, end || this.end);
  }

  inside(row: number, column: number): boolean {
    if (this.compare(row, column) === 0) {
      if (this.isEnd(row, column) || this.isStart(row, column)) {
        return false;
      } else {
        return true;
      }
    }
    return false;
  }

  insideEnd(row: number, column: number): boolean {
    if (this.compare(row, column) === 0) {
      if (this.isStart(row, column)) {
        return false;
      } else {
        return true;
      }
    }
    return false;
  }

  insideStart(row: number, column: number): boolean {
    if (this.compare(row, column) === 0) {
      if (this.isEnd(row, column)) {
        return false;
      } else {
        return true;
      }
    }
    return false;
  }

  intersects(range: Ace.Range): boolean {
    const cmp = this.compareRange(range);
    return (cmp === -1 || cmp === 0 || cmp === 1);
  }

  isEmpty(): boolean {
    return (this.start.row === this.end.row && this.start.column === this.end.column);
  }

  isEnd(row: number, column: number): boolean {
    return this.end.row === row && this.end.column === column;
  }

  isEqual(range: Ace.Range): boolean {
    return this.start.row === range.start.row &&
      this.end.row === range.end.row &&
      this.start.column === range.start.column &&
      this.end.column === range.end.column;
  }

  isMultiLine(): boolean {
    return (this.start.row !== this.end.row);
  }

  isStart(row: number, column: number): boolean {
    return this.start.row === row && this.start.column === column;
  }

  moveBy(row: number, column: number): void {
    this.start.row += row;
    this.start.column += column;
    this.end.row += row;
    this.end.column += column;
  }

  setEnd(row: number, column: number): void {
    if (typeof row === 'object') {
      this.end.column = (row as Ace.Point).column;
      this.end.row = (row as Ace.Point).row;
    } else {
      this.end.row = row;
      this.end.column = column;
    }
  }

  setStart(row: number, column: number): void {
    if (typeof row === 'object') {
      this.start.column = (row as Ace.Point).column;
      this.start.row = (row as Ace.Point).row;
    } else {
      this.start.row = row;
      this.start.column = column;
    }
  }

  toScreenRange(session: Ace.EditSession): Ace.Range {
    const screenPosStart = session.documentToScreenPosition(this.start);
    const screenPosEnd = session.documentToScreenPosition(this.end);

    return new Range(
      screenPosStart.row, screenPosStart.column,
      screenPosEnd.row, screenPosEnd.column
    );
  }

}
