package n3phele.storage;

import javax.ws.rs.core.StreamingOutput;

public class ObjectStream {
	private StreamingOutput outputStream;
	private String contextType;
	public ObjectStream(StreamingOutput stream, String contentType) {
		this.outputStream = stream;
		this.contextType = contentType;
	}
	
	
	/**
	 * @return the outputStream
	 */
	public StreamingOutput getOutputStream() {
		return outputStream;
	}


	/**
	 * @return the contextType
	 */
	public String getContextType() {
		return contextType;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("ObjectStream [outputStream=%s, contextType=%s]",
				outputStream, contextType);
	}
	
}
