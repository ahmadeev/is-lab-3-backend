package controllers;

import auth.AuthService;
import auth.User;
import dto.utils.ImportHistoryUnitDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.ws.rs.*;

import jakarta.ws.rs.core.*;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import responses.ResponseEntity;
import responses.ResponseStatus;
import services.DragonImportService;
import utils.FileUploadData;

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

        try {
            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
            List<InputPart> inputParts = uploadForm.get("files");
            if (inputParts == null || inputParts.isEmpty()) {
                inputParts = uploadForm.get("file");
            }

            // проверка на минимальное количество файлов
            if (inputParts.size() < MIN_FILES_COUNT) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntity(ResponseStatus.ERROR, "Minimum " + MIN_FILES_COUNT + " files required", null))
                        .build();
            }

            List<FileUploadData> files = new ArrayList<>();
            for (InputPart part : inputParts) {
                MultivaluedMap<String, String> headers = part.getHeaders();
                String contentDisposition = headers.getFirst("Content-Disposition");
                String fileName = parseFileName(contentDisposition);

                String contentLengthStr = headers.getFirst("Content-Length");
                long fileSize = 0;
                if (contentLengthStr != null) {
                    try {
                        fileSize = Long.parseLong(contentLengthStr);
                    } catch (NumberFormatException ex) {
                        // TODO: Если не удалось получить размер, можно оставить fileSize = 0, но это может повлиять на загрузку
                    }
                }

                String contentType = headers.getFirst("Content-Type");
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                InputStream stream = part.getBody(InputStream.class, null);
                files.add(new FileUploadData(stream, fileName, fileSize, contentType));
            }

            // передаём список файлов в сервис импорта
            // importDragonsFromCsv последний раз видели в коммите 57f0594e
            dragonImportService.importDragonsFromCsvWith2PC(files, user);

            return Response.ok()
                    .entity(new ResponseEntity(ResponseStatus.SUCCESS, "Dragons imported successfully", null))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntity(ResponseStatus.ERROR, "Error during import: " + e.getMessage(), null))
                    .build();
        }
    }

    private String parseFileName(String contentDisposition) {
        if (contentDisposition == null) {
            return "unknown";
        }
        for (String cdPart : contentDisposition.split(";")) {
            if (cdPart.trim().startsWith("filename")) {
                String[] namePair = cdPart.split("=");
                if (namePair.length > 1) {
                    return namePair[1].trim().replaceAll("\"", "");
                }
            }
        }
        return "unknown";
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
