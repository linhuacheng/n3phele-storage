package n3phele.service.impl;


import com.wordnik.swagger.annotations.*;
import javax.ws.rs.*;

@Path("/hello2.json")
@Api(value = "/hello2", description = "Operations about hello")
@Produces({"application/json"})
public class HelloWorldResourceJSON extends HelloWorldResource {}