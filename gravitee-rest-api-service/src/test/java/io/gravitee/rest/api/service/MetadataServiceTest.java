/**
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
package io.gravitee.rest.api.service;

import static io.gravitee.repository.management.model.MetadataReferenceType.API;

import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.MetadataServiceImpl;
import org.junit.Test;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MetadataServiceTest {

    private final MetadataService metadataService = new MetadataServiceImpl();

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badEmailFormat() {
        metadataService.checkMetadataFormat(MetadataFormat.MAIL, "test");
    }

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badURLFormat() {
        metadataService.checkMetadataFormat(MetadataFormat.URL, "test");
    }

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badEmailFormat_EL() {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("test");
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setPrimaryOwner(primaryOwnerEntity);
        ((MetadataServiceImpl) metadataService).setFreemarkerConfiguration(
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_22)
            );
        metadataService.checkMetadataFormat(MetadataFormat.MAIL, "${api.primaryOwner.email}", API, apiEntity);
    }

    @Test
    public void checkMetadataFormat_nominalCase_EL() {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("gs@support.com");
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setPrimaryOwner(primaryOwnerEntity);
        ((MetadataServiceImpl) metadataService).setFreemarkerConfiguration(
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_22)
            );
        metadataService.checkMetadataFormat(MetadataFormat.MAIL, "${api.primaryOwner.email}", API, apiEntity);
    }

    @Test
    public void checkMetadataFormat_userWithoutEmail() {
        UserEntity userEntity = new UserEntity();
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setPrimaryOwner(primaryOwnerEntity);
        ((MetadataServiceImpl) metadataService).setFreemarkerConfiguration(
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_22)
            );
        metadataService.checkMetadataFormat(MetadataFormat.MAIL, "${(api.primaryOwner.email)!''}", API, apiEntity);
    }
}
