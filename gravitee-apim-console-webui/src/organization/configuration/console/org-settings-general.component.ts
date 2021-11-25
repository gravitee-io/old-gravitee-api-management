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

import { Observable, Subject } from 'rxjs';
import { map, startWith, takeUntil, tap } from 'rxjs/operators';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { MatChipInputEvent, MatChipList } from '@angular/material/chips';
import { cloneDeep, get, isEmpty, merge } from 'lodash';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';

import { ConsoleSettingsService } from '../../../services-ngx/console-settings.service';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { CorsUtil } from '../../../shared/utils';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '../../../shared/components/gio-confirm-dialog/gio-confirm-dialog.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'org-settings-general',
  template: require('./org-settings-general.component.html'),
  styles: [require('./org-settings-general.component.scss')],
})
export class OrgSettingsGeneralComponent implements OnInit, OnDestroy {
  isLoading = true;

  formSettings: FormGroup;

  settings: ConsoleSettings;

  providedConfigurationMessage = 'Configuration provided by the system';

  httpMethods = CorsUtil.httpMethods;

  allowHeadersInputFormControl = new FormControl();
  allowHeadersFilteredOptions$: Observable<string[]>;

  exposedHeadersInputFormControl = new FormControl();
  exposedHeadersFilteredOptions$: Observable<string[]>;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  private allowAllOriginsConfirmDialog?: MatDialogRef<GioConfirmDialogComponent, boolean>;
  public formInitialValues: unknown;

  constructor(
    private readonly fb: FormBuilder,
    private readonly consoleSettingsService: ConsoleSettingsService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.consoleSettingsService
      .get()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((settings) => {
        this.isLoading = false;
        this.settings = settings;

        // As in Angular the forms are not typed :
        // The object structure of this.formSettings matches that of ConsoleSettings to be able to send the form result directly
        this.formSettings = this.fb.group({
          management: this.fb.group({
            title: [toFormState(this.settings, 'management.title')],
            url: [toFormState(this.settings, 'management.url')],
            support: this.fb.group({ enabled: [toFormState(this.settings, 'management.support.enabled')] }),
            pathBasedApiCreation: this.fb.group({ enabled: [toFormState(this.settings, 'management.pathBasedApiCreation.enabled')] }),
            userCreation: this.fb.group({ enabled: [toFormState(this.settings, 'management.userCreation.enabled')] }),
            automaticValidation: this.fb.group({ enabled: [toFormState(this.settings, 'management.automaticValidation.enabled')] }),
          }),
          theme: this.fb.group({
            name: [toFormState(this.settings, 'theme.name')],
            logo: [toFormState(this.settings, 'theme.logo')],
            loader: [toFormState(this.settings, 'theme.loader')],
          }),
          scheduler: this.fb.group({
            tasks: [toFormState(this.settings, 'scheduler.tasks')],
            notifications: [toFormState(this.settings, 'scheduler.notifications')],
          }),
          alert: this.fb.group({ enabled: [toFormState(this.settings, 'alert.enabled')] }),
          cors: this.fb.group({
            allowOrigin: [toFormState(this.settings, 'cors.allowOrigin', []), [CorsUtil.allowOriginValidator()]],
            allowMethods: [toFormState(this.settings, 'cors.allowMethods', [])],
            allowHeaders: [toFormState(this.settings, 'cors.allowHeaders', [])],
            exposedHeaders: [toFormState(this.settings, 'cors.exposedHeaders', [])],
            maxAge: [toFormState(this.settings, 'cors.maxAge')],
          }),

          email: this.fb.group({
            enabled: [toFormState(this.settings, 'email.enabled')],
            host: [toFormState(this.settings, 'email.host')],
            port: [toFormState(this.settings, 'email.port')],
            username: [toFormState(this.settings, 'email.username')],
            password: [toFormState(this.settings, 'email.password')],
            protocol: [toFormState(this.settings, 'email.protocol')],
            subject: [toFormState(this.settings, 'email.subject')],
            from: [toFormState(this.settings, 'email.from')],
            properties: this.fb.group({
              auth: [toFormState(this.settings, 'email.properties.auth')],
              startTlsEnable: [toFormState(this.settings, 'email.properties.startTlsEnable')],
              sslTrust: [toFormState(this.settings, 'email.properties.sslTrust')],
            }),
          }),
        });

        // Disable `management.automaticValidation.enabled` if `management.userCreation.enabled` is not checked
        this.formSettings
          .get('management.userCreation.enabled')
          .valueChanges.pipe(takeUntil(this.unsubscribe$))
          .subscribe((checked) => {
            if (checked) {
              this.formSettings.get('management.automaticValidation.enabled').enable();
              return;
            }
            this.formSettings.get('management.automaticValidation.enabled').disable();
          });

        // Disable all `email` group if `email.enabled` is not checked without impacting the already readonly properties
        const controlKeys = [
          'host',
          'port',
          'username',
          'password',
          'protocol',
          'subject',
          'from',
          'properties.auth',
          'properties.startTlsEnable',
          'properties.sslTrust',
        ];
        this.formSettings.get('email.enabled').valueChanges.subscribe((checked) => {
          controlKeys
            .filter((k) => !isReadonlySetting(this.settings, `email.${k}`))
            .forEach((k) => {
              return checked ? this.formSettings.get(`email.${k}`).enable() : this.formSettings.get(`email.${k}`).disable();
            });
        });

        this.formInitialValues = this.formSettings.getRawValue();
      });

    this.allowHeadersFilteredOptions$ = this.allowHeadersInputFormControl.valueChanges.pipe(
      startWith(''),
      map((value: string | null) => {
        return CorsUtil.defaultHttpHeaders.filter((defaultHeader) => defaultHeader.toLowerCase().includes((value ?? '').toLowerCase()));
      }),
    );

    this.exposedHeadersFilteredOptions$ = this.exposedHeadersInputFormControl.valueChanges.pipe(
      startWith(''),
      map((value: string | null) => {
        return CorsUtil.defaultHttpHeaders.filter((defaultHeader) => defaultHeader.toLowerCase().includes((value ?? '').toLowerCase()));
      }),
    );
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  addChipToFormControl(event: MatChipInputEvent, formControlPath: string, matChipList: MatChipList): void {
    const input = event.chipInput.inputElement;
    const chipToAdd = (event.value ?? '').trim();
    const formControl = this.formSettings.get(formControlPath);

    // Add new Chip in form control
    if (!isEmpty(chipToAdd)) {
      // Delete Chip if already existing
      const formControlValue = [...formControl.value].filter((v) => v !== chipToAdd);

      formControl.setValue([...formControlValue, chipToAdd]);
    }

    // Reset the input value
    if (input) {
      input.value = '';
    }

    // Check error state
    matChipList.errorState = formControl.errors !== null;
  }

  confirmAllowAllOrigins(): (tag: string, validationCb: (shouldAddTag: boolean) => void) => void {
    return (tag, validationCb) => {
      // Confirm allow all origins
      if ('*' === tag && !this.allowAllOriginsConfirmDialog) {
        this.allowAllOriginsConfirmDialog = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData>(
          GioConfirmDialogComponent,
          {
            width: '450px',
            data: {
              title: 'Are you sure?',
              content: 'Do you want to remove all cross-origin restrictions?',
              confirmButton: 'Yes, I want to allow all origins.',
            },
            role: 'alertdialog',
            id: 'allowAllOriginsConfirmDialog',
          },
        );

        this.allowAllOriginsConfirmDialog
          .afterClosed()
          .pipe(takeUntil(this.unsubscribe$))
          .subscribe((shouldAddTag) => {
            this.allowAllOriginsConfirmDialog = null;
            validationCb(shouldAddTag);
          });
      } else {
        validationCb(true);
      }
    };
  }

  removeChipToFormControl(value: string, formControlPath: string, matChipList: MatChipList) {
    const formControl = this.formSettings.get(formControlPath);
    // Remove Chip in form control
    formControl.setValue([...formControl.value].filter((v) => v !== value));

    // Check error state
    matChipList.errorState = formControl.errors !== null;
  }

  addSelectedToFormControl(event: MatAutocompleteSelectedEvent, formControlPath: string): void {
    const optionToAdd = event.option.viewValue;

    // Add selected option in form control
    if (!isEmpty(optionToAdd)) {
      const formControl = this.formSettings.get(formControlPath);
      // Delete Chip if already existing
      const formControlValue = [...formControl.value].filter((v) => v !== optionToAdd);

      formControl.setValue([...formControlValue, optionToAdd]);
    }
    this.allowHeadersInputFormControl.setValue(null);
    this.allowHeadersInputFormControl.updateValueAndValidity();
  }

  onSubmit() {
    if (this.formSettings.invalid) {
      return;
    }

    const formSettingsValue = this.formSettings.getRawValue();

    const settingsToSave = merge(cloneDeep(this.settings), formSettingsValue);

    this.consoleSettingsService
      .save(settingsToSave)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
      )
      .subscribe(() => this.ngOnInit());
  }

  isReadonlySetting(property: string): boolean {
    return isReadonlySetting(this.settings, property);
  }
}

const isReadonlySetting = (consoleSettings: ConsoleSettings, property: string): boolean => {
  return ConsoleSettingsService.isReadonly(consoleSettings, property);
};

const toFormState = (consoleSettings: ConsoleSettings, path: string, defaultValue: unknown = undefined) => {
  return { value: get(consoleSettings, path, defaultValue), disabled: isReadonlySetting(consoleSettings, path) };
};
