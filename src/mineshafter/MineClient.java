package mineshafter;

import java.applet.Applet;
import java.awt.Frame;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import mineshafter.proxy.MineProxy;
import mineshafter.util.Resources;
import mineshafter.util.SimpleRequest;
import mineshafter.util.Streams;

public class MineClient extends Applet
{
	private static final long serialVersionUID=1L;
	protected static float VERSION=2.9f;
	protected static int proxyPort=8061;
	protected static int proxyHTTPPort=8062;
	protected static int proxyHTTPSPort=8063;
	protected static String launcherDownloadURL="https://s3.amazonaws.com/MinecraftDownload/launcher/minecraft.jar";//"http://www.minecraft.net/download/minecraft.jar";
	protected static String normalLauncherFilename="Minecraft.jar";
	protected static String hackedLauncherFilename="Minecraft_modified.jar";
	protected static boolean portableMode=false;
	
	public void init()
	{
		MineClient.main(new String[0]);
	}
	
	public static void main(String[] args)
	{
		try{
		String verstring=new String(SimpleRequest.get("http://mineshafter.appspot.com/update?name=client"));
		if(verstring.isEmpty()) verstring="0";
		float version;
		try{version=Float.parseFloat(verstring);}
		catch(Exception e){version=0;}
		System.out.println("Current proxy version: "+VERSION);
		System.out.println("Gotten proxy version: "+version);
		if(VERSION<version)
		{
			JOptionPane.showMessageDialog(null,"A new version of Mineshafter is available at http://mineshafter.appspot.com/\nGo get it.","Update Available",JOptionPane.PLAIN_MESSAGE);
			System.exit(0);
		}
		}catch(Exception e){System.out.println("Error while updating:");e.printStackTrace();System.exit(1);}
		
		try{
		String auth=null;
		for(String arg:args)
		{
			if(arg.startsWith("-"))
			{
				if(arg.equalsIgnoreCase("-p")||arg.equalsIgnoreCase("--portable")) portableMode=true;
			}
			else auth=arg;
		}
		MineProxy.listen(proxyPort,proxyHTTPPort,proxyHTTPSPort,VERSION,auth);
		startLauncher();
		System.setProperty("http.proxyHost","127.0.0.1");
		System.setProperty("http.proxyPort",Integer.toString(proxyPort));
		System.setProperty("https.proxyHost","127.0.0.1");
		System.setProperty("https.proxyPort",Integer.toString(proxyPort));
		}catch(Exception e){System.out.println("Something bad happened:");e.printStackTrace();System.exit(1);}
	}
	
	public static void startLauncher()
	{
		try{
		if(new File(hackedLauncherFilename).exists())
		{
			URL u=new File(hackedLauncherFilename).toURI().toURL();
			URLClassLoader cl=new URLClassLoader(new URL[]{u});
			@SuppressWarnings("unchecked")
			Class<Frame> launcherFrame=(Class<Frame>)cl.loadClass("net.minecraft.LauncherFrame");
			Class<?> utilClass=cl.loadClass("net.minecraft.Util");
			Field portable=utilClass.getDeclaredField("portable");
			portable.setBoolean(null,portableMode);
			Method main=launcherFrame.getMethod("main",new Class[]{String[].class});
			main.invoke(launcherFrame,new Object[]{new String[0]});
		}
		else if(new File(normalLauncherFilename).exists())
		{
			editLauncher();
			startLauncher();
		}
		else
		{
			try{
			InputStream in=new URL(launcherDownloadURL).openStream();
			OutputStream out=new FileOutputStream(normalLauncherFilename);
			Streams.pipeStreams(in,out);
			in.close();
			out.close();
			startLauncher();
			}catch(Exception ex){System.out.println("Error downloading launcher:");ex.printStackTrace();return;}
		}
		}catch(Exception e1){System.out.println("Error starting launcher:");e1.printStackTrace();}
	}
	
	public static void editLauncher()
	{
		try{
		ZipInputStream in=new ZipInputStream(new FileInputStream(normalLauncherFilename));
		ZipOutputStream out=new ZipOutputStream(new FileOutputStream(hackedLauncherFilename));
		ZipEntry entry;
		String n;
		InputStream dataSource;
		while((entry=in.getNextEntry())!=null)
		{
			n=entry.getName();
			if(n.contains(".svn")||n.equals("META-INF/MOJANG_C.SF")||n.equals("META-INF/MOJANG_C.DSA")||n.equals("net/minecraft/minecraft.key")||n.equals("net/minecraft/Util$OS.class")) continue;
			out.putNextEntry(entry);
			if(n.equals("META-INF/MANIFEST.MF")) dataSource=Resources.load("manifest.txt");
			else if(n.equals("net/minecraft/Util.class")) dataSource=Resources.load("Util.class");
			else dataSource=in;
			Streams.pipeStreams(dataSource,out);
			out.flush();
		}
		/*out.putNextEntry(new ZipEntry("net/minecraft/AllowAll.class"));
		dataSource=Resources.load("AllowAll.class");
		Streams.pipeStreams(dataSource,out);*/
		in.close();
		out.close();
		}catch(Exception e){System.out.println("Editing launcher failed:");e.printStackTrace();}
	}
}