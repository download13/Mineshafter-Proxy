package net.minecraft;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class AllowAll implements HostnameVerifier
{
	public boolean verify(String hostname,SSLSession session)
	{
		return true;
	}
}