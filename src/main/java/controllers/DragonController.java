package controllers;

import auth.AuthService;
import dto.DragonDTO;
import dto.DragonHeadDTO;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import objects.*;
import responses.ResponseEntity;
import responses.ResponseStatus;
import jakarta.inject.Inject;
import services.DragonService;
import utils.DragonWebSocket;
import utils.PairReturnBooleanString;

import java.util.ArrayList;
import java.util.List;

@Named(value = "mainController")
@ApplicationScoped
@Path("/user")
public class DragonController {

    @Inject
    private DragonService dragonService;

    @Inject
    private AuthService authService;

    @PostConstruct
    private void init() {
        System.out.println("DragonController initialized");
    }

    @POST
    @Path("/dragon")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDragon(@Valid DragonDTO dragonDTO, @Context SecurityContext securityContext) {
        System.out.println("Trying to create dragon");

        String username = securityContext.getUserPrincipal().getName();
        long userId = authService.getUserByName(username).getId();

        System.out.println("user id: " + userId + ", username: " + username);

        Dragon dragon = dragonService.createDragon(
                dragonDTO,
                userId
        );

        System.out.println("Successfully created dragon");

        DragonWebSocket.broadcast((new ResponseEntity(ResponseStatus.SUCCESS, "Table needs to be updated!", null)).toJson());

        return Response.ok().entity(
                new ResponseEntity(ResponseStatus.SUCCESS,"Successfully created dragon", dragon)
        ).build();
    }

    @GET
    @Path("/dragon/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDragon(@PathParam("id") long id) {
        System.out.println("Trying to get dragon #" + id);
        Dragon dragon = dragonService.getDragonById(id);
        if (dragon != null) return Response.ok().entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "", dragon)
        ).build();
        return Response.status(Response.Status.NOT_FOUND).entity(
                new ResponseEntity(ResponseStatus.ERROR, "Dragon not found", null)
        ).build();
    }

    // @QueryParam в случае '/dragons?page=1&size=10, @PathParam для штук типа '/dragon/{id}'
    @GET
    @Path("/dragons")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDragons(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize,
            @QueryParam("filterValue") @DefaultValue("") String filterValue,
            @QueryParam("filterCol") @DefaultValue("") String filterCol,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDir") @DefaultValue("ASC") String sortDir
            ) {
        List<Dragon> dragons = dragonService.getDragons(page, pageSize, filterValue, filterCol, sortBy, sortDir);

        return Response.ok().entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "", dragons)
        ).build();
    }

    // подумать. пока было трудно
    @PUT
    @Path("/dragon/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDragon(@PathParam("id") long id, @Valid DragonDTO dragonDTO, @Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        System.out.println(username);

        long userId = authService.getUserByName(username).getId();
        System.out.println(userId);

        boolean isUpdated = dragonService.updateDragonById(id, userId, dragonDTO);

        if (isUpdated) {
            return Response.ok().entity(
                    new ResponseEntity(ResponseStatus.SUCCESS,"Successfully updated dragon", null)
            ).build();
        }

        DragonWebSocket.broadcast((new ResponseEntity(ResponseStatus.SUCCESS, "Table needs to be updated!", null)).toJson());

        return Response.status(Response.Status.FORBIDDEN).entity(
                new ResponseEntity(ResponseStatus.ERROR,"Dragon was not updated", null)
        ).build();
    }

    @DELETE
    @Path("/dragon/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDragon(@PathParam("id") long id, @Context SecurityContext securityContext) {
        System.out.println("Trying to delete dragon #" + id);

        String username = securityContext.getUserPrincipal().getName();
        System.out.println(username);

        long userId = authService.getUserByName(username).getId();
        System.out.println(userId);

        // узнал я, что ошибка летит после неудачного каскадного удаления, поэтому теперь так :с
        PairReturnBooleanString result;
        try {
            result = dragonService.deleteDragonById(id, userId);
        } catch (Exception e) {
            result = new PairReturnBooleanString(
                    false,
                    "You are not allowed to delete this dragon. Dragon is linked to another dragon(s)."
            );
        }

        DragonWebSocket.broadcast((new ResponseEntity(ResponseStatus.SUCCESS, "Table needs to be updated!", null)).toJson());

        if (result.isFirst()) {
            return Response.noContent().entity(
                    new ResponseEntity(ResponseStatus.SUCCESS, result.getSecond(), null)
            ).build(); // Статус 204, если удаление успешно
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity(
                    new ResponseEntity(ResponseStatus.ERROR, result.getSecond(), null)
            ).build(); // Статус 404, если дракон не найден
        }
    }

    @DELETE
    @Path("/dragons")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDragons(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        System.out.println(username);

        long userId = authService.getUserByName(username).getId();
        System.out.println(userId);

        int rowsDeleted = dragonService.deleteDragons(userId);

        DragonWebSocket.broadcast((new ResponseEntity(ResponseStatus.SUCCESS, "Table needs to be updated!", null)).toJson());

        if (rowsDeleted > 0) {
            //  можно использовать noContent(), но тогда не будет тела ответа
            return Response.ok().entity(
                    new ResponseEntity(ResponseStatus.SUCCESS, "Successfully deleted %d dragons".formatted(rowsDeleted), null)
            ).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity(
                    new ResponseEntity(ResponseStatus.ERROR, "Dragons belong to user not found", null)
            ).build();
        }
    }

    // ---------------- вспомогательные функции

    @GET
    @Path("/coordinates")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCoordinates() {

        List<Coordinates> coordinates = dragonService.getCoordinates();
        ArrayList<String> result = new ArrayList<>();

        for (Coordinates coordinate : coordinates) {
            result.add(coordinate.toJson());
        }

        return Response.status(Response.Status.OK).entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "", result)
        ).build();
    }

    @GET
    @Path("/caves")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDragonCave() {

        List<DragonCave> dragonCaves = dragonService.getDragonCave();
        ArrayList<String> result = new ArrayList<>();

        for (DragonCave cave : dragonCaves) {
            result.add(cave.toJson());
        }

        return Response.status(Response.Status.OK).entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "", result)
        ).build();
    }

    @GET
    @Path("/persons")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPerson() {

        List<Person> persons = dragonService.getPerson();
        ArrayList<String> result = new ArrayList<>();

        for (Person person : persons) {
            result.add(person.toJson());
        }

        return Response.status(Response.Status.OK).entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "", result)
        ).build();
    }

    @GET
    @Path("/heads")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDragonHead() {

        List<DragonHead> dragonHeads = dragonService.getDragonHead();
        ArrayList<String> result = new ArrayList<>();

        for (DragonHead head : dragonHeads) {
            result.add(head.toJson());
        }

        return Response.status(Response.Status.OK).entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "", result)
        ).build();
    }

    // ---------------- дополнительные функции

    @DELETE
    @Path("/fun-1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fun1(@Valid DragonHeadDTO head) {
        System.out.println(head);

        int deleted = dragonService.fun1(head.getEyesCount(), head.getToothCount());

        if (deleted > 0) {
            return Response.status(Response.Status.OK).entity(
                    new ResponseEntity(ResponseStatus.SUCCESS, "Successfully deleted %d dragons.".formatted(deleted), null)
            ).build();
        } else {
            return Response.status(Response.Status.OK).entity(
                    new ResponseEntity(ResponseStatus.SUCCESS, "Dragons either do not exist or can not be deleted.", null)
            ).build();
        }
    }

    @GET
    @Path("/fun-2")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fun2(@QueryParam("wingspan") Long wingspan) {
        System.out.println(wingspan);

        int count = dragonService.fun2(wingspan);

        return Response.status(Response.Status.OK).entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "There are %d dragon(s) with the wingspan of %d.".formatted(count, wingspan), null)
        ).build();
    }

    @GET
    @Path("/fun-3")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fun3(@QueryParam("character") String character) {
        System.out.println(character);

        List<Dragon> dragons = dragonService.fun3(character);

        return Response.status(Response.Status.OK).entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "Successfully found dragons meet the condition.", dragons)
        ).build();
    }

    @GET
    @Path("/fun-4")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fun4() {
        Dragon dragon = dragonService.fun4();

        return Response.status(Response.Status.OK).entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "Successfully found a dragon with the deepest cave.", dragon)
        ).build();
    }

    @PUT
    @Path("/fun-5/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fun5(@PathParam("id") long id, @Valid DragonDTO dragonDTO) {
        System.out.println(id + ") " + dragonDTO);

        long killerId = dragonService.fun5(id);

        return Response.status(Response.Status.OK).entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "Person #%d killed the dragon #%d.".formatted(killerId, id), null)
        ).build();
    }

    // ---------------- вспомогательные к дополнительным

    @GET
    @Path("/alive-dragons")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAliveDragons() {
        List<Dragon> dragons = dragonService.getAliveDragons();
        ArrayList<String> result = new ArrayList<>();

        for (Dragon dragon : dragons) {
            result.add(dragon.toJson());
        }

        return Response.status(Response.Status.OK).entity(
                new ResponseEntity(ResponseStatus.SUCCESS, "", result)
        ).build();
    }
}
