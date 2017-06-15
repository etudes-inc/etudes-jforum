/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/dao/generic/PrivateMessageDaoGeneric.java $ 
 * $Id: PrivateMessageDaoGeneric.java 12733 2016-07-21 21:53:34Z murthyt $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Etudes, Inc. 
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.Post;
import org.etudes.api.app.jforum.PrivateMessage;
import org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType;
import org.etudes.api.app.jforum.PrivateMessageFolder;
import org.etudes.api.app.jforum.User;
import org.etudes.api.app.jforum.dao.AttachmentDao;
import org.etudes.api.app.jforum.dao.PrivateMessageDao;
import org.etudes.api.app.jforum.dao.UserDao;
import org.etudes.component.app.jforum.PostImpl;
import org.etudes.component.app.jforum.PrivateMessageFolderImpl;
import org.etudes.component.app.jforum.PrivateMessageImpl;
import org.etudes.component.app.jforum.UserImpl;
import org.etudes.component.app.jforum.util.JForumUtil;
import org.etudes.util.HtmlHelper;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;

public abstract class PrivateMessageDaoGeneric implements PrivateMessageDao
{
	private static Log logger = LogFactory.getLog(PrivateMessageDaoGeneric.class);
	
	/** Dependency: AttachmentDao */
	protected AttachmentDao attachmentDao = null;
	
	/** Dependency: SqlService */
	protected SqlService sqlService = null;
	
	/** Dependency: UserDao */
	protected UserDao userDao = null;
	
	/**
	 * {@inheritDoc}
	 */
	public int addPrivateMessageFolder(String context, int userId, String folderName)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (userId < 0)
		{
			throw new IllegalArgumentException("User id is needed.");
		}
		
		if (folderName == null || folderName.trim().length() == 0)
		{
			throw new IllegalArgumentException("Folder name is needed.");
		}
		
		return insertPrivateMessageFolder(context, userId, folderName);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void delete(PrivateMessage privateMessage)
	{
		if (privateMessage == null || privateMessage.getId() == 0 || privateMessage.getFromUser() == null || privateMessage.getToUser() == null)
		{
			return;
		}
		deleteTx(privateMessage);		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void deletePrivateMessageFolder(int userId, int folderId)
	{
		if (userId < 0)
		{
			throw new IllegalArgumentException("User id is needed.");
		}
		
		if (folderId <= 0)
		{
			throw new IllegalArgumentException("folder id is needed.");
		}
		
		deletePrivateMessageFolderTx(userId, folderId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessage> drafts(String context, String sakaiUserId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User sakai id is needed.");
		}
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT pm.privmsgs_id, pm.privmsgs_type, pm.privmsgs_subject, pm.privmsgs_from_userid, pm.privmsgs_to_userid, pm.privmsgs_date, pm.privmsgs_ip, ");
		sql.append("pm.privmsgs_enable_bbcode, pm.privmsgs_enable_html, pm.privmsgs_enable_smilies, pm.privmsgs_attach_sig, pm.privmsgs_attachments, pm.privmsgs_flag_to_follow, ");
		sql.append("pm.privmsgs_replied, pm.privmsgs_priority, pm.privmsgs_draft_to_userids, pm.privmsgs_bcc_userids, pm.privmsgs_reply_draft_from_privmsgs_id, pm.privmsgs_folder_id, ");
		sql.append("u1.user_id, u1.sakai_user_id, u1.username, u1.user_fname, u1.user_lname, u1.user_avatar ");
		sql.append("FROM jforum_privmsgs pm, jforum_users u1, jforum_sakai_course_privmsgs cp ");
		sql.append("WHERE u1.sakai_user_id = ? ");
		//sql.append("AND u2.user_id = pm.privmsgs_to_userid ");
		sql.append("AND u1.user_id = pm.privmsgs_from_userid ");
		sql.append("AND (pm.privmsgs_type = "+ PrivateMessage.PrivateMessageType.DRAFT.getType() +" OR pm.privmsgs_type =  "+ PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType() +") ");
		sql.append("AND pm.privmsgs_id = cp.privmsgs_id AND cp.course_id = ? ");
		sql.append("ORDER BY pm.privmsgs_date DESC ");		
						
		int i = 0; 
		Object[] fields = new Object[2];
		fields[i++] = sakaiUserId;
		fields[i++] = context;
		
		final List<PrivateMessage> pmList = new ArrayList<PrivateMessage>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PrivateMessage pm = getPm(result, false);
										
					pmList.add(pm);
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("drafts: " + e, e);
					}					
				}
				return null;
			}
		});
			
		return pmList;	
	}
	

	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessage> folderMessages(String context, String sakaiUserId, int folderId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User sakai id is needed.");
		}
		
		if (folderId <= 0)
		{
			throw new IllegalArgumentException("folder id is needed.");
		}
		
		StringBuilder sql = new StringBuilder();
		

		sql.append("SELECT pm.privmsgs_id, pm.privmsgs_type, pm.privmsgs_subject, pm.privmsgs_from_userid, pm.privmsgs_to_userid, pm.privmsgs_date, pm.privmsgs_ip, ");
		sql.append("pm.privmsgs_enable_bbcode, pm.privmsgs_enable_html, pm.privmsgs_enable_smilies, pm.privmsgs_attach_sig, pm.privmsgs_attachments, pm.privmsgs_flag_to_follow, ");
		sql.append("pm.privmsgs_replied, pm.privmsgs_priority, pm.privmsgs_draft_to_userids, pm.privmsgs_bcc_userids, pm.privmsgs_folder_id , u2.user_id , u2.sakai_user_id, u2.username, u2.user_fname, u2.user_lname, u2.user_avatar ");
		sql.append("FROM jforum_privmsgs pm, jforum_users u1, jforum_users u2, jforum_sakai_course_privmsgs cp ");
		sql.append("WHERE u1.sakai_user_id = ? ");
		sql.append("AND pm.privmsgs_folder_id = ? ");
		sql.append("AND u1.user_id = pm.privmsgs_to_userid ");
		sql.append("AND u2.user_id = pm.privmsgs_from_userid ");
		sql.append("AND ( pm.privmsgs_type = "+ PrivateMessage.TYPE_NEW +" ");
		sql.append("OR pm.privmsgs_type = "+ PrivateMessage.TYPE_READ +" ");
		sql.append("OR privmsgs_type = "+ PrivateMessage.TYPE_UNREAD +") ");
		sql.append("AND pm.privmsgs_id = cp.privmsgs_id AND cp.course_id = ? ");
		sql.append("ORDER BY pm.privmsgs_date DESC ");		
						
		int i = 0; 
		Object[] fields = new Object[3];
		fields[i++] = sakaiUserId;
		fields[i++] = folderId;
		fields[i++] = context;
		
		final List<PrivateMessage> pmList = new ArrayList<PrivateMessage>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PrivateMessage pm = getPm(result, false);
					
					UserImpl fromUser = new UserImpl();
					fromUser.setId(result.getInt("user_id"));
					fromUser.setSakaiUserId(result.getString("sakai_user_id"));
					fromUser.setFirstName(result.getString("user_fname")==null ? "" : result.getString("user_fname"));
					fromUser.setLastName(result.getString("user_lname")==null ? "" : result.getString("user_lname"));
					fromUser.setAvatar(result.getString("user_avatar"));
					fromUser.setUsername(result.getString("username"));
					pm.setFromUser(fromUser);
					
					pmList.add(pm);
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("inbox: " + e, e);
					}					
				}
				return null;
			}
		});
		
		return pmList;		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessage> inbox(String context, String sakaiUserId, boolean includeFolderMessages)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User sakai id is needed.");
		}
		
		StringBuilder sql = new StringBuilder();
		
		if (includeFolderMessages)
		{
			sql.append("SELECT pm.privmsgs_id, pm.privmsgs_type, pm.privmsgs_subject, pm.privmsgs_from_userid, pm.privmsgs_to_userid, pm.privmsgs_date, pm.privmsgs_ip, ");
			sql.append("pm.privmsgs_enable_bbcode, pm.privmsgs_enable_html, pm.privmsgs_enable_smilies, pm.privmsgs_attach_sig, pm.privmsgs_attachments, pm.privmsgs_flag_to_follow, ");
			sql.append("pm.privmsgs_replied, pm.privmsgs_priority, pm.privmsgs_draft_to_userids, pm.privmsgs_bcc_userids, pm.privmsgs_folder_id, u2.user_id , u2.sakai_user_id, u2.username, u2.user_fname, u2.user_lname, u2.user_avatar ");
			sql.append("FROM jforum_privmsgs pm, jforum_users u1, jforum_users u2, jforum_sakai_course_privmsgs cp ");
			sql.append("WHERE u1.sakai_user_id = ? ");
			sql.append("AND u1.user_id = pm.privmsgs_to_userid ");
			sql.append("AND u2.user_id = pm.privmsgs_from_userid ");
			sql.append("AND ( pm.privmsgs_type = "+ PrivateMessage.TYPE_NEW +" ");
			sql.append("OR pm.privmsgs_type = "+ PrivateMessage.TYPE_READ +" ");
			sql.append("OR privmsgs_type = "+ PrivateMessage.TYPE_UNREAD +") ");
			sql.append("AND pm.privmsgs_id = cp.privmsgs_id AND cp.course_id = ? ");
			sql.append("ORDER BY pm.privmsgs_date DESC ");			
		}
		else
		{
			sql.append("SELECT pm.privmsgs_id, pm.privmsgs_type, pm.privmsgs_subject, pm.privmsgs_from_userid, pm.privmsgs_to_userid, pm.privmsgs_date, pm.privmsgs_ip, ");
			sql.append("pm.privmsgs_enable_bbcode, pm.privmsgs_enable_html, pm.privmsgs_enable_smilies, pm.privmsgs_attach_sig, pm.privmsgs_attachments, pm.privmsgs_flag_to_follow, ");
			sql.append("pm.privmsgs_replied, pm.privmsgs_priority, pm.privmsgs_draft_to_userids, pm.privmsgs_bcc_userids, pm.privmsgs_folder_id, u2.user_id , u2.sakai_user_id, u2.username, u2.user_fname, u2.user_lname, u2.user_avatar ");
			sql.append("FROM jforum_privmsgs pm, jforum_users u1, jforum_users u2, jforum_sakai_course_privmsgs cp ");
			sql.append("WHERE u1.sakai_user_id = ? ");
			sql.append("AND u1.user_id = pm.privmsgs_to_userid ");
			sql.append("AND u2.user_id = pm.privmsgs_from_userid ");
			sql.append("AND pm.privmsgs_folder_id IS NULL ");
			sql.append("AND ( pm.privmsgs_type = "+ PrivateMessage.TYPE_NEW +" ");
			sql.append("OR pm.privmsgs_type = "+ PrivateMessage.TYPE_READ +" ");
			sql.append("OR privmsgs_type = "+ PrivateMessage.TYPE_UNREAD +") ");
			sql.append("AND pm.privmsgs_id = cp.privmsgs_id AND cp.course_id = ? ");
			sql.append("ORDER BY pm.privmsgs_date DESC ");
		}
						
		int i = 0; 
		Object[] fields = new Object[2];
		fields[i++] = sakaiUserId;
		fields[i++] = context;
		
		final List<PrivateMessage> pmList = new ArrayList<PrivateMessage>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PrivateMessage pm = getPm(result, false);
					
					UserImpl fromUser = new UserImpl();
					fromUser.setId(result.getInt("user_id"));
					fromUser.setSakaiUserId(result.getString("sakai_user_id"));
					fromUser.setFirstName(result.getString("user_fname")==null ? "" : result.getString("user_fname"));
					fromUser.setLastName(result.getString("user_lname")==null ? "" : result.getString("user_lname"));
					fromUser.setAvatar(result.getString("user_avatar"));
					fromUser.setUsername(result.getString("username"));
					pm.setFromUser(fromUser);
					
					pmList.add(pm);
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("inbox: " + e, e);
					}					
				}
				return null;
			}
		});
		
		return pmList;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void moveTofolder(String context, int userId, int privateMessageId, int moveToFolderId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (userId < 0)
		{
			throw new IllegalArgumentException("User id is needed.");
		}
		
		if (privateMessageId < 0)
		{
			throw new IllegalArgumentException("message id is needed.");
		}
		
		/*if (moveToFolderId < 0)
		{
			throw new IllegalArgumentException("folder id is needed.");
		}*/
		
		
		PrivateMessage pm = selectById(privateMessageId);
		
		if ((pm == null) || (pm.getToUser().getId() != userId) || !(pm.getContext().equalsIgnoreCase(context)))
		{
			return;
		}
				
		/*only messages in inbox and folders can be moved to other folders. SentBox messages are not allowed to move to other folders. If moved to inbox change the folder to NULL*/
		if (((pm.getType() == PrivateMessage.PrivateMessageType.NEW.getType() ) || (pm.getType() == PrivateMessage.PrivateMessageType.READ.getType()) || (pm.getType() == PrivateMessage.PrivateMessageType.UNREAD.getType())) 
				&& ((pm.getFolderId() == 0) || (pm.getFolderId() > 0)))
		{
			moveTofolderTx(context, userId, privateMessageId, moveToFolderId);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int saveDraft(PrivateMessage pm)
	{
		if (pm == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (pm.getFromUser() == null)
		{
			throw new IllegalArgumentException("From user information is needed");
		}
		
		if (pm.getToUser() != null)
		{
			throw new IllegalArgumentException("To user is not needed for drafts. Use draft user id's to save draft message To users");
		}
		
		if ((pm.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (pm.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		if (pm.getPost().getSubject() != null && pm.getPost().getSubject().length() > 100)
		{
			pm.getPost().setSubject(pm.getPost().getSubject().substring(0, 99));
		}
		
		return saveDraftTx(pm);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Integer> saveMessage(PrivateMessage pm)
	{
		if (pm == null)
		{
			throw new IllegalArgumentException();
		}
		
		if ((pm.getFromUser() == null) || (pm.getToUser() == null))
		{
			throw new IllegalArgumentException("From user and To user information is needed");
		}
		
		if ((pm.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (pm.getPost().getSubject() != null && pm.getPost().getSubject().length() > 100)
		{
			pm.getPost().setSubject(pm.getPost().getSubject().substring(0, 99));
		}
		
		return saveMessageTx(pm);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PrivateMessage selectById(int privateMessageId)
	{

		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT p.privmsgs_id, p.privmsgs_type, p.privmsgs_subject, p.privmsgs_from_userid, ");
		sql.append("p.privmsgs_to_userid, p.privmsgs_date, p.privmsgs_ip, p.privmsgs_enable_bbcode, ");
		sql.append("p.privmsgs_enable_html, p.privmsgs_enable_smilies, p.privmsgs_attach_sig, ");
		sql.append("p.privmsgs_attachments, p.privmsgs_flag_to_follow, p.privmsgs_replied, ");
		sql.append("p.privmsgs_priority, pt.privmsgs_text, pc.course_id, p.privmsgs_draft_to_userids, p.privmsgs_bcc_userids, p.privmsgs_reply_draft_from_privmsgs_id, p.privmsgs_folder_id, ");
		sql.append("u1.sakai_user_id AS from_user_sakai_id, u1.user_fname AS from_user_first_name, ");
		sql.append("u1.user_lname AS from_user_last_name, u1.user_avatar AS from_user_avatar, ");
		sql.append("u1.user_attachsig AS from_user_attachsig, u1.user_sig AS from_user_sig, ");
		sql.append("u2.sakai_user_id AS to_user_sakai_id, u2.user_fname AS to_user_first_name, ");
		sql.append("u2.user_lname AS to_user_last_name, u2.user_avatar AS to_user_avatar, ");
		sql.append("u2.user_attachsig AS to_user_attachsig, u2.user_sig AS to_user_sig ");
		sql.append("FROM jforum_privmsgs p, jforum_privmsgs_text pt, jforum_sakai_course_privmsgs pc, jforum_users u1, jforum_users u2 ");
		sql.append("WHERE p.privmsgs_id = pt.privmsgs_id ");
		sql.append("AND p.privmsgs_id = ? ");
		sql.append("AND p.privmsgs_id = pc.privmsgs_id ");
		sql.append("AND p.privmsgs_from_userid = u1.user_id ");
		sql.append("AND p.privmsgs_to_userid = u2.user_id ");
		
		int i = 0; 
		Object[] fields = new Object[1];
		fields[i++] = privateMessageId;
		
		final List<PrivateMessage> pmList = new ArrayList<PrivateMessage>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PrivateMessage pm = getPm(result, true);
					pm.setContext(result.getString("course_id"));
					
					pmList.add(pm);
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectById: " + e, e);
					}					
				}
				return null;
			}
		});
		
		PrivateMessage privateMessage = null;
		
		if (pmList.size() == 1)
		{
			privateMessage = pmList.get(0);
			//PostUtil.preparePostForDisplay(privateMessage.getPost());
			
			// get PM attachments
			if (privateMessage.getPost().hasAttachments())
			{
				privateMessage.getPost().setAttachments(attachmentDao.selectPrivateMessageAttachments(privateMessage.getId()));
			}
		}
		return privateMessage;
	
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PrivateMessage selectDraftById(int privateMessageId)
	{
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT p.privmsgs_id, p.privmsgs_type, p.privmsgs_subject, p.privmsgs_from_userid, ");
		sql.append("p.privmsgs_to_userid, p.privmsgs_date, p.privmsgs_ip, p.privmsgs_enable_bbcode, ");
		sql.append("p.privmsgs_enable_html, p.privmsgs_enable_smilies, p.privmsgs_attach_sig, ");
		sql.append("p.privmsgs_attachments, p.privmsgs_flag_to_follow, p.privmsgs_replied, ");
		sql.append("p.privmsgs_priority, pt.privmsgs_text, pc.course_id, p.privmsgs_draft_to_userids, p.privmsgs_bcc_userids, p.privmsgs_reply_draft_from_privmsgs_id, p.privmsgs_folder_id, ");
		sql.append("u1.sakai_user_id AS from_user_sakai_id, u1.user_fname AS from_user_first_name, ");
		sql.append("u1.user_lname AS from_user_last_name, u1.user_avatar AS from_user_avatar, ");
		sql.append("u1.user_attachsig AS from_user_attachsig, u1.user_sig AS from_user_sig ");
		sql.append("FROM jforum_privmsgs p, jforum_privmsgs_text pt, jforum_sakai_course_privmsgs pc, jforum_users u1 ");
		sql.append("WHERE p.privmsgs_id = pt.privmsgs_id ");
		sql.append("AND p.privmsgs_id = ? ");
		sql.append("AND p.privmsgs_id = pc.privmsgs_id ");
		sql.append("AND p.privmsgs_from_userid = u1.user_id ");
		
		int i = 0; 
		Object[] fields = new Object[1];
		fields[i++] = privateMessageId;
		
		final List<PrivateMessage> pmList = new ArrayList<PrivateMessage>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PrivateMessage pm = getPm(result, true);
					pm.setContext(result.getString("course_id"));
					
					pmList.add(pm);
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectDraftById: " + e, e);
					}					
				}
				return null;
			}
		});
		
		PrivateMessage privateMessage = null;
		
		if (pmList.size() == 1)
		{
			privateMessage = pmList.get(0);
			
			// get PM attachments
			if (privateMessage.getPost().hasAttachments())
			{
				privateMessage.getPost().setAttachments(attachmentDao.selectPrivateMessageAttachments(privateMessage.getId()));
			}
		}
		
		if (!(privateMessage.getType() != PrivateMessage.PrivateMessageType.DRAFT.getType() || privateMessage.getType() != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType()))
		{
			return null;
		}
		
		return privateMessage;		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int selectDraftsCount(String context, int userId)
	{
		if (context == null || context.trim().length() == 0 || userId <= 0)
		{
			return 0;
		}
		
		final List<Integer> count = new ArrayList<Integer>();
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT count(*) AS total FROM jforum_privmsgs p, jforum_sakai_course_privmsgs cp ");
		sql.append("WHERE p.privmsgs_from_userid = ? ");
		sql.append("AND (p.privmsgs_type = ? OR p.privmsgs_type = ?)");
		sql.append("AND p.privmsgs_id = cp.privmsgs_id ");
		sql.append("AND cp.course_id = ?");						
				
		Object[] fields;
		int i = 0;
		
		fields = new Object[4];
		fields[i++] = userId;
		fields[i++] = PrivateMessage.PrivateMessageType.DRAFT.getType();
		fields[i++] = PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType();
		fields[i++] = context;
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					count.add(result.getInt("total"));
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectDraftsCount: " + e, e);
					}
					return null;
				}
			}
		});
		
		int privateMessagesDraftsCount = 0;
		if (count.size() == 1)
		{
			privateMessagesDraftsCount = count.get(0);
		}
		
		return privateMessagesDraftsCount;	
		
	
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PrivateMessageFolder selectFolder(int folderId)
	{
		if (folderId < 0)
		{
			throw new IllegalArgumentException("User id is needed.");
		}
		
		String sql = "SELECT folder_id, course_id, user_id, folder_name FROM jforum_privmsgs_folders WHERE folder_id = ?";
		
		int i = 0; 
		Object[] fields = new Object[1];
		fields[i++] = folderId;
		
		final List<PrivateMessageFolder> pmFolderList = new ArrayList<PrivateMessageFolder>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PrivateMessageFolderImpl pmFolder = new PrivateMessageFolderImpl();
					pmFolder.setId(result.getInt("folder_id"));
					pmFolder.setSiteId(result.getString("course_id"));
					pmFolder.setUserId(result.getInt("user_id"));
					pmFolder.setFolderName(result.getString("folder_name"));
					
					pmFolderList.add(pmFolder);
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectFolder: " + e, e);
					}					
				}
					return null;
			}
		});
		
		PrivateMessageFolder privateMessageFolder = null;
		
		if (pmFolderList.size() == 1)
		{
			privateMessageFolder = pmFolderList.get(0);
		}
		
		return privateMessageFolder;
	}

	/**
	 * {@inheritDoc}
	 */
	public int selectFolderMessageCount(int folderId)
	{
		final List<Integer> count = new ArrayList<Integer>();
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT count(*) AS total FROM jforum_privmsgs p WHERE p.privmsgs_folder_id = ? ");
				
		Object[] fields;
		int i = 0;
		
		fields = new Object[1];
		fields[i++] = folderId;
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					count.add(result.getInt("total"));
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectFolderMessageCount: " + e, e);
					}
					return null;
				}
			}
		});
		
		int folderMessageCount = 0;
		if (count.size() == 1)
		{
			folderMessageCount = count.get(0);
		}
		
		return folderMessageCount;	
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessageFolder> selectFolders(String context, int userId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (userId < 0)
		{
			throw new IllegalArgumentException("User id is needed.");
		}
		
		String sql = "SELECT folder_id, course_id, user_id, folder_name FROM jforum_privmsgs_folders WHERE course_id = ? AND user_id = ?  ORDER BY folder_name ASC";
		
		int i = 0; 
		Object[] fields = new Object[2];
		fields[i++] = context;
		fields[i++] = userId;
		
		final List<PrivateMessageFolder> pmFolderList = new ArrayList<PrivateMessageFolder>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PrivateMessageFolderImpl pmFolder = new PrivateMessageFolderImpl();
					pmFolder.setId(result.getInt("folder_id"));
					pmFolder.setSiteId(result.getString("course_id"));
					pmFolder.setUserId(result.getInt("user_id"));
					pmFolder.setFolderName(result.getString("folder_name"));
					
					pmFolderList.add(pmFolder);
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectFolders: " + e, e);
					}					
				}
					return null;
			}
		});
		
		
		return pmFolderList;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Map<Integer, Integer> selectFoldersUnreadCount(String context, int userId)
	{
		if (context == null || context.trim().length() == 0 || userId <= 0)
		{
			return new HashMap<Integer, Integer>();
		}
		
		final Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT privmsgs_folder_id, count(p.privmsgs_id) AS total FROM jforum_privmsgs p, jforum_sakai_course_privmsgs cp ");
		sql.append("WHERE p.privmsgs_to_userid = ? ");
		sql.append("AND p.privmsgs_type = ? ");
		sql.append("AND p.privmsgs_id = cp.privmsgs_id ");
		sql.append("AND cp.course_id = ? ");
		sql.append("GROUP BY privmsgs_folder_id");
				
		Object[] fields;
		int i = 0;
		
		fields = new Object[3];
		fields[i++] = userId;
		fields[i++] = PrivateMessage.PrivateMessageType.NEW.getType();
		fields[i++] = context;
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					/* For inbox the privmsgs_folder_id is null. In the hasmap the inbox key stores as zero */
					countMap.put(result.getInt("privmsgs_folder_id"), result.getInt("total"));
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectFoldersUnreadCount: " + e, e);
					}
					return null;
				}
			}
		});
		
		return countMap;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int selectFolderUnreadCount(String context, int userId, int folderId)
	{
		if (context == null || context.trim().length() == 0 || userId <= 0 || folderId < 0)
		{
			return 0;
		}
		
		final List<Integer> count = new ArrayList<Integer>();
		
		StringBuilder sql = new StringBuilder();
		
		Object[] fields;
		int i = 0;
		
		if (folderId == 0)
		{
			sql.append("SELECT count(*) AS total FROM jforum_privmsgs p, jforum_sakai_course_privmsgs cp ");
			sql.append("WHERE p.privmsgs_to_userid = ? ");
			sql.append("AND p.privmsgs_type = ? ");
			sql.append("AND p.privmsgs_id = cp.privmsgs_id ");
			sql.append("AND cp.course_id = ? AND privmsgs_folder_id IS NULL");
			
			fields = new Object[3];
			fields[i++] = userId;
			fields[i++] = PrivateMessage.PrivateMessageType.NEW.getType();
			fields[i++] = context;
		}
		else
		{
			sql.append("SELECT count(*) AS total FROM jforum_privmsgs p, jforum_sakai_course_privmsgs cp ");
			sql.append("WHERE p.privmsgs_to_userid = ? ");
			sql.append("AND p.privmsgs_type = ? AND privmsgs_folder_id = ?");
			sql.append("AND p.privmsgs_id = cp.privmsgs_id ");
			sql.append("AND cp.course_id = ?");
			
			fields = new Object[4];
			fields[i++] = userId;
			fields[i++] = folderId;
			fields[i++] = PrivateMessage.PrivateMessageType.NEW.getType();
			fields[i++] = context;
		}
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					count.add(result.getInt("total"));
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectFolderUnreadCount: " + e, e);
					}
					return null;
				}
			}
		});
		
		int unreadPrivateMessagesCount = 0;
		if (count.size() == 1)
		{
			unreadPrivateMessagesCount = count.get(0);
		}
		
		return unreadPrivateMessagesCount;	
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int selectUnreadCount(String context, int userId)
	{
		if (context == null || context.trim().length() == 0 || userId <= 0)
		{
			return 0;
		}
		
		final List<Integer> count = new ArrayList<Integer>();
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT count(*) AS total FROM jforum_privmsgs p, jforum_sakai_course_privmsgs cp ");
		sql.append("WHERE p.privmsgs_to_userid = ? ");
		sql.append("AND p.privmsgs_type = ? ");
		sql.append("AND p.privmsgs_id = cp.privmsgs_id ");
		sql.append("AND cp.course_id = ?");						
				
		Object[] fields;
		int i = 0;
		
		fields = new Object[3];
		fields[i++] = userId;
		fields[i++] = PrivateMessage.PrivateMessageType.NEW.getType();
		fields[i++] = context;
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					count.add(result.getInt("total"));
					
					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectUnreadCount: " + e, e);
					}
					return null;
				}
			}
		});
		
		int unreadPrivateMessagesCount = 0;
		if (count.size() == 1)
		{
			unreadPrivateMessagesCount = count.get(0);
		}
		
		return unreadPrivateMessagesCount;	
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessage> sentbox(String context, String sakaiUserId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User sakai id is needed.");
		}
		
		StringBuilder sql = new StringBuilder();		

		sql.append("SELECT pm.privmsgs_id, pm.privmsgs_type, pm.privmsgs_subject, pm.privmsgs_from_userid, pm.privmsgs_to_userid, pm.privmsgs_date, pm.privmsgs_ip, ");
		sql.append("pm.privmsgs_enable_bbcode, pm.privmsgs_enable_html, pm.privmsgs_enable_smilies, pm.privmsgs_attach_sig, pm.privmsgs_attachments, pm.privmsgs_flag_to_follow, ");
		sql.append("pm.privmsgs_replied, pm.privmsgs_priority, pm.privmsgs_draft_to_userids, pm.privmsgs_bcc_userids, pm.privmsgs_reply_draft_from_privmsgs_id, pm.privmsgs_folder_id, u2.user_id, u2.sakai_user_id, u2.username, u2.user_fname, u2.user_lname, u2.user_avatar ");
		sql.append("FROM jforum_privmsgs pm, jforum_users u1, jforum_users u2, jforum_sakai_course_privmsgs cp ");
		sql.append("WHERE u1.sakai_user_id = ? ");
		sql.append("AND u2.user_id = pm.privmsgs_to_userid ");
		sql.append("AND u1.user_id = pm.privmsgs_from_userid ");
		sql.append("AND pm.privmsgs_type = "+ PrivateMessage.TYPE_SENT +" ");
		sql.append("AND pm.privmsgs_id = cp.privmsgs_id AND cp.course_id = ? ");
		sql.append("ORDER BY pm.privmsgs_date DESC ");		
						
		int i = 0; 
		Object[] fields = new Object[2];
		fields[i++] = sakaiUserId;
		fields[i++] = context;
		
		final List<PrivateMessage> pmList = new ArrayList<PrivateMessage>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PrivateMessage pm = getPm(result, false);
					
					UserImpl toUser = new UserImpl();
					toUser.setId(result.getInt("user_id"));
					toUser.setSakaiUserId(result.getString("sakai_user_id"));
					toUser.setFirstName(result.getString("user_fname")==null ? "" : result.getString("user_fname"));
					toUser.setLastName(result.getString("user_lname")==null ? "" : result.getString("user_lname"));
					toUser.setAvatar(result.getString("user_avatar"));
					pm.setToUser(toUser);
					
					pmList.add(pm);
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("sentbox: " + e, e);
					}					
				}
				return null;
			}
		});
		
		return pmList;
	}
	
	/**
	 * @param attachmentDao the attachmentDao to set
	 */
	public void setAttachmentDao(AttachmentDao attachmentDao)
	{
		this.attachmentDao = attachmentDao;
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
	 * @param userDao the userDao to set
	 */
	public void setUserDao(UserDao userDao)
	{
		this.userDao = userDao;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateDraft(PrivateMessage pm)
	{
		if (pm == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (pm.getFromUser() == null)
		{
			throw new IllegalArgumentException("From user information is needed");
		}
		
		if (pm.getToUser() != null)
		{
			throw new IllegalArgumentException("To user is not needed for drafts. Use draft user id's to save draft message To users");
		}
		
		if ((pm.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (pm.getPost().getSubject() != null && pm.getPost().getSubject().length() > 100)
		{
			pm.getPost().setSubject(pm.getPost().getSubject().substring(0, 99));
		}
		
		PrivateMessage exisPrivateMessageDraft = selectDraftById(pm.getId());
		
		if (exisPrivateMessageDraft == null)
		{
			throw new IllegalArgumentException("Private message draft is not existing.");
		}
		
		if (!(pm.getType() != PrivateMessageType.DRAFT.getType() || pm.getType() != PrivateMessageType.DRAFT_REPLY.getType()))
		{
			return;
		}
		
		updateDraftTx(pm);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateFlagToFollowup(int messageId, boolean flag)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE jforum_privmsgs SET privmsgs_flag_to_follow = ? WHERE privmsgs_id = ?");
		
		int i = 0;
		Object[] fields = new Object[2];
		fields[i++] = flag ?  1 : 0;
		fields[i++] = messageId;
		
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("updateFlagToFollowup: db write failed");
		}
	
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateMessageType(int messageId, int messageType)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE jforum_privmsgs SET privmsgs_type = ? WHERE privmsgs_id = ?");
		
		int i = 0;
		Object[] fields = new Object[2];
		fields[i++] = messageType;
		fields[i++] = messageId;
		
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("updateMessageType: db write failed");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updatePrivateMessageFolder(int userId, int folderId, String folderName)
	{
		if (folderId < 0)
		{
			throw new IllegalArgumentException("Folder id is needed.");
		}
		
		if (folderName == null || folderName.trim().length() == 0)
		{
			throw new IllegalArgumentException("Folder name is needed.");
		}
		
		
		String sql = "UPDATE jforum_privmsgs_folders SET folder_name = ? WHERE folder_id = ? AND user_id = ?";
		
		
		Object[] fields = new Object[3];
		int i = 0;
		fields[i++] = folderName;
		fields[i++] = folderId;
		fields[i++] = userId;		
				
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
			
			throw new RuntimeException("updatePrivateMessageFolder: dbWrite failed");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void updateRepliedStatus(PrivateMessage pm)
	{
		StringBuilder sql = new StringBuilder();
		
		sql.append("UPDATE jforum_privmsgs SET privmsgs_replied = ? WHERE privmsgs_id = ?");
		
		int i = 0;
		
		Object[] fields = new Object[2];
		fields[i++] = pm.isReplied() ?  1 : 0;
		fields[i++] = pm.getId();
		
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("updateRepliedStatus: db write failed");
		}
	}
	
	private void deleteFolder(int userId, int folderId)
	{
		String sql = "DELETE FROM jforum_privmsgs_folders WHERE folder_id = ? AND user_id = ?";
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = folderId;
		fields[i++] = userId;		
		
		if (!sqlService.dbWrite(sql, fields)) 
		{
			throw new RuntimeException("deleteFolder: db write failed");
		}
	}
	
	/**
	 * add private message to course
	 * 
	 * @param context	Course id
	 * 
	 * @param pmId	Private message id
	 */
	protected void addPrivateMessageToCourse(String context, int pmId)
	{
		// add to jforum_privmsgs_text table
		String sql = "INSERT INTO jforum_sakai_course_privmsgs (course_id, privmsgs_id) VALUES (?, ?)";
		
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = context;
		fields[i++] = pmId;
		
		if (!sqlService.dbWrite(sql, fields)) 
		{
			throw new RuntimeException("addPrivateMessageToCourse: db write failed");
		}
	}
	
	protected void deletePrivateMessage(PrivateMessage privateMessage)
	{
		//READ(0), NEW(1), SENT(2), SAVED_IN(3), SAVED_OUT(4), UNREAD(5, DRAFT(6));
		
		// delete from jforum_privmsgs table
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM jforum_privmsgs WHERE privmsgs_id = ? AND ");
		sql.append("( (privmsgs_from_userid = ? AND privmsgs_type = " +	PrivateMessage.PrivateMessageType.SENT.getType() +") OR ");
		sql.append("(privmsgs_to_userid = ? AND privmsgs_type ");
		sql.append("IN(");
		sql.append(PrivateMessage.PrivateMessageType.READ.getType());
		sql.append(", ");
		sql.append(PrivateMessage.PrivateMessageType.NEW.getType());
		sql.append(", ");
		sql.append(PrivateMessage.PrivateMessageType.UNREAD.getType());
		sql.append(")) OR (privmsgs_from_userid = ? AND privmsgs_to_userid = 0 AND (privmsgs_type = " +	PrivateMessage.PrivateMessageType.DRAFT.getType() +" OR privmsgs_type ="+ PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType() +")))");
		
		Object[] fields = new Object[4];
		int i = 0;
		fields[i++] = privateMessage.getId();
		fields[i++] = privateMessage.getFromUser().getId();
		fields[i++] = privateMessage.getToUser().getId();
		fields[i++] = privateMessage.getFromUser().getId();
		
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("deletePrivateMessage: db write failed");
		}
		
	}
		
	protected void deletePrivateMessageAttachment(PrivateMessage privateMessage)
	{
		String sql = "DELETE FROM jforum_privmsgs_attach WHERE privmsgs_id = ?";
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = privateMessage.getId();
		
		if (!sqlService.dbWrite(sql, fields)) 
		{
			throw new RuntimeException("deletePrivateMessageAttachment: db write failed");
		}
	}
	
	protected void deletePrivateMessageFolderTx(final int userId, final int folderId)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				// TO DO check and delete folder if there are no messages in the folder				
				int count = selectFolderMessageCount(folderId);
				
				if (count == 0)
				{
					deleteFolder(userId, folderId);
				}
			}
		}, "deletePrivateMessageFolderTx: " + folderId);
	}
	
	protected void deletePrivateMessageFromCourse(PrivateMessage privateMessage)
	{
		String sql = "DELETE FROM jforum_sakai_course_privmsgs WHERE privmsgs_id = ?";
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = privateMessage.getId();
		
		if (!sqlService.dbWrite(sql, fields)) 
		{
			throw new RuntimeException("deletePrivateMessageFromCourse: db write failed");
		}
	}
	
	protected void deletePrivateMessageText(PrivateMessage privateMessage)
	{
		String sql = "DELETE FROM jforum_privmsgs_text WHERE privmsgs_id = ?";
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = privateMessage.getId();
		
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("deletePrivateMessageText: db write failed");
		}
	}
	
	/**
	 * Deletes private message 
	 * 
	 * @param privateMessage	Private message
	 */
	protected void deleteTx(final PrivateMessage privateMessage)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				deletePrivateMessage(privateMessage);
				deletePrivateMessageText(privateMessage);
				deletePrivateMessageAttachment(privateMessage);
				deletePrivateMessageFromCourse(privateMessage);				
			}
		}, "deleteTx: " + privateMessage.getId());
	}
	
	/**
	 * Gets the private message from the result set
	 * 
	 * @param rs	The result set
	 * 
	 * @param full	With private message text or with out
	 * 
	 * @return	The private message from the result set
	 * 
	 * @throws SQLException
	 */
	protected PrivateMessage getPm(ResultSet rs, boolean full) throws SQLException
	{
		PrivateMessageImpl pm = new PrivateMessageImpl();
		PostImpl p = new PostImpl();

		pm.setId(rs.getInt("privmsgs_id"));
		pm.setType(rs.getInt("privmsgs_type"));
		p.setTime(rs.getTimestamp("privmsgs_date"));
		p.setSubject(JForumUtil.removeScriptTags(rs.getString("privmsgs_subject")));
		p.setHasAttachments(Boolean.valueOf(rs.getInt("privmsgs_attachments") > 0));
		p.setBbCodeEnabled(rs.getInt("privmsgs_enable_bbcode") == 1);
		p.setSignatureEnabled(rs.getInt("privmsgs_attach_sig") == 1);
		p.setHtmlEnabled(rs.getInt("privmsgs_enable_html") == 1);
		p.setSmiliesEnabled(rs.getInt("privmsgs_enable_smilies") == 1);
		
			
		if (full) 
		{
			User fromUser = userDao.selectByUserId(rs.getInt("privmsgs_from_userid"));
						
			pm.setFromUser(fromUser);
			
			if (rs.getInt("privmsgs_to_userid") > 0)
			{
				User toUser = userDao.selectByUserId(rs.getInt("privmsgs_to_userid"));
						
				pm.setToUser(toUser);
			}			
			
			p.setText(JForumUtil.escapeScriptTag(JForumUtil.stripBodyTags(HtmlHelper.clean(this.getPmText(rs), true))));
			p.setRawText(this.getPmText(rs));
		}
		
		// draft to users
		if (rs.getInt("privmsgs_to_userid") == 0 && ((rs.getInt("privmsgs_type") == PrivateMessage.PrivateMessageType.DRAFT.getType()) 
				|| (rs.getInt("privmsgs_type") == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())))
		{
			List<Integer> draftToUserIds = getUserIdList(rs.getString("privmsgs_draft_to_userids"));
			pm.setDraftToUserIds(draftToUserIds);
		}
		
		// only sent messages and reply message drafts have bcc users
		if ((rs.getInt("privmsgs_type") == PrivateMessage.PrivateMessageType.SENT.getType()) || (rs.getInt("privmsgs_type") == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType()))
		{
			List<Integer> bccUserIds = getUserIdList(rs.getString("privmsgs_bcc_userids"));
			pm.setBccUserIds(bccUserIds);
		}
		
		if (rs.getInt("privmsgs_type") == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			((PrivateMessageImpl)pm).setReplyDraftFromPrivmsgId(rs.getInt("privmsgs_reply_draft_from_privmsgs_id"));
		}
		
		((PrivateMessageImpl)pm).setFolderId(rs.getInt("privmsgs_folder_id")); 
		
		pm.setFlagToFollowup(rs.getInt("privmsgs_flag_to_follow") > 0);
		pm.setReplied(rs.getInt("privmsgs_replied") > 0);
		pm.setPriority(rs.getInt("privmsgs_priority"));		
		
		pm.setPost((Post)p);

		return pm;
	}
	
	/**
	 * Gets the private message text
	 * 
	 * @param rs	The result set
	 * 
	 * @return	The private message text
	 * 
	 * @throws SQLException
	 */
	abstract protected String getPmText(ResultSet rs) throws SQLException;
	
	/**
	 * get userid list from the string
	 * 
	 * @param userIds
	 *        userids string
	 * 
	 * @return list of userid's
	 */
	protected List<Integer> getUserIdList(String userIds)
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
	 * Insert the message
	 * 
	 * @param pm	Private message
	 * 
	 * @return	Newly created private message id
	 */
	protected abstract int insertPrivateMessage(PrivateMessage pm);
	
	/**
	 * Insert the message draft
	 * 
	 * @param pm	Private message
	 * 
	 * @return	Newly created private message id
	 */
	protected abstract int insertPrivateMessageDraft(PrivateMessage pm);
	
	abstract protected int insertPrivateMessageFolder(String context, int userId, String folderName);
	
	/**
	 * Insert private message text
	 * 
	 * @param id	Private message id
	 * 
	 * @param messageText	Message text
	 */
	protected abstract void insertPrivateMessageText(int pmId, String messageText);
	
	protected boolean moveTofolderTx(final String context, final int userId, final int privateMessageId, final int moveToFolderId)
	{
		Boolean success = Boolean.FALSE;
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				// only messages in folders and inbox can be moved to other folders. SentBox messages are not allowed to move to other folders. If moved to inbox change the folder to NULL.
				if (moveToFolderId > 0)
				{
					PrivateMessageFolder userFolder = selectUserFolder(context, userId, moveToFolderId);
					
					if (userFolder != null)
					{
						updateMessageFolder(privateMessageId, userFolder.getId());
					}
				}
				else
				{
					updateMessageFolder(privateMessageId, 0);
				}
			}
		}, "moveTofolderTx: " + userId +":"+ privateMessageId +":"+ moveToFolderId);
		
		return success;
	}
	
	/**
	 * save private message draft
	 * 
	 * @param pm	private message draft
	 * 
	 * @return	the id of newly created private message draft
	 */
	protected int saveDraftTx(final PrivateMessage pm)
	{
		final List<Integer> pmIds = new ArrayList<Integer>();
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					int pmDraftId = saveInFromUserDrafts(pm);
					pmIds.add(Integer.valueOf(pmDraftId));
					
					((PrivateMessageImpl)pm).setId(pmDraftId);
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}

					throw new RuntimeException("Error while creating new private message draft.", e);
				}
			}
		}, "saveDraftTx: " + pm.getPost().getSubject());
		
		int pmId = 0;
		if (pmIds.size() > 0)
		{
			pmId = pmIds.get(0);
		}
		return pmId;
	}
	
	/**
	 * Save the private message in bcc user inbox
	 * 
	 * @param pm	Private message
	 * 
	 * @return	newly created private message id
	 */
	protected int saveInBccUserInbox(PrivateMessage pm)
	{
		pm.setType(PrivateMessage.PrivateMessageType.NEW.getType());
		
		// message
		int pmId = insertPrivateMessage(pm);
		
		// add private message to course
		addPrivateMessageToCourse(pm.getContext(), pmId);
		
		// message text
		insertPrivateMessageText(pmId, pm.getPost().getText());
		
		return pmId;
	}
	
	/**
	 * Save the private message draft for the users
	 * 
	 * @param pm	private message draft
	 * 
	 * @return	The newly created private message draft id
	 */
	protected int saveInFromUserDrafts(PrivateMessage pm)
	{
		if (!(pm.getType() != PrivateMessageType.DRAFT.getType() || pm.getType() != PrivateMessageType.DRAFT_REPLY.getType()))
		{
			pm.setType(PrivateMessage.PrivateMessageType.DRAFT.getType());
		}
				
		// message
		int pmId = insertPrivateMessageDraft(pm);
		
		// add private message to course
		addPrivateMessageToCourse(pm.getContext(), pmId);
		
		// message text
		insertPrivateMessageText(pmId, pm.getPost().getText());
		
		return pmId;
	}
	
	/**
	 * Save the message in from user sent box
	 * 
	 * @param pm	Private message
	 * 
	 * @return newly created private message id
	 */
	protected int saveInFromUserSentbox(PrivateMessage pm)
	{
		pm.setType(PrivateMessage.PrivateMessageType.SENT.getType());
		
		// message
		int pmId = insertPrivateMessage(pm);
		
		// add private message to course
		addPrivateMessageToCourse(pm.getContext(), pmId);
		
		// message text
		insertPrivateMessageText(pmId, pm.getPost().getText());
		
		return pmId;
	}
	
	/**
	 * Save the private message in to user inbox
	 * 
	 * @param pm	Private message

	 * @return newly created private message id
	 */
	protected int saveInToUserInbox(PrivateMessage pm)
	{
		pm.setType(PrivateMessage.PrivateMessageType.NEW.getType());
		
		// message
		int pmId = insertPrivateMessage(pm);
		
		// add private message to course
		addPrivateMessageToCourse(pm.getContext(), pmId);
		
		// message text
		insertPrivateMessageText(pmId, pm.getPost().getText());
		
		return pmId;
	}
	
	/**
	 * Save private message in from user sent box and to user inbox and update replied status if replying to an existing private message
	 * 
	 * @param pm	Private Message
	 *
	 * @return newly created private message id's of "from user sent box" and "to user inbox"
	 */
	protected List<Integer> saveMessageTx(final PrivateMessage pm)
	{
		final List<Integer> pmIds = new ArrayList<Integer>();
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					int pmType = pm.getType();
					if ((pm.getId() > 0) && (pmType == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType()))
					{
						// to save bcc users in the sent box message
						pm.setReplied(true);
					}
					int fromUserPMId = saveInFromUserSentbox(pm);
					pmIds.add(Integer.valueOf(fromUserPMId));
					
					int toUserPMId = saveInToUserInbox(pm);
					pmIds.add(Integer.valueOf(toUserPMId));
					
					// update replied status of the existing private message
					if ((pm.getId() > 0) && (pm.isReplied()))
					{
						/*update the replied status if replied to original message. If saved as draft the id is draft id and 
						 	replied status cannot be updated as draft will be removed once message is saved*/
						if ((pm.getId() > 0) && (pmType != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType()))
						{
							updateRepliedStatus(pm);
						}
						else if ((pm.getId() > 0) && (pmType == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType()))
						{
							// get the existing message from the private message reply draft and update reply status to true
							int exisPmId = ((PrivateMessageImpl)pm).getReplyDraftFromPrivmsgId();
							PrivateMessage exisPrivateMessage = selectById(exisPmId);
							
							if (exisPrivateMessage != null)
							{
								exisPrivateMessage.setReplied(true);
								updateRepliedStatus(exisPrivateMessage);
							}
						}
						
						// save messages for bcc users
						List<Integer> bccUserIds = pm.getBccUserIds();
						
						if (bccUserIds != null && !bccUserIds.isEmpty())
						{
							User toUser = pm.getToUser();
							
							for (int bccUserId : bccUserIds)
							{
								User bccUser = userDao.selectByUserId(bccUserId);
								
								if (bccUser != null)
								{
									// save in bcc user inbox
									pm.setToUser(bccUser);
									
									int bccUserPMId = saveInBccUserInbox(pm);
									pmIds.add(Integer.valueOf(bccUserPMId));
								}
							}
							
							// set the private message to actual to user
							pm.setToUser(toUser);
						}						
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}

					throw new RuntimeException("Error while creating new private message.", e);
				}
			}
		}, "saveMessageTx: " + pm.getPost().getSubject());
		
		return pmIds;
	}
	
	
	
	protected PrivateMessageFolder selectUserFolder(String context, int userId, int folderId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (userId < 0)
		{
			throw new IllegalArgumentException("User id is needed.");
		}
		
		if (folderId < 0)
		{
			throw new IllegalArgumentException("folder id is needed.");
		}
		
		String sql = "SELECT folder_id, course_id, user_id, folder_name FROM jforum_privmsgs_folders WHERE course_id = ? AND user_id = ? AND folder_id = ?";
		
		int i = 0; 
		Object[] fields = new Object[3];
		fields[i++] = context;
		fields[i++] = userId;
		fields[i++] = folderId;
		
		final List<PrivateMessageFolder> pmFolderList = new ArrayList<PrivateMessageFolder>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PrivateMessageFolderImpl pmFolder = new PrivateMessageFolderImpl();
					pmFolder.setId(result.getInt("folder_id"));
					pmFolder.setSiteId(result.getString("course_id"));
					pmFolder.setUserId(result.getInt("user_id"));
					pmFolder.setFolderName(result.getString("folder_name"));
					
					pmFolderList.add(pmFolder);
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("selectFolders: " + e, e);
					}					
				}
					return null;
			}
		});
		
		PrivateMessageFolder privateMessageFolder = null;
		
		if (pmFolderList.size() == 1)
		{
			privateMessageFolder = pmFolderList.get(0);
		}
		return privateMessageFolder;
	}
	
	
	
	/**
	 * Update draft
	 * 
	 * @param pm	private message draft
	 * 
	 * @return	created
	 */
	protected void updateDraftTx(final PrivateMessage pm)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					updatePrivateMessageDraft(pm);
					
					updatePrivateMessageDraftText(pm.getId(), pm.getPost().getText());
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}

					throw new RuntimeException("Error while updating private message draft.", e);
				}
			}
		}, "updateDraftTx: " + pm.getId());		
	}
	
	protected void updateMessageFolder(int messageId, int folderId)
	{
		StringBuilder sql = new StringBuilder();
		
		int i = 0;
		Object[] fields;
		
		if (folderId > 0)
		{
			sql.append("UPDATE jforum_privmsgs SET privmsgs_folder_id = ? WHERE privmsgs_id = ?");
			
			i = 0;
			fields = new Object[2];
			fields[i++] = folderId;
			fields[i++] = messageId;
		}
		else
		{
			i = 0;
			fields = new Object[1];
			fields[i++] = messageId;
			
			sql.append("UPDATE jforum_privmsgs SET privmsgs_folder_id = NULL WHERE privmsgs_id = ?");
		}
		
		
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("updateMessageFolder: db write failed");
		}
	}
	
	/**
	 * update private message draft
	 * 
	 * @param pm	private message draft
	 */
	protected void updatePrivateMessageDraft(PrivateMessage pm)
	{
		StringBuilder sql = new StringBuilder();
		
		if (pm.getType() == PrivateMessage.PrivateMessageType.DRAFT.getType())
		{
			sql.append("UPDATE jforum_privmsgs SET privmsgs_subject = ?, privmsgs_from_userid = ?, privmsgs_to_userid = ?, privmsgs_date = ?, privmsgs_enable_bbcode = ?, ");
			sql.append("privmsgs_enable_html = ?, privmsgs_enable_smilies = ?, privmsgs_attach_sig = ?, privmsgs_priority = ?, privmsgs_draft_to_userids = ? ");
			sql.append("WHERE privmsgs_id = ?");
			
			Object[] fields = new Object[11];
			int i = 0;
			//fields[i++] = PrivateMessage.PrivateMessageType.DRAFT.getType();
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
			fields[i++] = pm.getId();
			
			if (!sqlService.dbWrite(sql.toString(), fields)) 
			{
				throw new RuntimeException("updatePrivateMessageDraft: db write failed");
			}
		}
		else if (pm.getType() == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			sql.append("UPDATE jforum_privmsgs SET privmsgs_subject = ?, privmsgs_from_userid = ?, privmsgs_to_userid = ?, privmsgs_date = ?, privmsgs_enable_bbcode = ?, ");
			sql.append("privmsgs_enable_html = ?, privmsgs_enable_smilies = ?, privmsgs_attach_sig = ?, privmsgs_priority = ?, privmsgs_bcc_userids = ? ");
			sql.append("WHERE privmsgs_id = ?");
			
			Object[] fields = new Object[11];
			int i = 0;
			//fields[i++] = PrivateMessage.PrivateMessageType.DRAFT.getType();
			fields[i++] = pm.getPost().getSubject();
			fields[i++] = pm.getFromUser().getId();
			fields[i++] = 0;
			fields[i++] = new Timestamp((new Date()).getTime());
			fields[i++] = pm.getPost().isBbCodeEnabled() ? 1 : 0;
			fields[i++] = pm.getPost().isHtmlEnabled() ? 1 : 0;
			fields[i++] = pm.getPost().isSmiliesEnabled() ? 1 : 0;
			fields[i++] = pm.getPost().isSignatureEnabled() ? 1 : 0;
			fields[i++] = pm.getPriority();
			fields[i++] = getUserIdString(pm.getBccUserIds());
			fields[i++] = pm.getId();
			
			if (!sqlService.dbWrite(sql.toString(), fields)) 
			{
				throw new RuntimeException("updatePrivateMessageDraft: db write failed");
			}
		}		
				
	}
	
	/**
	 * update existing private message draft text
	 * 
	 * @param pmId		private message draft id
	 * 
	 * @param messageText	private message draft text
	 */
	abstract protected void updatePrivateMessageDraftText(int pmId, String messageText);
}
