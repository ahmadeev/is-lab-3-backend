package controllers;

import auth.AuthService;
import auth.User;
import dto.utils.ImportHistoryUnitDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Named(value = "dragonImportController")
@ApplicationScoped
@Path("/user/import")
@MultipartConfig
public class DragonImportController {
    private static final int MIN_FILES_COUNT = 1;

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

        System.out.println("name: " + securityContext.getUserPrincipal().getName() + ", id: " + userId);

        try {
            // получаем файлы из multipart запроса
            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
            List<InputPart> inputParts = uploadForm.get("files");

            if (inputParts.isEmpty()) {
                inputParts = uploadForm.get("file");
            }

            // проверка на минимальное количество файлов
            if (inputParts.size() < MIN_FILES_COUNT) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntity(ResponseStatus.ERROR, "Minimum " + MIN_FILES_COUNT + " files required", null))
                        .build();
            }

            // обрабатываем каждый файл
            List<InputStream> inputStreams = new ArrayList<>();
            for (InputPart part : inputParts) {
                inputStreams.add(part.getBody(InputStream.class, null));
            }

            dragonImportService.importDragonsFromCsv(inputStreams, user);

            return Response.ok()
                    .entity(new ResponseEntity(ResponseStatus.SUCCESS, "Dragons imported successfully", null))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntity(ResponseStatus.ERROR, "Error during import: " + e.getMessage(), null))
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

        List<ImportHistoryUnitDTO> importHistory = dragonImportService.getImportHistory(user, page, pageSize, filterValue, filterCol, sortBy, sortDir);

        return Response.ok().entity(
                new ResponseEntity(ResponseStatus.SUCCESS,"Successfully got import history", importHistory)
        ).build();
    }
}
