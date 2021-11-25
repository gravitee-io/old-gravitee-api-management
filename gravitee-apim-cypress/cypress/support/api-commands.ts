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
import { ApiFakers } from '@fakers/apis';
import { Api, ApiErrorCodes, ApiLifecycleState, PortalApi } from '@model/apis';
import { User, BasicAuthentication } from '@model/users';

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Creates an API by sending a POST request to the API server
       *
       * User must have API_PUBLISHER or ADMIN role to create an API.
       *
       * @param auth User authentication object
       * @example cy.createApi({username: "myApiUser", password: "password123"})
       */
      createApi(auth: BasicAuthentication): Chainable;

      /**
       * Publishes an API by sending a PUT request to the API server
       *
       * User must have the MANAGE_API permission
       *
       * @param auth User authentication object
       * @param apiId ID of the API
       */
      publishApi(auth: BasicAuthentication, apiId: string): Chainable;

      /**
       * Deletes an API by sending a PUT request to the API server
       *
       * User must have the DELETE permission
       *
       * @param auth User authentication object
       * @param apiId ID of the API
       */
      deleteApi(auth: BasicAuthentication, apiId: string): Chainable;
    }
  }
}

Cypress.Commands.add('createApi', (auth) => {
  const fakeApi: Api = ApiFakers.api();

  return cy.request({
    method: 'POST',
    url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/apis`,
    body: fakeApi,
    auth,
    failOnStatusCode: false,
  });
});

Cypress.Commands.add('deleteApi', (auth, apiId) => {
  return cy.request({
    method: 'DELETE',
    url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/apis/${apiId}`,
    auth,
    failOnStatusCode: false,
  });
});
