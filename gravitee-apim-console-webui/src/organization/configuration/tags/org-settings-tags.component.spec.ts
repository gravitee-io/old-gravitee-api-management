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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { OrgSettingsTagsComponent } from './org-settings-tags.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { Tag } from '../../../entities/tag/tag';
import { fakeTag } from '../../../entities/tag/tag.fixture';
import { Group } from '../../../entities/group/group';
import { fakeGroup } from '../../../entities/group/group.fixture';
import { CurrentUserService } from '../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../entities/user';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { fakePortalSettings } from '../../../entities/portal/portalSettings.fixture';
import { Entrypoint } from '../../../entities/entrypoint/entrypoint';
import { fakeEntrypoint } from '../../../entities/entrypoint/entrypoint.fixture';

describe('OrgSettingsTagsComponent', () => {
  const currentUser = new DeprecatedUser();
  currentUser.userPermissions = [
    'organization-tag-c',
    'organization-tag-u',
    'organization-tag-d',
    'organization-entrypoint-c',
    'organization-entrypoint-u',
    'organization-entrypoint-d',
  ];

  let fixture: ComponentFixture<OrgSettingsTagsComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
      providers: [{ provide: CurrentUserService, useValue: { currentUser: currentUser } }],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsTagsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display tags table', async () => {
    fixture.detectChanges();
    expectTagsListRequest([fakeTag({ restricted_groups: ['group-a'] })]);
    expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
    expectPortalSettingsGetRequest(fakePortalSettings());
    expectEntrypointsListRequest([fakeEntrypoint()]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#tagsTable' }));
    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await await parallel(() => rows.map((row) => row.getCellTextByColumnName()));

    expect(headerCells).toEqual([
      {
        description: 'Description',
        id: 'Id',
        name: 'Name',
        restrictedGroupsName: 'Restricted groups',
        actions: '',
      },
    ]);
    expect(rowCells).toEqual([
      {
        description: 'A tag for all external stuff',
        id: expect.stringContaining('external'),
        name: 'External',
        restrictedGroupsName: 'Group A',
        actions: 'editdelete',
      },
    ]);
  });

  it('should edit default configuration', async () => {
    fixture.detectChanges();
    expectTagsListRequest([fakeTag({ restricted_groups: ['group-a'] })]);
    expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
    expectPortalSettingsGetRequest(
      fakePortalSettings({
        portal: {
          entrypoint: 'https://api.company.com',
        },
      }),
    );
    expectEntrypointsListRequest([fakeEntrypoint()]);

    const entrypointInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=entrypoint]' }));
    const saveBar = await loader.getHarness(GioSaveBarHarness);

    expect(await entrypointInput.getValue()).toEqual('https://api.company.com');

    await entrypointInput.setValue('https://my-new-api.company.com');

    expect(await saveBar.isVisible()).toBeTruthy();
    await saveBar.clickSubmit();

    expectPortalSettingsSaveRequest(
      fakePortalSettings({
        portal: {
          entrypoint: 'https://my-new-api.company.com',
        },
      }),
    );
  });

  it('should lock default configuration entrypoint input', async () => {
    fixture.detectChanges();
    expectTagsListRequest([fakeTag({ restricted_groups: ['group-a'] })]);
    expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
    expectPortalSettingsGetRequest(
      fakePortalSettings({
        portal: {
          entrypoint: 'https://api.company.com',
        },
        metadata: { readonly: ['portal.entrypoint'] },
      }),
    );
    expectEntrypointsListRequest([fakeEntrypoint()]);

    const entrypointInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=entrypoint]' }));

    expect(await entrypointInput.getValue()).toEqual('https://api.company.com');
    expect(await entrypointInput.isDisabled()).toEqual(true);
  });

  it('should display entrypoint mappings table', async () => {
    fixture.detectChanges();
    expectTagsListRequest([fakeTag({ restricted_groups: ['group-a'] })]);
    expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
    expectPortalSettingsGetRequest(fakePortalSettings());
    expectEntrypointsListRequest([fakeEntrypoint()]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#entrypointsTable' }));
    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await await parallel(() => rows.map((row) => row.getCellTextByColumnName()));

    expect(headerCells).toEqual([
      {
        entrypoint: 'Entrypoint',
        tags: 'Tags id',
        actions: '',
      },
    ]);
    expect(rowCells).toEqual([
      {
        entrypoint: expect.stringContaining('https://googl.co'),
        tags: 'internal',
        actions: 'editdelete',
      },
    ]);
  });

  it('should remove a tag', async () => {
    fixture.detectChanges();
    expectTagsListRequest([fakeTag({ id: 'tag-1', restricted_groups: ['group-a'] })]);
    expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
    expectPortalSettingsGetRequest(fakePortalSettings());
    expectEntrypointsListRequest([fakeEntrypoint({ tags: ['tag-1', 'tag-2'] })]);
    fixture.detectChanges();

    const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to delete a tag"]' }));
    await deleteButton.click();

    const confirmDialogButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Remove' }));
    await confirmDialogButton.click();

    // Update entrypoint to remove tag
    const updateEntrypointReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/`,
    });
    expect(updateEntrypointReq.request.body.tags).toEqual(['tag-2']);
    updateEntrypointReq.flush({ status: 200 });

    // Delete tag
    httpTestingController.expectOne({
      method: 'DELETE',
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags/tag-1`,
    });
    // no flush stop test here
  });

  function expectTagsListRequest(tags: Tag[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`,
      })
      .flush(tags);
  }

  function expectGroupListByOrganizationRequest(groups: Group[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/groups`,
      })
      .flush(groups);
  }

  function expectPortalSettingsGetRequest(portalSettings: PortalSettings) {
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/settings` }).flush(portalSettings);
  }

  function expectPortalSettingsSaveRequest(portalSettings: PortalSettings) {
    const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.baseURL}/settings` });
    expect(req.request.body).toStrictEqual(portalSettings);
    // no flush to stop test here
  }

  function expectEntrypointsListRequest(entrypoints: Entrypoint[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints`,
        method: 'GET',
      })
      .flush(entrypoints);
  }
});