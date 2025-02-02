package controllers;

import auth.AdminRequest;
import auth.AdminRequestDTO;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import responses.ResponseEntity;
import responses.ResponseStatus;
import services.AdminService;

import java.util.List;

@Named(value = "adminController")
@ApplicationScoped
@Path("/admin")
public class AdminController {

    @Inject
    private AdminService adminService;

    @PostConstruct
    private void init() {
        System.out.println("AdminController initialized");
    }

    @GET
    @Path("/user/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("id") long id) {
        System.out.println("Trying to get user #" + id);
        AdminRequest adminRequest = adminService.getUserById(id);
        if (adminRequest != null) return Response.ok().entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "", adminRequest)
        ).build();
        return Response.status(Response.Status.NOT_FOUND).entity(
                new ResponseEntity(ResponseStatus.ERROR, "User not found", null)
        ).build();
    }

    // @QueryParam в случае '/dragons?page=1&size=10, @PathParam для штук типа '/dragon/{id}'
    @GET
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserList(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize,
            @QueryParam("filterValue") @DefaultValue("") String filterValue,
            @QueryParam("filterCol") @DefaultValue("") String filterCol,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDir") @DefaultValue("ASC") String sortDir
    ) {
        List<AdminRequestDTO> adminRequests = adminService.getUserList(page, pageSize, filterValue, filterCol, sortBy, sortDir);

        return Response.ok().entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "", adminRequests)
        ).build();
    }

    @PUT
    @Path("/user/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveAdminRequest(@PathParam("id") long id) {
        System.out.println("Trying to create user");

        adminService.createUser(id);

        System.out.println("Successfully created user");
        return Response.ok().entity(
                new ResponseEntity(ResponseStatus.SUCCESS,"Successfully created user", null)
        ).build();
    }

    @DELETE
    @Path("/user/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response denyAdminRequest(@PathParam("id") long id) {
        System.out.println("Trying to delete user #" + id);

        boolean isDeleted = adminService.deleteUserById(id);
        if (isDeleted) {
            return Response.noContent().entity(
                    new ResponseEntity(ResponseStatus.SUCCESS, "Successfully deleted user", null)
            ).build(); // статус 204, если удаление успешно
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity(
                    new ResponseEntity(ResponseStatus.ERROR, "User not found", null)
            ).build(); // статус 404, если дракон не найден
        }
    }
}

