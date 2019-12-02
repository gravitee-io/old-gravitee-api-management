/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Injectable } from '@angular/core';
import { Route, Router, Routes } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ApiService, PortalService } from '@gravitee/ng-portal-webclient';
import { FeatureGuardService } from './feature-guard.service';

export enum RouteType {
  main,
  login,
  user,
  catalog
}

@Injectable({
  providedIn: 'root'
})
export class RouteService {
  private flattenedRoutes: any[];

  constructor(private router: Router,
              private translateService: TranslateService,
              private apiService: ApiService,
              private portalService: PortalService,
              private featureGuardService: FeatureGuardService) {
    this.flattenedRoutes = this.getFlattenedRoutes(this.router.config);
  }

  private getFlattenedRoutes(routes: Routes) {
    const tmpRoutes = JSON.parse(JSON.stringify(routes));
    const flattenedRoutes = tmpRoutes.map((route) => {
      if (route.children) {
        const children = route.children.map((child) => {
          child.path = route.path + '/' + child.path;
          return child;
        });
        delete route.children;
        return this.getFlattenedRoutes(children.concat(route));
      }
      if (route.redirectTo && route.data && route.data.fallbackRedirectTo) {
        const targetRoute = tmpRoutes.find(r => r.path.endsWith(route.redirectTo));
        if (!this.featureGuardService.canActivate(targetRoute)) {
          const routeConfig = this.getRouteConfig(this.router.config, route);
          if (routeConfig) {
            routeConfig.redirectTo = route.data.fallbackRedirectTo;
          }
        }
      }
      return route;
    });
    return [].concat(...flattenedRoutes);
  }

  getRouteConfig(routes: Routes, targetRoute: Route): Route {
    let routeConfig = null;
    if (routes) {
      routes.forEach(route => {
        if (route.children) {
          routeConfig = this.getRouteConfig(route.children, targetRoute);
        } else {
          if (route.redirectTo === targetRoute.redirectTo) {
            routeConfig = route;
          }
        }
      });
    }
    return routeConfig;
  }

  getRoutes(type: RouteType) {
    const c = this.flattenedRoutes
      .filter(route => route.data && (route.data.type === type) && this.featureGuardService.canActivate(route))
      .map(({ path, data: { title, icon, separator, categoryApiQuery } }) => {
        let help = null;
        if (type === RouteType.catalog) {
          if (categoryApiQuery) {
            help = this.apiService
              .getApis({ cat: categoryApiQuery, size: 0 })
              .toPromise()
              .then(({ metadata: { data: { total } } }) => total);
          } else {
            help = this.portalService
              .getViews({ size: 0 })
              .toPromise()
              .then(({ metadata: { data: { total } } }) => total);
          }
        }

        return {
          path,
          icon,
          active: this.router.isActive(`/${ path }`, false),
          title: this.translateService.get(title).toPromise(),
          separator,
          help
        };
      });
    return c;
  }

}
