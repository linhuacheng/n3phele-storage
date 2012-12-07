package n3phele.service.impl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import n3phele.service.model.core.Credential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wordnik.swagger.annotations.ApiOperation;

public class HelloWorldResource {
	Log log = LogFactory.getLog(HelloWorldResource.class);

	@GET
	@Path("/greet")
	@ApiOperation(value = "Greeting", notes = "Add extra notes here", responseClass = "n3phele.service.model.core.Credential")
	public Response greet() {
		Credential credential =new Credential("test", "test");
		return Response.ok().entity(credential).build();
	}
}
