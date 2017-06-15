/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/dao/AttachmentDao.java $ 
 * $Id: AttachmentDao.java 5739 2013-08-27 22:31:41Z murthyt $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013 Etudes, Inc. 
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
package org.etudes.api.app.jforum.dao;

import java.util.List;
import java.util.Map;

import org.etudes.api.app.jforum.Attachment;
import org.etudes.api.app.jforum.AttachmentExtension;

public interface AttachmentDao
{
	/**
	 * Creates a new attachment for post
	 * 
	 * @param attachment	Attachment with AttachmentInfo
	 */
	public void addPostAttachment(Attachment attachment);
	
	/**
	 * Creates a new attachment for the private message
	 * 
	 * @param attachment	Attachment with AttachmentInfo
	 * 
	 * @param pmIds		Attachment private message id's
	 */
	public void addPrivateMessageAttachment(Attachment attachment, List<Integer> pmIds);
	
	/**
	 * Deletes the post Attachment
	 * 
	 * @param postId	Post id
	 * 
	 * @param attachmentId	Attachment id
	 */
	public void deletePostAttachment(int postId, int attachmentId);
	
	/**
	 * Deletes the private message attachment
	 * 
	 * @param privateMessageId private message id for the drafts id is same as private message id
	 * 
	 * @param attachmentId	attachment id
	 */
	public void deletePrivateMessageAttachment(int privateMessageId, int attachmentId);
	
	/**
	 * Selects attachment by id
	 * 
	 * @param attachmentId attachment id
	 * 
	 * @return	Attachment if exists or null
	 */
	public Attachment selectAttachmentById(int attachmentId);
	
	/**
	 * Gets the attachment extensions that are available in the jforum
	 * 
	 * @return	The attachment extensions that are in jforum with extension id as key and attachment extension as value
	 */
	public Map<Integer, AttachmentExtension> selectAttachmentExtensions();
	
	/**
	 * Gets the count of private messages referencing the attachment
	 * 
	 * @param attachId	Attachment id
	 * 
	 * @return	The count of private messages referencing the attachment
	 */
	public int selectAttachmentPrivateMessagesCount(int attachId);
	
	/**
	 * Gets post attachments
	 * 
	 * @param postId	The post id
	 * 
	 * @return	Post attachments
	 */
	public List<Attachment> selectPostAttachments(int postId);
	
	/**
	 * Gets the private message attachments
	 * 
	 * @param privateMessageId	Private message id
	 * 
	 * @return	List of private message attachments
	 */
	public List<Attachment> selectPrivateMessageAttachments(int privateMessageId);
}
