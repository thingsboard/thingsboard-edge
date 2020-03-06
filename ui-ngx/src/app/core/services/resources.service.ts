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

import {
  Compiler,
  ComponentFactory,
  Inject,
  Injectable,
  Injector,
  ModuleWithComponentFactories,
  Type
} from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { forkJoin, Observable, ReplaySubject, throwError } from 'rxjs';

declare const SystemJS;

@Injectable({
  providedIn: 'root'
})
export class ResourcesService {

  private loadedResources: { [url: string]: ReplaySubject<any> } = {};
  private loadedModules: { [url: string]: ReplaySubject<ComponentFactory<any>[]> } = {};

  private anchor = this.document.getElementsByTagName('head')[0] || this.document.getElementsByTagName('body')[0];

  constructor(@Inject(DOCUMENT) private readonly document: any,
              private compiler: Compiler,
              private injector: Injector) {}

  public loadResource(url: string): Observable<any> {
    if (this.loadedResources[url]) {
      return this.loadedResources[url].asObservable();
    }

    let fileType;
    const match = /[.](css|less|html|htm|js)?((\?|#).*)?$/.exec(url);
    if (match !== null) {
      fileType = match[1];
    }
    if (!fileType) {
      return throwError(new Error(`Unable to detect file type from url: ${url}`));
    } else if (fileType !== 'css' && fileType !== 'js') {
      return throwError(new Error(`Unsupported file type: ${fileType}`));
    }
    return this.loadResourceByType(fileType, url);
  }

  public loadModule(url: string, modulesMap: {[key: string]: any}): Observable<ComponentFactory<any>[]> {
    if (this.loadedModules[url]) {
      return this.loadedModules[url].asObservable();
    }
    const subject = new ReplaySubject<ComponentFactory<any>[]>();
    this.loadedModules[url] = subject;
    if (modulesMap) {
      for (const moduleId of Object.keys(modulesMap)) {
        SystemJS.set(moduleId, modulesMap[moduleId]);
      }
    }
    SystemJS.import(url).then(
      (module) => {
        const modules = this.extractNgModules(module);
        if (modules.length) {
          const tasks: Promise<ModuleWithComponentFactories<any>>[] = [];
          for (const m of modules) {
            tasks.push(this.compiler.compileModuleAndAllComponentsAsync(m));
          }
          forkJoin(tasks).subscribe((compiled) => {
            try {
              const componentFactories: ComponentFactory<any>[] = [];
              for (const c of compiled) {
                c.ngModuleFactory.create(this.injector);
                componentFactories.push(...c.componentFactories);
              }
              this.loadedModules[url].next(componentFactories);
              this.loadedModules[url].complete();
            } catch (e) {
              this.loadedModules[url].error(new Error(`Unable to init module from url: ${url}`));
              delete this.loadedModules[url];
            }
          },
          (e) => {
              this.loadedModules[url].error(new Error(`Unable to compile module from url: ${url}`));
              delete this.loadedModules[url];
          });
        } else {
          this.loadedModules[url].error(new Error(`Module '${url}' doesn't have default export!`));
          delete this.loadedModules[url];
        }
      },
      (e) => {
        this.loadedModules[url].error(new Error(`Unable to load module from url: ${url}`));
        delete this.loadedModules[url];
      }
    );
    return subject.asObservable();
  }

  private extractNgModules(module: any, modules: Type<any>[] = [] ): Type<any>[] {
    if (module && 'ɵmod' in module) {
      modules.push(module);
    } else {
      for (const k of Object.keys(module)) {
        this.extractNgModules(module[k], modules);
      }
    }
    return modules;
  }

  private loadResourceByType(type: 'css' | 'js', url: string): Observable<any> {
    const subject = new ReplaySubject();
    this.loadedResources[url] = subject;
    let el;
    let loaded = false;
    switch (type) {
      case 'js':
        el = this.document.createElement('script');
        el.type = 'text/javascript';
        el.async = true;
        el.src = url;
        break;
      case 'css':
        el = this.document.createElement('link');
        el.type = 'text/css';
        el.rel = 'stylesheet';
        el.href = url;
        break;
    }
    el.onload = el.onreadystatechange = (e) => {
      if (el.readyState && !/^c|loade/.test(el.readyState) || loaded) { return; }
      el.onload = el.onreadystatechange = null;
      loaded = true;
      this.loadedResources[url].next();
      this.loadedResources[url].complete();
    };
    el.onerror = () => {
      this.loadedResources[url].error(new Error(`Unable to load ${url}`));
      delete this.loadedResources[url];
    };
    this.anchor.appendChild(el);
    return subject.asObservable();
  }
}
