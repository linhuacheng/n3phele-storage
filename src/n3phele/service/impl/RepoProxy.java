package n3phele.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import n3phele.service.model.core.Credential;
import n3phele.service.core.NotFoundException;
import n3phele.service.model.repository.Repository;
import n3phele.storage.CloudStorage;
import n3phele.storage.ObjectStream;

import com.google.common.io.ByteStreams;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010. All rights reserved.
 * 
 * This module proxies for a cloud filesystem that doesnt explicitly support
 * temporary access URLs for file access or upload. Instead we use a background
 * process in GAE to do this work on behalf of the cloud store. Temporary URLs
 * are being added to openstack, so this might not even still be necessary.
 */
@Path("proxy")
public class RepoProxy {
	private static Logger log = Logger.getLogger(RepoProxy.class.getName());  

	@Context UriInfo uriInfo;
	@Context SecurityContext securityContext;
	
	public RepoProxy() {
	}
	
	

	
	@GET
	@Path("{id}/redirect/{filename:[/\\w\\.\\%\\+\\-]+}")
	public Response redirect(@PathParam ("id") Long id, 
			@PathParam ("filename") String filename,
			@QueryParam("expires") long expires,
			@QueryParam("signature") String signature) throws NotFoundException {

		Repository item = null;
		
		//FIXME: need to pass in the required repo object items
		//       for this code to work!
		
		
		log.info("filename="+filename);

		UriBuilder result = UriBuilder.fromUri(item.getTarget());
		result.path(item.getRoot()).path(filename);
		if(expires == 0) {
			try {
				URL url = CloudStorage.factory().getURL(item, "", filename).toURL();
				final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				log.info("Access " + filename +" "+connection.getContentType());
				StreamingOutput stream = new StreamingOutput() {
			        public void write(OutputStream output) throws IOException, WebApplicationException {
			            try {
			            	ByteStreams.copy(connection.getInputStream(), output);
			            } catch (Exception e) {
			                throw new WebApplicationException(e);
			            }
			        }
			    };
				return Response.ok(stream).type(connection.getContentType()).build();
			} catch (Exception e) {
				log.log(Level.INFO, "Access error ", e);
				throw new NotFoundException();
			} 
		} else {
			if(!checkTemporaryCredential(expires, signature, item.getCredential().decrypt().getSecret(), result.build().toString())) {
				log.severe("Expired temporary authorization");
				throw new NotFoundException();
			}
			
			ObjectStream stream = CloudStorage.factory().getObject(item, "", filename);
			log.info("Access " + filename +" "+stream.getContextType());	
			return Response.ok(stream.getOutputStream()).type(stream.getContextType()).build();
		}
	}
	
	@POST
	@Path("{id}/upload/{bucket}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response upload(@PathParam ("id") Long id, @PathParam("bucket") String bucket,
					@QueryParam("name") String destination,
					@QueryParam("expires") long expires,
					@QueryParam("signature") String signature,
					@Context HttpServletRequest request) throws NotFoundException {
		Repository repo = null;
		
		// FIXME: Need to pass in the appropriate repo items for this to work
		
		if(!checkTemporaryCredential(expires, signature, repo.getCredential().decrypt().getSecret(), bucket+":"+destination)) {
			log.severe("Expired temporary authorization");
			throw new NotFoundException();
		}

			
		try {
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
					URI target = CloudStorage.factory().putObject(repo, stream, item.getContentType(), destination);
					Response.created(target).build();
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Processing error", e);
		}
		
		return Response.status(Status.REQUEST_ENTITY_TOO_LARGE).build();
	}
	
	/** Check temporary credential for validity
	 * @param expires
	 * @param signature
	 * @param secret
	 * @param path
	 * @return TRUE if temporary credential is valid
	 */
	private boolean checkTemporaryCredential(long expires, String signature, String secret, String path) {
		Credential temporary = Credential.encrypt(new Credential(path, path+expires), secret);
		long now = (Calendar.getInstance().getTimeInMillis())/1000;
		if(now > expires) {
			log.info("Key has expired");
			return false;
		}
		if(!signature.equals(temporary.getSecret())) {
			temporary.setAccount(signature);
			temporary = Credential.decrypt(temporary, secret);
			log.info("Signature mismatch. generated using \""+path+expires+"\" against "+temporary.getAccount());
			return false;
		}
		return true;
	}
}
