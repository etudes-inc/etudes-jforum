/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/JForumPrivateMessageService.java $ 
 * $Id: JForumPrivateMessageService.java 8540 2014-08-29 22:18:25Z murthyt $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Etudes, Inc. 
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

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface JForumPrivateMessageService
{
	/**
	 * Creates new private message folder in the site for the user
	 * 
	 * @param context The context
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @param folderName	Folder name
	 * 
	 * @return	Newly created private message folder id
	 * 
	 * @throws JForumAccessException	if user has no access to the private message site or context
	 */
	public int createPrivateMessageFolder(String context, String sakaiUserId, String folderName) throws JForumAccessException;
	
	/**
	 * Deletes the private message drafts and it's attachments
	 * 
	 * @param context		The context or site id
	 *  
	 * @param sakaiUserId		User sakai id
	 * 
	 * @param privateMessageDraftIds	Private message id's that are to be deleted
	 * 
	 * @throws JForumAccessException	If user has has no access to the private message site or context
	 */
	public void deleteDraft(String context, String sakaiUserId, List<Integer> privateMessageDraftIds)throws JForumAccessException;
	
	/**
	 * Deletes the private message and it's attachments
	 * 
	 * @param context		The context or site id
	 * 
	 * @param sakaiUserId		User sakai id
	 * 
	 * @param privateMessageIds		Private message id's that are to be deleted
	 * 
	 * @throws JForumAccessException	If user has has no access to the private message site or context 
	 */
	public void deleteMessage(String context, String sakaiUserId, List<Integer> privateMessageIds)throws JForumAccessException;
	
	/**
	 * Deletes the user private message folder
	 * 
	 * @param context	The context
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @param folderId		Folder id
	 * 
	 * @throws JForumAccessException	If user has no access to the site
	 */
	public void deletePrivateMessageFolder(String context, String sakaiUserId, int folderId) throws JForumAccessException;
	
	/**
	 * Gets the private message drafts list by the user in the site
	 * 
	 * @param context	The context
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return The list of user private message drafts in the site
	 */
	public List<PrivateMessage> drafts(String context, String sakaiUserId);
	
	/**
	 * 	Flags the private message to follow up or not
	 * 
	 * @param privateMessage	Private message
	 */
	public void flagToFollowup(PrivateMessage privateMessage);
	
	/**
	 * Gets the user folder messages
	 * 
	 * @param context	Context or site id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @param folderId	Folder id
	 * 
	 * @return	List of user folder messages
	 */
	public List<PrivateMessage> folderMessages(String context, String sakaiUserId, int folderId);
	
	/**
	 * Gets the private message folder
	 * 
	 * @param folderId	Folder id
	 * 
	 * @return	Gets the private message folder or null if there is no private message folder with given id
	 */
	public PrivateMessageFolder getFolder(int folderId);
	
	
	/**
	 * Gets the messages count of the folder
	 * 
	 * @param folderId	Folder id
	 * 
	 * @return	The messages count of the folder
	 */
	public int getFolderMessagesCount(int folderId);
	
	/**
	 * Get the private message
	 * 
	 * @param privateMessageId	The private message id
	 * 
	 * @return	The private message
	 */
	public PrivateMessage getPrivateMessage(int privateMessageId);
	
	/**
	 * Gets the private message attachment
	 * 
	 * @param attachmentId	Attachment id
	 * 
	 * @return	Attachment
	 */
	public Attachment getPrivateMessageAttachment(int attachmentId);
	
	/**
	 * Gets the private message draft
	 * 
	 * @param privateMessageId	The private message draft id
	 * 
	 * @return	The private message draft
	 */
	public PrivateMessage getPrivateMessageDraft(int privateMessageId);
	
	/**
	 * Gets the count of user private messages drafts
	 * 
	 * @param context	The context
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	The count of user private messages drafts
	 */
	public int getUserDraftsCount(String context, String sakaiUserId);
	
	/**
	 * Gets the user private message folders in the site
	 * 
	 * @param context	The context
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	The user private message folders in the site
	 * 
	 * @throws JForumAccessException	if user has no access to the private message site or context
	 */
	public List<PrivateMessageFolder> getUserFolders(String context, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the unread messages count in the user folders
	 * 
	 * @param context	Site id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	Returns the unread messages count in the user folders
	 */
	public Map<Integer, Integer> getUserFoldersUnreadCount(String context, String sakaiUserId);
	
	/**
	 * Gets the unread messages count in the user folder
	 * 
	 * @param context	Site id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @param folderId	Folder id 
	 * 
	 * @return	The unread messages count in the user folder
	 */
	public int getUserFolderUnreadCount(String context, String sakaiUserId, int folderId);
	
	/**
	 * Gets the count of user private messages that are new
	 * 
	 * @param context	The context
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	The count of user private messages that are new
	 */
	public int getUserUnreadCount(String context, String sakaiUserId);
	
	/**
	 * Gets the private messages received by the user in the site
	 * 
	 * @param context		The context
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	The list of private messages received by the user in the site
	 */
	public List<PrivateMessage> inbox(String context, String sakaiUserId);
	
	/**
	 * 08/21/2014 - Added this method to keep intouch iPhone app working same as before
	 * 
	 * Gets the private messages received by the user in the site
	 * 
	 * @param context		The context
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @param includeFolderMessages	true - Folder messages are included
	 * 								false - Folder messages are not included
	 * 
	 * @return	The list of private messages received by the user in the site
	 */
	public List<PrivateMessage> inbox(String context, String sakaiUserId, boolean includeFolderMessages);
	
	/**
	 * Marks user private message read or unread
	 * 
	 * @param messageId		Privare message id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @param readTime		Read time or unread marked time
	 * 
	 * @param isRead		Read or unread
	 */
	public void markMessageRead(int messageId, String sakaiUserId, Date readTime, boolean isRead);
	
	/**
	 * Modifies the private message folder name
	 * 
	 * @param context	The context
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @param folderId		Folder id
	 * 
	 * @param folderName	Folder name
	 * 
	 * @throws JForumAccessException	If user has no access to the site
	 */
	public void modifyPrivateMessageFolder(String context, String sakaiUserId, int folderId, String folderName) throws JForumAccessException;
	
	/**
	 * modify the existing private message draft
	 * 
	 * @param privateMessage	Private message draft
	 * 
	 * @param toSakaiUserIds	to sakai user id's. Should be null for reply messages and for reply private message the draft To Users is already set from the existing message
	 * 
	 * @param bccSakaiUserIds	Bcc users sakai user id's. Only reply messages can have bcc users and same with drafts
	 * 
	 * @throws JForumAccessException	if "from user" or "to user' has no access to the private message site or context
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 * 
	 */
	public void modifyPrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> toSakaiUserIds, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Move inbox and messages in other user folders to another folder
	 *  
	 * @param context	The contest
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @param privateMessageId	Private message id
	 * 
	 * @param moveToFolderId	Move to folder id
	 * 
	 * @throws JForumAccessException	if user has no access to the private message site or context
	 */
	public void moveTofolder(String context, String sakaiUserId, int privateMessageId, int moveToFolderId) throws JForumAccessException;
	
	/**
	 * Creates new instance of Attachment object with AttachmentInfo
	 * 
	 * @param fileName		Attachment info file name
	 * 
	 * @param contentType	Content type
	 * 
	 * @param comments		Comments or description
	 * 
	 * @param fileContent	File content or body
	 * 
	 * @return	The attachment object or null
	 */
	public Attachment newAttachment(String fileName, String contentType, String comments, byte[] fileContent);
	
	/**
	 * Creates new instance of PrivateMessage object from existing private message with id, context, from user, and to user 
	 * 
	 * @param existingPrivateMessageId	Existing private message id
	 * 
	 * @return	The private message object with id, context, from user, and to user if existing or null
	 */
	public PrivateMessage newPrivateMessage(int existingPrivateMessageId);
	
	/**
	 * Creates new private message object
	 * 
	 * @param fromSakaiUserId	From sakai user id
	 * 
	 * @return	The private message object with from user
	 */
	public PrivateMessage newPrivateMessage(String fromSakaiUserId);
	
	/**
	 * Creates new instance of PrivateMessage object
	 * 
	 * @param	fromSakaiUserId		From sakai user id
	 * 
	 * @param	toSakaiUserId		To sakai user id
	 * 
	 * @return	The private message object with from user and to user
	 */
	public PrivateMessage newPrivateMessage(String fromSakaiUserId, String toSakaiUserId);
	
	/**
	 * Creates new instance of PrivateMessage object for private message reply draft from existing private message context, from user, to user, and reply draft from private message id
	 * 
	 * @param existingPrivateMessageId	Existing private message id
	 * 
	 * @return	The private message object with context, from user, to user, and reply draft from private message id
	 */
	public PrivateMessage newPrivateMessageReplyDraft(int existingPrivateMessageId);
	
	/**
	 * Reply to an existing private message
	 * 
	 * @param privateMessage	Private message with id, context, from user, and to user from exiting private message and post information
	 * 
	 * @throws JForumAccessException	if "from user" or "to user' has no access to the private message site or context
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void replyPrivateMessage(PrivateMessage privateMessage) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Reply to an existing private message. Bcc users can added to reply messages
	 * 
	 * @param privateMessage	Private message with id, context, from user, and to user from exiting private message and post information
	 * 
	 * @param bccSakaiUserIds	BCC users sakai user id's
	 * 
	 * @throws JForumAccessException	if "from user" or "to user' has no access to the private message site or context
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void replyPrivateMessage(PrivateMessage privateMessage, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Saves the private message as draft
	 * 
	 * @param privateMessage	Private message
	 * 
	 * @param toSakaiUserIds	To users sakai user id's. Should be null for reply messages and for reply private message the draft To Users is already set from the existing message
	 * 
	 * @param bccSakaiUserIds	Bcc users sakai user id's. Only reply messages can have bcc users and same with drafts
	 * 
	 * @throws JForumAccessException	if "from user" or "to user' has no access to the private message site or context
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void savePrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> toSakaiUserIds, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Sends drafted private message
	 * 
	 * @param privateMessage	Private message
	 *
	 * @param bccSakaiUserIds	Bcc users sakai user id's. Only reply messages can have bcc users and same with drafts
	 * 
	 * @throws JForumAccessException	if "from user" or "to user' has no access to the private message site or context
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void sendDraftedPrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Sends drafted private message to users
	 * 
	 * @param privateMessage	Private message
	 * 
	 * @param toSakaiUserIds	To users sakai user id's
	 * 
	 * @param bccSakaiUserIds	Bcc users sakai user id's. Only reply messages can have bcc users and same with drafts
	 * 
	 * @throws JForumAccessException	if "from user" or "to user' has no access to the private message site or context
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void sendDraftedPrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> toSakaiUserIds, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Sends the private message to one user and supports attachments with no validation for max attachments size and extensions. 
	 * If attachments exceeds max quota allowed they are not uploaded and if attachments have extensions that are not allowed they are not uploaded
	 * 
	 * @param privateMessage	Private message
	 */
	public void sendPrivateMessage(PrivateMessage privateMessage);
	
	/**
	 * Sends the private message to one user and supports attachments with validation for max attachments size and extensions
	 * 
	 * @param privateMessage	Private message
	 * 
	 * @throws JForumAccessException	if "from user" or "to user' has no access to the private message site or context
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void sendPrivateMessageWithAttachments(PrivateMessage privateMessage) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Sends the private message to multiple users and supports attachments with validation for max attachments size and extensions
	 * 
	 * @param privateMessage	Privare message with private message info
	 * 
	 * @param toSakaiUserIds	Sakai user id's to whom the private message is sent
	 * 
	 * @throws JForumAccessException	if "from user" or "To sakai users" has no access to the private message site or context
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void sendPrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> toSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Gets the private messages sent by the user in the site
	 * 
	 * @param context		The context
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	The list of private messages sent by the user in the site
	 */
	public List<PrivateMessage> sentbox(String context, String sakaiUserId);
}
