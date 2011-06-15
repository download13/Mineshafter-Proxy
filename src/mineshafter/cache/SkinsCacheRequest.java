package mineshafter.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SkinsCacheRequest extends java.net.CacheRequest
{
	final ByteArrayOutputStream body=new ByteArrayOutputStream();
	
	public OutputStream getBody() throws IOException
	{
		return body;
	}
	
	public void abort(){}
}
