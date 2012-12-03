package n3phele.storage.googledrive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import n3phele.service.core.ForbiddenException;
import n3phele.service.core.NotFoundException;
import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.repository.UploadSignature;
import n3phele.storage.CloudStorageInterface;
import n3phele.storage.ObjectStream;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;

public class CloudStorageImpl implements CloudStorageInterface {
	private static Logger log = Logger.getLogger(CloudStorageImpl.class
			.getName());
	/** Global instance of the HTTP transport. */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static String CLIENT_ID = "181673875910.apps.googleusercontent.com";
	private static String CLIENT_SECRET = "B5EYtnmskw_-s19eFr9GJoqf";
	private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	private static String GOOGLE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
	
	@Override
	public boolean createBucket(Repository repo) throws ForbiddenException {
		boolean result = false;
		log.info("Create bucket on " + repo.getRoot());
		
		Files.List request;
		try {
			Drive drive = getGoogleDrive(repo);
		
			// Check if folder exists
			request = drive.files().list()
					.setQ("title = '" + repo.getRoot() + "' and mimeType = '"
							+ GOOGLE_FOLDER_MIME_TYPE + "'");
			FileList files = request.execute();

			if (files.getItems().size() > 0) {
				log.info("Bucket " + repo.getRoot() + " already exists.");
			} else {
				// If the folder doesn't exist create it.
				File body = new File();
				body.setTitle(repo.getRoot());
				body.setDescription("n3phele repository");
				body.setMimeType(GOOGLE_FOLDER_MIME_TYPE);

				drive.files().insert(body).execute();

				log.info("Bucket " + repo.getRoot() + " created.");
			}
			result = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.log(Level.SEVERE, "Client Error processing created bucket "
					+ repo, e);
			throw new NotFoundException("Create bucket " + repo + " fails "
					+ e.toString());
		} catch (Exception e ){
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public FileNode getMetadata(Repository repo, String filename) {
		FileNode f = new FileNode();
		log.info("Get info on " + repo.getRoot() + " " + filename);
		try {
			File info = this.getFile(repo, repo.getRoot(), filename);

			FileNode file;
			if (GOOGLE_FOLDER_MIME_TYPE.equals(info.getMimeType())) {
				String name = info.getTitle();
				boolean isPublic = false;
				String prefix = "";
				Drive drive = getGoogleDrive(repo);
			
				// Check if the permission for the folder is public
				PermissionList permissions = drive.permissions().list(info.getId()).execute();
				for (Permission permission: permissions.getItems()){
					if ("anyone".equals(permission.getType()) && "reader".equals(permission.getRole())) {
						isPublic = true;
						break;
					}
				}
				file = FileNode.newFolder(name, prefix, repo, isPublic);
				file.setModified(new Date(info.getModifiedDate().getValue()));
			} else {
				String name = info.getTitle();
				String prefix = "";
				file = FileNode.newFile(name, prefix, repo, new Date(info
						.getModifiedDate().getValue()), info.getFileSize());
				file.setMime(info.getMimeType());
			}
			log.info("File:" + file);
			log.info(filename + " " + f.getModified() + " " + f.getSize());
		} catch (NotFoundException e) {
			log.log(Level.WARNING, "Service Error processing " + f + repo, e);
			throw new NotFoundException("Retrieve " + filename + " fails "
					+ e.toString());
		} catch (Exception e) {
			log.log(Level.SEVERE, "Client Error processing " + f + repo, e);
			throw new NotFoundException("Retrieve " + filename + " fails "
					+ e.toString());
		}
		return f;
	}

	@Override
	public boolean deleteFile(Repository repo, String filename) {
		boolean result = false;
		log.info("Deleting file on " + repo.getRoot() + " " + filename);
		try {
			File file = getFile(repo, repo.getRoot(), filename);
			Drive drive = getGoogleDrive(repo);
		
			drive.files().delete(file.getId());
			log.info("File deleted " + repo.getRoot() + " " + filename);
			result = true;
		} catch (NotFoundException e) {
			log.log(Level.WARNING, "Service Error getting " + repo + " "
					+ filename, e);
		} catch (IOException e) {
			e.printStackTrace();
			log.log(Level.SEVERE, "Client Error deleting " + repo + " "
					+ filename, e);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();			
			log.log(Level.SEVERE, "Client credential error " + repo + " "
					+ filename, e);
		}
		return result;
	}

	@Override
	public boolean deleteFolder(Repository repo, String filename) {
		boolean result = false;
		log.info("Deleting folder on " + repo.getRoot() + " " + filename);
		try {
			File folder = getFile(repo, repo.getRoot(), filename, true);
			Drive drive = getGoogleDrive(repo);
		
			ChildList childs = drive.children().list(folder.getId()).execute();
			for (ChildReference child: childs.getItems()) {
				drive.children().delete(folder.getId(), child.getId());
				log.info("Child files deleted " + folder.getId() + " - " +  child.getId());
			}

			log.info("Folder files deleted " + repo.getRoot() + " " + filename);
			result = true;
		} catch (NotFoundException e) {
			log.log(Level.WARNING, "Service Error getting folder " + repo + " "
					+ filename, e);
		} catch (IOException e) {
			e.printStackTrace();
			log.log(Level.SEVERE, "Client Error deleting folder " + repo + " "
					+ filename, e);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();			
			log.log(Level.SEVERE, "Client credential error " + repo + " "
					+ filename, e);
		}
		return result;
	}

	@Override
	public boolean setPermissions(Repository repo, String filename,
			boolean isPublic) {
		boolean result = false;
		log.info("Setting file permission on " + repo.getRoot() + " " + filename + " to " + (isPublic?"public": "private"));

		try {
			File file = this.getFile(repo, repo.getRoot(), filename);
			Drive drive = getGoogleDrive(repo);
		
			// Insert public permission
			if (isPublic) {
				  Permission permission = new Permission();
				  permission.setValue("");
				  permission.setType("anyone");
				  permission.setRole("reader");
				  drive.permissions().insert(file.getId(), permission).execute();
			} else {
				// Remove any public permissions		
				PermissionList permissions = drive.permissions().list(file.getId()).execute();
				for (Permission permission: permissions.getItems()){				
					if ("anyone".equals(permission.getType()) && "reader".equals(permission.getRole())) {
						drive.permissions().delete(file.getId(), permission.getId()).execute();
					}
				}
			}
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
			log.log(Level.SEVERE, "Client Error setting file permission on " + repo.getRoot() + " " + filename + " to " + (isPublic?"public": "private"), e);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();			
			log.log(Level.SEVERE, "Client credential error " + repo + " "
					+ filename, e);
		}

		return result;
	}

	@Override
	public boolean checkExists(Repository repo, String filename) {
		try {
			File file = getFile(repo, repo.getRoot(), filename);
			return (file != null);
		} catch (NotFoundException nfe) {
			return false;
		}

	}

	private File getFile(Repository repo, String parentTitle, String fileName) {
		return getFile(repo, parentTitle, fileName, false);
	}
	
	private File getFile(Repository repo, String parentTitle, String filename, boolean folder) {
		
		String parentId;
		Files.List request;

		// Get the id of parent folder
		try {
			Drive drive = getGoogleDrive(repo);
		
			String query = "title = '" + parentTitle + "'";
			if (folder) {
				query += "and mimeType = '" + GOOGLE_FOLDER_MIME_TYPE + "'";
			}
			request = drive.files().list().setQ(query);
			FileList files = request.execute();
			if (files.getItems().size() > 0) {
				parentId = files.getItems().get(0).getId();
			} else {
				throw new NotFoundException(parentTitle);
			}

			request = drive
					.files()
					.list()
					.setQ("'" + parentId + "' in parents and title = '"
							+ filename + "'");

			files = request.execute();
			if (files.getItems().size() > 0) {
				return files.getItems().get(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
			log.log(Level.SEVERE, "Client Error setting file permission on " + repo.getRoot() + " " + filename, e);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();			
			log.log(Level.SEVERE, "Client credential error " + repo + " "
					+ filename, e);
		}


		throw new NotFoundException(filename);

	}

	@Override
	public URI getRedirectURL(Repository repo, String path, String filename) {
		log.info("Getting redirect URL for " + repo.getRoot() + " " + filename);
		
		File file = getFile(repo, repo.getRoot(), filename);	
		return URI.create(file.getDownloadUrl());
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
	
	private Drive getGoogleDrive(Repository repo) throws IOException, GeneralSecurityException {
		GoogleCredential gCredential = getGoogleCredential(repo.getCredential());
		log.log(Level.INFO, "Credential " + gCredential);
		Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, gCredential).build();
	
		return drive;
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
					FileNode fileNode = null;
					if (GOOGLE_FOLDER_MIME_TYPE.equals(file.getMimeType())) {
						String name = file.getTitle();
						boolean isPublic = false;			
						// Check if the permission for the folder is public
						PermissionList permissions = drive.permissions().list(file.getId()).execute();
						for (Permission permission: permissions.getItems()){
							if ("anyone".equals(permission.getType()) && "reader".equals(permission.getRole())) {
								isPublic = true;
								break;
							}
						}
						fileNode = FileNode.newFolder(name, file.getDownloadUrl(), repo, isPublic);
						fileNode.setModified(new Date(file.getModifiedDate().getValue()));
					} else {
						String name = file.getTitle();
						fileNode = FileNode.newFile(name, file.getDownloadUrl(), repo, new Date(file
								.getModifiedDate().getValue()), file.getFileSize());
						fileNode.setMime(file.getMimeType());
					}
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
