package mineshafter;

import java.applet.Applet;
import java.awt.Frame;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import sun.applet.Main;

import mineshafter.proxy.MineProxy;
import mineshafter.util.Resources;
import mineshafter.util.SimpleRequest;
import mineshafter.util.Streams;

public class MineClient extends Applet {
	private static final long serialVersionUID = 1L;
	
	protected static float VERSION = 3.7f;
	
	protected static String launcherDownloadURL = "https://s3.amazonaws.com/MinecraftDownload/launcher/minecraft.jar"; // "http://www.minecraft.net/download/minecraft.jar";
	protected static String normalLauncherFilename = "minecraft.jar";
	protected static String hackedLauncherFilename = "minecraft_modified.jar";
	
	protected static String MANIFEST_TEXT = "Manifest-Version: 1.2\nCreated-By: 1.6.0_22 (Sun Microsystems Inc.)\nMain-Class: net.minecraft.MinecraftLauncher\n";
	
	public void init() {
		MineClient.main(new String[0]);
	}
	
	public static void main(String[] args) {
		try {
			byte[] verdata = SimpleRequest.get("http://mineshafter.appspot.com/update");
			String verstring = new String();
			if(verdata == null) verstring = "0";
			else verstring = new String(verdata);
			if(verstring.isEmpty()) verstring = "0";
			
			float version;
			try {
				version = Float.parseFloat(verstring);
			} catch(Exception e) {
				version = 0;
			}
			System.out.println("Current proxy version: " + VERSION);
			System.out.println("Gotten proxy version: " + version);
			if(VERSION < version) {
				JOptionPane.showMessageDialog(null, "A new version of Mineshafter is available at http://mineshafter.appspot.com/\nGo get it.", "Update Available", JOptionPane.PLAIN_MESSAGE);
				System.exit(0);
			}
		} catch(Exception e) {
			System.out.println("Error while updating:");
			e.printStackTrace();
			// System.exit(1);
		}
		
		try {
			MineProxy proxy = new MineProxy(VERSION);
			proxy.start();
			int proxyPort = proxy.getPort();
			
			System.setErr(System.out);
			
			System.setProperty("http.proxyHost", "127.0.0.1");
			System.setProperty("http.proxyPort", Integer.toString(proxyPort));
			System.setProperty("java.net.preferIPv4Stack", "true");
			System.setProperty("minecraft.applet.WrapperClass", "mineshafter.MineClient");
			
			// Make sure we have a fresh launcher every time
			File hackedFile = new File(hackedLauncherFilename);
			if(hackedFile.exists()) hackedFile.delete();
			
			startLauncher(args);
			
		} catch(Exception e) {
			System.out.println("Something bad happened:");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void startLauncher(String[] args) {
		try {
			if(new File(hackedLauncherFilename).exists()) {
				URL u = new File(hackedLauncherFilename).toURI().toURL();
				URLClassLoader cl = new URLClassLoader(new URL[]{u}, Main.class.getClassLoader());
				
				@SuppressWarnings("unchecked")
				Class<Frame> launcherFrame = (Class<Frame>) cl.loadClass("net.minecraft.LauncherFrame");
				
				String[] nargs = new String[args.length];
				System.arraycopy(args, 0, nargs, 0, args.length); // Transfer the arguments from the process call so that the launcher gets them
				Method main = launcherFrame.getMethod("main", new Class[]{ String[].class });
				main.invoke(launcherFrame, new Object[]{ nargs });
				
			} else if(new File(normalLauncherFilename).exists()) {
				editLauncher();
				startLauncher(args);
				
			} else {
				try{
					byte[] data = SimpleRequest.get(launcherDownloadURL);
					OutputStream out = new FileOutputStream(normalLauncherFilename);
					out.write(data);
					out.flush();
					out.close();
					startLauncher(args);
					
				} catch(Exception ex) {
					System.out.println("Error downloading launcher:");
					ex.printStackTrace();
					return;
				}
			}
		} catch(Exception e1) {
			System.out.println("Error starting launcher:");
			e1.printStackTrace();
		}
	}
	
	public static void editLauncher() {
		try {
			File hackedFile = new File(System.getProperty("java.io.tmpdir"), hackedLauncherFilename);
			System.out.println(hackedFile.toString());
			ZipInputStream in = new ZipInputStream(new FileInputStream(normalLauncherFilename));
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(hackedFile));
			ZipEntry entry;
			String n;
			InputStream dataSource;
			while((entry = in.getNextEntry()) != null) {
				n = entry.getName();
				if(n.contains(".svn")
						|| n.equals("META-INF/MOJANG_C.SF")
						|| n.equals("META-INF/MOJANG_C.DSA")
						|| n.equals("net/minecraft/minecraft.key")
						|| n.equals("net/minecraft/Util$OS.class")) continue;
				
				out.putNextEntry(entry);
				if(n.equals("META-INF/MANIFEST.MF")) dataSource = new ByteArrayInputStream(MANIFEST_TEXT.getBytes());
				else if(n.equals("net/minecraft/Util.class")) dataSource = Resources.load("net/minecraft/Util.class");
				else dataSource = in;
				Streams.pipeStreams(dataSource, out);
				out.flush();
			}
			in.close();
			out.close();
		} catch(Exception e) {
			System.out.println("Editing launcher failed:");
			e.printStackTrace();
		}
	}
}