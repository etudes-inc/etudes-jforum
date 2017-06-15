/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/dao/generic/ForumDaoGeneric.java $ 
 * $Id: ForumDaoGeneric.java 8326 2014-06-30 23:00:37Z murthyt $ 
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
package org.etudes.component.app.jforum.dao.generic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.AccessDates;
import org.etudes.api.app.jforum.Category;
import org.etudes.api.app.jforum.Forum;
import org.etudes.api.app.jforum.Grade;
import org.etudes.api.app.jforum.JForumCategoryService;
import org.etudes.api.app.jforum.JForumForumTopicsExistingException;
import org.etudes.api.app.jforum.JForumGradeService;
import org.etudes.api.app.jforum.JForumPostService;
import org.etudes.api.app.jforum.JForumSecurityService;
import org.etudes.api.app.jforum.SpecialAccess;
import org.etudes.api.app.jforum.Topic;
import org.etudes.api.app.jforum.dao.CategoryDao;
import org.etudes.api.app.jforum.dao.ForumDao;
import org.etudes.api.app.jforum.dao.GradeDao;
import org.etudes.api.app.jforum.dao.SpecialAccessDao;
import org.etudes.api.app.jforum.dao.TopicDao;
import org.etudes.component.app.jforum.AccessDatesImpl;
import org.etudes.component.app.jforum.ForumImpl;
import org.etudes.component.app.jforum.GradeImpl;
import org.etudes.util.DateHelper;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;

public abstract class ForumDaoGeneric implements ForumDao
{
	private static Log logger = LogFactory.getLog(ForumDaoGeneric.class);
	
	protected CategoryDao categoryDao = null;

	/** Dependency: GradeDao */
	protected GradeDao gradeDao = null;

	/** Dependency: JForumCategoryService. */
	protected JForumCategoryService jforumCategoryService = null;
	
	/** Dependency: JForumGradeService. */
	protected JForumGradeService jforumGradeService = null;
	
	/** Dependency: JForumPostService. */
	protected JForumPostService jforumPostService = null;
	
	/** Dependency: JForumSecurityService. */
	protected JForumSecurityService jforumSecurityService = null;
	
	/** Dependency: SqlService */
	protected SpecialAccessDao specialAccessDao = null;

	/** Dependency: SqlService */
	protected SqlService sqlService = null;

	protected TopicDao topicDao = null;
	
	/**
	 * {@inheritDoc}
	 */
	public int addNew(Forum forum)
	{
		if ((forum == null) || (forum.getName() == null) || (forum.getName().trim().length() == 0) ||  (forum.getCategoryId() == 0))
		{
			throw new IllegalArgumentException("forum information is missing");
		}
		
		if (forum.getId() > 0)
		{
			throw new IllegalArgumentException("New forum cannot be created as forum has id.");
		}
		
		if (forum.getName() != null && forum.getName().length() > 150)
		{
			forum.setName(forum.getName().substring(0, 149));
		}
		
		/*if (forum.getDescription() != null && forum.getDescription().length() > 255)
		{
			forum.setDescription(forum.getDescription().substring(0, 254));
		}*/
		
		return creatNewForumTx(forum);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int copyForum(int forumId)
	{
		if (forumId <= 0)
		{
			throw new IllegalArgumentException("Existing forum information is missing.");
		}
		
		return copyForumTx(forumId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void decrementTotalTopics(int forumId, int count)
	{
		StringBuilder sql = new StringBuilder();
		
		sql.append("UPDATE jforum_forums SET forum_topics = forum_topics - ? WHERE forum_id = ?");
		
		Object[] fields = new Object[2];
		int i = 0;
		
		fields[i++] = count;
		fields[i++] = forumId;
		
		if (!sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("decrementTotalTopics: db write failed");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void delete(int forumId) throws JForumForumTopicsExistingException
	{
		Forum forum = selectById(forumId);
		
		if (forum == null)
		{
			return;
		}
		
		// if forum have topics don't allow to delete forum
		if (getTotalTopics(forumId) > 0)
		{
			throw new JForumForumTopicsExistingException(forum.getName());
		}
		
		deleteTx(forum);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getMaxPostId(int forumId)
	{
		int maxPostId = -1;
		
		final List<Integer> postIds = new ArrayList<Integer>();
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT MAX(post_id) AS post_id FROM jforum_posts WHERE forum_id = ?");						
				
		Object[] fields;
		int i = 0;
		
		fields = new Object[1];
		fields[i++] = forumId;
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					postIds.add(result.getInt("post_id"));
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("getMaxPostId: " + e, e);
					}
					return null;
				}
			}
		});
		
		if (postIds.size() == 1)
		{
			maxPostId = postIds.get(0);
		}
		
		return maxPostId;
	}
	/**
	 * {@inheritDoc}
	 */
	public int getTotalTopics(int forumId)
	{
		int topicsCount = 0;
		
		final List<Integer> count = new ArrayList<Integer>();
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT COUNT(topic_id) AS topics_count FROM jforum_topics WHERE forum_id = ?");						
				
		Object[] fields;
		int i = 0;
		
		fields = new Object[1];
		fields[i++] = forumId;
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					count.add(result.getInt("topics_count"));
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("getTotalTopics: " + e, e);
					}
					return null;
				}
			}
		});
		
		if (count.size() == 1)
		{
			topicsCount = count.get(0);
		}
		
		return topicsCount;	
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void incrementTotalTopics(int forumId, int count)
	{
		StringBuilder sql = new StringBuilder();
		
		sql.append("UPDATE jforum_forums SET forum_topics = forum_topics + ? WHERE forum_id = ?");
		
		Object[] fields = new Object[2];
		int i = 0;
		
		fields[i++] = count;
		fields[i++] = forumId;
		
		if (!sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("incrementTotalTopics: db write failed");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isUserSubscribed(int forumId, int userId)
	{
		if (forumId <= 0 || userId <= 0)
		{
			throw new IllegalArgumentException("forum or user information is missing");
		}
		
		String sql = "SELECT user_id FROM jforum_forums_watch WHERE forum_id = ? AND user_id = ?";
				
		Object[] fields;
		int i = 0;
		
		fields = new Object[2];
		fields[i++] = forumId;
		fields[i++] = userId;
		
		final List<Integer> subscribedList = new ArrayList<Integer>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					subscribedList.add(result.getInt("user_id"));

					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("isUserSubscribed: " + e, e);
					}
					return null;
				}
			}
		});
		
		boolean subscribed = false;
		if (subscribedList.size() > 0)
		{
			subscribed = true;
		}
		return subscribed;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeSubscription(final int forumId)
	{
		if (forumId <= 0)
		{
			throw new IllegalArgumentException("forum information is missing");
		}
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					// delete forum all topics subscriptions
					topicDao.removeForumSubscriptions(forumId);	
		
					// delete forum subscription
					String sql = "DELETE FROM jforum_forums_watch WHERE forum_id = ?";
				
					Object[] fields;
					int i = 0;
					
					fields = new Object[1];
					fields[i++] = forumId;
					
					if (!sqlService.dbWrite(sql, fields)) 
					{
						throw new RuntimeException("removeSubscription() - db write failed");
					}			
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while subscribing user to watch the forum.", e);
				}
			}
		}, "removeSubscription: forum id: " + forumId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void removeUserSubscription(final int forumId, final int userId)
	{
		if (forumId <= 0 || userId <= 0)
		{
			throw new IllegalArgumentException("forum or user information is missing");
		}
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					// delete user topics subscriptions
					topicDao.removeUserForumSubscriptions(userId, forumId);
		
					String sql = "DELETE FROM jforum_forums_watch WHERE forum_id = ? AND user_id = ?";
				
					Object[] fields;
					int i = 0;
					
					fields = new Object[2];
					fields[i++] = forumId;
					fields[i++] = userId;
					
					if (!sqlService.dbWrite(sql, fields)) 
					{
						throw new RuntimeException("removeUserSubscription() - db write failed");
					}				
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while subscribing user to watch the forum.", e);
				}
			}
		}, "removeUserSubscription: forum id: " + forumId +" user id: "+ userId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Forum> selectAllByCourse(String courseId)
	{
		if (logger.isDebugEnabled()) 
		{
			logger.debug("selectAllByCourse - courseId:"+ courseId);
		}
		
		// all site forums
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT f.forum_id, f.categories_id, f.forum_name, f.forum_desc, f.forum_order, f.forum_topics, f.forum_last_post_id, f.moderated, f.start_date, f.hide_until_open, f.end_date, f.allow_until_date, ");
		sql.append("f.forum_type, f.forum_access_type, f.forum_grade_type, f.lock_end_date, f.forum_topic_order, f.forum_topic_likes, COUNT(p.post_id) AS total_posts "); 
		sql.append("FROM jforum_sakai_course_categories c, jforum_forums f ");
		sql.append("LEFT JOIN jforum_topics t ON f.forum_id = t.forum_id ");
		sql.append("LEFT JOIN jforum_posts p ON p.topic_id = t.topic_id ");
		sql.append("WHERE f.categories_id = c.categories_id ");
		sql.append("AND c.course_id = ? ");
		sql.append("GROUP BY f.categories_id, f.forum_id ");
		sql.append("ORDER BY f.forum_order ASC");
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = courseId;
		
		List<Forum> forums = readForums(sql.toString(), fields);		
		
		for (Forum forum : forums)
		{
			// groups
			if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
			{
				forum.setGroups(getForumGroups(forum.getId()));
				forum.setGroupsReadonyAccessType(getForumGroupsReadonlyAccessType(forum.getId()));
			}
			
			// special access
			if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))
			{
				List<SpecialAccess> specialAccess = specialAccessDao.selectByForum(forum.getId());
				forum.getSpecialAccess().addAll(specialAccess);
			}
			
			// grades
			if (forum.getGradeType() == Grade.GradeType.FORUM.getType())
			{
				Grade grade = gradeDao.selectByForum(forum.getId());
				forum.setGrade(grade);
			}
		}
		
		return forums;		
	}
	/**
	 * {@inheritDoc}
	 */
	public List<Forum> selectByCategoryId(int categoryId)
	{
		if (logger.isDebugEnabled()) 
		{
			logger.debug("selectByCategoryId - categoryId:"+ categoryId);
		}
		
		// all category forums
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT f.forum_id, f.categories_id, f.forum_name, f.forum_desc, f.forum_order, f.forum_topics, f.forum_last_post_id, f.moderated, ");
		sql.append("f.start_date, f.hide_until_open, f.end_date, f.allow_until_date, f.forum_type, f.forum_access_type, f.forum_grade_type, f.lock_end_date, f.forum_topic_order, f.forum_topic_likes, COUNT(p.post_id) AS total_posts ");
		sql.append("FROM jforum_sakai_course_categories c, jforum_forums f ");
		sql.append("LEFT JOIN jforum_topics t ON f.forum_id = t.forum_id ");
		sql.append("LEFT JOIN jforum_posts p ON p.topic_id = t.topic_id ");
		sql.append("WHERE f.categories_id = c.categories_id ");
		sql.append("AND c.categories_id = ? ");
		sql.append("GROUP BY f.categories_id, f.forum_id ");
		sql.append("ORDER BY f.categories_id, f.forum_order ASC");

		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = categoryId;
		
		List<Forum> forums = readForums(sql.toString(), fields);		
		
		for (Forum forum : forums)
		{
			// groups
			if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
			{
				forum.setGroups(getForumGroups(forum.getId()));
				forum.setGroupsReadonyAccessType(getForumGroupsReadonlyAccessType(forum.getId()));
			}
			
			// special access
			if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))
			{
				List<SpecialAccess> specialAccess = specialAccessDao.selectByForum(forum.getId());
				forum.getSpecialAccess().addAll(specialAccess);
			}
			
			// grades
			if (forum.getGradeType() == Grade.GradeType.FORUM.getType())
			{
				Grade grade = gradeDao.selectByForum(forum.getId());
				forum.setGrade(grade);
			}
		}
		
		return forums;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Forum selectById(int forumId)
	{
		// all site forums
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT forum_id, categories_id, forum_name, forum_desc, forum_order, forum_topics, ");
		sql.append("forum_last_post_id, moderated, start_date, hide_until_open, end_date, allow_until_date, forum_type, forum_access_type, ");
		sql.append("forum_grade_type, lock_end_date, forum_topic_order, forum_topic_likes FROM jforum_forums WHERE forum_id = ?");

		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = forumId;
		
		final List<Forum> forums =  new ArrayList<Forum>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Forum forum = fillForum(result);
					forums.add(forum);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectById: " + e, e);
					}
					return null;
				}
			}

			
		});	
		
		Forum forum = null;
		
		if (forums.size() == 1)
		{
			forum = forums.get(0);			

			// groups
			if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
			{
				forum.setGroups(getForumGroups(forum.getId()));
				forum.setGroupsReadonyAccessType(getForumGroupsReadonlyAccessType(forum.getId()));
			}
			
			// special access
			if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))
			{
				List<SpecialAccess> specialAccess = specialAccessDao.selectByForum(forum.getId());
				forum.getSpecialAccess().addAll(specialAccess);
			}
			
			// grades
			if (forum.getGradeType() == Grade.GradeType.FORUM.getType())
			{
				Grade grade = gradeDao.selectByForum(forum.getId());
				forum.setGrade(grade);
			}
		
		}
		
		return forum;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Integer> selectForumSubscriptions(int forumId)
	{
		if (forumId <= 0)
		{
			throw new IllegalArgumentException("Forum information is missing.");
		}
		
		String sql = "SELECT user_id FROM jforum_forums_watch WHERE forum_id = ?";
		
		Object[] fields;
		int i = 0;
		
		fields = new Object[1];
		fields[i++] = forumId;
		
		final List<Integer> subscribedList = new ArrayList<Integer>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					subscribedList.add(result.getInt("user_id"));

					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectForumSubscriptions: " + e, e);
					}
					return null;
				}
			}
		});
		
		return subscribedList;
		
	}
	
	/**
	 * @param categoryDao the categoryDao to set
	 */
	public void setCategoryDao(CategoryDao categoryDao)
	{
		this.categoryDao = categoryDao;
	}
	
	/**
	 * @param gradeDao 
	 * 			The gradeDao to set
	 */
	public void setGradeDao(GradeDao gradeDao)
	{
		this.gradeDao = gradeDao;
	}
	
	/**
	 * @param jforumCategoryService the jforumCategoryService to set
	 */
	public void setJforumCategoryService(JForumCategoryService jforumCategoryService)
	{
		this.jforumCategoryService = jforumCategoryService;
	}
	
	/**
	 * 
	 * @param jforumGradeService	the jforumGradeService to set
	 */
	public void setJforumGradeService(JForumGradeService jforumGradeService) 
	{
		this.jforumGradeService = jforumGradeService;
	}
	
	/**
	 * @param jforumPostService the jforumPostService to set
	 */
	public void setJforumPostService(JForumPostService jforumPostService)
	{
		this.jforumPostService = jforumPostService;
	}
	
	public void setJforumSecurityService(JForumSecurityService jforumSecurityService)
	{
		this.jforumSecurityService = jforumSecurityService;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setLastPost(int forumId, int postId)
	{
		StringBuilder sql = new StringBuilder();
		
		sql.append("UPDATE jforum_forums SET forum_last_post_id = ? WHERE forum_id = ?");
		
		Object[] fields = new Object[2];
		int i = 0;
		
		fields[i++] = postId;
		fields[i++] = forumId;
		
		if (!sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("setLastPost: db write failed");
		}
	}
	
	/**
	 * @param specialAccessDao 
	 * 			The specialAccessDao to set
	 */
	public void setSpecialAccessDao(SpecialAccessDao specialAccessDao)
	{
		this.specialAccessDao = specialAccessDao;
	}
	
	/**
	 * @param sqlService
	 *          The sqlService to set
	 */
	public void setSqlService(SqlService sqlService)
	{
		this.sqlService = sqlService;
	}
	
	/**
	 * @param topicDao the topicDao to set
	 */
	public void setTopicDao(TopicDao topicDao)
	{
		this.topicDao = topicDao;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void subscribeUser(final int forumId, final int userId)
	{
		if (forumId <= 0 || userId <= 0)
		{
			throw new IllegalArgumentException("forum or user information is missing");
		}
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					if (!isUserSubscribed(forumId, userId))
					{
								
						String sql = "INSERT INTO jforum_forums_watch(forum_id, user_id, created_time) VALUES (?, ?, ?)";
								
						Object[] fields;
						int i = 0;
						
						fields = new Object[3];
						fields[i++] = forumId;
						fields[i++] = userId;
						fields[i++] = new Timestamp(new Date().getTime());
						
						if (!sqlService.dbWrite(sql, fields)) 
						{
							throw new RuntimeException("subscribeUser() - db write failed");
						}
						
						// insert watch to all topics for the user
						List<Topic> topics = topicDao.selectForumTopics(forumId);
						
						for (Topic topic: topics)
						{
							// remove any existing topic subscriptions(watches) of the user
							topicDao.removeUserSubscription(topic.getId(), userId);
							
							// add new forum topic subscription with topic id, user id, and forum id
							topicDao.subscribeUser(topic.getId(), userId, forumId);
						}						
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while subscribing user to watch the forum.", e);
				}
			}
		}, "subscribeUser: forum id: " + forumId +" user id: "+ userId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void update(Forum forum)
	{
		if ((forum == null) || (forum.getName() == null) || (forum.getName().trim().length() == 0) ||  (forum.getCategoryId() == 0))
		{
			throw new IllegalArgumentException("forum information is missing");
		}
		
		if (forum.getId() == 0)
		{
			throw new IllegalArgumentException("Forum cannot be edited as forum information is missing.");
		}
		
		if (forum.getName() != null && forum.getName().length() > 150)
		{
			forum.setName(forum.getName().substring(0, 149));
		}
		
		/*if (forum.getDescription() != null && forum.getDescription().length() > 255)
		{
			forum.setDescription(forum.getDescription().substring(0, 254));
		}*/
		
		updateTx(forum);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Forum updateOrder(Forum forum, Forum other)
	{
		if (forum.getCategoryId() != other.getCategoryId())
		{
			return null;
		}
		
		if (forum.getOrder() <= 0 || other.getOrder() <= 0)
		{
			return null;
		}
		
		return updateOrderTx(forum, other);
	}
	
	/**
	 * Adds forums groups
	 * 
	 * @param forum	Forum with groups
	 */
	protected void addForumGroups(Forum forum)
	{
		if (forum.getAccessType() != Forum.ForumAccess.GROUPS.getAccessType())
		{
			return;
		}
		
		String sql = "INSERT INTO jforum_forum_sakai_groups (forum_id, sakai_group_id) VALUES (?, ?)";
		
		List<String> groups = forum.getGroups();
			
		if (groups != null && groups.size() > 0)
		{
			for (String groupId : groups)
			{
				Object[] fields = new Object[2];
				int i = 0;
				fields[i++] = forum.getId();
				fields[i++] = groupId;
				
				try
				{
					this.sqlService.dbWrite(sql, fields);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e, e);
					}
					
					throw new RuntimeException("addForumGroups: dbInsert failed");
				}
			}
			
			if (forum.getGroupsReadonyAccessType() == Forum.ForumGroupsReadonlyAccessType.IMMEDIATE.getForumGroupsReadonyAccessType() || forum.getGroupsReadonyAccessType() == Forum.ForumGroupsReadonlyAccessType.DUEDATE.getForumGroupsReadonyAccessType())
			{
				sql = "INSERT INTO jforum_forum_sakai_groups_readonly_access (forum_id, access_type) VALUES (?, ?)";
				
				Object[] fields = new Object[2];
				int i = 0;
				fields[i++] = forum.getId();
				fields[i++] = forum.getGroupsReadonyAccessType();
				
				try
				{
					this.sqlService.dbWrite(sql, fields);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e, e);
					}
					
					throw new RuntimeException("deleteForumGroupsOthersReadonlyAccess: dbInsert failed");
				}
				
			}			
		}
		
		
	}
	
	/**
	 * Created the copy of the forum with the same title but will get "Copied on date" at the end of its title
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return	The id of the newly created forum
	 */
	protected int copyForumTx(final int forumId)
	{
		if (forumId <= 0)
		{
			throw new IllegalArgumentException("Existing forum information is missing.");
		}
		
		final List<Integer> idList = new ArrayList<Integer>();
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					Forum exisForum = selectById(forumId);
					
					if (exisForum == null)
					{
						throw new RuntimeException("No forum existing with id:"+ forumId);
					}
					
					// create forum from existing forum
					Forum toForum = new ForumImpl();
					toForum.setCategoryId(exisForum.getCategoryId());
					toForum.setName(exisForum.getName() + " (Copied "+ DateHelper.formatDateForName(new Date(), null) +")");
					toForum.setDescription(exisForum.getDescription());
					toForum.setType(exisForum.getType());
					toForum.setAccessType(exisForum.getAccessType());
					toForum.setGradeType(exisForum.getGradeType());
					
					// topic order
					toForum.setTopicOrder(exisForum.getTopicOrder());
					
					// forum topic post likes
					toForum.setAllowTopicPostLikes(exisForum.isAllowTopicPostLikes());
					
					// dates
					if (exisForum.getAccessDates() != null)
					{
						AccessDates accessDates = new AccessDatesImpl();
						if (exisForum.getAccessDates().getOpenDate() != null)
						{
							accessDates.setOpenDate(new Date(exisForum.getAccessDates().getOpenDate().getTime()));
							
							accessDates.setHideUntilOpen(exisForum.getAccessDates().isHideUntilOpen().booleanValue());
						}
						
						if (exisForum.getAccessDates().getDueDate() != null)
						{
							accessDates.setDueDate(new Date(exisForum.getAccessDates().getDueDate().getTime()));
						}
						
						if (exisForum.getAccessDates().getAllowUntilDate() != null)
						{
							accessDates.setAllowUntilDate(new Date(exisForum.getAccessDates().getAllowUntilDate().getTime()));
						}
						
						toForum.setAccessDates(accessDates);
					}
					
					// groups
					if ((exisForum.getAccessType() ==  org.etudes.api.app.jforum.Forum.ForumAccess.GROUPS.getAccessType()) && (exisForum.getGroups() != null && exisForum.getGroups().size() > 0))
					{
						toForum.setGroups(new ArrayList<String>(exisForum.getGroups()));
						toForum.setGroupsReadonyAccessType(exisForum.getGroupsReadonyAccessType());
					}
					
					// grade
					if (exisForum.getGradeType() == org.etudes.api.app.jforum.Grade.GradeType.FORUM.getType() && exisForum.getGrade() != null)
					{
						Grade grade = new GradeImpl();
						grade.setContext(exisForum.getGrade().getContext());
						grade.setPoints(exisForum.getGrade().getPoints());
						grade.setMinimumPostsRequired(exisForum.getGrade().isMinimumPostsRequired().booleanValue());
						grade.setMinimumPosts(exisForum.getGrade().getMinimumPosts());
						grade.setAddToGradeBook(exisForum.getGrade().isAddToGradeBook().booleanValue());
						
						toForum.setGrade(grade);
					}
					else if (exisForum.getGradeType() == org.etudes.api.app.jforum.Grade.GradeType.TOPIC.getType())
					{
						toForum.setGradeType(org.etudes.api.app.jforum.Grade.GradeType.TOPIC.getType());
					}
					else
					{
						toForum.setGradeType(org.etudes.api.app.jforum.Grade.GradeType.DISABLED.getType());
					}
					
					int id = creatNewForumTx(toForum);
					idList.add(id);
					
					//change the forum order
					List<Forum> forums = selectByCategoryId(exisForum.getCategoryId());
					
					int exisForumIndex = 0;
					for (Forum forum : forums)
					{
						if (forum.getId() == exisForum.getId())
						{							
							break;
						}
						exisForumIndex++;
					}
					
					if ((exisForumIndex + 1) < forums.size())
					{
						Forum reOrderForum = forums.get(exisForumIndex + 1);
						Forum copiedForum = selectById(id);
						
						if (copiedForum != null && (copiedForum.getId() != reOrderForum.getId()))
						{
							updateOrder(copiedForum, reOrderForum);
						}
					}
					
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					throw new RuntimeException("Error while copying the forum.", e);
				}
			}
		}, "copyForum: " + forumId);
		
		int newForumId = -1;
		
		if (idList.size() == 1)
		{
			newForumId = idList.get(0);
		}
		
		return newForumId;
	}

	/**
	 * Creates new forum and grade if gradable and the forum groups if forum has groups
	 * 
	 * @param forum
	 * @return
	 */
	protected int creatNewForumTx(final Forum forum)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					/*create new forum*/
					int forumId = insertForum(forum);
					((ForumImpl)forum).setId(forumId);
					
					// forum groups
					addForumGroups(forum);
					
					// create grade if forum is gradable.
					if ((forum.getGradeType() == Grade.GradeType.FORUM.getType()) && (forum.getGrade() != null))
					{
						//set the grade context in the impl
						forum.getGrade().setType(Grade.GradeType.FORUM.getType());
						forum.getGrade().setCategoryId(0);
						forum.getGrade().setForumId(forum.getId());
						forum.getGrade().setTopicId(0);
						
						gradeDao.addNew(forum.getGrade());
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					// remove any external entries like entry in grade book if forum is gradable if created
					throw new RuntimeException("Error while creating new forum.", e);
				}
			}
		}, "creatNewForum: " + forum.getName());
		
		
		return forum.getId();
		
	}
	
	/**
	 * Deletes forum groups
	 * 
	 * @param forumId	The forum id
	 */
	protected void deleteForumGroups(int forumId)
	{
		String sql = "DELETE FROM jforum_forum_sakai_groups WHERE forum_id = ?";
		
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = forumId;
		
		try
		{
			this.sqlService.dbWrite(sql, fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("deleteForumGroups: dbWrite failed");
		}
		
		sql = "DELETE FROM jforum_forum_sakai_groups_readonly_access WHERE forum_id = ?";
		
		fields = new Object[1];
		i = 0;
		fields[i++] = forumId;
		
		try
		{
			this.sqlService.dbWrite(sql, fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("deleteForumGroupsOthersReadonlyAccess: dbWrite failed");
		}
	}
	
	/**
	 * deletes the forum and it's special access, groups, grade
	 * @param forum
	 */
	protected void deleteTx(final Forum forum)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					// forum with dates - delete forum special access
					//List<SpecialAccess> forumSpecialAccessList = specialAccessDao.selectByForum(forum.getId());
					List<SpecialAccess> forumSpecialAccessList = forum.getSpecialAccess();
					
					for (SpecialAccess specialAccess : forumSpecialAccessList)
					{
						specialAccessDao.delete(specialAccess.getId());
					}
					
					// gradable forum - delete forum grade
					if (forum.getGradeType() == Grade.GradeType.FORUM.getType())
					{
						Grade grade = forum.getGrade();
						
						if (grade != null)
						{
							//delete grade
							gradeDao.delete(grade.getId());
						}
					}
					
					// delete Forum Groups
					if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
					{
						deleteForumGroups(forum.getId());
					}
					
					// delete forum subscriptions or watches
					removeSubscription(forum.getId());
					
					//delete forum
					String sql = "DELETE FROM jforum_forums WHERE forum_id = ?";
					
					Object[] fields = new Object[1];
					int i = 0;
					fields[i++] =  forum.getId();
					
					sqlService.dbWrite(sql, fields);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}

					throw new RuntimeException("Error while deleting forum.", e);
				}
			}
		}, "delete: " + forum.getName());
	}

	/**
	 * edits forum
	 * 
	 * @param forum		The forum object
	 */
	protected void editForum(Forum forum)
	{
		StringBuilder sql = new StringBuilder();
		
		sql.append("UPDATE jforum_forums SET categories_id = ?, forum_name = ?, forum_desc = ?, start_date = ?, hide_until_open = ?, end_date = ?, allow_until_date = ?, ");
		sql.append("moderated = ?, forum_type = ?, forum_access_type = ?, forum_grade_type = ?, forum_topic_order = ?,  forum_topic_likes = ? WHERE forum_id = ?");
		
		Object[] fields = new Object[14];
		int i = 0;
		fields[i++] = forum.getCategoryId();
		fields[i++] = forum.getName();
		fields[i++] = forum.getDescription();

		if (forum.getAccessDates() != null)
		{
			if (forum.getAccessDates().getOpenDate() == null)
			{
				fields[i++] = null;
				fields[i++] = 0;
			} 
			else
			{
				fields[i++] = new Timestamp(forum.getAccessDates().getOpenDate().getTime());
				fields[i++] = forum.getAccessDates().isHideUntilOpen() ? 1 : 0;
			}
	
			if (forum.getAccessDates().getDueDate() == null)
			{
				fields[i++] = null;
				//fields[i++] = 0;
			} 
			else
			{
				// 08/20/2012 - lock on due is not supported anymore
				fields[i++] = new Timestamp(forum.getAccessDates().getDueDate().getTime());
				//fields[i++] = forum.getAccessDates().isLocked() ? 1 : 0;
			}
			
			if (forum.getAccessDates().getAllowUntilDate() == null)
			{
				fields[i++] = null;
			} 
			else
			{
				fields[i++] = new Timestamp(forum.getAccessDates().getAllowUntilDate().getTime());
			}
		}
		else
		{
			fields[i++] = null;
			fields[i++] = 0;
			fields[i++] = null;
			//fields[i++] = 0;
			fields[i++] = null;
		}

		fields[i++] = 0;
		fields[i++] = forum.getType();
		fields[i++] = forum.getAccessType();
		fields[i++] = forum.getGradeType();
		fields[i++] = forum.getTopicOrder();
		fields[i++] = forum.isAllowTopicPostLikes() ? 1 : 0;
		fields[i++] = forum.getId();
				
		try
		{
			this.sqlService.dbWrite(sql.toString(), fields);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
			
			throw new RuntimeException("editForum: dbWrite failed");
		}
		
	}

	/**
	 * Deletes and adds groups to the forum
	 * 
	 * @param forum
	 */
	protected void editForumGroups(Forum forum)
	{
		// delete forum groups
		deleteForumGroups(forum.getId());
		
		if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
		{		
			// add groups
			addForumGroups(forum);
		}
	}

	/**
	 * Fills the forum object from the result set
	 * 
	 * @param rs	Result set
	 * 
	 * @return	Forum
	 * 
	 * @throws SQLException
	 */
	protected Forum fillForum(ResultSet rs) throws SQLException
	{
		ForumImpl forum = new ForumImpl(this.jforumPostService, this.jforumCategoryService, this.jforumSecurityService, this.jforumGradeService);

		forum.setId(rs.getInt("forum_id"));
		forum.setCategoryId(rs.getInt("categories_id"));
		forum.setName(rs.getString("forum_name"));
		forum.setDescription(rs.getString("forum_desc"));
		forum.setOrder(rs.getInt("forum_order"));
		forum.setTotalTopics(rs.getInt("forum_topics"));
		
		// TODO: last post(er) info
		//forum.setLastPostInfo(lastPostInfo);
		forum.setLastPostId(rs.getInt("forum_last_post_id"));
		forum.setType(rs.getInt("forum_type"));
		forum.setAccessType(rs.getInt("forum_access_type"));
		forum.setGradeType(rs.getInt("forum_grade_type"));
		
		if ((rs.getDate("start_date") != null) || (rs.getDate("end_date") != null) || (rs.getDate("allow_until_date") != null))
		{
			AccessDates accessDates = new AccessDatesImpl();
			accessDates.setOpenDate(rs.getTimestamp("start_date"));
			accessDates.setHideUntilOpen(rs.getInt("hide_until_open") > 0);
			
			if (rs.getDate("end_date") != null)
		    {
		      Timestamp endDate = rs.getTimestamp("end_date");
		      accessDates.setDueDate(endDate);
		      // 08/20/2012 - lock on due is not supported anymore
		      // accessDates.setLocked(rs.getInt("lock_end_date") > 0);
		    }
		    else
		    {
		    	accessDates.setDueDate(null);
		    }
			accessDates.setAllowUntilDate(rs.getTimestamp("allow_until_date"));
			
			forum.setAccessDates(accessDates);
		}
		else
		{
			AccessDates accessDates = new AccessDatesImpl();
			forum.setAccessDates(accessDates);
		}
		
		forum.setTopicOrder(rs.getInt("forum_topic_order"));
		forum.setAllowTopicPostLikes(rs.getInt("forum_topic_likes") > 0);
		
		return forum;
	}
	
	/**
	 * get forum groups
	 * 
	 * @param forum	Forum
	 * 
	 * @return	List of forum groups
	 */
	protected List<String> getForumGroups(int forumId)
	{
		String sql;
		Object[] fields;
		int i = 0;
		
		sql = "SELECT forum_id, sakai_group_id from jforum_forum_sakai_groups where forum_id = ?";						
				
		fields = new Object[1];
		fields[i++] = forumId;
		

		final List<String> groups = new ArrayList<String>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					groups.add(result.getString("sakai_group_id"));

					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("getForumGroups: " + e, e);
					}
					return null;
				}
			}
		});
		
		return groups;
	}
	
	/**
	 * gets the forum groups others ready only access type
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return	The forum groups others ready only access type
	 */
	protected int getForumGroupsReadonlyAccessType(int forumId)
	{
		String sql;
		Object[] fields;
		int i = 0;
		
		sql = "SELECT forum_id, access_type from jforum_forum_sakai_groups_readonly_access where forum_id = ?";						
				
		fields = new Object[1];
		fields[i++] = forumId;
		

		final List<Integer> groupsOthersAccessType = new ArrayList<Integer>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					groupsOthersAccessType.add(result.getInt("access_type"));

					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("getForumGroupsOthersAccessType: " + e, e);
					}
					return null;
				}
			}
		});
		
		int groupsOthersAccessTypeVal = 0;
		
		if (groupsOthersAccessType.size() == 1)
		{
			groupsOthersAccessTypeVal = groupsOthersAccessType.get(0);
		}
		
		return groupsOthersAccessTypeVal;
	}
	
	/**
	 * Gets the maximum display order of the current forums in the database
	 * 
	 * @return	The maximum display order of the forums
	 */
	protected int getMaxDisplayOrder()
	{
		final List<Integer> displayOrderList =  new ArrayList<Integer>();
		
		String sql = "SELECT MAX(forum_order) FROM jforum_forums";

		Object[] fields = null;
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					displayOrderList.add(new Integer(result.getInt(1)));
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("getMaxDisplayOrder: " + e, e);
					}					
				}
				return null;
			}
		});
		
		int maxDisplayOrder = -1;
		if (displayOrderList.size() == 1)
		{
			maxDisplayOrder = displayOrderList.get(0).intValue();
		}
		return maxDisplayOrder;
	}
	
	protected abstract int insertForum(Forum forum);
	
	/**
	 * Get forums
	 * 
	 * @param sql	The query
	 * 
	 * @param fields	Query parameters
	 * 
	 * @return List of forums belong to the course
	 */
	protected List<Forum> readForums(String sql, Object[] fields)
	{
		final List<Forum> forums =  new ArrayList<Forum>();
						
		this.sqlService.dbRead(sql, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Forum forum = fillForum(result);
					
					forum.setTotalPosts(result.getInt("total_posts"));
					
					forums.add(forum);
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("readForums: " + e, e);
					}
					return null;
				}
			}

			
		});		
		
		return forums;
	}
	
	/**
	 * Updates the forum order and adjusts the order for the other forums in the category
	 * 
	 * @param forum			Forum the order to be updated
	 * 
	 * @param related		Related forum in the current position
	 * 
	 * @return		The updated forum
	 */
	protected Forum updateOrderTx(final Forum forum, final Forum related)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					int curOrder = forum.getOrder();
					int newOrder = related.getOrder();					

					forum.setOrder(newOrder);
							
					List<Forum> forums = selectByCategoryId(forum.getCategoryId());
					
					if (curOrder < newOrder)
					{
						// move forums above and including existing forum in new position one level up
						for (Forum f : forums)
						{
							if ((f.getOrder() > curOrder) && (f.getOrder() <= newOrder))
							{
								
								String sql = "UPDATE jforum_forums SET forum_order = ? WHERE forum_id = ?";
								
								Object[] fields = new Object[2];
								int i = 0;
								fields[i++] = f.getOrder() - 1;
								fields[i++] = f.getId();
								
								try
								{
									sqlService.dbWrite(sql, fields);
								}
								catch (Exception e)
								{
									if (logger.isErrorEnabled())
									{
										logger.error(e, e);
									}
									
									throw new RuntimeException("updateOrder: Other forums order update failed");
								}
							}
						}
						
						// actual forum that need to reset it's order
						String sql = "UPDATE jforum_forums SET forum_order = ? WHERE forum_id = ?";
						
						Object[] fields = new Object[2];
						int i = 0;
						fields[i++] = forum.getOrder();
						fields[i++] = forum.getId();
						
						try
						{
							sqlService.dbWrite(sql, fields);
						}
						catch (Exception e)
						{
							if (logger.isErrorEnabled())
							{
								logger.error(e, e);
							}
							
							throw new RuntimeException("updateOrder: forum order update failed");
						}
					}
					else if (curOrder > newOrder)
					{
						// move forums above and including existing forum in new position one level down
						for (Forum f : forums)
						{
							if ((f.getOrder() >= newOrder) && (f.getOrder() < curOrder))
							{
								String sql = "UPDATE jforum_forums SET forum_order = ? WHERE forum_id = ?";
								
								Object[] fields = new Object[2];
								int i = 0;
								fields[i++] = f.getOrder() + 1;
								fields[i++] = f.getId();
								
								try
								{
									sqlService.dbWrite(sql, fields);
								}
								catch (Exception e)
								{
									if (logger.isErrorEnabled())
									{
										logger.error(e, e);
									}
									
									throw new RuntimeException("updateOrder: Other forums order update failed");
								}
							}
						}
						
						// actual forum that need to reset it's order
						String sql = "UPDATE jforum_forums SET forum_order = ? WHERE forum_id = ?";
						
						Object[] fields = new Object[2];
						int i = 0;
						fields[i++] = forum.getOrder();
						fields[i++] = forum.getId();
						
						try
						{
							sqlService.dbWrite(sql, fields);
						}
						catch (Exception e)
						{
							if (logger.isErrorEnabled())
							{
								logger.error(e, e);
							}
							
							throw new RuntimeException("updateOrder: forum order update failed");
						}
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}

					throw new RuntimeException("Error while updating forum.", e);
				}
			}
		}, "updateOrder: " + forum.getName());
		
		return selectById(forum.getId());
	}
	
	/**
	 * Edits forum and it's groups, grade, special access
	 * 
	 * @param forum		Forum object
	 */
	protected void updateTx(final Forum forum)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					// check for existing forum
					Forum exisForum = selectById(forum.getId());
					
					if (exisForum == null)
					{
						return;
					}
					
					Category category = categoryDao.selectById(forum.getCategoryId());
					
					if (category == null)
					{
						return;
					}
					
					// check for exiting forum category and modified forum category and check the dates, grades etc
					if (exisForum.getCategoryId() != forum.getCategoryId())
					{
						if ((category.getAccessDates() != null) && ((category.getAccessDates().getOpenDate() != null) || (category.getAccessDates().getDueDate() != null) || (category.getAccessDates().getAllowUntilDate() != null)))
						{
							forum.setAccessDates(null);
							
							// if category has dates forum topics cannot have dates
							int topicDatesCount = topicDao.selectTopicDatesCountByForum(forum.getId());
							
							if (topicDatesCount > 0)
							{
								List<Topic> topicsWithDates = topicDao.selectTopicsWithDatesByForum(forum.getId());
								
								for (Topic topic: topicsWithDates)
								{
									topic.getAccessDates().setOpenDate(null);
									topic.getAccessDates().setDueDate(null);
									topic.getAccessDates().setAllowUntilDate(null);
									
									topicDao.updateTopic(topic);
								}
							}
						}							
						
						if (category.isGradable())
						{
							forum.setGradeType(Grade.GradeType.DISABLED.getType());
						}
					}
					
					// update forum
					editForum(forum);
					
					// forum groups
					editForumGroups(forum);					
					
					
					//TODO: check evaluations
					
					// forum grade
					if (forum.getGradeType() == Grade.GradeType.FORUM.getType())
					{
						if (forum.getGrade() != null)
						{
							// existing grade
							Grade exisGrade = gradeDao.selectByForum(forum.getId());
							
							if (exisGrade == null)
							{
								//set the grade context in the impl
								forum.getGrade().setType(Grade.GradeType.FORUM.getType());
								forum.getGrade().setCategoryId(0);
								forum.getGrade().setForumId(forum.getId());
								forum.getGrade().setTopicId(0);
								
								gradeDao.addNew(forum.getGrade());
							}
							else
							{
								//set the grade context in the impl
								exisGrade.setType(Grade.GradeType.FORUM.getType());
								exisGrade.setPoints(forum.getGrade().getPoints());
								exisGrade.setContext(forum.getGrade().getContext());
								exisGrade.setAddToGradeBook(forum.getGrade().isAddToGradeBook());
								exisGrade.setMinimumPostsRequired(forum.getGrade().isMinimumPostsRequired());
								exisGrade.setMinimumPosts(forum.getGrade().getMinimumPosts());
								exisGrade.setCategoryId(0);
								exisGrade.setTopicId(0);
								
								gradeDao.updateForumGrade(exisGrade);
								
							}
						}
						else
						{
							// remove forum grade and update forum grade type
							gradeDao.delete(forum.getId(), 0);
							
							forum.setGradeType(Grade.GradeType.DISABLED.getType()); 
							editForum(forum);
						}
					}
					else if (forum.getGradeType() == Grade.GradeType.TOPIC.getType())
					{
						// remove forum grade
						gradeDao.deleteForumGrade(forum.getId());
					}
					else
					{
						// remove forum grade
						gradeDao.deleteForumGrade(forum.getId());
					}
					
					// delete topic grades if category is gradable or forum is gradable
					if (category.isGradable() || (forum.getGradeType() == Grade.GradeType.FORUM.getType()) || forum.getGradeType() == Grade.GradeType.DISABLED.getType())
					{
						List<Grade> topicGrades = gradeDao.selectTopicGradesByForumId(forum.getId());
						
						for (Grade grade : topicGrades)
						{
							gradeDao.delete(grade.getId());
							// update topic for grade type
							Topic topic = topicDao.selectById(grade.getTopicId());
							if (topic != null)
							{
								topic.setGradeTopic(Boolean.FALSE);
								topicDao.updateTopic(topic);
							}
						}					
					}
					
					// remove existing special access if no dates
					if (forum.getAccessDates() == null || ((forum.getAccessDates().getOpenDate() == null) && (forum.getAccessDates().getDueDate() == null) && (forum.getAccessDates().getAllowUntilDate() == null)))
					{
						List<SpecialAccess> forumSpecialAccess = specialAccessDao.selectByForum(forum.getId());
						for (SpecialAccess specialAccess : forumSpecialAccess)
						{
							specialAccessDao.delete(specialAccess.getId());
						}
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					// remove any external entries like entry in grade book if forum is gradable if created
					throw new RuntimeException("Error while updating forum.", e);
				}
			}
		}, "editForum: " + forum.getName());
				
	}
}
