/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.model.repository;

import java.net.URI;

import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import n3phele.service.model.core.Credential;
import n3phele.service.model.core.Entity;


import com.google.appengine.api.datastore.Text;

@XmlRootElement(name="Repository")
@XmlType(name="Repository", propOrder={ "description", "target", "root", "kind"})
public class Repository extends Entity {
	@Id private Long id;
	private Text description;
	@Embedded private Credential credential;
	private String target;
	private String root;
	private String kind;
	
	public Repository() {}
	

	/**
	 * @param name of the repository
	 * @param description human readable description of the repository
	 * @param credential used to access repository
	 * @param target repository address
	 * @param root repository root path
	 * @param kind repository access method
	 */
	public Repository(String name, String description,
			Credential credential, URI target, String root, String kind, URI user, boolean isPublic) {
		super(name, null, "application/vnd.com.n3phele.Repository+json", user, isPublic);
		this.id = null;
		setDescription(description);
		this.credential = credential;
		this.target = (target == null)? null : target.toString();
		this.root = root;
		this.kind = kind;
	}
	
//	public FileRef fileRef(String localName, String description, String key) {
//		return new FileRef(localName, 
//				           description, 
//				           (this.target == null)?null : URI.create(this.target),
//				           this.root, 
//				           key, 
//				           this.credential, 
//				           this.kind,
//				           0,
//				           null);
//	}
	
	/*
	 * Getters and Setters
	 * -------------------
	 */
	
	/**
	 * @return the id
	 */
	@XmlTransient
	public Long getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}


	/**
	 * @return the description
	 */
	public String getDescription() {
		return (description==null)?null:description.getValue();
	}


	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = (description==null)?null:new Text(description);
	}


	/**
	 * @return the credential
	 */
	@XmlTransient
	public Credential getCredential() {
		return credential;
	}


	/**
	 * @param credential the credential to set
	 */
	public void setCredential(Credential credential) {
		this.credential = credential;
	}


	/**
	 * @return the target
	 */
	public URI getTarget() {
		URI result = null;
		if(this.target != null) 
			result = URI.create(this.target);
		return result;
	}


	/**
	 * @param target the target to set
	 */
	public void setTarget(URI target) {
		this.target = (target == null)? null: target.toString();
	}


	/**
	 * @return the root
	 */
	public String getRoot() {
		return root;
	}


	/**
	 * @param root the root to set
	 */
	public void setRoot(String root) {
		this.root = root;
	}


	/**
	 * @return the kind
	 */
	public String getKind() {
		return kind;
	}


	/**
	 * @param kind the kind to set
	 */
	public void setKind(String kind) {
		this.kind = kind;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("Repository [name=%s, uri=%s, mime=%s, id=%s, description=%s, credential=%s, target=%s, root=%s, kind=%s]",
						name, uri, mime, id, getDescription(), credential, target,
						root, kind);
	}


	public static Repository summary(Repository c) {
		if(c == null) return null;
		Repository result = new Repository(c.name, null, null, null, null, null, c.getOwner(), c.isPublic);
		result.uri = c.uri;
		return result;
	}
	

}
