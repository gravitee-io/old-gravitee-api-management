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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';

import { OrgSettingAddTenantComponent, OrgSettingAddTenantDialogData } from './org-settings-add-tenant.component';

import { TenantService } from '../../../services-ngx/tenant.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Tenant } from '../../../entities/tenant/tenant';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'org-settings-tenants',
  template: require('./org-settings-tenants.component.html'),
  styles: [require('./org-settings-tenants.component.scss')],
})
export class OrgSettingsTenantsComponent implements OnInit, OnDestroy {
  displayedColumns: string[] = ['id', 'name', 'description', 'actions'];
  tenantsDataSource: MatTableDataSource<Tenant> = new MatTableDataSource([]);

  private unsubscribe$ = new Subject<boolean>();
  private tenants: Tenant[] = [];

  constructor(
    private readonly tenantService: TenantService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.tenantService.list().subscribe((tenants) => {
      this.tenants = tenants;
      this.tenantsDataSource.data = tenants;
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onAddTenantClicked(): void {
    this.matDialog
      .open<OrgSettingAddTenantComponent, OrgSettingAddTenantDialogData, Tenant>(OrgSettingAddTenantComponent, {
        width: '450px',
        data: {},
        role: 'dialog',
        id: 'addTenantDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((result) => !!result),
        switchMap((newTenant) => this.tenantService.create([newTenant])),
        tap(() => {
          this.snackBarService.success('Tenant successfully created!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe(() => this.ngOnInit());
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function,@typescript-eslint/no-unused-vars
  onDeleteTenantClicked(element: any) {}

  onEditTenantClicked(tenant: Tenant) {
    this.matDialog
      .open<OrgSettingAddTenantComponent, OrgSettingAddTenantDialogData, Tenant>(OrgSettingAddTenantComponent, {
        width: '450px',
        data: {
          tenant,
        },
        role: 'dialog',
        id: 'editTenantDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((result) => !!result),
        switchMap((updatedTenant) => this.tenantService.update([updatedTenant])),
        tap(() => {
          this.snackBarService.success('Tenant successfully updated!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe(() => this.ngOnInit());
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.tenantsDataSource.data = gioTableFilterCollection(this.tenants, filters);
  }
}
