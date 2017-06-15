/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/util/MD5.java $ 
 * $Id: MD5.java 3638 2012-12-02 21:33:06Z ggolden $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010, 2011, 2012 Etudes, Inc. 
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5
{
	/**
	 * Encodes a string
	 * 
	 * @param str
	 *        String to encode
	 *        
	 * @return Encoded String
	 * 
	 * @throws Exception 
	 */
	public static String crypt(String str) throws Exception
	{
		if (str == null || str.length() == 0)
		{
			throw new IllegalArgumentException("String to encript cannot be null or zero length");
		}

		StringBuffer hexString = new StringBuffer();

		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			byte[] hash = md.digest();

			for (int i = 0; i < hash.length; i++)
			{
				if ((0xff & hash[i]) < 0x10)
				{
					hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
				}
				else
				{
					hexString.append(Integer.toHexString(0xFF & hash[i]));
				}
			}
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new Exception("" + e);
		}

		return hexString.toString();
	}
}
