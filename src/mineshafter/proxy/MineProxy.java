package mineshafter.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import com.sun.net.httpserver.*;

import mineshafter.cache.SkinsResponseCache;
import mineshafter.util.Resources;
import mineshafter.util.Streams;

public class MineProxy
{
	public static float version=0;
	public static int port=0;
	public static int httpport=0;
	public static int httpsport=0;
	public static String authServer=Resources.loadString("servers").split("\n")[0].trim();
	public static String skinServer=Resources.loadString("servers").split("\n")[1].trim();
	public static String cloakServer=Resources.loadString("servers").split("\n")[2].trim();
	public static URLRewriter[] rewriters=null;
	
	public static void listen(final int port,int httpport,int httpsport,float version) throws UnknownHostException, IOException
	{
		listen(port,httpport,httpsport,version,null);
	}
	public static void listen(final int port,int httpport,int httpsport,float version,String authServer) throws UnknownHostException, IOException
	{
		try{
		if(authServer!=null) MineProxy.skinServer=MineProxy.authServer=authServer;
		MineProxy.rewriters=new URLRewriter[]
		{
			new URLRewriter("http://s3\\.amazonaws\\.com/MinecraftSkins/(.+?)\\.png",
					"http://"+MineProxy.skinServer+"/skin/%s.png"),
			new URLRewriter("http://s3\\.amazonaws\\.com/MinecraftCloaks/(.+?)\\.png",
					"http://"+MineProxy.cloakServer+"/cloak/get.jsp?user=%s"),
			new URLRewriter("http://www\\.minecraft\\.net/game/(.*)",
					"http://"+MineProxy.authServer+"/game/%s"),
			new URLRewriter("http://session\\.minecraft\\.net/game/(.*)","http://"+MineProxy.authServer+"/game/%s"),
			new URLRewriter("http://login\\.minecraft\\.net/","http://"+MineProxy.authServer+"/game/getversion.jsp")
		};
		MineProxy.version=version;
		MineProxy.port=port;
		MineProxy.httpport=httpport;
		MineProxy.httpsport=httpsport;
		
		ResponseCache.setDefault(new SkinsResponseCache());
		
		HttpServer server=HttpServer.create(new InetSocketAddress(MineProxy.port),4);
		server.createContext("/",new ProxyHandler());
		server.setExecutor(null);
		server.start();
		
		/*
		ServerSocketFactory ssf=ServerSocketFactory.getDefault();
		final ServerSocket ss=ssf.createServerSocket(port,4);
		Thread wrapperThread=new Thread("Proxy Wrapper Thread"){public void run()
		{
			int p;
			byte[] head=new byte[7];
			Socket s;
			Socket scs;
			while(true)
			{
				try{
				s=ss.accept();
				s.getInputStream().read(head);
				System.out.println("wrapperthread: "+new String(head));
				if(new String(head).equalsIgnoreCase("CONNECT")) p=MineProxy.httpsport;
				else p=MineProxy.httpport;
				scs=new Socket("127.0.0.1",p);
				scs.setSoTimeout(5000);
				scs.getOutputStream().write(head);
				Streams.pipeStreamsActive(s.getInputStream(),scs.getOutputStream());
				Streams.pipeStreamsActive(scs.getInputStream(),s.getOutputStream());
				}catch(IOException e){System.out.println("Wrapper thread died:");e.printStackTrace();}
			}
		}};
		wrapperThread.start();
		*/
		}catch(Exception e){System.out.println("Proxy starting error:");e.printStackTrace();}
	}
	/*
	public static boolean ShouldCache(String url)
	{
		for(Pattern p:MineProxy.cacheOn)
		{
			if(p.matcher(url).matches()) return true;
		}
		return false;
	}
	*/
	public static String RewriteURL(String u)
	{
		try{
		for(URLRewriter ur:MineProxy.rewriters)
		{
			String newurl=ur.MatchAndReplace(u);
			if(newurl!=null)
			{
				u=newurl;
				break;
			}
		}
		//need to find a better way to do this
		if(u.endsWith("/game/getversion.jsp"))
		{
			u+="?proxy="+Float.toString(MineProxy.version);
		}
		}catch(Exception e){System.out.println("URL couldn't be rewritten:");e.printStackTrace();}
		return u;
	}
	
	public static class ProxyHandler implements HttpHandler
	{
		public void handle(HttpExchange t) throws IOException
		{
			String method=t.getRequestMethod();
			String url=this.makeURL(t);
			String murl=RewriteURL(url);
			System.out.println("Request: "+method+" "+url+"\nWill go to "+murl);
			URL urlm=new URL(murl);
			String host=urlm.getHost();
			//String path=urlm.getPath();
			//if(urlm.getQuery()!=null) path+="?"+urlm.getQuery();
			boolean post=method.equalsIgnoreCase("POST");
			
			HttpURLConnection c=(HttpURLConnection)new URL(murl).openConnection(Proxy.NO_PROXY);
			c.setRequestMethod(method);
			if(post) c.setDoOutput(true);
			this.transferHeaders(t,c,host);
			if(post) Streams.pipeStreams(t.getRequestBody(),c.getOutputStream());
			this.transferHeaders(c,t);
			int contentLength=c.getContentLength();
			int responseCode=c.getResponseCode();
			System.out.println("resp: "+responseCode+", len: "+contentLength);
			if(contentLength==-1)
			{
				String encoding=c.getHeaderFields().get("Transfer-Encoding").get(0);
				if(encoding.equalsIgnoreCase("chunked")) contentLength=0;
			}
			OutputStream out=t.getResponseBody();
			t.sendResponseHeaders(responseCode,contentLength);
			if(contentLength>-1)
			{
				System.out.println(out);
				Streams.pipeStreams(c.getInputStream(),out);
			}
			out.flush();
			out.close();
			t.close();
			/*
			if(method.equalsIgnoreCase("CONNECT"))
			{
				Socket s=new Socket("127.0.0.1",MineProxy.httpport);
				s.setSoTimeout(5000);
				Streams.pipeStreamsActive(t.getRequestBody(),s.getOutputStream());
				Streams.pipeStreamsActive(s.getInputStream(),t.getResponseBody());
				//if this doesn't work, wrap a socket around this whole server
				//and send anything that starts with CONNECT to the sslserver
				//send everything else here
			}
			*/
		}
		protected void transferHeaders(HttpExchange t,HttpURLConnection c,String host)
		{
			Headers headers=t.getRequestHeaders();
			for(String h:headers.keySet())
			{
				if(h==null) continue;
				System.out.println("Transferring header: "+h+" = "+headers.getFirst(h));
				if(h.equalsIgnoreCase("host")) c.setRequestProperty(h,host);
				else c.setRequestProperty(h,headers.getFirst(h));
			}
		}
		protected void transferHeaders(HttpURLConnection c,HttpExchange t)
		{
			Map<String,List<String>> serverHeaders=c.getHeaderFields();
			Headers clientHeaders=t.getResponseHeaders();
			for(String h:serverHeaders.keySet())
			{
				if(h==null) continue;
				System.out.println("Transferring header back: "+h+" = "+serverHeaders.get(h).get(0));
				clientHeaders.put(h,serverHeaders.get(h));
			}
		}
		protected String makeURL(HttpExchange t)
		{
			URI uri=t.getRequestURI();
			String host=uri.getHost();
			if(host==null) host=t.getRequestHeaders().getFirst("host");
			String path=uri.getPath();
			if(uri.getQuery()!=null) path+="?"+uri.getQuery();
			//if(uri.getFragment()!=null) path+="#"+uri.getFragment();
			String url=uri.getScheme();
			if(url==null) url="http://";
			else url+="://";
			url+=host+path;
			return url;
		}
	}
}

/*
EVERYONE RELAX!

*dances*
:D|-<
:D/-<
:D|-<
:D\-<

I'm here.
*/