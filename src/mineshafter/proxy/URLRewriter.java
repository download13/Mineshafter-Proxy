package mineshafter.proxy;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
//import java.text.MessageFormat;

public class URLRewriter
{
	public Pattern matcher;
	public String replacer;
	
	public URLRewriter(String matcher,String replacer)
	{
		this.matcher=Pattern.compile(matcher);
		this.replacer=replacer;
	}
	public String MatchAndReplace(String url)
	{
		Matcher m=this.matcher.matcher(url);
		if(m.matches())
		{
			int c=m.groupCount();
			String[] strings=new String[c];
			for(int i=0;i<c;i++) strings[i]=m.group(i+1);
			String r=String.format(replacer,(Object[])strings);
			return r;
		}
		else return null;
	}
}