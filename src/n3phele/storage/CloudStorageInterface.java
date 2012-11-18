package n3phele.storage;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import n3phele.service.core.ForbiddenException;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.repository.UploadSignature;

public interface CloudStorageInterface {
	public boolean createBucket(Repository repo) throws ForbiddenException;
	public FileNode getMetadata(Repository repo, String filename);
	public boolean deleteFile(Repository repo, String filename);
	public boolean deleteFolder(Repository repo, String filename);
	public boolean setPermissions(Repository repo, String filename, boolean isPublic);
	public boolean checkExists(Repository repo, String filename);
	public URI getRedirectURL(Repository repo, String path, String filename);
	public UploadSignature getUploadSignature(Repository repo, String name);
	public boolean hasTemporaryURL(Repository repo);
	public List<FileNode> getFileList(Repository repo, String prefix, int max);
	public String getType();
	public URI putObject(Repository item, InputStream uploadedInputStream,
			String contentType, String destination);
	public ObjectStream getObject(Repository item, String path, String name);
	public URI getURL(Repository item, String path, String name) ;
}
