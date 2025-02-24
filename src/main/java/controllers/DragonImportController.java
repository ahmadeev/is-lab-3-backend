package controllers;

import auth.AuthService;
import auth.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.ws.rs.*;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import objects.utils.ImportHistoryUnit;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import responses.ResponseEntity;
import responses.ResponseStatus;
import services.DragonImportService;

import java.io.InputStream;
import java.util.List;
import java.util.Map;


@Named(value = "dragonImportController")
@ApplicationScoped
@Path("/user/import")
@MultipartConfig
public class DragonImportController {
    @Inject
    private DragonImportService dragonImportService;

    @Inject
    private AuthService authService;

    @POST
    @Path("/csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadDragons(MultipartFormDataInput input, @Context SecurityContext securityContext) {
        // TODO: переделать с JWT
        long userId = authService.getUserByName(securityContext.getUserPrincipal().getName()).getId();

        User user = new User();
        user.setId(userId);

        try {
            // получаем файлы из multipart запроса
            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
            List<InputPart> inputParts = uploadForm.get("files");

            if (inputParts.isEmpty()) {
                inputParts = uploadForm.get("file");
            }

            // обрабатываем каждый файл
            for (InputPart part : inputParts) {
                InputStream inputStream = part.getBody(InputStream.class, null);
                // TODO: складывать стримы в один объект и передавать ему одному методу
                dragonImportService.importDragonsFromCsv(inputStream, user);
            }

            return Response.ok().entity("Dragons imported successfully").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error during import: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/history")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportHistory(
            @Context SecurityContext securityContext,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize,
            @QueryParam("filterValue") @DefaultValue("") String filterValue,
            @QueryParam("filterCol") @DefaultValue("") String filterCol,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDir") @DefaultValue("ASC") String sortDir
    ) {
        // id, status, user, amount

        // TODO: переделать с JWT
        User user = authService.getUserByName(securityContext.getUserPrincipal().getName());

        List<ImportHistoryUnit> importHistory = dragonImportService.getImportHistory(user, page, pageSize, filterValue, filterCol, sortBy, sortDir);

        return Response.ok().entity(
                new ResponseEntity(ResponseStatus.SUCCESS,"Successfully got import history", importHistory)
        ).build();
    }
}
