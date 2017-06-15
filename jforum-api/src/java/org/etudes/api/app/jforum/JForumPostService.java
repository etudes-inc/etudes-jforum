/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/JForumPostService.java $ 
 * $Id: JForumPostService.java 9940 2015-01-29 01:36:33Z murthyt $ 
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

import org.etudes.api.app.jforum.Topic.TopicSort;

public interface JForumPostService
{
	//public final String HOT_TOPIC_BEGIN = "etudes.jforum.hot.topic.begin";
	
	public final String POSTS_PER_PAGE = "etudes.jforum.postsPerPage";
	
	public final int RECENT_TOPICS_LIMIT = 100;
	
	/**
	 * Marks or add the user post like if not marked or added previously
	 * 
	 * @param postId	Post id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return		true - if the post is marked like
	 * 				false - if the post is not marked like or already marked like
	 * 
	 * @throws JForumAccessException if user is not allowed to access topic
	 */
	public boolean addPostLike(int postId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Assigns zero to non submitters who has not posted any posts in the topic
	 * 
	 * @param topicId		Topic id
	 * 
	 * @param evaluatedBySakaiUserId	Evaluated by sakai user id
	 * 
	 * @throws JForumAccessException	If user has no access to grade the category
	 */
	public void assignZeroTopicNonSubmitters(int topicId, String evaluatedBySakaiUserId) throws JForumAccessException;
	
	/**
	 * validates post attachments for allowed extensions and quota limit then creates new post for an existing topic
	 * 
	 * @param topic		Topic with post information
	 * 
	 * @return	New post id
	 * 
	 * @throws JForumAccessException	if user is not allowed to access topic
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public int createPost(Topic topic) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Creates new topic 
	 * 
	 * @param topic	Topic with post information
	 * 
	 * @return	New topic id
	 *
	 * @throws JForumAccessException	if user is not allowed to access topic's forum
	 */
	public int createTopic(Topic topic) throws JForumAccessException;
	
	/**
	 * Created new topic 
	 * 
	 * @param topic	Topic with post information
	 * 
	 * @param notifications	true - notifications sent to "new topic" notification subscribers
	 * 						false - notifications not sent
	 * @return	New topic id
	 * 
	 * @throws JForumAccessException
	 */
	public int createTopic(Topic topic, boolean notifications) throws JForumAccessException;
	
	/**
	 * creates new post for an existing topic
	 * 
	 * @param topic	Topic with post information
	 *
	 * @return	New post id
	 * 
	 * @throws JForumAccessException	if user is not allowed to access topic
	 */
	public int createTopicPost(Topic topic) throws JForumAccessException;
	
	/**
	 * validates topic post attachments for allowed extensions and quota limit then creates new topic and post
	 * 
	 * @param topic		Topic
	 * 
	 * @return	New topic id
	 * 
	 * @throws JForumAccessException	if user is not allowed to access topic
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public int createTopicWithAttachments(Topic topic) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * validates topic post attachments for allowed extensions and quota limit then creates new topic and post
	 * 
	 * @param topic		Topic
	 * 
	 * @param notifications	true - notifications sent to "new topic" notification subscribers
	 * 						false - notifications not sent
	 * 
	 * @return	New topic id
	 * 
	 * @throws JForumAccessException	if user is not allowed to access topic
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public int createTopicWithAttachments(Topic topic, boolean notifications) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Evaluates topic
	 * 
	 * @param topic	Topic with evaluations
	 * 
	 * @throws JForumAccessException	If user has no access to evaluations
	 */
	public void evaluateTopic(Topic topic) throws JForumAccessException;
	
	/**
	 * Gets the export or reuse and gradable topics of the forum
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return The export or reuse and gradable topics of the forum
	 */
	public List<Topic> getForumExportAndGradableTopics(int forumId);
	
	/**
	 * Gets the reusable and gradable topics count of the forum
	 * 
	 * @param forumId	The fourm id
	 * 
	 * @return	The reusable and gradable topics count of the forum
	 */
	public int getForumExportAndGradableTopicsCount(int forumId);
	
	/**
	 * Gets the export or reuse topics of the forum
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return	The export or reuse topics of the forum
	 */
	public List<Topic> getForumExportTopics(int forumId);
	
	/**
	 * Gets the forum gradable topics
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return	The forum gradable topics
	 */
	public List<Topic> getForumGradableTopics(int forumId);
	
	/**
	 * Gets the latest post info of the forum for the user based on topics accessibility
	 * 
	 * @param siteId	The site id
	 * 
	 * @param forumId	The forum id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The latest post info of the forum for the user
	 */
	public LastPostInfo getForumLatestPostInfo(String siteId, int forumId, String sakaiUserId);
	
	/**
	 * Gets the forum topics latest post times not posted by the user
	 * 
	 * @param forumId	The forum id
	 * 
	 * @param userId	The jforum user id
	 * 
	 * @param after		Posts "after" time or null if need to consider all posts
	 * 
	 * @param topicDatesNeeded	true - if topic dates are needed
	 * 							false - if topic dates are needed
	 * 
	 * @return 	The forum topics latest post times not posted by the user
	 */
	public List<Topic> getForumTopicLatestPosttimes(int forumId, int userId, Date after, boolean topicDatesNeeded);
	
	/**
	 * Gets the topics of the forum
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return	List of topics of the forum
	 */
	public List<Topic> getForumTopics(int forumId);
	
	/**
	 * Gets the topics of the forum limiting the number of records
	 * 
	 * @param forumId		The forum id
	 * 
	 * @param startFrom		Rows from
	 * 
	 * @param count			Number of records
	 * 
	 * @return	The topics of the forum limiting the number of records
	 */
	public List<Topic> getForumTopics(int forumId, int startFrom, int count);
	
	/**
	 * get the accessible topics and not hidden(visible but not accessible) count
	 * 
	 * @param forumId	The forum id
	 * @return			count of accessible and not hidden(visible but not accessible) topics
	 */
	public int getForumTopicsAccessibleCount(int forumId);
	
	/**
	 * get the accessible and not hidden(visible but not accessible) topics messages count
	 * 
	 * @param forumId	The forum id
	 * @return			count of the accessible and not hidden(visible but not accessible) topics messages count
	 */
	public int getForumTopicsAccessibleMessagesCount(int forumId);
	
	/**
	 * gets the forum topics count
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return	The forum topics count
	 */
	public int getForumTopicsCount(int forumId);
	
	/**
	 * Gets the user marked unread topics count for the forum
	 * 
	 * @param forumId	The forum id
	 * 
	 * @param userId	The jforum user id
	 * 
	 * @return	The marked unread topics count for the forum
	 */
	public int getMarkedTopicsCountByForum(int forumId, int userId);
	
	/**
	 * Gets the post
	 * 
	 * @param postId	The post id
	 * 
	 * @return	The Post object or null if the topic doesn't exist
	 */
	public Post getPost(int postId);
	
	/**
	 * Gets the post attachment
	 * 
	 * @param attachmentId	Attachment id
	 * 
	 * @return	Attachment
	 */
	public Attachment getPostAttachment(int attachmentId);
	
	/**
	 * Gets the recent topics of the course
	 * 
	 * @param context	The context or course id
	 * 
	 * @param limit		The number of topics to retrieve
	 * 
	 * @return	List of recent topics of the context
	 */
	public List<Topic> getRecentTopics(String context, int limit);
	
	/**
	 * Gets the recent topics of the course for the user
	 * 
	 * @param context	The context or course id
	 * 
	 * @param limit		The number of topics to retrieve
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return			List of recent topics of the context
	 * 
	 * @throws JForumAccessException	If user is not site participant
	 */
	public List<Topic> getRecentTopics(String context, int limit, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the topic
	 * 
	 * @param topicId	The topic id
	 * 
	 * @return	The Topic object or null if the topic doesn't exist
	 */
	public Topic getTopic(int topicId);
	
	/**
	 * Gets the user topic
	 * 
	 * @param topicId	The topic id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The user topic
	 * 
	 * @throws JForumAccessException If user is not allowed to access the topic
	 */
	public Topic getTopic(int topicId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the topics count that have dates for the category
	 * 
	 * @param categoryId	Category id
	 * 
	 * @return	The count of the topics that have dates
	 */	
	public int getTopicDatesCountByCategory(int categoryId);
	
	
	/**
	 * Gets the topics count that have dates for the forum
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return The count of the topics that have dates
	 */
	public int getTopicDatesCountByForum(int forumId);
	
	/**
	 * Gets the topic latest post time not posted by the user
	 * 
	 * @param topicId	The topic id
	 * 
	 * @param userId	Jforum user id
	 * 
	 * @param after		Posts "after" time or null if need to consider all posts
	 * 
	 * @param topicDatesNeeded	true - if topic dates are needed
	 * 							false - if topic dates are needed
	 * 
	 * @return	The topic latest post time not posted by the user
	 */
	public Topic getTopicLatestPosttime(int topicId, int userId, Date after, boolean topicDatesNeeded);
	
	/**
	 * Gets the topic posts limiting the number of records
	 * 
	 * @param topicId		The topic id
	 * 
	 * @param startFrom		Starting row
	 * 
	 * @param count			Number of records
	 * 
	 * @return	The topic posts limiting the number of records
	 */
	public List<Post> getTopicPosts(int topicId, int startFrom, int count);
	
	/**
	 * Gets the topic posts for the user limiting the number of records
	 * 
	 * @param topicId		The topic id
	 * 
	 * @param startFrom		Starting row
	 * 
	 * @param count			Number of records
	 * 
	 * @param	sakaiUserId User sakai id
	 * 
	 * @return	The topic posts limiting the number of records
	 *
	 * @throws JForumAccessException If user is not allowed to access the topic
	 */
	public List<Post> getTopicPosts(int topicId, int startFrom, int count, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the users topics of the forum limiting the number of records
	 * 
	 * @param forumId		The forum id
	 * 
	 * @param startFrom		Rows from
	 * 
	 * @param count			Number of records
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	The list of topics
	 * 
	 * @throws JForumAccessException	If user is not authorized to access the category, forum if there are forum topics.
	 */
	public List<Topic> getTopics(int forumId, int startFrom, int count, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the users sorted topics of the forum limiting the number of records
	 * 
	 * @param forumId		The forum id
	 * 
	 * @param topicSort		Topic sort
	 * 
	 * @param startFrom		Rows from
	 * 
	 * @param count			Number of records
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	The list of topics
	 * 
	 * @throws JForumAccessException	If user is not authorized to access the category, forum if there are forum topics.
	 */
	public List<Topic> getTopics(int forumId, Topic.TopicSort topicSort, int startFrom, int count, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the topic posts count
	 * 
	 * @param topicId	The topic id
	 * 
	 * @return	The topic posts count
	 */
	public int getTotalPosts(int topicId);
	
	/**
	 * Gets the users topics of the forum that are accessible
	 * 
	 * @param forumId	The forum id
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @param topicSort null or topic sort
	 * 
	 * @return	The list of topics
	 * 
	 * @throws JForumAccessException	If user is not authorized to access the category, forum if there are forum topics.
	 */
	public List<Topic> getUserAccessibleTopics(int forumId, String sakaiUserId, TopicSort topicSort) throws JForumAccessException;
	
	/**
	 * Gets the user category posts
	 * 
	 * @param categoryId	category id
	 * 
	 * @param sakaiUserId	Sakai user id for fetching user posts
	 * 
	 * @return	The user category posts
	 * 
	 * @throws JForumAccessException	If the user is not in the site
	 */
	public List<Post> getUserCategoryPosts(int categoryId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the user category posts
	 * 
	 * @param categoryId	category id
	 * 
	 * @param sakaiUserId	Sakai user id for fetching user posts
	 * 
	 * @param fetchedBySakaiUserId Sakai user id of the user fetching the posts
	 * 
	 * @return	The user category posts
	 * 
	 * @throws JForumAccessException	If the user is not in the site or dropped and fetched by user is not instructor
	 */
	public List<Post> getUserCategoryPosts(int categoryId, String sakaiUserId, String fetchedBySakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the user category posts count
	 * 
	 * @param categoryId	Category id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The user category posts count
	 * 
	 * @throws JForumAccessException	If the user is not in the site
	 */
	public int getUserCategoryPostsCount(int categoryId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the user forum posts
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param sakaiUserId	Sakai user id for fetching user posts
	 * 
	 * @return	The user forum posts
	 * 
	 * @throws JForumAccessException	If the user is not in the site
	 */
	public List<Post> getUserForumPosts(int forumId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the user forum posts
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param sakaiUserId	Sakai user id for fetching user posts
	 * 
	 * @param fetchedBySakaiUserId Sakai user id of the user fetching the posts
	 * 
	 * @return	The user forum posts
	 * 
	 * @throws JForumAccessException	If the user is not in the site or dropped and fetched by user is not instructor
	 */
	public List<Post> getUserForumPosts(int forumId, String sakaiUserId, String fetchedBySakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the user forum posts count
	 * 
	 * @param forumId	Category id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The user forum posts count
	 * 
	 * @throws JForumAccessException	If the user is not in the site
	 */
	public int getUserForumPostsCount(int forumId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the user topic visit times if user visited the topic
	 * 
	 * @param forumId	The forum id
	 * 
	 * @param userId	The jforum user id
	 * 
	 * @return	The user topic visit times
	 */
	public List<Topic> getUserForumTopicVisittimes(int forumId, int userId);
	
	/**
	 * Gets the user posts in the site
	 * 
	 * @param siteId	Site id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The user posts in the site
	 * 
	 * @throws JForumAccessException	If the user is not in the site
	 */
	public List<Post> getUserPosts(String siteId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the user topic mark or visit time
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	Topic with user mark time, read status or null
	 */
	public Topic getUserTopicMarkTime(int topicId, String sakaiUserId);
	
	/**
	 * Gets the user topic posts
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param sakaiUserId	Sakai user id for fetching user posts
	 * 
	 * @return	The user topic posts
	 * 
	 * @throws JForumAccessException	If the user is not in the site
	 */
	public List<Post>  getUserTopicPosts(int topicId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the user topic posts
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param sakaiUserId	Sakai user id for fetching user posts
	 * 
	 * @param fetchedBySakaiUserId Sakai user id of the user fetching the posts
	 * 
	 * @return	The user topic posts
	 * 
	 * @throws JForumAccessException	If the user is not in the site or dropped and fetched by user is not instructor
	 */
	public List<Post>  getUserTopicPosts(int topicId, String sakaiUserId, String fetchedBySakaiUserId) throws JForumAccessException;
	
	
	/**
	 * Gets the user topic posts count
	 * 
	 * @param categoryId	Topic id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The user topic posts count
	 * 
	 * @throws JForumAccessException	If the user is not in the site
	 */
	public int getUserTopicPostsCount(int topicId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Checks the user's topic subscription status
	 * 
	 * @param topicId	The topic id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The user's topic subscription status
	 */
	public boolean isUserSubscribedToTopic(int topicId, String sakaiUserId);
	
	/**
	 * Lock or unlcok the topics
	 * 
	 * @param topicId	Topic id's
	 * 
	 * @param status	Lock or unlock topic status
	 */
	public void lockUnlock(int[] topicId, Topic.TopicStatus status);
	
	/**
	 * Mark topic read or unread for the user along with visit or mark time
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param sakaiUserId	Sakai User id
	 * 
	 * @param markTime	Mark time
	 * 
	 * @param isRead	Read or unread
	 */
	public void markTopicRead(int topicId, String sakaiUserId, Date markTime, boolean isRead);
	
	/**
	 * Modifies post and it's topic. Validates attachment quota limit and file extensions.
	 *  
	 * @param post	Post with changes and if first post any topic changes 
	 * 
	 * @throws JForumAccessException	if user is not allowed to access topic
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 */
	public void modifyPost(Post post) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException;
	
	/**
	 * Modifies the topic and it's grade if topic is gradable
	 * 
	 * @param topic	Topic
	 *
	 * @param bySakaiUserId	Modified by sakai user id
	 * 
	 * @throws JForumItemNotFoundException	If topic is not existing
	 * 
	 * @throws JForumAccessException	if user is not allowed to access topic
	 */
	public void modifyTopic(Topic topic, String bySakaiUserId) throws JForumItemNotFoundException, JForumAccessException, JForumGradesModificationException;
	
	/**
	 * Modifies post and it's topic. Validates attachment quota limit and if the post is first post checks if gradable topic is evaluated
	 * 
	 * @param post	Post with changes and if first post any topic changes
	 * 
	 * @throws JForumAccessException	if user is not allowed to access topic
	 * 
	 * @throws JForumAttachmentOverQuotaException	if attachments total size is more than allowed
	 * 
	 * @throws JForumAttachmentBadExtensionException	if the attachment has not allowed extension
	 * 
	 * @throws JForumGradesModificationException	if the post is first post and gradable topic is evaluated
	 */
	public void modifyTopicPost(Post post) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException, JForumGradesModificationException;
	
	/**
	 * Modify the topics to re-use or not to re-use
	 * 
	 * @param topicId	Topic id's
	 * 
	 * @param exportImportCode TopicExportImportCode
	 */
	public void modifyTopicReuse(int[] topicId, Topic.TopicExportImportCode exportImportCode);
	
	/**
	 * Moves topic to another forum
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param toForumId	Forum id to be moved to
	 * 
	 * @param movedBySakaiUserId	Moved by sakai user id
	 * 
	 * @throws JForumItemNotFoundException	If topic is not existing
	 * 
	 * @throws JForumAccessException	If user has no access to move the topic
	 * 
	 * @throws JForumGradesModificationException	Gradable topic and gradable forum topics cannot be moved
	 */
	public void moveTopic(int topicId, int toForumId, String movedBySakaiUserId) throws JForumItemNotFoundException, JForumAccessException, JForumGradesModificationException;
	
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
	 * @return	The attachment object
	 */
	public Attachment newAttachment(String fileName, String contentType, String comments, byte[] fileContent);
	
	/**
	 * Creates new instance of Post object
	 * 
	 * @return	The Post object
	 */
	public Post newPost();
	
	/**
	 * Creates new instance of Topic object
	 * 
	 * @return	The Topic object
	 */
	public Topic newTopic();
	
	/**
	 * Creates new instance of Grade object for the topic
	 * 
	 * @param topic	The topic object
	 * 
	 * @return	The grade object thats added to topic
	 */
	public Grade newTopicGrade(Topic topic);
	
	/**
	 * Preview post before posting
	 * 
	 * @param post	Post with subject and text
	 */
	public void previewPostForDisplay(Post post);
	
	/**
	 * Removes post and it's attachments
	 * 
	 * @param postId	Post id
	 * 
	 * @param deletedBySakaiUserId	Deleted by sakai user id
	 * 
	 * @throws JForumItemNotFoundException	If post is not found
	 * 
	 * @throws JForumAccessException	If user is not allowed to access this item or perform this action
	 * 
	 * @throws JForumGradesModificationException	If the topic has evaluations and the only post is deleted
	 */
	public void removePost(int postId, String deletedBySakaiUserId) throws JForumItemNotFoundException, JForumAccessException, JForumGradesModificationException;
	
	/**
	 * Removes the user post like
	 * 
	 * @param postId	Post id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	true - if the post is marked like
	 * 			false - if the post is not marked like or already marked like
	 * 
	 * @throws JForumAccessException	if user is not allowed to access topic
	 */
	public boolean removePostLike(int postId, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Removes topic and it's posts
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param deletedBySakaiUserId	Deletec by sakai user id
	 * 
	 * @throws JForumItemNotFoundException	If post is not found
	 * 
	 * @throws JForumAccessException	If user is not allowed to access this item or perform this action
	 * 
	 * @throws JForumItemEvaluatedException	If the gradable topic ot gradable topic forum has evaluations 
	 */
	public void removeTopic(int topicId, String deletedBySakaiUserId) throws JForumItemNotFoundException, JForumAccessException, JForumItemEvaluatedException;
	
	/**
	 * Clean all subscriptions of some topic
	 * 
	 * @param topicId The topic id
	 */
	public void removeTopicSubscription(int topicId);
	
	/**
	 * Remove user topic subscription
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param sakaiUserId	Sakai user id
	 */
	public void removeUserTopicSubscription(int topicId, String sakaiUserId);
	
	/**
	 * Subscribe user to a topic to receive notifications of new posts
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param sakaiUserId	Sakai user id
	 */
	public void subscribeUserToTopic(int topicId, String sakaiUserId);
	
	/**
	 * Updates the dates of the topic if changed
	 * 
	 * @param topic	Topic
	 */
	public void updateDates(Topic topic);
}