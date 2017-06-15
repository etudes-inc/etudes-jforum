/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/JForumPrivateMessageServiceImpl.java $ 
 * $Id: JForumPrivateMessageServiceImpl.java 12733 2016-07-21 21:53:34Z murthyt $ 
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
package org.etudes.component.app.jforum;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.Attachment;
import org.etudes.api.app.jforum.JForumAccessException;
import org.etudes.api.app.jforum.JForumAttachmentBadExtensionException;
import org.etudes.api.app.jforum.JForumAttachmentOverQuotaException;
import org.etudes.api.app.jforum.JForumAttachmentService;
import org.etudes.api.app.jforum.JForumEmailExecutorService;
import org.etudes.api.app.jforum.JForumPrivateMessageService;
import org.etudes.api.app.jforum.JForumSecurityService;
import org.etudes.api.app.jforum.JForumUserService;
import org.etudes.api.app.jforum.Post;
import org.etudes.api.app.jforum.PrivateMessage;
import org.etudes.api.app.jforum.PrivateMessageFolder;
import org.etudes.api.app.jforum.User;
import org.etudes.api.app.jforum.dao.AttachmentDao;
import org.etudes.api.app.jforum.dao.PrivateMessageDao;
import org.etudes.component.app.jforum.util.JForumUtil;
import org.etudes.component.app.jforum.util.post.EmailUtil;
import org.etudes.component.app.jforum.util.post.PostUtil;
import org.etudes.util.HtmlHelper;
import org.etudes.util.XrefHelper;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.util.Web;

public class JForumPrivateMessageServiceImpl implements JForumPrivateMessageService
{
	private static Log logger = LogFactory.getLog(JForumPrivateMessageServiceImpl.class);
	
	/** Dependency: AttachmentDao */
	protected AttachmentDao attachmentDao = null;
	
	/** Dependency: JForumAttachmentService */
	protected JForumAttachmentService jforumAttachmentService = null;
	
	/** Dependency: JForumUserService. */
	protected JForumEmailExecutorService jforumEmailExecutorService = null;
	
	/** Dependency: JForumSecurityService. */
	protected JForumSecurityService jforumSecurityService = null;
	
	/** Dependency: JForumUserService. */
	protected JForumUserService jforumUserService = null;
	
	/** Dependency: PrivateMessageDao */
	protected PrivateMessageDao privateMessageDao = null;
	
	/** Dependency: SiteService. */
	protected SiteService siteService = null;
	
	/** Dependency: SqlService */
	protected SqlService sqlService = null;
	
	/**
	 * {@inheritDoc}
	 */
	public int createPrivateMessageFolder(String context, String sakaiUserId, String folderName) throws JForumAccessException
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User sakai id is needed.");
		}
		
		if (folderName == null || folderName.trim().length() == 0)
		{
			throw new IllegalArgumentException("Folder name is needed.");
		}
		
		//access check for user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(context, sakaiUserId);
		
		// Observer not allowed to perform private message functions
		boolean observer = jforumSecurityService.isEtudesObserver(context, sakaiUserId);
		
		boolean participant = false;
		if (!facilitator)
		{
			if (!observer)
			{
				participant = jforumSecurityService.isJForumParticipant(context, sakaiUserId);
			}
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			return 0;
		}
		
		// create user private message folder
		return privateMessageDao.addPrivateMessageFolder(context, user.getId(), folderName);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void deleteDraft(String context, String sakaiUserId, List<Integer> privateMessageDraftIds) throws JForumAccessException
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("Sakai user id is needed");
		}
		
		if (privateMessageDraftIds == null || privateMessageDraftIds.isEmpty())
		{
			throw new IllegalArgumentException("Privatemessage id's needed");
		}
		//access check for user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(context, sakaiUserId);
		
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(context, sakaiUserId);
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			return;
		}
		
		for (Integer privateMessageDraftId : privateMessageDraftIds)
		{
			PrivateMessage privateMessage = new PrivateMessageImpl(privateMessageDraftId, context, user, user);
			
			PrivateMessage exisPrivateMessageDraft = privateMessageDao.selectDraftById(privateMessageDraftId);
			
			if (exisPrivateMessageDraft == null)
			{
				continue;
			}
			
			// Check if the private message drafts belong to the user
			if (!exisPrivateMessageDraft.getFromUser().getSakaiUserId().equalsIgnoreCase(sakaiUserId))
			{
				continue;
			}
			//delete private message
			privateMessageDao.delete(privateMessage);
			
			//delete attachments on the file system if not referenced by other private messages
			if (exisPrivateMessageDraft.getPost().hasAttachments())
			{
				privateMessage.getPost().setHasAttachments(true);
				privateMessage.getPost().getAttachments().addAll(exisPrivateMessageDraft.getPost().getAttachments());
				jforumAttachmentService.deletePrivateMessageAttachments(privateMessage);
			}
		}		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void deleteMessage(String context, String sakaiUserId, List<Integer> privateMessageIds)throws JForumAccessException
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("Sakai user id is needed");
		}
		
		if (privateMessageIds == null || privateMessageIds.isEmpty())
		{
			throw new IllegalArgumentException("Privatemessage id's needed");
		}
		//access check for user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(context, sakaiUserId);
		
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(context, sakaiUserId);
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			return;
		}
		
		for (Integer privateMessageId : privateMessageIds)
		{
			PrivateMessage privateMessage = new PrivateMessageImpl(privateMessageId, context, user, user);
			
			PrivateMessage exisPrivateMessage = privateMessageDao.selectById(privateMessageId);
			
			//delete private message
			privateMessageDao.delete(privateMessage);
			
			//delete attachments on the file system if not referenced by other private messages
			if (exisPrivateMessage.getPost().hasAttachments())
			{
				privateMessage.getPost().setHasAttachments(true);
				privateMessage.getPost().getAttachments().addAll(exisPrivateMessage.getPost().getAttachments());
				jforumAttachmentService.deletePrivateMessageAttachments(privateMessage);
			}
		}		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void deletePrivateMessageFolder(String context, String sakaiUserId, int folderId) throws JForumAccessException
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
			throw new IllegalArgumentException("Folder id is needed.");
		}
		
		//access check for user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(context, sakaiUserId);
				
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(context, sakaiUserId);
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			return;
		}
		
		privateMessageDao.deletePrivateMessageFolder(user.getId(), folderId);		
	}
	
	public void destroy()
	{
		if (logger.isInfoEnabled())
			logger.info("destroy....");
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessage> drafts(String context, String sakaiUserId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("Context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("sakai User Id is needed.");
		}
		
		List<PrivateMessage> pmList = privateMessageDao.drafts(context, sakaiUserId);
		
		for (PrivateMessage pm : pmList)
		{
			PostUtil.preparePostForDisplay(pm.getPost());
		}
		
		return pmList;	
	}

	/**
	 * {@inheritDoc}
	 */
	public void flagToFollowup(PrivateMessage privateMessage)
	{
		if (privateMessage == null)
		{
			new IllegalArgumentException();
		}
		
		privateMessageDao.updateFlagToFollowup(privateMessage.getId(), privateMessage.isFlagToFollowup());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessage> folderMessages(String context, String sakaiUserId, int folderId)
	{

		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("Context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("sakai User Id is needed.");
		}
		
		if (folderId <= 0)
		{
			throw new IllegalArgumentException("folder id is needed.");
		}
		
		List<PrivateMessage> pmList = privateMessageDao.folderMessages(context, sakaiUserId, folderId);
		
		for (PrivateMessage pm : pmList)
		{
			PostUtil.preparePostForDisplay(pm.getPost());
		}
		return pmList;
	
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PrivateMessageFolder getFolder(int folderId)
	{
		if (folderId <= 0)
		{
			new IllegalArgumentException("Folder id is needed.");
		}
		
		return privateMessageDao.selectFolder(folderId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getFolderMessagesCount(int folderId)
	{
		if (folderId <= 0)
		{
			throw new IllegalArgumentException("folder information is needed");
		}
		
		return privateMessageDao.selectFolderMessageCount(folderId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PrivateMessage getPrivateMessage(int privateMessageId)
	{
		if (privateMessageId <= 0)
		{
			throw new IllegalArgumentException();
		}
		
		PrivateMessage privateMessage = privateMessageDao.selectById(privateMessageId);
		
		if (privateMessage != null)
		{
			if (privateMessage.getPost() != null)
			{
				PostUtil.preparePostForDisplay(privateMessage.getPost());
			}
			
			// user signature
			if (privateMessage.getFromUser() != null)
			{
				User fromUser = privateMessage.getFromUser();
				if (fromUser.getAttachSignatureEnabled() && ((fromUser.getSignature() != null) && (fromUser.getSignature().trim().length() > 0)))
				{
					fromUser.setSignature(PostUtil.processText(fromUser.getSignature()));
				}
				
				//ignore smilies in the signature to UI
			}
			
			List<Integer> bccUserIds = privateMessage.getBccUserIds();
			// bcc users
			if (privateMessage.getType() == PrivateMessage.PrivateMessageType.SENT.getType() &&  bccUserIds != null && !bccUserIds.isEmpty())
			{
				for (Integer bccUserId : bccUserIds)
				{
					User bccUser = jforumUserService.getByUserId(bccUserId);
					if (bccUser != null)
					{
						privateMessage.getBccUsers().add(bccUser);
					}
				}
			}
		}
				
		return privateMessage;		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Attachment getPrivateMessageAttachment(int attachmentId)
	{
		if (attachmentId <= 0) throw new IllegalArgumentException("Attachment id should be greater than 0");
		
		return attachmentDao.selectAttachmentById(attachmentId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PrivateMessage getPrivateMessageDraft(int privateMessageId)
	{
		if (privateMessageId <= 0)
		{
			throw new IllegalArgumentException();
		}
		
		PrivateMessage privateMessage = privateMessageDao.selectDraftById(privateMessageId);
		
		if (privateMessage != null)
		{
			if (privateMessage.getPost() != null)
			{
				PostUtil.preparePostForDisplay(privateMessage.getPost());
			}
			
			// user signature
			if (privateMessage.getFromUser() != null)
			{
				User fromUser = privateMessage.getFromUser();
				if (fromUser.getAttachSignatureEnabled() && ((fromUser.getSignature() != null) && (fromUser.getSignature().trim().length() > 0)))
				{
					fromUser.setSignature(PostUtil.processText(fromUser.getSignature()));
				}
				
				//ignore smilies in the signature to UI
			}
		}
				
		return privateMessage;		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getUserDraftsCount(String context, String sakaiUserId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("Sakai user id is needed");
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			return 0;
		}
		
		return privateMessageDao.selectDraftsCount(context, user.getId());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessageFolder> getUserFolders(String context, String sakaiUserId) throws JForumAccessException
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User sakai id is needed.");
		}
		
		//access check for user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(context, sakaiUserId);
		
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(context, sakaiUserId);
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// user folders
		List<PrivateMessageFolder> userFolders = privateMessageDao.selectFolders(context, user.getId());
		
		if (userFolders.size() > 0)
		{
			// user folders messages unread count
			Map<Integer, Integer> foldersUnreadCountMap = privateMessageDao.selectFoldersUnreadCount(context, user.getId());
			
			if (foldersUnreadCountMap.size() > 0)
			{
				int unreadCount = 0;
				for (PrivateMessageFolder userFolder : userFolders)
				{
					unreadCount = 0;
					if (foldersUnreadCountMap.containsKey(userFolder.getId()))
					{
						unreadCount = foldersUnreadCountMap.get(userFolder.getId());
						
						if (unreadCount > 0)
						{
							((PrivateMessageFolderImpl)userFolder).setUnreadCount(unreadCount);
						}
					}
				}
				
				// inbox unread count, key is zero
				/*unreadCount = foldersUnreadCountMap.get(0);
				
				PrivateMessageFolderImpl pmFolder = new PrivateMessageFolderImpl();
				pmFolder.setId(0);
				pmFolder.setSiteId(context);
				pmFolder.setUserId(user.getId());
				pmFolder.setFolderName("");
				((PrivateMessageFolderImpl)pmFolder).setUnreadCount(unreadCount);
				
				userFolders.add(pmFolder);*/
			}
		}
		
		return userFolders;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Map<Integer, Integer> getUserFoldersUnreadCount(String context, String sakaiUserId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("Sakai user id is needed");
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			return new HashMap<Integer, Integer>();
		}
		
		return privateMessageDao.selectFoldersUnreadCount(context, user.getId());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getUserFolderUnreadCount(String context, String sakaiUserId, int folderId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("Sakai user id is needed");
		}
		
		// if folderId is zero fetch inbox unread count
		if (folderId < 0)
		{
			throw new IllegalArgumentException("folder id is needed");
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			return 0;
		}
		
		return privateMessageDao.selectFolderUnreadCount(context, user.getId(), folderId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getUserUnreadCount(String context, String sakaiUserId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("Sakai user id is needed");
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			return 0;
		}
		
		return privateMessageDao.selectUnreadCount(context, user.getId());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessage> inbox(String context, String sakaiUserId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("Context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("sakai User Id is needed.");
		}
		
		List<PrivateMessage> pmList = privateMessageDao.inbox(context, sakaiUserId, true);
		
		for (PrivateMessage pm : pmList)
		{
			PostUtil.preparePostForDisplay(pm.getPost());
		}
		return pmList;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessage> inbox(String context, String sakaiUserId, boolean includeFolderMessages)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("Context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("sakai User Id is needed.");
		}
		
		List<PrivateMessage> pmList = privateMessageDao.inbox(context, sakaiUserId, includeFolderMessages);
		
		for (PrivateMessage pm : pmList)
		{
			PostUtil.preparePostForDisplay(pm.getPost());
		}
		return pmList;
		
	}
	
	public void init()
	{
		if (logger.isInfoEnabled())
			logger.info("init....");
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void markMessageRead(int messageId, String sakaiUserId, Date readTime, boolean isRead)
	{
		if (messageId == 0)
		{
			new IllegalArgumentException();
		}
		
		int messageType = isRead ? PrivateMessage.PrivateMessageType.READ.getType() : PrivateMessage.PrivateMessageType.NEW.getType();
		
		privateMessageDao.updateMessageType(messageId, messageType);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyPrivateMessageFolder(String context, String sakaiUserId, int folderId, String folderName) throws JForumAccessException
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
			throw new IllegalArgumentException("Folder id is needed.");
		}
		
		if (folderName == null || folderName.trim().length() == 0)
		{
			throw new IllegalArgumentException("Folder name is needed.");
		}
		
		//access check for user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(context, sakaiUserId);
				
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(context, sakaiUserId);
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		if (user == null)
		{
			return;
		}
		
		privateMessageDao.updatePrivateMessageFolder(user.getId(), folderId, folderName);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyPrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> toSakaiUserIds, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		if (privateMessage == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (privateMessage.getId() < 0)
		{
			throw new IllegalArgumentException("Private message draft missing id.");
		}
	
		// while saving as draft the private message no need to have to users
		if (privateMessage.getFromUser() == null)
		{
			throw new IllegalArgumentException("From user and To users information is needed");
		}
		
		if (privateMessage.getToUser() != null)
		{
			throw new IllegalArgumentException("To user is not needed for drafts. Use draft user id's to save draft message To users");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		// check with exiting private message draft(from user, context etc)
		PrivateMessage exisPrivateMessageDraft = privateMessageDao.selectDraftById(privateMessage.getId());
		
		if (exisPrivateMessageDraft == null)
		{
			throw new IllegalArgumentException("Private message draft is not existing.");
		}
		
		if (exisPrivateMessageDraft.getContext() == null || !exisPrivateMessageDraft.getContext().equalsIgnoreCase(privateMessage.getContext()))
		{
			throw new IllegalArgumentException("Private message draft context is not the same the existing one.");
		}
		
		if (exisPrivateMessageDraft.getFromUser().getId() != privateMessage.getFromUser().getId())
		{
			throw new IllegalArgumentException("Private message draft user is not the same the existing user.");
		}
		
		if (exisPrivateMessageDraft.getType() != privateMessage.getType())
		{
			throw new IllegalArgumentException("Private message draft type cannot be changed.");
		}
		
		if (privateMessage.getType() == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType() && (toSakaiUserIds != null) && (privateMessage.getDraftToUserIds().size() != 1))
		{
			throw new IllegalArgumentException("Private message draft for reply private messages should have one user.");
		}
		
		if (privateMessage.getType() != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			bccSakaiUserIds = null;
			((PrivateMessageImpl)privateMessage).setBccUserIds(null);
		}
		
		List<User> toUsers = new ArrayList<User>();
		
		if (toSakaiUserIds != null && !toSakaiUserIds.isEmpty())
		{
			User toJforumUser = null;
			
			boolean facilitator = false;
			boolean participant = false;
			for (String toSakaiUserId : toSakaiUserIds)
			{
				if (toSakaiUserId == null || toSakaiUserId.trim().length() == 0)
				{
					continue;
				}
				toJforumUser = jforumUserService.getBySakaiUserId(toSakaiUserId);
				if (toJforumUser != null)
				{
					//access check To user
					facilitator = false;
					participant = false;
					
					facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), toSakaiUserId);
					if (!facilitator)
					{
						participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), toSakaiUserId);
					}
					
					if (!(facilitator || participant))
					{
						continue;
					}
					
					toUsers.add(toJforumUser);
				}
			}
		}
		
		// reply messages can have bcc users and same with reply message drafts
		if (privateMessage.getType() == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			List<User> bccUsers = new ArrayList<User>();
			
			if (bccSakaiUserIds != null && !bccSakaiUserIds.isEmpty())
			{
				User bccJforumUser = null;
				
				boolean facilitator = false;
				boolean participant = false;
				for (String bccSakaiUserId : bccSakaiUserIds)
				{
					if (bccSakaiUserId == null || bccSakaiUserId.trim().length() == 0)
					{
						continue;
					}
					bccJforumUser = jforumUserService.getBySakaiUserId(bccSakaiUserId);
					if (bccJforumUser != null)
					{
						//access check To user
						facilitator = false;
						participant = false;
						
						facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), bccSakaiUserId);
						if (!facilitator)
						{
							participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), bccSakaiUserId);
						}
						
						if (!(facilitator || participant))
						{
							continue;
						}
						
						bccUsers.add(bccJforumUser);
					}
				}
				
				List<Integer> bccUserIds = new ArrayList<Integer>();
				for (User bccUser : bccUsers)
				{
					bccUserIds.add(new Integer(bccUser.getId()));
				}
				((PrivateMessageImpl)privateMessage).setBccUserIds(bccUserIds);
			}
			else
			{
				((PrivateMessageImpl)privateMessage).setBccUserIds(null);
			}
		}
		
		//access check for from user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getFromUser().getSakaiUserId());
		}
		
		Post post = privateMessage.getPost();
		
		String cleanedPostText = HtmlHelper.clean(post.getText(), true);
		// strip body tags
		cleanedPostText = JForumUtil.stripBodyTags(cleanedPostText);
		// escape script tag
		cleanedPostText = JForumUtil.escapeScriptTag(cleanedPostText);
		post.setText(cleanedPostText);
		
		// remove script tags
		post.setSubject(JForumUtil.removeScriptTags(post.getSubject()));
		
		//attachments
		if (post.hasAttachments())
		{
			jforumAttachmentService.validateEditPostAttachments(post);
		}
		
		// for reply private message the draftToUserIds is already set from the existing message
		if (privateMessage.getType() != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			List<Integer>  draftToUserIds = new ArrayList<Integer>();
			for (User toUser : toUsers)
			{
				draftToUserIds.add(new Integer(toUser.getId()));
			}
		
			((PrivateMessageImpl)privateMessage).setDraftToUserIds(draftToUserIds);
		}
				
		// modify private message draft
		privateMessageDao.updateDraft(privateMessage);
		
		// modify private message draft attachments
		try
		{
			jforumAttachmentService.processEditPrivateMessageAttachments(privateMessage);
		}
		catch (JForumAttachmentOverQuotaException e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn(" Private Message - Draft '" + post.getSubject() + "' and with id "+ post.getId() +" attachment(s) are not saved. " + e.toString());
			}
		}
		catch(JForumAttachmentBadExtensionException e1)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn(" Private Message - Draft '" + post.getSubject() + "' and with id "+ post.getId() +" attachment(s) are not saved. " + e1.toString());
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void moveTofolder(String context, String sakaiUserId, int privateMessageId, int moveToFolderId) throws JForumAccessException
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("Sakai User id is needed.");
		}
		
		if (privateMessageId < 0)
		{
			throw new IllegalArgumentException("message id is needed.");
		}
		
		/*if (moveToFolderId < 0)
		{
			throw new IllegalArgumentException("folder id is needed.");
		}*/
		
		//access check for user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(context, sakaiUserId);
		
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(context, sakaiUserId);
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		if (user == null)
		{
			return;
		}
		
		privateMessageDao.moveTofolder(context, user.getId(), privateMessageId, moveToFolderId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Attachment newAttachment(String fileName, String contentType, String comments, byte[] fileContent)
	{
		if ((fileName == null || fileName.trim().length() == 0) ||(fileContent == null || fileContent.length == 0))
		{
			return null;
		}
		
		// Get only the filename, without the path
		String separator = "/";
		int index = fileName.indexOf(separator);

		if (index == -1)
		{
			separator = "\\";
			index = fileName.indexOf(separator);
		}

		if (index > -1)
		{
			if (separator.equals("\\"))
			{
				separator = "\\\\";
			}

			String[] p = fileName.split(separator);
			fileName = p[p.length - 1];
		}
		
		Attachment attachment = new AttachmentImpl();
		
		AttachmentInfoImpl attachmentInfo = new AttachmentInfoImpl();
		attachmentInfo.setMimetype(contentType);
		attachmentInfo.setRealFilename(fileName);
		attachmentInfo.setComment(comments);
		attachmentInfo.setFileContent(fileContent);
		
		attachment.setInfo(attachmentInfo);
		
		return attachment;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PrivateMessage newPrivateMessage(int existingPrivateMessageId)
	{
		PrivateMessage existingPrivateMessage = privateMessageDao.selectById(existingPrivateMessageId);
		
		if (existingPrivateMessage == null)
		{
			return null;
		}
		
		int id = existingPrivateMessage.getId();
		String context = existingPrivateMessage.getContext();
		
		User toUser = null;
		User fromUser = null;
		if (existingPrivateMessage.getType() == PrivateMessage.PrivateMessageType.SENT.getType())
		{
			// reply to message sent
			toUser = existingPrivateMessage.getToUser();
			fromUser = existingPrivateMessage.getFromUser();
		}
		else
		{
			// reply to message received
			fromUser = existingPrivateMessage.getToUser();
			toUser = existingPrivateMessage.getFromUser();
		}
				
		PrivateMessage privateMessage = new PrivateMessageImpl(id, context, fromUser, toUser);
				
		return privateMessage;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PrivateMessage newPrivateMessage(String fromSakaiUserId)
	{
		PrivateMessage privateMessage = new PrivateMessageImpl();
		
		privateMessage.setPost(new PostImpl());
		
		User fromUser = jforumUserService.getBySakaiUserId(fromSakaiUserId);
		privateMessage.setFromUser(fromUser);
		
		return privateMessage;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PrivateMessage newPrivateMessage(String fromSakaiUserId, String toSakaiUserId)
	{
		PrivateMessage privateMessage = new PrivateMessageImpl();
		
		privateMessage.setPost(new PostImpl());
		
		User fromUser = jforumUserService.getBySakaiUserId(fromSakaiUserId);
		privateMessage.setFromUser(fromUser);
		
		User toUser = jforumUserService.getBySakaiUserId(toSakaiUserId);
		privateMessage.setToUser(toUser);
		
		return privateMessage;
	}

	/**
	 * {@inheritDoc}
	 */
	public PrivateMessage newPrivateMessageReplyDraft(int existingPrivateMessageId)
	{
		PrivateMessage existingPrivateMessage = privateMessageDao.selectById(existingPrivateMessageId);
		
		if (existingPrivateMessage == null)
		{
			return null;
		}
		
		int id = existingPrivateMessage.getId();
		String context = existingPrivateMessage.getContext();
		
		User toUser = null;
		User fromUser = null;
		if (existingPrivateMessage.getType() == PrivateMessage.PrivateMessageType.SENT.getType())
		{
			// reply to message sent
			toUser = existingPrivateMessage.getToUser();
			fromUser = existingPrivateMessage.getFromUser();
		}
		else
		{
			// reply to message received
			fromUser = existingPrivateMessage.getToUser();
			toUser = existingPrivateMessage.getFromUser();
		}
				
		PrivateMessage privateMessage = new PrivateMessageImpl(context, fromUser, toUser, id);
				
		return privateMessage;	
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void replyPrivateMessage(PrivateMessage privateMessage) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		if (privateMessage == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (privateMessage.getId() <= 0)
		{
			throw new IllegalArgumentException("Existing message id is needed.");
		}
		
		if ((privateMessage.getFromUser() == null) || (privateMessage.getToUser() == null))
		{
			throw new IllegalArgumentException("From user and To user information is needed");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		//access check from user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getFromUser().getSakaiUserId());
		}
		
		//access check to user
		facilitator = false;
		participant = false;
		
		facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getToUser().getSakaiUserId());
		}
		
		Post post = privateMessage.getPost();
		
		//attachments
		if (post.hasAttachments())
		{
			jforumAttachmentService.validatePostAttachments(post);
		}
		
		replyToPrivateMessage(privateMessage);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void replyPrivateMessage(PrivateMessage privateMessage, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		if (privateMessage == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (privateMessage.getId() <= 0)
		{
			throw new IllegalArgumentException("Existing message id is needed.");
		}
		
		if ((privateMessage.getFromUser() == null) || (privateMessage.getToUser() == null))
		{
			throw new IllegalArgumentException("From user and To user information is needed");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		//access check from user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getFromUser().getSakaiUserId());
		}
		
		//access check to user
		facilitator = false;
		participant = false;
		
		facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getToUser().getSakaiUserId());
		}
		
		// set bcc users
		setBccUsers(privateMessage, bccSakaiUserIds);		
		
		Post post = privateMessage.getPost();
		
		//attachments
		if (post.hasAttachments())
		{
			jforumAttachmentService.validatePostAttachments(post);
		}
		
		replyToPrivateMessage(privateMessage);		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void savePrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> toSakaiUserIds, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		if (privateMessage == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (privateMessage.getId() > 0)
		{
			throw new IllegalArgumentException("New private message cannot be created as private message having id");
		}
	
		// while saving as draft the private message no need to have to users
		if (privateMessage.getFromUser() == null)
		{
			throw new IllegalArgumentException("From user information is needed");
		}
		
		if (privateMessage.getToUser() != null)
		{
			throw new IllegalArgumentException("To user is not needed for drafts. Use draft user id's to save draft message To users");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		if (!(privateMessage.getType() != PrivateMessage.PrivateMessageType.DRAFT.getType() || privateMessage.getType() != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType()))
		{
			throw new IllegalArgumentException("Private message draft type is needed");
		}
		
		if (privateMessage.getType() == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType() && (toSakaiUserIds != null) && (privateMessage.getDraftToUserIds().size() != 1))
		{
			throw new IllegalArgumentException("Private message draft for reply private messages should have one user.");
		}
		
		if (privateMessage.getType() != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			bccSakaiUserIds = null;
			((PrivateMessageImpl)privateMessage).setBccUserIds(null);
		}
		
		List<User> toUsers = new ArrayList<User>();
		
		if (toSakaiUserIds != null && !toSakaiUserIds.isEmpty())
		{
			User toJforumUser = null;
			
			boolean facilitator = false;
			boolean participant = false;
			for (String toSakaiUserId : toSakaiUserIds)
			{
				if (toSakaiUserId == null || toSakaiUserId.trim().length() == 0)
				{
					continue;
				}
				toJforumUser = jforumUserService.getBySakaiUserId(toSakaiUserId);
				if (toJforumUser != null)
				{
					//access check To user
					facilitator = false;
					participant = false;
					
					facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), toSakaiUserId);
					
					// Observer not allowed to perform private message functions
					boolean observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), toSakaiUserId);
										
					if (!facilitator)
					{
						if (!observer)
						{
							participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), toSakaiUserId);
						}
					}
					
					if (!(facilitator || participant))
					{
						continue;
					}
					
					toUsers.add(toJforumUser);
				}
			}
		}
		
		// reply messages can have bcc users and same with reply message drafts
		if (privateMessage.getType() == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			List<User> bccUsers = new ArrayList<User>();
			
			if (bccSakaiUserIds != null && !bccSakaiUserIds.isEmpty())
			{
				User bccJforumUser = null;
				
				boolean facilitator = false;
				boolean participant = false;
				for (String bccSakaiUserId : bccSakaiUserIds)
				{
					if (bccSakaiUserId == null || bccSakaiUserId.trim().length() == 0)
					{
						continue;
					}
					bccJforumUser = jforumUserService.getBySakaiUserId(bccSakaiUserId);
					if (bccJforumUser != null)
					{
						//access check To user
						facilitator = false;
						participant = false;
						
						facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), bccSakaiUserId);
						
						// Observer not allowed to perform private message functions
						boolean observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), bccSakaiUserId);
						
						if (!facilitator)
						{
							if (!observer)
							{
								participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), bccSakaiUserId);
							}
						}
						
						if (!(facilitator || participant))
						{
							continue;
						}
						
						bccUsers.add(bccJforumUser);
					}
				}
				
				List<Integer> bccUserIds = new ArrayList<Integer>();
				for (User bccUser : bccUsers)
				{
					bccUserIds.add(new Integer(bccUser.getId()));
				}
				((PrivateMessageImpl)privateMessage).setBccUserIds(bccUserIds);
			}
		}
		
		//access check for from user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		boolean participant = false;
		if (!facilitator)
		{
			participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getFromUser().getSakaiUserId());
		}
		
		Post post = privateMessage.getPost();
		
		String cleanedPostText = HtmlHelper.clean(post.getText(), true);
		// strip body tags
		cleanedPostText = JForumUtil.stripBodyTags(cleanedPostText);
		// escape script tag
		cleanedPostText = JForumUtil.escapeScriptTag(cleanedPostText);
		post.setText(cleanedPostText);
		
		// remove script tags
		post.setSubject(JForumUtil.removeScriptTags(post.getSubject()));
		
		//attachments
		if (post.hasAttachments())
		{
			jforumAttachmentService.validatePostAttachments(post);
		}
		
		// for reply private message the draftToUserIds is already set from the existing message
		if (privateMessage.getType() != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			List<Integer>  draftToUserIds = new ArrayList<Integer>();
			for (User toUser : toUsers)
			{
				draftToUserIds.add(new Integer(toUser.getId()));
			}
			
			((PrivateMessageImpl)privateMessage).setDraftToUserIds(draftToUserIds);
		}
		
		// save private message draft
		privateMessageDao.saveDraft(privateMessage);
		
		// process attachments
		if (post.hasAttachments())
		{
			try
			{
				Integer draftPMId = privateMessage.getId();
				
				jforumAttachmentService.processPrivateMessageDraftAttachments(privateMessage, draftPMId);
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - Draft '" + post.getSubject() + "' and with id "+ post.getId() +" attachment(s) are not saved. " + e.toString());
				}
			}
			catch(JForumAttachmentBadExtensionException e1)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - Draft '" + post.getSubject() + "' and with id "+ post.getId() +" attachment(s) are not saved. " + e1.toString());
				}
			}
		}
		
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
		
		String sql = "SELECT folder_id, course_id, user_id, folder_name FROM jforum_privmsgs_folders WHERE course_id = ? AND user_id = ?";
		
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
	public void sendDraftedPrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{

		if (privateMessage == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (privateMessage.getId() <= 0)
		{
			throw new IllegalArgumentException("New private message cannot be created as private message draft id is missing");
		}
		// check for the saved drafted message and deleted the draft after sending		
		if ((privateMessage.getFromUser() == null) || (privateMessage.getToUser() == null))
		{
			throw new IllegalArgumentException("From user and To user information is needed");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
				
		// check with exiting private message draft(from user, context etc)
		PrivateMessage privateMessageDraft = privateMessageDao.selectDraftById(privateMessage.getId());
		
		if (privateMessageDraft == null)
		{
			throw new IllegalArgumentException("Private message draft is not existing.");
		}
		
		if (privateMessageDraft.getContext() == null || !privateMessageDraft.getContext().equalsIgnoreCase(privateMessage.getContext()))
		{
			throw new IllegalArgumentException("Private message draft context is not the same the existing one.");
		}
		
		if (privateMessageDraft.getFromUser().getId() != privateMessage.getFromUser().getId())
		{
			throw new IllegalArgumentException("Private message draft user is not the same the existing user.");
		}
		
		if (privateMessageDraft.getType() != privateMessage.getType())
		{
			throw new IllegalArgumentException("Private message draft type cannot be changed.");
		}
		
		if (privateMessageDraft.getType() != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			bccSakaiUserIds = null;
			((PrivateMessageImpl)privateMessage).setBccUserIds(null);
		}
		
		//access check from user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		// Observer not allowed to perform private message functions
		boolean observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
						
		boolean participant = false;
		if (!facilitator)
		{
			if (!observer)
			{
				participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
			}
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getFromUser().getSakaiUserId());
		}
		
		if (privateMessageDraft.getType() == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			int toUserId = privateMessage.getToUser().getId();
			List<Integer> draftToUserIds= privateMessageDraft.getDraftToUserIds();
			
			if (draftToUserIds != null && draftToUserIds.size() == 1)
			{
				int draftToUserId = draftToUserIds.get(0);
				
				if (draftToUserId != toUserId)
				{
					throw new IllegalArgumentException("To user cannot be changed in the reply private message.");
				}
				
				((PrivateMessageImpl)privateMessage).setDraftToUserIds(draftToUserIds);
			}
			else
			{
				throw new IllegalArgumentException("To user id is missing in the reply private message draft.");
			}
			
		}
		
		//access check to user
		facilitator = false;
		participant = false;
		observer = false;
		
		facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
		
		// Observer not allowed to perform private message functions
		observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
						
		if (!facilitator)
		{
			if (!observer)
			{
				participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
			}
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getToUser().getSakaiUserId());
		}
		
		// set bcc users
		setBccUsers(privateMessage, bccSakaiUserIds);
		
		Post post = privateMessage.getPost();
		
		String cleanedPostText = HtmlHelper.clean(post.getText(), true);
		// strip body tags
		cleanedPostText = JForumUtil.stripBodyTags(cleanedPostText);
		// escape script tag
		cleanedPostText = JForumUtil.escapeScriptTag(cleanedPostText);
		post.setText(cleanedPostText);
		
		// remove script tags
		post.setSubject(JForumUtil.removeScriptTags(post.getSubject()));
		
		//attachments
		if (post.hasAttachments())
		{
			// jforumAttachmentService.validatePostAttachments(post);
			jforumAttachmentService.validateSendingPrivateMessageDraftAttachments(post);
		}
		
		// delete draft after sending the private message
		//((PrivateMessageImpl)privateMessage).setId(0);
				
		// clean html
		String cleanedHtml = HtmlHelper.clean(privateMessage.getPost().getText(), true);
		// strip body tags
		cleanedPostText = JForumUtil.stripBodyTags(cleanedPostText);
		// escape script tag
		cleanedPostText = JForumUtil.escapeScriptTag(cleanedPostText);
		privateMessage.getPost().setText(cleanedHtml);
		
		// save the message
		List<Integer> pmIds = privateMessageDao.saveMessage(privateMessage);

		if (post.hasAttachments())
		{
			try
			{
				jforumAttachmentService.processPrivateMessageAttachments(privateMessage, pmIds);
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - '" + privateMessage.getPost().getSubject()  + "' and with id's "+ pmIds.toString() +" attachment(s) are not saved. " + e.toString());
				}
				post.setHasAttachments(false);
			}
			catch(JForumAttachmentBadExtensionException e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - '" + privateMessage.getPost().getSubject()  + "' and with id's "+ pmIds.toString() +" attachment(s) are not saved. " + e.toString());
				}
				post.setHasAttachments(false);
			}
		}
		
		// delete draft
		List<Integer> existingDraftIds = new ArrayList<Integer>();
		existingDraftIds.add(privateMessageDraft.getId());
		try
		{
			deleteDraft(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId(), existingDraftIds);
		}
		catch (JForumAccessException e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("Error while deleting the draft(s)", e);
			} 
		}
		
		// send email if user opted to receive private message email's
		sendPrivateMessageEmail(privateMessage);
		
		// send email to bcc users if user opted to receive private message email's
		if (privateMessage.getBccUsers() != null && !privateMessage.getBccUsers().isEmpty())
		{
			for (User bccUser : privateMessage.getBccUsers())
			{
				privateMessage.setToUser(bccUser);
				sendPrivateMessageEmail(privateMessage);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void sendDraftedPrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> toSakaiUserIds, List<String> bccSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		if (privateMessage == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (privateMessage.getId() <= 0)
		{
			throw new IllegalArgumentException("New private message cannot be created as private message draft id is missing");
		}
		
		if (privateMessage.getFromUser() == null)
		{
			throw new IllegalArgumentException("From user information is needed");
		}
		
		if ((privateMessage.getType() != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType()) && ((toSakaiUserIds == null) || (toSakaiUserIds.size() == 0)))
		{
			throw new IllegalArgumentException("To users information is needed");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		if (privateMessage.getType() == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType() && (toSakaiUserIds != null) && (privateMessage.getDraftToUserIds().size() != 1))
		{
			throw new IllegalArgumentException("Private message draft for reply private messages should have one user.");
		}
		
		int privateMessageDraftId = privateMessage.getId();
		
		PrivateMessage exisPrivateMessageDraft = getPrivateMessageDraft(privateMessageDraftId);
		
		if (exisPrivateMessageDraft == null)
		{	
			throw new IllegalArgumentException("Private message draft is not existing.");
		}
		
		if (exisPrivateMessageDraft.getContext() == null || !exisPrivateMessageDraft.getContext().equalsIgnoreCase(privateMessage.getContext()))
		{
			throw new IllegalArgumentException("Private message draft context is not the same the existing one.");
		}
		
		if (exisPrivateMessageDraft.getFromUser().getId() != privateMessage.getFromUser().getId())
		{
			throw new IllegalArgumentException("Private message draft user is not the same the existing user.");
		}
		
		if (exisPrivateMessageDraft.getType() != privateMessage.getType())
		{
			throw new IllegalArgumentException("Private message draft type cannot be changed.");
		}
		
		if (exisPrivateMessageDraft.getType() != PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			bccSakaiUserIds = null;
			((PrivateMessageImpl)privateMessage).setBccUserIds(null);
		}
		/*check for the saved drafted message and deleted the draft after sending*/
		
		List<User> toUsers = new ArrayList<User>();
		
		List<Integer> pmIds = new ArrayList<Integer>();
		
		User toJforumUser = null;
		
		boolean facilitator = false;
		boolean participant = false;
		boolean observer = false;
		
		if (privateMessage.getType() == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
		{
			List<Integer> draftToUserIds = privateMessage.getDraftToUserIds();
		
			if (draftToUserIds!= null && draftToUserIds.size() == 1)
			{
				// reply should have only one to user
				int toUserId = draftToUserIds.get(0);
				
				toJforumUser = jforumUserService.getByUserId(toUserId);
				if (toJforumUser != null)
				{
					//access check To user
					facilitator = false;
					participant = false;
					
					facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), toJforumUser.getSakaiUserId());
					
					// Observer not allowed to perform private message functions
					observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), toJforumUser.getSakaiUserId());
							
					if (!facilitator)
					{
						if (!observer)
						{
							participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), toJforumUser.getSakaiUserId());
						}
					}
					
					if (!(facilitator || participant))
					{
						throw new JForumAccessException(toJforumUser.getSakaiUserId());
					}
					
					toUsers.add(toJforumUser);
				}
				else
				{
					throw new IllegalArgumentException("To user is not in the jforum.");
				}
			}
			else
			{
				throw new IllegalArgumentException("To user id is missing in the reply private message draft.");
			}
		}
		else
		{
			for (String toSakaiUserId : toSakaiUserIds)
			{
				if (toSakaiUserId == null || toSakaiUserId.trim().length() == 0)
				{
					continue;
				}
				toJforumUser = jforumUserService.getBySakaiUserId(toSakaiUserId);
				if (toJforumUser != null)
				{
					//access check To user
					facilitator = false;
					participant = false;
					observer = false;
					
					facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), toSakaiUserId);
					
					// Observer not allowed to perform private message functions
					observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), toSakaiUserId);
							
					if (!facilitator)
					{
						if (!observer)
						{
							participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), toSakaiUserId);
						}
					}
					
					if (!(facilitator || participant))
					{
						// throw new JForumAccessException(toSakaiUserId);
						continue;
					}
					
					toUsers.add(toJforumUser);
				}
			}			
		}
		
		if (toUsers.size() == 0)
		{
			return;
		}
		
		//access check from user
		facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		// Observer not allowed to perform private message functions
		observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
				
		participant = false;
		if (!facilitator)
		{
			if (!observer)
			{
				participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
			}
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getFromUser().getSakaiUserId());
		}
		
		// set bcc users
		if (privateMessage.getType() == PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType() && (toSakaiUserIds.size() == 1))
		{
			setBccUsers(privateMessage, bccSakaiUserIds);
		}
		
		Post post = privateMessage.getPost();
		
		String cleanedPostText = HtmlHelper.clean(post.getText(), true);
		// strip body tags
		cleanedPostText = JForumUtil.stripBodyTags(cleanedPostText);
		// escape script tag
		cleanedPostText = JForumUtil.escapeScriptTag(cleanedPostText);
		post.setText(cleanedPostText);
		
		// remove script tags
		post.setSubject(JForumUtil.removeScriptTags(post.getSubject()));
		
		//attachments
		if (post.hasAttachments())
		{
			// jforumAttachmentService.validatePostAttachments(post);
			jforumAttachmentService.validateSendingPrivateMessageDraftAttachments(post);
		}
		
		for (User toUser : toUsers)
		{
			privateMessage.setToUser(toUser);
			
			// save the message
			List<Integer> createdPmIds = privateMessageDao.saveMessage(privateMessage);
			
			pmIds.addAll(createdPmIds);
		}
		
		// process attachments
		if (post.hasAttachments())
		{
			try
			{
				jforumAttachmentService.processPrivateMessageAttachments(privateMessage, pmIds);
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - Post '" + post.getSubject() + "' and with id "+ post.getId() +" attachment(s) are not saved. " + e.toString());
				}
			}
			catch(JForumAttachmentBadExtensionException e1)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - Post '" + post.getSubject() + "' and with id "+ post.getId() +" attachment(s) are not saved. " + e1.toString());
				}
			}
		}
		
		// delete draft
		List<Integer> existingDraftIds = new ArrayList<Integer>();
		existingDraftIds.add(privateMessageDraftId);
		try
		{
			deleteDraft(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId(), existingDraftIds);
		}
		catch (JForumAccessException e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("Error while deleting the draft(s)", e);
			}
		}
		
		//send email if user opted to receive private message email's
		for (User toUser : toUsers)
		{
			privateMessage.setToUser(toUser);
			sendPrivateMessageEmail(privateMessage);
		}
		
		// send email to bcc users if user opted to receive private message email's
		if (privateMessage.getBccUsers() != null && !privateMessage.getBccUsers().isEmpty())
		{
			for (User bccUser : privateMessage.getBccUsers())
			{
				privateMessage.setToUser(bccUser);
				sendPrivateMessageEmail(privateMessage);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void sendPrivateMessage(PrivateMessage privateMessage)
	{
		if (privateMessage == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (privateMessage.getId() > 0)
		{
			throw new IllegalArgumentException("New private message cannot be created as private message having id");
		}
		
		if ((privateMessage.getFromUser() == null) || (privateMessage.getToUser() == null))
		{
			throw new IllegalArgumentException("From user and To user information is needed");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		// clean html
		String cleanedPostText = HtmlHelper.clean(privateMessage.getPost().getText(), true);
		// strip body tags
		cleanedPostText = JForumUtil.stripBodyTags(cleanedPostText);
		// escape script tag
		cleanedPostText = JForumUtil.escapeScriptTag(cleanedPostText);
		privateMessage.getPost().setText(cleanedPostText);
		
		// save the message
		List<Integer> pmIds = privateMessageDao.saveMessage(privateMessage);
		
		// process attachments
		Post post = privateMessage.getPost();
		if (post.hasAttachments())
		{
			try
			{
				jforumAttachmentService.processPrivateMessageAttachments(privateMessage, pmIds);
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - '" + privateMessage.getPost().getSubject()  + "' and with id's "+ pmIds.toString() +" attachment(s) are not saved. " + e.toString());
				}
				post.setHasAttachments(false);
			}
			catch(JForumAttachmentBadExtensionException e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - '" + privateMessage.getPost().getSubject()  + "' and with id's "+ pmIds.toString() +" attachment(s) are not saved. " + e.toString());
				}
				post.setHasAttachments(false);
			}
		}
		
		// send email if user opted to receive private message email's
		sendPrivateMessageEmail(privateMessage);
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendPrivateMessageWithAttachments(PrivateMessage privateMessage) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		if (privateMessage == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (privateMessage.getId() > 0)
		{
			throw new IllegalArgumentException("New private message cannot be created as private message having id");
		}
		
		if ((privateMessage.getFromUser() == null) || (privateMessage.getToUser() == null))
		{
			throw new IllegalArgumentException("From user and To user information is needed");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		//access check from user
		boolean facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		// Observer not allowed to perform private message functions
		boolean observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		boolean participant = false;
		if (!facilitator)
		{
			if (!observer)
			{
				participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
			}
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getFromUser().getSakaiUserId());
		}
		
		//access check to user
		facilitator = false;
		participant = false;
		
		facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
		
		// Observer not allowed to perform private message functions
		observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
		
		if (!facilitator)
		{
			if (!observer)
			{
				participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getToUser().getSakaiUserId());
			}
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getToUser().getSakaiUserId());
		}
		
		Post post = privateMessage.getPost();
		
		String cleanedPostText = HtmlHelper.clean(post.getText(), true);
		// strip body tags
		cleanedPostText = JForumUtil.stripBodyTags(cleanedPostText);
		// escape script tag
		cleanedPostText = JForumUtil.escapeScriptTag(cleanedPostText);
		post.setText(cleanedPostText);
		
		// remove script tags
		post.setSubject(JForumUtil.removeScriptTags(post.getSubject()));
		
		//attachments
		if (post.hasAttachments())
		{
			jforumAttachmentService.validatePostAttachments(post);
		}
		
		sendPrivateMessage(privateMessage);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void sendPrivateMessageWithAttachments(PrivateMessage privateMessage, List<String> toSakaiUserIds) throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		if (privateMessage == null)
		{
			throw new IllegalArgumentException();
		}
		
		if (privateMessage.getId() > 0)
		{
			throw new IllegalArgumentException("New private message cannot be created as private message having id");
		}
		
		if ((privateMessage.getFromUser() == null) || (toSakaiUserIds == null) || (toSakaiUserIds.size() == 0))
		{
			throw new IllegalArgumentException("From user and To users information is needed");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		List<User> toUsers = new ArrayList<User>();
		
		List<Integer> pmIds = new ArrayList<Integer>();
		
		User toJforumUser = null;
		
		boolean facilitator = false;
		boolean participant = false;
		boolean observer = false;
		for (String toSakaiUserId : toSakaiUserIds)
		{
			if (toSakaiUserId == null || toSakaiUserId.trim().length() == 0)
			{
				continue;
			}
			toJforumUser = jforumUserService.getBySakaiUserId(toSakaiUserId);
			if (toJforumUser != null)
			{
				//access check To user
				facilitator = false;
				participant = false;
				observer = false;
				
				facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), toSakaiUserId);
				
				// Observer not allowed to perform private message functions
				observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), toSakaiUserId);
				
				if (!facilitator)
				{
					if (!observer)
					{
						participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), toSakaiUserId);
					}
				}
				
				if (!(facilitator || participant))
				{
					// throw new JForumAccessException(toSakaiUserId);
					continue;
				}
				
				toUsers.add(toJforumUser);
			}
		}
		
		if (toUsers.size() == 0)
		{
			return;
		}
		
		//access check from user
		facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		// Observer not allowed to perform private message functions
		observer = jforumSecurityService.isEtudesObserver(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
		
		participant = false;
		if (!facilitator)
		{
			if (!observer)
			{
				participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), privateMessage.getFromUser().getSakaiUserId());
			}
		}
		
		if (!(facilitator || participant))
		{
			throw new JForumAccessException(privateMessage.getFromUser().getSakaiUserId());
		}
		
		Post post = privateMessage.getPost();
		
		String cleanedPostText = HtmlHelper.clean(post.getText(), true);
		// strip body tags
		cleanedPostText = JForumUtil.stripBodyTags(cleanedPostText);
		// escape script tag
		cleanedPostText = JForumUtil.escapeScriptTag(cleanedPostText);
		post.setText(cleanedPostText);
		
		// remove script tags
		post.setSubject(JForumUtil.removeScriptTags(post.getSubject()));
		
		//attachments
		if (post.hasAttachments())
		{
			jforumAttachmentService.validatePostAttachments(post);
		}
		
		for (User toUser : toUsers)
		{
			privateMessage.setToUser(toUser);
			
			// save the message
			List<Integer> createdPmIds = privateMessageDao.saveMessage(privateMessage);
			
			pmIds.addAll(createdPmIds);
		}
		
		// process attachments
		if (post.hasAttachments())
		{
			try
			{
				jforumAttachmentService.processPrivateMessageAttachments(privateMessage, pmIds);
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - Post '" + post.getSubject() + "' and with id "+ post.getId() +" attachment(s) are not saved. " + e.toString());
				}
			}
			catch(JForumAttachmentBadExtensionException e1)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - Post '" + post.getSubject() + "' and with id "+ post.getId() +" attachment(s) are not saved. " + e1.toString());
				}
			}
		}
		
		//send email if user opted to receive private message email's
		for (User toUser : toUsers)
		{
			privateMessage.setToUser(toUser);
			sendPrivateMessageEmail(privateMessage);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<PrivateMessage> sentbox(String context, String sakaiUserId)
	{
		if (context == null || context.trim().length() == 0)
		{
			throw new IllegalArgumentException("Context is needed.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("sakai User Id is needed.");
		}
		
		List<PrivateMessage> pmList = privateMessageDao.sentbox(context, sakaiUserId);
		
		for (PrivateMessage pm : pmList)
		{
			PostUtil.preparePostForDisplay(pm.getPost());
		}
				
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
	 * @param jforumAttachmentService the jforumAttachmentService to set
	 */
	public void setJforumAttachmentService(JForumAttachmentService jforumAttachmentService)
	{
		this.jforumAttachmentService = jforumAttachmentService;
	}

	/**
	 * @param jforumEmailExecutorService the jforumEmailExecutorService to set
	 */
	public void setJforumEmailExecutorService(JForumEmailExecutorService jforumEmailExecutorService)
	{
		this.jforumEmailExecutorService = jforumEmailExecutorService;
	}
	
	/**
	 * @param jforumSecurityService the jforumSecurityService to set
	 */
	public void setJforumSecurityService(JForumSecurityService jforumSecurityService)
	{
		this.jforumSecurityService = jforumSecurityService;
	}
	
	/**
	 * @param jforumUserService the jforumUserService to set
	 */
	public void setJforumUserService(JForumUserService jforumUserService)
	{
		this.jforumUserService = jforumUserService;
	}
	
	/**
	 * @param privateMessageDao the privateMessageDao to set
	 */
	public void setPrivateMessageDao(PrivateMessageDao privateMessageDao)
	{
		this.privateMessageDao = privateMessageDao;
	}
	
	/**
	 * @param siteService the siteService to set
	 */
	public void setSiteService(SiteService siteService)
	{
		this.siteService = siteService;
	}
	
	/**
	 * @param sqlService 
	 * 			The sqlService to set
	 */
	public void setSqlService(SqlService sqlService)
	{
		this.sqlService = sqlService;
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
	protected String getPmText(ResultSet rs) throws SQLException
	{
		return rs.getString("privmsgs_text");
	}
	
	/**
	 * Sends reply to existing private message
	 * 
	 * @param privateMessage	Private message
	 */
	protected void replyToPrivateMessage(PrivateMessage privateMessage)
	{
		if (privateMessage == null)
		{
			throw new IllegalArgumentException("Private message is null.");
		}
		
		if (privateMessage.getId() <= 0)
		{
			throw new IllegalArgumentException("Existing message id is needed.");
		}
		
		if ((privateMessage.getFromUser() == null) || (privateMessage.getToUser() == null))
		{
			throw new IllegalArgumentException("From user and To user information is needed");
		}
		
		if ((privateMessage.getPost() == null))
		{
			throw new IllegalArgumentException("Private message post information is needed");
		}
		
		if (privateMessage.getContext() == null)
		{
			throw new IllegalArgumentException("Private message context is needed");
		}
		
		// check for existing private message
		PrivateMessage exisPrivateMessage = privateMessageDao.selectById(privateMessage.getId());
		
		if (exisPrivateMessage == null)
		{
			throw new IllegalArgumentException("No existing private message and cannot sent reply.");
		}
		
		privateMessage.setReplied(true);
		
		// save the message
		List<Integer> pmIds = privateMessageDao.saveMessage(privateMessage);
		
		// process attachments
		Post post = privateMessage.getPost();
		if (post.hasAttachments())
		{
			try
			{
				jforumAttachmentService.processPrivateMessageAttachments(privateMessage, pmIds);
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - '" + privateMessage.getPost().getSubject()  + "' and with id's "+ pmIds.toString() +" attachment(s) are not saved. " + e.toString());
				}
				post.setHasAttachments(false);
			}
			catch(JForumAttachmentBadExtensionException e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(" Private Message - '" + privateMessage.getPost().getSubject()  + "' and with id's "+ pmIds.toString() +" attachment(s) are not saved. " + e.toString());
				}
				post.setHasAttachments(false);
			}
		}
		
		// send email if user opted to receive private message email's
		sendPrivateMessageEmail(privateMessage);
		
		// send email to bcc users if user opted to receive private message email's
		if (privateMessage.getBccUsers() != null && !privateMessage.getBccUsers().isEmpty())
		{
			for (User bccUser : privateMessage.getBccUsers())
			{
				privateMessage.setToUser(bccUser);
				sendPrivateMessageEmail(privateMessage);
			}
		}
	}
	
	/**
	 * Sends private message
	 * 
	 * @param privateMessage	Private message
	 * 
	 * @param toUser	To user
	 */
	protected void sendPrivateMessageEmail(PrivateMessage privateMessage)
	{
		if ((privateMessage == null) || (privateMessage.getToUser() == null))
		{
			return;
		}
		
		// send email if to users opted to receive private message email's or high priority private messages
		if (!privateMessage.getToUser().isNotifyPrivateMessagesEnabled())
		{
			if ((privateMessage.getPriority() != PrivateMessage.PRIORITY_HIGH))
			{
				return;
			}
		}
		
		if ((privateMessage.getToUser().getEmail() == null) || (privateMessage.getToUser().getEmail().trim().length() == 0))
		{
			return;
		}
		
		InternetAddress from = null;
		InternetAddress[] to = null;
		
		try
		{
			InternetAddress toUserEmail = new InternetAddress(privateMessage.getToUser().getEmail());
			
			to = new InternetAddress[1];
			to[0] = toUserEmail;
			
			String fromUserEmail = "\"" + ServerConfigurationService.getString("ui.service", "Sakai") + "\"<no-reply@" + ServerConfigurationService.getServerName() + ">";
			from = new InternetAddress(fromUserEmail);
		} 
		catch (AddressException e)
		{
			// if (logger.isWarnEnabled()) logger.warn("sendPrivateMessageEmail(): Private message email notification error : "+ e);
			return;
		}
		String subject = privateMessage.getPost().getSubject();
		String postText = privateMessage.getPost().getText();
		
		// full URL's for smilies etc
		if (postText != null && postText.trim().length() > 0)
		{
			postText = XrefHelper.fullUrls(postText);
		}
		
		// format message text
		Map<String, String> params = new HashMap<String, String>();
		
		StringBuilder toUserName = new StringBuilder();
		toUserName.append(privateMessage.getToUser().getFirstName() != null ? privateMessage.getToUser().getFirstName() : "");
		toUserName.append(" ");
		toUserName.append(privateMessage.getToUser().getLastName() != null ? privateMessage.getToUser().getLastName() : "");
		params.put("pm.to", toUserName.toString());
		
		StringBuilder fromUserName = new StringBuilder();
		fromUserName.append(privateMessage.getFromUser().getFirstName() != null ? privateMessage.getFromUser().getFirstName() : "");
		fromUserName.append(" ");
		fromUserName.append(privateMessage.getFromUser().getLastName() != null ? privateMessage.getFromUser().getLastName() : "");
		params.put("pm.from", fromUserName.toString());
		
		params.put("pm.subject", subject);
		params.put("pm.text", postText);
		
		StringBuilder emailSubject = new StringBuilder();
		try
		{
			Site site = siteService.getSite(privateMessage.getContext());
			
			if (!site.isPublished())
			{
				return;
			}
			
			params.put("site.title", site.getTitle());
			
			emailSubject.append(subject);
			emailSubject.append(" - "+ PrivateMessage.MAIL_NEW_PM_SUBJECT);
			emailSubject.append("(");
			emailSubject.append(site.getTitle());
			emailSubject.append(") ");			
			
			String portalUrl = ServerConfigurationService.getPortalUrl();
			params.put("portal.url", portalUrl);
			
			//String currToolId = ToolManager.getCurrentPlacement().getId();
			//ToolConfiguration toolConfiguration = site.getTool(currToolId);
			
			ToolConfiguration toolConfiguration = site.getToolForCommonId("sakai.jforum.tool");
			
			String siteNavUrl = portalUrl + "/"+ "site" + "/"+ Web.escapeUrl(site.getId());
			
			if (toolConfiguration != null)
				siteNavUrl = siteNavUrl + "/" + "page" + "/"+ toolConfiguration.getPageId();
			
			params.put("site.url", siteNavUrl);
		}
		catch (IdUnusedException e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn(e.toString(), e);
			}
		}
		
		String messageText = EmailUtil.getPrivateMessageText(params);
		
		if (messageText != null)
		{
			Post post = privateMessage.getPost();
			if ((post !=null) && (post.hasAttachments()))
			{
				//email with attachments
				jforumEmailExecutorService.notifyUsers(from, to, emailSubject.toString(), messageText, post.getAttachments());
			}
			else
			{
				jforumEmailExecutorService.notifyUsers(from, to, emailSubject.toString(), messageText);
			}
		}
		else
		{
			Post post = privateMessage.getPost();
			if ((post !=null) && (post.hasAttachments()))
			{
				//email with attachments
				jforumEmailExecutorService.notifyUsers(from, to, emailSubject.toString(), postText, post.getAttachments());
			}
			else
			{
				jforumEmailExecutorService.notifyUsers(from, to, emailSubject.toString(), postText);
			}
		}
	}
	
	/**
	 * sets the bcc users and returns bcc users
	 * 
	 * @param privateMessage	Private message
	 * 
	 * @param bccSakaiUserIds	Bcc users sakai id's
	 * 
	 * @return list of bcc user
	 */
	protected void setBccUsers(PrivateMessage privateMessage, List<String> bccSakaiUserIds)
	{
		boolean facilitator;
		boolean participant;
		
		if (bccSakaiUserIds != null && !bccSakaiUserIds.isEmpty())
		{
			List<User> bccUsers = new ArrayList<User>();
			List<Integer>  bccUserIds = new ArrayList<Integer>();
			
			User bccJforumUser = null;
			
			facilitator = false;
			participant = false;
			for (String bccSakaiUserId : bccSakaiUserIds)
			{
				if (bccSakaiUserId == null || bccSakaiUserId.trim().length() == 0)
				{
					continue;
				}
				bccJforumUser = jforumUserService.getBySakaiUserId(bccSakaiUserId);
				if (bccJforumUser != null)
				{
					//access check To user
					facilitator = false;
					participant = false;
					
					facilitator = jforumSecurityService.isJForumFacilitator(privateMessage.getContext(), bccSakaiUserId);
					if (!facilitator)
					{
						participant = jforumSecurityService.isJForumParticipant(privateMessage.getContext(), bccSakaiUserId);
					}
					
					if (!(facilitator || participant))
					{
						continue;
					}
					
					bccUsers.add(bccJforumUser);
					bccUserIds.add(new Integer(bccJforumUser.getId()));
				}
			}
			
			if (!bccUserIds.isEmpty())
			{
				((PrivateMessageImpl)privateMessage).setBccUserIds(bccUserIds);
				((PrivateMessageImpl)privateMessage).setBccUsers(bccUsers);
			}
		}
		else
		{
			((PrivateMessageImpl)privateMessage).setBccUserIds(null);
			((PrivateMessageImpl)privateMessage).setBccUsers(null);
		}
	}
}


