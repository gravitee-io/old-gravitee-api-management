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

context('API tests', () => {
  describe('Create APIs', function () {
    it('should create an API as admin user', function () {
      cy.createApi(ADMIN_USER).should((response) => {
        expect(response.body.state).equal('STOPPED');
        expect(response.body.visibility).equal('PRIVATE');
        expect(response.body.lifecycle_state).equal('CREATED');
      });
    });

    it('should fail to create an API if user lacks required permssions', function () {
      cy.createApi(LOW_PERMISSION_USER).should((response) => {
        expect(response.status).to.equal(403);
        expect(response.body.message).to.equal('You do not have sufficient rights to access this resource');
      });
    });
  });

  describe('Delete an API', function () {
    beforeEach(function () {
      cy.createApi(ADMIN_USER).should((response) => {
        expect(response.status).to.equal(201);
        cy.wrap(response.body.id).as('apiId');
      });
    });

    it('should delete an API as admin user', function () {
      cy.deleteApi(ADMIN_USER, this.apiId).its('status').should('equal', 204);
    });

    it('should fail to delete an API as low permission user', function () {
      cy.deleteApi(LOW_PERMISSION_USER, this.apiId).should((response) => {
        expect(response.status).to.be.equal(403);
        expect(response.body.message).to.be.equal('You do not have sufficient rights to access this resource');
      });
    });
  });
});
