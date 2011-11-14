package net.minecraft;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;

import org.omg.CORBA_2_3.portable.OutputStream;

public class Util
{
	private static File workDir=null;
	public static boolean portable=false;
	
	public static File getWorkingDirectory()
	{
		if(workDir==null)
		{
			if(portable) workDir=new File("minecraft.files");
			else workDir=getWorkingDirectory("minecraft");
		}
		return workDir;
	}
	public static File getWorkingDirectory(String applicationName)
	{
		String userHome=System.getProperty("user.home",".");
		File workingDirectory;
		switch(getPlatform())
		{
		case 2:
			workingDirectory=new File(userHome,"."+applicationName+"/");
			break;
		case 0:
			String applicationData=System.getenv("APPDATA");
			if(applicationData!=null) workingDirectory=new File(applicationData,"."+applicationName+"/");
			else workingDirectory=new File(userHome,"."+applicationName+"/");
			break;
		case 1:
			workingDirectory=new File(userHome,"Library/Application Support/"+applicationName);
			break;
		default:
			workingDirectory=new File(userHome,applicationName+"/");
		}
		if((!workingDirectory.exists())&&(!workingDirectory.mkdirs())) throw new RuntimeException("The working directory could not be created: "+workingDirectory);
		return workingDirectory;
	}

	private static int getPlatform()
	{
		String os=System.getProperty("os.name").toLowerCase();
		if(os.contains("win")) return 0;
		if(os.contains("mac")) return 1;
		if(os.contains("solaris")||os.contains("sunos")||os.contains("linux")||os.contains("unix")) return 2;
		return -1;
	}

	public static String excutePost(String targetURL,String urlParameters) throws IOException
	{
		HttpURLConnection c=null;
		if(targetURL.startsWith("https://login.minecraft.net")) targetURL="http://mineshafter.appspot.com/game/getversion.jsp";
		System.out.println("executePost: "+targetURL);
		try{
		byte[] params=urlParameters.getBytes();
		URL u=new URL(targetURL);
		c=(HttpURLConnection)u.openConnection();
		c.setRequestMethod("POST");
		c.setDoOutput(true);
		c.setDoInput(true);
		c.setRequestProperty("Host",u.getHost()); //does this get the original url host, or the redirect one?
		c.setRequestProperty("Content-Length",Integer.toString(params.length));
		c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		BufferedOutputStream tos=new BufferedOutputStream(c.getOutputStream());
		tos.write(params);
		tos.flush();
		tos.close();
		InputStream in=c.getInputStream();
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		byte[] b=new byte[4096];
		int read;
		while(true)
		{
			try{
			read=in.read(b);
			if(read==-1) break;
			}catch(IOException e){break;}
			out.write(b,0,read);
		}
		out.flush();
		return new String(out.toByteArray());
		}catch(FileNotFoundException e)
		{
			String errstr;
			InputStream es=c.getErrorStream();
			if(es==null) errstr="null";
			else
			{
				byte[] eb=new byte[es.available()];
				es.read(eb);
				errstr=new String(eb);
			}
			System.out.println("executePost failed: server sent 404\n"+errstr);
			return null;
		}
		catch(Exception e){System.out.println("executePost failed:");e.printStackTrace();return null;}
	}
}