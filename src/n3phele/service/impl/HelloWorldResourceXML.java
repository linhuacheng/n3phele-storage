package n3phele.service.impl;


import com.wordnik.swagger.annotations.*;
import javax.ws.rs.*;

@Path("/hello.xml")
@Api(value = "/hello", description = "Operations about hello")
@Produces({"application/xml"})
public class HelloWorldResourceXML extends HelloWorldResource {}