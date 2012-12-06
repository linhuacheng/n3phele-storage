package n3phele.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import com.google.gson.Gson;

import n3phele.service.core.ForbiddenException;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.repository.UploadSignature;
import n3phele.service.model.store.RepositoryStore;
import n3phele.storage.CloudStorage;
import n3phele.storage.ObjectStream;




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
		
		
		@Path("deleteFile/{fileName}")
		@DELETE
		@Produces("text/html")
		public Response deleteFile(@QueryParam("repoId") Integer repoId
				, @PathParam("fileName") String fileName)
		{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			boolean result = false;
			result = CloudStorage.factory().deleteFile(repository, fileName);
			if(result)
				return Response.status(Status.OK).build();
			else
				return Response.ok("Unable to delete or file not found").build();
			
		}
		
		
		@Path("deleteFolder/{fileName}")
		@DELETE
		@Produces("text/html")
		public Response deleteFolder(@QueryParam("repoId") Integer repoId
				, @PathParam("fileName") String fileName)
		{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			boolean result = false;
			result = CloudStorage.factory().deleteFolder(repository, fileName);
			if(result)
				return Response.status(Status.OK).build();
			else
				return Response.status(Status.CONFLICT).build();
		}

		@Path("setPermissions")
		@POST
		@Produces("text/html")
		public Response setPermissions(@QueryParam("fileName") String fileName
				, @QueryParam("repoId") Integer repoId
				, @QueryParam("isPublic") boolean isPublic)
		{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			boolean result = false;
			result = CloudStorage.factory().setPermissions(repository, fileName, true);
			if(result)
				return Response.status(Status.OK).build();
			else
				return Response.status(Status.CONFLICT).build();
		}
		
		@Path("checkExists")
		@GET
		@Produces("text/html")
		public Response checkExists(@QueryParam("repoId") Integer repoId, @QueryParam("fileName") String fileName) {
			Repository repository = RepositoryStore.getRepositoryById(repoId);	
			boolean result=false;
			result = CloudStorage.factory().checkExists(repository, fileName);
			if(result)
				return Response.status(Status.OK).build();
			else
				return Response.status(Status.FORBIDDEN).build();
		}
		
		
		@Path("hasURL")
		@GET
		@Produces("text/html")
		public Response hasTemporaryUrl(@QueryParam("repoId") Integer repoId) {
			Repository repository = RepositoryStore.getRepositoryById(repoId);	
			boolean result=false;
			result = CloudStorage.factory().hasTemporaryURL(repository);
			if(result)
				return Response.status(Status.OK).build();
			else
				return Response.status(Status.FORBIDDEN).build();
		}

		@Path("putObject")
		@POST
		@Produces("text/html")
		public Response putObject(@QueryParam("fileName") String fileName
				, @QueryParam("repoId") Integer repoId
				, @QueryParam("isPublic") boolean isPublic
				, @Context HttpServletRequest request) throws FileUploadException, IOException
		{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			boolean result = false;
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator iterator = upload.getItemIterator(request);
			log.info("FileSizeMax ="+upload.getFileSizeMax()+" SizeMax="+upload.getSizeMax()+" Encoding "+upload.getHeaderEncoding());
			while (iterator.hasNext()) {         
				FileItemStream item = iterator.next();  
				       
				if (item.isFormField()) {
					log.info("FieldName: "+item.getFieldName()+" value:"+Streams.asString(item.openStream()));
				} else {
					InputStream stream = item.openStream();   
					log.warning("Got an uploaded file: " + item.getFieldName() +", name = " + item.getName()+" content "+item.getContentType()); 
					URI target = CloudStorage.factory().putObject(repository, stream, item.getContentType(), fileName);
					Response.created(target).build();
				}
			}
//			result = CloudStorage.factory().setPermissions(repository, fileName, true);
			if(result)
				return Response.status(Status.OK).build();
			else
				return Response.ok("Unable to upload").build();
		}

		
		@Path("getFileList")
		@GET
		//@Produces("application/json")
		public Response getFileList(@QueryParam("repoId") Integer repoId
				, @QueryParam("filePrefix") String prefix
				, @QueryParam("max") Integer max){
			
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			List<FileNode> fileNodes = CloudStorage.factory().getFileList(repository, prefix, max);
			Gson gson = new Gson();
			String json = gson.toJson(fileNodes);
	
			if (fileNodes != null){
				return Response.ok(json).build();	
			} else {
				return Response.ok("nofile found").build();	
			}
			
			//return Response.ok("hello world").build();	
		}
		
		@Path("getMetaData")
		@GET
		@Produces("application/json")

		//public FileNode getMetadata(Repository repo, String filename)
		public Response getMetadata(@QueryParam("repoId") Integer repoId
				, @QueryParam("filename") String filename){
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			System.out.println("HP meta data ...... in service");
			FileNode filenode = CloudStorage.factory().getMetadata(repository, filename);
			System.out.println("HP meta data ...... in service");
			Gson gson = new Gson();
			String json = gson.toJson(filenode);
			if (filenode != null){
				return Response.ok(json).build();	
			} else {
				return Response.ok("No such File").build();	
			}
		}
		
		//public URI getURL(Repository item, String path, String name)
		@Path("getURL")
		@GET
		@Produces("application/json")
		public Response getURL(@QueryParam("repoId") Integer repoId
				, @QueryParam("path") String path,
				@QueryParam("name") String name){
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			URI uri = CloudStorage.factory().getURL(repository, path, name);
			Gson gson = new Gson();
			String json = gson.toJson(uri);
			if (uri != null){
				return Response.ok(json).build();	
			} else {
				return Response.ok("no repository, url").build();	
			}
		}
		
		//public ObjectStream getObject(Repository item, String path, String name);
		@Path("getObject")
		@GET
		//@Produces("application/json")
		public Response getObject(@QueryParam("repoId") Integer repoId
				, @QueryParam("path") String path,
				@QueryParam("name") String name){
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			ObjectStream stream = CloudStorage.factory().getObject(repository, path, name);
			Gson gson = new Gson();
//			String json = gson.toJson(objstr);
//			if (objstr != null){
//				return Response.	
//			} else {
//				return Response.ok("no repository, url").build();	
//			}
			return Response.ok(stream.getOutputStream()).type(stream.getContextType()).build();
		}
		
		//public UploadSignature getUploadSignature(Repository repo, String name);
		@Path("getUploadSignature")
		@GET
		@Produces("application/json")
		public Response getUploadSignature(@QueryParam("repoId") Integer repoId
				,@QueryParam("name") String name){
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			UploadSignature upsign = CloudStorage.factory().getUploadSignature(repository, name);
			Gson gson = new Gson();
			String json = gson.toJson(upsign);
			if (upsign != null){
				return Response.ok(json).build();	
			} else {
				return Response.ok("no signature").build();	
			}
		}
		
//		public URI getRedirectURL(Repository repo, String path, String filename);
		@Path("getRedirectURL")
		@GET
		@Produces("application/json")
		public Response getRedirectURL(@QueryParam("repoId") Integer repoId
				,@QueryParam("path") String path
				,@QueryParam("filename") String filename) {
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			URI uri = CloudStorage.factory().getRedirectURL(repository, path, filename);
			Gson gson = new Gson();
			String json = gson.toJson(uri);
			if (uri != null){
				return Response.ok(json).build();	
			} else {
				return Response.ok("no signature").build();	
			}
		}
		
		//public boolean createBucket(Repository repo) throws ForbiddenException;
		@Path("createBucket")
		@GET
		@Produces("application/json")
		public Response createBucket(@QueryParam("repoId") Integer repoId) throws ForbiddenException{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			boolean boolVal = CloudStorage.factory().createBucket(repository);
			Gson gson = new Gson();
			String json = gson.toJson(boolVal);
			if (boolVal == true){
				return Response.ok(json).build();	
			} else {
				return Response.ok("Bucket not created").build();	
			}
		}

				
		//public String getType();
		@Path("getType")
		@GET
		@Produces("application/json")
		public Response getType(@QueryParam("repoId") Integer repoId) throws ForbiddenException{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			String type = CloudStorage.factory().getType(repository);
			Gson gson = new Gson();
			String json = gson.toJson(type);
			if (type != null){
				return Response.ok(json).build();	
			} else {
				return Response.ok("Bucket not created").build();	
			}
		}
	}