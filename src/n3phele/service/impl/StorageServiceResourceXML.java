package n3phele.service.impl;


import com.wordnik.swagger.annotations.*;
import javax.ws.rs.*;

@Path("/service.xml")
@Api(value = "/service", description = "Operations about Storage Service")
@Produces({"application/xml"})
public class StorageServiceResourceXML extends StorageServiceResource {}