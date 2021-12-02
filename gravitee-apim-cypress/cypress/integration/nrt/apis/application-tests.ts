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
import { ADMIN_USER, API_PUBLISHER_USER, LOW_PERMISSION_USER } from '@fakers/users/users';

context('Application tests', () => {
  describe('Create an Application', function () {
    after(function () {
      cy.deleteApplication(ADMIN_USER, this.applicationId).its('status').should('equal', 204);
    });

    it('should create an application as admin user', function () {
      cy.createApplication(ADMIN_USER).should((response) => {
        expect(response.status).to.equal(201);
        expect(response.body.status).equal('ACTIVE');
        cy.wrap(response.body.id).as('applicationId');
      });
    });

    it('should not create an application for unauthorized user', function () {
      cy.createApplication({ username: 'unknown_user', password: 'fake p@ssword' }).its('status').should('equal', 401);
    });
  });

  describe('Delete an Application', function () {
    before(function () {
      cy.createApplication(ADMIN_USER)
        .as('applicationId')
        .should(function (response) {
          expect(response.status).to.equal(201);
          cy.wrap(response.body.id).as('applicationId');
        });
    });

    it('should delete an application as admin user', function () {
      cy.deleteApplication(ADMIN_USER, this.applicationId).its('status').should('equal', 204);
    });
  });
});
