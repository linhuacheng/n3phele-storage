package n3phele.service.impl;

import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.store.RepositoryStore;
import n3phele.storage.CloudStorage;




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
		
		@Path("getFileList")
		@GET
		//@Produces("application/json")
		public Response getFileList(@QueryParam("repoId") Integer repoId
				, @QueryParam("filePrefix") String prefix
				, @QueryParam("max") Integer max){
			
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			List<FileNode> fileNodes = CloudStorage.factory().getFileList(repository, prefix, max);
			if (fileNodes != null){
				return Response.ok(fileNodes.get(0).getName()).build();	
			} else {
				return Response.ok("nofile found").build();	
			}
			
			//return Response.ok("hello world").build();	
		}
	}