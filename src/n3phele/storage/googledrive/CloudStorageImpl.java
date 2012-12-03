package n3phele.storage.googledrive;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

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
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Children;
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

    public static final String GOOGLE_DRIVE_TYPE = "GoogleDrive";

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
        FileNode file = new FileNode();
        log.info("Get info on " + repo.getRoot() + " " + filename);
        try {
            File info = this.getFile(repo, filename);

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
            log.info(filename + " " + file.getModified() + " " + file.getSize());
        } catch (NotFoundException e) {
            log.log(Level.WARNING, "Service Error processing " + file + repo, e);
            throw new NotFoundException("Retrieve " + filename + " fails "
                    + e.toString());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Client Error processing " + file + repo, e);
            throw new NotFoundException("Retrieve " + filename + " fails "
                    + e.toString());
        }
        return file;
    }

    @Override
    public boolean deleteFile(Repository repo, String filename) {
        boolean result = false;
        log.info("Deleting file on " + repo.getRoot() + " " + filename);
        try {
            File file = getFile(repo, filename);
            Drive drive = getGoogleDrive(repo);

            drive.files().delete(file.getId()).execute();
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
            Drive drive = getGoogleDrive(repo);
            String rootFileId = this.getRootFileId(repo);
            if (rootFileId == null || rootFileId.length() ==0) {
                throw new NotFoundException(repo.getRoot());
            }

            String query = "'"+ rootFileId + "' in parents";
            Files.List request = drive.files().list().setQ(query);
            FileList files = request.execute();
            for (File file: files.getItems()) {
                // if there is no prefix, this means clear the folder
                if (filename == null || filename.length() == 0) {
                    drive.files().delete(file.getId()).execute();
                    log.info("Child files deleted " + rootFileId + " - " +  file.getId());
                } else if(file.getTitle().startsWith(filename)) {
                    // if the prefix matches, delete the file
                    drive.files().delete(file.getId()).execute();
                    log.info("Child files deleted " + rootFileId + " - " +  file.getId());
                }
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
            File file = this.getFile(repo, filename);
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
            File file = getFile(repo, filename);
            return (file != null);
        } catch (NotFoundException nfe) {
            return false;
        }

    }

    private File getFile(Repository repo, String fileName) {
        return getFile(repo, fileName, false);
    }

    private File getFile(Repository repo, String filename, boolean folder) {
        // Get the id of parent folder
        try {
            Drive drive = getGoogleDrive(repo);
            String rootFileId = this.getRootFileId(repo);
            if (rootFileId == null || rootFileId.length() ==0) {
                throw new NotFoundException(repo.getRoot());
            }

            String query = "'"+ rootFileId + "' in parents";
            Files.List request = drive.files().list().setQ(query);
            FileList files = request.execute();
            for (File file: files.getItems()) {
                if (file.getTitle().equals(filename))
                    return file;
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

        File file = getFile(repo, filename);
        return URI.create(file.getDownloadUrl());
    }

    @Override
    public UploadSignature getUploadSignature(Repository repo, String name) {

        return null;
    }

    @Override
    public boolean hasTemporaryURL(Repository repo) {
        //google drive support temporary url
        return true;
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

    public String getRootFileId(Repository repo) {
        String fileId = "";
        Drive drive;
        try {
            drive = getGoogleDrive(repo);

            FileList files = drive.files().list()
                    .setQ("title = '" + repo.getRoot() + "' and mimeType = '"
                            + GOOGLE_FOLDER_MIME_TYPE + "'").execute();

            if (files.getItems().size() >0) {
                fileId = files.getItems().get(0).getId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileId;
    }


    @Override
    public List<FileNode> getFileList(Repository repo, String prefix, int max) {

        List<FileNode> fileNodeList = new ArrayList<FileNode>();
        try {
            com.google.api.services.drive.Drive.Files.List list = null;
            String query= null;
            Drive drive = getGoogleDrive(repo);
            if (prefix != null && !prefix.trim().isEmpty()){
                prefix = prefix.trim();

                query = "title contains '"+ prefix +"'";
            }
            //TODO:CK, commenting as it was not working, check with Cheng about it
//            String rootFileId = getRootFileId(repo);
//            if (rootFileId != null && rootFileId.length()>0)
//                query = query + "'" + rootFileId + "' in parents";

            if (query != null){
                list = drive.files().list().setQ(query);
            } else {
                list = drive.files().list();
            }


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
                    //check for max and break
                    if(max > 0 && fileNodeList.size() > max) {
                        break;
                    }
                    fileNodeList.add(fileNode);
                }
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Exception: " + e.getMessage());
        } catch (Exception e) {
            log.log(Level.WARNING, "Exception: " + e.getMessage());
        }

        return fileNodeList;
    }

    @Override
    public String getType() {
        return GOOGLE_DRIVE_TYPE;
    }

    @Override
    public URI putObject(Repository repo, InputStream uploadedInputStream,
                         String contentType, String destination) {
        File file = null;
        URI fileUri = null;

        try {

            Drive drive = getGoogleDrive(repo);
            File meta = new File();
            //CK, assuming destination is file name, destination might have parent folder name which is not accounted for
            meta.setTitle(destination);
            meta.setMimeType(contentType);
            meta.setEditable(true);

            BufferedInputStream bufferedStream = new BufferedInputStream(uploadedInputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);
            byte[] buf = new byte[1024];
            int length = 0;
            int readBytes = 0;
            while ((readBytes = bufferedStream.read(buf, 0, buf.length)) > 0){
                outputStream.write(buf, 0, readBytes);
                length = length + readBytes;

            }
            boolean isPublic = repo.isPublic();

            InputStreamContent streamContent = new InputStreamContent(contentType, new ByteArrayInputStream(outputStream.toByteArray()));
            streamContent.setLength(length);
            file = drive.files().insert(meta, streamContent).execute();

            if (file != null){
                String rootFileId = getRootFileId(repo);
                if (rootFileId != null && rootFileId.length()>0) {
                    ChildReference newChild = new ChildReference();
                    newChild.setId(file.getId());
                    drive.children().insert(rootFileId, newChild).execute();
                }
                executePermission(drive, file, isPublic);
            }
            fileUri = file != null ? new URI(file.getDownloadUrl().toString()): null;
            log.log(Level.INFO, "File Id:" + file.getId());
        } catch (IOException e) {
            log.log(Level.SEVERE,e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            log.log(Level.SEVERE,e.getMessage(), e);
        } catch (URISyntaxException e){
            log.log(Level.SEVERE,e.getMessage(), e);
        }

        return fileUri;
    }

    private void executePermission(Drive drive, File file, boolean isPublic) throws IOException {
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
    }

    @Override
    public ObjectStream getObject(Repository repo, String path, String name) {

        File file = getFile(repo, name, false);
        if (file!= null) {
            String urlStr = file.getDownloadUrl();
            Drive drive;
            try {
                drive = getGoogleDrive(repo);
                HttpResponse resp = drive.getRequestFactory().buildGetRequest(new GenericUrl(urlStr)).execute();
                final InputStream inputStream = resp.getContent();

                StreamingOutput output = new StreamingOutput() {


                    @Override
                    public void write(OutputStream outputStream) throws IOException,
                            WebApplicationException {
                        int i = 0;
                        while((i = inputStream.read()) != - 1){
                            outputStream.write(i);
                        }

                    }
                };
                return new ObjectStream(output, resp.getContentType());
            } catch (IOException e) {
                log.log(Level.SEVERE,e.getMessage(), e);
            } catch (GeneralSecurityException e) {
                log.log(Level.SEVERE,e.getMessage(), e);
            }
        }
        return null;

    }

    @Override
    public URI getURL(Repository repo, String path, String name) {
        URI fileUri = null;
        File file = getFile(repo, name, false);
        if (file!= null) {
            try {
                fileUri = file != null ? new URI(file.getDownloadUrl().toString()): null;
            } catch (URISyntaxException e) {
                log.log(Level.SEVERE,e.getMessage(), e);
            }
        }
        return fileUri;
    }

}
