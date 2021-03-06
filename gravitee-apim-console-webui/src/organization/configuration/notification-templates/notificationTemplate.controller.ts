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

import { NotificationTemplate } from '../../../entities/notification/notificationTemplate';

class NotificationTemplateController {
  public notifTemplates: NotificationTemplate[];
  public alertingStatus: any;
  private hasAlertingPlugin: boolean;
  private isScopeAlert: boolean;

  private portalNotifTemplate: NotificationTemplate;
  private emailNotifTemplate: NotificationTemplate;

  private templateName: string;

  constructor(private $stateParams) {
    'ngInject';
  }

  $onInit() {
    if (this.$stateParams.scope.toUpperCase() === 'TEMPLATES_TO_INCLUDE') {
      this.emailNotifTemplate = this.notifTemplates.find((notif) => notif.name === this.$stateParams.hook);
    } else {
      this.hasAlertingPlugin = this.alertingStatus?.available_plugins > 0;
      this.isScopeAlert = this.$stateParams.scope.toUpperCase() === 'TEMPLATES_FOR_ALERT';

      this.notifTemplates.forEach((notif) => {
        if (notif.type.toUpperCase() === 'PORTAL') {
          this.portalNotifTemplate = notif;
        } else {
          this.emailNotifTemplate = notif;
        }
      });
    }
    this.templateName = this.portalNotifTemplate ? this.portalNotifTemplate.name : this.emailNotifTemplate.name;
  }
}

export default NotificationTemplateController;
