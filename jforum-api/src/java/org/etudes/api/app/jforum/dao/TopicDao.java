/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/dao/TopicDao.java $ 
 * $Id: TopicDao.java 8540 2014-08-29 22:18:25Z murthyt $ 
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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.etudes.api.app.jforum.LastPostInfo;
import org.etudes.api.app.jforum.Post;
import org.etudes.api.app.jforum.Topic;
import org.etudes.api.app.jforum.User;

public interface TopicDao
{
	/**
	 * Creates topic
	 * 
	 * @param topic	Topic object with post data
	 * 
	 * @return	The newly created topic id
	 */
	public int addNew(Topic topic);
	
	/**
	 * Creates new post for the topic
	 * 
	 * @param topic	Topic object with post data
	 * 
	 * @return	The newly created post id
	 */
	public int addNewTopicPost(Topic topic);
	
	/**
	 * Adds user post like if not already added
	 * 
	 * @param postId	Post id
	 * 
	 * @param userId	User id
	 * 
	 * @return true - if user post like is added
	 * 		   false - if user post like is not added
	 */
	public boolean addUserPostLike(int postId, int userId);
	
	/**
	 * Decrements the topic replies count
	 * 
	 * @param topicId	Topic id
	 */
	public void decrementTotalReplies(int topicId);
	
	/**
	 * Deletes topic and it's messages
	 * 
	 * @param topicId	Topic id
	 */
	public void deleteTopic(int topicId);
	
	/**
	 * Deletes topic post
	 * 
	 * @param postId	Post id
	 */
	public void deleteTopicPost(int postId);
	
	/**
	 * Increments the topic replies count
	 * 
	 * @param topicId	Topic id
	 */
	public void incrementTotalReplies(int topicId);
	
	/**
	 * Return the subscription status of the user on the topic.
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The subscrition status of the user on the topic.
	 */
	public boolean isUserSubscribed(int topicId, int userId);
	
	/**
	 * Lock or unlock a topic.
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param status	Lock or unlock status
	 */
	public void lockUnlock(int topicId, Topic.TopicStatus status);
	
	/**
	 * Moves topic to another forum
	 * 
	 * @param topicId	Topic id of the topic that is to be moved
	 * 
	 * @param toForumid	Forum Id
	 */
	public void moveTopic(int topicId, int toForumid);
	
	/**
	 * Gets the topic watch users list and updates the read flag so that the users will receive next 
	 * email notification after reading the topic
	 * 
	 * @param topicId		Topic id
	 * 
	 * @param posterId		Poster id - notification is not sent to poster
	 * 
	 * @return	The list of user who are watching the topic
	 */
	public List<User> notifyUsers(int topicId, int posterId);
	
	/**
	 * Removes all the forum subscriptions
	 * 
	 * @param forumId	Forum id
	 */
	public void removeForumSubscriptions(int forumId);
	
	/**
	 * Clean all subscriptions of the topic
	 * 
	 * @param topicId	Topic id
	 */
	public void removeSubscription(int topicId);
	
	/*
	 * This is not needed as user forum subscriptions can be deleted only if user is unsubscribed to forum and can be done using removeUserForumSubscriptions(int userId, int forumId);
	 * 
	 * public void removeUserForumSubscription(int topicId, int userId, int forumId);
	 */
	
	/**
	 * Remove the user's forum subscriptions
	 * 
	 * @param userId	User id
	 * 
	 * @param forumId	Forum id
	 */
	public void removeUserForumSubscriptions(int userId, int forumId);
	
	/**
	 * Removes user post like
	 * 
	 * @param postId	Post id
	 * 
	 * @param userId	User id
	 * 
	 * @return	true - if user post like is deleted
	 * 		   	false - if user post like is not deleted
	 */
	public boolean removeUserPostLike(int postId, int userId);
	
	/**
	 * Remove the user's subscription of the topic
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param userId	User id
	 */
	public void removeUserSubscription(int topicId, int userId);
	
	/**
	 * Select topic by id
	 * 
	 * @param topicId	Topic id
	 * 
	 * @return	Existing topic or null
	 */
	public Topic selectById(int topicId);
	
	/**
	 * selects the export or reuse and gradable topics of the forum
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return	The list of export or reuse and gradable topics of the forum
	 */
	public List<Topic> selectForumExportAndGradableTopics(int forumId);
	
	/**
	 * Gets the count of reusable and gradable topics of the forum
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return	The count of reusable and gradable topics of the forum
	 */
	public int selectForumExportAndGradableTopicsCount(int forumId);
	
	/**
	 * selects the export or reuse topics of the forum
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return	The list of export or reuse topics of the forum
	 */
	public List<Topic> selectForumExportTopics(int forumId);
	
	/**
	 * Selects the gradable topics of the forum
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return	The list of gradable topics of the forum
	 */
	public List<Topic> selectForumGradableTopics(int forumId);
	
	/**
	 * Select forum last post info
	 * 
	 * @param forumId			The forum id
	 * 
	 * @param invisbleTopics	true - if topic dates are to be considered
	 * 							false - if topic dates are to be ignored
	 * 
	 * @return	The last post info
	 */
	public LastPostInfo selectForumLastPostInfo(int forumId, boolean invisbleTopics);
	
	/**
	 * select the topics of the forum
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return	List of topics of the forum
	 */
	public List<Topic> selectForumTopics(int forumId);
	
	/**
	 * select the topics of the forum limiting the number of records returned
	 * 
	 * @param forumId		The forum id
	 * 
	 * @param startFrom		From row
	 * 
	 * @param count			Number of records
	 * 
	 * @return	The topics of the forum limiting the number of records returned
	 */
	public List<Topic> selectForumTopics(int forumId, int startFrom, int count);
	
	/**
	 * select the sorted topics of the forum limiting the number of records returned
	 * 
	 * @param forumId		The forum Id
	 * 
	 * @param topicSort		Topic sort
	 * 
	 * @param startFrom		From row
	 * 
	 * @param count			Number of records
	 * 			
	 * @return	The sorted topics of the forum limiting the number of records returned
	 */
	public List<Topic> selectForumTopics(int forumId, Topic.TopicSort topicSort, int startFrom, int count);
	
	/**
	 * Selects the count of marked topic count by forum for the user
	 * 
	 * @param forumId	The forum id
	 * 
	 * @param userId	Jforum user id
	 * 
	 * @return	The count of marked topic count by forum for the user
	 */
	public int selectMarkedTopicsCountByForum(int forumId, int userId);
	
	/**
	 * Selects user topic mark or visit time
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param userId	Jforum user id
	 * 
	 * @return	Topic with user mark time, read status or null
	 */
	public Topic selectMarkTime(int topicId, int userId);
	
	/**
	 * Select post by id
	 * 
	 * @param postId	Post id
	 * 
	 * @return	Existing post or null 
	 */
	public Post selectPostById(int postId);

	/**
	 * Selects the user posts in the category
	 * 
	 * @param categoryId	Category id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The user posts in the category
	 */
	public List<Post> selectPostsByCategoryByUser(int categoryId, int userId);
	
	/**
	 * Selects the users posts in the forum
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The users posts in the forum
	 */
	public List<Post> selectPostsByForumByUser(int forumId, int userId);
	
	/**
	 * Selects the user posts in the topic
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The user posts in the topic
	 */
	public List<Post> selectPostsByTopicByUser(int topicId, int userId);
	
	/**
	 * Selects the user posts in the site
	 * 
	 * @param siteId	Site id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The user posts in the site
	 */
	public List<Post> selectPostsByUser(String siteId, int userId);
	
	/**
	 * Gets the topic posts count
	 * 
	 * @param topicId	Topic id
	 * 
	 * @return	The topic posts count
	 */
	public int selectPostsCountByTopic(int topicId);
	
	/**
	 * select the recent topics of the course
	 * 
	 * @param context	The context or course id
	 * @param limit		The number of topics to retrieve
	 * 
	 * @return	List of recent topics of the context
	 */
	public List<Topic> selectRecentTopics(String context, int limit);
	
	/**
	 * Gets the topics count that have dates for the category
	 * 
	 * @param categoryId	Category id
	 * 
	 * @return	The count of the topics that have dates
	 */	
	public int selectTopicDatesCountByCategory(int categoryId);
	
	/**
	 * Gets the topics count that have dates for the forum
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return The count of the topics that have dates
	 */
	public int selectTopicDatesCountByForum(int forumId);
	
	/**
	 * Select the topic last post info
	 * 
	 * @param topicId	The topic id
	 * 
	 * @return	The topic last post info
	 */
	public LastPostInfo selectTopicLastPostInfo(int topicId);
	
	/**
	 * Selects the topic latest post time not posted by the user
	 * 
	 * @param topicId	The topic id
	 * 
	 * @param userId	JForum user id
	 * 
	 * @param after		Get the post times after this time or null if all the post are to be considered
	 * 
	 * @param topicDatesNeeded	true - if topic dates are needed
	 * 							false - if topic dates are not needed
	 * 
	 * @return	Topic with latest post
	 */
	public Topic selectTopicLatestPosttime(int topicId, int userId, Date after, boolean topicDatesNeeded);
	
	/**
	 * Selects the topic latest post times of the forum not posted by the user
	 * 
	 * @param forumId	The forum id
	 * 
	 * @param userId	JForum user id
	 * 
	 * @param after		Get the post times after this time or null if all the post are to be considered  
	 * 
	 * @param topicDatesNeeded	true - if topic dates are needed
	 * 							false - if topic dates are not needed
	 * 
	 * @return	List of topics with latest post times.
	 */
	public List<Topic> selectTopicLatestPosttimesByForum(int forumId, int userId, Date after, boolean topicDatesNeeded);
	
	/**
	 * select the posts of the topic
	 * 
	 * @param topicId	The topic id
	 * 
	 * @return	List of posts of the topic
	 */
	public List<Post> selectTopicPosts(int topicId);
	
	/**
	 * Selects the topic posts limiting the number of records returned
	 * 
	 * @param topicId		Topic id
	 * 
	 * @param startFrom		Starting row
	 * 
	 * @param count			Number of records
	 * 
	 * @return	The topic posts limiting the number of records returned
	 */
	public List<Post> selectTopicPostsByLimit(int topicId, int startFrom, int count);
	
	/**
	 * Selects the topics count for the forum
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return	The forum topics count
	 */
	public int selectTopicsCountByForum(int forumId);
	
	/**
	 * Gets the topics that have dates for the forum
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return	The topics that have dates for the forum
	 */
	public List<Topic> selectTopicsWithDatesByForum(int forumId);
	
	
	/**
	 * Gets the category post id's that user liked
	 * 
	 * @param categoryId	Category id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The category post id's that user liked
	 */
	public Set<Integer> selectUserCategoryPostLikes(int categoryId, int userId);
	
	/**
	 * Gets the forum post id's that user liked
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The forum post id's that user liked
	 */
	public Set<Integer> selectUserForumPostLikes(int forumId, int userId);
	
	/**
	 * Gets the user topic post id's that user liked
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The user topic post id's that user liked
	 */
	public Set<Integer> selectUserTopicPostLikes(int topicId, int userId);
	
	/**
	 * Select user topic visit times
	 * 
	 * @param forumId	The forum id
	 * 
	 * @param userId	Jforum user id
	 * 
	 * @return	List of user topic visit times
	 */
	public List<Topic> selectUserTopicVisitTimesByForum(int forumId, int userId);
	
	/**
	 * Subscribe the user for notification of new post on the topic 
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param userId	User id
	 */
	public void subscribeUser(int topicId, int userId);
	
	/**
	 * Subscribe the user for notification of new post on the topic of the forum. The user can be subscribed to forum topic only if user is subscribed  to forum
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param userId	User id
	 * 
	 * @param forumId	Forum id
	 */
	public void subscribeUser(int topicId, int userId, int forumId);
	
	/**
	 * Changes the topic to re-use or not to re-use.
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param exportImportCode	TopicExportImportCode
	 */
	public void updateExportImportCode(int topicId, Topic.TopicExportImportCode exportImportCode);
	
	/**
	 * Change the subscribed topic read status 
	 * 
	 * @param topicId The topic id
	 * 
	 * @param userId The user id
	 * 
	 * @param read <code>true</code> or <code>false</code>
	 */
	public void updateSubscribedTopicReadStatus(int topicId, int userId, boolean read);
	
	/**
	 * update topic
	 * 
	 * @param topic	Topic
	 */
	public void updateTopic(Topic topic);
	
	/**
	 * Update user topic mark or visit time if record existing else adds a record
	 * 
	 * @param topicId	The topic id
	 * 
	 * @param userId	Jforum user id
	 * 
	 * @param markTime	Mark time
	 * 
	 * @param isRead	true for read and false for unread
	 */
	public void updateTopicMarkTime(int topicId, int userId, Date markTime, boolean isRead);
	
	/**
	 * Updates topic post
	 * 
	 * @param post	Post object
	 */
	public void updateTopicPost(Post post);
}
