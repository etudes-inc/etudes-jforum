/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/Attachment.java $ 
 * $Id: Attachment.java 3638 2012-12-02 21:33:06Z ggolden $ 
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
package org.etudes.api.app.jforum;


public interface Attachment
{
	/**
	 * @return the id
	 */
	public int getId();
	
	/**
	 * @return the info
	 */
	public AttachmentInfo getInfo();
	
	/**
	 * @return the postId
	 */
	public int getPostId();
	
	/**
	 * @return the privmsgsId
	 */
	public int getPrivmsgsId();
	
	/**
	 * @return the userId
	 */
	public int getUserId();
	
	/**
	 * @param info the info to set
	 */
	public void setInfo(AttachmentInfo info);
	
	/**
	 * @param postId the postId to set
	 */
	public void setPostId(int postId);
	
	/**
	 * @param privmsgsId the privmsgsId to set
	 */
	public void setPrivmsgsId(int privmsgsId);
	
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(int userId);
}
