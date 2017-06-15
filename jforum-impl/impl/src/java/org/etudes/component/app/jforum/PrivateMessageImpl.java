/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/PrivateMessageImpl.java $ 
 * $Id: PrivateMessageImpl.java 8540 2014-08-29 22:18:25Z murthyt $ 
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
package org.etudes.component.app.jforum;

import java.util.ArrayList;
import java.util.List;

import org.etudes.api.app.jforum.Post;
import org.etudes.api.app.jforum.PrivateMessage;
import org.etudes.api.app.jforum.User;


public class PrivateMessageImpl implements PrivateMessage
{
	protected List<Integer> bccUserIds = new ArrayList<Integer>();
	protected List<User> bccUsers = new ArrayList<User>();
	protected String context = null;
	protected List<Integer> draftToUserIds = new ArrayList<Integer>();
	protected boolean flagToFollowup;
	protected int folderId;
	protected User fromUser;
	protected boolean hasAttachments;
	protected int id;
	protected Post post;
	protected int priority;
	protected boolean replied;
	protected int replyDraftFromPrivmsgId;
	
	protected User toUser;

	protected int type;


	public PrivateMessageImpl()
	{		
	}

	protected PrivateMessageImpl(int id, String context, User fromUser, User toUser)
	{
		this.id = id;
		this.context = context;
		this.fromUser = fromUser;
		this.toUser = toUser;
		
		this.setPost(new PostImpl());
	}


	/**
	 * For private message reply draft 
	 * 
	 * @param context	Context
	 * 
	 * @param fromUser	From user
	 * 
	 * @param toUser	To user
	 * 
	 * @param replyDraftFromPrivmsgId	Reply Draft From private message id
	 */
	protected PrivateMessageImpl(String context, User fromUser, User toUser, int replyDraftFromPrivmsgId)
	{
		this.context = context;
		this.fromUser = fromUser;
		this.draftToUserIds.add(toUser.getId());
		this.replyDraftFromPrivmsgId = replyDraftFromPrivmsgId;
		
		this.setPost(new PostImpl());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Integer> getBccUserIds()
	{
		return bccUserIds;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<User> getBccUsers()
	{
		return bccUsers;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContext()
	{
		return this.context;
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public List<Integer> getDraftToUserIds()
	{
		return this.draftToUserIds;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getFolderId()
	{
		return folderId;
	}

	/**
	 * {@inheritDoc}
	 */
	public User getFromUser()
	{
		return fromUser;
	}


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
	public Post getPost()
	{
		return post;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getPriority()
	{
		return priority;
	}
	
	/**
	 * @return the replyDraftFromPrivmsgId
	 */
	public int getReplyDraftFromPrivmsgId()
	{
		return replyDraftFromPrivmsgId;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public User getToUser()
	{
		return toUser;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getType()
	{
		return type;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isFlagToFollowup()
	{
		return flagToFollowup;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isHasAttachments()
	{
		return hasAttachments;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isReplied()
	{
		return replied;
	}
	
	/**
	 * @param bccUserIds the bccUserIds to set
	 */
	public void setBccUserIds(List<Integer> bccUserIds)
	{
		this.bccUserIds = bccUserIds;
	}
	
	/**
	 * @param bccUsers the bccUsers to set
	 */
	public void setBccUsers(List<User> bccUsers)
	{
		this.bccUsers = bccUsers;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setContext(String context)
	{
		this.context = context;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setDraftToUserIds(List<Integer> draftToUserIds)
	{
		this.draftToUserIds = draftToUserIds;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setFlagToFollowup(boolean flagToFollowup)
	{
		this.flagToFollowup = flagToFollowup;
	}
	
	/**
	 * @param folderId the folderId to set
	 */
	public void setFolderId(int folderId)
	{
		this.folderId = folderId;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setFromUser(User fromUser)
	{
		this.fromUser = fromUser;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setHasAttachments(boolean hasAttachments)
	{
		this.hasAttachments = hasAttachments;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setPost(Post post)
	{
		this.post = post;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setPriority(int priority)
	{
		this.priority = priority;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setReplied(boolean replied)
	{
		this.replied = replied;
	}
	
	/**
	 * @param replyDraftFromPrivmsgId the replyDraftFromPrivmsgId to set
	 */
	public void setReplyDraftFromPrivmsgId(int replyDraftFromPrivmsgId)
	{
		this.replyDraftFromPrivmsgId = replyDraftFromPrivmsgId;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setToUser(User toUser)
	{
		this.toUser = toUser;
	}


	/**
	 * {@inheritDoc}
	 */
	public void setType(int type)
	{
		this.type = type;
	}
}
