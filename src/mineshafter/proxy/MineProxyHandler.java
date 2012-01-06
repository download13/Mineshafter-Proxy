package mineshafter.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;

import mineshafter.util.Streams;

public class MineProxyHandler extends Thread {
	private BufferedReader fromClient;
	private DataOutputStream toClient;
	private Socket connection;
	
	private MineProxy proxy;
	
	public MineProxyHandler(MineProxy proxy, Socket conn) throws IOException {
		this.setName("MineProxyHandler Thread");
		
		this.proxy = proxy;
		
		this.connection = conn;
		this.fromClient = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		this.toClient = new DataOutputStream(conn.getOutputStream());
	}
	
	public void run() {
		/*
		 * Get request location
		 * rewrite
		 * check for cloak or skin
		 *   check for cache
		 *     return cache
		 *   else request
		 *     save to cache
		 *     return
		 * check for asset server
		 *   kill connection to get it to not use proxy
		 * check for auth
		 *   get auth answer
		 *   return
		 */
		
		String header = null;
		HashMap<String, String> headers = new HashMap<String, String>();
		String url = null;
		String method = null;
		
		// Read the incoming request for the proxy
		try {
			String[] requestLine = this.fromClient.readLine().split(" ");
			method = requestLine[0].trim().toUpperCase();
			url = requestLine[1].trim();
			
		} catch(IOException e) {
			System.out.println("Unable to read request");
			e.printStackTrace();
			return;
		}
		System.out.println("Request: " + method + " " + url);
		// Read the incoming headers
		//System.out.println("Headers:");
		try {
			do {
				header = this.fromClient.readLine().trim();
				//System.out.println("H: " + header + ", " + header.length());
				int splitPoint = header.indexOf(":");
				if(splitPoint != -1) {
					headers.put(header.substring(0, splitPoint).toLowerCase().trim(), header.substring(splitPoint + 1).trim());
				}
			} while(header.length() > 0);
		} catch(IOException e) {
			System.out.println("Unable to read headers");
			e.printStackTrace();
			return;
		}
		
		Matcher skinMatcher = MineProxy.SKIN_URL.matcher(url);
		Matcher cloakMatcher = MineProxy.CLOAK_URL.matcher(url);
		Matcher getversionMatcher = MineProxy.GETVERSION_URL.matcher(url);
		Matcher joinserverMatcher = MineProxy.JOINSERVER_URL.matcher(url);
		Matcher checkserverMatcher = MineProxy.CHECKSERVER_URL.matcher(url);
		
		byte[] data = null;
		String params;
		
		if(skinMatcher.matches()) {
			System.out.println("Skin");
			
			String username = skinMatcher.group(1);
			if(this.proxy.skinCache.containsKey(username)) { // Is the skin in the cache?
				System.out.println("Skin from cache");
				
				data = this.proxy.skinCache.get(username); // Then get it from there
			} else {
				url = "http://" + MineProxy.authServer + "/skin/" + username + ".png";
				System.out.println("To: " + url);
				
				data = getRequest(url); // Then get it...
				System.out.println("Response length: " + data.length);
				
				this.proxy.skinCache.put(username, data); // And put it in there
			}
			
		} else if(cloakMatcher.matches()) {
			System.out.println("Cloak");
			
			String username = cloakMatcher.group(1);
			if(this.proxy.cloakCache.containsKey(username)) {
				System.out.println("Cloak from cache");
				data = this.proxy.cloakCache.get(username);
			} else {
				url = "http://" + MineProxy.authServer + "/cloak/get.jsp?user=" + username;
				System.out.println("To: " + url);
				
				data = getRequest(url);
				System.out.println("Response length: " + data.length);
				
				this.proxy.cloakCache.put(username, data);
			}
			
		} else if(getversionMatcher.matches()) {
			System.out.println("GetVersion");
			
			url = "http://" + MineProxy.authServer + "/game/getversion.jsp?proxy=" + this.proxy.version;
			System.out.println("To: " + url);
			
			try {
				int postlen = Integer.parseInt(headers.get("content-length"));
				char[] postdata = new char[postlen];
				this.fromClient.read(postdata);
				
				data = postRequest(url, new String(postdata), "application/x-www-form-urlencoded");
				
			} catch(IOException e) {
				System.out.println("Unable to read POST data from getversion request");
				e.printStackTrace();
			}
			
		} else if(joinserverMatcher.matches()) {
			System.out.println("JoinServer");
			
			params = joinserverMatcher.group(1);
			url = "http://" + MineProxy.authServer + "/game/joinserver.jsp" + params;
			System.out.println("To: " + url);
			
			data = getRequest(url);
			
		} else if(checkserverMatcher.matches()) {
			System.out.println("CheckServer");
			
			params = checkserverMatcher.group(1);
			url = "http://" + MineProxy.authServer + "/game/checkserver.jsp" + params;
			System.out.println("To: " + url);
			
			data = getRequest(url);
			
		} else {
			System.out.println("No handler. Piping.");
			
			try {
				if(!url.startsWith("http://") && !url.startsWith("https://")) {
					url = "http://" + url;
				}
				URL u = new URL(url);
				if(method.equals("CONNECT")) {
					int port = u.getPort();
					if(port == -1) port = 80;
					Socket sock = new Socket(u.getHost(), port);
					
					Streams.pipeStreamsActive(sock.getInputStream(), this.toClient);
					Streams.pipeStreamsActive(this.connection.getInputStream(), sock.getOutputStream());
					
				} else if(method.equals("GET")) {
					HttpURLConnection c = (HttpURLConnection) u.openConnection(Proxy.NO_PROXY);
					c.setRequestMethod("GET");
					
					for(String k : headers.keySet()) {
						c.setRequestProperty(k, headers.get(k));
					}
					
					//Collect the headers from the server and retransmit them
					String res = "HTTP/1.0 " + c.getResponseCode() + " " + c.getResponseMessage() + "\r\n";
					res += "Connection: close\r\nProxy-Connection: close\r\n";
					
					java.util.Map<String, java.util.List<String>> h = c.getHeaderFields();
					for(String k : h.keySet()) {
						if(k == null || k.equalsIgnoreCase("Connection") || k.equalsIgnoreCase("Proxy-Connection")) continue;
						java.util.List<String> vals = h.get(k);
						for(String v : vals) {
							res += k + ": " + v + "\r\n";
						}
					}
					res += "\r\n";
					
					// System.out.println(res);
					
					this.toClient.writeBytes(res);
					int size = Streams.pipeStreams(c.getInputStream(), this.toClient);
					
					this.toClient.close();
					this.connection.close();
					
					System.out.println("Piping finished, data size: " + size);
					
				} else if(method.equals("HEAD")) {
					HttpURLConnection c = (HttpURLConnection) u.openConnection(Proxy.NO_PROXY);
					c.setRequestMethod("HEAD");
					
					for(String k : headers.keySet()) {
						c.setRequestProperty(k, headers.get(k));
					}
					
					String res = "HTTP/1.0 " + c.getResponseCode() + " " + c.getResponseMessage() + "\r\n";
					res += "Proxy-Connection: close\r\n";
					
					java.util.Map<String, java.util.List<String>> h = c.getHeaderFields();
					for(String k : h.keySet()) {
						if(k == null) continue;
						java.util.List<String> vals = h.get(k);
						for(String v : vals) {
							res += k + ": " + v + "\r\n";
						}
					}
					res += "\r\n";
					
					//System.out.println(res);
					
					this.toClient.writeBytes(res); // TODO exception socket write error
					this.toClient.close();
					this.connection.close();
					
				} else {
					System.out.println("UNEXPECTED REQUEST TYPE: " + method);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return;
		}
		
		try {
			this.toClient.writeBytes("HTTP/1.0 200 OK\r\nConnection: close\r\nProxy-Connection: close\r\nContent-Length: " + data.length + "\r\n\r\n");
			this.toClient.write(data);
			this.connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] getRequest(String url) {
		try {
			HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
			int code = c.getResponseCode();
			System.out.println("Response: " + code);
			if(code / 100 == 4) {
				return new byte[0];
			}
			BufferedInputStream in = new BufferedInputStream(c.getInputStream());
			
			return grabData(in);
			
		} catch (MalformedURLException e) {
			System.out.println("Bad URL in getRequest:");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO error during a getRequest:");
			e.printStackTrace();
		}
		
		return new byte[0];
	}
	
	public static byte[] postRequest(String url, String postdata, String contentType) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(out);
		//System.out.println("Postdata: " + postdata);
		
		try {
			writer.write(postdata);
			writer.flush();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		byte[] rd = postRequest(url, out.toByteArray(), contentType);
		
		return rd;
	}
	
	public static byte[] postRequest(String url, byte[] postdata, String contentType) {
		try {
			URL u = new URL(url);
			
			HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
			c.setDoOutput(true);
			c.setRequestMethod("POST");
			
			//System.out.println("Postdata_bytes: " + new String(postdata));
			
			c.setRequestProperty("Host", u.getHost());
			c.setRequestProperty("Content-Length", Integer.toString(postdata.length));
			c.setRequestProperty("Content-Type", contentType);
			
			BufferedOutputStream out = new BufferedOutputStream(c.getOutputStream());
			out.write(postdata);
			out.flush();
			out.close();
			
			byte[] data = grabData(new BufferedInputStream(c.getInputStream()));
			return data;
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static byte[] grabData(InputStream in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		
		while(true) {
			int len;
			try {
				len = in.read(buffer);
				if(len == -1) break;
			} catch(IOException e) {
				break;
			}
			out.write(buffer, 0, len);
		}
		
		return out.toByteArray();
	}
}
