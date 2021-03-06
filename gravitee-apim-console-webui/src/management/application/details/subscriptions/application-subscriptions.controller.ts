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

import { StateService } from '@uirouter/core';
import * as angular from 'angular';
import * as _ from 'lodash';

import { PagedResult } from '../../../../entities/pagedResult';
import { ApiService } from '../../../../services/api.service';
import ApplicationService from '../../../../services/application.service';
import NotificationService from '../../../../services/notification.service';

const defaultStatus = ['ACCEPTED', 'PENDING', 'PAUSED'];

export class SubscriptionQuery {
  status?: string[] = defaultStatus;
  apis?: string[];
  applications?: string[];
  plans?: string[];
  api_key: string;
  page?: number = 1;
  size?: number = 10;
}

class ApplicationSubscriptionsController {
  private subscriptions: PagedResult;
  private subscribers: any[];
  private application: any;

  private query: SubscriptionQuery = new SubscriptionQuery();

  private status = {
    ACCEPTED: 'Accepted',
    CLOSED: 'Closed',
    PAUSED: 'Paused',
    PENDING: 'Pending',
    REJECTED: 'Rejected',
  };

  private subscriptionsFiltersForm: any;

  constructor(
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private ApiService: ApiService,
    private $state: StateService,
  ) {
    'ngInject';

    this.onPaginate = this.onPaginate.bind(this);
    if (this.$state.params.status) {
      if (Array.isArray(this.$state.params.status)) {
        this.query.status = this.$state.params.status;
      } else {
        this.query.status = this.$state.params.status.split(',');
      }
    }
    if (this.$state.params.api) {
      if (Array.isArray(this.$state.params.api)) {
        this.query.apis = this.$state.params.api;
      } else {
        this.query.apis = this.$state.params.api.split(',');
      }
    }
    if (this.$state.params.api_key) {
      this.query.api_key = this.$state.params.api_key;
    }
  }

  onPaginate(page) {
    this.query.page = page;
    this.doSearch();
  }

  clearFilters() {
    this.subscriptionsFiltersForm.$setPristine();
    this.query = new SubscriptionQuery();
    this.doSearch();
  }

  search() {
    this.query.page = 1;
    this.query.size = 10;
    this.doSearch();
  }

  buildQuery() {
    let query = '?page=' + this.query.page + '&size=' + this.query.size + '&';
    const parameters: any = {};

    if (this.query.status !== undefined) {
      parameters.status = this.query.status.join(',');
    }

    if (this.query.apis !== undefined) {
      parameters.api = this.query.apis.join(',');
    }

    if (this.query.api_key !== undefined) {
      parameters.api_key = this.query.api_key;
    }

    _.mapKeys(parameters, (value, key) => {
      return (query += key + '=' + value + '&');
    });

    return query;
  }

  doSearch() {
    const query = this.buildQuery();
    this.$state.transitionTo(
      this.$state.current,
      _.merge(this.$state.params, {
        status: this.query.status ? this.query.status.join(',') : '',
        api: this.query.apis ? this.query.apis.join(',') : '',
        page: this.query.page,
        size: this.query.size,
        api_key: this.query.api_key,
      }),
      { notify: false },
    );

    this.ApplicationService.listSubscriptions(this.application.id, query).then((response) => {
      this.subscriptions = response.data as PagedResult;
    });
  }

  toggleSubscription(scope, subscription) {
    scope.toggle();
    if (!subscription.apiKeys) {
      this.listApiKeys(subscription);
    }
  }

  listApiKeys(subscription) {
    this.ApplicationService.listApiKeys(this.application.id, subscription.id).then((response) => {
      subscription.apiKeys = response.data;
    });
  }

  hasKeysDefined() {
    return this.subscriptions !== null && Object.keys(this.subscriptions).length > 0;
  }

  generateAPIKey(applicationId, subscription) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to renew your API Key?',
          msg: 'Your previous API Key will be no longer valid in 1 hour !',
          confirmButton: 'Renew',
        },
      })
      .then((response) => {
        if (response) {
          this.ApplicationService.renewApiKey(applicationId, subscription.id).then(() => {
            this.NotificationService.show('A new API Key has been generated');
            this.listApiKeys(subscription);
          });
        }
      });
  }

  revoke(subscription, apiKey) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: "Are you sure you want to revoke API Key '" + apiKey.key + "'?",
          confirmButton: 'Revoke',
        },
      })
      .then((response) => {
        if (response) {
          this.ApplicationService.revokeApiKey(this.application.id, subscription.id, apiKey.id).then(() => {
            this.NotificationService.show('API Key ' + apiKey.key + ' has been revoked !');
            this.listApiKeys(subscription);
          });
        }
      });
  }

  onClipboardSuccess(e) {
    this.NotificationService.show('API Key has been copied to clipboard');
    e.clearSelection();
  }

  /*
  showSubscribeApiModal(ev) {
    this.$mdDialog.show({
      controller: 'DialogSubscribeApiController',
      templateUrl: 'application/dialog/subscribeApi.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      locals: {
        application: this.application,
        subscriptions: this.subscriptions
      }
    }).then(application =>{
      if (application) {
        // TODO : check it ! There was no ApiService...
        this.ApiService.getSubscriptions(application.id);
      }
    });
  }
  */

  showExpirationModal(apiId, subscriptionId, apiKey) {
    this.$mdDialog
      .show({
        controller: 'DialogApiKeyExpirationController',
        controllerAs: 'dialogApiKeyExpirationController',
        template: require('../../../api/portal/subscriptions/dialog/apikey.expiration.dialog.html'),
        clickOutsideToClose: true,
      })
      .then((expirationDate) => {
        apiKey.expire_at = expirationDate;

        this.ApiService.updateApiKey(apiId, subscriptionId, apiKey).then(() => {
          this.NotificationService.show('An expiration date has been settled for API Key');
        });
      });
  }

  hasFilter() {
    return (
      _.difference(defaultStatus, this.query.status).length > 0 ||
      _.difference(this.query.status, defaultStatus).length > 0 ||
      (this.query.applications && this.query.applications.length) ||
      (this.query.plans && this.query.plans.length) ||
      this.query.api_key
    );
  }
}

export default ApplicationSubscriptionsController;
