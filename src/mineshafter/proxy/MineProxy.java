package mineshafter.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.regex.Pattern;


public class MineProxy extends Thread {
	public static String authServer = "mineshafter.appspot.com";
	
	public static Pattern SKIN_URL = Pattern.compile("http://s3\\.amazonaws\\.com/MinecraftSkins/(.+?)\\.png");
	public static Pattern CLOAK_URL = Pattern.compile("http://s3\\.amazonaws\\.com/MinecraftCloaks/(.+?)\\.png");
	public static Pattern GETVERSION_URL = Pattern.compile("http://session\\.minecraft\\.net/game/getversion\\.jsp");
	public static Pattern JOINSERVER_URL = Pattern.compile("http://session\\.minecraft\\.net/game/joinserver\\.jsp(.*)");
	public static Pattern CHECKSERVER_URL = Pattern.compile("http://session\\.minecraft\\.net/game/checkserver\\.jsp(.*)");
	// public static Pattern LOGIN_URL = Pattern.compile("login\\.minecraft\\.net/");
	
	public float version = 0;
	
	public Hashtable<String, byte[]> skinCache;
	public Hashtable<String, byte[]> cloakCache;
	
	private int port = 0;
	
	public MineProxy(final int port, float version) {
		this.setName("MineProxy Thread");
		
		try {
			this.version = version;
			this.port = port;
			
			this.skinCache = new Hashtable<String, byte[]>();
			this.cloakCache = new Hashtable<String, byte[]>();
		} catch(Exception e) {
			System.out.println("Proxy starting error:");
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			ServerSocket server = new ServerSocket(this.port);
			
			while(true) {
				Socket connection = server.accept();
				
				MineProxyHandler handler = new MineProxyHandler(this, connection);
				handler.start();
			}
		} catch(IOException e) {
			System.out.println("Error in server accept loop:");
			e.printStackTrace();
		}
	}
	
}
