package n3phele.storage.S3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.Request;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AbstractAWSSigner;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.SigningAlgorithm;
import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.resources.S3ObjectResource;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sun.jersey.core.util.Base64;

import n3phele.service.core.ForbiddenException;
import n3phele.service.model.core.Credential;
import n3phele.service.core.NotFoundException;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.PolicyHelper;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.repository.UploadSignature;
import n3phele.storage.CloudStorageInterface;
import n3phele.storage.ObjectStream;

public class CloudStorageImpl implements CloudStorageInterface {
	private static Logger log = Logger.getLogger(CloudStorageImpl.class.getName()); 

	public boolean createBucket(Repository repo) throws ForbiddenException {
		Credential credential = repo.getCredential().decrypt();
		AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(credential.getAccount(), credential.getSecret()));
		s3.setEndpoint(repo.getTarget().toString());
		try {
			try {
	            s3.listObjects(new ListObjectsRequest(repo.getRoot(), null, null, null, 0));

	            // it exists and the current account owns it
	            return false;
	        } catch (AmazonServiceException ase) {
	            switch (ase.getStatusCode()) {
	            case 403:
	                /*
	                 * A permissions error means the bucket exists, but is owned by
	                 * another account.
	                 */
	                throw new ForbiddenException("Bucket "+repo.getRoot()+" has already been created by another user.");
	            case 404:
	            	Bucket bucket = s3.createBucket(repo.getRoot());
	            	log.info("Bucket created "+bucket.getName());
	                return true;
	            default:
	                throw ase;
	            }
	        }
			
		} catch (AmazonServiceException e) {
			log.log(Level.WARNING, "Service Error processing "+repo, e);

		}  catch (AmazonClientException e) {
			log.log(Level.SEVERE, "Client Error processing "+repo, e);

		}	
		return false;
	}

	public FileNode getMetadata(Repository repo, String filename) {
		FileNode f = new FileNode();
		UriBuilder result = UriBuilder.fromUri(repo.getTarget());
		result.path(repo.getRoot()).path(filename);			
		f.setName(result.build().toString());
		Credential credential = repo.getCredential().decrypt();
		
		log.info("Get info on "+repo.getRoot()+" "+filename);
		AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(credential.getAccount(), credential.getSecret()));
		s3.setEndpoint(repo.getTarget().toString());
		try {
			ObjectListing metadata = s3.listObjects(new ListObjectsRequest().withBucketName(repo.getRoot())
					.withPrefix(filename).withMaxKeys(1));
			List<S3ObjectSummary> metadataList = metadata.getObjectSummaries();
			if(metadataList == null || metadataList.size()!= 1) {
				throw new NotFoundException(filename);
			}
			f.setModified(metadataList.get(0).getLastModified());
			f.setSize(metadataList.get(0).getSize());
			
			log.info(filename+" "+f.getModified()+" "+f.getSize());
		} catch (AmazonServiceException e) {
			log.log(Level.WARNING, "Service Error processing "+f+repo, e);
			throw new NotFoundException("Retrieve "+filename+" fails "+e.toString());
		}  catch (AmazonClientException e) {
			log.log(Level.SEVERE, "Client Error processing "+f+repo, e);
			throw new NotFoundException("Retrieve "+filename+" fails "+e.toString());
		}
		return f;
	}

	public boolean deleteFile(Repository repo, String filename) {
		boolean result = false;
		Credential credential = repo.getCredential().decrypt();

		AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(credential.getAccount(), credential.getSecret()));
		s3.setEndpoint(repo.getTarget().toString());
		try {
			System.out.println("In the deleteFile method... ");
			System.out.println("bucket name ..."+repo.getRoot());
			System.out.println("File Name ... "+filename);
//			DeleteObjectRequest delreq = new DeleteObjectRequest(repo.getRoot(), filename);
//			s3.deleteObject(delreq);
			s3.deleteObject(repo.getRoot(), filename);
//			s3.deleteObject(new DeleteObjectRequest(repo.getRoot(), filename));
			
			return true;
		} catch (AmazonServiceException e) {
			log.log(Level.WARNING, "Service Error processing "+repo, e);
		}  catch (AmazonClientException e) {
			log.log(Level.SEVERE, "Client Error processing "+repo, e);
		}
		return result;
	}

	public boolean deleteFolder(Repository repo, String filename) {
		boolean result = false;
		Credential credential = repo.getCredential().decrypt();
		int retry = 3;
		
//		setPermissions(repo, filename, false);
		if(!filename.endsWith("/")) {
			filename += "/";
		}
		
		AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(credential.getAccount(), credential.getSecret()));
		s3.setEndpoint(repo.getTarget().toString());
		while(retry-- > 0) {
			try {
			    ObjectListing objects = s3.listObjects(repo.getRoot(), filename);
			    for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
			    	log.info("Delete "+repo.getRoot()+":"+objectSummary.getKey());
			    	DeleteObjectRequest ob = new DeleteObjectRequest(repo.getRoot(), objectSummary.getKey());
			        s3.deleteObject(ob);
			    }	
			    if(objects.isTruncated()) {
			    	retry++;
			    	log.info("Doing next portion");
			    	continue;
			    }
				return true;
//				break;
			} catch (AmazonServiceException e) {
				log.log(Level.WARNING, "Service Error processing "+repo, e);
			}  catch (AmazonClientException e) {
				log.log(Level.SEVERE, "Client Error processing "+repo, e);
			}  catch (Exception e) {
				log.log(Level.SEVERE, "Exception Occurred"+repo, e);
			}
		}
		return result;
	}

	public boolean setPermissions(Repository repo, String filename, boolean isPublic) {
		String bucket = repo.getRoot();
		Credential credential = repo.getCredential().decrypt();
		AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(credential.getAccount(), credential.getSecret()));
		String key = new S3ObjectResource(bucket, filename).getId();
		boolean inserted = false;

		s3.setEndpoint(repo.getTarget().toString());
		try {			
			List<Statement> statements = new ArrayList<Statement>();
			Policy policy = null;
			BucketPolicy bp = s3.getBucketPolicy(repo.getRoot());
			if(bp != null && bp.getPolicyText() != null) {
				log.info("Policy text "+bp.getPolicyText());
				policy = PolicyHelper.parse(bp.getPolicyText());
				log.info("Policy object is "+(policy==null?null:policy.toJson()));
	
				if(policy!=null) {
					if(policy.getStatements() != null) {
						for(Statement statement : policy.getStatements()) {
							if(statement.getId().equals("n3phele")) {
								List<com.amazonaws.auth.policy.Resource> resources = statement.getResources();
								List<com.amazonaws.auth.policy.Resource> update = new ArrayList<com.amazonaws.auth.policy.Resource>();
								if(resources != null) {
									for(com.amazonaws.auth.policy.Resource resource : resources) {
										String resourceName = resource.getId();
										if(resourceName.endsWith("*")) {
											resourceName = resourceName.substring(0, resourceName.length()-1);
										}
										if(!(resourceName+"/").startsWith(key+"/")) {
											update.add(resource);
										} else {
											log.info("Removing "+resource.getId());
										}
									}
								}
								if(isPublic && !inserted)
									update.add(new S3ObjectResource(repo.getRoot(), filename+"*"));
								if(update.size()>0) {
									statement.setResources(update);
									statements.add(statement);
								}
								inserted = true;
							} else {
								statements.add(statement);
							}
						}
					}
					if(!inserted && isPublic) {
						Statement statement = new Statement(Effect.Allow);
						statement.setId("n3phele");
						statement.setPrincipals(Arrays.asList(new Principal("*")));
						statement.setActions(Arrays.asList((Action)S3Actions.GetObject));
						statement.setResources(Arrays.asList((com.amazonaws.auth.policy.Resource) new S3ObjectResource(repo.getRoot(),filename+"*")));
						statements.add(statement);
					}
				} 
			}
			if(policy == null && isPublic) {
				policy = new Policy("n3phele-"+repo.getRoot());
				Statement statement = new Statement(Effect.Allow);
				statement.setId("n3phele");
				statement.setPrincipals(Arrays.asList(new Principal("*")));
				statement.setActions(Arrays.asList((Action)S3Actions.GetObject));
				statement.setResources(Arrays.asList((com.amazonaws.auth.policy.Resource) new S3ObjectResource(repo.getRoot(),filename+"*")));
				statements.add(statement);
			}
			if(policy != null) {
				if(statements.size() != 0) {
					policy.setStatements(statements);
					s3.setBucketPolicy(repo.getRoot(), policy.toJson());
					log.info("Set policy "+policy.toJson());
				} else {					
					s3.deleteBucketPolicy(repo.getRoot());
				}
			}
			return true;
			
		} catch (AmazonServiceException e) {
			log.log(Level.WARNING, "Service Error processing "+repo, e);
		}  catch (AmazonClientException e) {
			log.log(Level.SEVERE, "Client Error processing "+repo, e);
		}  catch (IllegalArgumentException e) {
			log.log(Level.SEVERE, "parse error ", e);
			log.log(Level.SEVERE, "cause", e.getCause());
		}
		return false;
	}

	public boolean checkExists(Repository repo, String filename) {
		boolean result = false;
		Credential credential = repo.getCredential().decrypt();

		AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(credential.getAccount(), credential.getSecret()));
		s3.setEndpoint(repo.getTarget().toString());
		try {
			GetObjectRequest gom = new GetObjectRequest(repo.getRoot(), filename);
			if(s3.getObject(gom)!=null)
				return true;
			log.info("Exists "+s3.getObject(gom).getObjectMetadata().getContentType());
		} catch (AmazonServiceException e) {
			log.log(Level.WARNING, "Service Error processing "+repo+" filename "+filename, e);
		}  catch (AmazonClientException e) {
			log.log(Level.SEVERE, "Client Error processing "+repo+" filename "+filename, e);
		}
		return result;
	}

	public List<FileNode> getFileList(Repository repo, String prefix, int max) {
		List<FileNode> result = new ArrayList<FileNode>();
		Credential credential = repo.getCredential().decrypt();

		AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(credential.getAccount(), credential.getSecret()));
		s3.setEndpoint(repo.getTarget().toString());
		Policy p=null;
		try {
			BucketPolicy bp = s3.getBucketPolicy(repo.getRoot());
			log.info("Policy text "+bp.getPolicyText());
			if(bp != null && bp.getPolicyText() != null) {
				p = PolicyHelper.parse(bp.getPolicyText());
				log.info("Policy object is "+(p==null?null:p.toJson()));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Policy not supported", e);
		}
			
		try {
			ObjectListing s3Objects = s3.listObjects(new ListObjectsRequest(repo.getRoot(), prefix, null, "/", max));
			if(s3Objects.getCommonPrefixes() != null) {
				for(String dirName : s3Objects.getCommonPrefixes()) {
					String name = dirName;
					if(dirName.endsWith("/")) {
						name = dirName.substring(0,dirName.length()-1);
					}
					boolean isPublic = isPublicFolder(p, repo.getRoot(), name);
					name = name.substring(name.lastIndexOf("/")+1);
					FileNode folder = FileNode.newFolder(name, prefix, repo, isPublic);
					log.info("Folder:"+folder);
					result.add(folder);
				}
			}
			if(s3Objects.getObjectSummaries() != null) {
				for(S3ObjectSummary fileSummary : s3Objects.getObjectSummaries()) {
					String key = fileSummary.getKey();
					if(key != null && !key.equals(prefix)) {
						String name = key.substring(key.lastIndexOf("/")+1);
						FileNode file = FileNode.newFile(name, prefix, repo, fileSummary.getLastModified(), fileSummary.getSize());
						log.info("File:"+file);
						result.add(file);
					}
				}
			}
			
		} catch (AmazonServiceException e) {
			log.log(Level.WARNING, "Service Error processing "+repo, e);
		}  catch (AmazonClientException e) {
			log.log(Level.SEVERE, "Client Error processing "+repo, e);
		}
		return result;
	}

	
	public String getType(Repository repo) {
		return "S3";
	}
	
	@Override
	public URI getRedirectURL(Repository item, String path, String filename) {
		UriBuilder result = null;
		result = UriBuilder.fromUri(item.getTarget());
		result.path(item.getRoot()).path(path).path(filename);
		String expires = Long.toString((Calendar.getInstance().getTimeInMillis()/1000) + 60*60);
		String stringToSign = "GET\n\n\n"+expires+"\n"+result.build().getPath().replace(" ", "%20");
		String signature = signS3QueryString(stringToSign, item.getCredential());

		result.queryParam("AWSAccessKeyId", item.getCredential().decrypt().getAccount());
		result.queryParam("Expires", expires);
		result.queryParam("Signature", signature);
		log.warning("Access "+result.build().getPath()+ " "+result.build());
		return result.build();
	}

	@Override
	public boolean hasTemporaryURL(Repository repo) {
		return true;
	}
	
	@Override
	public UploadSignature getUploadSignature(Repository item, String name) {
		Calendar expires = Calendar.getInstance();
		expires.add(Calendar.MINUTE,30);
		String acl = "private";
		String policy = getPolicy(expires, item.getRoot(), name, acl);
		String base64Policy = new String(Base64.encode(policy));
		String signature = signS3QueryString(base64Policy, item.getCredential());
		String awsId = item.getCredential().decrypt().getAccount();
		String contentType = Mimetypes.getInstance().getMimetype(name);
		
		UploadSignature result = new UploadSignature(name,acl,item.getTarget(), item.getRoot(), base64Policy, signature,awsId,contentType);
		return result;

	}
	
	/**
	 * @param policy
	 * @param bucket
	 * @param foldername
	 * @return
	 */
	private static boolean isPublicFolder(Policy policy, String bucket, String foldername) {
		if(policy==null)
			return false;
		String name = new S3ObjectResource(bucket, foldername).getId();
		if(policy.getStatements() != null) {
			for(Statement statement : policy.getStatements()) {
				if(statement.getId().equals("n3phele")) {
					List<com.amazonaws.auth.policy.Resource> resources = statement.getResources();
					if(resources != null) {
						for(com.amazonaws.auth.policy.Resource resource : resources) {
							String resourceKey = resource.getId();
							if(resourceKey.endsWith("*"))
								resourceKey = resourceKey.substring(0, resourceKey.length()-1);
							if((name+"/").startsWith(resourceKey+"/"))
								return true;
						}
					}
				}
			}
		}
		return false;
	}


	
	public String signS3QueryString(String stringToSign, Credential credential ) {
		QuerySigner signer = new QuerySigner();
		return signer.sign(stringToSign, credential.decrypt().getSecret());
	}
	
	private static class QuerySigner extends AbstractAWSSigner {

		/* (non-Javadoc)
		 * @see com.amazonaws.auth.Signer#sign(com.amazonaws.Request, com.amazonaws.auth.AWSCredentials)
		 */
		@Override
		public void sign(Request<?> arg0, AWSCredentials arg1)
				throws AmazonClientException {
		}
		
		
		public String sign(String stringToSign, String key) {
			   return sign(stringToSign, key, SigningAlgorithm.HmacSHA1);
		}
		
	}

	
	private String getPolicy(Calendar date, String bucket, String filename, String acl) {
		String expires = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(date.getTime());
		return String.format(
				"{\"expiration\": \"%s\","+
				  "\"conditions\":["+
				  "{\"bucket\": \"%s\"},"+ 
				  "{\"key\": \"%s\"},"+
				  "{\"success_action_status\": \"201\"},"+
				  "{\"acl\": \"%s\"},"+
				  "[\"starts-with\", \"$Content-Type\", \"\"],"+
				  "]"+
				"}",
				expires, bucket, filename, acl);
	}

	@Override
	public URI putObject(Repository item, InputStream uploadedInputStream,
			String contentType, String destination) {

		Credential credential = item.getCredential().decrypt();
		AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(credential.getAccount(), credential.getSecret()));
		s3.setEndpoint(item.getTarget().toString());

		ObjectMetadata ob = new ObjectMetadata();
		ob.setContentType(contentType);
		URI result = null;
		
		try {
            System.out.println("Uploading a new object to S3 from a file\n");
            s3.putObject(item.getRoot(), destination, uploadedInputStream, ob);
            result = getURL(item, null, destination);
            return result;
//            return getMetadata(item, destination).getRepository();
         } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }

		return result;
		
//		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public ObjectStream getObject(Repository item, String path, String name) {
		String contentType = null;
		Credential credential = item.getCredential().decrypt();

		AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(credential.getAccount(), credential.getSecret()));
		s3.setEndpoint(item.getTarget().toString());
		try {
			GetObjectRequest gom = new GetObjectRequest(item.getRoot(), name);
			System.out.println(gom.toString());
			if(s3.getObject(gom)!=null)
			{
				System.out.println("inside if block ...");
				S3Object s3Object = new S3Object();
				s3Object = s3.getObject(gom);
				contentType = s3Object.getObjectMetadata().getContentType();
				final InputStream in = s3Object.getObjectContent();

				System.out.println("after getting inputstream ... ");
				StreamingOutput so = new StreamingOutput() {

						@Override
						public void write(OutputStream arg0) throws IOException,
								WebApplicationException {
							// TODO Auto-generated method stub
							System.out.println("in the streamingoutput");
							int i = 0;
	                        while((i = in.read()) != - 1){
	                        	System.out.println(i);
	                            arg0.write(i);
	                        }
						}
					}; 
					
					System.out.println(so.toString());
				ObjectStream obStream = new ObjectStream(so, contentType);	
				return obStream;
			}
				
		} catch (AmazonServiceException e) {
			log.log(Level.WARNING, "Service Error processing "+item+" filename "+name, e);
		}  catch (AmazonClientException e) {
			log.log(Level.SEVERE, "Client Error processing "+item+" filename "+name, e);
		}

		return null;

	}

	@Override
	public URI getURL(Repository item, String path, String name) {
		UriBuilder filename = UriBuilder.fromUri(item.getTarget()).path(item.getRoot()).path(path).path(name);
		log.info("Path is "+filename.build().getPath());
		return filename.build();
	}
	

}
