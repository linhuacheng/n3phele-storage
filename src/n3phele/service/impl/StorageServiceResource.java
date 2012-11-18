package n3phele.service.impl;

import java.util.logging.Logger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;


//FIXME: Add the REST service endpoints to implement the CloudStorageInterface
// access points.


@Path("")
public class StorageServiceResource  {
		private static Logger log = Logger.getLogger(StorageServiceResource.class.getName()); 

		@Path("list")
		@GET
		@Produces("application/json")
		public Response list(
				@DefaultValue("false") @QueryParam("summary") Boolean summary) {

			log.warning("/storage entered with summary "+summary);
			
			return Response.ok("hello world").build();
		}
	}