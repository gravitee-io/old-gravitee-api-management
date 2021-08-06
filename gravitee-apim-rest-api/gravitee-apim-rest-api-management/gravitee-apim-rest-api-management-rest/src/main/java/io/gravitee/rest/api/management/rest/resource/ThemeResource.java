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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.theme.ThemeEntity;
import io.gravitee.rest.api.model.theme.UpdateThemeEntity;
import io.gravitee.rest.api.service.ThemeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Themes" })
public class ThemeResource extends AbstractResource {

    @Inject
    private ThemeService themeService;

    @PathParam("themeId")
    @ApiParam(name = "themeId", required = true)
    private String themeId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = RolePermissionAction.READ) })
    public ThemeEntity getTheme() {
        return themeService.findEnabled();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = RolePermissionAction.UPDATE) })
    public ThemeEntity updateTheme(@Valid @NotNull final UpdateThemeEntity theme) {
        theme.setId(themeId);
        return themeService.update(theme);
    }

    @Path("/reset")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = RolePermissionAction.UPDATE) })
    public ThemeEntity resetTheme() {
        return themeService.resetToDefaultTheme(themeId);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = RolePermissionAction.DELETE) })
    public void deleteTheme() {
        themeService.delete(themeId);
    }

    @GET
    @Path("/logo")
    public Response getThemeLogo(@Context Request request) {
        return this.buildPictureResponse(themeService.getLogo(themeId), request);
    }

    @GET
    @Path("/optionalLogo")
    public Response getLogoLight(@Context Request request) {
        return this.buildPictureResponse(themeService.getOptionalLogo(themeId), request);
    }

    @GET
    @Path("/backgroundImage")
    public Response getThemeBackground(@Context Request request) {
        return this.buildPictureResponse(themeService.getBackgroundImage(themeId), request);
    }

    private Response buildPictureResponse(PictureEntity picture, @Context Request request) {
        if (picture == null) {
            return Response.ok().build();
        }

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        InlinePictureEntity image = (InlinePictureEntity) picture;

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response.ok().entity(baos).cacheControl(cc).tag(etag).type(image.getType()).build();
    }
}
