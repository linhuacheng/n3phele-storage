package n3phele.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import n3phele.service.core.ForbiddenException;
import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.repository.UploadSignature;
import n3phele.service.model.store.RepositoryStore;
import n3phele.storage.CloudStorage;
import n3phele.storage.ObjectStream;
import n3phele.util.ServiceResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.ApiOperation;




//FIXME: Add the REST service endpoints to implement the CloudStorageInterface
// access points.
public class StorageServiceResource  {
		private static Logger log = Logger.getLogger(StorageServiceResource.class.getName()); 
		
		@DELETE
		@Path("/deleteFile/{repoId}/{file}")
		@ApiOperation(value="Delete a file from repository", notes="Returns resource not found if file could not be located")
		public Response deleteFile(@PathParam("repoId") Integer repoId
				, @PathParam("file") String fileName)
		{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			boolean result = false;
			result = CloudStorage.factory().deleteFile(repository, fileName);
			if(result) {
				return Response.status(Status.NO_CONTENT).entity("File deleted").build();
			} else {
				return Response.status(Status.NOT_FOUND).entity("File not found").build();
			}
		}
		
		@DELETE
		@Path("/deleteFolder/{repoId}/{folder}")
		@ApiOperation(value="Delete a folder and its contents from repository", notes="Returns resouce not found if folder could not be located")
		public Response deleteFolder(@PathParam("repoId") Integer repoId
				, @PathParam("folder") String folderName)
		{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			boolean result = false;
			result = CloudStorage.factory().deleteFolder(repository, folderName);
			if(result) {
				return Response.status(Status.NO_CONTENT).entity("Folder deleted").build();
			} else {
				return Response.status(Status.NOT_FOUND).entity("Folder not found").build();
			}
		}

		@PUT
		@Path("/setPermissions")
		@ApiOperation(value="Set permission for a given file", notes="Makes the folder either public or private", responseClass = "n3phele.util.ServiceResponse")
		public Response setPermissions(@QueryParam("file") String fileName
				, @QueryParam("repoId") Integer repoId
				, @QueryParam("isPublic") boolean isPublic)
		{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			boolean result = false;
			result = CloudStorage.factory().setPermissions(repository, fileName, true);
			ServiceResponse response = null;
			if(result) {
				response = new ServiceResponse(Status.OK.toString(), "Permissions set");
				return Response.ok().entity(response).build();
			} else {
				response = new ServiceResponse(Status.NOT_FOUND.toString(), "File not found");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}
		
		@GET
		@Path("/checkExists/{repoId}/{file}")
		@ApiOperation(value="Checks for the presense of a file", notes="Http Resource not found if file could not be found", responseClass="n3phele.util.ServiceResponse")
		public Response checkExists(@PathParam("repoId") Integer repoId, @PathParam("file") String fileName) {
			Repository repository = RepositoryStore.getRepositoryById(repoId);	
			boolean result=false;
			result = CloudStorage.factory().checkExists(repository, fileName);
			ServiceResponse response = null;
			if(result) {
				response = new ServiceResponse(Status.OK.toString(), "File exists");
				return Response.ok().entity(response).build();
			} else {
				response = new ServiceResponse(Status.NOT_FOUND.toString(), "File do not exist");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}

		@GET
		@Path("/hasTempURL/{repoId}")
		@ApiOperation(value="Checks for the temporary URL", notes="No Temporary URL is there is no temporary URL", responseClass="n3phele.util.ServiceResponse")
		public Response hasTemporaryUrl(@PathParam("repoId") Integer repoId) {
			Repository repository = RepositoryStore.getRepositoryById(repoId);	
			boolean result=false;
			result = CloudStorage.factory().hasTemporaryURL(repository);
			ServiceResponse response = null;
			if(result) {
				response = new ServiceResponse(Status.OK.toString(), "Has temporary URL");
				return Response.ok().entity(response).build();
			} else {
				response = new ServiceResponse(Status.NOT_FOUND.toString(), "Does not have temporary URL");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}

		@POST
		@Path("/putObject")
		@ApiOperation(value="Puts Object in the bucket", notes="Cannot be created if the object already exists or cannot be created", responseClass="n3phele.util.ServiceResponse")
		public Response putObject(@QueryParam("file") String fileName
				, @QueryParam("repoId") Integer repoId
				, @QueryParam("isPublic") boolean isPublic
				, @Context HttpServletRequest request) throws FileUploadException, IOException
		{
			Response response = Response.ok().build();
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			int i = 0;
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator iterator = upload.getItemIterator(request);
			List<URI> uris = new ArrayList<URI>();
			log.info("FileSizeMax ="+upload.getFileSizeMax()+" SizeMax="+upload.getSizeMax()+" Encoding "+upload.getHeaderEncoding());
			while (iterator.hasNext()) {         
				FileItemStream item = iterator.next();  
				i++;   
				if (item.isFormField()) {
					log.info("FieldName: "+item.getFieldName()+" value:"+Streams.asString(item.openStream()));
				} else {
					InputStream stream = item.openStream();   
					log.warning("Got an uploaded file: " + item.getFieldName() +", name = " + item.getName()+" content "+item.getContentType()); 
					URI target = CloudStorage.factory().putObject(repository, stream, item.getContentType(), fileName + i);
					uris.add(target);					
				}
			}
			String value = "";
			for (URI uri: uris) {
				if (value.length() >0)
					value += ", ";
				value += uri.toString();
			}
			ServiceResponse s = new ServiceResponse(Status.CREATED.toString(), value);
			response = Response.status(Status.CREATED).entity(s).build();
			
			return response;
		}

		@GET
		@Path("/getFileList")
		@ApiOperation(value="gets the list of number of files from the specified bucket ", notes="Only the maximum number of files are returned", responseClass="java.util.List")
		public Response getFileList(@QueryParam("repoId") Integer repoId
				, @QueryParam("filePrefix") String prefix
				, @QueryParam("max") Integer max){
			
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			List<FileNode> fileNodes = CloudStorage.factory().getFileList(repository, prefix, max);
			if (fileNodes != null){
				return Response.ok().entity(fileNodes).build();	
			} else {
				return Response.status(Status.NOT_FOUND).entity("no file found").build();	
			}			
		}
		

		@GET
		@Path("/getMetaData/{repoId}/{file}")
		@ApiOperation(value="Gets the metadata of the specified file ", notes="Returns No Content if the file is not in the bucket", responseClass="n3phele.service.model.repository.FileNode")
		public Response getMetadata(@PathParam("repoId") Integer repoId
				, @PathParam("file") String filename){
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			System.out.println("HP meta data ...... in service");
			FileNode filenode = CloudStorage.factory().getMetadata(repository, filename);
			System.out.println("HP meta data ...... in service");
			if (filenode != null){
				return Response.ok().entity(filenode).build();	
			} else {
				return Response.status(Status.NOT_FOUND).entity("No such File").build();	
			}
		}		

		@GET
		@Path("/getURL/{repoId}/{path}/{file}")
		@ApiOperation(value="Gets the private URL of the file", notes="Returns no content if File cannot be found", responseClass="n3phele.util.ServiceResponse")
		public Response getURL(@PathParam("repoId") Integer repoId
				, @PathParam("path") String path,
				@PathParam("file") String name){
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			URI uri = CloudStorage.factory().getURL(repository, path, name);
			ServiceResponse response = null;
			if (uri != null){
				response = new ServiceResponse(Status.OK.toString(), uri.toString());
				return Response.ok().entity(response).build();
			} else {
				response = new ServiceResponse(Status.NOT_FOUND.toString(), "no repository, url");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}
		
		@GET
		@Path("/getObject/{repoId}/{path}/{file}")
		@ApiOperation(value="Gets content of an Object in the bucket", notes="Cannot be found if the object does not exist", responseClass="n3phele.util.ObjectStream")
		public Response getObject(@PathParam("repoId") Integer repoId
				, @PathParam("path") String path,
				@PathParam("file") String name){
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			ObjectStream stream = CloudStorage.factory().getObject(repository, path, name);
			return Response.ok(stream.getOutputStream()).type(stream.getContextType()).build();
		}
		
		@GET
		@Path("/getUploadSignature/{repoId}/{file}")
		@ApiOperation(value="Gets the upload signature for an Object in the bucket", notes="Cannot be found if the object does not exist", responseClass="n3phele.util.UploadSignature")
		public Response getUploadSignature(@PathParam("repoId") Integer repoId
				,@PathParam("file") String name){
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			UploadSignature upsign = CloudStorage.factory().getUploadSignature(repository, name);
			if (upsign != null){
				return Response.ok().entity(upsign).build();	
			} else {
				return Response.status(Status.NOT_FOUND).entity("no signature").build();	
			}
		}
		
		@GET
		@Path("/getRedirectURL/{repoId}/{path}/{file}")
   	    @ApiOperation(value="Gets the redirect/public URL of the file", notes="Returns No Content if the file is not in the bucket", responseClass="n3phele.util.ServiceResponse")
		public Response getRedirectURL(@PathParam("repoId") Integer repoId
				,@PathParam("path") String path
				,@PathParam("file") String filename) {
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			URI uri = CloudStorage.factory().getRedirectURL(repository, path, filename);
			ServiceResponse response = null;
			if (uri != null){
				response = new ServiceResponse(Status.OK.toString(), uri.toString());
				return Response.ok().entity(response).build();
			} else {
				response = new ServiceResponse(Status.NOT_FOUND.toString(), "no repository, url");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}
		
		@GET
		@Path("/createBucket")
		@ApiOperation(value = "Create the Bucket in the Repository", notes = "If the bucket already exist, return a CONFLICT error.", responseClass = "n3phele.util.ServiceResponse")
		public Response createBucket(@QueryParam("repoId") Integer repoId) throws ForbiddenException{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			boolean boolVal = CloudStorage.factory().createBucket(repository);
			ServiceResponse response = null;
			if (boolVal == true){
				response = new ServiceResponse(Status.CREATED.toString(), "Bucket Created");
				return Response.status(Status.CREATED).entity(response).build();	
			} else {
				response = new ServiceResponse(Status.CONFLICT.toString(), "Bucket cannot be created");
				return Response.status(Status.CONFLICT).entity(response).build();	
			}
		}

				
		@GET
		@Path("/getType")
		@ApiOperation(value = "Get the Type of Repository", notes = "Possible values are: S3, Swift or Google Drive", responseClass = "n3phele.util.ServiceResponse")
		public Response getType(@QueryParam("repoId") Integer repoId) throws ForbiddenException{
			Repository repository = RepositoryStore.getRepositoryById(repoId);
			String type = CloudStorage.factory().getType(repository);
			ServiceResponse response = new ServiceResponse(Status.OK.toString(), type);
			if (type != null){
				return Response.ok().entity(response).build();	
			} else {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Bucket not created").build();	
			}			
			
		}
	
	}