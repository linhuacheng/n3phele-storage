/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.model.core;

import java.net.URI;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="Entity")
@XmlType(name="Entity", propOrder={"name", "uri", "mime", "owner", "public"})
public class Entity {
	
	protected String name;
	protected String uri;
	protected String mime;
	protected String owner;
	protected boolean isPublic;


	public Entity() {
		super();

	}
	

	/**
	 * @param name
	 * @param uri
	 * @param mime
	 * @param owner
	 * @param isPublic
	 */
	public Entity(String name, URI uri, String mime, URI owner, boolean isPublic) {
		this();
		this.name = name;
		this.uri = (uri == null)? null : uri.toString();
		this.mime = mime;
		this.owner = (owner == null)?null:owner.toString();
		this.isPublic = isPublic;
	}

	/*
	 * Getters and Setters..
	 * ---------------------
	 */
	

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * @return the uri
	 */
	public URI getUri() {
		URI result = null;
		if(this.uri != null)
			result = URI.create(uri);
		return result;
	}


	/**
	 * @param uri the uri to set
	 */
	public void setUri(URI uri) {
		if(uri != null)
			this.uri = uri.toString();
		else
			uri = null;
	}


	/**
	 * @return the mime
	 */
	public String getMime() {
		return mime;
	}


	/**
	 * @param mime the mime to set
	 */
	public void setMime(String mime) {
		this.mime = mime;
	}
	
	/**
	 * @return the owner uri
	 */
	public URI getOwner() {
		return (this.owner==null)?null:URI.create(this.owner);
	}


	/**
	 * @param owner the owner uri to set
	 */
	public void setOwner(URI owner) {
		this.owner = (owner == null)?null: owner.toString();
	}

	/**
	 * @return the isPublic visibility
	 */
	public boolean isPublic() {
		return isPublic;
	}
	
	/**
	 * @return the isPublic visibility
	 */
	public boolean getPublic() {
		return isPublic;
	}

	/**
	 * @param visibility the isPublic visibility to set
	 */
	public void setPublic(boolean visibility) {
		this.isPublic = visibility;
	}


	
}
