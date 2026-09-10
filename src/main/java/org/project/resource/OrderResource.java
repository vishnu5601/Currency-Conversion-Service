package org.project.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.project.dto.OrderRequest;
import org.project.dto.OrderResponse;
import org.project.model.Order;
import jakarta.validation.Valid;
import org.project.service.OrderService;

import java.util.List;

@Path("/api/v1/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {
    @Inject
    OrderService orderService;

    @POST
    public Response createOrder(@Valid OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getOrder(@PathParam("id") Long id) {
        Order order = orderService.findById(id);
        if (order == null) {
            throw new jakarta.ws.rs.NotFoundException("No order found with id " + id);
        }
        return Response.ok(OrderResponse.from(order)).build();
    }

    @GET
    public Response listOrders(@QueryParam("customerId") String customerId) {
        List<OrderResponse> orders = orderService.listOrders(customerId);
        return Response.ok(orders).build();
    }
}
