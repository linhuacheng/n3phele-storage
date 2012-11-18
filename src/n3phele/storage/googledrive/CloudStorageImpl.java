package n3phele.storage.googledrive;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import n3phele.service.core.ForbiddenException;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.repository.UploadSignature;
import n3phele.storage.CloudStorageInterface;
import n3phele.storage.ObjectStream;

public class CloudStorageImpl implements CloudStorageInterface {

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
		// TODO Auto-generated method stub
		return null;
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
