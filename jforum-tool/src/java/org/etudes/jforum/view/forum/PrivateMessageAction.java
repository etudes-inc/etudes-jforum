/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/view/forum/PrivateMessageAction.java $ 
 * $Id: PrivateMessageAction.java 12349 2015-12-23 21:10:53Z ggolden $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 Etudes, Inc. 
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
 * 
 * Portions completed before July 1, 2004 Copyright (c) 2003, 2004 Rafael Steil, All rights reserved, licensed under the BSD license. 
 * http://www.opensource.org/licenses/bsd-license.php 
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met: 
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following disclaimer. 
 * 2) Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution. 
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE 
 ***********************************************************************************/
package org.etudes.jforum.view.forum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.JForumAccessException;
import org.etudes.api.app.jforum.JForumAttachmentBadExtensionException;
import org.etudes.api.app.jforum.JForumAttachmentOverQuotaException;
import org.etudes.api.app.jforum.JForumPrivateMessageService;
import org.etudes.api.app.jforum.JForumSecurityService;
import org.etudes.api.app.jforum.JForumUserService;
import org.etudes.api.app.jforum.PrivateMessageFolder;
import org.etudes.jforum.Command;
import org.etudes.jforum.JForum;
import org.etudes.jforum.SessionFacade;
import org.etudes.jforum.dao.DataAccessDriver;
import org.etudes.jforum.dao.PrivateMessageDAO.PrivateMessageSort;
import org.etudes.jforum.entities.PrivateMessage;
import org.etudes.jforum.entities.PrivateMessageType;
import org.etudes.jforum.entities.User;
import org.etudes.jforum.entities.UserSession;
import org.etudes.jforum.repository.SmiliesRepository;
import org.etudes.jforum.util.I18n;
import org.etudes.jforum.util.concurrent.executor.QueuedExecutor;
import org.etudes.jforum.util.mail.EmailSenderTask;
import org.etudes.jforum.util.mail.PrivateMessageSpammer;
import org.etudes.jforum.util.preferences.ConfigKeys;
import org.etudes.jforum.util.preferences.SakaiSystemGlobals;
import org.etudes.jforum.util.preferences.SystemGlobals;
import org.etudes.jforum.util.preferences.TemplateKeys;
import org.etudes.jforum.util.user.JForumUserUtil;
import org.etudes.jforum.view.forum.common.AttachmentCommon;
import org.etudes.jforum.view.forum.common.PostCommon;
import org.etudes.jforum.view.forum.common.ViewCommon;
import org.etudes.util.HtmlHelper;
import org.etudes.util.api.RosterAdvisor;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.cover.UserDirectoryService;

/**
 * @author Rafael Steil
 */
public class PrivateMessageAction extends Command
{
	private static Log logger = LogFactory.getLog(PrivateMessageAction.class);

	private String templateName;

	/**
	 * add the flag of the PM to follow up
	 * @throws Exception
	 */
	public void addFlag() throws Exception
	{
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}

		String ids[] = this.request.getParameterValues("id");
		UserSession us = SessionFacade.getUserSession();
		int userId = us.getUserId();
		
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		if (ids != null && ids.length > 0)
		{
			for (int i = 0; i < ids.length; i++)
			{

				org.etudes.api.app.jforum.PrivateMessage pm = jforumPrivateMessageService.getPrivateMessage(Integer.parseInt(ids[i]));

				if (pm == null)
					continue;

				if (pm.getToUser().getId() == userId || pm.getFromUser().getId() == userId)
				{

					// Update the flag
					pm.setFlagToFollowup(true);

					jforumPrivateMessageService.flagToFollowup(pm);
				}
			}
		}
		
		// redirect to inbox or folder where the messages are moved from
		String folderId = this.request.getParameter("folder_id");
		if (folderId != null)
		{
			this.context.put("folderId", folderId);
			this.messages();
		}
		else
		{
			this.inbox();
		}

		/*this.setTemplateName(TemplateKeys.PM_FLAGGED);
		this.context.put("message", I18n.getMessage("PrivateMessage.flaggingDone",
						new String[] { this.request.getContextPath() + "/pm/inbox/date/d"
										+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));*/
	}
	
	/**
	 * Private message folder form
	 * 
	 * @throws Exception
	 */
	public void addFolder() throws Exception
	{
		if (isUserObserver())
		{
			return;
		}
		
		getFolders();
		
		this.context.put("moduleName", "pm");
		this.setTemplateName(TemplateKeys.PM_FOLDER_INSERT);
	}
	
	/**
	 * Sends the private message from activity meter.
	 * 
	 * @throws Exception
	 */
	public void amSend() throws Exception
	{
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		String returnTo = this.request.getParameter("return_to");
		String returnPath[] = returnTo.split("~");
		
		if (returnPath.length > 1)
		{
			returnTo = returnTo.replace("~", "/");
		}
				
		User user = DataAccessDriver.getInstance().newUserDAO().selectById(SessionFacade.getUserSession().getUserId());
		user.setSignature(PostCommon.processText(user.getSignature()));
		user.setSignature(PostCommon.processSmilies(user.getSignature(), SmiliesRepository.getSmilies()));

		this.sendFormCommon(user);
		
		List users = null;
		try
		{
			users = JForumUserUtil.updateMembersInfo();
			this.context.put("users", users);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled()) logger.error(e.toString());
		}
		
		// overwrite action
		this.context.put("action", "amSendSave");
		this.context.put("returnTo", returnTo);
		this.setTemplateName(TemplateKeys.PM_AM_SENDFORM);
		
		Site currentSite = null;
		try
		{
		  currentSite = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
				  
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled()) logger.error(this.getClass().getName() + ".amSendSave() : " + e.getMessage(), e);
		}
		
		if (currentSite != null)
		{
			StringBuilder siteNavUrl = new StringBuilder();
			
			String portalUrl = ServerConfigurationService.getPortalUrl();
			siteNavUrl.append(portalUrl);
			siteNavUrl.append("/directtool/");
			
			ToolConfiguration jforumToolConfiguration = currentSite.getToolForCommonId("sakai.activitymeter");
			
			if (jforumToolConfiguration != null)
			{
				siteNavUrl.append(jforumToolConfiguration.getId());	
				if (returnTo != null)
				{
					siteNavUrl.append("/");
					siteNavUrl.append(returnTo);
				}
			}
			
			this.context.put("returnUrl", siteNavUrl);
		}
		
		//this.context.put("maxPMToUsers", new Integer(SakaiSystemGlobals.getIntValue(ConfigKeys.MAX_PM_TOUSERS)));
	}

	/** 
	 * Saves the private message that was invoked from activity meter and displays message with link
	 * back to activity meter
	 * 
	 * @throws Exception
	 */
	public void amSendSave() throws Exception
	{
		if (logger.isDebugEnabled()) logger.debug("PrivateMessageAction - amsendSave");
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}

		String sId = this.request.getParameter("toUserId");
		String toUsername = this.request.getParameter("toUsername");
		String userEmail = this.request.getParameter("toUserEmail");		
		String returnTo = this.request.getParameter("return_to");
		
		String returnPath[] = returnTo.split("~");
		
		if (returnPath.length > 1)
		{
			returnTo = returnTo.replace("~", "/");
		}

		boolean pmEmailEnabled = false;

		PrivateMessage pm = new PrivateMessage();
		pm.setPost(PostCommon.fillPostFromRequest());

		if ((this.request.getParameter("high_priority_pm") != null) && (this.request.getIntParameter("high_priority_pm") == 1))
		{
			pm.setPriority(PrivateMessage.PRIORITY_HIGH);
		}
		else
		{
			pm.setPriority(PrivateMessage.PRIORITY_GENERAL);
		}

		User fromUser = DataAccessDriver.getInstance().newUserDAO().selectById(SessionFacade.getUserSession().getUserId());
		pm.setFromUser(fromUser);

		int attachmentIds[] = null;
		try
		{
			attachmentIds = addPMAttachments();
		}
		catch (Exception e)
		{
			this.context.put("post", pm.getPost());
			this.context.put("pm", pm);
			this.request.addParameter("return_to", returnTo);
			this.amSend();
			return;
		}

		int toUserId = -1;
		if (sId == null || sId.trim().equals(""))
		{

			String toUserIds[] = (String[]) this.request.getObjectParameter("toUsername" + "ParamValues");
			if (toUserIds != null)
			{

				for (int i = 0; i < toUserIds.length; i++)
				{
					if (toUserIds[i] != null && toUserIds[i].trim().length() > 0)
					{
						toUserId = Integer.parseInt(toUserIds[i]);
						User usr = DataAccessDriver.getInstance().newUserDAO().selectById(toUserId);
						toUserId = usr.getId();
						userEmail = usr.getEmail();
						toUsername = usr.getFirstName() + " " + usr.getLastName();
						pmEmailEnabled = usr.isNotifyPrivateMessagesEnabled();
						sendPrivateMessage(pm, toUsername, userEmail, pmEmailEnabled, toUserId, attachmentIds);
					}
				}
			}
			else
			{
				toUserId = Integer.parseInt(toUsername);
				User usr = DataAccessDriver.getInstance().newUserDAO().selectById(toUserId);
				toUserId = usr.getId();
				userEmail = usr.getEmail();
				toUsername = usr.getFirstName() + " " + usr.getLastName();
				pmEmailEnabled = usr.isNotifyPrivateMessagesEnabled();
				sendPrivateMessage(pm, toUsername, userEmail, pmEmailEnabled, toUserId, attachmentIds);
			}
		}
		else
		{
			toUserId = Integer.parseInt(sId);
			User usr = DataAccessDriver.getInstance().newUserDAO().selectById(toUserId);
			toUsername = usr.getFirstName() + " " + usr.getLastName();
			pmEmailEnabled = usr.isNotifyPrivateMessagesEnabled();
			sendPrivateMessage(pm, toUsername, userEmail, pmEmailEnabled, toUserId, attachmentIds);
		}

		this.setTemplateName(TemplateKeys.PM_AM_SENDSAVE);

		Site currentSite = null;
		try
		{
		  currentSite = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
				  
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled()) logger.error(this.getClass().getName() + ".amSendSave() : " + e.getMessage(), e);
		}
		
		if (currentSite != null)
		{
			StringBuilder siteNavUrl = new StringBuilder();
			
			String portalUrl = ServerConfigurationService.getPortalUrl();
			siteNavUrl.append(portalUrl);
			siteNavUrl.append("/directtool/");
			
			ToolConfiguration jforumToolConfiguration = currentSite.getToolForCommonId("sakai.activitymeter");
			
			if (jforumToolConfiguration != null)
			{
				siteNavUrl.append(jforumToolConfiguration.getId());
				if (returnTo != null)
				{
					siteNavUrl.append("/");
					siteNavUrl.append(returnTo);
				}
			}
			
			this.context.put("returnUrl", siteNavUrl.toString());
			
			this.context.put("message", I18n.getMessage("PrivateMessage.messageSent.returnToActivityMeter",	new String[] { siteNavUrl.toString()}));
		}
	}
	
	/**
	 * Sends the private message from activity meter.
	 * 
	 * @throws Exception
	 */
	public void amSendTo() throws Exception
	{
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		String sakUserIdStr = this.request.getParameter("sakai_user_id");
		//String sakUserIdsArr[] = sakUserIdStr.split("~");
		
		//RosterAdvisor rosterAdvisor = (RosterAdvisor)ComponentManager.get("org.etudes.util.api.RosterAdvisor");
		RosterAdvisor rosterAdvisor = (RosterAdvisor)ComponentManager.get(RosterAdvisor.class);
		JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		
		if (rosterAdvisor != null)
		{
			List<String> sakUserIds = rosterAdvisor.getUsers(sakUserIdStr);
			
			Set<String> sakaiUserIds = new HashSet<String>();
			
			String highPriority = this.request.getParameter("high_priority");
			this.context.put("highPriority", highPriority);
			
			if (sakUserIds.isEmpty())
			{
				String sakai_user_id_save_exp = this.request.getParameter("sakai_user_id_save_exp");
				
				if (sakai_user_id_save_exp != null)
				{
					String[] sakUserIdsSaveExp = sakai_user_id_save_exp.split(",");
					
					for (String sakUserId : sakUserIdsSaveExp)
					{
						sakaiUserIds.add(sakUserId);
					}
				}
			}
			else
			{
				for (String sakUserId : sakUserIds)
				{
					sakaiUserIds.add(sakUserId);
				}
			}
			
			try
			{
				List<org.etudes.api.app.jforum.User> users = getUsers();
				
				Iterator<org.etudes.api.app.jforum.User> userIter = users.iterator();
			    while (userIter.hasNext())
			    {
			    	org.etudes.api.app.jforum.User siteUser = userIter.next();
			    	if (!sakaiUserIds.contains(siteUser.getSakaiUserId()))
			    	{
			    		userIter.remove();
			    	}
			    }
				this.context.put("users", users);
			}
			catch (Exception e)
			{
				if (logger.isErrorEnabled())
				{
					logger.error(e.toString(), e);
				}
				this.context.put("users", new ArrayList());
			}
		}
		else
		{
			this.context.put("users", new ArrayList());
		}
		
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
		user.setSignature(PostCommon.processText(user.getSignature()));
		user.setSignature(PostCommon.processSmilies(user.getSignature(), SmiliesRepository.getSmilies()));
		
		this.sendFormCommon(user);
			
		// overwrite action
		this.context.put("action", "sendAMPMSave");
		this.context.put("sakaiToUserIds", sakUserIdStr);
		this.setTemplateName(TemplateKeys.PM_AM_SENDFORM);
		this.context.put("pageTitle", I18n.getMessage("PrivateMessage.pageTitle"));				
		//this.context.put("maxPMToUsers", new Integer(SakaiSystemGlobals.getIntValue(ConfigKeys.MAX_PM_TOUSERS)));
	}
	
	/**
	 * clear the flag of the PM to follow up
	 * @throws Exception
	 */
	public void clearFlag() throws Exception
	{
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}

		String ids[] = this.request.getParameterValues("id");
		UserSession us = SessionFacade.getUserSession();
		int userId = us.getUserId();

		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService) ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");

		if (ids != null && ids.length > 0)
		{
			for (int i = 0; i < ids.length; i++)
			{

				org.etudes.api.app.jforum.PrivateMessage pm = jforumPrivateMessageService.getPrivateMessage(Integer.parseInt(ids[i]));

				if (pm == null)
					continue;

				if (pm.getToUser().getId() == userId || pm.getFromUser().getId() == userId)
				{

					// Update the flag
					pm.setFlagToFollowup(false);
					jforumPrivateMessageService.flagToFollowup(pm);
				}
			}
		}

		// redirect to inbox or folder where the messages are moved from
		String folderId = this.request.getParameter("folder_id");
		if (folderId != null)
		{
			this.context.put("folderId", folderId);
			this.messages();
		}
		else
		{
			this.inbox();
		}
		
		/*this.setTemplateName(TemplateKeys.PM_FLAGGED);
		this.context.put( "message", I18n.getMessage("PrivateMessage.flaggingDone",
						new String[] { this.request.getContextPath() + "/pm/inbox/date/d" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));*/
	}
	
	public void delete() throws Exception
	{
		if (!SessionFacade.isLogged()) 
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}

		String ids[] = this.request.getParameterValues("id");

		if (ids != null && ids.length > 0)
		{
			String draftsList = this.request.getParameter("drafts_list");
			
			JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
			List<Integer> privateMessageIds = new ArrayList<Integer>();
			
			for (int i = 0; i < ids.length; i++)
			{
				privateMessageIds.add(Integer.parseInt(ids[i]));
			}
			
			try
			{
				if (draftsList != null && draftsList.equalsIgnoreCase("true"))
				{
					jforumPrivateMessageService.deleteDraft(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), privateMessageIds);
					
					// redirect to drafts
					this.drafts();
				}
				else
				{
					jforumPrivateMessageService.deleteMessage(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), privateMessageIds);
					
					// redirect to inbox or folder where the messages are moved from
					String folderId = this.request.getParameter("folder_id");
					if (folderId != null)
					{
						this.context.put("folderId", folderId);
						this.messages();
					}
					else
					{
						this.inbox();
					}
				}
			}
			catch (JForumAccessException e)
			{
				
			}
			
			/*PrivateMessage[] pm = new PrivateMessage[ids.length];
			User u = new User();
			u.setId(SessionFacade.getUserSession().getUserId());

			for (int i = 0; i < ids.length; i++)
			{
				pm[i] = new PrivateMessage();
				pm[i].setFromUser(u);
				pm[i].setId(Integer.parseInt(ids[i]));
			}

			// delete attachments if any
			if (pm != null)
			{
				for (int i = 0; i < pm.length; i++)
				{
					new AttachmentCommon(this.request).deletePMAttachments(pm[i].getId());
				}
			}

			DataAccessDriver.getInstance().newPrivateMessageDAO().delete(pm);*/
		}

		/*this.setTemplateName(TemplateKeys.PM_DELETE);
		this.context.put("message", I18n.getMessage("PrivateMessage.deleteDone",
						new String[] { this.request.getContextPath() + "/pm/inbox/date/d"
										+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));*/
	}
	
	/**
	 * Deletes the folder
	 * 
	 * @throws Exception
	 */
	public void deleteFolders() throws Exception
	{
		if (!SessionFacade.isLogged()) 
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}

		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		String ids[] = this.request.getParameterValues("id");

		StringBuilder strBul = new StringBuilder();
				
		if (ids != null && ids.length > 0)
		{
			for (int i = 0; i < ids.length; i++)
			{
				// if folder has messages the folder cannot be deleted
				int count = jforumPrivateMessageService.getFolderMessagesCount(Integer.parseInt(ids[i]));
				if (count > 0)
				{
					PrivateMessageFolder privateMessageFolder = jforumPrivateMessageService.getFolder(Integer.parseInt(ids[i]));
					if (privateMessageFolder != null)
					{
						if (strBul.length() > 0)
						{
							strBul.append(", ");
						}
						strBul.append(privateMessageFolder.getName());						
					}
					continue;
				}
				
				jforumPrivateMessageService.deletePrivateMessageFolder(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), Integer.parseInt(ids[i]));
			}
			
			if (strBul.length() > 0)
			{
				JForum.getContext().put("errorMessage", I18n.getMessage("PrivateMessage.CannotDeleteFolders", new Object[] { strBul.toString() }));
			}
		}
		
		this.folders();
	}
	
	public void drafts() throws Exception
	{
		if (!SessionFacade.isLogged()) {
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}

		User user = new User();
		user.setId(SessionFacade.getUserSession().getUserId());

		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		List<org.etudes.api.app.jforum.PrivateMessage> pmList = jforumPrivateMessageService.drafts(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		
		sortPrivateMessageList(pmList, false);

		this.context.put("draftsList", true);
		this.context.put("pmList", pmList);
		
		// user private message folders
		List<org.etudes.api.app.jforum.PrivateMessageFolder> pmFoldersList = jforumPrivateMessageService.getUserFolders(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		this.context.put("pmFoldersList", pmFoldersList);
		
		// inbox unread count
		int inboxUnreadCount = jforumPrivateMessageService.getUserFolderUnreadCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), 0);
		this.context.put("inboxUnreadCount", inboxUnreadCount);
				
		this.setTemplateName(TemplateKeys.PM_DRAFTS);
		this.putTypes();
	}
	
	public void draftShow() throws Exception
	{

		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}
		
		int id = this.request.getIntParameter("id");
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		org.etudes.api.app.jforum.PrivateMessage pmDraft = jforumPrivateMessageService.getPrivateMessageDraft(id);
		
		if (pmDraft == null)
		{
			this.setTemplateName(TemplateKeys.PM_EDIT_DRAFT_DENIED);
			this.context.put("message", I18n.getMessage("PrivateMessage.draftNotExisting"));
			return;
		}
		
		// Don't allow the edit draft that don't belongs to the current user
		UserSession us = SessionFacade.getUserSession();
		int userId = us.getUserId();
		
		if (pmDraft.getFromUser().getId() == userId) 
		{
			pmDraft.getPost().setText(pmDraft.getPost().getRawText());
			this.context.put("pmDraft", pmDraft);
		}
		else
		{
			this.setTemplateName(TemplateKeys.PM_EDIT_DRAFT_DENIED);
			this.context.put("message", I18n.getMessage("PrivateMessage.editDraftDenied"));
			return;
		}
		
		JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
		this.sendFormCommon(user);
		
		try
		{
			List<org.etudes.api.app.jforum.User> users = getUsers();
			this.context.put("users", users);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e.toString(), e);
			}
			this.context.put("users", new ArrayList());
		}
		
		if (pmDraft.getPost().hasAttachments())
		{
			this.context.put("attachments", pmDraft.getPost().getAttachments());
		}
		this.context.put("attachmentsEnabled", true);

		boolean facilitator = JForumUserUtil.isJForumFacilitator(user.getSakaiUserId());
		this.context.put("facilitator", facilitator);
		this.context.put("pmDraftEdit", true);
		this.context.put("privateMessageSend", true);
	}
	
	/**
	 * Modify the folder name
	 * 
	 * @throws Exception
	 */
	public void editFolder() throws Exception
	{
		if (isUserObserver())
		{
			return;
		}
		
		String folderIdparam = this.request.getParameter("folder_id");
		
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		if ((folderIdparam != null) && (folderIdparam.trim().length() > 0))
		{
			int folderId;
			try
			{
				folderId = Integer.parseInt(folderIdparam);
				
				PrivateMessageFolder privateMessageFolder = jforumPrivateMessageService.getFolder(folderId);
				this.context.put("privateMessageFolder", privateMessageFolder);
			}
			catch (NumberFormatException e)
			{
				
			}			
		}
		
		getFolders();
		
		this.context.put("moduleName", "pm");
		this.setTemplateName(TemplateKeys.PM_FOLDER_EDIT);
	}

	//Mallika's new code beg
	public void findFluser() throws Exception
	{
		boolean showResult = false;
		String username = this.request.getParameter("username");

		if (username != null && !username.equals("")) {
			List namesList = DataAccessDriver.getInstance().newUserDAO().findByFlName(username, false);
			this.context.put("namesList", namesList);
			showResult = true;
		}

		this.setTemplateName(TemplateKeys.PM_FIND_USER);

		this.context.put("username", username);
		this.context.put("showResult", showResult);
	}
	//Mallika's new code end

	public void findUser() throws Exception
	{
		boolean showResult = false;
		String username = this.request.getParameter("username");

		if (username != null && !username.equals("")) {
			List namesList = DataAccessDriver.getInstance().newUserDAO().findByName(username, false);
			this.context.put("namesList", namesList);
			showResult = true;
		}

		this.setTemplateName(TemplateKeys.PM_FIND_USER);

		this.context.put("username", username);
		this.context.put("showResult", showResult);
	}

	/**
	 * flag or clear the flag of the PM to follow up
	 * @throws Exception
	 */
	public void flag() throws Exception
	{
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}

		int id = this.request.getIntParameter("id");

		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService) ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		org.etudes.api.app.jforum.PrivateMessage pm = jforumPrivateMessageService.getPrivateMessage(id);

		// Don't allow the flag the message that don't belongs to the current user
		UserSession us = SessionFacade.getUserSession();
		int userId = us.getUserId();
		if (pm.getToUser().getId() == userId || pm.getFromUser().getId() == userId)
		{

			// Update the flag
			pm.setFlagToFollowup(!pm.isFlagToFollowup());
			jforumPrivateMessageService.flagToFollowup(pm);
		}
		else
		{
			this.setTemplateName(TemplateKeys.PM_FLAG_FOLLOWUP_DENIED);
			this.context.put("message", I18n.getMessage("PrivateMessage.flagToFollowUpDenied"));
		}
		this.read();
	}
	
	
	public void folders() throws Exception
	{
		if (isUserObserver())
		{
			return;
		}
		
		getFolders();
		
		this.context.put("moduleName", "pm");
		this.setTemplateName(TemplateKeys.PM_FOLDERS);
	}

	public void inbox() throws Exception
	{
		if (!SessionFacade.isLogged()) 
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}

		User user = new User();
		user.setId(SessionFacade.getUserSession().getUserId());

		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		List<org.etudes.api.app.jforum.PrivateMessage> pmList = jforumPrivateMessageService.inbox(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), false);
		
		sortPrivateMessageList(pmList, true);
		
		this.context.put("inbox", true);
		this.context.put("pmList", pmList);
		this.context.put("userPMDraftsCount", jforumPrivateMessageService.getUserDraftsCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId()));
		
		// user private message folders
		List<org.etudes.api.app.jforum.PrivateMessageFolder> pmFoldersList = jforumPrivateMessageService.getUserFolders(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		this.context.put("pmFoldersList", pmFoldersList);
		
		// inbox unread count
		int inboxUnreadCount = jforumPrivateMessageService.getUserFolderUnreadCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), 0);
		this.context.put("inboxUnreadCount", inboxUnreadCount);
		
		this.setTemplateName(TemplateKeys.PM_INBOX);
		this.putTypes();
	}
	
	/**
	 * @see org.etudes.jforum.Command#list()
	 */
	public void list() throws Exception
	{
		this.inbox();
	}

	/**
	 * Mark selected private messages as read
	 * 
	 * @throws Exception
	 */
	public void markRead() throws Exception
	{
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}

		String ids[] = this.request.getParameterValues("id");
		UserSession us = SessionFacade.getUserSession();
		int userId = us.getUserId();
		
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		if (ids != null && ids.length > 0)
		{
			for (int i = 0; i < ids.length; i++)
			{

				org.etudes.api.app.jforum.PrivateMessage pm = jforumPrivateMessageService.getPrivateMessage(Integer.parseInt(ids[i]));

				if (pm == null)
					continue;

				if (pm.getToUser().getId() == userId || pm.getFromUser().getId() == userId)
				{

					// mark as read
					pm.setType(org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType.READ.getType());
					jforumPrivateMessageService.markMessageRead(pm.getId(), us.getSakaiUserId(), null, true);
					us.setPrivateMessages(us.getPrivateMessages() - 1);
				}
			}
		}
		
		// redirect to inbox or folder where the messages are moved from
		String folderId = this.request.getParameter("folder_id");
		if (folderId != null)
		{
			this.context.put("folderId", folderId);
			this.messages();
		}
		else
		{
			this.inbox();
		}
	}
	
	/**
	 * User folder messages
	 * 
	 * @throws Exception
	 */
	public void messages() throws Exception
	{
		if (!SessionFacade.isLogged()) 
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		User user = new User();
		user.setId(SessionFacade.getUserSession().getUserId());

		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		List<org.etudes.api.app.jforum.PrivateMessage> pmList = null;
		
		String folderId = this.request.getParameter("folder_id");
		
		if (folderId != null)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("FolderId : "+ folderId);
			}
			
			
			try
			{
				int fId = Integer.parseInt(folderId);
				
				pmList = jforumPrivateMessageService.folderMessages(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), fId);
				this.context.put("folderId", fId);
			}
			catch (NumberFormatException e)
			{
				pmList = jforumPrivateMessageService.inbox(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), false);
				this.context.put("inbox", true);
			}
		}
		else
		{
			pmList = jforumPrivateMessageService.inbox(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), false);
			this.context.put("inbox", true);
		}
		
		sortPrivateMessageList(pmList, true);		
		
		this.context.put("pmList", pmList);
		this.context.put("userPMDraftsCount", jforumPrivateMessageService.getUserDraftsCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId()));
		
		// user private message folders
		List<org.etudes.api.app.jforum.PrivateMessageFolder> pmFoldersList = jforumPrivateMessageService.getUserFolders(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		this.context.put("pmFoldersList", pmFoldersList);
		
		this.setTemplateName(TemplateKeys.PM_INBOX);
		this.putTypes();
	}
	
	public void moveMessages() throws Exception
	{
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		String folderId = this.request.getParameter("folder_id");
		if (folderId != null)
		{
			try
			{
				int fId = Integer.parseInt(folderId);
				
				this.context.put("folderId", fId);
			}
			catch (NumberFormatException e)
			{				
			}
		}
		
		String ids[] = this.request.getParameterValues("id");
		
		if (ids != null && ids.length > 0)
		{
			StringBuffer sb = new StringBuffer();
			
			for (int i = 0; i < ids.length; i++)
			{
				sb.append(ids[i]).append(",");
			}
			
			String sbStr = sb.toString().trim();
			if (sbStr.endsWith(","))
			{
				sbStr = sbStr.substring(0, sbStr.length() - 1);
			}
			
			JForum.getContext().put("messages", sbStr);
		}
		
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		List<org.etudes.api.app.jforum.PrivateMessageFolder> pmFoldersList = jforumPrivateMessageService.getUserFolders(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		
		this.context.put("pmFoldersList", pmFoldersList);
		this.context.put("userPMDraftsCount", jforumPrivateMessageService.getUserDraftsCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId()));
		
		this.context.put("moduleName", "pm");
		this.setTemplateName(TemplateKeys.PM_MOVE);
	}
	
	public void moveToFolder() throws Exception
	{
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		String folderId = this.request.getParameter("folder_id");
		if (folderId != null)
		{
			try
			{
				int fId = Integer.parseInt(folderId);
				
				this.context.put("folderId", fId);
			}
			catch (NumberFormatException e)
			{				
			}
		}
		
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		// save move messages
		int toFolderId = Integer.parseInt(JForum.getRequest().getParameter("to_folder"));
		
		String ids[] = this.request.getParameterValues("id");
		
		if (ids != null && ids.length > 0)
		{
			for (int i = 0; i < ids.length; i++)
			{
				int movedPrivateMessageId = Integer.parseInt(ids[i]);
				
				try
				{
					jforumPrivateMessageService.moveTofolder(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), movedPrivateMessageId, toFolderId);
				}
				catch (JForumAccessException e)
				{
					this.context.put("errorMessage", e.toString());
				}
			}
		}		
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting saveMoveMessages");
		}
		
		// redirect to inbox or folder where the messages are moved from
		if (folderId != null)
		{
			this.context.put("folderId", folderId);
			this.messages();
		}
		else
		{
			this.inbox();
		}
	}
	
	public void quote() throws Exception
	{
		if (!SessionFacade.isLogged()) 
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}

		int id = this.request.getIntParameter("id");

		//PrivateMessage pm = new PrivateMessage();
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService) ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		org.etudes.api.app.jforum.PrivateMessage pm = jforumPrivateMessageService.getPrivateMessage(id);
		pm.getPost().setText(pm.getPost().getRawText());
		//pm.setId(id);
		//pm = DataAccessDriver.getInstance().newPrivateMessageDAO().selectById(pm);

		int userId = SessionFacade.getUserSession().getUserId();

		//if (pm.getToUser().getId() != userId && pm.getFromUser().getId() != userId)
		if (pm.getToUser().getId() != userId)
		{
			this.setTemplateName(TemplateKeys.PM_READ_DENIED);
			this.context.put("message", I18n.getMessage("PrivateMessage.readDenied"));
			return;
		}
		
		JForumSecurityService jforumSecurityService = (JForumSecurityService)ComponentManager.get("org.etudes.api.app.jforum.JForumSecurityService");
		
		if (!jforumSecurityService.isUserActive(ToolManager.getCurrentPlacement().getContext(), pm.getToUser().getSakaiUserId()) || jforumSecurityService.isUserBlocked(ToolManager.getCurrentPlacement().getContext(), pm.getToUser().getSakaiUserId()))
		{
			this.setTemplateName(TemplateKeys.PM_READ_DENIED);
			this.context.put("message", I18n.getMessage("PrivateMessage.userInactive1"));
			return;
		}
		
		if (!jforumSecurityService.isUserActive(ToolManager.getCurrentPlacement().getContext(), pm.getFromUser().getSakaiUserId()) || jforumSecurityService.isUserBlocked(ToolManager.getCurrentPlacement().getContext(), pm.getFromUser().getSakaiUserId()))
		{
			this.setTemplateName(TemplateKeys.PM_READ_DENIED);
			this.context.put("message", I18n.getMessage("PrivateMessage.userInactive1"));
			return;
		}
       
		//Mallika - adding this if to strip out additional Res
		//Only add Re: at beginning if this is the first reply
		if (pm.getPost().getSubject().length() >= 3)
		{
		  if (!(pm.getPost().getSubject().substring(0,3).equals("Re:")))
		  {
		    pm.getPost().setSubject(I18n.getMessage("PrivateMessage.replyPrefix") + pm.getPost().getSubject());
		  }
	    }
		else
		{
			 pm.getPost().setSubject(I18n.getMessage("PrivateMessage.replyPrefix") + pm.getPost().getSubject());
			 
		}
		
		//this.sendFormCommon(DataAccessDriver.getInstance().newUserDAO().selectById(userId));
		JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);
		this.sendFormCommon(jforumUserService.getByUserId(userId));

		this.context.put("quote", "true");
		this.context.put("quoteUser", pm.getFromUser());
		this.context.put("pmQuote", true);
		
		if (this.context.get("replyError") == null)
		{
			this.context.put("post", pm.getPost());
		}
		this.context.put("pm", pm);
		this.context.put("isNewPost", true);
		this.context.put("privateMessageSend", true);
		
		try
		{
			// List<org.etudes.api.app.jforum.User> users = getUsers();
			// this.context.put("users", users);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e.toString(), e);
			}
			this.context.put("users", new ArrayList());
		}
		
		boolean facilitator = JForumUserUtil.isJForumFacilitator(user.getSakaiUserId());
		this.context.put("facilitator", facilitator);
	}
	
	
	
	public void read() throws Exception
	{
		if (!SessionFacade.isLogged()) 
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}

		int id = this.request.getIntParameter("id");
		
		// Don't allow the read of messages that don't belongs to the current user
		UserSession us = SessionFacade.getUserSession();
		int userId = us.getUserId();
		
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
				
		org.etudes.api.app.jforum.PrivateMessage pm = jforumPrivateMessageService.getPrivateMessage(id);
		
		if (pm == null)
		{
			this.setTemplateName(TemplateKeys.PM_READ_DENIED);
			this.context.put("message", I18n.getMessage("PrivateMessage.readDenied"));
			return;
		}
		
		if (pm.getToUser().getId() == userId || pm.getFromUser().getId() == userId) 
		{
			// Update the message status, if needed
			if (pm.getType() == org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType.NEW.getType())
			{
				pm.setType(org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType.READ.getType());
				jforumPrivateMessageService.markMessageRead(pm.getId(), us.getSakaiUserId(), null, true);
				us.setPrivateMessages(us.getPrivateMessages() - 1);
			}
			
			org.etudes.api.app.jforum.User u = pm.getFromUser();
			//u.setSignature(PostCommon.processText(u.getSignature()));
			u.setSignature(PostCommon.processSmilies(u.getSignature(), SmiliesRepository.getSmilies()));

			this.context.put("pm", pm);
			this.context.put("userPMDraftsCount", jforumPrivateMessageService.getUserDraftsCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId()));
			this.setTemplateName(TemplateKeys.PM_READ);
			
			String folderId = this.request.getParameter("folder_id");
			
			// get first, previous, next, and last messages
			List<org.etudes.api.app.jforum.PrivateMessage> pmList = new ArrayList<org.etudes.api.app.jforum.PrivateMessage>();
			
			if (pm.getType() == org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType.SENT.getType())
			{
				pmList.addAll(jforumPrivateMessageService.sentbox(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId()));
				sortPrivateMessageList(pmList, false);
				this.context.put("sentbox", true);
			}
			else
			{
				if (folderId != null)
				{
					try
					{
						int fId = Integer.parseInt(folderId);
						
						pmList.addAll(jforumPrivateMessageService.folderMessages(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), fId));
						this.context.put("folderId", fId);
						
						// get folder info
						PrivateMessageFolder privateMessageFolder = jforumPrivateMessageService.getFolder(fId);
						this.context.put("privateMessageFolder", privateMessageFolder);
					}
					catch (NumberFormatException e)
					{
						pmList = jforumPrivateMessageService.inbox(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), false);
					}
				}
				else
				{
					pmList.addAll(jforumPrivateMessageService.inbox(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), false));
					this.context.put("inbox", true);
				}
				sortPrivateMessageList(pmList, true);
			}
			
			int indexCount = 0;
			boolean foundIndex = false;
			for (org.etudes.api.app.jforum.PrivateMessage privateMessage : pmList)
			{
				indexCount++;
				if (privateMessage.getId() == id)
				{
					indexCount--;
					foundIndex = true;
					break;
				}
			}
			
			if (foundIndex)
			{
				// first
				if (pmList.size() > 0)
				{
					this.context.put("first", pmList.get(0));
				}
				
				// previous
				if (((indexCount == (pmList.size() - 1)) ||  (indexCount < (pmList.size() - 1))) && indexCount > 0)
				{
					this.context.put("previous", pmList.get(indexCount - 1));
				}
				
				// next
				if (((indexCount == (pmList.size() - 1)) ||  (indexCount < (pmList.size() - 1))) && (indexCount < pmList.size() - 1))
				{
					this.context.put("next", pmList.get(indexCount + 1));
				}
				
				// last
				if (pmList.size() > 0)
				{
					this.context.put("last", pmList.get(pmList.size() - 1));
				}
				
				String[] params = new String[2];
				params[0] = String.valueOf(indexCount + 1);
				params[1] = String.valueOf(pmList.size());
						
				this.context.put("navText", I18n.getMessage("PrivateMessage.viewingPrivateMessage", params));
			}
			
			// user private message folders
			List<org.etudes.api.app.jforum.PrivateMessageFolder> pmFoldersList = jforumPrivateMessageService.getUserFolders(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
			this.context.put("pmFoldersList", pmFoldersList);
				
		}
		else
		{
			this.setTemplateName(TemplateKeys.PM_READ_DENIED);
			this.context.put("message", I18n.getMessage("PrivateMessage.readDenied"));
		}		
	}
	
	public void reply() throws Exception
	{
		if (!SessionFacade.isLogged()) {
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}

		int id = this.request.getIntParameter("id");

		PrivateMessage pm = new PrivateMessage();
		pm.setId(id);
		pm = DataAccessDriver.getInstance().newPrivateMessageDAO().selectById(pm);

		int userId = SessionFacade.getUserSession().getUserId();

		if (pm.getToUser().getId() != userId && pm.getFromUser().getId() != userId) {
			this.setTemplateName(TemplateKeys.PM_READ_DENIED);
			this.context.put("message", I18n.getMessage("PrivateMessage.readDenied"));
			return;
		}
		//Mallika - adding this if to strip out additional Res
		//Only add Re: at beginning if this is the first reply
		if (pm.getPost().getSubject().length() >= 3)
		{
		  if (!(pm.getPost().getSubject().substring(0,3).equals("Re:")))
		  {
		    pm.getPost().setSubject(I18n.getMessage("PrivateMessage.replyPrefix") + pm.getPost().getSubject());
		  }
	    }
		else
		{
			 pm.getPost().setSubject(I18n.getMessage("PrivateMessage.replyPrefix") + pm.getPost().getSubject());
			 
		}


		this.context.put("pm", pm);
		this.context.put("pmReply", true);

		this.sendFormCommon(DataAccessDriver.getInstance().newUserDAO().selectById(
				SessionFacade.getUserSession().getUserId()));
	}
	
	public void review() throws Exception
	{
		this.read();
		this.setTemplateName(TemplateKeys.PM_READ_REVIEW);
	}
	
	/**
	 * Creates the folder or mofifies the folder name
	 * 
	 * @throws Exception
	 */
	public void saveFolder() throws Exception
	{
		if (isUserObserver())
		{
			return;
		}
		
		// add folder
		String folderName = this.request.getParameter("folder_name");
		
		if (folderName == null || folderName.trim().length() == 0)
		{
			this.folders();
			return;
		}
		
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		String folderId = this.request.getParameter("folder_id");
		if (folderId != null)
		{
			try
			{
				int fId = Integer.parseInt(folderId);
				
				jforumPrivateMessageService.modifyPrivateMessageFolder(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), fId, folderName);
			}
			catch(NumberFormatException e)
			{
				
			}
		
		}
		else
		{
			jforumPrivateMessageService.createPrivateMessageFolder(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), folderName);
		}
				
		this.folders();
	}

	/**
	 * Save the moved message
	 * 
	 * @throws Exception
	 */
	public void saveMoveMessages() throws Exception
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering saveMoveMessages");
		}
		
		if (isUserObserver())
		{
			return;
		}
		
		String messages = JForum.getRequest().getParameter("messages");
		if (messages != null)
		{
			JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
			
			// save move messages
			int toFolderId = Integer.parseInt(JForum.getRequest().getParameter("to_folder"));
			
			// messages to be moved
			String privateMessageIds[] = messages.split(",");
			
			for (String privateMessageId : privateMessageIds)
			{
				int movedPrivateMessageId = Integer.parseInt(privateMessageId);
				
				try
				{
					jforumPrivateMessageService.moveTofolder(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), movedPrivateMessageId, toFolderId);
				}
				catch (JForumAccessException e)
				{
					this.context.put("errorMessage", e.toString());
				}
			}
		}
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting saveMoveMessages");
		}
		
		// redirect to inbox or folder where the messages are moved from
		String folderId = this.request.getParameter("folder_id");
		if (folderId != null)
		{
			this.context.put("folderId", folderId);
			this.messages();
		}
		else
		{
			this.inbox();
		}
	}
	
	public void send() throws Exception
	{
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}
		
		JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
		this.sendFormCommon(user);
		
		List<org.etudes.api.app.jforum.User> users = getUsers();
		this.context.put("users", users);
		
		
		boolean facilitator = JForumUserUtil.isJForumFacilitator(user.getSakaiUserId());
		this.context.put("facilitator", facilitator);
		this.context.put("isNewPost", true);
		this.context.put("privateMessageSend", true);
	}

	/** 
	 * Saves the private message that was invoked from activity meter and displays message with link
	 * back to activity meter
	 * 
	 * @throws Exception
	 */
	public void sendAMPMSave() throws Exception
	{
		if (logger.isDebugEnabled()) logger.debug("PrivateMessageAction - amsendSave");
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}

		String sakUserIdStr = this.request.getParameter("sakai_user_id");
		this.context.put("sakaiToUserIds", sakUserIdStr);
		
		String highPriority = this.request.getParameter("high_priority");
		this.context.put("highPriority", highPriority);
		
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		String toUsername = this.request.getParameter("toUsername");
		
		String toUserIds[] = (String[]) this.request.getObjectParameter("toUsername" + "ParamValues");
		
		int[] toJFUserIds = null;
		
		if (toUserIds != null)
		{
			toJFUserIds = new int[toUserIds.length];
			
			for (int i = 0; i < toUserIds.length; i++)
			{
				if (toUserIds[i] != null && toUserIds[i].trim().length() > 0)
				{
					toJFUserIds[i] = Integer.parseInt(toUserIds[i]);
				}
			}
		}
		else
		{
			toJFUserIds = new int[1];
			
			toJFUserIds[0] = Integer.parseInt(toUsername);
		}
		
		JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User fromUser = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
		
		org.etudes.api.app.jforum.User[] toUsers = null;
		try
		{
			toUsers =  new org.etudes.api.app.jforum.User[toJFUserIds.length];
			
			for (int i = 0; i < toJFUserIds.length; i++)
			{
				toUsers[i] = jforumUserService.getByUserId(toJFUserIds[i]);
			}
			
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn(e.toString(), e);
			}
		}
		String fromSakaiUserId = fromUser.getSakaiUserId(); 
		
		org.etudes.api.app.jforum.PrivateMessage privateMessage = null;
		
		List<String> toSakaiUserIds =  new ArrayList<String>();
		if (toUsers.length == 1)
		{
			privateMessage = jforumPrivateMessageService.newPrivateMessage(fromSakaiUserId, toUsers[0].getSakaiUserId());
			toSakaiUserIds.add(toUsers[0].getSakaiUserId());
		}
		else
		{
			privateMessage = jforumPrivateMessageService.newPrivateMessage(fromSakaiUserId);
			
			for (int i = 0; i < toUsers.length; i++)
			{
				toSakaiUserIds.add(toUsers[i].getSakaiUserId());
			}
		}
		
		privateMessage.setContext(ToolManager.getCurrentPlacement().getContext());
		
		if ((this.request.getParameter("high_priority_pm") != null) && (this.request.getIntParameter("high_priority_pm") == 1))
		{
			privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_HIGH);
		}
		else
		{
			privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_GENERAL);
		}
		
		org.etudes.api.app.jforum.Post post = privateMessage.getPost();
		
		post.setTime(new Date());
		
		post.setSubject(JForum.getRequest().getParameter("subject"));
				
		post.setBbCodeEnabled(this.request.getParameter("disable_bbcode") != null ? false : true);
		post.setSmiliesEnabled(this.request.getParameter("disable_smilies") != null ? false : true);
		post.setSignatureEnabled(this.request.getParameter("attach_sig") != null ? true : false);
		
		String message = this.request.getParameter("message");
		message = HtmlHelper.clean(message, true);
		post.setText(message);
		
		/*String modMessage = cleanMessage(message);
		post.setText(modMessage);		
		
		post.setHtmlEnabled(this.request.getParameter("disable_html") != null);
		
		if (post.isHtmlEnabled())
		{
			post.setText(SafeHtml.makeSafe(message));
		}
		else
		{
			post.setText(message);
		}*/
				
		//process post attachments
		processPostAttachments(jforumPrivateMessageService, privateMessage);
		
		if (toSakaiUserIds.size() == 1)
		{
			//jforumPrivateMessageService.sendPrivateMessage(privateMessage);
			try
			{
				jforumPrivateMessageService.sendPrivateMessageWithAttachments(privateMessage);
			}
			catch (JForumAccessException e)
			{
				JForum.enableCancelCommit();
				StringBuffer toSakUserIdSb = new StringBuffer();
				for (String sakUserId : toSakaiUserIds)
				{
					toSakUserIdSb.append(sakUserId);
					toSakUserIdSb.append(",");
				}
				
				String toSakUserIdStr = toSakUserIdSb.toString();
				if (toSakUserIdStr.endsWith(","))
				{
					toSakUserIdStr = toSakUserIdStr.substring(0, toSakUserIdStr.length() - 1);
				}
				this.request.addParameter("sakai_user_id_save_exp", toSakUserIdStr.toString());
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				this.amSendTo();
				return;
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				JForum.enableCancelCommit();
				
				StringBuffer toSakUserIdSb = new StringBuffer();
				for (String sakUserId : toSakaiUserIds)
				{
					toSakUserIdSb.append(sakUserId);
					toSakUserIdSb.append(",");
				}
				
				String toSakUserIdStr = toSakUserIdSb.toString();
				if (toSakUserIdStr.endsWith(","))
				{
					toSakUserIdStr = toSakUserIdStr.substring(0, toSakUserIdStr.length() - 1);
				}
				this.request.addParameter("sakai_user_id_save_exp", toSakUserIdStr.toString());
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				this.amSendTo();
				return;
			}
			catch (JForumAttachmentBadExtensionException e)
			{
				JForum.enableCancelCommit();
				StringBuffer toSakUserIdSb = new StringBuffer();
				for (String sakUserId : toSakaiUserIds)
				{
					toSakUserIdSb.append(sakUserId);
					toSakUserIdSb.append(",");
				}
				
				String toSakUserIdStr = toSakUserIdSb.toString();
				if (toSakUserIdStr.endsWith(","))
				{
					toSakUserIdStr = toSakUserIdStr.substring(0, toSakUserIdStr.length() - 1);
				}
				this.request.addParameter("sakai_user_id_save_exp", toSakUserIdStr.toString());
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				this.amSendTo();
				return;
			}
		}
		else
		{
			try
			{
				jforumPrivateMessageService.sendPrivateMessageWithAttachments(privateMessage, toSakaiUserIds);
			}
			catch (JForumAccessException e)
			{
				JForum.enableCancelCommit();
				StringBuffer toSakUserIdSb = new StringBuffer();
				for (String sakUserId : toSakaiUserIds)
				{
					toSakUserIdSb.append(sakUserId);
					toSakUserIdSb.append(",");
				}
				
				String toSakUserIdStr = toSakUserIdSb.toString();
				if (toSakUserIdStr.endsWith(","))
				{
					toSakUserIdStr = toSakUserIdStr.substring(0, toSakUserIdStr.length() - 1);
				}
				this.request.addParameter("sakai_user_id_save_exp", toSakUserIdStr.toString());
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				this.amSendTo();
				return;
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				JForum.enableCancelCommit();
				StringBuffer toSakUserIdSb = new StringBuffer();
				for (String sakUserId : toSakaiUserIds)
				{
					toSakUserIdSb.append(sakUserId);
					toSakUserIdSb.append(",");
				}
				
				String toSakUserIdStr = toSakUserIdSb.toString();
				if (toSakUserIdStr.endsWith(","))
				{
					toSakUserIdStr = toSakUserIdStr.substring(0, toSakUserIdStr.length() - 1);
				}
				this.request.addParameter("sakai_user_id_save_exp", toSakUserIdStr);
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				this.amSendTo();
				return;
			}
			catch (JForumAttachmentBadExtensionException e)
			{
				JForum.enableCancelCommit();
				StringBuffer toSakUserIdSb = new StringBuffer();
				for (String sakUserId : toSakaiUserIds)
				{
					toSakUserIdSb.append(sakUserId);
					toSakUserIdSb.append(",");
				}
				
				String toSakUserIdStr = toSakUserIdSb.toString();
				if (toSakUserIdStr.endsWith(","))
				{
					toSakUserIdStr = toSakUserIdStr.substring(0, toSakUserIdStr.length() - 1);
				}
				this.request.addParameter("sakai_user_id_save_exp", toSakUserIdStr.toString());
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				this.amSendTo();
				return;
			}
		}
		
		/*boolean pmEmailEnabled = false;

		PrivateMessage pm = new PrivateMessage();
		pm.setPost(PostCommon.fillPostFromRequest());

		if ((this.request.getParameter("high_priority_pm") != null) && (this.request.getIntParameter("high_priority_pm") == 1))
		{
			pm.setPriority(PrivateMessage.PRIORITY_HIGH);
		}
		else
		{
			pm.setPriority(PrivateMessage.PRIORITY_GENERAL);
		}

		User fromUser = DataAccessDriver.getInstance().newUserDAO().selectById(SessionFacade.getUserSession().getUserId());
		pm.setFromUser(fromUser);

		int attachmentIds[] = null;
		try
		{
			attachmentIds = addPMAttachments();
		}
		catch (Exception e)
		{
			this.context.put("post", pm.getPost());
			this.context.put("pm", pm);
			this.request.addParameter("sakai_user_id", sakUserIdStr);
			this.request.addParameter("highPriority", highPriority);
			this.amSendTo();
			return;
		}

		String toUsername = this.request.getParameter("toUsername");
		
		String userEmail = null;
		int toUserId = -1;
		String toUserIds[] = (String[]) this.request.getObjectParameter("toUsername" + "ParamValues");
		if (toUserIds != null)
		{

			for (int i = 0; i < toUserIds.length; i++)
			{
				if (toUserIds[i] != null && toUserIds[i].trim().length() > 0)
				{
					toUserId = Integer.parseInt(toUserIds[i]);
					User usr = DataAccessDriver.getInstance().newUserDAO().selectById(toUserId);
					toUserId = usr.getId();
					userEmail = usr.getEmail();
					toUsername = usr.getFirstName() + " " + usr.getLastName();
					pmEmailEnabled = usr.isNotifyPrivateMessagesEnabled();
					sendPrivateMessage(pm, toUsername, userEmail, pmEmailEnabled, toUserId, attachmentIds);
				}
			}
		}
		else
		{
			toUserId = Integer.parseInt(toUsername);
			User usr = DataAccessDriver.getInstance().newUserDAO().selectById(toUserId);
			toUserId = usr.getId();
			userEmail = usr.getEmail();
			toUsername = usr.getFirstName() + " " + usr.getLastName();
			pmEmailEnabled = usr.isNotifyPrivateMessagesEnabled();
			sendPrivateMessage(pm, toUsername, userEmail, pmEmailEnabled, toUserId, attachmentIds);
		}*/
		
		this.setTemplateName(TemplateKeys.PM_AM_SENDSAVE);
		this.context.put("saveSuccess", Boolean.TRUE);
		this.context.put("pageTitle", I18n.getMessage("PrivateMessage.pageTitle"));
	}

	/**
	 * save private message sent to individual user from pop up window
	 * @throws Exception
	 */
	public void sendPMSave() throws Exception
	{
		if (!SessionFacade.isLogged()) 
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}

		String sId = this.request.getParameter("toUserId");
		/*String toUsername = this.request.getParameter("toUsername");
		String userEmail = this.request.getParameter("toUserEmail");
		
		boolean preview = (this.request.getParameter("preview") != null && this.request.getParameter("preview").trim().length() > 0);*/
		
		JForumPrivateMessageService jforumPrivateMessageService = null;
		jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		if (jforumPrivateMessageService == null) return;
		
		JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		
		// to user
		org.etudes.api.app.jforum.User toUser = jforumUserService.getByUserId(Integer.parseInt(sId));		
		
		org.etudes.api.app.jforum.PrivateMessage privateMessage = null;
		
		List<String> toSakaiUserIds =  new ArrayList<String>();
		
		privateMessage = jforumPrivateMessageService.newPrivateMessage(UserDirectoryService.getCurrentUser().getId(), toUser.getSakaiUserId());
		
		toSakaiUserIds.add(toUser.getSakaiUserId());
		
		privateMessage.setContext(ToolManager.getCurrentPlacement().getContext());
		
		if ((this.request.getParameter("high_priority_pm") != null) && (this.request.getIntParameter("high_priority_pm") == 1))
		{
			privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_HIGH);
		}
		else
		{
			privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_GENERAL);
		}
		
		org.etudes.api.app.jforum.Post post = privateMessage.getPost();
		
		post.setTime(new Date());
		
		post.setSubject(this.request.getParameter("subject"));
		
		post.setBbCodeEnabled(this.request.getParameter("disable_bbcode") != null ? false : true);
		post.setSmiliesEnabled(this.request.getParameter("disable_smilies") != null ? false : true);
		post.setSignatureEnabled(this.request.getParameter("attach_sig") != null ? true : false);
		
		String message = this.request.getParameter("message");
		message = HtmlHelper.clean(message, true);
		post.setText(message);
		
		/*String modMessage = cleanMessage(message);
		post.setText(modMessage);		
		
		post.setHtmlEnabled(this.request.getParameter("disable_html") != null);
		
		if (post.isHtmlEnabled())
		{
			post.setText(SafeHtml.makeSafe(message));
		}
		else
		{
			post.setText(message);
		}		*/
		
		//process post attachments
		processPostAttachments(jforumPrivateMessageService, privateMessage);
		
		if (toSakaiUserIds.size() == 1)
		{
			//jforumPrivateMessageService.sendPrivateMessage(privateMessage);
			try
			{
				jforumPrivateMessageService.sendPrivateMessageWithAttachments(privateMessage);
			}
			catch (JForumAccessException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("postPreview", privateMessage);
				this.request.addParameter("user_id", sId);
				this.sendTo();
				this.context.put("savesucess", false);
				return;
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("postPreview", privateMessage);
				this.request.addParameter("user_id", sId);
				this.sendTo();
				this.context.put("savesucess", false);
				return;
			}
			catch (JForumAttachmentBadExtensionException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("postPreview", privateMessage);
				this.request.addParameter("user_id", sId);
				this.sendTo();
				this.context.put("savesucess", false);
				return;
			}
		}
		
		/*boolean pmEmailEnabled = false;
		
		PrivateMessage pm = new PrivateMessage();
		pm.setPost(PostCommon.fillPostFromRequest());

		if ((this.request.getParameter("high_priority_pm") != null) && (this.request.getIntParameter("high_priority_pm") == 1))
		{
			pm.setPriority(PrivateMessage.PRIORITY_HIGH);
		}
		else
		{
			pm.setPriority(PrivateMessage.PRIORITY_GENERAL);
		}
		
		if (preview) 
		{
			this.context.put("postPreview", pm);
			this.context.put("preview", true);
			this.request.addParameter("user_id", sId);
			this.sendTo();
			return;
		}
		
		int attachmentIds[] = null;
		try
		{
			attachmentIds = addPMAttachments();
		}
		catch (Exception e)
		{
			this.request.addParameter("user_id", sId);
			this.sendTo();
			return;
		}			
		int toUserId = Integer.parseInt(sId);
       	User usr = DataAccessDriver.getInstance().newUserDAO().selectById(toUserId);
		toUsername = usr.getFirstName()+ " "+usr.getLastName();
		pmEmailEnabled = usr.isNotifyPrivateMessagesEnabled();
		
		sendPrivateMessage(pm, toUsername, userEmail, pmEmailEnabled, toUserId, attachmentIds);*/

		this.context.put("savesucess", true);
		this.setTemplateName(TemplateKeys.PM_SENDTOSAVE);
	}

	
	public void sendSave() throws Exception
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("PrivateMessageAction - sendSave");
		}
			
		if (!SessionFacade.isLogged())
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}
		
		try
		{
			/*if (JForum.getRequest().getParameter("postSubmit") != null)
			{
				sendPrivateMessage();
			}
			else if (JForum.getRequest().getParameter("saveDraft") != null)
			{
				savePrivateMessage();
			}*/
			
			String savePMAction = JForum.getRequest().getParameter("savePMaction");
			
			if (savePMAction != null)
			{
				if (savePMAction.trim().equalsIgnoreCase("saveDraft"))
				{
					JForum.getRequest().addParameter("saveDraft", "saveDraft");
					savePrivateMessage();
				}
				else
				{
					sendPrivateMessage();
				}
			}
			else
			{
				sendPrivateMessage();
			}
			
		}
		catch (JForumAccessException e)
		{
			this.send();
			return;
		}
		catch (JForumAttachmentOverQuotaException e)
		{
			this.send();
			return;
		}
		catch (JForumAttachmentBadExtensionException e)
		{
			this.send();
			return;
		}
	}

	public void sendTo() throws Exception
	{
		if (!SessionFacade.isLogged()) 
		{
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}

		// User user = DataAccessDriver.getInstance().newUserDAO().selectById(SessionFacade.getUserSession().getUserId());
		JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());

		int userId = this.request.getIntParameter("user_id");
		
		if (userId > 0)
		{
			//User user1 = DataAccessDriver.getInstance().newUserDAO().selectById(userId);
			org.etudes.api.app.jforum.User user1 = jforumUserService.getByUserId(userId);
			JForumSecurityService jforumSecurityService = (JForumSecurityService)ComponentManager.get("org.etudes.api.app.jforum.JForumSecurityService");
			
			if (!jforumSecurityService.isUserActive(ToolManager.getCurrentPlacement().getContext(), user1.getSakaiUserId()) || jforumSecurityService.isUserBlocked(ToolManager.getCurrentPlacement().getContext(), user1.getSakaiUserId()))
			{
				this.setTemplateName(TemplateKeys.PM_SENDSAVE_USER_NOTFOUND);
				this.context.put("message", I18n.getMessage("PrivateMessage.userInactive"));
				return;
			}
			else
			{
				boolean facilitator = JForumUserUtil.isJForumFacilitator(user1.getSakaiUserId());
				boolean participant = JForumUserUtil.isJForumParticipant(user1.getSakaiUserId());
				if (!facilitator && !participant)
				{
					this.setTemplateName(TemplateKeys.PM_SENDSAVE_USER_NOTFOUND);
					this.context.put("message", I18n.getMessage("PrivateMessage.userInactive"));
					return;
				}
			}

			
			this.context.put("pmRecipient", user1);
			this.context.put("toUserId", String.valueOf(user1.getId()));

            this.context.put("toUsername", user1.getFirstName()+" "+user1.getLastName());
			
			this.context.put("toUserEmail", user1.getEmail());
			
			this.context.put("pageTitle", SystemGlobals.getValue(ConfigKeys.FORUM_NAME) + " - " + I18n.getMessage("PrivateMessage.title"));
		}
		else
		{
			this.setTemplateName(TemplateKeys.PM_SENDSAVE_USER_NOTFOUND);
			this.context.put("message", I18n.getMessage("PrivateMessage.PrivateMessage.userNotFound"));
			return;
		}

		this.sendFormCommon(user);
		
		this.context.put("action", "sendPMSave");
		this.setTemplateName(TemplateKeys.PM_SENDTO);
	}

	public void sentbox() throws Exception
	{
		if (!SessionFacade.isLogged()) {
			this.setTemplateName(ViewCommon.contextToLogin());
			return;
		}
		
		if (isUserObserver())
		{
			return;
		}

		User user = new User();
		user.setId(SessionFacade.getUserSession().getUserId());

		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		List<org.etudes.api.app.jforum.PrivateMessage> pmList = jforumPrivateMessageService.sentbox(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		
		sortPrivateMessageList(pmList, false);

		this.context.put("sentbox", true);
		this.context.put("pmList", pmList);
		this.context.put("userPMDraftsCount", jforumPrivateMessageService.getUserDraftsCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId()));
		
		// user private message folders
		List<org.etudes.api.app.jforum.PrivateMessageFolder> pmFoldersList = jforumPrivateMessageService.getUserFolders(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		this.context.put("pmFoldersList", pmFoldersList);
		
		// inbox unread count
		int inboxUnreadCount = jforumPrivateMessageService.getUserFolderUnreadCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), 0);
		this.context.put("inboxUnreadCount", inboxUnreadCount);
		
		this.setTemplateName(TemplateKeys.PM_SENTBOX);
		this.putTypes();
	}
	
	/**
	 * save attachments
	 * @return attachment Id's
	 * @throws Exception
	 */
	private int[] addPMAttachments() throws Exception
	{
		AttachmentCommon attachments = new AttachmentCommon(this.request);

		try
		{
			attachments.preProcess();
		}
		catch (Exception e)
		{
			JForum.enableCancelCommit();
			this.context.put("errorMessage", e.getMessage());
			throw e;
		}

		return attachments.insertPMAttachments();
	}

	private void putTypes()
	{
		this.context.put("NEW", new Integer(PrivateMessageType.NEW));
		this.context.put("READ", new Integer(PrivateMessageType.READ));
		this.context.put("UNREAD", new Integer(PrivateMessageType.UNREAD));
		this.context.put("PRIORITY_HIGH", new Integer(PrivateMessage.PRIORITY_HIGH));
	}
	
	private void sendFormCommon(org.etudes.api.app.jforum.User user)
	{
		this.context.put("user", user);
		this.context.put("moduleName", "pm");
		this.context.put("action", "sendSave");
		this.setTemplateName(TemplateKeys.PM_SENDFORM);
		//this.context.put("htmlAllowed", true);
		// this.context.put("maxAttachments", SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_MAX_POST));
		this.context.put("maxAttachments", SakaiSystemGlobals.getValue(ConfigKeys.ATTACHMENTS_MAX_POST));
		//12/11/2007 Murthy - attachments enabled
		this.context.put("attachmentsEnabled", true);
		this.context.put("maxAttachmentsSize", new Integer(0));
		this.context.put("pmPost", true);
	}	
	
	private void sendFormCommon(User user)
	{
		this.context.put("user", user);
		this.context.put("moduleName", "pm");
		this.context.put("action", "sendSave");
		this.setTemplateName(TemplateKeys.PM_SENDFORM);
		//this.context.put("htmlAllowed", true);
		// this.context.put("maxAttachments", SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_MAX_POST));
		this.context.put("maxAttachments", SakaiSystemGlobals.getValue(ConfigKeys.ATTACHMENTS_MAX_POST));
		//12/11/2007 Murthy - attachments enabled
		this.context.put("attachmentsEnabled", true);
		this.context.put("maxAttachmentsSize", new Integer(0));
		this.context.put("pmPost", true);
	}
	
	/**
	 * send private message
	 * @param pm - PrivateMessage
	 * @param toUsername - to user name
	 * @param userEmail - user email
	 * @param pmEmailEnabled - is pm enabled
	 * @param toUserId - to user id
	 * @param attachmentIds - attachment Id's
	 * @throws Exception
	 */
	private void sendPrivateMessage(PrivateMessage pm, String toUsername, String userEmail, boolean pmEmailEnabled, int toUserId, int attachmentIds[]) throws Exception {
		if (toUserId == -1) {
			this.setTemplateName(TemplateKeys.PM_SENDSAVE_USER_NOTFOUND);
			this.context.put("message", I18n.getMessage("PrivateMessage.userIdNotFound"));
			return;
		}

		User fromUser = DataAccessDriver.getInstance().newUserDAO().selectById(SessionFacade.getUserSession().getUserId());
		pm.setFromUser(fromUser);

		User toUser = new User();
		toUser.setId(toUserId);
		toUser.setUsername(toUsername);
		toUser.setEmail(userEmail);
		pm.setToUser(toUser);

		boolean preview = (this.request.getParameter("preview") != null);

		if (!preview) {
			//DataAccessDriver.getInstance().newPrivateMessageDAO().send(pm);
			DataAccessDriver.getInstance().newPrivateMessageDAO().saveMessage(pm, attachmentIds);

			/*this.setTemplateName(TemplateKeys.PM_SENDSAVE);
			this.context.put("message", I18n.getMessage("PrivateMessage.messageSent",
							new String[] { this.request.getContextPath() +"/pm/inbox"
											+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION)}));*/

			// If the target user if in the forum, then increments its
			// private messate count
			String sid = SessionFacade.isUserInSession(toUserId);
			if (sid != null) {
				UserSession us = SessionFacade.getUserSession(sid);
				us.setPrivateMessages(us.getPrivateMessages() + 1);
			}
			
			if (logger.isDebugEnabled()) logger.debug("Before userEmail");
			if (userEmail != null && userEmail.trim().length() > 0) {
				if (logger.isDebugEnabled()) logger.debug("Useremail is not null "+pmEmailEnabled);
				//Mallika-commenting line below and going based off of settings instead
				/*if (SystemGlobals.getBoolValue(ConfigKeys.MAIL_NOTIFY_ANSWERS)) {*/
				if ((pmEmailEnabled == true) || (pm.getPriority() == PrivateMessage.PRIORITY_HIGH)) {
					if (logger.isDebugEnabled()) logger.debug("Sending email");
					
					try	{
						new InternetAddress(toUser.getEmail());
					} catch (AddressException e) {
						if (logger.isWarnEnabled()) logger.warn("sendPrivateMessage(...) : "+ toUser.getEmail() + " is invalid. And exception is : "+ e);
						return;
					}
					
					//get attachments
					List attachments = DataAccessDriver.getInstance().newAttachmentDAO().selectPMAttachments(pm.getId());
					try {
						if (attachments != null && attachments.size() > 0)
						{
							QueuedExecutor.getInstance().execute(new EmailSenderTask(new PrivateMessageSpammer(toUser, pm, attachments)));
						}
						else
						{
							QueuedExecutor.getInstance().execute(new EmailSenderTask(new PrivateMessageSpammer(toUser, pm)));
						}
					}
					catch (Exception e) {
						logger.error(this.getClass().getName() +
								".sendSave() : " + e.getMessage(), e);
					}
				}
			}
		}
	}
	

	/**
	 * Apply sort to the PM list
	 * 
	 * @param pmList	PM list
	 * 
	 * @param pmSort	Sort
	 * 
	 * @param fromUser	true - sort on From user last name
	 * 					false - sort on To user last name
	 */
	protected void applySort(List pmList, PrivateMessageSort pmSort, final boolean fromUser)
	{
		if (pmSort == PrivateMessageSort.last_name_a)
		{
			Collections.sort(pmList, new Comparator<org.etudes.api.app.jforum.PrivateMessage>()
			{
				public int compare(org.etudes.api.app.jforum.PrivateMessage p1, org.etudes.api.app.jforum.PrivateMessage p2) 
				{
					if (fromUser)
					{
						return p1.getFromUser().getLastName().toUpperCase().compareTo(p2.getFromUser().getLastName().toUpperCase());
					}
					else
					{
						return p1.getToUser().getLastName().toUpperCase().compareTo(p2.getToUser().getLastName().toUpperCase());
					}
					
				}
			});
		}
		else if (pmSort == PrivateMessageSort.last_name_d)
		{
			Collections.sort(pmList, new Comparator<org.etudes.api.app.jforum.PrivateMessage>()
			{
				public int compare(org.etudes.api.app.jforum.PrivateMessage p1, org.etudes.api.app.jforum.PrivateMessage p2) 
				{
					if (fromUser)
					{
						return -1 * (p1.getFromUser().getLastName().toUpperCase().compareTo(p2.getFromUser().getLastName().toUpperCase()));
					}
					else
					{
						return -1 * (p1.getToUser().getLastName().toUpperCase().compareTo(p2.getToUser().getLastName().toUpperCase()));
					}
					
				}
			});
		}
		else if (pmSort == PrivateMessageSort.date_a)
		{
			Collections.sort(pmList, new Comparator<org.etudes.api.app.jforum.PrivateMessage>()
			{
				public int compare(org.etudes.api.app.jforum.PrivateMessage p1, org.etudes.api.app.jforum.PrivateMessage p2) 
				{
					return p1.getPost().getTime().compareTo(p2.getPost().getTime());
					
				}
			});
		}
		else if (pmSort == PrivateMessageSort.date_d)
		{
			Collections.sort(pmList, new Comparator<org.etudes.api.app.jforum.PrivateMessage>()
			{
				public int compare(org.etudes.api.app.jforum.PrivateMessage p1, org.etudes.api.app.jforum.PrivateMessage p2) 
				{
					return -1 * (p1.getPost().getTime().compareTo(p2.getPost().getTime()));
					
				}
			});
		}
	}
	
	protected void editPostAttachments(org.etudes.api.app.jforum.Post post)
	{
		String[] delete = null;
		String s = this.request.getParameter("delete_attach");

		if (s != null)
		{
			delete = s.split(",");
		}
		
		List<org.etudes.api.app.jforum.Attachment> attachments = post.getAttachments();

		if (delete != null)
		{
			for (int i = 0; i < delete.length; i++)
			{
				if (delete[i] != null && !delete[i].equals(""))
				{
					int id = Integer.parseInt(delete[i]);
					
					for (Iterator<org.etudes.api.app.jforum.Attachment> iter = attachments.iterator(); iter.hasNext();)
					{
						org.etudes.api.app.jforum.Attachment attachment = iter.next();
						
						if (attachment.getId() == id)
						{
							iter.remove();
							break;
						}
					}
				}
			}
		}
	}
	
	protected void getFolders() throws JForumAccessException
	{
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		List<org.etudes.api.app.jforum.PrivateMessageFolder> pmFoldersList = jforumPrivateMessageService.getUserFolders(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		
		this.context.put("pmFoldersList", pmFoldersList);
		this.context.put("userPMDraftsCount", jforumPrivateMessageService.getUserDraftsCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId()));
		
		// inbox unread count
		int inboxUnreadCount = jforumPrivateMessageService.getUserFolderUnreadCount(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId(), 0);
		this.context.put("inboxUnreadCount", inboxUnreadCount);
	}
	
	protected List<String> getRequestBccSakaiUserIds()
	{
		// bcc users
		String bccUserIds[] = (String[]) this.request.getObjectParameter("bccUsername" + "ParamValues");
		String bccUsername = this.request.getParameter("bccUsername");
		int[] bccJFUserIds = null;
		
		if (bccUserIds != null && bccUserIds.length > 0 )
		{
			if (bccUserIds != null)
			{
				bccJFUserIds = new int[bccUserIds.length];
				
				for (int i = 0; i < bccUserIds.length; i++)
				{
					if (bccUserIds[i] != null && bccUserIds[i].trim().length() > 0)
					{
						bccJFUserIds[i] = Integer.parseInt(bccUserIds[i]);
					}
				}
			}
		}
		else
		{
			if (bccUsername != null && bccUsername.trim().length() != 0)
			{
				bccJFUserIds = new int[1];
				
				bccJFUserIds[0] = Integer.parseInt(bccUsername);
			}
		}
		
		List<org.etudes.api.app.jforum.User> bccUsers = new ArrayList<org.etudes.api.app.jforum.User>();
		
		if (bccJFUserIds != null)
		{
			JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			try
			{
				for (int i = 0; i < bccJFUserIds.length; i++)
				{
					if (bccJFUserIds[i] > 0)
					{
						bccUsers.add(jforumUserService.getByUserId(bccJFUserIds[i]));
					}
				}					
			}
			catch (Exception e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(e.toString(), e);
				}
			}
		}
		
		List<String> bccSakaiUserIds =  new ArrayList<String>();
		for (org.etudes.api.app.jforum.User user : bccUsers)
		{
			bccSakaiUserIds.add(user.getSakaiUserId());
		}
		
		if (bccSakaiUserIds.isEmpty())
		{
			return null;
		}
		else
		{
			return bccSakaiUserIds;
		}
	}
	
	/**
	 * Gets site users(observers are removed from the list)
	 * 
	 * @return	Site users
	 */
	protected List<org.etudes.api.app.jforum.User> getUsers()
	{
		List<org.etudes.api.app.jforum.User> users = null;
		try
		{
			JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			JForumSecurityService jforumSecurityService = (JForumSecurityService)ComponentManager.get("org.etudes.api.app.jforum.JForumSecurityService");
			
			users = jforumUserService.getSiteUsers(ToolManager.getCurrentPlacement().getContext());
			
			Iterator<org.etudes.api.app.jforum.User> userIterator = users.iterator();
			while (userIterator.hasNext())
			{
				org.etudes.api.app.jforum.User etudesUser = (org.etudes.api.app.jforum.User) userIterator.next();
				if (jforumSecurityService.isEtudesObserver(ToolManager.getCurrentPlacement().getContext(), etudesUser.getSakaiUserId()))
				{
					userIterator.remove();
				}
			}
			
			return users;
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled())
			{
				logger.error(e.toString(), e);
			}
			
			//this.context.put("users", new ArrayList<org.etudes.api.app.jforum.User>());
			return new ArrayList<org.etudes.api.app.jforum.User>();
		}
	}
	
	/**
	 * Observer not allowed to perform private message functions
	 * 
	 * @return	true - if user has observer role
	 * 			false - if user has no observer role
	 */
	protected boolean isUserObserver()
	{
		JForumSecurityService jforumSecurityService = (JForumSecurityService)ComponentManager.get("org.etudes.api.app.jforum.JForumSecurityService");
		
		boolean isObserver = jforumSecurityService.isEtudesObserver(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		if (isObserver)
		{
			this.context.put("errorMessage", I18n.getMessage("User.Observer.NotAllowed.PrivateMessages"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED);
			return true;
		}
		
		return false;
	}
	
	/**
	 * process post attachments
	 * 
	 * @param jforumPrivateMessageService	jforumPrivateMessageService
	 * 
	 * @param privateMessage	Private message with attachments
	 */
	protected void processPostAttachments(JForumPrivateMessageService jforumPrivateMessageService, org.etudes.api.app.jforum.PrivateMessage privateMessage)
	{
		org.etudes.api.app.jforum.Post post = privateMessage.getPost();
		
		String t = this.request.getParameter("total_files");

		if (t == null || "".equals(t))
		{
			return;
		}

		int total = Integer.parseInt(t);

		if (total < 1)
		{
			return;
		}
		post.setHasAttachments(true);
		

		for (int i = 0; i < total; i++)
		{
			DiskFileItem item = (DiskFileItem) this.request.getObjectParameter("file_" + i);
			if (item == null)
			{
				continue;
			}

			if (item.getName().indexOf('\000') > -1)
			{
				logger.warn("Possible bad attachment (null char): " + item.getName() + " - user_id: " + SessionFacade.getUserSession().getUserId());
				continue;
			}
			
			String fileName = null;
			String contentType = null;
			String comments = null;
			byte[] fileContent = null;
			
			fileName = item.getName();
			contentType = item.getContentType();
			comments = this.request.getParameter("comment_" + i);
			fileContent = item.get();
			
			org.etudes.api.app.jforum.Attachment  attachment = jforumPrivateMessageService.newAttachment(fileName, contentType, comments, fileContent);
			if (attachment != null)
			{
				post.getAttachments().add(attachment);
			}
		}

	}
	
	/**
	 * Reply to private message
	 * 
	 * @throws JForumAccessException
	 * @throws JForumAttachmentOverQuotaException
	 * @throws JForumAttachmentBadExtensionException
	 */
	protected void replyToPrivateMessage() throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		//String sId = this.request.getParameter("toUserId");
		//String toUsername = this.request.getParameter("toUsername");
		
		//int toUserId = Integer.parseInt(sId);
		
		JForumPrivateMessageService jforumPrivateMessageService = null;
		jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		if (jforumPrivateMessageService == null) return;
		
		String exisPmId = this.request.getParameter("exisPmId");
		if (exisPmId != null && exisPmId.trim().length() > 0)
		{
			org.etudes.api.app.jforum.PrivateMessage privateMessage = null;
			
			privateMessage = jforumPrivateMessageService.newPrivateMessage(Integer.parseInt(exisPmId));
			
			//privateMessage.setContext(ToolManager.getCurrentPlacement().getContext());
			JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			org.etudes.api.app.jforum.User currentUser = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
			if ((privateMessage == null || currentUser == null) || (privateMessage.getFromUser().getId() != currentUser.getId()))
			{
				this.context.put("errorMessage", "Error while replying to existing message.");
			}
			
			if ((this.request.getParameter("high_priority_pm") != null) && (this.request.getIntParameter("high_priority_pm") == 1))
			{
				privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_HIGH);
			}
			else
			{
				privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_GENERAL);
			}
			
			org.etudes.api.app.jforum.Post post = privateMessage.getPost();
			
			post.setTime(new Date());
			
			post.setSubject(JForum.getRequest().getParameter("subject"));
			
			post.setBbCodeEnabled(this.request.getParameter("disable_bbcode") != null ? false : true);
			post.setSmiliesEnabled(this.request.getParameter("disable_smilies") != null ? false : true);
			post.setSignatureEnabled(this.request.getParameter("attach_sig") != null ? true : false);
			
			String message = this.request.getParameter("message");
			message = HtmlHelper.clean(message, true);
			post.setText(message);
			
			// bcc users - only reply message can have bcc users
						
			/*String modMessage = cleanMessage(message);
			post.setText(modMessage);		
			
			post.setHtmlEnabled(this.request.getParameter("disable_html") != null);
			
			if (post.isHtmlEnabled())
			{
				post.setText(SafeHtml.makeSafe(message));
			}
			else
			{
				post.setText(message);
			}*/			
			
			//process post attachments
			processPostAttachments(jforumPrivateMessageService, privateMessage);
			
			try
			{
				List<String> bccSakaiUserIds =  new ArrayList<String>();
				if (getRequestBccSakaiUserIds() != null)
				{
					bccSakaiUserIds.addAll(getRequestBccSakaiUserIds());
				}
				
				if (bccSakaiUserIds == null || bccSakaiUserIds.isEmpty())
				{
					jforumPrivateMessageService.replyPrivateMessage(privateMessage);
				}
				else
				{
					jforumPrivateMessageService.replyPrivateMessage(privateMessage, bccSakaiUserIds);
				}
			}
			catch (JForumAccessException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("replyError", true);
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				this.request.addParameter("id", String.valueOf(privateMessage.getId()));
				try
				{
					this.quote();
				}
				catch (Exception e1)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn(e1.toString(), e1);
					}
				}
				throw e;
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("replyError", true);
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				this.request.addParameter("id", String.valueOf(privateMessage.getId()));
				try
				{
					this.quote();
				}
				catch (Exception e1)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn(e1.toString(), e1);
					}
				}
				throw e;
			}
			catch (JForumAttachmentBadExtensionException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("replyError", true);
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				this.request.addParameter("id", String.valueOf(privateMessage.getId()));
				try
				{
					this.quote();
				}
				catch (Exception e1)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn(e1.toString(), e1);
					}
				}
				throw e;
			}
		}
		
	}
	
	/**
	 * saves private message as draft
	 */
	protected void savePrivateMessage() throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		if ((JForum.getRequest().getParameter("saveDraft")) != null && (JForum.getRequest().getParameter("saveDraft").equals("saveDraft")))
		{

			String pmQuote = JForum.getRequest().getParameter("pmQuote");
			
			String sId = this.request.getParameter("toUserId");
			String toUsername = this.request.getParameter("toUsername");
			
			int[] toJFUserIds = null;
			
			if (sId == null || sId.trim().equals(""))
			{
				String toUserIds[] = (String[]) this.request.getObjectParameter("toUsername" + "ParamValues");
				if (toUserIds != null)
				{
					toJFUserIds = new int[toUserIds.length];
					
					for (int i = 0; i < toUserIds.length; i++)
					{
						if (toUserIds[i] != null && toUserIds[i].trim().length() > 0)
						{
							toJFUserIds[i] = Integer.parseInt(toUserIds[i]);
						}
					}
				}
				else
				{
					if (toUsername != null && toUsername.trim().length() != 0)
					{
						toJFUserIds = new int[1];
						
						toJFUserIds[0] = Integer.parseInt(toUsername);
					}
				}
			}
			else
			{
				toJFUserIds = new int[1];
				toJFUserIds[0] = Integer.parseInt(sId);
			}
			
			JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
			
			if (jforumPrivateMessageService == null) return;
			
			JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			org.etudes.api.app.jforum.User fromUser = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
			List<org.etudes.api.app.jforum.User> toUsers = new ArrayList<org.etudes.api.app.jforum.User>();
			
			if (toJFUserIds != null)
			{
				try
				{
					for (int i = 0; i < toJFUserIds.length; i++)
					{
						if (toJFUserIds[i] > 0)
						{
							toUsers.add(jforumUserService.getByUserId(toJFUserIds[i]));
						}
					}
					
				}
				catch (Exception e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn(e.toString(), e);
					}
				}
			}
			String fromSakaiUserId = fromUser.getSakaiUserId(); 
			
			org.etudes.api.app.jforum.PrivateMessage privateMessage = null;
			
			List<String> toSakaiUserIds =  new ArrayList<String>();
			
			String pmDraftId = this.request.getParameter("pm_draft_id");
			
			int exispmDraftId = 0;
			try
			{
				exispmDraftId = Integer.parseInt(pmDraftId);
			}
			catch (NumberFormatException e)
			{
				
			}
			
			if (pmDraftId != null && exispmDraftId > 0)
			{
				privateMessage = jforumPrivateMessageService.getPrivateMessageDraft(exispmDraftId);
				
				if (privateMessage ==  null)
				{
					this.setTemplateName(TemplateKeys.PM_EDIT_DRAFT_DENIED);
					this.context.put("message", I18n.getMessage("PrivateMessage.draftNotExisting"));
					return;
				}
				
				// Don't allow the edit draft that don't belongs to the current user
				UserSession us = SessionFacade.getUserSession();
				int userId = us.getUserId();
				
				if (privateMessage.getFromUser().getId() != userId) 
				{
					this.setTemplateName(TemplateKeys.PM_EDIT_DRAFT_DENIED);
					this.context.put("message", I18n.getMessage("PrivateMessage.editDraftDenied"));
					return;
				}
			}
			else
			{
				if (pmQuote != null && pmQuote.trim().equalsIgnoreCase("1"))
				{
					String exisPmId = this.request.getParameter("exisPmId");
					if (exisPmId != null && exisPmId.trim().length() > 0)
					{
						// reply messages to user is from the existing message
						privateMessage = jforumPrivateMessageService.newPrivateMessageReplyDraft(Integer.parseInt(exisPmId));
					}
					else
					{
						privateMessage = jforumPrivateMessageService.newPrivateMessage(fromSakaiUserId);
					}
					privateMessage.setType(org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType());
				}
				else
				{
					privateMessage = jforumPrivateMessageService.newPrivateMessage(fromSakaiUserId);
					privateMessage.setType(org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType.DRAFT.getType());
				}
			}
			
			/*if (toUsers != null)
			{
				if (toUsers.length == 1)
				{
					toSakaiUserIds.add(toUsers[0].getSakaiUserId());
				}
				else
				{
					for (int i = 0; i < toUsers.length; i++)
					{
						toSakaiUserIds.add(toUsers[i].getSakaiUserId());
					}
				}
			}*/
			
			if (privateMessage.getType() == org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
			{
				if (privateMessage.getDraftToUserIds().size() != 1)
				{
					for (org.etudes.api.app.jforum.User user : toUsers)
					{
						toSakaiUserIds.add(user.getSakaiUserId());
					}
				}
			}
			else
			{
				for (org.etudes.api.app.jforum.User user : toUsers)
				{
					toSakaiUserIds.add(user.getSakaiUserId());
				}
			}
									
			privateMessage.setContext(ToolManager.getCurrentPlacement().getContext());
			
			if ((this.request.getParameter("high_priority_pm") != null) && (this.request.getIntParameter("high_priority_pm") == 1))
			{
				privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_HIGH);
			}
			else
			{
				privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_GENERAL);
			}
			
			org.etudes.api.app.jforum.Post post = privateMessage.getPost();
			
			post.setTime(new Date());
			
			post.setSubject(JForum.getRequest().getParameter("subject"));
					
			post.setBbCodeEnabled(this.request.getParameter("disable_bbcode") != null ? false : true);
			post.setSmiliesEnabled(this.request.getParameter("disable_smilies") != null ? false : true);
			post.setSignatureEnabled(this.request.getParameter("attach_sig") != null ? true : false);
			
			String message = this.request.getParameter("message");
			message = HtmlHelper.clean(message, true);
			post.setText(message);
			
			//process post attachments 
			processPostAttachments(jforumPrivateMessageService, privateMessage);
			
			//process attachments in the edit mode
			if (pmDraftId != null && exispmDraftId > 0)
			{
				editPostAttachments(post);
			}
			
			try
			{
				if (pmDraftId != null && exispmDraftId > 0)
				{
					// reply message drafts can have bcc users
					jforumPrivateMessageService.modifyPrivateMessageWithAttachments(privateMessage, toSakaiUserIds, getRequestBccSakaiUserIds());
				}
				else
				{
					if (privateMessage.getType() == org.etudes.api.app.jforum.PrivateMessage.PrivateMessageType.DRAFT_REPLY.getType())
					{
						// reply messages to user is from the existing message
						jforumPrivateMessageService.savePrivateMessageWithAttachments(privateMessage, null, getRequestBccSakaiUserIds());
					}
					else
					{
						// reply messages saved as drafts can have bcc users
						jforumPrivateMessageService.savePrivateMessageWithAttachments(privateMessage, toSakaiUserIds, getRequestBccSakaiUserIds());
					}
				}
			}
			catch (JForumAccessException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				throw e;
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				throw e;
			}
			catch (JForumAttachmentBadExtensionException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				throw e;
			}
			
			/*
			this.setTemplateName(TemplateKeys.PM_SENDSAVE);

			this.context.put("message", I18n.getMessage("PrivateMessage.messageSaved", new String[] { this.request.getContextPath()
					+ "/pm/inbox/date/d" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));
			*/
			try
			{
				// redirect to inbox
				this.drafts();
			}
			catch (Exception e)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn(e.toString(), e);
				}
			}
		}		
	}
	
	/**
	 * Sends private message
	 *
	 */
	protected void sendPrivateMessage() throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		String sId = this.request.getParameter("toUserId");
		String toUsername = this.request.getParameter("toUsername");
		
		
		if (sId == null || sId.trim().equals(""))
		{

			String toUserIds[] = (String[]) this.request.getObjectParameter("toUsername" + "ParamValues");
			if (toUserIds != null)
			{
				
				sendPrivateMessageWithAttachments();
			}
			else
			{
				int toUserId = Integer.parseInt(toUsername);
				if (toUserId != -1)
				{
					sendPrivateMessageWithAttachments();
				}
			}
		}
		else
		{
			//quote
			int toUserId = Integer.parseInt(sId);
			if (toUserId != -1)
			{
				try
				{
					replyToPrivateMessage();
				}
				catch (Exception e)
				{
					return;
				}
			}
		}
		
		/*
		this.setTemplateName(TemplateKeys.PM_SENDSAVE);

		this.context.put("message", I18n.getMessage("PrivateMessage.messageSent", new String[] { this.request.getContextPath()
				+ "/pm/inbox/date/d" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));
		*/
		try
		{
			// redirect to inbox
			this.inbox();
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn(e.toString(), e);
			}
		}		
	}
	
	/**
	 * Send private message with attachments
	 * 
	 * @throws JForumAccessException
	 * @throws JForumAttachmentOverQuotaException
	 * @throws JForumAttachmentBadExtensionException
	 */
	protected void sendPrivateMessageWithAttachments() throws JForumAccessException, JForumAttachmentOverQuotaException, JForumAttachmentBadExtensionException
	{
		String sId = this.request.getParameter("toUserId");
		String toUsername = this.request.getParameter("toUsername");
		
		int[] toJFUserIds = null;
		
		if (sId == null || sId.trim().equals(""))
		{
			String toUserIds[] = (String[]) this.request.getObjectParameter("toUsername" + "ParamValues");
			if (toUserIds != null)
			{
				toJFUserIds = new int[toUserIds.length];
				
				for (int i = 0; i < toUserIds.length; i++)
				{
					if (toUserIds[i] != null && toUserIds[i].trim().length() > 0)
					{
						toJFUserIds[i] = Integer.parseInt(toUserIds[i]);
					}
				}
			}
			else
			{
				toJFUserIds = new int[1];
				
				toJFUserIds[0] = Integer.parseInt(toUsername);
				/*try
				{
					User usr = DataAccessDriver.getInstance().newUserDAO().selectById(toJFUserIds[0]);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				//JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
				
				//org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
				
			}
		}
		JForumPrivateMessageService jforumPrivateMessageService = (JForumPrivateMessageService)ComponentManager.get("org.etudes.api.app.jforum.JForumPrivateMessageService");
		
		if (jforumPrivateMessageService == null) return;
		
		//User fromUser = null;
		JForumUserService jforumUserService = (JForumUserService)ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User fromUser = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
		//User[] toUsers = null;
		org.etudes.api.app.jforum.User[] toUsers = null;
		try
		{
			toUsers =  new org.etudes.api.app.jforum.User[toJFUserIds.length];
			
			//fromUser = DataAccessDriver.getInstance().newUserDAO().selectById(SessionFacade.getUserSession().getUserId());
			
			for (int i = 0; i < toJFUserIds.length; i++)
			{
				//toUsers[i] = DataAccessDriver.getInstance().newUserDAO().selectById(toJFUserIds[i]);
				toUsers[i] = jforumUserService.getByUserId(toJFUserIds[i]);
			}
			
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn(e.toString(), e);
			}
		}
		String fromSakaiUserId = fromUser.getSakaiUserId();
		
		org.etudes.api.app.jforum.PrivateMessage privateMessage = null;
		
		// send draft as private message
		String pmDraftId = this.request.getParameter("pm_draft_id");
		int exispmDraftId = 0;
		if (pmDraftId != null && pmDraftId.trim().length() > 0)
		{
			try
			{
				exispmDraftId = Integer.parseInt(pmDraftId);
			}
			catch (NumberFormatException e)
			{			
			}
			
			if (exispmDraftId > 0)
			{
				privateMessage = jforumPrivateMessageService.getPrivateMessageDraft(exispmDraftId);
			}
		}
		
		List<String> toSakaiUserIds =  new ArrayList<String>();
		if (toUsers.length == 1)
		{
			if (privateMessage != null)
			{
				privateMessage.setFromUser(fromUser);
				privateMessage.setToUser(toUsers[0]);
			}
			else
			{
				privateMessage = jforumPrivateMessageService.newPrivateMessage(fromSakaiUserId, toUsers[0].getSakaiUserId());
			}
			toSakaiUserIds.add(toUsers[0].getSakaiUserId());
		}
		else
		{
			if (privateMessage != null)
			{
				
			}
			else
			{
				privateMessage = jforumPrivateMessageService.newPrivateMessage(fromSakaiUserId);
			}
			
			for (int i = 0; i < toUsers.length; i++)
			{
				toSakaiUserIds.add(toUsers[i].getSakaiUserId());
			}
		}
		
		privateMessage.setContext(ToolManager.getCurrentPlacement().getContext());
		
		if ((this.request.getParameter("high_priority_pm") != null) && (this.request.getIntParameter("high_priority_pm") == 1))
		{
			privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_HIGH);
		}
		else
		{
			privateMessage.setPriority(org.etudes.api.app.jforum.PrivateMessage.PRIORITY_GENERAL);
		}
		
		org.etudes.api.app.jforum.Post post = privateMessage.getPost();
		
		post.setTime(new Date());
		
		post.setSubject(JForum.getRequest().getParameter("subject"));
				
		post.setBbCodeEnabled(this.request.getParameter("disable_bbcode") != null ? false : true);
		post.setSmiliesEnabled(this.request.getParameter("disable_smilies") != null ? false : true);
		post.setSignatureEnabled(this.request.getParameter("attach_sig") != null ? true : false);
		
		String message = this.request.getParameter("message");
		message = HtmlHelper.clean(message, true);
		post.setText(message);
		/*String modMessage = cleanMessage(message);
		post.setText(modMessage);		
		
		post.setHtmlEnabled(this.request.getParameter("disable_html") != null);
		
		if (post.isHtmlEnabled())
		{
			post.setText(SafeHtml.makeSafe(message));
		}
		else
		{
			post.setText(message);
		}*/
				
		//process post attachments
		processPostAttachments(jforumPrivateMessageService, privateMessage);
		
		//process draft attachments
		if (pmDraftId != null && exispmDraftId > 0)
		{
			editPostAttachments(post);
		}	
		
		if (toSakaiUserIds.size() == 1)
		{
			//jforumPrivateMessageService.sendPrivateMessage(privateMessage);
			try
			{
				
				if (pmDraftId != null && exispmDraftId > 0)
				{
					// only reply drafts can have bcc users
					List<String> bccSakaiUserIds =  new ArrayList<String>();
					if (getRequestBccSakaiUserIds() != null)
					{
						bccSakaiUserIds.addAll(getRequestBccSakaiUserIds());
					}
					
					jforumPrivateMessageService.sendDraftedPrivateMessageWithAttachments(privateMessage, bccSakaiUserIds);
				}
				else
				{
					jforumPrivateMessageService.sendPrivateMessageWithAttachments(privateMessage);
				}
			}
			catch (JForumAccessException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				//this.send();
				throw e;
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				//this.send();
				throw e;
			}
			catch (JForumAttachmentBadExtensionException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				//this.send();
				throw e;
			}
		}
		else
		{
			try
			{
				if (pmDraftId != null && exispmDraftId > 0)
				{
					// only reply drafts can have bcc users
					List<String> bccSakaiUserIds =  new ArrayList<String>();
					if (getRequestBccSakaiUserIds() != null)
					{
						bccSakaiUserIds.addAll(getRequestBccSakaiUserIds());
					}
					
					jforumPrivateMessageService.sendDraftedPrivateMessageWithAttachments(privateMessage, toSakaiUserIds, bccSakaiUserIds);
				}
				else
				{
					jforumPrivateMessageService.sendPrivateMessageWithAttachments(privateMessage, toSakaiUserIds);
				}
			}
			catch (JForumAccessException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				//this.send();
				throw e;
			}
			catch (JForumAttachmentOverQuotaException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				//this.send();
				throw e;
			}
			catch (JForumAttachmentBadExtensionException e)
			{
				JForum.enableCancelCommit();
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", privateMessage.getPost());
				this.context.put("pm", privateMessage);
				//this.send();
				throw e;
			}
		}
	}
	
	/**
	 * sorts the PM list
	 * 
	 * @param pmList	THe PM List
	 */
	protected void sortPrivateMessageList(List<org.etudes.api.app.jforum.PrivateMessage> pmList, final boolean fromUser)
	{
		String sortColumn = this.request.getParameter("sort_column");
		String sortDirection = this.request.getParameter("sort_direction");
		
		PrivateMessageSort pmSort = PrivateMessageSort.date_d;
		
		if (sortColumn != null && sortDirection != null)
		{
			if (sortColumn.equals("name") && sortDirection.equals("a"))
			{
				pmSort = PrivateMessageSort.last_name_a;
			}
			else if (sortColumn.equals("name") && sortDirection.equals("d"))
			{
				pmSort = PrivateMessageSort.last_name_d;
			}
			else if (sortColumn.equals("date") && sortDirection.equals("a"))
			{
				pmSort = PrivateMessageSort.date_a;
			}
			else if (sortColumn.equals("date") && sortDirection.equals("d"))
			{
				pmSort = PrivateMessageSort.date_d;
			}			
		} 
		else 
		{
			sortColumn = "date";
			sortDirection = "d";
		}
		
		applySort(pmList, pmSort, fromUser);
		
		this.context.put("sort_column", sortColumn);
		this.context.put("sort_direction", sortDirection);
	}
	
	/** 
	 * Removes html comments and extra spaces, escapes javascript tags, adds anchor tags
	 * 
	 * @param message	Message
	 * 
	 * @return	Cleaned message with removed html comments and extra spaces, escapes javascript tags, adds anchor tags
	 */
	/*protected String cleanMessage(String message)
	{
		if (message == null)
		{
			return null;
		}
		
		String formatMessage = message;
		
		formatMessage = SafeHtml.escapeJavascript(formatMessage);
		
		// strip html comments
		//formatMessage = SafeHtml.stripHTMLComments(formatMessage);
		
		// strip excess spaces
		//formatMessage = SafeHtml.removeExcessSpaces(formatMessage);
		
		// add target to anchor tag
		//formatMessage = SafeHtml.addAnchorTarget(formatMessage);
		
		return formatMessage;
	}*/
}
