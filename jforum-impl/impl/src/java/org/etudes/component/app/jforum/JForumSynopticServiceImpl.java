/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/JForumSynopticServiceImpl.java $ 
 * $Id: JForumSynopticServiceImpl.java 3638 2012-12-02 21:33:06Z ggolden $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010, 2011, 2012 Etudes, Inc. 
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.JForumCategoryService;
import org.etudes.api.app.jforum.JForumPrivateMessageService;
import org.etudes.api.app.jforum.JForumSynopticService;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;

public class JForumSynopticServiceImpl implements JForumSynopticService
{
	private static Log logger = LogFactory.getLog(JForumSynopticServiceImpl.class);

	/** Dependency: SqlService */
	protected SqlService sqlService = null;

	/** Dependency: ToolManager */
	protected ToolManager toolManager = null;
	
	/** Dependency: SiteService */
	protected SiteService siteService = null;
	
	/** Dependency: JForumPrivateMessageService */
	protected JForumPrivateMessageService jforumPrivateMessageService = null;
	
	/** Dependency: JForumCategoryService */
	protected JForumCategoryService jforumCategoryService = null;
	
	/**
	 * @param jforumCategoryService the jforumCategoryService to set
	 */
	public void setJforumCategoryService(JForumCategoryService jforumCategoryService)
	{
		this.jforumCategoryService = jforumCategoryService;
	}

	/**
	 * @param jforumPrivateMessageService the jforumPrivateMessageService to set
	 */
	public void setJforumPrivateMessageService(JForumPrivateMessageService jforumPrivateMessageService)
	{
		this.jforumPrivateMessageService = jforumPrivateMessageService;
	}

	public static final String ROLE_FACILITATOR="jforum.manage";
	public static final String ROLE_ADMIN="jforum.admin";
	public static final String ROLE_PARTICIPANT="jforum.member";

	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
	}

	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}
	
	public void setSiteService(SiteService siteService)
	{
		this.siteService = siteService;
	}

	public void init()
	{
		if (logger.isInfoEnabled())
			logger.info("init....");
	}

	public void destroy()
	{
		if (logger.isInfoEnabled())
			logger.info("destroy....");
	}

	/* 
	 * @see org.etudes.api.app.jforum.JForumSynopticService#getUserNewPrivateMessageCount
	 */
	public int getUserNewPrivateMessageCount()
	{
		boolean facilitator = isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		boolean participant = isJForumParticipant(UserDirectoryService.getCurrentUser().getId());
		
		if (!facilitator && !participant)
		{
			return 0;
		}
		
		int total = 0;
		
		/*
		PreparedStatement p = null;
		ResultSet rs = null;

		Connection conn = null;
		
		try
		{
			conn = sqlService.borrowConnection();
			User u = getJforumUser(conn);
			
			if (u == null)
			{
				return 0;
			}

			String selectNewPMCount = "SELECT count(*) AS total FROM jforum_privmsgs p, jforum_sakai_course_privmsgs cp	"
					+ "WHERE p.privmsgs_to_userid = ? AND p.privmsgs_type = ? AND p.privmsgs_id = cp.privmsgs_id AND " + "cp.course_id = ?";

			p = conn.prepareStatement(selectNewPMCount);
			p.setInt(1, u.getId());
			p.setInt(2, 1);
			p.setString(3, toolManager.getCurrentPlacement().getContext());

			rs = p.executeQuery();

			total = 0;
			if (rs.next())
			{
				total = rs.getInt("total");
			}

		}
		catch (SQLException e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".getUserNewPrivateMessageCount() : "+ e.getMessage(), e);
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
				}
			}

			if (p != null)
			{
				try
				{
					p.close();
				}
				catch (SQLException e)
				{
				}
			}

			if (conn != null)
			{
				sqlService.returnConnection(conn);
			}
		}*/
		
		total = jforumPrivateMessageService.getUserUnreadCount(toolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());

		return total;
	}
	
	/**
	 * @see org.etudes.api.app.jforum.JForumSynopticService#isUserHasUnreadTopicsAndReplies()
	 */
	public boolean isUserHasUnreadTopicsAndReplies()
	{
		boolean facilitator = isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		boolean participant = isJForumParticipant(UserDirectoryService.getCurrentUser().getId());
		
		if (!facilitator && !participant)
		{
			return false;
		}
		
		boolean unReadTopicsReplies = jforumCategoryService.isUserHasUnreadTopicsAndReplies(toolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());;
		
		/*List<Category> categories = jforumCategoryService.getUserContextCategories(toolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		
		for (Category category : categories)
		{
			for (Forum forum : category.getForums())
			{
				if (forum.isUnread())
				{
					unReadTopicsReplies = true;
					break;
				}
			}
			
			if (unReadTopicsReplies)
			{
				break;
			}
		}*/
		/*Connection conn = null;

		try
		{
			conn = sqlService.borrowConnection();
			User u = getJforumUser(conn);
			
			if (u == null)
			{
				return false;
			}
			
			UserSession userSession = getJforumUserSession(u, conn);
			
			// site categories
			List<Category> categoryList = getSiteCategories(conn);
			if(categoryList.size() == 0)
			{
				return false;			}
			
			get special access for the site
			List<SpecialAccess> siteSpecialAccessList = getSiteSpecialAccess(conn);
			Map<Integer, List<SpecialAccess>> forumSpecialAccessMap = new HashMap<Integer, List<SpecialAccess>>();
			Map<Integer, List<SpecialAccess>> topicSpecialAccessMap = new HashMap<Integer, List<SpecialAccess>>();
			Map<Integer, Category> catMap = new HashMap<Integer, Category>();
			
			for (SpecialAccess specialAccess : siteSpecialAccessList)
			{
				if ((specialAccess.getForumId() > 0) && (specialAccess.getTopicId() == 0))
				{
					if (forumSpecialAccessMap.get(specialAccess.getForumId()) == null)
					{
						List<SpecialAccess> forumSpecialAccessList = new ArrayList<SpecialAccess>();
						forumSpecialAccessList.add(specialAccess);
						forumSpecialAccessMap.put(specialAccess.getForumId(), forumSpecialAccessList);
					}
					else
					{
						List<SpecialAccess> forumSpecialAccessList = forumSpecialAccessMap.get(specialAccess.getForumId());
						forumSpecialAccessList.add(specialAccess);
						forumSpecialAccessMap.put(specialAccess.getForumId(), forumSpecialAccessList);
					}
				}
				else if ((specialAccess.getForumId() > 0) && (specialAccess.getTopicId() > 0))
				{
					if (topicSpecialAccessMap.get(specialAccess.getForumId()) == null)
					{
						List<SpecialAccess> topicSpecialAccessList = new ArrayList<SpecialAccess>();
						topicSpecialAccessList.add(specialAccess);
						topicSpecialAccessMap.put(specialAccess.getForumId(), topicSpecialAccessList);
					}
					else
					{
						List<SpecialAccess> topicSpecialAccessList = topicSpecialAccessMap.get(specialAccess.getForumId());
						topicSpecialAccessList.add(specialAccess);
						topicSpecialAccessMap.put(specialAccess.getForumId(), topicSpecialAccessList);
					}
				}
			}
			
			ArrayList<Category> categoriesNotAccessible =  new ArrayList<Category>();
			ArrayList<Integer> notAccessibleCategoryIds =  new ArrayList<Integer>();
			
			for (Category category:categoryList)
			{
				if (!isCategoryAccessible(u, category))
				{
					categoriesNotAccessible.add(category);
					notAccessibleCategoryIds.add(new Integer(category.getId()));
				}
			}
			
			if (categoriesNotAccessible.size() > 0)
			{
				categoryList.removeAll(categoriesNotAccessible);
			}
			
			// category maps
			for (Category category:categoryList)
			{
				catMap.put(category.getId(), category);
			}
			
			if(categoryList.size() == 0)
			{
				return false;
			}

			List<Forum> siteForums = getSiteForums(conn, notAccessibleCategoryIds);
			if(siteForums == null || siteForums.size() == 0)
			{
				return false;
			}
			else if (userSession == null || userSession.getVisitTime() == null)
			{
				return true;
			}
						
			for (Forum forum:siteForums)
			{
				forum.setSpecialAccessList(forumSpecialAccessMap.get(forum.getId()));
				
				if (!isForumAccessibleToUser(u, forum, catMap))
				{
					continue;
				}
				
				// check for "marked as unread" topics
				//if (checkUnreadMarkedTopics(forum.getId(), u.getId(), topicSpecialAccessMap, conn))
				if (checkUnreadMarkedTopics(forum.getId(), u.getId(), conn))
				{
					return true;
				}
			}
			

			//check for unread posts
			boolean checkunreadTopics = false;
			for (Forum forum:siteForums)
			{
				if (!isForumAccessibleToUser(u, forum, catMap))
				{
					continue;
				}
				
				if (checkUnreadPosts(forum.getId(), topicSpecialAccessMap, userSession, conn))
				{
					checkunreadTopics =  true;
					break;
				}
			}
			
			return checkunreadTopics;
		}
		catch (SQLException e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".isUserHasUnreadTopicsAndReplies() : "+ e.getMessage(), e);
		}
		finally
		{
			if (conn != null)
			{
				sqlService.returnConnection(conn);
			}
		}*/

		return unReadTopicsReplies;
	}
	
	/**
	 * 
	 * @param conn		connection
	 * @param forumId	forum id
	 * @param userId	user id
	 * @return			true  - if user has "marked topics as unread"
	 * 					false - if user has no "marked topics as unread"
	 */
	//protected boolean checkUnreadMarkedTopics(int forumId, int userId, Map<Integer, List<SpecialAccess>> topicSpecialAccessMap, Connection conn)
	/*protected boolean checkUnreadMarkedTopics(int forumId, int userId, Connection conn)
	{
		// ignore the special access to get this count as user may mark as unread if has access to topic 
		PreparedStatement markedTopicsByForumPrepStmt = null;
		//PreparedStatement forumTopicsSpecialAccessCountStmt = null;
		ResultSet rsMarkedTopicsByForum = null;
		//ResultSet rsForumTopicsSpecialAccessCount = null;
		try
		{
			// if topics of the forum have no special access use this statement
			forumTopicsSpecialAccessCountStmt = conn.prepareStatement("SELECT COUNT(1) AS sa_count FROM jforum_special_access WHERE forum_id = ? AND topic_id > 0");
			forumTopicsSpecialAccessCountStmt.setInt(1, forumId);
			rsForumTopicsSpecialAccessCount = forumTopicsSpecialAccessCountStmt.executeQuery();
			boolean forumTopicsSpecialAccess = false;
			if (rsForumTopicsSpecialAccessCount.next())
			{
				if (rsForumTopicsSpecialAccessCount.getInt("sa_count") > 0) 
				{
					forumTopicsSpecialAccess = true;
				}
			}

			List<SpecialAccess> forumTopicsSpecialAccessList = topicSpecialAccessMap.get(forumId);			
			
			if ((forumTopicsSpecialAccessList != null) && (forumTopicsSpecialAccessList.size() > 0))
			{
				 check the current user special access 
				// get forum topics special access
				PreparedStatement forumTopicsSpecialAccessStmt = conn.prepareStatement("SELECT special_access_id, forum_id, topic_id, start_date, end_date, lock_end_date, " +
										"override_start_date, override_end_date, password, users FROM jforum_special_access " +
										"WHERE forum_id = ? AND topic_id > 0");
				forumTopicsSpecialAccessStmt.setInt(1, forumId);
				
				ResultSet rsForumTopicsSpecialAccess = forumTopicsSpecialAccessStmt.executeQuery();
				List<SpecialAccess> specialAccessList = new ArrayList<SpecialAccess>();
				
				while (rsForumTopicsSpecialAccess.next()) 
				{
					specialAccessList.add(this.fillSpecialAccess(rsForumTopicsSpecialAccess));
				}
				GregorianCalendar rightNow = new GregorianCalendar();
				boolean specialAccessUser = false, specialAccessUserAccess = false;
				
				// check current user in the topics special access list
				for (SpecialAccess sa : forumTopicsSpecialAccessList)
				{
					if ((sa.getUserIds().contains(new Integer(userId))))
					{
						specialAccessUser = true;
						
						if (sa.getStartDate() == null)
						{
							specialAccessUserAccess = true;
						}
						else if (rightNow.getTime().after(sa.getStartDate()))
						{
							specialAccessUserAccess = true;
						}
						break;
					}
				}
				
				if (specialAccessUser)
				{
					 
					 * consider topics that have no dates 
					 * topics that have dates and user has no special access
					 * topics that have dates and user has special access
					 * 
					
				}
				else
				{
					markedTopicsByForumPrepStmt = conn.prepareStatement("SELECT count(j.topic_id) AS topics_count FROM " +
							"jforum_topics j, jforum_topics_mark m WHERE j.forum_id = ? " +
							"AND (j.start_date < ? OR j.start_date IS NULL)" +
							"AND j.topic_id = m.topic_id AND m.user_id = ? AND m.is_read = ?");
					markedTopicsByForumPrepStmt.setInt(1, forumId);
					markedTopicsByForumPrepStmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
					markedTopicsByForumPrepStmt.setInt(3, userId);
					markedTopicsByForumPrepStmt.setInt(4, 1);
					
				}
				
				rsMarkedTopicsByForum = markedTopicsByForumPrepStmt.executeQuery();
				if (rsMarkedTopicsByForum.next())
				{
					if (rsMarkedTopicsByForum.getInt("topics_count") > 0) return true;
				}
				
			}
			else
			{
				markedTopicsByForumPrepStmt = conn.prepareStatement("SELECT count(j.topic_id) AS topics_count FROM " +
									"jforum_topics j, jforum_topics_mark m WHERE j.forum_id = ? " +
									//"AND (j.start_date < ? OR j.start_date IS NULL)" +
									"AND j.topic_id = m.topic_id AND m.user_id = ? AND m.is_read = ?");
				markedTopicsByForumPrepStmt.setInt(1, forumId);
				//markedTopicsByForumPrepStmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
				markedTopicsByForumPrepStmt.setInt(2, userId);
				markedTopicsByForumPrepStmt.setInt(3, 1);
				
				rsMarkedTopicsByForum = markedTopicsByForumPrepStmt.executeQuery();
				if (rsMarkedTopicsByForum.next())
				{
					if (rsMarkedTopicsByForum.getInt("topics_count") > 0) return true;
				}
			//}
		}
		catch (SQLException e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".checkUnreadTopics() : "+ e.getMessage(), e);
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".checkUnreadTopics() : "+ e.getMessage(), e);
		}
		finally
		{
			if (rsMarkedTopicsByForum != null)
			{
				try
				{
					rsMarkedTopicsByForum.close();
				}
				catch (SQLException e) {}
				
			}
			if (markedTopicsByForumPrepStmt != null)
			{
				try
				{
					markedTopicsByForumPrepStmt.close();
				}
				catch (SQLException e) {}
			}
		}
		return false;
	}*/
	
	/**
	 * 
	 * @param forumId		forum id
	 * @param userSession	user session
	 * @param conn			connection
	 * @return				true  - if user has unread posts
	 * 						false - if user has no unread posts	
	 */
	/*protected boolean checkUnreadPosts(int forumId, Map<Integer, List<SpecialAccess>> topicSpecialAccessMap, UserSession userSession, Connection conn)
	{
		List<Topic> forumTopics = new ArrayList<Topic>();
		
		List<SpecialAccess> forumTopicsSpecialAccessList = topicSpecialAccessMap.get(forumId);	
		
		boolean facilitator = isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
				
		if (!facilitator && (forumTopicsSpecialAccessList != null) && (forumTopicsSpecialAccessList.size() > 0))
		{   // forum with topic special access
			
			Date currentTime = Calendar.getInstance().getTime();
			
			HashMap<Integer, SpecialAccess> userSpecialAccessAccessMap = new HashMap<Integer, SpecialAccess>();
			HashMap<Integer, SpecialAccess> userSpecialAccessNoAccessMap = new HashMap<Integer, SpecialAccess>();
			
			Map<Integer,Topic> topics = new HashMap<Integer,Topic>();
			
			String selectTopicById = "SELECT topic_id, forum_id, topic_title, user_id, topic_time, topic_replies, topic_status, topic_vote, topic_type, topic_first_post_id, topic_last_post_id, moderated, topic_grade, topic_export, start_date, end_date, lock_end_date FROM jforum_topics WHERE topic_id = ?";
			
			for (SpecialAccess sa : forumTopicsSpecialAccessList)
			{
				if (sa.getUserIds().contains(new Integer(userSession.getUserId())))
				{
					Topic t = topics.get(sa.getTopicId());
					
					if (t == null)
					{
						PreparedStatement topicStmt = null;
						ResultSet rsTopic = null;
						try
						{
							topicStmt = conn.prepareStatement(selectTopicById);
							topicStmt.setInt(1, sa.getTopicId());
							
							rsTopic = topicStmt.executeQuery();
							
							if (rsTopic.next()) 
							{
								t = new Topic();
								t.setId(rsTopic.getInt("topic_id"));
														
								if (rsTopic.getDate("start_date") != null)
								{
								  Timestamp startDate = rsTopic.getTimestamp("start_date");
								  t.setStartDate(startDate);
								}
								
								if (rsTopic.getDate("end_date") != null)
							    {
							      Timestamp endDate = rsTopic.getTimestamp("end_date");
								  t.setEndDate(endDate);
							    }
								
								t.setLockTopic(rsTopic.getInt("lock_end_date") > 0);
								
								topics.put(new Integer(t.getId()), t);
							}
						} catch (SQLException e)
						{
							if (logger.isWarnEnabled()) 
								logger.warn(this.getClass().getName() +".checkUnreadTopics() : "+ e.getMessage(), e);
						}
						finally
						{
							if (rsTopic != null)
							{
								try
								{
									rsTopic.close();
								}
								catch (SQLException e)
								{
								}
							}
				
							if (topicStmt != null)
							{
								try
								{
									topicStmt.close();
								}
								catch (SQLException e)
								{
								}
							}
						}
					}
					
					if (!sa.isOverrideStartDate())
					{
						sa.setStartDate(t.getStartDate());
					}
					if (!sa.isOverrideEndDate())
					{
						sa.setEndDate(t.getEndDate());
					}
					if (!sa.isOverrideLockEndDate())
					{
						sa.setLockOnEndDate(t.isLockTopic());
					}
					
					if (sa.getStartDate() == null)
					{
						userSpecialAccessAccessMap.put(new Integer(sa.getTopicId()), sa);
					}
					else if (currentTime.after(sa.getStartDate()))
					{
						userSpecialAccessAccessMap.put(new Integer(sa.getTopicId()), sa);
					}
					else
					{
						userSpecialAccessNoAccessMap.put(new Integer(sa.getTopicId()), sa);
					}
				}
			}
			
			String selectUserMarkAllReadtime = "SELECT course_id, user_id, visit_time, markall_time FROM jforum_sakai_sessions WHERE course_id=? AND user_id = ?";
			PreparedStatement selectUserMarkAllReadTimeStmt = null;
			ResultSet selectUserMarkAllReadTimeRs = null;
			
			String selectMaxPostTimeByTopicWithMarkTime = "SELECT MAX(p.post_time), p.topic_id, t.start_date, t.end_date FROM jforum_posts p, jforum_topics t WHERE p.forum_id = ? AND p.post_time > ? " +
							" AND p.topic_id = t.topic_id GROUP BY topic_id";
			
			String selectMaxPostTimeByTopic = "SELECT MAX(p.post_time), p.topic_id, t.start_date, t.end_date FROM jforum_posts p, jforum_topics t WHERE p.forum_id = ? " +
							"AND p.topic_id = t.topic_id GROUP BY topic_id";
			PreparedStatement p = null;
			ResultSet rs = null;
			
			String selectUserTopicVisittime = "SELECT topic_id, user_id, mark_time, is_read FROM jforum_topics_mark WHERE topic_id = ? AND user_id = ? AND is_read = 0";
			PreparedStatement selectUserTopicVisittimeStmt = null;
			ResultSet selectUserTopicVisittimeRs = null;		
			
			try
			{
				Date userMarkAllTime = null;
				
				selectUserMarkAllReadTimeStmt = conn.prepareStatement(selectUserMarkAllReadtime);
				selectUserMarkAllReadTimeStmt.setString(1, toolManager.getCurrentPlacement().getContext());
				selectUserMarkAllReadTimeStmt.setInt(2, userSession.getUserId());
				selectUserMarkAllReadTimeRs = selectUserMarkAllReadTimeStmt.executeQuery();
				
				if (selectUserMarkAllReadTimeRs.next())
				{
					if (selectUserMarkAllReadTimeRs.getTimestamp("markall_time") != null)
					{
						userMarkAllTime = new Date(selectUserMarkAllReadTimeRs.getTimestamp("markall_time").getTime());
					}
				}
				
				if (userMarkAllTime == null)
				{
					p = conn.prepareStatement(selectMaxPostTimeByTopic);
					p.setInt(1, forumId);
				}
				else
				{
					p = conn.prepareStatement(selectMaxPostTimeByTopicWithMarkTime);
					p.setInt(1, forumId);
					p.setTimestamp(2, new Timestamp(userMarkAllTime.getTime()));
				}
							
				rs = p.executeQuery();
	
				while (rs.next()) 
				{
					Topic t = new Topic();
					t.setId(rs.getInt("topic_id"));
					t.setMaxPostTime(new Date(rs.getTimestamp(1).getTime()));
					
					if (rs.getDate("start_date") != null)
					{
					  Timestamp startDate = rs.getTimestamp("start_date");
					  t.setStartDate(startDate);
					}
					
					if (rs.getDate("end_date") != null)
				    {
				      Timestamp endDate = rs.getTimestamp("end_date");
					  t.setEndDate(endDate);
				    }
					
					if (userSpecialAccessNoAccessMap.containsKey(t.getId()))
					{
						continue;
					}
					else if (userSpecialAccessAccessMap.containsKey(t.getId()))
					{
						forumTopics.add(t);
					}
					else if (t.getStartDate() == null || t.getStartDate().before(currentTime))
					{
						forumTopics.add(t);
					}
				}
				
				if (forumTopics.size() == 0) 
				{
					return false;
				}
										
				boolean topicUnread = false;
				for (Topic topic:forumTopics)
				{
					Date userTopicVisitMarkTime = null;
					 check latest post time of the topic with entry in jforum_topics_mark table.
					 * There will be no entry if the user is not visited the topic. There will be entry in the table
					 * if the user is visited the topic and there may be new replies after that.
					 * 
					 * check latest post time of the topic with mark all time in the jforum_sakai_sessions
					 
					selectUserTopicVisittimeStmt = conn.prepareStatement(selectUserTopicVisittime);
					selectUserTopicVisittimeStmt.setInt(1, topic.getId());
					selectUserTopicVisittimeStmt.setInt(2, userSession.getUserId());
					selectUserTopicVisittimeRs = selectUserTopicVisittimeStmt.executeQuery();
									
					if (selectUserTopicVisittimeRs.next())
					{
						Date topicVisitTime = new Date(selectUserTopicVisittimeRs.getTimestamp("mark_time").getTime());
						userTopicVisitMarkTime = topicVisitTime;
					}
					
					if (userTopicVisitMarkTime == null)
					{
						userTopicVisitMarkTime = userMarkAllTime;
					}
					
					if((userMarkAllTime != null) && (userTopicVisitMarkTime.compareTo(userMarkAllTime) < 0))
					{
						userTopicVisitMarkTime = userMarkAllTime;
					}
					
					if (userTopicVisitMarkTime == null)
					{
						topicUnread = true;
						break;
					}
					else
					{
						if (topic.getMaxPostTime().compareTo(userTopicVisitMarkTime) > 0)
						{
							topicUnread = true;
							break;
						}
					}
				}
				
				if (topicUnread)
				{
					return true;
				}
			}
			catch (SQLException e)
			{
				if (logger.isWarnEnabled()) 
					logger.warn(this.getClass().getName() +".checkUnreadTopics() : "+ e.getMessage(), e);
			}
			finally
			{
				if (selectUserMarkAllReadTimeRs != null)
				{
					try
					{
						selectUserMarkAllReadTimeRs.close();
					}
					catch (SQLException e)
					{
					}
				}
	
				if (selectUserMarkAllReadTimeStmt != null)
				{
					try
					{
						selectUserMarkAllReadTimeStmt.close();
					}
					catch (SQLException e)
					{
					}
				}
				
				
				if (selectUserTopicVisittimeRs != null)
				{
					try
					{
						selectUserTopicVisittimeRs.close();
					}
					catch (SQLException e)
					{
					}
				}
	
				if (selectUserTopicVisittimeStmt != null)
				{
					try
					{
						selectUserTopicVisittimeStmt.close();
					}
					catch (SQLException e)
					{
					}
				}
				
				if (rs != null)
				{
					try
					{
						rs.close();
					}
					catch (SQLException e)
					{
					}
				}
	
				if (p != null)
				{
					try
					{
						p.close();
					}
					catch (SQLException e)
					{
					}
				}
			}			
		}
		else
		{ //no topic special access forum
		
			String selectUserMarkAllReadtime = "SELECT course_id, user_id, visit_time, markall_time FROM jforum_sakai_sessions WHERE course_id=? AND user_id = ?";
			PreparedStatement selectUserMarkAllReadTimeStmt = null;
			ResultSet selectUserMarkAllReadTimeRs = null;
			
			String selectMaxPostTimeByTopicWithMarkTime = null;
			String selectMaxPostTimeByTopic = null;
			
			if (facilitator)
			{
				selectMaxPostTimeByTopicWithMarkTime = "SELECT MAX(p.post_time), p.topic_id FROM jforum_posts p, jforum_topics t WHERE p.forum_id = ? AND p.post_time > ? " +
							" AND p.topic_id = t.topic_id GROUP BY topic_id";
			}
			else
			{
				selectMaxPostTimeByTopicWithMarkTime = "SELECT MAX(p.post_time), p.topic_id FROM jforum_posts p, jforum_topics t WHERE p.forum_id = ? AND p.post_time > ? " +
				" AND p.topic_id = t.topic_id AND (t.start_date < ? OR t.start_date IS NULL) GROUP BY topic_id";
			}
			
			if (facilitator)
			{
				selectMaxPostTimeByTopic = "SELECT MAX(p.post_time), p.topic_id FROM jforum_posts p, jforum_topics t WHERE p.forum_id = ? " +
							"AND p.topic_id = t.topic_id GROUP BY topic_id";
			}
			else
			{
				selectMaxPostTimeByTopic = "SELECT MAX(p.post_time), p.topic_id FROM jforum_posts p, jforum_topics t WHERE p.forum_id = ? " +
							"AND p.topic_id = t.topic_id AND (t.start_date < ? OR t.start_date IS NULL) GROUP BY topic_id";
			}
			
			PreparedStatement p = null;
			ResultSet rs = null;
			
			String selectUserTopicVisittime = "SELECT topic_id, user_id, mark_time, is_read FROM jforum_topics_mark WHERE topic_id = ? AND user_id = ? AND is_read = 0";
			PreparedStatement selectUserTopicVisittimeStmt = null;
			ResultSet selectUserTopicVisittimeRs = null;		
			
			try
			{
				Date userMarkAllTime = null;
				
				selectUserMarkAllReadTimeStmt = conn.prepareStatement(selectUserMarkAllReadtime);
				selectUserMarkAllReadTimeStmt.setString(1, toolManager.getCurrentPlacement().getContext());
				selectUserMarkAllReadTimeStmt.setInt(2, userSession.getUserId());
				selectUserMarkAllReadTimeRs = selectUserMarkAllReadTimeStmt.executeQuery();
				
				if (selectUserMarkAllReadTimeRs.next())
				{
					if (selectUserMarkAllReadTimeRs.getTimestamp("markall_time") != null)
					{
						userMarkAllTime = new Date(selectUserMarkAllReadTimeRs.getTimestamp("markall_time").getTime());
					}
				}
				
				if (userMarkAllTime == null)
				{
					p = conn.prepareStatement(selectMaxPostTimeByTopic);
					p.setInt(1, forumId);
					if (!facilitator)
					{
						p.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
					}					
				}
				else
				{
					p = conn.prepareStatement(selectMaxPostTimeByTopicWithMarkTime);
					p.setInt(1, forumId);
					p.setTimestamp(2, new Timestamp(userMarkAllTime.getTime()));
					
					if (!facilitator)
					{
						p.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
					}
				}
							
				rs = p.executeQuery();
	
				while (rs.next()) 
				{
					Topic t = new Topic();
					t.setId(rs.getInt("topic_id"));
					t.setMaxPostTime(new Date(rs.getTimestamp(1).getTime()));
					
					forumTopics.add(t);
				}
				
				if (forumTopics.size() == 0) 
				{
					return false;
				}
										
				boolean topicUnread = false;
				for (Topic topic:forumTopics)
				{
					Date userTopicVisitMarkTime = null;
					 check latest post time of the topic with entry in jforum_topics_mark table.
					 * There will be no entry if the user is not visited the topic. There will be entry in the table
					 * if the user is visited the topic and there may be new replies after that.
					 * 
					 * check latest post time of the topic with mark all time in the jforum_sakai_sessions
					 
					selectUserTopicVisittimeStmt = conn.prepareStatement(selectUserTopicVisittime);
					selectUserTopicVisittimeStmt.setInt(1, topic.getId());
					selectUserTopicVisittimeStmt.setInt(2, userSession.getUserId());
					selectUserTopicVisittimeRs = selectUserTopicVisittimeStmt.executeQuery();
									
					if (selectUserTopicVisittimeRs.next())
					{
						Date topicVisitTime = new Date(selectUserTopicVisittimeRs.getTimestamp("mark_time").getTime());
						userTopicVisitMarkTime = topicVisitTime;
					}
					
					if (userTopicVisitMarkTime == null)
					{
						userTopicVisitMarkTime = userMarkAllTime;
					}
					
					if((userMarkAllTime != null) && (userTopicVisitMarkTime.compareTo(userMarkAllTime) < 0))
					{
						userTopicVisitMarkTime = userMarkAllTime;
					}
					
					if (userTopicVisitMarkTime == null)
					{
						topicUnread = true;
						break;
					}
					else
					{
						if (topic.getMaxPostTime().compareTo(userTopicVisitMarkTime) > 0)
						{
							topicUnread = true;
							break;
						}
					}
				}
				
				if (topicUnread)
				{
					return true;
				}
			}
			catch (SQLException e)
			{
				if (logger.isWarnEnabled()) 
					logger.warn(this.getClass().getName() +".checkUnreadTopics() : "+ e.getMessage(), e);
			}
			finally
			{
				if (selectUserMarkAllReadTimeRs != null)
				{
					try
					{
						selectUserMarkAllReadTimeRs.close();
					}
					catch (SQLException e)
					{
					}
				}
	
				if (selectUserMarkAllReadTimeStmt != null)
				{
					try
					{
						selectUserMarkAllReadTimeStmt.close();
					}
					catch (SQLException e)
					{
					}
				}
				
				
				if (selectUserTopicVisittimeRs != null)
				{
					try
					{
						selectUserTopicVisittimeRs.close();
					}
					catch (SQLException e)
					{
					}
				}
	
				if (selectUserTopicVisittimeStmt != null)
				{
					try
					{
						selectUserTopicVisittimeStmt.close();
					}
					catch (SQLException e)
					{
					}
				}
				
				if (rs != null)
				{
					try
					{
						rs.close();
					}
					catch (SQLException e)
					{
					}
				}
	
				if (p != null)
				{
					try
					{
						p.close();
					}
					catch (SQLException e)
					{
					}
				}
			}
		}
		
		return false;
	}*/

	/**
	 * gets jforum user
	 * @param conn	Connection
	 * @return 		Jforum user
	 */
	/*protected User getJforumUser(Connection conn)
	{
		org.sakaiproject.user.api.User sakUser = UserDirectoryService.getCurrentUser();

		String selectBySakaiUserId = "SELECT user_id, user_fname, user_lname, sakai_user_id FROM jforum_users WHERE sakai_user_id = ?";

		User user = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try
		{
			p = conn.prepareStatement(selectBySakaiUserId);
			p.setString(1, sakUser.getId());

			rs = p.executeQuery();

			if (rs.next())
			{
				try
				{
					user = fillUserFromResultSet(rs);
				}
				catch (Exception e)
				{
					if (logger.isWarnEnabled()) 
						logger.warn(this.getClass().getName() +".getJforumUser() : "+ e.getMessage(), e);
				}
			}

		}
		catch (SQLException e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".getJforumUser() : "+ e.getMessage(), e);
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
				}
			}

			if (p != null)
			{
				try
				{
					p.close();
				}
				catch (SQLException e)
				{
				}
			}
		}
		return user;
	}*/
	
	/**
	 * gets jforum user session
	 * @param conn Connection
	 * @return jforum user session
	 */
	/*protected UserSession getJforumUserSession(User user, Connection conn)
	{
		UserSession userSession = null;
		String selectUserSession = "SELECT course_id, user_id, visit_time, markall_time FROM jforum_sakai_sessions WHERE user_id = ? AND course_id = ?";

		PreparedStatement p = null;
		ResultSet rs = null;
		try
		{
			p = conn.prepareStatement(selectUserSession);
			p.setInt(1, user.getId());
			p.setString(2, toolManager.getCurrentPlacement().getContext());

			rs = p.executeQuery();

			if (rs.next())
			{
				userSession = new UserSession();
				userSession.setUserId(rs.getInt("user_id"));
				userSession.setCourseId(rs.getString("course_id"));
				userSession.setVisitTime(rs.getTimestamp("visit_time"));
				userSession.setMarkAllTime(rs.getTimestamp("markall_time"));
			}

		}
		catch (SQLException e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".getJforumUserSession() : "+ e.getMessage(), e);
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
				}
			}

			if (p != null)
			{
				try
				{
					p.close();
				}
				catch (SQLException e)
				{
				}
			}
		}
		return userSession;
	}*/
	
	/**
	 * gets site categories
	 * @param conn Connection
	 * @return list of site categories
	 */
	/*protected List<Category> getSiteCategories(Connection conn)
	{
		List<Category> categoryList =  new ArrayList<Category>();
		
		String selectAllByCourseId = "SELECT a.categories_id, a.title, a.display_order, a.moderated, a.gradable, a.start_date, a.end_date, " +
				" a.lock_end_date FROM jforum_categories a, jforum_sakai_course_categories b WHERE a.categories_id = b.categories_id and b.course_id = ? ORDER BY a.display_order";

		PreparedStatement p = null;
		ResultSet rs = null;
		try
		{
			p = conn.prepareStatement(selectAllByCourseId);
			p.setString(1, toolManager.getCurrentPlacement().getContext());

			rs = p.executeQuery();
			
			while (rs.next())
			{
				Category c = getCategory(rs);
				categoryList.add(c);
			}

		}
		catch (SQLException e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".getSiteCategories() : "+ e.getMessage(), e);
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".getSiteCategories() : "+ e.getMessage(), e);
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
				}
			}

			if (p != null)
			{
				try
				{
					p.close();
				}
				catch (SQLException e)
				{
				}
			}
		}
		return categoryList;
	}*/
	
	/**
	 * get category from the resultset
	 * @param rs	resulset
	 * @return		category
	 */
	/*protected Category getCategory(ResultSet rs) throws Exception
	{
		Category c = new Category();
		
		c.setId(rs.getInt("categories_id"));
		c.setName(rs.getString("title"));
		c.setGradeCategory(rs.getInt("gradable") == 1);
		
		if (rs.getDate("start_date") != null)
		{
		  Timestamp startDate = rs.getTimestamp("start_date");
		  c.setStartDate(startDate);
	    }
	    else
	    {
		  c.setStartDate(null);
	    }
	    if (rs.getDate("end_date") != null)
	    {
	      Timestamp endDate = rs.getTimestamp("end_date");
		  c.setEndDate(endDate);
		  
		  c.setLockCategory(rs.getInt("lock_end_date") > 0);
	    }
	    else
	    {
			c.setEndDate(null);
	    }
		
		return c;
	}*/

	/**
	 * gets site forums
	 * @param conn Connection
	 * @return The site forums
	 */
	/*protected List<Forum> getSiteForums(Connection conn, List<Integer> notAccessibleCategoryIds)
	{
		List<Forum> forumList =  new ArrayList<Forum>();
		
		String selectAllForumsByCourseId = "SELECT f.* FROM jforum_sakai_course_categories c, jforum_categories jc, jforum_forums f" +
		" WHERE c.categories_id = jc.categories_id AND f.categories_id = c.categories_id AND c.course_id = ? "+
		" ORDER BY f.categories_id, f.forum_order ";

		PreparedStatement p = null;
		ResultSet rs = null;
		try
		{
			p = conn.prepareStatement(selectAllForumsByCourseId);
			p.setString(1, toolManager.getCurrentPlacement().getContext());

			rs = p.executeQuery();
			
			while (rs.next()) 
			{
				
				Forum f = new Forum();
				f = this.fillForum(rs);
				
				if (notAccessibleCategoryIds.contains(new Integer(f.getCategoriesId())))
				{
					continue;
				}
				
				if (f.getAccessType() == Forum.ACCESS_GROUPS)
				{
					PreparedStatement frmGrpPrepStmt = null;
					ResultSet rsFrmGrps = null;
					try
					{
						frmGrpPrepStmt = conn.prepareStatement("SELECT forum_id, sakai_group_id from jforum_forum_sakai_groups where forum_id = ?");
						frmGrpPrepStmt.setInt(1, f.getId());
						rsFrmGrps = frmGrpPrepStmt.executeQuery();
						List<String> selectedGrpList = new ArrayList<String>();
						while (rsFrmGrps.next())
						{
							selectedGrpList.add(rsFrmGrps.getString("sakai_group_id"));
						}

						f.setGroups(selectedGrpList);
					}
					catch (SQLException e)
					{
						if (logger.isWarnEnabled()) 
							logger.warn(this.getClass().getName() +".getSiteForums() : "+ e.getMessage(), e);
					}
					finally
					{
						if (rsFrmGrps != null)
						{
							try
							{
								rsFrmGrps.close();
							}
							catch (SQLException e) {}
						}
						if (frmGrpPrepStmt != null)
						{
							try
							{
								frmGrpPrepStmt.close();
							}
							catch (SQLException e) {}
						}
					}
				}
				else
				{
					f.setGroups(new ArrayList<String>());
				}
				
				forumList.add(f);
				
			}
		}
		catch (SQLException e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".getSiteForums() : "+ e.getMessage(), e);
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".getSiteForums() : "+ e.getMessage(), e);
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
				}
			}

			if (p != null)
			{
				try
				{
					p.close();
				}
				catch (SQLException e)
				{
				}
			}
		}
		return forumList;
	}*/
	
	/**
	 * Gets the site special access
	 * @param conn	connection
	 * @return 		The site special access
	 */
	/*protected List<SpecialAccess> getSiteSpecialAccess(Connection conn)
	{

		List<SpecialAccess> specialAccessList = new ArrayList<SpecialAccess>();
		
		PreparedStatement p = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT  sa.special_access_id, sa.forum_id, sa.topic_id, sa.start_date, sa.end_date, sa.lock_end_date, " +
					"sa.override_start_date, sa.override_end_date,  sa.override_lock_end_date, sa.password, sa.users " +
					"FROM jforum_special_access sa, jforum_sakai_course_categories cc, jforum_forums f " +
					"WHERE cc.course_id = ? AND cc.categories_id = f.categories_id " +
					"AND f.forum_id = sa.forum_id ORDER BY sa.forum_id";
			
			p = conn.prepareStatement(sql);
			
			p.setString(1, toolManager.getCurrentPlacement().getContext());
			
			rs = p.executeQuery();
			while (rs.next()) 
			{
				specialAccessList.add(this.fillSpecialAccess(rs));
			}

		}
		catch (SQLException e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".getSiteSpecialAccess() : "+ e.getMessage(), e);
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled()) 
				logger.warn(this.getClass().getName() +".getSiteSpecialAccess() : "+ e.getMessage(), e);
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
				}
			}

			if (p != null)
			{
				try
				{
					p.close();
				}
				catch (SQLException e)
				{
				}
			}
		}
		
		return specialAccessList;
	}*/
	
	/**
	 * if forum has groups check to see if the forum is accessible to user
	 * @param forum		forum id
	 * @return			true  - if user is in the forum groups
	 * 					false - if user is not in the forum groups
	 */
	/*protected boolean isForumAccessibleToUser(User user, Forum forum, Map<Integer, Category> catMap)
	{
		if (isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser())
			return true;
		
		GregorianCalendar rightNow = new GregorianCalendar();
		if (forum.getAccessType() == Forum.ACCESS_DENY)
		{
			return false;
		}
		else if (forum.getAccessType() == Forum.ACCESS_GROUPS)
		{
			if (forum.getGroups() != null)
			{
				try
				{
					
					Site site = siteService.getSite(toolManager.getCurrentPlacement().getContext());
					Collection<org.sakaiproject.site.api.Group> userGroups = site.getGroupsWithMember(UserDirectoryService.getCurrentUser().getId());
					
					for (org.sakaiproject.site.api.Group grp : userGroups) 
					{
						if (forum.getGroups().contains(grp.getId()))
						{
							boolean specialAccessUser = false, specialAccessUserAccess = false;
							
							// check for forum group special access
							List<SpecialAccess> specialAccessList = forum.getSpecialAccessList();
						
							if ((specialAccessList != null) && (specialAccessList.size() > 0))
							{
								for (SpecialAccess sa : specialAccessList)
								{
									if ((sa.getUserIds().contains(new Integer(user.getId()))))
									{
										specialAccessUser = true;
										
										if (!sa.isOverrideStartDate())
										{
											sa.setStartDate(forum.getStartDate());
										}
										if (!sa.isOverrideEndDate())
										{
											sa.setEndDate(forum.getEndDate());
										}
										if (!sa.isOverrideLockEndDate())
										{
											sa.setLockOnEndDate(forum.isLockForum());
										}
										
										if (sa.getStartDate() == null)
										{
											specialAccessUserAccess = true;
										}
										else if (rightNow.getTime().after(sa.getStartDate()))
										{
											specialAccessUserAccess = true;
										}
										break;
									}
								}
							}
							
							if (specialAccessUser)
							{
								if (specialAccessUserAccess)
								{
									return true;
								}
								return false;
							}
							else
							{
								if (forum.getStartDate() != null)
								{
									if (forum.getStartDate().after(new Date()))
									{
										return false;
									}
									return true;
								}
								else if (forum.getEndDate() == null)
								{
									// check category dates if any
									Category category = catMap.get(forum.getCategoriesId());
									
									if (category != null)
									{
										if ((category.getStartDate() != null) && (category.getStartDate().after(new Date())))
										{
											return false;
										}
										return true;
									}
									else
									{
										return false;
									}
								}
							}
						}
					}
				}
				catch (IdUnusedException e)
				{
					if (logger.isWarnEnabled()) logger.warn(this.getClass().getName() +".isForumAccessibleToUser() : "+ e.getMessage(), e);
					return false;
				}
			}
			else
			{
				return false;
			}
		}
		else 
		{
			// check for forum special access
			List<SpecialAccess> specialAccessList = forum.getSpecialAccessList();
			
			boolean specialAccessUser = false, specialAccessUserAccess = false;
			if ((specialAccessList != null) && (specialAccessList.size() > 0))
			{
				for (SpecialAccess sa : specialAccessList)
				{
					if (sa.getUserIds().contains(new Integer(user.getId())))
					{
						specialAccessUser = true;
						
						if (!sa.isOverrideStartDate())
						{
							sa.setStartDate(forum.getStartDate());
						}
						if (!sa.isOverrideEndDate())
						{
							sa.setEndDate(forum.getEndDate());
						}
						if (!sa.isOverrideLockEndDate())
						{
							sa.setLockOnEndDate(forum.isLockForum());
						}
						
						if (sa.getStartDate() == null)
						{
							specialAccessUserAccess = true;
						}
						else if (rightNow.getTime().after(sa.getStartDate()))
						{
							specialAccessUserAccess = true;
						}
						break;
					}
				}
			}
			
			if (specialAccessUser)
			{
				if (specialAccessUserAccess)
				{
					return true;
				}
			}
			else
			{
				if (forum.getStartDate() != null)
				{
					if (forum.getStartDate().after(new Date()))
					{
						return false;
					}
					return true;
				}
				else if (forum.getEndDate() == null)
				{
					// check category dates if any
					Category category = catMap.get(forum.getCategoriesId());
					
					if (category != null)
					{
						if ((category.getStartDate() != null) && (category.getStartDate().after(new Date())))
						{
							return false;
						}
						return true;
					}
					else
					{
						return false;
					}
				}
			}
		}	
		
		return false;
	}*/
	
	/**
	 * Check if category is accessible.
	 * 
	 * @param category	category
	 * @param user 		user
	 * @return 	true	if access to the category is allowed. 
	 */
	/*protected boolean isCategoryAccessible(User user, Category category)
	{
		if (category == null)
			throw new IllegalArgumentException();
		
		if (isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser())
			return true;
		
		GregorianCalendar gc = new GregorianCalendar();
		Date nowDate = gc.getTime();
		
		if (category.getStartDate() != null)
		{
			if (nowDate.getTime() < category.getStartDate().getTime())
			{
				return false;
			}
		}
		
		return true;
	}*/
		
	/**
	 * fill special access object form the resultset
	 * @param rs			resultset
	 * @return				special access object
	 * @throws Exception
	 */
	/*protected SpecialAccess fillSpecialAccess(ResultSet rs) throws Exception
	{
		SpecialAccess specialAccess = new SpecialAccess();
		 
		specialAccess.setId(rs.getInt("special_access_id"));
		specialAccess.setForumId(rs.getInt("forum_id"));
		specialAccess.setTopicId(rs.getInt("topic_id"));
		
		if (rs.getDate("start_date") != null)
		{
		  Timestamp startDate = rs.getTimestamp("start_date");
		  specialAccess.setStartDate(startDate);
	    }
	    else
	    {
	    	specialAccess.setStartDate(null);
	    }
		
		if (rs.getDate("end_date") != null)
	    {
	      Timestamp endDate = rs.getTimestamp("end_date");
	      specialAccess.setEndDate(endDate);
		}
	    else
	    {
	    	specialAccess.setEndDate(null);
	    }
		
		specialAccess.setOverrideStartDate(rs.getInt("override_start_date") == 1);
		specialAccess.setOverrideEndDate(rs.getInt("override_end_date") == 1);
		specialAccess.setOverrideLockEndDate(rs.getInt("override_lock_end_date") == 1);
		
		specialAccess.setLockOnEndDate(rs.getInt("lock_end_date") > 0);
		
		List<Integer> userIds = getUserIdList(rs.getString("users"));
		specialAccess.setUserIds(userIds);
		
		return specialAccess;
	}*/
	
	/**
	 * get userid list from the string
	 * @param userIds
	 * @return list of userid's
	 */
	/*protected List<Integer> getUserIdList(String userIds)
	{
		if ((userIds == null) || (userIds.trim().length() == 0)) 
		{
			return null;
		}
		List<Integer> userIdList = new ArrayList<Integer>();
		
		String[] userIdsArray = userIds.split(":");
		if ((userIdsArray != null) && (userIdsArray.length > 0))
		{
			for (String userId : userIdsArray)
			{
				userIdList.add(new Integer(userId));
			}
		}
		
		return userIdList;
	}*/
	
	/**
	 * checks if user is jforum facilitator
	 * @param sakaiUserId
	 * @return true if jforum facilitator
	 * 		   false if not jforum facilitator
	 */
	protected boolean isJForumFacilitator(String sakaiUserId) {
		try {
			org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(sakaiUserId);
			String thisSiteId = toolManager.getCurrentPlacement().getContext();
			String realmName = "/site/" + thisSiteId;
		    return SecurityService.unlock(sakUser, ROLE_FACILITATOR, realmName);

		} catch (UserNotDefinedException e) {
			return false;
		}
	}
	
	/**
	 * checks if user is jforum participant
	 * @param sakaiUserId
	 * @return true if jforum participant
	 * 		   false if not jforum participant
	 */
	protected boolean isJForumParticipant(String sakaiUserId) {
		try {
			org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(sakaiUserId);
			String thisSiteId = toolManager.getCurrentPlacement().getContext();
			String realmName = "/site/" + thisSiteId;
		    return SecurityService.unlock(sakUser, ROLE_PARTICIPANT, realmName);

		} catch (UserNotDefinedException e) {
			return false;
		}
	}
	
	/**
	 * 
	 * @param rs Result set
	 * @throws Exception 
	 */
	/*protected User fillUserFromResultSet(ResultSet rs) throws Exception
	{
		User u = new User();
		
		u.setId(rs.getInt("user_id"));
		u.setFirstName(rs.getString("user_fname") == null ? "" : rs.getString("user_fname"));
		u.setLastName(rs.getString("user_lname") == null ? "" : rs.getString("user_lname"));
		u.setSakaiUserId(rs.getString("sakai_user_id"));
		
		return u;
	}
	
	*//**
	 * 
	 * @param rs Result set
	 * @throws Exception 
	 *//*
	protected Forum fillForum(ResultSet rs) throws Exception
	{
		Forum forum = new Forum();
		
		forum.setId(rs.getInt("forum_id"));
		forum.setCategoriesId(rs.getInt("categories_id"));
		forum.setName(rs.getString("forum_name"));
		forum.setDescription(rs.getString("forum_desc")==null ? "" : rs.getString("forum_desc"));
		forum.setType(rs.getInt("forum_type"));
		forum.setAccessType(rs.getInt("forum_access_type"));
		if (rs.getDate("start_date") != null)
		{
		  Timestamp startDate = rs.getTimestamp("start_date");
		  forum.setStartDate(startDate);
	    }
	    else
	    {
	    	forum.setStartDate(null);
	    }
		
	    if (rs.getDate("end_date") != null)
	    {
	      Timestamp endDate = rs.getTimestamp("end_date");
	      forum.setEndDate(endDate);
	      forum.setLockForum(rs.getInt("lock_end_date") > 0);
	    }
	    else
	    {
	    	forum.setEndDate(null);
	    }
		
		return forum;
	}*/
	
	/*public class User
	{
		private int id;
		private String firstName;
		private String lastName;
		private String sakaiUserId;
		
		*//**
		 * Default Constructor
		 *//*
		public User()
		{
		}
		*//**
		 * @return the id
		 *//*
		public int getId()
		{
			return id;
		}
		*//**
		 * @param id the id to set
		 *//*
		public void setId(int id)
		{
			this.id = id;
		}
		*//**
		 * @return the firstName
		 *//*
		public String getFirstName()
		{
			return firstName;
		}
		*//**
		 * @param firstName the firstName to set
		 *//*
		public void setFirstName(String firstName)
		{
			this.firstName = firstName;
		}
		*//**
		 * @return the lastName
		 *//*
		public String getLastName()
		{
			return lastName;
		}
		*//**
		 * @param lastName the lastName to set
		 *//*
		public void setLastName(String lastName)
		{
			this.lastName = lastName;
		}
		*//**
		 * @return the sakaiUserId
		 *//*
		public String getSakaiUserId()
		{
			return sakaiUserId;
		}
		*//**
		 * @param sakaiUserId the sakaiUserId to set
		 *//*
		public void setSakaiUserId(String sakaiUserId)
		{
			this.sakaiUserId = sakaiUserId;
		}

	}
	
	public class UserSession
	{
		private int userId;
		private String courseId;
		private Date visitTime;
		private Date markAllTime;
		
		*//**
		 * Default Constructor
		 *//*
		public UserSession()
		{
		}

		*//**
		 * @return the userId
		 *//*
		public int getUserId()
		{
			return userId;
		}

		*//**
		 * @param id the userId to set
		 *//*
		public void setUserId(int userId)
		{
			this.userId = userId;
		}

		*//**
		 * @return the courseId
		 *//*
		public String getCourseId()
		{
			return courseId;
		}

		*//**
		 * @param courseId the courseId to set
		 *//*
		public void setCourseId(String courseId)
		{
			this.courseId = courseId;
		}

		*//**
		 * @return the visitTime
		 *//*
		public Date getVisitTime()
		{
			return visitTime;
		}

		*//**
		 * @param visitTime the visitTime to set
		 *//*
		public void setVisitTime(Date visitTime)
		{
			this.visitTime = visitTime;
		}

		*//**
		 * @return the markAllTime
		 *//*
		public Date getMarkAllTime()
		{
			return markAllTime;
		}

		*//**
		 * @param markAllTime the markAllTime to set
		 *//*
		public void setMarkAllTime(Date markAllTime)
		{
			this.markAllTime = markAllTime;
		}

	}
	
	
	public class Category
	{
		private int id;
		private String name;
		private boolean gradeCategory;
		private Date startDate;
		private Date endDate;
		private boolean lockCategory;
		
		private List<SpecialAccess> specialAccessList = new ArrayList<SpecialAccess>();
		
		public Category() { }

		*//**
		 * @return the id
		 *//*
		public int getId()
		{
			return id;
		}
		*//**
		 * @param id the id to set
		 *//*
		public void setId(int id)
		{
			this.id = id;
		}
		*//**
		 * @return the name
		 *//*
		public String getName()
		{
			return name;
		}
		*//**
		 * @param name the name to set
		 *//*
		public void setName(String name)
		{
			this.name = name;
		}
		*//**
		 * @return the gradeCategory
		 *//*
		public boolean isGradeCategory()
		{
			return gradeCategory;
		}
		*//**
		 * @param gradeCategory the gradeCategory to set
		 *//*
		public void setGradeCategory(boolean gradeCategory)
		{
			this.gradeCategory = gradeCategory;
		}
		*//**
		 * @return the startDate
		 *//*
		public Date getStartDate()
		{
			return startDate;
		}
		*//**
		 * @param startDate the startDate to set
		 *//*
		public void setStartDate(Date startDate)
		{
			this.startDate = startDate;
		}
		*//**
		 * @return the endDate
		 *//*
		public Date getEndDate()
		{
			return endDate;
		}
		*//**
		 * @param endDate the endDate to set
		 *//*
		public void setEndDate(Date endDate)
		{
			this.endDate = endDate;
		}
		*//**
		 * @return the lockCategory
		 *//*
		public boolean isLockCategory()
		{
			return lockCategory;
		}
		*//**
		 * @param lockCategory the lockCategory to set
		 *//*
		public void setLockCategory(boolean lockCategory)
		{
			this.lockCategory = lockCategory;
		}

		*//**
		 * @return the specialAccessList
		 *//*
		public List<SpecialAccess> getSpecialAccessList()
		{
			return specialAccessList;
		}

		*//**
		 * @param specialAccessList the specialAccessList to set
		 *//*
		public void setSpecialAccessList(List<SpecialAccess> specialAccessList)
		{
			this.specialAccessList = specialAccessList;
		}
		
	}
	
	
	public class Forum
	{
		public static final int TYPE_NORMAL = 0;
		public static final int TYPE_REPLY_ONLY = 1;
		public static final int TYPE_READ_ONLY = 2;
		public static final int ACCESS_SITE = 0;
		public static final int ACCESS_DENY = 1;
		public static final int ACCESS_GROUPS = 2;
		public static final int GRADE_DISABLED = 0;
		public static final int GRADE_BY_TOPIC = 1;
		public static final int GRADE_BY_FORUM = 2;
		public static final int GRADE_BY_CATEGORY = 3;
		
		private int id;
		private int categoriesId;
		private String name;
		private String description;
		private Date startDate;
		private Date endDate;
		private boolean lockForum;
		private int type;
		private int accessType;
		private List<String> groups;
		private List<SpecialAccess> specialAccessList = new ArrayList<SpecialAccess>();
		
		public Forum() { }

		*//**
		 * @return the id
		 *//*
		public int getId()
		{
			return id;
		}

		*//**
		 * @param id the id to set
		 *//*
		public void setId(int id)
		{
			this.id = id;
		}

		*//**
		 * @return the categoriesId
		 *//*
		public int getCategoriesId()
		{
			return categoriesId;
		}

		*//**
		 * @param categoriesId the categoriesId to set
		 *//*
		public void setCategoriesId(int categoriesId)
		{
			this.categoriesId = categoriesId;
		}

		*//**
		 * @return the name
		 *//*
		public String getName()
		{
			return name;
		}

		*//**
		 * @param name the name to set
		 *//*
		public void setName(String name)
		{
			this.name = name;
		}

		*//**
		 * @return the description
		 *//*
		public String getDescription()
		{
			return description;
		}

		*//**
		 * @param description the description to set
		 *//*
		public void setDescription(String description)
		{
			this.description = description;
		}

		*//**
		 * @return the startDate
		 *//*
		public Date getStartDate()
		{
			return startDate;
		}

		*//**
		 * @param startDate the startDate to set
		 *//*
		public void setStartDate(Date startDate)
		{
			this.startDate = startDate;
		}

		*//**
		 * @return the endDate
		 *//*
		public Date getEndDate()
		{
			return endDate;
		}

		*//**
		 * @param endDate the endDate to set
		 *//*
		public void setEndDate(Date endDate)
		{
			this.endDate = endDate;
		}

		*//**
		 * @return the lockForum
		 *//*
		public boolean isLockForum()
		{
			return lockForum;
		}

		*//**
		 * @param lockForum the lockForum to set
		 *//*
		public void setLockForum(boolean lockForum)
		{
			this.lockForum = lockForum;
		}

		*//**
		 * @return the type
		 *//*
		public int getType()
		{
			return type;
		}

		*//**
		 * @param type the type to set
		 *//*
		public void setType(int type)
		{
			this.type = type;
		}

		*//**
		 * @return the accessType
		 *//*
		public int getAccessType()
		{
			return accessType;
		}

		*//**
		 * @param accessType the accessType to set
		 *//*
		public void setAccessType(int accessType)
		{
			this.accessType = accessType;
		}

		*//**
		 * @return the groups
		 *//*
		public List<String> getGroups()
		{
			return groups;
		}

		*//**
		 * @param groups the groups to set
		 *//*
		public void setGroups(List<String> groups)
		{
			this.groups = groups;
		}
		
		*//**
		 * @return the specialAccessList
		 *//*
		public List<SpecialAccess> getSpecialAccessList()
		{
			return specialAccessList;
		}

		*//**
		 * @param specialAccessList the specialAccessList to set
		 *//*
		public void setSpecialAccessList(List<SpecialAccess> specialAccessList)
		{
			this.specialAccessList = specialAccessList;
		}

	}
	
	
	public class Topic
	{
		private int id;
		private Date startDate;
		private Date endDate;
		private boolean lockTopic;
		private boolean read = true;
		private Date maxPostTime;
				
		public Topic() { }
		
		*//**
		 * @return the id
		 *//*
		public int getId()
		{
			return id;
		}
		*//**
		 * @param id the id to set
		 *//*
		public void setId(int id)
		{
			this.id = id;
		}
		*//**
		 * @return the startDate
		 *//*
		public Date getStartDate()
		{
			return startDate;
		}

		*//**
		 * @param startDate the startDate to set
		 *//*
		public void setStartDate(Date startDate)
		{
			this.startDate = startDate;
		}

		*//**
		 * @return the endDate
		 *//*
		public Date getEndDate()
		{
			return endDate;
		}

		*//**
		 * @param endDate the endDate to set
		 *//*
		public void setEndDate(Date endDate)
		{
			this.endDate = endDate;
		}

		*//**
		 * @return the lockTopic
		 *//*
		public boolean isLockTopic()
		{
			return lockTopic;
		}

		*//**
		 * @param lockTopic the lockTopic to set
		 *//*
		public void setLockTopic(boolean lockTopic)
		{
			this.lockTopic = lockTopic;
		}
		
		*//**
		 * @return the read
		 *//*
		public boolean isRead()
		{
			return read;
		}
		*//**
		 * @param read the read to set
		 *//*
		public void setRead(boolean read)
		{
			this.read = read;
		}
		*//**
		 * @return the maxPostTime
		 *//*
		public Date getMaxPostTime()
		{
			return maxPostTime;
		}
		*//**
		 * @param maxPostTime the maxPostTime to set
		 *//*
		public void setMaxPostTime(Date maxPostTime)
		{
			this.maxPostTime = maxPostTime;
		}
	}
	
	public class SpecialAccess
	{
		private int id;
		private int forumId;
		private int topicId;
		private Date startDate;
		private Date endDate;
		private Boolean overrideStartDate = false;
		private Boolean overrideEndDate = false;
		private Boolean overrideLockEndDate = false;
		private boolean lockOnEndDate = false;
		protected List<Integer> userIds = new ArrayList<Integer>();
		protected List<User> users = new ArrayList<User>(); 
		
		*//**
		 * @return the id
		 *//*
		public int getId()
		{
			return id;
		}
		*//**
		 * @param id the id to set
		 *//*
		public void setId(int id)
		{
			this.id = id;
		}
		*//**
		 * @return the forumId
		 *//*
		public int getForumId()
		{
			return forumId;
		}
		*//**
		 * @param forumId the forumId to set
		 *//*
		public void setForumId(int forumId)
		{
			this.forumId = forumId;
		}
		*//**
		 * @param topicId the topicId to set
		 *//*
		public void setTopicId(int topicId)
		{
			this.topicId = topicId;
		}
		*//**
		 * @return the topicId
		 *//*
		public int getTopicId()
		{
			return topicId;
		}
		*//**
		 * @return the startDate
		 *//*
		public Date getStartDate()
		{
			return startDate;
		}
		*//**
		 * @param startDate the startDate to set
		 *//*
		public void setStartDate(Date startDate)
		{
			this.startDate = startDate;
		}
		*//**
		 * @return the endDate
		 *//*
		public Date getEndDate()
		{
			return endDate;
		}
		*//**
		 * @param endDate the endDate to set
		 *//*
		public void setEndDate(Date endDate)
		{
			this.endDate = endDate;
		}
		*//**
		 * @return the overrideStartDate
		 *//*
		public Boolean isOverrideStartDate()
		{
			return overrideStartDate;
		}
		*//**
		 * @param overrideStartDate the overrideStartDate to set
		 *//*
		public void setOverrideStartDate(Boolean overrideStartDate)
		{
			this.overrideStartDate = overrideStartDate;
		}
		*//**
		 * @return the overrideEndDate
		 *//*
		public Boolean isOverrideEndDate()
		{
			return overrideEndDate;
		}
		*//**
		 * @param overrideEndDate the overrideEndDate to set
		 *//*
		public void setOverrideEndDate(Boolean overrideEndDate)
		{
			this.overrideEndDate = overrideEndDate;
		}
		*//**
		 * @return the overrideLockEndDate
		 *//*
		public Boolean isOverrideLockEndDate()
		{
			return overrideLockEndDate;
		}
		*//**
		 * @param overrideLockEndDate the overrideLockEndDate to set
		 *//*
		public void setOverrideLockEndDate(Boolean overrideLockEndDate)
		{
			this.overrideLockEndDate = overrideLockEndDate;
		}
		*//**
		 * @return the lockOnEndDate
		 *//*
		public boolean isLockOnEndDate()
		{
			return lockOnEndDate;
		}
		*//**
		 * @param lockOnEndDate the lockOnEndDate to set
		 *//*
		public void setLockOnEndDate(boolean lockOnEndDate)
		{
			this.lockOnEndDate = lockOnEndDate;
		}
		*//**
		 * @return the userIds
		 *//*
		public List<Integer> getUserIds()
		{
			return userIds;
		}
		*//**
		 * @param userIds the userIds to set
		 *//*
		public void setUserIds(List<Integer> userIds)
		{
			this.userIds = userIds;
		}
		*//**
		 * @return the users
		 *//*
		public List<User> getUsers()
		{
			return users;
		}
		*//**
		 * @param users the users to set
		 *//*
		public void setUsers(List<User> users)
		{
			this.users = users;
		}
	}*/
}
