/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/view/forum/AjaxAction.java $ 
 * $Id: AjaxAction.java 12262 2015-12-11 22:32:59Z murthyt $ 
 ***********************************************************************************
 * Code changed after November 18, 2009 Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 Etudes, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 
 * Portions completed before November 18, 2009 (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the 
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
 * 
 * Created on 09/08/2007 09:31:17
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package org.etudes.jforum.view.forum;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.JForumAccessException;
import org.etudes.api.app.jforum.JForumCategoryService;
import org.etudes.api.app.jforum.JForumPostService;
import org.etudes.api.app.jforum.JForumSecurityService;
import org.etudes.api.app.jforum.JForumUserService;
import org.etudes.api.app.jforum.Post;
import org.etudes.jforum.Command;
import org.etudes.jforum.util.I18n;
import org.etudes.jforum.util.preferences.TemplateKeys;
import org.etudes.jforum.util.user.JForumUserUtil;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.cover.UserDirectoryService;


/**
 * @author Rafael Steil
 */
public class AjaxAction extends Command
{
	private static Log logger = LogFactory.getLog(AjaxAction.class);
	
	/**
	 * Add post like
	 */
	public void addUserPostLike()
	{
		String postIdparam = this.request.getParameter("post_id");
		
		if (postIdparam == null || postIdparam.trim().length() == 0)
		{
			return;
		}
		
		int postId = 0;
		
		try
		{
			postId = Integer.parseInt(postIdparam);
		}
		catch (NumberFormatException e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("Error while parsing post id : "+ postIdparam, e);
			}
			return;
		}
		
		JForumPostService jForumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		
		try
		{
			jForumPostService.addPostLike(postId, UserDirectoryService.getCurrentUser().getId());
		}
		catch (JForumAccessException e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn(e.toString(), e);
			}
			// return;
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("Error while adding post like.", e);
			}
			// return;
		}
		
		Post post = jForumPostService.getPost(postId);
		this.context.put("post", post);
		this.setTemplateName(TemplateKeys.AJAX_ADD_POST_USER_LIKE);
	}
	
	/**
	 * @see net.jforum.Command#list()
	 */
	public void list()
	{
		this.ignoreAction();
	}
	
	/**
	 * Fetches BCC users to show for private message reply/quote
	 */
	public void loadBccUsers()
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		boolean isParticipant = JForumUserUtil.isJForumParticipant(UserDirectoryService.getCurrentUser().getId());
		
		if (!isParticipant) {
			if (!isfacilitator )
			{
				this.context.put("users", new ArrayList<org.etudes.api.app.jforum.User>());
				return;
			}
		}
		
		this.context.put("users", getBccUsers());
		this.setTemplateName(TemplateKeys.PM_SHOW_BCC_USER);
	}
	
	/**
	 * preview PM
	 */
	public void previewPM()
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		boolean isParticipant = JForumUserUtil.isJForumParticipant(UserDirectoryService.getCurrentUser().getId());
		
		JForumPostService jforumPostService = (JForumPostService)ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		
		if (jforumPostService == null)
		{
			return;
		}
		
		org.etudes.api.app.jforum.Post post = jforumPostService.newPost();
		
		//PrivateMessage pm = new PrivateMessage();
		//Post post = new Post();
		
		if (!isParticipant) {
			if (!isfacilitator )
			{
				//PrivateMessage pm = new PrivateMessage();
				//Post post = new Post();
				post.setText("");
				post.setSubject("");
				
				//pm.setPost(post);
				return;
			}
		}		
		
		//post.setText(this.request.getParameter("text"));
		//post.setSubject(this.request.getParameter("subject"));
		//post.setHtmlEnabled("true".equals(this.request.getParameter("html")));
		//post.setBbCodeEnabled("true".equals(this.request.getParameter("bbcode")));
		//post.setSmiliesEnabled("true".equals(this.request.getParameter("smilies")));
		
		/*if (post.isHtmlEnabled()) {
			post.setText(new SafeHtml().makeSafe(post.getText()));
		}*/
		
		/*post = PostCommon.preparePostForDisplay(post);
		post.setSubject(StringEscapeUtils.escapeJavaScript(post.getSubject()));
		post.setText(StringEscapeUtils.escapeJavaScript(post.getText()));
		pm.setPost(post);*/
		
		post.setSubject(this.request.getParameter("subject"));
		post.setText(this.request.getParameter("text"));
		jforumPostService.previewPostForDisplay(post);

		this.context.put("post", post);
		
		this.setTemplateName(TemplateKeys.AJAX_PREVIEW_PM);
		//this.context.put("postPreview", pm);
	}
	
	/**
	 * Sends a test message
	 * @param sender The sender's email address
	 * @param host the smtp host
	 * @param auth if need authorization or not
	 * @param username the smtp server username, if auth is needed
	 * @param password the smtp server password, if auth is needed
	 * @param to the recipient
	 * @return The status message
	 *//*
	public void sendTestMail()
	{
		String sender = this.request.getParameter("sender");
		String host = this.request.getParameter("host");
		String port = this.request.getParameter("port");
		String auth = this.request.getParameter("auth");
		String ssl = this.request.getParameter("ssl");
		String username = this.request.getParameter("username");
		String password = this.request.getParameter("password");
		String to = this.request.getParameter("to");
		
		// Save the current values
		String originalHost = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_HOST);
		String originalAuth = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_AUTH);
		String originalUsername = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_USERNAME);
		String originalPassword = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_PASSWORD);
		String originalSender = SystemGlobals.getValue(ConfigKeys.MAIL_SENDER);
		String originalSSL = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_SSL);
		String originalPort = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_PORT);
		
		// Now put the new ones
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_HOST, host);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_AUTH, auth);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_USERNAME, username);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PASSWORD, password);
		SystemGlobals.setValue(ConfigKeys.MAIL_SENDER, sender);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_SSL, ssl);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PORT, port);
		
		String status = "OK";
		
		// Send the test mail
		class TestSpammer extends Spammer {
			public TestSpammer(String to) {
				List l = new ArrayList();
				
				User user = new User();
				user.setEmail(to);
				
				l.add(user);
				
				this.setUsers(l);
				
				this.setTemplateParams(new SimpleHash());
				this.prepareMessage("JForum Test Mail", null);
			}
			
			protected String processTemplate() throws Exception {
				return ("Test mail from JForum Admin Panel. Sent at " + new Date());
			}
			
			protected void createTemplate(String messageFile) throws Exception {}
		}
		
		Spammer s = new TestSpammer(to);
		
		try {
			s.dispatchMessages();
		}
		catch (Exception e) {
			status = StringEscapeUtils.escapeJavaScript(e.toString());
			logger.error(e.toString(), e);
		}
		finally {
			// Restore the original values
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_HOST, originalHost);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_AUTH, originalAuth);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_USERNAME, originalUsername);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PASSWORD, originalPassword);
			SystemGlobals.setValue(ConfigKeys.MAIL_SENDER, originalSender);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_SSL, originalSSL);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PORT, originalPort);
		}
		
		this.setTemplateName(TemplateKeys.AJAX_TEST_MAIL);
		this.context.put("status", status);
	}
	
	public void isPostIndexed()
	{
		int postId = this.request.getIntParameter("post_id");

		this.setTemplateName(TemplateKeys.AJAX_IS_POST_INDEXED);
		
		LuceneManager manager = (LuceneManager)SearchFacade.manager();
		Document doc = manager.luceneSearch().findDocumentByPostId(postId);
		
		this.context.put("doc", doc);
	}
	
	public void loadPostContents()
	{
		int postId = this.request.getIntParameter("id");
		PostDAO dao = DataAccessDriver.getInstance().newPostDAO();
		Post post = dao.selectById(postId);
		this.setTemplateName(TemplateKeys.AJAX_LOAD_POST);
		this.context.put("post", post);
	}
	
	public void savePost()
	{
		PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		Post post = postDao.selectById(this.request.getIntParameter("id"));
		
		String originalMessage = post.getText();
		
		if (!PostCommon.canEditPost(post)) {
			post = PostCommon.preparePostForDisplay(post);
		}
		else {
			post.setText(this.request.getParameter("value"));
			postDao.update(post);
			SearchFacade.update(post);
			post = PostCommon.preparePostForDisplay(post);
		}
		
		boolean isModerator = SecurityRepository.canAccess(SecurityConstants.PERM_MODERATION_POST_EDIT);
		
		if (SystemGlobals.getBoolValue(ConfigKeys.MODERATION_LOGGING_ENABLED)
				&& isModerator && post.getUserId() != SessionFacade.getUserSession().getUserId()) {
			ModerationHelper helper = new ModerationHelper();
			
			this.request.addParameter("log_original_message", originalMessage);
			this.request.addParameter("post_id", String.valueOf(post.getId()));
			this.request.addParameter("topic_id", String.valueOf(post.getTopicId()));
			
			ModerationLog log = helper.buildModerationLogFromRequest();
			log.getPosterUser().setId(post.getUserId());
			
			helper.saveModerationLog(log);
		}
		
		if (SystemGlobals.getBoolValue(ConfigKeys.POSTS_CACHE_ENABLED)) {
			PostRepository.update(post.getTopicId(), PostCommon.preparePostForDisplay(post));
		}
		
		this.setTemplateName(TemplateKeys.AJAX_LOAD_POST);
		this.context.put("post", post);
	}*/
	
	public void previewPost()
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		boolean isParticipant = JForumUserUtil.isJForumParticipant(UserDirectoryService.getCurrentUser().getId());
		
		JForumPostService jforumPostService = (JForumPostService)ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		
		if (jforumPostService == null)
		{
			return;
		}
		
		org.etudes.api.app.jforum.Post post = jforumPostService.newPost();
		
		//Post post = new Post();
		
		if (!isParticipant) {
			if (!isfacilitator )
			{
				post.setText("");
				post.setSubject("");
				return;
			}
		}	
		
		//post.setText(this.request.getParameter("text"));
		//post.setSubject(this.request.getParameter("subject"));
		//post.setHtmlEnabled("true".equals(this.request.getParameter("html")));
		//post.setBbCodeEnabled("true".equals(this.request.getParameter("bbcode")));
		//post.setSmiliesEnabled("true".equals(this.request.getParameter("smilies")));
		
		/*if (post.isHtmlEnabled()) {
			post.setText(new SafeHtml().makeSafe(post.getText()));
		}*/
		
		/*post = PostCommon.preparePostForDisplay(post);
		post.setSubject(StringEscapeUtils.escapeJavaScript(post.getSubject()));
		post.setText(StringEscapeUtils.escapeJavaScript(post.getText()));*/
		
		
		post.setSubject(this.request.getParameter("subject"));
		post.setText(this.request.getParameter("text"));
		jforumPostService.previewPostForDisplay(post);

		this.setTemplateName(TemplateKeys.AJAX_PREVIEW_POST);
		
		this.context.put("post", post);
	}
	
	/**
	 * Remove post like
	 */
	public void removeUserPostLike()
	{
		String postIdparam = this.request.getParameter("post_id");
		
		if (postIdparam == null || postIdparam.trim().length() == 0)
		{
			return;
		}
		
		int postId = 0;
		
		try
		{
			postId = Integer.parseInt(postIdparam);
		}
		catch (NumberFormatException e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("Error while parsing post id : "+ postIdparam, e);
			}
			return;
		}
		
		JForumPostService jForumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		
		try
		{
			jForumPostService.removePostLike(postId, UserDirectoryService.getCurrentUser().getId());
		}
		catch (JForumAccessException e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn(e.toString(), e);
			}
			// return;
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("Error while removing post like.", e);
			}
			// return;
		}
		
		Post post = jForumPostService.getPost(postId);
		this.context.put("post", post);
		this.setTemplateName(TemplateKeys.AJAX_REMOVE_POST_USER_LIKE);
	}
	
	/**
	 * validate category grading option
	 */
	public void validateCategoryGrading()
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		this.setTemplateName(TemplateKeys.AJAX_GRADING_CATEGORY_VALIDATION);
		
		if (!isfacilitator) {
			this.context.put("errorValidation", I18n.getMessage("User.NotAuthorized"));
			return;
		}
		
		int categoryId = this.request.getIntParameter("categories_id");
		
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Category c = jforumCategoryService.getCategory(categoryId);
		
		this.context.put("c", c);
		
		/*CategoryDAO cm = DataAccessDriver.getInstance().newCategoryDAO();
		
		try
		{
			Category c = cm.selectById(categoryId);
			this.context.put("c", c);
		}
		catch (Exception e)
		{
			if (logger.isErrorEnabled()) logger.warn(e);
		}*/
	}
	
	/**
	 * Gets site users(observers are removed from the list)
	 * 
	 * @return	Site users
	 */
	protected List<org.etudes.api.app.jforum.User> getBccUsers()
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
			
			return new ArrayList<org.etudes.api.app.jforum.User>();
		}
	}
}
