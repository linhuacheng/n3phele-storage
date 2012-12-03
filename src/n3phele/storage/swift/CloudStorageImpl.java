package n3phele.storage.swift;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.gae.config.AsyncGoogleAppEngineConfigurationModule;
import org.jclouds.hpcloud.objectstorage.HPCloudObjectStorageAsyncClient;
import org.jclouds.hpcloud.objectstorage.HPCloudObjectStorageClient;
import org.jclouds.hpcloud.objectstorage.HPCloudObjectStorageProviderMetadata;
import org.jclouds.hpcloud.objectstorage.options.CreateContainerOptions;
import org.jclouds.http.options.GetOptions;
import org.jclouds.openstack.swift.domain.ContainerMetadata;
import org.jclouds.openstack.swift.domain.MutableObjectInfoWithMetadata;
import org.jclouds.openstack.swift.domain.ObjectInfo;
import org.jclouds.openstack.swift.domain.SwiftObject;
import org.jclouds.openstack.swift.options.ListContainerOptions;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.rest.RestContext;

import com.amazonaws.services.s3.internal.Mimetypes;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

import n3phele.service.core.ForbiddenException;
import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.repository.UploadSignature;
import n3phele.storage.CloudStorageInterface;
import n3phele.storage.ObjectStream;

public class CloudStorageImpl implements CloudStorageInterface {
	private static Logger log = Logger.getLogger(CloudStorageImpl.class.getName());
	private boolean onGAE;
	public CloudStorageImpl() { this(true); }
	public CloudStorageImpl(boolean onGAE) {
		this.onGAE = onGAE;
	}

	public boolean createBucket(Repository repo) throws ForbiddenException {
		HPCloudObjectStorageClient client = getJcloudsContext(repo.getTarget(), Credential.unencrypted(repo.getCredential()));
		try {
            client.listObjects(repo.getRoot(), ListContainerOptions.Builder.underPath("").maxResults(1));
            // it exists and the current account owns it
            return false;
        } catch (ContainerNotFoundException e) {
        	boolean result = client.createContainer(repo.getRoot());
        	return result;
        } catch (Exception e) {
        	log.log(Level.INFO,"Bucket create", e);
        	throw new ForbiddenException("Bucket "+repo.getRoot()+" has already been created by another user.");
        }
	}
	
	public boolean deleteBucket(Repository repo) {
		HPCloudObjectStorageClient client = getJcloudsContext(repo.getTarget(), Credential.unencrypted(repo.getCredential()));
		try {
            return client.deleteContainerIfEmpty(repo.getRoot());
        } catch (ContainerNotFoundException e) {
        	return false;
        } 
	}

	public FileNode getMetadata(Repository repo, String filename) {
		HPCloudObjectStorageClient client = getJcloudsContext(repo.getTarget(), Credential.unencrypted(repo.getCredential()));
		try {
			MutableObjectInfoWithMetadata info = client.getObjectInfo(repo.getRoot(), filename);
			if(info == null)
				return null; // not found
			log.info("got "+info);

			FileNode file;
			if("application/directory".equals(info.getContentType())) {
				String fullName = info.getName();
				if(fullName.endsWith("/")) {
					fullName = fullName.substring(0, fullName.length()-1);
				}
				int end = fullName.lastIndexOf("/");
				String name = fullName.substring(end+1);
				String prefix = end < 0 ? "" : fullName.substring(0, end);
				ContainerMetadata metadata = client.getContainerMetadata(repo.getRoot());
				boolean isPublic = metadata.isPublic();
				file = FileNode.newFolder(name, prefix, repo, isPublic);
				file.setModified(info.getLastModified());
			} else {
				int end = info.getName().lastIndexOf("/");
				String name = info.getName().substring(end+1);
				String prefix = end < 0 ? "" : info.getName().substring(0, end+1);
				file = FileNode.newFile(name, prefix, repo, info.getLastModified(), info.getBytes());
				file.setMime(info.getContentType());
			}
			log.info("File:"+file);
			return file;
		} catch (AuthorizationException e) {
			throw new ForbiddenException("Unauthorized to access container "+repo.getRoot());
		}
	}

	public boolean deleteFile(Repository repo, String filename) {
		HPCloudObjectStorageClient client = getJcloudsContext(repo.getTarget(), Credential.unencrypted(repo.getCredential()));
		boolean result = false;
		try {
			client.removeObject(repo.getRoot(), filename);
			result = true;
		} catch (AuthorizationException e) {
			log.warning("Unauthorized to access Container "+repo.getRoot());
		}
		return result;
	}

	public boolean deleteFolder(Repository repo, String filename) {
		boolean result = false;
		HPCloudObjectStorageClient client = getJcloudsContext(repo.getTarget(), Credential.unencrypted(repo.getCredential()));
		int retry = 3;
		
		// setPermissions(repo, filename, false); // not needed since swift pattern is global
		if(!filename.endsWith("/")) {
			filename += "/";
		}

		while(retry-- > 0) {
			try {
				PageSet<ObjectInfo> list = client.listObjects(repo.getRoot(), ListContainerOptions.Builder.withPrefix(filename));
				log.info("Delete "+repo.getRoot()+" gets "+list.size());
				if(list.isEmpty()) {
					result = true;
					break;
				} else {
					retry++;
				}
			    for (ObjectInfo objectSummary : list) {
			    	log.info("Delete "+repo.getRoot()+":"+objectSummary.getName());
			        client.removeObject(repo.getRoot(), objectSummary.getName());
			    }	
			    if(list.getNextMarker()!= null) {
			    	//retry++;
			    	log.info("Doing next portion");
			    	continue;
			    }
//				result = true;
//				break;
			} catch (AuthorizationException e) {
				throw new ForbiddenException("Unauthorized to access Container "+repo.getRoot());
			}
		}
		client.removeObject(repo.getRoot(), filename.substring(0, filename.length()-1));

		return result;
	}

	public boolean setPermissions(Repository repo, String filename,
			boolean isPublic) {
		HPCloudObjectStorageClient client = getJcloudsContext(repo.getTarget(), Credential.unencrypted(repo.getCredential()));
		ContainerMetadata metadata = client.getContainerMetadata(repo.getRoot());
		if(metadata.isPublic() != isPublic) {
			CreateContainerOptions options;
			if(isPublic) {
				options = new CreateContainerOptions().withPublicAccess();
			} else {
				options = new CreateContainerOptions();
				options.buildRequestHeaders().put("X-Container-Read","");
			}
			client.createContainer(repo.getRoot(),options);
		}
		
		return true;
	}

	public boolean checkExists(Repository repo, String filename) {
		HPCloudObjectStorageClient client = getJcloudsContext(repo.getTarget(), Credential.unencrypted(repo.getCredential()));
		try {
			return client.objectExists(repo.getRoot(), filename);
		} catch (AuthorizationException e) {
			throw new ForbiddenException("Unauthorized to access Container "+repo.getRoot());
		}
	}

	public List<FileNode> getFileList(Repository repo, String prefix, int max) {
		ListContainerOptions options = ListContainerOptions.Builder.withPrefix(prefix==null?"":prefix).withDelimiter("/");
		if(max > 0)
			options = options.maxResults(max);
		List<FileNode> result = new ArrayList<FileNode> ();

		HPCloudObjectStorageClient client = getJcloudsContext(repo.getTarget(), Credential.unencrypted(repo.getCredential()));
		ContainerMetadata metadata = client.getContainerMetadata(repo.getRoot());
		try {
			PageSet<ObjectInfo> content = client.listObjects(repo.getRoot(), options);
			int i = 0;
			for(ObjectInfo o : content) {
				i++;
				if(o.getName().endsWith("/")) {
					if(o.getName().equals(prefix) && "application/directory".equals(o.getContentType()))
						continue; // found self psuedo-directory
					
					String name = o.getName().substring(0,o.getName().length()-1);
					boolean isPublic = metadata.isPublic();
					name = name.substring(name.lastIndexOf("/")+1);
					FileNode folder = FileNode.newFolder(name, prefix, repo, isPublic);
					result.add(folder);
					log.info("Folder:"+folder.toString());
				} else  {
					String key = o.getName();
					log.info("Found "+key);
					if(key != null && !key.equals(prefix)) {
						String name = key.substring(key.lastIndexOf("/")+1);
						FileNode file = FileNode.newFile(name, prefix, repo, o.getLastModified(), o.getBytes());
						file.setMime(o.getContentType());
						log.info("File:"+file);
						result.add(file);
					}
	
				}
			}
			return result;
		} catch (AuthorizationException e) {
			throw new ForbiddenException("Unauthorized to access Container "+repo.getRoot());
		}
	}
	
	public void createObject(Repository repo, String filename, String payload) {
		HPCloudObjectStorageClient client = getJcloudsContext(repo.getTarget(), Credential.unencrypted(repo.getCredential()));
		SwiftObject content = client.newSwiftObject();
		content.setPayload(payload);
		content.getInfo().setName(filename);
		client.putObject(repo.getRoot(), content);
	}
	
	
	@Override
	public UploadSignature getUploadSignature(Repository item, String name) {
		UriBuilder path = UriBuilder.fromUri("http://changeme.appspot.com/repository/");
		path.path(Long.toString(item.getId()));
		path.path("upload");
		path.queryParam("name", name);
		path.queryParam("path", "");
		long expires = ((Calendar.getInstance().getTimeInMillis())/1000) + 30*60;
		String signature = makeTemporaryCredential(expires, item.getCredential().decrypt().getSecret(), item.getRoot()+":"+name);
		path.queryParam("expires", expires);
		path.queryParam("signature", signature);

		log.warning("Gateway Upload "+path.build().getPath()+" "+path.build());
		
		String contentType = Mimetypes.getInstance().getMimetype(name);
		
		UploadSignature result = new UploadSignature(name,"acl",path.build(), item.getRoot(), "policy", "signature", "id", contentType);
		return result;
	} 
	
	public String getType() {
		return "Swift";
	}
	@Override
	public URI getRedirectURL(Repository repo, String path, String filename) {
		UriBuilder result;
		result = UriBuilder.fromUri("http://changeme.appspot.com/repository/");
		result.path(Long.toString(repo.getId()));
		result.path("redirect");
		if(!isNullOrBlank(path))
			result.path(path);
		result.path(filename);
		UriBuilder pathURI = UriBuilder.fromUri(repo.getTarget());
		pathURI.path(repo.getRoot()).path(path).path(filename);
		long expires = ((Calendar.getInstance().getTimeInMillis())/1000) + 30*60;
		String signature = makeTemporaryCredential(expires, repo.getCredential().decrypt().getSecret(), pathURI.build().toString());
		result.queryParam("expires", expires);
		result.queryParam("signature", signature);

		log.warning("Gateway Access "+result.build().getPath()+" "+result.build());
		return result.build();
	}
	
	private String makeTemporaryCredential(long expires, String secret, String path) {
		Credential temporary = Credential.encrypt(new Credential(path, path+expires), secret);
		return temporary.getSecret();
		
	}
	@Override
	public boolean hasTemporaryURL(Repository repo) {
		return false;
	}
	
	@Override
	public URI putObject(Repository item, InputStream uploadedInputStream,
			String contentType, String destination) {
		HPCloudObjectStorageClient swift = getJcloudsContext(item.getTarget(), item.getCredential().decrypt());
		URI result;
		try {
			SwiftObject objectDefn = swift.newSwiftObject();
			objectDefn.getInfo().setName(destination);
			objectDefn.setPayload(uploadedInputStream);
			if(contentType != null && !contentType.contains("octet-stream")) {
				objectDefn.getInfo().setContentType(contentType);
			}
			swift.putObject(item.getRoot(), objectDefn);
			result = objectDefn.getInfo().getUri();
		} finally {
		}

		return result;
	}
	
	@Override
	public ObjectStream getObject(Repository item, String path, String name) {
		HPCloudObjectStorageClient swift = getJcloudsContext(item.getTarget(), item.getCredential().decrypt());
		UriBuilder filename = UriBuilder.fromPath(path).path(name);
		log.info("Path is "+filename.build().getPath());
		SwiftObject file = swift.getObject(item.getRoot(), filename.build().getPath(), GetOptions.NONE);
		StreamingOutput stream = new SwiftStreamingOutput(file);
		ObjectStream result = new ObjectStream(stream, file.getPayload().getContentMetadata().getContentType());
		return result;		
	}
	
	private class SwiftStreamingOutput implements StreamingOutput {
		final private SwiftObject source;
		public SwiftStreamingOutput(SwiftObject file) {
			source = file;
		}

		@Override
		public void write(OutputStream output) throws IOException, WebApplicationException {
			try {
				source.getPayload().writeTo(output);
			} catch (Exception e) {
				throw new WebApplicationException(e);
			}
		}
		
	}

	private final static Map<String, RestContext<HPCloudObjectStorageClient, HPCloudObjectStorageAsyncClient>> clientContextMap = new HashMap<String, RestContext<HPCloudObjectStorageClient, HPCloudObjectStorageAsyncClient>>();
	private HPCloudObjectStorageClient getJcloudsContext(URI endpoint, Credential credential) {
		if(endpoint == null)
			endpoint = URI.create("https://region-a.geo-1.identity.hpcloudsvc.com:35357");
		Properties overrides = new Properties(); 
        overrides.setProperty(Constants.PROPERTY_ENDPOINT, endpoint.toString()); 
        overrides.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, 10000+"");
        overrides.setProperty(Constants.PROPERTY_SO_TIMEOUT, 10000+"");
        overrides.setProperty(Constants.PROPERTY_MAX_RETRIES, "1");
		RestContext<HPCloudObjectStorageClient, HPCloudObjectStorageAsyncClient> clientContext;
		String cacheKey = endpoint.toString()+"#"+credential.getAccount()+"#"+credential.getSecret();

		synchronized(clientContextMap) {
			clientContext = clientContextMap.get(cacheKey);
		}
		if(clientContext != null) {
			return clientContext.getApi();
		}	
		if(this.onGAE){
			clientContext =
			ContextBuilder.newBuilder(new HPCloudObjectStorageProviderMetadata())
			   .credentials(credential.getAccount(), credential.getSecret())
			   .modules(ImmutableSet.<Module>of(new AsyncGoogleAppEngineConfigurationModule()))
			   .overrides(overrides)
			   .build();
		} else {
			clientContext =
				ContextBuilder.newBuilder(new HPCloudObjectStorageProviderMetadata())
				   .credentials(credential.getAccount(), credential.getSecret())
				   .build();
				
		}
		
		HPCloudObjectStorageClient client = clientContext.getApi();
		synchronized(clientContextMap) {
			clientContextMap.put(cacheKey, clientContext);
		}
		return client;
	}
	
	@Override
	public URI getURL(Repository item, String path, String name) {
		HPCloudObjectStorageClient swift = getJcloudsContext(item.getTarget(), item.getCredential().decrypt());
		UriBuilder filename = UriBuilder.fromPath(path).path(name);
		log.info("Path is "+filename.build().getPath());
		MutableObjectInfoWithMetadata metadata = swift.getObjectInfo(item.getRoot(), filename.build().getPath());
		return metadata.getUri();		
	}
	
	private boolean isNullOrBlank(String s) {
		return s==null || s.isEmpty();
	}

}
