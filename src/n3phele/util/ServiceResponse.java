package n3phele.util;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ServiceResponse")
public class ServiceResponse {
	
	public ServiceResponse(String statusCode, String value) {
		this.statusCode = statusCode;
		this.value = value;	
	}

	private String statusCode;
	private String value;
	
	@XmlElement(name = "statusCode")
	public String getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	
	@XmlElement(name = "value")
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}
