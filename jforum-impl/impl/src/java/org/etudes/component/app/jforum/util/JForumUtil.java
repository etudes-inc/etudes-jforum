/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/util/JForumUtil.java $ 
 * $Id: JForumUtil.java 12733 2016-07-21 21:53:34Z murthyt $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Etudes, Inc. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 ***********************************************************************************/
package org.etudes.component.app.jforum.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JForumUtil
{
	/**
	 * If there is no text between script tags the script tag is removed else replaces the characters < and > with &lt; and &gt; associated with text "script"
	 * 
	 * @param contents
	 *        string that is to be modified
	 *        
	 * @return modified string
	 */
	public static String escapeScriptTag(String contents)
	{
		if (contents == null) return null;
		
		return contents.replaceAll("(?i)<(/?script[^>]*)>", "&lt;$1&gt;");
	}

	/**
	 * Replace script tags and text between them with empty string. If the text is empty after removing script tags "script script" is returned
	 * 
	 * @param contents
	 *        string that is to be modified
	 *        
	 * @return modified string
	 */
	public static String removeScriptTags(String contents)
	{
		if (contents == null) return null;

		contents = contents.replaceAll("(?i)(<script(.*?)>(.*?)</script>)", "");
		
		if (contents.trim().length() == 0)
		{
			contents = "script script";
		}
		
		return contents;
	}
	
	/**
	 * Strips body tags
	 * 
	 * @param 	data	The source with HTML content
	 * 
	 * @return	Cleaned source with out body tags
	 */
	public static String stripBodyTags(String data)
	{
		if (data == null) return data;

		// pattern to find body tag
		//Pattern p = Pattern.compile("(?:<body[^>]*>)(.*)</body>", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		Pattern p = Pattern.compile("(?:<\\s*body[^>]*>)(.*)<\\s*/body\\s*>", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE | Pattern.DOTALL);
		
		Matcher m = p.matcher(data);
		StringBuffer sb = new StringBuffer();
		
		while (m.find())
		{
			String text = m.group(1);
			// process text
			m.appendReplacement(sb, Matcher.quoteReplacement(text));			
		}

		m.appendTail(sb);

		return sb.toString();
	}
	
	/**
	 * This is from mneme Convert the points / score value into a double, without picking up float to double conversion junk
	 * 
	 * @param score
	 *        The value to convert.
	 * @return The converted value.
	 */
	static public double toDoubleScore(Float score)
	{
		if (score == null) return 0.0d;

		// we want only .xx precision, and we don't want any double junk from the float to double conversion
		float times100 = score.floatValue() * 100f;

		Double rv = (double) times100;

		rv = rv / 100d;
		return rv;
	}
	
	private JForumUtil()
	{
	}
}
