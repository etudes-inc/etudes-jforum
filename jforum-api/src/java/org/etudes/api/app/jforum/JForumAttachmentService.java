/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/JForumAttachmentService.java $ 
 * $Id: JForumAttachmentService.java 5739 2013-08-27 22:31:41Z murthyt $ 
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
package org.etudes.api.app.jforum;

import java.util.List;

public interface JForumAttachmentService
{
	/* Default total attachments quota limit in mega bytes */
	public static final int ATTACHMENTS_DEFAULT_QUOTA_LIMIT = 2;
	public static final String ATTACHMENTS_QUOTA_LIMIT = "etudes.jforum.attachments.quota.limit";
	public static final String ATTACHMENTS_STORE_DIR = "etudes.jforum.attachments.store.dir";
	
	public static final String AVATAR_CLUSTERED = "etudes.jforum.avatar.clustered";
	public static final String AVATAR_CONTEXT = "etudes.jforum.avatar.context";
	public static final String AVATAR_DEFAULT_MAX_HEIGHT = "150";
	public static final String AVATAR_DEFAULT_MAX_WIDTH = "150";
	public static final String AVATAR_MAX_HEIGHT = "etudes.jforum.avatar.maxHeight";
	public static final String AVATAR_MAX_WIDTH = "etudes.jforum.avatar.maxWidth";
	public static final String AVATAR_PATH = "etudes.jforum.avatar.path";	
	
	/**
	 * Deletes post attachments
	 * 
	 * @param post	Post
	 */
	public void deletePostAttachments(Post post);
	
	/**
	 * Deletes private message attachments if not referenced by private messages
	 * 
	 * @param privateMessage	Private message
	 */
	public void deletePrivateMessageAttachments(PrivateMessage privateMessage);
	
	/**
	 * Processes the post attachments in the post edit mode. Removes the attachments if removed from the post and adds new attachments
	 *  
	 * @param post	Post
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void processEditPostAttachments(Post post) throws JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Processes the private draft message attachments. Removes the attachments if removed from the private message draft and adds new attachments
	 * 
	 * @param privateMessage	Private message draft
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void processEditPrivateMessageAttachments(PrivateMessage privateMessage) throws JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Saves post attachments
	 * 
	 * @param post
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void processPostAttachments(Post post) throws JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Saves private message attachments
	 * 
	 * @param privateMessage	private message	
	 * 
	 * @param pmIds 	private message id's of from user sent box and to user inbox private
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void processPrivateMessageAttachments(PrivateMessage privateMessage, List<Integer> pmIds) throws JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Saves private message DRAFT attachments
	 * 
	 * @param privateMessage	private message
	 * 
	 * @param pmDraftId	private message draft id
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void processPrivateMessageDraftAttachments(PrivateMessage privateMessage, Integer pmDraftId) throws JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
		
	/**
	 * Validate edit post attachments
	 * 
	 * @param post	Post
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void validateEditPostAttachments(Post post) throws JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Validate sending drafted private message attachments
	 * 
	 * @param post
	 * 
	 * @throws JForumAttachmentOverQuotaException
	 * 
	 * @throws JForumAttachmentBadExtensionException
	 */
	public void validateSendingPrivateMessageDraftAttachments(Post post) throws JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * checks the post attachments for not allowed extensions and attachments quota limit
	 * 
	 * @param post	Post
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void validatePostAttachments(Post post) throws JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
}
