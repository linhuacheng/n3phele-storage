package n3phele.service.impl;


import com.wordnik.swagger.annotations.*;
import javax.ws.rs.*;

@Path("/service.json")
@Api(value = "/service", description = "Operations about Storage Service")
@Produces({"application/json"})
public class StorageServiceResourceJSON extends StorageServiceResource {}