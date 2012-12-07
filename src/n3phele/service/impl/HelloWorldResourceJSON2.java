package n3phele.service.impl;


import com.wordnik.swagger.annotations.*;
import javax.ws.rs.*;

@Path("/hello.json")
@Api(value = "/hello", description = "Operations about hello")
@Produces({"application/json"})
public class HelloWorldResourceJSON2 extends HelloWorldResource {}