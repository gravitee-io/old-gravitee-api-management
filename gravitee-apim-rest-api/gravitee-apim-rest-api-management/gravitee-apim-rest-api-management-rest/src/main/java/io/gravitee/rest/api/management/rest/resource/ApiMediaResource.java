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
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import io.swagger.annotations.*;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * @author Guillaume Gillon
 */
@Api(tags = { "API Media" })
public class ApiMediaResource extends AbstractResource {

    @Inject
    private MediaService mediaService;

    @PathParam("api")
    @ApiParam(name = "api", required = true, value = "The ID of the API")
    private String api;

    @POST
    @ApiOperation(
        value = "Create a media for an API",
        notes = "User must have the API_DOCUMENTATION[CREATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Media successfully created", response = PageEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = RolePermissionAction.CREATE) })
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public Response uploadApiMediaImage(
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail,
        @FormDataParam("file") final FormDataBodyPart body
    ) throws IOException {
        final String mediaId;

        if (fileDetail.getSize() > this.mediaService.getMediaMaxSize()) {
            throw new UploadUnauthorized("Max size achieved " + fileDetail.getSize());
        } else {
            MediaEntity mediaEntity = new MediaEntity();
            mediaEntity.setSize(fileDetail.getSize());
            mediaEntity.setType(body.getMediaType().getType());
            mediaEntity.setSubType(body.getMediaType().getSubtype());
            mediaEntity.setData(IOUtils.toByteArray(uploadedInputStream));
            mediaEntity.setFileName(fileDetail.getFileName());

            try {
                ImageUtils.verify(body.getMediaType().getType(), body.getMediaType().getSubtype(), mediaEntity.getData());
            } catch (InvalidImageException e) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid image format").build();
            }

            mediaId = mediaService.saveApiMedia(api, mediaEntity);
        }

        return Response.status(200).entity(mediaId).build();
    }

    @GET
    @Path("/{hash}")
    @ApiOperation(value = "Retrieve a media for an API")
    public Response getApiMediaImage(@Context Request request, @PathParam("hash") String hash) {
        MediaEntity mediaEntity = mediaService.findByHashAndApiId(hash, api);

        if (mediaEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        EntityTag etag = new EntityTag(hash);
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        return Response.ok(mediaEntity.getData()).type(mediaEntity.getMimeType()).cacheControl(cc).tag(etag).build();
    }
}
