/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/PrivateMessage.java $ 
 * $Id: PrivateMessage.java 8540 2014-08-29 22:18:25Z murthyt $ 
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

public interface PrivateMessage
{
	public enum PrivateMessagePriority
	{
		GENERAL(0), HIGH(1);
		
		private final int priority;
		
		PrivateMessagePriority(int priority)
		{
			this.priority = priority;
		}
		
		final public int getPriority()
		{
			return priority;
		}
	}
	public enum PrivateMessageType
	{
		DRAFT(6), DRAFT_REPLY(7), NEW(1), READ(0), SAVED_IN(3), SAVED_OUT(4), SENT(2), UNREAD(5);
		
		private final int type;
		
		PrivateMessageType(int type)
		{
			this.type = type;
		}
		
		final public int getType()
		{
			return type;
		}
	}
	public static final String MAIL_NEW_PM_SUBJECT = "Private Message";
	public static final int PRIORITY_GENERAL = 0;
	public static final int PRIORITY_HIGH = 1;
	
	// TODO remove after updating references to enum PrivateMessageType
	public static final int TYPE_DRAFT = 6;
	public static final int TYPE_DRAFT_REPLY = 7;
	public static final int TYPE_NEW = 1;
	public static final int TYPE_READ = 0;
	public static final int TYPE_SAVED_IN = 3;
	
	public static final int TYPE_SAVED_OUT = 4;
	
	public static final int TYPE_SENT = 2;
	
	public static final int TYPE_UNREAD = 5;
	
	/**
	 * @return the bccUserIds
	 */
	public List<Integer> getBccUserIds();
	
	/**
	 * @param id the id to set
	 */
	//public void setId(int id);
	
	/**
	 * @return the bccUsers
	 */
	public List<User> getBccUsers();
	
	/**
	 * @return the draftToUserIds
	 */
	public List<Integer> getDraftToUserIds();
	
	/**
	 * @return the folderId
	 */
	public int getFolderId();
	
	/**
	 * @return the fromUser
	 */
	public User getFromUser();
	
	/**
	 * @return the id
	 */
	public int getId();
	
	/**
	 * @return the post
	 */
	public Post getPost();
	
	/**
	 * @return the priority
	 */
	public int getPriority();
	
	/**
	 * @return the toUser
	 */
	public User getToUser();
	
	/**
	 * @return the type
	 */
	public int getType();
	
	/**
	 * @return the flagToFollowup
	 */
	public boolean isFlagToFollowup();
	
	/**
	 * @return the hasAttachments
	 */
	public boolean isHasAttachments();
	
	/**
	 * @return the replied
	 */
	public boolean isReplied();
	
	/**
	 * @param flagToFollowup the flagToFollowup to set
	 */
	public void setFlagToFollowup(boolean flagToFollowup);
	
	/**
	 * @param fromUser the fromUser to set
	 */
	public void setFromUser(User fromUser);
	
	/**
	 * @param hasAttachments the hasAttachments to set
	 */
	public void setHasAttachments(boolean hasAttachments);
	
	/**
	 * @param post the post to set
	 */
	public void setPost(Post post);
	
	/**
	 * @param priority the priority to set
	 */
	public void setPriority(int priority);
	
	/**
	 * @param replied the replied to set
	 */
	public void setReplied(boolean replied);
	
	/**
	 * @param toUser the toUser to set
	 */
	public void setToUser(User toUser);
	
	/**
	 * @param type the type to set
	 */
	public void setType(int type);
	
	/**
	 * Gets the category context
	 * 
	 * @return	The category's context
	 */
	String getContext();
	
	/**
	 * Sets the context
	 * 
	 * @param context	The context to set
	 */
	void setContext(String context);
}
