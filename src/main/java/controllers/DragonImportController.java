package controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.ws.rs.*;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
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

    @POST
    @Path("/csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadDragons(MultipartFormDataInput input) {
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
                dragonImportService.importDragonsFromCsv(inputStream);
            }

            return Response.ok().entity("Dragons imported successfully").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error during import: " + e.getMessage())
                    .build();
        }
    }
}
