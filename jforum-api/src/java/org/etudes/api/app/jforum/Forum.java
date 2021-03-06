/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/Forum.java $ 
 * $Id: Forum.java 8009 2014-05-23 20:22:26Z murthyt $ 
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

import java.util.List;
import java.util.Map;

/**
 * Represents a forum
 */
public interface Forum
{
	public enum ForumAccess
	{
		DENY(1), GROUPS(2), SITE(0);
		
		private final int access;
		
		ForumAccess(int access)
		{
			this.access = access;
		}
		
		final public int getAccessType()
		{
			return access;
		}
	}
	
	public enum ForumDisplayStatus
	{
		DENY_ACCESS, DENY_ACCESS_NEW, FOLDER, FOLDER_NEW, GROUP, GROUP_NEW, GROUP_READ_ONLY, GROUP_READ_ONLY_NEW, GROUP_REPLY_ONLY, GROUP_REPLY_ONLY_NEW, LOCKED, LOCKED_NEW, NOT_YET_OPEN, NOT_YET_OPEN_INVISIBLE, NOT_YET_OPEN_NEW, READ_ONLY, READ_ONLY_NEW, REPLY_ONLY, REPLY_ONLY_NEW;
	}
	//public static final int TYPE_NORMAL = 0;
	//public static final int TYPE_REPLY_ONLY = 1;
	//public static final int TYPE_READ_ONLY = 2;
	
	public enum ForumGroupsReadonlyAccessType
	{
		DUEDATE(2), IMMEDIATE(1);
		
		private final int groupsReadonyAccessType;
		
		ForumGroupsReadonlyAccessType(int groupsReadonyAccessType)
		{
			this.groupsReadonyAccessType = groupsReadonyAccessType;
		}
		
		final public int getForumGroupsReadonyAccessType()
		{
			return groupsReadonyAccessType;
		}
	}
	
	public enum ForumTopicOrder
	{
		DUE_DATE(2), OPEN_DATE(1), RECENT(0);
		
		private final int topicOrder;
		
		ForumTopicOrder(int topicOrder)
		{
			this.topicOrder = topicOrder;
		}
		
		final public int getTopicOrder()
		{
			return topicOrder;
		}
	}
	
	public enum ForumType
	{
		NORMAL(0), READ_ONLY(2), REPLY_ONLY(1);
		
		private final int type;
		
		ForumType(int type)
		{
			this.type = type;
		}
		
		final public int getType()
		{
			return type;
		}
	}
	
	public static final int ACCESS_DENY = 1;
	public static final int ACCESS_GROUPS = 2;
	
	// course map uses these. After updating course map and remove the below code
	public static final int ACCESS_SITE = 0;
	
	/**
	 * @return the modifiedBySakaiUserId
	 */
	public String getModifiedBySakaiUserId();
	
	/**
	 * @return the topicOrder
	 */
	public int getTopicOrder();

	/**
	 * @return the allowTopicPostLikes
	 */
	public Boolean isAllowTopicPostLikes();

	/**
	 * @param allowTopicPostLikes the allowTopicPostLikes to set
	 */
	public void setAllowTopicPostLikes(Boolean allowTopicPostLikes);
	
	/**
	 * @param modifiedBySakaiUserId the modifiedBySakaiUserId to set
	 */
	public void setModifiedBySakaiUserId(String modifiedBySakaiUserId);

	/**
	 * @param topicOrder the topicOrder to set
	 */
	public void setTopicOrder(int topicOrder);

	/**
	 * Get the category access dates
	 * 
	 * @return	The category access dates
	 */
	AccessDates getAccessDates();

	/**
	 * Gets the access type
	 * 
	 * @return 	The accessType
	 */
	int getAccessType();

	/**
	 * @return the blocked
	 */
	Boolean getBlocked();

	/**
	 * @return the blockedByDetails
	 */
	String getBlockedByDetails();

	/**
	 * @return the blockedByTitle
	 */
	String getBlockedByTitle();

	/**
	 * @return the category
	 */
	Category getCategory();

	/**
	 * Gets the category id
	 * 
	 * @return 	The categoryId
	 */
	int getCategoryId();
	
	/**
	 * @return the createdBy
	 */
	String getCreatedBySakaiUserId();
	
	/**
	 * Gets the description
	 * 
	 * @return 	The description
	 */
	String getDescription();
	
	/**
	 * @return the evaluations
	 */
	List<Evaluation> getEvaluations();

	/**
	 * Get the evaluation status of the gradable forum. Only facilitator has the access to it.
	 * 
	 * @return The evaluation status of the gradable forum.
	 */
	Evaluation.EvaluationStatus getEvaluationStatus();
	
	/**
	 * @return The grade
	 */
	Grade getGrade();
	
	/**
	 * Gets the grade type
	 * 
	 * @return 	The gradeType
	 */
	int getGradeType();
	
	/**
	 * Get the forum groups
	 * 
	 * @return the groups
	 */
	List<String> getGroups();
	
	/**
	 * @return the groupsOthersAccessType
	 */
	int getGroupsReadonyAccessType();
	
	/**
	 * Gets the id
	 * 
	 * @return	The id
	 */
	int getId();
	
	/**
	 * @return the lastPostId
	 */
	int getLastPostId();
	
	/**
	 * Gets the lastPostInfo
	 * 
	 * @return The lastPostInfo
	 */
	LastPostInfo getLastPostInfo();
	
	/**
	 * Gets the last post info of the forum for the user
	 * 
	 * @param siteId	The site id
	 * 
	 * @param sakaiUserId	The sakai user id
	 * 
	 * @return	The last post info of the forum for the user
	 */
	LastPostInfo getLastPostInfo(String siteId, String sakaiUserId);
	

	/**
	 * Gets the name
	 * 
	 * @return 	The name
	 */
	String getName();
	
	/**
	 * Gets the forum order
	 * 
	 * @return The order
	 */
	int getOrder();

	/**
	 * @return the special access list
	 */
	List<SpecialAccess> getSpecialAccess();
	
	/**
	 * Gets the forum topics
	 * 
	 * @return	The forum topics
	 */
	List<Topic> getTopics();
	
	/**
	 * @return the totalPosts
	 */
	int getTotalPosts();

	/**
	 * @return the totalTopics
	 */
	int getTotalTopics();

	/**
	 * Gets the type
	 * 
	 * @return 	The type
	 */
	int getType();

	/**
	 * Get forum display status for icons for the user
	 * 
	 * @return The forum display status
	 */
	ForumDisplayStatus getUserDiplayStatus();

	/**
	 * Gets the user evaluation for gradable forum. If the user posts are not graded the id is not set.
	 * 
	 * @return	The user evaluation for gradable forum
	 */
	Evaluation getUserEvaluation();
	
	/**
	 * Gets user last poster info if categories or forums are fetched for user
	 * 
	 * @return	The last post info of the forum for the user
	 */
	LastPostInfo getUserLastPostInfo();
	
	/**
	 * Gets the user post count
	 * 
	 * @return the userPostCount
	 */
	Map<String, Integer> getUserPostCount();
	
	/**
	 * @return the groupReadonlyUser
	 */
	boolean isGroupReadonlyUser();

	/**
	 * @return the groupUser
	 */
	boolean isGroupUser();

	/**
	 * @return the unread
	 */
	boolean isUnread();

	/**
	 * Gets user access to post or reply to the topic
	 * 
	 * @return
	 */
	Boolean mayCreateTopic();

	/**
	 * Sets the access dates
	 * 
	 * @param accessDates 	The accessDates to set
	 */
	void setAccessDates(AccessDates accessDates);

	/**
	 * Sets the access type
	 * 
	 * @param accessType 	The accessType to set
	 */
	void setAccessType(int accessType);
	
	/**
	 * Sets the lastPostInfo
	 * 
	 * @param lastPostInfo 
	 * 		The lastPostInfo to set
	 */
	//void setLastPostInfo(LastPostInfo lastPostInfo);
	
	/**
	 * @param blocked the blocked to set
	 */
	void setBlocked(Boolean blocked);
	
	/**
	 * @param blockedByDetails the blockedByDetails to set
	 */
	void setBlockedByDetails(String blockedByDetails);
	
	/**
	 * @param blockedByTitle the blockedByTitle to set
	 */
	void setBlockedByTitle(String blockedByTitle);
	
	/**
	 * Sets the category id
	 * 
	 * @param categoryId 	The categoryId to set
	 */
	void setCategoryId(int categoryId);

	/**
	 * @param createdBySakaiUserId the createdBySakaiUserId to set
	 */
	void setCreatedBySakaiUserId(String createdBySakaiUserId);
	
	/**
	 * Sets the description
	 * 
	 * @param description 	The description to set
	 */
	void setDescription(String description);
	
	/**
	 * @param grade 
	 * 		The grade to set
	 */
	void setGrade(Grade grade);
	
	/**
	 * Sets the grade type
	 * 
	 * @param gradeType 	The gradeType to set
	 */
	void setGradeType(int gradeType);
	
	/**
	 * Set the forum groups
	 * 
	 * @param groups the groups to set
	 */
	void setGroups(List<String> groups);

	/**
	 * @param groupsReadonyAccessType the groupsReadonyAccessType to set
	 */
	void setGroupsReadonyAccessType(int groupsReadonyAccessType);
	
	/**
	 * Sets the name
	 * 
	 * @param name 	The name to set
	 */
	void setName(String name);

	/**
	 * Sets the forum order
	 * 
	 * @param order The order to set
	 */
	void setOrder(int order);
	
	/**
	 * @param totalPosts the totalPosts to set
	 */
	void setTotalPosts(int totalPosts);

	/**
	 * @param totalTopics the totalTopics to set
	 */
	void setTotalTopics(int totalTopics);
	
	/**
	 * Sets the type
	 * 
	 * @param type 	The type to set
	 */
	void setType(int type);
	
	/**
	 * @param unread the unread to set
	 */
	void setUnread(boolean unread);
}
