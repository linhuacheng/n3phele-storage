package n3phele.storage;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import n3phele.service.core.ForbiddenException;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.repository.UploadSignature;

public abstract class CloudStorage implements CloudStorageInterface {

	public static CloudStorageInterface factory(String type) {
		if("S3".equalsIgnoreCase(type)) {
			return new n3phele.storage.S3.CloudStorageImpl();
		}
		if("Swift".equalsIgnoreCase(type)) {
			return new n3phele.storage.swift.CloudStorageImpl();
		}
		throw new IllegalArgumentException("Unknown Cloud storage type: "+type);
	}
	public static CloudStorageInterface factory(Repository repo) {
		return factory(repo.getKind());
	}
	
	private final static CloudStorageInterface provider = new CloudStorageInterface() {

		@Override
		public boolean createBucket(Repository repo) throws ForbiddenException {
			return factory(repo.getKind()).createBucket(repo);
		}

		@Override
		public FileNode getMetadata(Repository repo, String filename) {
			return factory(repo.getKind()).getMetadata(repo, filename);
		}

		@Override
		public boolean deleteFile(Repository repo, String filename) {
			return factory(repo.getKind()).deleteFile(repo, filename);
		}

		@Override
		public boolean deleteFolder(Repository repo, String filename) {
			return factory(repo.getKind()).deleteFolder(repo, filename);
		}

		@Override
		public boolean setPermissions(Repository repo, String filename,
				boolean isPublic) {
			return factory(repo.getKind()).setPermissions(repo, filename, isPublic);
		}

		@Override
		public boolean checkExists(Repository repo, String filename) {
			return factory(repo.getKind()).checkExists(repo, filename);
		}

		@Override
		public List<FileNode> getFileList(Repository repo, String prefix,
				int max) {
			return factory(repo.getKind()).getFileList(repo, prefix, max);
		}

		@Override
		public String getType() {
			throw new IllegalArgumentException();
		}

		@Override
		public URI getRedirectURL(Repository repo, String path, String filename) {
			return factory(repo.getKind()).getRedirectURL(repo, path, filename);
		}

		@Override
		public boolean hasTemporaryURL(Repository repo) {
			return factory(repo.getKind()).hasTemporaryURL(repo);
		}

		@Override
		public UploadSignature getUploadSignature(Repository repo, String name) {
			return factory(repo.getKind()).getUploadSignature(repo, name);
		}

		@Override
		public URI putObject(Repository repo, InputStream uploadedInputStream,
				String contentType, String destination) {
			return factory(repo.getKind()).putObject(repo, uploadedInputStream, contentType, destination);
		}

		@Override
		public ObjectStream getObject(Repository repo, String path, String name) {
			return factory(repo.getKind()).getObject(repo, path, name);
		}

		@Override
		public URI getURL(Repository repo, String path, String name) {
			return factory(repo.getKind()).getURL(repo, path, name);
		}
		
		
	};
	public static CloudStorageInterface factory() {
		return provider;
	}
}
