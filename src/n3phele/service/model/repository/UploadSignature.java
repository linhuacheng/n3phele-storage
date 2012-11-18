/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.model.repository;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="UploadSignature")
@XmlType(name="UploadSignature", propOrder={"filename", "acl", "url", "base64Policy", "signature", "awsId", "contentType"})
public class UploadSignature {
	private String filename;
	private String acl ;
	private String url;
	private String base64Policy;
	private String signature;
	private String awsId;
	private String contentType;
	
	public UploadSignature() {};
	
	/**
	 * @param filename
	 * @param acl
	 * @param address
	 * @param bucket
	 * @param base64Policy
	 * @param signature
	 * @param awsId
	 * @param contentType
	 */
	public UploadSignature(String filename, String acl, URI address, String bucket,
			String base64Policy, String signature, String awsId,
			String contentType) {
		super();
		this.filename = filename;
		this.acl = acl;
		this.url = makeUrl(address, bucket);
		this.base64Policy = base64Policy;
		this.signature = signature;
		this.awsId = awsId;
		this.contentType = contentType;
	}





	/**
	 * @return the filename
	 */
	public String getFilename() {
		return this.filename;
	}
	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	/**
	 * @return the acl
	 */
	public String getAcl() {
		return this.acl;
	}
	/**
	 * @param acl the acl to set
	 */
	public void setAcl(String acl) {
		this.acl = acl;
	}

	/**
	 * @return the url
	 */
	public synchronized final String getUrl() {
		return this.url;
	}

	/**
	 * @param url the url to set
	 */
	public synchronized final void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the base64Policy
	 */
	public String getBase64Policy() {
		return this.base64Policy;
	}
	/**
	 * @param base64Policy the base64Policy to set
	 */
	public void setBase64Policy(String base64Policy) {
		this.base64Policy = base64Policy;
	}
	/**
	 * @return the signature
	 */
	public String getSignature() {
		return this.signature;
	}
	/**
	 * @param signature the signature to set
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}
	/**
	 * @return the awsId
	 */
	public String getAwsId() {
		return this.awsId;
	}
	/**
	 * @param awsId the awsId to set
	 */
	public void setAwsId(String awsId) {
		this.awsId = awsId;
	}
	/**
	 * @return the contentType
	 */
	public String getContentType() {
		return this.contentType;
	}
	/**
	 * @param contentType the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("UploadSignature [filename=%s, acl=%s, url=%s, base64Policy=%s, signature=%s, awsId=%s, contentType=%s]",
						this.filename, this.acl, this.url, this.base64Policy,
						this.signature, this.awsId, this.contentType);
	}

	private String makeUrl(URI address, String bucket) {
		UriBuilder builder = UriBuilder.fromUri(address);
		builder.path(bucket);
		return builder.build().toString();
//		String result = "https://"+bucket+"."+address.getHost();
//		return result;
	}
	

}
