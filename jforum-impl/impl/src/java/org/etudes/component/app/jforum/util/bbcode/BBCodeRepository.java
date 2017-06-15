/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/util/bbcode/BBCodeRepository.java $ 
 * $Id: BBCodeRepository.java 3638 2012-12-02 21:33:06Z ggolden $ 
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
package org.etudes.component.app.jforum.util.bbcode;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BBCodeRepository
{
	private static final Log logger = LogFactory.getLog(BBCodeRepository.class);

	private static Map<String, BBCodeHandler> cache = new HashMap<String, BBCodeHandler>();

	private static final String BBCOLLECTION = "bbCollection";
	
	static
	{
		try
		{
			BBCodeRepository.setBBCollection(new BBCodeHandler().parse());
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error("Error while setting BB collection", e);
			}
		}
	}

	public static void setBBCollection(BBCodeHandler bbCollection)
	{
		cache.put(BBCOLLECTION, bbCollection);
	}

	public static BBCodeHandler getBBCollection()
	{
		try
		{
			if (cache.get(BBCOLLECTION) == null)
			{
				BBCodeRepository.setBBCollection(new BBCodeHandler().parse());
			}
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled()) logger.error("Error while setting " + "BB Collection" + e.toString(), e);
		}

		return (BBCodeHandler) cache.get(BBCOLLECTION);
	}

	public static BBCode findByName(String tagName)
	{
		return getBBCollection().findByName(tagName);
	}
}
