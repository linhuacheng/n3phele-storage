package n3phele.storage.googledrive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential.AccessMethod;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


import n3phele.service.core.ForbiddenException;
import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.repository.UploadSignature;
import n3phele.storage.CloudStorageInterface;
import n3phele.storage.ObjectStream;


public class CloudStorageImpl implements CloudStorageInterface {
	private static Logger log = Logger.getLogger(CloudStorageImpl.class.getName()); 
	/** Global instance of the HTTP transport. */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	  private static String CLIENT_ID = "181673875910-j3sg9gk1107upfu5tlrgih9bl9oeg13c.apps.googleusercontent.com";
	  private static String CLIENT_SECRET = "B5EYtnmskw_-s19eFr9GJoqf";
	  private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
	  
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	
	@Override
	public boolean createBucket(Repository repo) throws ForbiddenException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FileNode getMetadata(Repository repo, String filename) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteFile(Repository repo, String filename) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteFolder(Repository repo, String filename) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setPermissions(Repository repo, String filename,
			boolean isPublic) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkExists(Repository repo, String filename) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public URI getRedirectURL(Repository repo, String path, String filename) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UploadSignature getUploadSignature(Repository repo, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasTemporaryURL(Repository repo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<FileNode> getFileList(Repository repo, String prefix, int max) {
		
		List<FileNode> fileNodeList = new ArrayList<FileNode>();
		try {
			Credential cred = repo.getCredential();
			// service account credential (uncomment setServiceAccountUser for domain-wide delegation)
//        GoogleCredential credential = new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT)
//            .setJsonFactory(JSON_FACTORY)
//            .setServiceAccountId("[[INSERT SERVICE ACCOUNT EMAIL HERE]]")
//            .setServiceAccountScopes(PlusScopes.PLUS_ME)
//            .setServiceAccountPrivateKeyFromP12File(new File("key.p12"))
//            // .setServiceAccountUser("user@example.com")
//            .build();
			// set up global Plus instance
			
			//GoogleCredential gCredential = new GoogleCredential.Builder().setServiceAccountId(cred.getAccount()).build();
			java.io.File keyFile = new java.io.File("test/gdrive/b64713b4d6f38c36d0dd5b4f83fee7d5bebc04af-privatekey.p12");
//			GoogleCredential gCredential = new GoogleCredential.Builder()
//		            .setJsonFactory(new JacksonFactory())
//		            .setServiceAccountId("chandu.kempaiah@gmail.com")
//		            .setServiceAccountScopes(DriveScopes.DRIVE)
//		            .setServiceAccountPrivateKeyFromP12File(keyFile)
//		    
//		            // .setServiceAccountUser("user@example.com")
//		            .build();
		    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
		            HTTP_TRANSPORT, JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
		            .setAccessType("offline")
		            .setApprovalPrompt("auto").build();
			
			GoogleTokenResponse response = flow. newTokenRequest("4/UrHlo_JD3DN40yu6u2KxK7qT2DC9.koTWNrCy8Q0TuJJVnL49Cc8a69k5dgI").setRedirectUri(REDIRECT_URI).execute();
		    GoogleCredential gCredential = new GoogleCredential().setFromTokenResponse(response);
			
			log.log(Level.INFO, "Credential " + gCredential);
			Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, gCredential).build();
			com.google.api.services.drive.Drive.Files.List list = drive.files().list();
			FileList fileList = list.execute();
			for (File file : fileList.getItems()){
				
				//TODO: remove hardcoded properties and replace with appropriate values  
				FileNode fileNode = FileNode.newFolder(file.getTitle(), file.getDownloadUrl(),repo, true);
				log.log(Level.INFO, "File Title " + file.getTitle());
				fileNodeList.add(fileNode);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.log(Level.WARNING, "Exception: " + e.getMessage());			
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.log(Level.WARNING, "Exception: " + e.getMessage());			
			e.printStackTrace();
		} 
		
		return fileNodeList;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI putObject(Repository item, InputStream uploadedInputStream,
			String contentType, String destination) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectStream getObject(Repository item, String path, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getURL(Repository item, String path, String name) {
		// TODO Auto-generated method stub
		return null;
	}

}
