package mineshafter.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class SkinsCacheResponse extends java.net.CacheResponse
{
	Map<String,List<String>> headers;
	ByteArrayOutputStream body;
	
	public SkinsCacheResponse(Map<String,List<String>> headers,OutputStream body)
	{
		this.headers=headers;
		this.body=(ByteArrayOutputStream)body;
	}
	
	public InputStream getBody() throws IOException
	{
		return new ByteArrayInputStream(this.body.toByteArray());
	}
	
	public Map<String,List<String>> getHeaders() throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}
}
