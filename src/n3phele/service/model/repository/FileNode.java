/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.model.repository;

import java.net.URI;
import java.util.Date;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="FileNode")
@XmlType(name="FileNode", propOrder={"name", "path", "mime", "repository", "repositoryName", "modified", "size"})
public class FileNode {

	private String name;
	private String path;
	private String mime;
	private String repository;
	@Transient
	private String repositoryName;
	private Date modified;
	private long size;
	
	public FileNode() {
	}
	

	/**
	 * @param name
	 * @param path
	 * @param mime
	 * @param repository
	 * @param modified
	 * @param size
	 */
	public FileNode(String name, String path, String mime,
			 URI repository, Date modified, long size) {
		this.name = name;
		this.path = path;
		this.mime = mime;
		setRepository(repository);
		this.modified = modified;
		this.size = size;
	}


	/*
	 * Getters and Setters
	 * -------------------
	 */
	
	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}


	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * @return the path
	 */
	public String getPath() {
		return this.path;
	}


	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}


	/**
	 * @return the mime
	 */
	public String getMime() {
		return this.mime;
	}


	/**
	 * @param mime the mime to set
	 */
	public void setMime(String mime) {
		this.mime = mime;
	}

	

	/**
	 * @return the repository
	 */
	public URI getRepository() {
		return this.repository==null?null:URI.create(this.repository);
	}


	/**
	 * @param repository the repository to set
	 */
	public void setRepository(URI repository) {
		this.repository = repository==null?null:repository.toString();
	}


	/**
	 * @return the modified
	 */
	public Date getModified() {
		return this.modified;
	}


	/**
	 * @param modified the modified to set
	 */
	public void setModified(Date modified) {
		this.modified = modified;
	}


	/**
	 * @return the size
	 */
	public long getSize() {
		return this.size;
	}


	/**
	 * @param size the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}

	

	/**
	 * @return the repositoryName
	 */
	@Transient
	public String getRepositoryName() {
		return this.repositoryName;
	}

	/**
	 * @param repositoryName the repositoryName to set
	 */
	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("FileNode [name=%s, path=%s, mime=%s, repository=%s, repositoryName=%s, modified=%s, size=%s]",
						this.name, this.path, this.mime, this.repository,
						this.repositoryName, this.modified, this.size);
	}


	/**
	 * @param dirName
	 * @param path
	 * @param repo
	 * @return
	 */
	public static FileNode newFolder(String dirName, String path,
			Repository repo, boolean isPublic) {
		FileNode f=  new FileNode(dirName, path, isPublic? "application/vnd.com.n3phele.PublicFolder":"application/vnd.com.n3phele.Folder", repo.getUri(), null, 0);
		f.setRepositoryName(repo.getName());
		return f;
	}


	/**
	 * @param name
	 * @param prefix
	 * @param repo
	 * @param lastModified
	 * @param size
	 * @return
	 */
	public static FileNode newFile(String name, String path,
			Repository repo, Date lastModified, long size) {
		FileNode f=  new FileNode(name, path, "application/vnd.com.n3phele.File", repo.getUri(), lastModified, size);
		f.setRepositoryName(repo.getName());
		return f;

	}


	

}
