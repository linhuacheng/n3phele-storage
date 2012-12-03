package n3phele.storage.googledrive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tools.ant.types.LogLevel;

import com.google.api.client.auth.oauth2.Credential.AccessMethod;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.FileContent;
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
	private static Logger log = Logger.getLogger(CloudStorageImpl.class
			.getName());
	/** Global instance of the HTTP transport. */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static String CLIENT_ID = "181673875910.apps.googleusercontent.com";
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

	private GoogleCredential getGoogleCredential(Credential cred) throws IOException,
			GeneralSecurityException {
		Credential decryptedCred = cred.decrypt();
		java.io.File keyFile = new java.io.File(decryptedCred.getSecret());

		GoogleCredential gCredential = new GoogleCredential.Builder()
				.setTransport(HTTP_TRANSPORT)
				.setJsonFactory(JSON_FACTORY)
				.setServiceAccountId(decryptedCred.getAccount())
				.setServiceAccountScopes(DriveScopes.DRIVE)
				.setServiceAccountScopes(DriveScopes.DRIVE_FILE)
				.setServiceAccountPrivateKeyFromP12File(keyFile).build();
		return gCredential;
	}

	@Override
	public List<FileNode> getFileList(Repository repo, String prefix, int max) {

		List<FileNode> fileNodeList = new ArrayList<FileNode>();
		try {

			GoogleCredential gCredential = getGoogleCredential(repo.getCredential());
			log.log(Level.INFO, "Credential " + gCredential);
			Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, gCredential).build();
			com.google.api.services.drive.Drive.Files.List list = drive.files().list();
			if (list != null) {
				FileList fileList = list.execute();
				for (File file : fileList.getItems()) {

					// TODO: remove hardcoded properties and replace with
					// appropriate values
					FileNode fileNode = FileNode.newFolder(file.getTitle(),
							file.getDownloadUrl(), repo, true);
					log.log(Level.INFO, "File Title " + file.getTitle());
					fileNodeList.add(fileNode);
				}
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
	public URI putObject(Repository repo, InputStream uploadedInputStream,
			String contentType, String destination) {

		try {
			GoogleCredential gCredential = getGoogleCredential(repo.getCredential());
			Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY,
					gCredential).build();
			File file = new File();
			file.setTitle("Sample File1");
			file.setDescription("Gdrive sample upload");
			file.setMimeType(contentType);
			file.setEditable(true);

			java.io.File fileContent = new java.io.File(
					"test/gdrive/document.txt");
			FileContent mediaContent = new FileContent("text/plain",
					fileContent);
			file = drive.files().insert(file, mediaContent).execute();
			log.log(Level.INFO, "File Id:" + file.getId());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
