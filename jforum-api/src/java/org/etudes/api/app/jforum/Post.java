/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/Post.java $ 
 * $Id: Post.java 7799 2014-04-14 23:41:28Z murthyt $ 
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

public interface Post
{

	/**
	 * @return the attachments
	 */
	public List<Attachment> getAttachments();

	/**
	 * @return the postLikesCount
	 */
	public int getPostLikesCount();

	/**
	 * @return the userPostLike
	 */
	public Boolean isUserPostLike();

	/**
	 * @param attachments the attachments to set
	 */
	public void setAttachments(List<Attachment> attachments);

	/**
	 * @return the editCount
	 */
	int getEditCount();

	/**
	 * @return the editTime
	 */
	Date getEditTime();

	/**
	 * @return the forumId
	 */
	int getForumId();

	/**
	 * @return the id
	 */
	int getId();

	/**
	 * @return the postedBy
	 */
	User getPostedBy();

	/**
	 * @return the text as saved
	 */
	String getRawText();

	/**
	 * @return the subject
	 */
	String getSubject();
	
	/**
	 * @return the text with formatting
	 */
	String getText();
	
	/**
	 * @return the time
	 */
	Date getTime();

	/**
	 * @return the topic
	 */
	Topic getTopic();

	/**
	 * @return the topicId
	 */
	int getTopicId();

	/**
	 * @return the userId
	 */
	int getUserId();

	/**
	 * @return the userIp
	 */
	String getUserIp();
	/**
	 * @return the userLatestPostTime
	 */
	Date getUserLatestPostTime();
	
	/**
	 * @return the hasAttachments
	 */
	Boolean hasAttachments();

	/**
	 * @return the bbCodeEnabled
	 */
	Boolean isBbCodeEnabled();
	
	/**
	 * @return the canEdit
	 */
	Boolean isCanEdit();
	
	/**
	 * @return the htmlEnabled
	 */
	@Deprecated
	Boolean isHtmlEnabled();

	/**
	 * @return the signatureEnabled
	 */
	Boolean isSignatureEnabled();

	/**
	 * @param canEdit the canEdit to set
	 *//*
	void setCanEdit(Boolean canEdit);*/
	
	/**
	 * @return the smiliesEnabled
	 */
	Boolean isSmiliesEnabled();

	/**
	 * @param bbCodeEnabled the bbCodeEnabled to set
	 */
	void setBbCodeEnabled(Boolean bbCodeEnabled);

	/**
	 * @param forumId the forumId to set
	 *//*
	void setForumId(int forumId);*/

	/**
	 * @param editCount the editCount to set
	 */
	void setEditCount(int editCount);

	/**
	 * @param editTime the editTime to set
	 */
	void setEditTime(Date editTime);

	/**
	 * @param hasAttachments the hasAttachments to set
	 */
	void setHasAttachments(Boolean hasAttachments);

	/**
	 * @param htmlEnabled the htmlEnabled to set
	 */
	@Deprecated
	void setHtmlEnabled(Boolean htmlEnabled);

	/**
	 * @param postedBy the postedBy to set
	 */
	void setPostedBy(User postedBy);

	/**
	 * @param signatureEnabled the signatureEnabled to set
	 */
	void setSignatureEnabled(Boolean signatureEnabled);

	/**
	 * @param smiliesEnabled the smiliesEnabled to set
	 */
	void setSmiliesEnabled(Boolean smiliesEnabled);

	/**
	 * @param subject the subject to set
	 */
	void setSubject(String subject);

	/**
	 * @param topicId the topicId to set
	 *//*
	void setTopicId(int topicId);*/
	
	/**
	 * @param text the text to set
	 */
	void setText(String text);
	
	/**
	 * @param time the time to set
	 */
	void setTime(Date time);
	
	/**
	 * @param userId the userId to set
	 */
	void setUserId(int userId);
	
	/**
	 * @param userIp the userIp to set
	 */
	void setUserIp(String userIp);
	
	/**
	 * @param userLatestPostTime the userLatestPostTime to set
	 */
	void setUserLatestPostTime(Date userLatestPostTime);
}