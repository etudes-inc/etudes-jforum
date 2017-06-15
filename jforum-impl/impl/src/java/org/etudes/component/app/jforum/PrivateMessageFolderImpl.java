/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/PrivateMessageFolderImpl.java $ 
 * $Id: PrivateMessageFolderImpl.java 8540 2014-08-29 22:18:25Z murthyt $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2014 Etudes, Inc. 
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
package org.etudes.component.app.jforum;

import org.etudes.api.app.jforum.PrivateMessageFolder;

public class PrivateMessageFolderImpl implements PrivateMessageFolder
{
	protected int id;
	protected String name;
	protected String siteId;
	protected int unreadCount;
	protected int userId;
	
	/**
	 * {@inheritDoc}
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSiteId()
	{
		return siteId;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getUnreadCount()
	{
		return unreadCount;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getUserId()
	{
		return userId;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setFolderName(String name)
	{
		this.name = name;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * @param siteId the siteId to set
	 */
	public void setSiteId(String siteId)
	{
		this.siteId = siteId;
	}
	
	/**
	 * @param unreadCount the unreadCount to set
	 */
	public void setUnreadCount(int unreadCount)
	{
		this.unreadCount = unreadCount;
	}
	
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(int userId)
	{
		this.userId = userId;
	}
}
