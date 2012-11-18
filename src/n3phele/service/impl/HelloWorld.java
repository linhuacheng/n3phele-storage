package n3phele.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.api.server.spi.response.ForbiddenException;
import com.google.common.io.ByteStreams;

@Path("helloworld")
public class HelloWorld {
	Log log = LogFactory.getLog(HelloWorld.class);
	
	@Path("greet")
	@GET
	public Response greet(@QueryParam("param1") String param1) throws Exception{
		
		
		
//		StreamingOutput stream = new StreamingOutput() {
//	        public void write(OutputStream output) throws IOException, WebApplicationException {
//	            try {
//	            	ByteStreams.copy(new ByteArrayInputStream("Hello World".getBytes()), output);
//	            } catch (Exception e) {
//	                throw new WebApplicationException(e);
//	            }
//	        }
//	    };
		log.debug("param1:" + param1);
		//throw new ForbiddenException("Resource is forbidden");
		return Response.ok("Hello World :" + param1).build();
	}

}
