package mineshafter.cache;

import java.io.IOException;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkinsResponseCache extends ResponseCache
{
	private static Pattern[] cacheOn=new Pattern[]
	{
		Pattern.compile("http://s3.amazonaws.com/MinecraftSkins/.*"),
		Pattern.compile("http://s3.amazonaws.com/MinecraftCloaks/.*"),
		Pattern.compile("http://mineshafter\\.appspot.com/skin/.*"),
		Pattern.compile("http://mineshafter\\.appspot.com/cloak/.*")
	};
	private Map<URI,CacheResponse> cache=new HashMap<URI,CacheResponse>();
	
	public CacheResponse get(URI uri,String rqstMethod,Map<String,List<String>> rqstHeaders) throws IOException
	{
		if(!rqstMethod.equalsIgnoreCase("GET")) return null;
		CacheResponse r=this.cache.get(uri);
		if(r!=null) System.out.println("Got from cache: "+uri.toString());
		return r;
	}

	public CacheRequest put(URI uri,URLConnection conn)
	{
		try{
		uri=conn.getURL().toURI();
		for(Pattern p:cacheOn)
		{
			Matcher m=p.matcher(uri.toString());
			if(m.matches())
			{
				CacheRequest req=(CacheRequest)new SkinsCacheRequest();
				Map<String,List<String>> headers=conn.getHeaderFields();
				CacheResponse resp=(CacheResponse)new SkinsCacheResponse(headers,req.getBody());
				this.cache.put(uri,resp);
				System.out.println("Put in cache: "+uri.toString());
				return req;
			}
		}
		return null;
		}catch(Exception e){System.out.println("Skin cache put failed:");e.printStackTrace();return null;}
	}
}
