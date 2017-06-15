/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/dao/mysql/PrivateMessageDaoMysql.java $ 
 * $Id: PrivateMessageDaoMysql.java 8540 2014-08-29 22:18:25Z murthyt $ 
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.PrivateMessage;
import org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType;
import org.etudes.component.app.jforum.PrivateMessageImpl;
import org.etudes.component.app.jforum.dao.generic.PrivateMessageDaoGeneric;

public class PrivateMessageDaoMysql extends PrivateMessageDaoGeneric
{
	private static Log logger = LogFactory.getLog(PrivateMessageDaoMysql.class);
	
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
	protected String getPmText(ResultSet rs) throws SQLException
	{
		return rs.getString("privmsgs_text");
	}
	
	/**
	 * Get user id string from the users id's list
	 * @param userIds		List of user id's
	 * @return				User id string
	 */
	protected String getUserIdString(List<Integer> userIds)
	{
		if ((userIds == null) || (userIds.size() == 0)) 
		{
			return null;
		}
		
		StringBuilder userIdSB = new StringBuilder();
		for (Integer userId : userIds)
		{
			if (userId != null)
			{
				userIdSB.append(userId);
				userIdSB.append(":");
			}
		}
		
		String userIdsStr = userIdSB.toString();
		return userIdsStr.substring(0, userIdsStr.length() - 1);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int insertPrivateMessage(PrivateMessage pm)
	{
		StringBuilder sql = new StringBuilder();
		
		// for reply messages save bcc userid's in the sent box
		sql.append("INSERT INTO jforum_privmsgs ( privmsgs_type, privmsgs_subject, privmsgs_from_userid, privmsgs_to_userid, privmsgs_date, privmsgs_enable_bbcode, ");
		sql.append("privmsgs_enable_html, privmsgs_enable_smilies, privmsgs_attach_sig, privmsgs_priority, privmsgs_bcc_userids ) ");
		sql.append("VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");			
				
		Object[] fields = null;
		fields = new Object[11];
		
		int i = 0;
		fields[i++] = pm.getType();
		fields[i++] = pm.getPost().getSubject();
		fields[i++] = pm.getFromUser().getId();
		fields[i++] = pm.getToUser().getId();
		fields[i++] = new Timestamp(pm.getPost().getTime().getTime());
		fields[i++] = pm.getPost().isBbCodeEnabled() ? 1 : 0;
		fields[i++] = pm.getPost().isHtmlEnabled() ? 1 : 0;
		fields[i++] = pm.getPost().isSmiliesEnabled() ? 1 : 0;
		fields[i++] = pm.getPost().isSignatureEnabled() ? 1 : 0;
		fields[i++] = pm.getPriority();
		
		if ((pm.getId() > 0) && (pm.isReplied()) && (pm.getType() == PrivateMessage.PrivateMessageType.SENT.getType()))
		{
			fields[i++] = getUserIdString(pm.getBccUserIds());
		}
		else
		{
			fields[i++] = null;
		}
		
		Long id = null;
		try
		{
			id = this.sqlService.dbInsert(null, sql.toString(), fields, "privmsgs_id");
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
			throw new RuntimeException("insertPrivateMessage: dbInsert failed");
		}
		
		return id.intValue();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int insertPrivateMessageDraft(PrivateMessage pm)
	{
		StringBuilder sql = new StringBuilder();
		
		if (pm.getType() == PrivateMessageType.DRAFT_REPLY.getType())
		{
			sql.append("INSERT INTO jforum_privmsgs ( privmsgs_type, privmsgs_subject, privmsgs_from_userid, privmsgs_to_userid, privmsgs_date, privmsgs_enable_bbcode, ");
			sql.append("privmsgs_enable_html, privmsgs_enable_smilies, privmsgs_attach_sig, privmsgs_priority, privmsgs_draft_to_userids, privmsgs_bcc_userids, privmsgs_reply_draft_from_privmsgs_id ) ");
			sql.append("VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
		}
		else
		{
			sql.append("INSERT INTO jforum_privmsgs ( privmsgs_type, privmsgs_subject, privmsgs_from_userid, privmsgs_to_userid, privmsgs_date, privmsgs_enable_bbcode, ");
			sql.append("privmsgs_enable_html, privmsgs_enable_smilies, privmsgs_attach_sig, privmsgs_priority, privmsgs_draft_to_userids ) ");
			sql.append("VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");			
		}
		
		Object[] fields = null;
		
		if (pm.getType() == PrivateMessageType.DRAFT_REPLY.getType())
		{
			fields = new Object[13];
		}
		else
		{
			fields = new Object[11];
		}
				
		int i = 0;
		if (!(pm.getType() != PrivateMessageType.DRAFT.getType() || pm.getType() != PrivateMessageType.DRAFT_REPLY.getType()))
		{
			fields[i++] = PrivateMessage.PrivateMessageType.DRAFT.getType();
		}
		else
		{
			fields[i++] = pm.getType();
		}
		fields[i++] = pm.getPost().getSubject();
		fields[i++] = pm.getFromUser().getId();
		fields[i++] = 0;
		fields[i++] = new Timestamp((new Date()).getTime());
		fields[i++] = pm.getPost().isBbCodeEnabled() ? 1 : 0;
		fields[i++] = pm.getPost().isHtmlEnabled() ? 1 : 0;
		fields[i++] = pm.getPost().isSmiliesEnabled() ? 1 : 0;
		fields[i++] = pm.getPost().isSignatureEnabled() ? 1 : 0;
		fields[i++] = pm.getPriority();
		fields[i++] = getUserIdString(pm.getDraftToUserIds());
		
		if (pm.getType() == PrivateMessageType.DRAFT_REPLY.getType())
		{
			fields[i++] = getUserIdString(pm.getBccUserIds());
			fields[i++] = ((PrivateMessageImpl)pm).getReplyDraftFromPrivmsgId();
		}
		
		Long id = null;
		try
		{
			id = this.sqlService.dbInsert(null, sql.toString(), fields, "privmsgs_id");
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
			throw new RuntimeException("insertPrivateMessageDraft: dbInsert failed");
		}
		
		return id.intValue();
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected int insertPrivateMessageFolder(String context, int userId, String folderName)
	{
		String sql = "INSERT INTO jforum_privmsgs_folders (course_id, user_id, folder_name) VALUES (?, ?, ?)";
		
		Object[] fields = new Object[3];
		int i = 0;
		fields[i++] = context;
		fields[i++] = userId;
		fields[i++] = folderName;
		
		Long id = null;
		try
		{
			id = this.sqlService.dbInsert(null, sql.toString(), fields, "folder_id");
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
			throw new RuntimeException("addPrivateMessageFolder: dbInsert failed");
		}
		
		return id.intValue();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void insertPrivateMessageText(int pmId, String messageText)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO jforum_privmsgs_text ( privmsgs_id, privmsgs_text ) VALUES (?, ?)");
		
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = pmId;
		fields[i++] = messageText;
		
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("insertPrivateMessageText: db write failed");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void updatePrivateMessageDraftText(int pmId, String messageText)
	{
		String sql = "UPDATE jforum_privmsgs_text SET privmsgs_text = ? WHERE privmsgs_id = ?";
		
		Object[] fields = new Object[2];
		int i = 0;
		
		fields[i++] = messageText;
		fields[i++] = pmId;
				
		if (!sqlService.dbWrite(sql, fields)) 
		{
			throw new RuntimeException("updatePrivateMessageDraftText: dbWrite failed");
		}		
	}

}
