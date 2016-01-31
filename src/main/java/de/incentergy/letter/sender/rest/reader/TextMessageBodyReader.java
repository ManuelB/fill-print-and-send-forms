package de.incentergy.letter.sender.rest.reader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
public class TextMessageBodyReader implements MessageBodyReader<Object> {

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return mediaType.getType().equals("text");
	}

	@Override
	public Object readFrom(Class<Object> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		// http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
		try (java.util.Scanner s = new java.util.Scanner(entityStream)) {
			return s.useDelimiter("\\A").hasNext() ? s.next() : "";
		}
	}

}
