/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/dao/mysql/ForumDaoMysql.java $ 
 * $Id: ForumDaoMysql.java 7799 2014-04-14 23:41:28Z murthyt $ 
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
package org.etudes.component.app.jforum.dao.mysql;

import java.sql.Timestamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.Forum;
import org.etudes.api.app.jforum.dao.ForumDao;
import org.etudes.component.app.jforum.dao.generic.ForumDaoGeneric;

public class ForumDaoMysql extends ForumDaoGeneric implements ForumDao
{
	private static Log logger = LogFactory.getLog(ForumDaoMysql.class);
	
	public void destroy()
	{
		if (logger.isInfoEnabled())
			logger.info("destroy....");
	}

	public void init()
	{
		if (logger.isInfoEnabled())
			logger.info("init....");
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected int insertForum(Forum forum)
	{
		int maxDisplayOrder = getMaxDisplayOrder();
		
		StringBuilder sql = new StringBuilder();
		//sql.append("INSERT INTO jforum_forums (categories_id, forum_name, forum_desc, start_date, end_date, lock_end_date, forum_order, moderated, forum_type, forum_access_type, forum_grade_type) ");
		sql.append("INSERT INTO jforum_forums (categories_id, forum_name, forum_desc, start_date, hide_until_open, end_date, allow_until_date, forum_order, moderated, forum_type, forum_access_type, forum_grade_type, forum_topic_order, forum_topic_likes) ");
		sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		
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
		
		fields[i++] = maxDisplayOrder + 1;
		fields[i++] = 0;
		fields[i++] = forum.getType();
		fields[i++] = forum.getAccessType();
		fields[i++] = forum.getGradeType();
		fields[i++] = forum.getTopicOrder();
		fields[i++] = forum.isAllowTopicPostLikes() ? 1 : 0;

		Long id = null;
		try
		{
			id = this.sqlService.dbInsert(null, sql.toString(), fields, "forum_id");
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e, e);
			}
		}
		
		if (id == null)
		{
			throw new RuntimeException("insertForum: dbInsert failed");
		}
		
		return id.intValue();
	}
	
	

}
