/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/PrivateMessageFolder.java $ 
 * $Id: PrivateMessageFolder.java 8540 2014-08-29 22:18:25Z murthyt $ 
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
package org.etudes.api.app.jforum;

public interface PrivateMessageFolder 
{
	/**
	 * @return the unreadCount
	 */
	public int getUnreadCount();
	
	/**
	 * @return the id
	 */
	int getId();
	
	/**
	 * @return the folder name
	 */
	String getName();
	
	/**
	 * @return the siteId
	 */
	String getSiteId();
	
	/**
	 * @return the userId
	 */
	int getUserId();
}
