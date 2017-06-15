/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/dao/PrivateMessageDao.java $ 
 * $Id: PrivateMessageDao.java 8540 2014-08-29 22:18:25Z murthyt $ 
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
package org.etudes.api.app.jforum.dao;

import java.util.List;
import java.util.Map;

import org.etudes.api.app.jforum.PrivateMessage;
import org.etudes.api.app.jforum.PrivateMessageFolder;


public interface PrivateMessageDao
{
	/**
	 * Adds the user private message folder
	 * 
	 * @param context	Site id
	 * 
	 * @param userId	User id
	 * 
	 * @param folderName	Folder name
	 * 
	 * @return	Newly created user private message folder id
	 */
	public int addPrivateMessageFolder(String context, int userId, String folderName);
	
	/**
	 * Deletes the private message
	 * 
	 * @param privateMessages	Deletes the private messages
	 */
	public void delete(PrivateMessage privateMessage);
	
	/**
	 * Deletes the user folder if there are no messages in the folder
	 * 
	 * @param folderId	Folder id
	 */
	public void deletePrivateMessageFolder(int userId, int folderId);
	
	/**
	 * Gets the user drafts in the site
	 * 
	 * @param context	Context or site id
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	The user drafts in the site
	 */
	public List<PrivateMessage> drafts(String context, String sakaiUserId);
	
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
	 * Gets the user messages in the in-box
	 * 
	 * @param context	Context or site id
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @param includeFolderMessages true - Folder messages included
	 * 								false - Folder messages not included
	 * 
	 * @return	Gets the user messages in the in-box
	 */
	public List<PrivateMessage> inbox(String context, String sakaiUserId, boolean includeFolderMessages);
	
	/**
	 * Move inbox and messages in other user folders to another folder
	 * 
	 * @param context	Site id
	 * 
	 * @param userId	User id
	 * 
	 * @param privateMessageId	Private message id
	 * 
	 * @param moveToFolderId	Move to folder id
	 */
	public void moveTofolder(String context, int userId, int privateMessageId, int moveToFolderId);
	
	/**
	 * Saves the private message draft
	 * 
	 * @param pm	Private message
	 * 
	 * @return	newly created private message draft id
	 */
	public int saveDraft(PrivateMessage pm);
	
	
	/**
	 * Create new private message
	 * 
	 * @param pm	Private message
	 * 
	 * @return newly created private message id's of "from user sent box" and "to user inbox"
	 */
	public List<Integer> saveMessage(PrivateMessage pm);
	
	/**
	 * Gets the private message
	 * 
	 * @param privateMessageId	Private message id
	 * 
	 * @return	Gets the private message or null if there is no private message with given id
	 */
	public PrivateMessage selectById(int privateMessageId);
	
	/**
	 * Gets the private message draft
	 * 
	 * @param privateMessageId	Private message draft id
	 * 
	 * @return	Gets the private message draft or null if there is no private message draft with given id
	 */
	public PrivateMessage selectDraftById(int privateMessageId);
	
	/**
	 * Get the unread count of the user private message drafts
	 * 
	 * @param context	Site id
	 * 
	 * @param userId	User id
	 * 
	 * @return	Returns the user private messages drafts
	 */
	public int selectDraftsCount(String context, int userId);
	
	/**
	 * Select the private message folder
	 * 
	 * @param folderId	Folder id
	 * 
	 * @return	Gets the private message folder or null if there is no private message folder with given id
	 */
	public PrivateMessageFolder selectFolder(int folderId);
	
	/**
	 * Gets the messages count in the folder
	 * 
	 * @param folderId	Folder id
	 * 
	 * @return	The messages count in the folder
	 */
	public int selectFolderMessageCount(int folderId);
	
	/**
	 * Gets the user private message folders in the site
	 * 
	 * @param context	Site id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The user private message folders in the site
	 */
	public List<PrivateMessageFolder> selectFolders(String context, int userId);
	
	/**
	 * Gets the unread messages count in the user folders
	 * 
	 * @param context	Site id
	 * 
	 * @param userId	User id
	 * 
	 * @return	Returns the unread messages count in the user folders
	 */
	public Map<Integer, Integer> selectFoldersUnreadCount(String context, int userId);
	
	/**
	 * Gets the unread messages count in the user folder. If folder id is zero fetch inbox unread count
	 * 
	 * @param context	Site id
	 * 
	 * @param userId	User id
	 * 
	 * @param folderId	Folder id 
	 * 
	 * @return	The unread messages count in the user folder
	 */
	public int selectFolderUnreadCount(String context, int userId, int folderId);
	
	/**
	 * Get the unread count of the user private messages
	 * 
	 * @param context	Site id
	 * 
	 * @param userId	User id
	 * 
	 * @return	Returns the unread private messages count
	 */
	public int selectUnreadCount(String context, int userId);
	
	/**
	 * Gets the user messages in the sent box
	 * 
	 * @param context	Context or site id
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	Gets the user messages in the sent box
	 */
	public List<PrivateMessage> sentbox(String context, String sakaiUserId);
	
	/**
	 * updates the existing private message draft
	 * 
	 * @param pm	Private message draft
	 */
	public void updateDraft(PrivateMessage pm);
	
	/**
	 * Updates follow up flag
	 * 
	 * @param messageId		Message id
	 * 
	 * @param flag	true - to add follow up flag
	 * 				flase - to clear follow up flag
	 */
	public void updateFlagToFollowup(int messageId, boolean flag);
	
	/**
	 * Updates message type
	 * 
	 * @param messageId		Message id
	 * 
	 * @param messageType	Message type
	 */
	public void updateMessageType(int messageId, int messageType);
	
	/**
	 * Updates private message folder name
	 * 
	 * @param userId	User id
	 * 
	 * @param folderId	Folder id
	 * 
	 * @param folderName	Folder name
	 */
	public void updatePrivateMessageFolder(int userId, int folderId, String folderName);
	
	/**
	 * Updates replied status
	 * 
	 * @param pm	Private message
	 */
	public void updateRepliedStatus(PrivateMessage pm);
}