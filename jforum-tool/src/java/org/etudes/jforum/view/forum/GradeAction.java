/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/view/forum/GradeAction.java $ 
 * $Id: GradeAction.java 12164 2015-11-30 22:37:10Z murthyt $ 
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
 ***********************************************************************************/

package org.etudes.jforum.view.forum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.JForumCategoryService;
import org.etudes.api.app.jforum.JForumForumService;
import org.etudes.api.app.jforum.JForumGradeService;
import org.etudes.api.app.jforum.JForumPostService;
import org.etudes.api.app.jforum.JForumUserService;
import org.etudes.api.app.jforum.User;
import org.etudes.jforum.Command;
import org.etudes.jforum.dao.DataAccessDriver;
import org.etudes.jforum.dao.EvaluationDAO;
import org.etudes.jforum.dao.EvaluationDAO.EvaluationsSort;
import org.etudes.jforum.entities.Evaluation;
import org.etudes.jforum.entities.Forum;
import org.etudes.jforum.entities.Grade;
import org.etudes.jforum.util.I18n;
import org.etudes.jforum.util.preferences.ConfigKeys;
import org.etudes.jforum.util.preferences.SakaiSystemGlobals;
import org.etudes.jforum.util.preferences.SystemGlobals;
import org.etudes.jforum.util.preferences.TemplateKeys;
import org.etudes.jforum.util.user.JForumUserUtil;
import org.etudes.jforum.view.forum.common.ViewCommon;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.cover.UserDirectoryService;

/**
 * @author Murthy Tanniru
 */
public class GradeAction extends Command
{

	private static Log logger = LogFactory.getLog(GradeAction.class);

	/**
	 * assign zero to non submitters
	 * 
	 * @throws Exception
	 */
	public void assignZeroCategoryNonSubmitters() throws Exception
	{
		int categoryId = this.request.getIntParameter("category_id");
		
		if (categoryId > 0)
		{
			JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
			jforumCategoryService.assignZeroCategoryNonSubmitters(categoryId, UserDirectoryService.getCurrentUser().getId());
		}
		
		this.evalCategoryList();
	}
	
	/**
	 * assign zero to non submitters
	 * 
	 * @throws Exception
	 */
	public void assignZeroForumNonSubmitters() throws Exception
	{
		int forumId = this.request.getIntParameter("forum_id");
		
		if (forumId > 0)
		{
			JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
			jforumForumService.assignZeroForumNonSubmitters(forumId, UserDirectoryService.getCurrentUser().getId());
		}
		
		this.evalForumList();
	}
	
	/**
	 * assign zero to non submitters
	 * 
	 * @throws Exception
	 */
	public void assignZeroTopicNonSubmitters() throws Exception
	{
		int topicId = this.request.getIntParameter("topic_id");
		
		if (topicId > 0)
		{
			JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
			jforumPostService.assignZeroTopicNonSubmitters(topicId, UserDirectoryService.getCurrentUser().getId());
		}
		
		this.evalTopicList();
	}

	/**
	 * get category evaluations
	 * @throws Exception
	 */
	public void evalCategoryList() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) 
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		
		//update memebers info
		//JForumUserUtil.updateMembersInfo(false);
				
		int categoryId = this.request.getIntParameter("category_id");
		String sortColumn = this.request.getParameter("sort_column");
		String sortDirection = this.request.getParameter("sort_direction");
		
		//EvaluationDAO.EvaluationsSort evalSort = EvaluationDAO.EvaluationsSort.last_name_a;
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;
		
		if (sortColumn != null && sortDirection != null)
		{
			if (sortColumn.equals("name") && sortDirection.equals("a"))
			{
				//evalSort = EvaluationDAO.EvaluationsSort.last_name_a;
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;
			}
			else if (sortColumn.equals("name") && sortDirection.equals("d"))
			{
				//evalSort = EvaluationDAO.EvaluationsSort.last_name_d;
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_d;
			}
			else if (sortColumn.equals("posts") && sortDirection.equals("a"))
			{
				//evalSort = EvaluationDAO.EvaluationsSort.total_posts_a;
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.total_posts_a;
			}
			else if (sortColumn.equals("posts") && sortDirection.equals("d"))
			{
				//evalSort = EvaluationDAO.EvaluationsSort.total_posts_d;
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.total_posts_d;
			}
			else if (sortColumn.equals("scores") && sortDirection.equals("a"))
			{
				//evalSort = EvaluationDAO.EvaluationsSort.scores_a;
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.scores_a;
			}
			else if (sortColumn.equals("scores") && sortDirection.equals("d"))
			{
				//evalSort = EvaluationDAO.EvaluationsSort.scores_d;
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.scores_d;
			}
			else if (sortColumn.equals("lastpostdate") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_post_date_a;
			}
			else if (sortColumn.equals("lastpostdate") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_post_date_d;
			}
			else if (sortColumn.equals("firstpostdate") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.first_post_date_a;
			}
			else if (sortColumn.equals("firstpostdate") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.first_post_date_d;
			}
			
		}
		else
		{
			sortColumn = "name";
			sortDirection = "a";
		}
		
		//Category category = ForumRepository.getCategory(categoryId);
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);
		
		if (category == null) return;

		this.context.put("category", category);

		this.context.put("facilitator", isfacilitator);
		
		//Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByCategoryId(categoryId);
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByCategoryId(categoryId);
		
		this.context.put("grade", grade);
		
		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("addToGradebook", true);
		}
		
		//get evaluations
		/*
		EvaluationDAO evaluationDao = DataAccessDriver.getInstance().newEvaluationDAO();
		List<Evaluation> evaluations = evaluationDao.selectCategoryEvaluations(categoryId, evalSort);
		*/
		List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getCategoryEvaluationsWithPosts(categoryId, evalSort, UserDirectoryService.getCurrentUser().getId(), true);
		
		
		this.context.put("sort_column", sortColumn);
		this.context.put("sort_direction", sortDirection);
		this.context.put("evaluations", evaluations);
		
		this.context.put("nowDate", new Date());
		
		//int forumSpecialAccessCount = DataAccessDriver.getInstance().newSpecialAccessDAO().selectCategoryForumSpecialAccessCount(category.getId());
		//this.context.put("forumSpecialAccessCount", forumSpecialAccessCount);
		
		this.setTemplateName(TemplateKeys.GRADE_EVAL_CATEGORY);
		
	}
	
	/**
	 * get forum evaluations
	 * @throws Exception
	 */
	public void evalForumList() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		// update members info
		//JForumUserUtil.updateMembersInfo(false);

		int forumId = this.request.getIntParameter("forum_id");
		String sortColumn = this.request.getParameter("sort_column");
		String sortDirection = this.request.getParameter("sort_direction");

		//EvaluationDAO.EvaluationsSort evalSort = EvaluationDAO.EvaluationsSort.last_name_a;
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;

		if (sortColumn != null && sortDirection != null)
		{
			if (sortColumn.equals("name") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;
			}
			else if (sortColumn.equals("name") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_d;
			}
			else if (sortColumn.equals("posts") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.total_posts_a;
			}
			else if (sortColumn.equals("posts") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.total_posts_d;
			}
			else if (sortColumn.equals("scores") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.scores_a;
			}
			else if (sortColumn.equals("scores") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.scores_d;
			}
			else if (sortColumn.equals("grouptitle") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.group_title_a;
			}
			else if (sortColumn.equals("grouptitle") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.group_title_d;
			}
			else if (sortColumn.equals("lastpostdate") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_post_date_a;
			}
			else if (sortColumn.equals("lastpostdate") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_post_date_d;
			}
			else if (sortColumn.equals("firstpostdate") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.first_post_date_a;
			}
			else if (sortColumn.equals("firstpostdate") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.first_post_date_d;
			}

		}
		else
		{
			sortColumn = "name";
			sortDirection = "a";
		}

		//Forum forum = ForumRepository.getForum(forumId);
		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);
		if (forum == null)
		{
			return;
		}

		this.context.put("forum", forum);

		//Category category = ForumRepository.getCategory(forum.getCategoryId());
		org.etudes.api.app.jforum.Category category = forum.getCategory();
		
		this.context.put("category", category);
		this.context.put("facilitator", isfacilitator);

		//Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumId(forum.getId());
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByForumId(forumId);

		this.context.put("grade", grade);

		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
			this.context.put("addToGradebook", true);

		// get evaluations
		//EvaluationDAO evaluationDao = DataAccessDriver.getInstance().newEvaluationDAO();
		//List<Evaluation> evaluations = evaluationDao.selectForumEvaluations(forum.getId(), evalSort);
		List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getForumEvaluationsWithPosts(forumId, evalSort, UserDirectoryService.getCurrentUser().getId(), true);

		// get forum groups
		if (forum.getAccessType() == Forum.ACCESS_GROUPS)
		{
			//getForumGroupUsers(forum, evaluations);
			getForumGroupUsers(forum);

			//sortEvaluationGroups(evalSort, evaluations);
		}

		this.context.put("sort_column", sortColumn);
		this.context.put("sort_direction", sortDirection);
		this.context.put("evaluations", evaluations);
		
		this.context.put("nowDate", new Date());

		// int catSpecialAccessCount =
		// DataAccessDriver.getInstance().newSpecialAccessDAO().selectCategorySpecialAccessCount(category.getId());
		// this.context.put("catSpecialAccessCount", catSpecialAccessCount);

		this.setTemplateName(TemplateKeys.GRADE_EVAL_FORUM);
	}

	
	/**
	 * get topic evaluations
	 * @throws Exception
	 */
	public void evalTopicList() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		// update memebers info
		//JForumUserUtil.updateMembersInfo(false);

		int topicId = this.request.getIntParameter("topic_id");
		String sortColumn = this.request.getParameter("sort_column");
		String sortDirection = this.request.getParameter("sort_direction");

		// EvaluationDAO.EvaluationsSort evalSort = EvaluationDAO.EvaluationsSort.last_name_a;
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;

		if (sortColumn != null && sortDirection != null)
		{
			if (sortColumn.equals("name") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;
			}
			else if (sortColumn.equals("name") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_d;
			}
			else if (sortColumn.equals("posts") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.total_posts_a;
			}
			else if (sortColumn.equals("posts") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.total_posts_d;
			}
			else if (sortColumn.equals("scores") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.scores_a;
			}
			else if (sortColumn.equals("scores") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.scores_d;
			}
			else if (sortColumn.equals("grouptitle") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.group_title_a;
			}
			else if (sortColumn.equals("grouptitle") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.group_title_d;
			}
			else if (sortColumn.equals("lastpostdate") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_post_date_a;
			}
			else if (sortColumn.equals("lastpostdate") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_post_date_d;
			}
			else if (sortColumn.equals("firstpostdate") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.first_post_date_a;
			}
			else if (sortColumn.equals("firstpostdate") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.first_post_date_d;
			}
		}
		else
		{
			sortColumn = "name";
			sortDirection = "a";
		}

		//TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
		//Topic topic = tm.selectById(topicId);

		//List<SpecialAccess> specialAccessList = DataAccessDriver.getInstance().newSpecialAccessDAO().selectByTopic(topic.getForumId(), topic.getId());
		//topic.setSpecialAccessList(specialAccessList);
		
		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		
		org.etudes.api.app.jforum.Topic topic = jforumPostService.getTopic(topicId);

		// The topic exists?
		if (topic == null)
		{
			if (logger.isErrorEnabled())
			{
				logger.error("topic is not available with id = "+ topicId);
			}
			this.topicNotFound();
			return;
		}

		//Forum forum = ForumRepository.getForum(topic.getForumId());
		org.etudes.api.app.jforum.Forum forum = topic.getForum();
		if (forum == null)
		{
			return;
		}
		
		this.context.put("forum", forum);
		
		org.etudes.api.app.jforum.Category category = forum.getCategory();

		// Category category = ForumRepository.getCategory(forum.getCategoryId());
		this.context.put("category", category);

		this.context.put("topic", topic);

		this.context.put("facilitator", isfacilitator);

		//Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumTopicId(topic.getForumId(), topicId);
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByForumTopicId(topic.getForumId(), topicId);

		this.context.put("grade", grade);
		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("addToGradebook", true);
		}

		// get evaluations
		//EvaluationDAO evaluationDao = DataAccessDriver.getInstance().newEvaluationDAO();
		//List<Evaluation> evaluations = evaluationDao.selectTopicEvaluations(forum.getId(), topicId, evalSort);
		List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getTopicEvaluationsWithPosts(topicId, evalSort, UserDirectoryService.getCurrentUser().getId(), true);

		if (forum.getAccessType() == Forum.ACCESS_GROUPS)
		{
			//getForumGroupUsers(forum, evaluations);

			//sortEvaluationGroups(evalSort, evaluations);
			
			getForumGroupUsers(forum);
		}

		this.context.put("nowDate", new Date());
		this.context.put("sort_column", sortColumn);
		this.context.put("sort_direction", sortDirection);
		this.context.put("evaluations", evaluations);
		this.setTemplateName(TemplateKeys.GRADE_EVAL_TOPIC);
	}

	/**
	 * evaluate category
	 * 
	 * @throws Exception
	 */
	public void evaluateCategory() throws Exception
	{
		//evaluateUsers();
		evaluateCategoryGrades();
		
		this.evalCategoryList();
	}

	/**
	 * evaluate user category
	 * @throws Exception
	 */
	public void evaluateCategoryUser() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("errorMessage", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED_POP_UP);
			return;
		}

		int categoryId = this.request.getIntParameter("category_id");
		int userId = this.request.getIntParameter("user_id");
		String release = this.request.getParameter("release");
		boolean releaseGrade = false;

		if ((release != null) && (Integer.parseInt(release) == 1))
		{
			releaseGrade = true;
		}

		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);
		
		if (!category.isGradable())
		{
			return;
		}

		this.context.put("category", category);
		this.context.put("userId", userId);

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);
		
		org.etudes.api.app.jforum.Evaluation evaluation = null;

		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		evaluation = jforumGradeService.getUserCategoryEvaluation(categoryId, user.getSakaiUserId());
		
		if (evaluation == null)
		{
			org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByCategoryId(categoryId);
			evaluation = jforumGradeService.newEvaluation(grade.getId(), UserDirectoryService.getCurrentUser().getId(), user.getSakaiUserId());			
		}
		
		Float score = null;
		try
		{
			score = Float.parseFloat(this.request.getParameter("score"));
			// if (score.floatValue() < 0) score = Float.valueOf(0.0f);
			if (score.floatValue() > 1000)
				score = Float.valueOf(1000.0f);
			score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
		}
		catch (NumberFormatException ne)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("evaluateCategoryUser() : " + ne);
			}
		}
		
		String comments = this.request.getParameter("comments");
		
		evaluation.setReleased(releaseGrade);
		evaluation.setScore(score);
		evaluation.setComments(comments);
		evaluation.setEvaluatedBySakaiUserId(UserDirectoryService.getCurrentUser().getId());

		jforumGradeService.addModifyUserEvaluation(evaluation);
		
		this.context.put("updatesucess", true);

		this.showEvalCategoryUser();
	}
	
	/**
	 * evaluate forum
	 * 
	 * @throws Exception
	 */
	public void evaluateForum() throws Exception
	{
		 //evaluateUsers();
		evaluateForumGrades();

		this.evalForumList();
	}
	
	/**
	 * evaluate user forum
	 * 
	 * @throws Exception
	 */
	public void evaluateForumUser() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("errorMessage", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED_POP_UP);
			return;
		}

		int forumId = this.request.getIntParameter("forum_id");
		int userId = this.request.getIntParameter("user_id");

		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);
		
		if (forum == null || forum.getGradeType() != org.etudes.api.app.jforum.Grade.GradeType.FORUM.getType())
		{
			return;
		}

		this.context.put("forum", forum);
		this.context.put("userId", userId);
		
		String release = this.request.getParameter("release");
		
		boolean releaseGrade = false;

		if ((release != null) && (Integer.parseInt(release) == 1))
		{
			releaseGrade = true;
		}

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);
		
		org.etudes.api.app.jforum.Evaluation evaluation = null;

		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		evaluation = jforumGradeService.getUserForumEvaluation(forumId, user.getSakaiUserId());
		
		if (evaluation == null)
		{
			org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByForumId(forumId);
			evaluation = jforumGradeService.newEvaluation(grade.getId(), UserDirectoryService.getCurrentUser().getId(), user.getSakaiUserId());			
		}

		Float score = null;
		try
		{
			score = Float.parseFloat(this.request.getParameter("score"));
			// if (score.floatValue() < 0) score = Float.valueOf(0.0f);
			if (score.floatValue() > 1000)
				score = Float.valueOf(1000.0f);
			score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
		}
		catch (NumberFormatException ne)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("evaluateForumUser() : " + ne);
			}
		}
		
		String comments = this.request.getParameter("comments");
		
		evaluation.setReleased(releaseGrade);
		evaluation.setScore(score);
		evaluation.setComments(comments);
		evaluation.setEvaluatedBySakaiUserId(UserDirectoryService.getCurrentUser().getId());

		jforumGradeService.addModifyUserEvaluation(evaluation);

		this.context.put("updatesucess", true);

		this.showEvalForumUser();
	}	
	
	/**
	 * evaluate topic
	 * 
	 * @throws Exception
	 */
	public void evaluateTopic() throws Exception
	{
		// evaluateUsers();
		evaluateTopicGrades();
		
		this.evalTopicList();
	}
	
	/**
	 * evaluate user topic
	 * @throws Exception
	 */
	public void evaluateTopicUser() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("errorMessage", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED_POP_UP);
			return;
		}

		int topicId = this.request.getIntParameter("topic_id");
		int userId = this.request.getIntParameter("user_id");
		String release = this.request.getParameter("release");
		boolean releaseGrade = false;

		if ((release != null) && (Integer.parseInt(release) == 1))
		{
			releaseGrade = true;
		}

		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		org.etudes.api.app.jforum.Topic topic = jforumPostService.getTopic(topicId);

		if (!topic.isGradeTopic())
		{
			return;
		}
		this.context.put("topic", topic);
		this.context.put("userId", userId);

		this.context.put("forum", topic.getForum());

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);
		
		org.etudes.api.app.jforum.Evaluation evaluation = null;

		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		evaluation = jforumGradeService.getUserTopicEvaluation(topicId, user.getSakaiUserId());
		
		if (evaluation == null)
		{
			org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByForumTopicId(topic.getForumId(), topic.getId());
			evaluation = jforumGradeService.newEvaluation(grade.getId(), UserDirectoryService.getCurrentUser().getId(), user.getSakaiUserId());			
		}

		Float score = null;
		try
		{
			score = Float.parseFloat(this.request.getParameter("score"));
			// if (score.floatValue() < 0) score = Float.valueOf(0.0f);
			if (score.floatValue() > 1000)
				score = Float.valueOf(1000.0f);
			score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
		}
		catch (NumberFormatException ne)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn("evaluateTopicUser() : " + ne);
			}
		}
		
		String comments = this.request.getParameter("comments");
		
		evaluation.setReleased(releaseGrade);
		evaluation.setScore(score);
		evaluation.setComments(comments);
		evaluation.setEvaluatedBySakaiUserId(UserDirectoryService.getCurrentUser().getId());

		jforumGradeService.addModifyUserEvaluation(evaluation);

		this.context.put("updatesucess", true);

		this.showEvalTopicUser();
	}
	
	/**
	 * 
	 * 
	 */
	public void list() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		
		//this.setTemplateName(TemplateKeys.GRADE_EVAL_FORUM);
	}
	
	/**
	 * show user user category evaluation
	 * 
	 * @throws Exception
	 */
	public void showEvalCategoryUser() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		if (!isfacilitator)
		{
			this.context.put("errorMessage", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED_POP_UP);
			return;
		}

		int categoryId = this.request.getIntParameter("category_id");
		int userId = this.request.getIntParameter("user_id");

		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);

		this.context.put("category", category);
		this.context.put("userId", userId);

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);
		
		this.context.put("user", user);
		
		if (!JForumUserUtil.isUserActive(user.getSakaiUserId()))
		{
			this.context.put("message", I18n.getMessage("Evaluation.userInactive"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserCategoryEvaluation(categoryId, user.getSakaiUserId(), true);
				
		if (evaluation != null)
		{
			this.context.put("evaluation", evaluation);
		}

		org.etudes.api.app.jforum.Grade grade = category.getGrade();
		this.context.put("grade", grade);

		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("addToGradebook", true);
		}

		this.context.put("pageTitle", SystemGlobals.getValue(ConfigKeys.FORUM_NAME) + " - " + I18n.getMessage("Grade.gradeCategoryUser"));

		this.setTemplateName(TemplateKeys.GRADE_EVAL_CATEGORY_USER);
	}
	
	
	/**
	 * show user forum evaluation
	 * 
	 * @throws Exception
	 */
	public void showEvalForumUser() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		if (!isfacilitator)
		{
			this.context.put("errorMessage", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED_POP_UP);
			return;
		}

		int forumId = this.request.getIntParameter("forum_id");
		int userId = this.request.getIntParameter("user_id");

		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);
		
		if (forum == null)
		{
			return;
		}

		this.context.put("forum", forum);
		this.context.put("userId", userId);

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);
		
		this.context.put("user", user);
		if (!JForumUserUtil.isUserActive(user.getSakaiUserId()))
		{
			this.context.put("message", I18n.getMessage("Evaluation.userInactive"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		
		org.etudes.api.app.jforum.Category category = forum.getCategory();
		this.context.put("category", category);
		
		// special access user dates
		List<org.etudes.api.app.jforum.SpecialAccess> specialAccessList = null;
		if ((forum != null) && (forum.getAccessDates() != null) && ((forum.getAccessDates().getOpenDate() != null) || (forum.getAccessDates().getDueDate() != null) || (forum.getAccessDates().getAllowUntilDate() != null)))
		{
			specialAccessList = forum.getSpecialAccess();
		}
		
		boolean specialAccessUser = false;
		if (specialAccessList != null)
		{
			for (org.etudes.api.app.jforum.SpecialAccess specialAccess : specialAccessList)
			{
				List<Integer> userIds = specialAccess.getUserIds();
				
				if (userIds == null || userIds.isEmpty())
				{
					continue;
				}
				
				for (Integer saUserId : userIds)
				{
					if (saUserId.intValue() == userId)
					{
						specialAccessUser = true;
						break;
					}
				}
				
				if (specialAccessUser)
				{
					if (specialAccess.getAccessDates() != null)
					{
						if (!specialAccess.isOverrideStartDate())
						{
							specialAccess.getAccessDates().setOpenDate(forum.getAccessDates().getOpenDate());
						}
						
						if (!specialAccess.isOverrideEndDate())
						{
							specialAccess.getAccessDates().setDueDate(forum.getAccessDates().getDueDate());
						}
						
						if (!specialAccess.isOverrideAllowUntilDate())
						{
							specialAccess.getAccessDates().setAllowUntilDate(forum.getAccessDates().getAllowUntilDate());
						}
					}
					this.context.put("userSpecialAccess", specialAccess);
					break;
				}
			}
		}

		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserForumEvaluation(forumId, user.getSakaiUserId(), true);
		
		if (evaluation != null)
		{
			this.context.put("evaluation", evaluation);
		}

		Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumId(forum.getId());
		this.context.put("grade", grade);

		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("addToGradebook", true);
		}

		this.context.put("pageTitle", SystemGlobals.getValue(ConfigKeys.FORUM_NAME) + " - " + I18n.getMessage("Grade.gradeForumUser"));

		this.setTemplateName(TemplateKeys.GRADE_EVAL_FORUM_USER);
	}

		
	/**
	 * show user topic evaluation
	 * 
	 * @throws Exception
	 */
	public void showEvalTopicUser() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		if (!isfacilitator)
		{
			this.context.put("errorMessage", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED_POP_UP);
			return;
		}

		int topicId = this.request.getIntParameter("topic_id");
		int userId = this.request.getIntParameter("user_id");

		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		org.etudes.api.app.jforum.Topic topic = jforumPostService.getTopic(topicId);
		
		this.context.put("topic", topic);
		this.context.put("userId", userId);

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);
		
		this.context.put("user", user);
		if (!JForumUserUtil.isUserActive(user.getSakaiUserId()))
		{
			this.context.put("message", I18n.getMessage("Evaluation.userInactive"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		org.etudes.api.app.jforum.Forum forum = topic.getForum();
		this.context.put("forum", forum);
		
		org.etudes.api.app.jforum.Category category = forum.getCategory();
		this.context.put("category", category);
		
		// special access user dates
		boolean topicDates = false, forumDates = false;
		List<org.etudes.api.app.jforum.SpecialAccess> specialAccessList = null;
		if ((topic.getAccessDates() != null) && ((topic.getAccessDates().getOpenDate() != null) || (topic.getAccessDates().getDueDate() != null) || (topic.getAccessDates().getAllowUntilDate() != null)))
		{
			specialAccessList = topic.getSpecialAccess();
			topicDates = true;
		}
		else if ((forum != null) && (forum.getAccessDates() != null) && ((forum.getAccessDates().getOpenDate() != null) || (forum.getAccessDates().getDueDate() != null) || (forum.getAccessDates().getAllowUntilDate() != null)))
		{
			specialAccessList = forum.getSpecialAccess();
			forumDates = true;
		}
		
		boolean specialAccessUser = false;
		if (specialAccessList != null)
		{
			for (org.etudes.api.app.jforum.SpecialAccess specialAccess : specialAccessList)
			{
				List<Integer> userIds = specialAccess.getUserIds();
				
				if (userIds == null || userIds.isEmpty())
				{
					continue;
				}
				
				for (Integer saUserId : userIds)
				{
					if (saUserId.intValue() == userId)
					{
						specialAccessUser = true;
						break;
					}
				}
				
				if (specialAccessUser)
				{
					if (specialAccess.getAccessDates() != null)
					{
						if (!specialAccess.isOverrideStartDate())
						{
							if (topicDates)
							{
								specialAccess.getAccessDates().setOpenDate(topic.getAccessDates().getOpenDate());
							}
							else if (forumDates)
							{
								specialAccess.getAccessDates().setOpenDate(forum.getAccessDates().getOpenDate());
							}
						}
						
						if (!specialAccess.isOverrideEndDate())
						{
							if (topicDates)
							{
								specialAccess.getAccessDates().setDueDate(topic.getAccessDates().getDueDate());
							}
							else if (forumDates)
							{
								specialAccess.getAccessDates().setDueDate(forum.getAccessDates().getDueDate());
							}
						}
						
						if (!specialAccess.isOverrideAllowUntilDate())
						{
							if (topicDates)
							{
								specialAccess.getAccessDates().setAllowUntilDate(topic.getAccessDates().getAllowUntilDate());
							}
							else if (forumDates)
							{
								specialAccess.getAccessDates().setAllowUntilDate(forum.getAccessDates().getAllowUntilDate());
							}
						}
					}
					this.context.put("userSpecialAccess", specialAccess);
					break;
				}
			}
		}		
		
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserTopicEvaluation(topicId, user.getSakaiUserId(), true);
		
		if (evaluation != null)
		{
			this.context.put("evaluation", evaluation);
		}

		Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumTopicId(topic.getForumId(), topic.getId());
		this.context.put("grade", grade);

		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("addToGradebook", true);
		}

		this.context.put("pageTitle", SystemGlobals.getValue(ConfigKeys.FORUM_NAME) + " - " + I18n.getMessage("Grade.gradeTopicUser"));

		this.setTemplateName(TemplateKeys.GRADE_EVAL_TOPIC_USER);
	}
	
	/**
	 * show user replies for the category
	 */
	public void showUserCategoryReplies() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		int categoryId = this.request.getIntParameter("category_id");
		int userId = this.request.getIntParameter("user_id");
		
		String sortColumn = this.request.getParameter("sort_column");
		String sortDirection = this.request.getParameter("sort_direction");
		
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = getSortEvaluations(sortColumn, sortDirection);

		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);

		this.context.put("category", category);

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		//org.etudes.api.app.jforum.User currentUser = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
		
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		//List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getCategoryEvaluationsWithPosts(categoryId, org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a, UserDirectoryService.getCurrentUser().getId(), false);
		List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getCategoryEvaluationsWithPosts(categoryId, evalSort, UserDirectoryService.getCurrentUser().getId(), false);

		int start = 0;
		String s = this.request.getParameter("start");
		if (s == null || s.trim().equals(""))
		{
			int indexCount = 0;
			boolean foundIndex = false;
			for (org.etudes.api.app.jforum.Evaluation presEval : evaluations)
			{
				indexCount++;
				if (presEval.getUserId() == userId)
				{
					// start = evaluations.indexOf(presEval);
					indexCount--;
					foundIndex = true;
					break;
				}
			}
			if (foundIndex)
			{
				start = indexCount;
			}

			this.context.put("presUserId", userId);
		}
		else
		{
			start = ViewCommon.getStartPage();
			org.etudes.api.app.jforum.Evaluation eval = evaluations.get(start);
			this.context.put("presUserId", eval.getUserId());
			userId = eval.getUserId();
		}
		
		ViewCommon.contextToPagination(start, evaluations.size(), 1);
		
		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);

		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		List<org.etudes.api.app.jforum.Post> posts = jforumPostService.getUserCategoryPosts(categoryId, user.getSakaiUserId(), UserDirectoryService.getCurrentUser().getId());
		
		this.context.put("attachmentsEnabled", true);
		this.context.put("canDownloadAttachments", true);
		this.context.put("posts", posts);
		
		if (sortColumn == null || sortColumn.trim() == "")
		{
			sortColumn = "name";
		}
		
		if (sortDirection == null || sortDirection.trim() == "")
		{
			sortDirection = "a";
		}
		
		this.context.put("sort_column", sortColumn);
		this.context.put("sort_direction", sortDirection);

		if (!JForumUserUtil.isUserActive(user.getSakaiUserId()))
		{
			this.context.put("message", I18n.getMessage("Evaluation.userInactive"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		this.setTemplateName(TemplateKeys.GRADE_VIEW_USER_CATEGORY_REPLIES);
	}
	
	
	/**
	 * show user replies for the forum topics
	 */
	public void showUserForumReplies() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		int forumId = this.request.getIntParameter("forum_id");
		int userId = this.request.getIntParameter("user_id");
		
		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);
		if (forum == null)
		{
			this.context.put("errorMessage", I18n.getMessage("Forum.notFound"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_FORUM);
			return;
		}
		this.context.put("forum", forum);
		
		String sortColumn = this.request.getParameter("sort_column");
		String sortDirection = this.request.getParameter("sort_direction");
		
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = getSortEvaluations(sortColumn, sortDirection);

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		//org.etudes.api.app.jforum.User currentUser = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
		
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		//List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getForumEvaluationsWithPosts(forumId, org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a, UserDirectoryService.getCurrentUser().getId(), false);
		List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getForumEvaluationsWithPosts(forumId, evalSort, UserDirectoryService.getCurrentUser().getId(), false);

		int start = 0;
		String s = this.request.getParameter("start");
		if (s == null || s.trim().equals(""))
		{
			int indexCount = 0;
			boolean foundIndex = false;
			for (org.etudes.api.app.jforum.Evaluation presEval : evaluations)
			{
				indexCount++;
				if (presEval.getUserId() == userId)
				{
					// start = evaluations.indexOf(presEval);
					indexCount--;
					foundIndex = true;
					break;
				}
			}
			if (foundIndex)
				start = indexCount;

			this.context.put("presUserId", userId);
		}
		else
		{
			start = ViewCommon.getStartPage();
			org.etudes.api.app.jforum.Evaluation eval = evaluations.get(start);
			this.context.put("presUserId", eval.getUserId());
			userId = eval.getUserId();
		}
		ViewCommon.contextToPagination(start, evaluations.size(), 1);

		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);

		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		List<org.etudes.api.app.jforum.Post> posts = jforumPostService.getUserForumPosts(forumId, user.getSakaiUserId(), UserDirectoryService.getCurrentUser().getId());
		
		this.context.put("attachmentsEnabled", true);
		this.context.put("canDownloadAttachments", true);
		this.context.put("posts", posts);
		
		if (sortColumn == null || sortColumn.trim() == "")
		{
			sortColumn = "name";
		}
		
		if (sortDirection == null || sortDirection.trim() == "")
		{
			sortDirection = "a";
		}
		
		this.context.put("sort_column", sortColumn);
		this.context.put("sort_direction", sortDirection);

		if (!JForumUserUtil.isUserActive(user.getSakaiUserId()))
		{
			this.context.put("message", I18n.getMessage("Evaluation.userInactive"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		this.setTemplateName(TemplateKeys.GRADE_VIEW_USER_FORUM_REPLIES);
	}
	
	/**
	 * show user replies for the forum topics
	 */
	public void showUserTopicReplies() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		int topicId = this.request.getIntParameter("topic_id");
		int userId = this.request.getIntParameter("user_id");

		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		org.etudes.api.app.jforum.Topic topic = jforumPostService.getTopic(topicId);
		if (topic == null)
		{
			this.context.put("errorMessage", I18n.getMessage("Forum.notFound"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_TOPIC);
			return;
		}
		
		String sortColumn = this.request.getParameter("sort_column");
		String sortDirection = this.request.getParameter("sort_direction");
		
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = getSortEvaluations(sortColumn, sortDirection);

		org.etudes.api.app.jforum.Forum forum = topic.getForum();
		
		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		//org.etudes.api.app.jforum.User currentUser = jforumUserService.getBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
		
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		//List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getTopicEvaluationsWithPosts(topicId, org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a, UserDirectoryService.getCurrentUser().getId(), false);
		List<org.etudes.api.app.jforum.Evaluation> evaluations = jforumGradeService.getTopicEvaluationsWithPosts(topicId, evalSort, UserDirectoryService.getCurrentUser().getId(), false);

		// pagination
		int start = 0;
		String s = this.request.getParameter("start");
		if (s == null || s.trim().equals(""))
		{
			int indexCount = 0;
			boolean foundIndex = false;
			for (org.etudes.api.app.jforum.Evaluation presEval : evaluations)
			{
				indexCount++;
				if (presEval.getUserId() == userId)
				{
					indexCount--;
					foundIndex = true;
					break;
				}
			}
			if (foundIndex)
				start = indexCount;

			this.context.put("presUserId", userId);
		}
		else
		{
			start = ViewCommon.getStartPage();
			org.etudes.api.app.jforum.Evaluation eval = evaluations.get(start);
			this.context.put("presUserId", eval.getUserId());
			userId = eval.getUserId();
		}
		ViewCommon.contextToPagination(start, evaluations.size(), 1);
		
		org.etudes.api.app.jforum.User user = jforumUserService.getByUserId(userId);
		
		List<org.etudes.api.app.jforum.Post> posts = jforumPostService.getUserTopicPosts(topicId, user.getSakaiUserId(), UserDirectoryService.getCurrentUser().getId());

		this.context.put("attachmentsEnabled", true);
		this.context.put("canDownloadAttachments", true);
		this.context.put("topic", topic);
		this.context.put("forum", forum);
		this.context.put("posts", posts);
		
		if (sortColumn == null || sortColumn.trim() == "")
		{
			sortColumn = "name";
		}
		
		if (sortDirection == null || sortDirection.trim() == "")
		{
			sortDirection = "a";
		}
		
		this.context.put("sort_column", sortColumn);
		this.context.put("sort_direction", sortDirection);

		if (!JForumUserUtil.isUserActive(user.getSakaiUserId()))
		{
			this.context.put("message", I18n.getMessage("Evaluation.userInactive"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		this.setTemplateName(TemplateKeys.GRADE_VIEW_USER_TOPIC_REPLIES);
	}
	
	/**
	 * view category grade
	 * 
	 * @throws Exception
	 */
	public void viewCategoryGrade() throws Exception
	{
		int categoryId = this.request.getIntParameter("category_id");

		//Category category = ForumRepository.getCategory(categoryId);
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);

		if (category == null)
		{
			return;
		}
		this.context.put("category", category);

		//Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByCategoryId(categoryId);
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByCategoryId(categoryId);
		
		this.context.put("grade", grade);

		String sakaiUserId = UserDirectoryService.getCurrentUser().getId();
		//int currentUserId = (DataAccessDriver.getInstance().newUserDAO().selectBySakaiUserId(sakuserId)).getId();
		
		//Evaluation evaluation = DataAccessDriver.getInstance().newEvaluationDAO().selectEvaluationByCategoryIdUserId(categoryId, currentUserId);
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserCategoryEvaluation(categoryId, sakaiUserId);
		this.context.put("evaluation", evaluation);

		if (evaluation == null || !evaluation.isReleased())
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE);
			this.context.put("sectionTitle", I18n.getMessage("Grade.Category.ViewGrades") + " " + category.getTitle());
			
			this.context.put( "message", I18n.getMessage("Evaluation.category.notDone"));
			/*this.context.put(
					"message",
					I18n.getMessage("Evaluation.category.notDone",
							new String[] { this.request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));*/
			String returnToURL = this.request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
			this.context.put("returnToURL", returnToURL);
		}
		else
		{
			// capture student visit time to view the grades
			//evaluation.setReviewedDate(new Timestamp(System.currentTimeMillis()));
			//DataAccessDriver.getInstance().newEvaluationDAO().updateUserReviewedDate(evaluation);
			
			jforumGradeService.markUserReviewedDate(evaluation);
			
			if (evaluation.getComments() != null)
			{
				evaluation.setComments(evaluation.getComments().replaceAll("\\r\\n|\\r", "<br/>"));
			}

			this.setTemplateName(TemplateKeys.GRADE_VIEW_CATEGORY);
			/*this.context.put(
					"message",
					I18n.getMessage("Evaluation.done",
							new String[] { this.request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));*/
			String returnToURL = this.request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
			this.context.put("returnToURL", returnToURL);
		}
	}
	
	/**
	 * show user user category evaluation. May be used to access from different tool like gradebook
	 * 
	 * @throws Exception
	 */
	public void viewEvalCategoryUser() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		if (!isfacilitator)
		{
			this.context.put("errorMessage", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED_POP_UP);
			return;
		}

		int categoryId = this.request.getIntParameter("category_id");
		String sakaiUserId = this.request.getParameter("sakai_user_id");

		if (!JForumUserUtil.isUserActive(sakaiUserId))
		{
			this.context.put("message", I18n.getMessage("Evaluation.userInactive"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		this.context.put("category", category);
		this.context.put("userId", user.getId());
		this.context.put("user", user);
		
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserCategoryEvaluation(categoryId, user.getSakaiUserId(), true);
				
		if (evaluation != null)
		{
			this.context.put("evaluation", evaluation);
		}

		org.etudes.api.app.jforum.Grade grade = category.getGrade();
		this.context.put("grade", grade);

		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("addToGradebook", true);
		}

		this.context.put("pageTitle", SystemGlobals.getValue(ConfigKeys.FORUM_NAME) + " - " + I18n.getMessage("Grade.gradeCategoryUser"));

		this.setTemplateName(TemplateKeys.GRADE_EVAL_CATEGORY_USER);
	}
	
	/**
	 * show user forum evaluation. May be used to access from different tool like gradebook
	 * 
	 * @throws Exception
	 */
	public void viewEvalForumUser() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		if (!isfacilitator)
		{
			this.context.put("errorMessage", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED_POP_UP);
			return;
		}

		int forumId = this.request.getIntParameter("forum_id");
		String sakaiUserId = this.request.getParameter("sakai_user_id");
		
		if (!JForumUserUtil.isUserActive(sakaiUserId))
		{
			this.context.put("message", I18n.getMessage("Evaluation.userInactive"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);
		
		if (forum == null)
		{
			return;
		}

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		this.context.put("user", user);
		this.context.put("forum", forum);
		this.context.put("userId", user.getId());
		
		org.etudes.api.app.jforum.Category category = forum.getCategory();
		this.context.put("category", category);
		
		// special access user dates
		List<org.etudes.api.app.jforum.SpecialAccess> specialAccessList = null;
		if ((forum != null) && (forum.getAccessDates() != null) && ((forum.getAccessDates().getOpenDate() != null) || (forum.getAccessDates().getDueDate() != null) || (forum.getAccessDates().getAllowUntilDate() != null)))
		{
			specialAccessList = forum.getSpecialAccess();
		}
		
		boolean specialAccessUser = false;
		if (specialAccessList != null)
		{
			for (org.etudes.api.app.jforum.SpecialAccess specialAccess : specialAccessList)
			{
				List<Integer> userIds = specialAccess.getUserIds();
				
				if (userIds == null || userIds.isEmpty())
				{
					continue;
				}
				
				for (Integer saUserId : userIds)
				{
					if (saUserId.intValue() == user.getId())
					{
						specialAccessUser = true;
						break;
					}
				}
				
				if (specialAccessUser)
				{
					if (specialAccess.getAccessDates() != null)
					{
						if (!specialAccess.isOverrideStartDate())
						{
							specialAccess.getAccessDates().setOpenDate(forum.getAccessDates().getOpenDate());
						}
						
						if (!specialAccess.isOverrideEndDate())
						{
							specialAccess.getAccessDates().setDueDate(forum.getAccessDates().getDueDate());
						}
						
						if (!specialAccess.isOverrideAllowUntilDate())
						{
							specialAccess.getAccessDates().setAllowUntilDate(forum.getAccessDates().getAllowUntilDate());
						}
					}
					this.context.put("userSpecialAccess", specialAccess);
					break;
				}
			}
		}

		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserForumEvaluation(forumId, user.getSakaiUserId(), true);
		
		if (evaluation != null)
		{
			this.context.put("evaluation", evaluation);
		}

		Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumId(forum.getId());
		this.context.put("grade", grade);

		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("addToGradebook", true);
		}

		this.context.put("pageTitle", SystemGlobals.getValue(ConfigKeys.FORUM_NAME) + " - " + I18n.getMessage("Grade.gradeForumUser"));

		this.setTemplateName(TemplateKeys.GRADE_EVAL_FORUM_USER);
	}
	
	/**
	 * show user topic evaluation. May be used to access from different tool like gradebook
	 * 
	 * @throws Exception
	 */
	public void viewEvalTopicUser() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		if (!isfacilitator)
		{
			this.context.put("errorMessage", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.USER_NOT_AUTHORIZED_POP_UP);
			return;
		}

		int topicId = this.request.getIntParameter("topic_id");
		String sakaiUserId = this.request.getParameter("sakai_user_id");
		
		if (!JForumUserUtil.isUserActive(sakaiUserId))
		{
			this.context.put("message", I18n.getMessage("Evaluation.userInactive"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}

		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		org.etudes.api.app.jforum.Topic topic = jforumPostService.getTopic(topicId);
		
		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		this.context.put("topic", topic);
		this.context.put("userId", user.getId());
		this.context.put("user", user);
		
		org.etudes.api.app.jforum.Forum forum = topic.getForum();
		this.context.put("forum", forum);
		
		org.etudes.api.app.jforum.Category category = forum.getCategory();
		this.context.put("category", category);
		
		// special access user dates
		boolean topicDates = false, forumDates = false;
		List<org.etudes.api.app.jforum.SpecialAccess> specialAccessList = null;
		if ((topic.getAccessDates() != null) && ((topic.getAccessDates().getOpenDate() != null) || (topic.getAccessDates().getDueDate() != null) || (topic.getAccessDates().getAllowUntilDate() != null)))
		{
			specialAccessList = topic.getSpecialAccess();
			topicDates = true;
		}
		else if ((forum != null) && (forum.getAccessDates() != null) && ((forum.getAccessDates().getOpenDate() != null) || (forum.getAccessDates().getDueDate() != null) || (forum.getAccessDates().getAllowUntilDate() != null)))
		{
			specialAccessList = forum.getSpecialAccess();
			forumDates = true;
		}
		
		boolean specialAccessUser = false;
		if (specialAccessList != null)
		{
			for (org.etudes.api.app.jforum.SpecialAccess specialAccess : specialAccessList)
			{
				List<Integer> userIds = specialAccess.getUserIds();
				
				if (userIds == null || userIds.isEmpty())
				{
					continue;
				}
				
				for (Integer saUserId : userIds)
				{
					if (saUserId.intValue() == user.getId())
					{
						specialAccessUser = true;
						break;
					}
				}
				
				if (specialAccessUser)
				{
					if (specialAccess.getAccessDates() != null)
					{
						if (!specialAccess.isOverrideStartDate())
						{
							if (topicDates)
							{
								specialAccess.getAccessDates().setOpenDate(topic.getAccessDates().getOpenDate());
							}
							else if (forumDates)
							{
								specialAccess.getAccessDates().setOpenDate(forum.getAccessDates().getOpenDate());
							}
						}
						
						if (!specialAccess.isOverrideEndDate())
						{
							if (topicDates)
							{
								specialAccess.getAccessDates().setDueDate(topic.getAccessDates().getDueDate());
							}
							else if (forumDates)
							{
								specialAccess.getAccessDates().setDueDate(forum.getAccessDates().getDueDate());
							}
						}
						
						if (!specialAccess.isOverrideAllowUntilDate())
						{
							if (topicDates)
							{
								specialAccess.getAccessDates().setAllowUntilDate(topic.getAccessDates().getAllowUntilDate());
							}
							else if (forumDates)
							{
								specialAccess.getAccessDates().setAllowUntilDate(forum.getAccessDates().getAllowUntilDate());
							}
						}
					}
					this.context.put("userSpecialAccess", specialAccess);
					break;
				}
			}
		}		
		
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserTopicEvaluation(topicId, user.getSakaiUserId(), true);
		
		if (evaluation != null)
		{
			this.context.put("evaluation", evaluation);
		}

		Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumTopicId(topic.getForumId(), topic.getId());
		this.context.put("grade", grade);

		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("addToGradebook", true);
		}

		this.context.put("pageTitle", SystemGlobals.getValue(ConfigKeys.FORUM_NAME) + " - " + I18n.getMessage("Grade.gradeTopicUser"));

		this.setTemplateName(TemplateKeys.GRADE_EVAL_TOPIC_USER);
	}
	
	/**
	 * view forum grade
	 * 
	 * @throws Exception
	 */
	public void viewForumGrade() throws Exception
	{
		int forumId = this.request.getIntParameter("forum_id");

		//Forum forum = ForumRepository.getForum(forumId);
		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);
		if (forum == null)
		{
			this.context.put("errorMessage", I18n.getMessage("Forum.notFound"));
			return;
		}
		
		this.context.put("forum", forum);

		//Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumId(forum.getId());
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByForumId(forumId);
		
		this.context.put("grade", grade);

		String sakaiUserId = UserDirectoryService.getCurrentUser().getId();
		//int currentUserId = (DataAccessDriver.getInstance().newUserDAO().selectBySakaiUserId(sakuserId)).getId();

		//Evaluation evaluation = DataAccessDriver.getInstance().newEvaluationDAO().selectEvaluationByForumIdUserId(forumId, currentUserId);
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserForumEvaluation(forumId, sakaiUserId);
		this.context.put("evaluation", evaluation);

		if (evaluation == null || !evaluation.isReleased())
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE);
			this.context.put("sectionTitle", I18n.getMessage("Grade.Forum.ViewGrades") + " " + forum.getName());
			this.context.put("message", I18n.getMessage("Evaluation.forum.notDone"));
			/*this.context.put(
					"message",
					I18n.getMessage("Evaluation.forum.notDone",
							new String[] { this.request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));*/
			String returnToURL = this.request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
			this.context.put("returnToURL", returnToURL);
		}
		else
		{
			// capture student visit time to view the grades
			jforumGradeService.markUserReviewedDate(evaluation);
			
			if (evaluation.getComments() != null)
			{
				evaluation.setComments(evaluation.getComments().replaceAll("\\r\\n|\\r", "<br/>"));
			}

			this.setTemplateName(TemplateKeys.GRADE_VIEW_FORUM);
			/*this.context.put(
					"message",
					I18n.getMessage("Evaluation.done",
							new String[] { this.request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));*/
			String returnToURL = this.request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
			this.context.put("returnToURL", returnToURL);
		}
	}

	/**
	 * view topic grade
	 * @throws Exception
	 */
	public void viewTopicGrade() throws Exception
	{
		int topicId = this.request.getIntParameter("topic_id");
			
		//TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
		//Topic topic = tm.selectById(topicId);
		
		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		org.etudes.api.app.jforum.Topic topic = jforumPostService.getTopic(topicId);
		
		//Forum forum = ForumRepository.getForum(topic.getForumId());
		org.etudes.api.app.jforum.Forum forum = topic.getForum();
				
		if (forum == null)
		{
			return;
		}
		this.context.put("forum", forum);
		
		this.context.put("topic", topic);
		
		//Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumTopicId(topic.getForumId(), topicId);
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByForumTopicId(topic.getForumId(), topic.getId());
		
		this.context.put("grade", grade);
		
		String sakaiUserId = UserDirectoryService.getCurrentUser().getId();
		//int currentUserId = (DataAccessDriver.getInstance().newUserDAO().selectBySakaiUserId(sakuserId)).getId();
		
		//Evaluation evaluation = DataAccessDriver.getInstance().newEvaluationDAO().selectEvaluationByForumIdTopicIdUserId(topic.getForumId(), topicId, currentUserId);
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserTopicEvaluation(topicId, sakaiUserId);
		this.context.put("evaluation", evaluation);
		
		if (evaluation == null || !evaluation.isReleased()) 
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE);
			this.context.put("sectionTitle",  I18n.getMessage("Grade.Topic.ViewGrades")+ " " + topic.getTitle());
			this.context.put("message", I18n.getMessage("Evaluation.topic.notDone"));
			/*this.context.put("message", I18n.getMessage("Evaluation.topic.notDone",
					new String[] { this.request.getContextPath() + "/forums/show/"+ forum.getId()
									+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));*/
			String returnToURL = this.request.getContextPath() + "/forums/show/"+ forum.getId() + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
			this.context.put("returnToURL", returnToURL);
		} 
		else 
		{
			// capture student visit time to view the grades
			//evaluation.setReviewedDate(new Timestamp(System.currentTimeMillis()));
			//DataAccessDriver.getInstance().newEvaluationDAO().updateUserReviewedDate(evaluation);
			jforumGradeService.markUserReviewedDate(evaluation);
			
			if (evaluation.getComments() != null)
			{
				evaluation.setComments(evaluation.getComments().replaceAll("\\r\\n|\\r", "<br/>"));
			}
			
			this.setTemplateName(TemplateKeys.GRADE_VIEW_TOPIC);
			/*this.context.put("message", I18n.getMessage("Evaluation.done",
					new String[] { this.request.getContextPath() + "/forums/show/"+ forum.getId()
									+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));*/
			String returnToURL = this.request.getContextPath() + "/forums/show/"+ forum.getId() + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
			this.context.put("returnToURL", returnToURL);
		}
	}
	
	/**
	 * View user category grades
	 * 
	 * @throws Exception
	 */
	public void viewUserCategoryGrade() throws Exception
	{
		int categoryId = this.request.getIntParameter("category_id");
		String sakaiUserId = this.request.getParameter("sakai_user_id");
		String returnTo = this.request.getParameter("return_to");
		String returnParams = this.request.getParameter("return_params");

		if (categoryId < 0 || sakaiUserId == null || returnTo == null)
		{
			return;
		}

		String returnPath[] = returnTo.split("~");

		if (returnPath.length > 1)
		{
			returnTo = returnTo.replace("~", "/");
		}
		
		StringBuilder siteNavUrl = new StringBuilder();

		String portalUrl = ServerConfigurationService.getPortalUrl();
		siteNavUrl.append(portalUrl);
		siteNavUrl.append("/directtool/");
		siteNavUrl.append(returnTo);
		
		if (returnParams != null && returnParams.trim().length() > 0)
		{
			if ((siteNavUrl.charAt(siteNavUrl.length() - 1) != '/') && (returnParams.charAt(0) != '/'))
			{
				siteNavUrl.append("/");
			}
			siteNavUrl.append(returnParams);
		}
		
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		boolean isParticipant = false;
		if (!isfacilitator)
		{
			isParticipant = JForumUserUtil.isJForumParticipant(UserDirectoryService.getCurrentUser().getId());
			
			if (isParticipant && !UserDirectoryService.getCurrentUser().getId().equalsIgnoreCase(sakaiUserId.trim()))
			{
				this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
				this.context.put("message", I18n.getMessage("User.NotAuthorized"));
				this.context.put("returnToURL", siteNavUrl.toString());
				return;
			}
		}
		
		if (!(isfacilitator || isParticipant))
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
			this.context.put("message", I18n.getMessage("User.NotAuthorized"));
			this.context.put("returnToURL", siteNavUrl.toString());
			return;
		}
		
		this.context.put("facilitator", isfacilitator);
		
		//Category category = ForumRepository.getCategory(categoryId);
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);

		if (category == null)
		{
			return;
		}
		this.context.put("category", category);

		//Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByCategoryId(categoryId);
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByCategoryId(categoryId);
		this.context.put("grade", grade);

		//int userId = (DataAccessDriver.getInstance().newUserDAO().selectBySakaiUserId(sakaiUserId)).getId();

		//Evaluation evaluation = DataAccessDriver.getInstance().newEvaluationDAO().selectEvaluationByCategoryIdUserId(categoryId, userId);
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserCategoryEvaluation(categoryId, sakaiUserId);
		this.context.put("evaluation", evaluation);

		

		if (evaluation == null || !evaluation.isReleased())
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
			this.context.put("sectionTitle", I18n.getMessage("Grade.Category.ViewGrades") + " " + category.getTitle());
			this.context.put("message", I18n.getMessage("Evaluation.category.notDone.user"));
			//this.context.put("message", I18n.getMessage("Evaluation.category.notDone.user", new String[] { siteNavUrl.toString() }));
			this.context.put("returnToURL", siteNavUrl.toString());
		}
		else
		{
			// capture student visit time to view the grades
			//evaluation.setReviewedDate(new Timestamp(System.currentTimeMillis()));
			//DataAccessDriver.getInstance().newEvaluationDAO().updateUserReviewedDate(evaluation);
			
			if (isParticipant)
			{
				jforumGradeService.markUserReviewedDate(evaluation);
			}
			
			if (evaluation.getComments() != null)
			{
				evaluation.setComments(evaluation.getComments().replaceAll("\\r\\n|\\r", "<br/>"));
			}
			
			this.setTemplateName(TemplateKeys.GRADE_VIEW_CATEGORY_USER);
			this.context.put("sectionTitle", I18n.getMessage("Grade.Category.ViewGrades") + " " + category.getTitle());
			//this.context.put("message", I18n.getMessage("Evaluation.done.user", new String[] { siteNavUrl.toString() }));
			this.context.put("returnToURL", siteNavUrl.toString());
		}
		
		if (isfacilitator)
		{
			JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);
			
			if (user != null)
			{
				this.context.put("evalUser", user);
			}
			
			JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
			List<org.etudes.api.app.jforum.Post> posts = jforumPostService.getUserCategoryPosts(categoryId, sakaiUserId, UserDirectoryService.getCurrentUser().getId());
			
			this.context.put("instructorReviewMode", true);
			
			if (posts.size() > 0 )
			{
				this.context.put("attachmentsEnabled", true);
				this.context.put("canDownloadAttachments", true);
				this.context.put("posts", posts);
			}
		}
	}

	/**
	 * View user category replies. This may be for use in different tool like gradebook
	 * 
	 * @throws Exception
	 */
	public void viewUserCategoryReplies() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		
		int categoryId = this.request.getIntParameter("category_id");
		String sakaiUserId = this.request.getParameter("sakai_user_id");
		String returnTo = this.request.getParameter("return_to");
		String returnParams = this.request.getParameter("return_params");
		
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId, UserDirectoryService.getCurrentUser().getId());

		if (category == null)
		{
			this.context.put("errorMessage", I18n.getMessage("Forums.Category.NotFound"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_CATEGORY);
			return;
		}
		
		String returnPath[] = returnTo.split("~");

		if (returnPath.length > 1)
		{
			returnTo = returnTo.replace("~", "/");
		}
		
		StringBuilder siteNavUrl = new StringBuilder();

		String portalUrl = ServerConfigurationService.getPortalUrl();
		siteNavUrl.append(portalUrl);
		siteNavUrl.append("/directtool/");
		siteNavUrl.append(returnTo);
		
		if (returnParams != null && returnParams.trim().length() > 0)
		{
			if ((siteNavUrl.charAt(siteNavUrl.length() - 1) != '/') && (returnParams.charAt(0) != '/'))
			{
				siteNavUrl.append("/");
			}
			siteNavUrl.append(returnParams);
		}
		
		if (!JForumUserUtil.isJForumParticipant(sakaiUserId.trim()))
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
			this.context.put("message", I18n.getMessage("User.NotParticipant"));
			this.context.put("returnToURL", siteNavUrl.toString());
			return;
		}
		
		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		List<org.etudes.api.app.jforum.Post> posts = jforumPostService.getUserCategoryPosts(categoryId, sakaiUserId, UserDirectoryService.getCurrentUser().getId());
		
		if (posts.size() == 0)
		{
			this.context.put("userNoPostsMessage", I18n.getMessage("Grade.userNoReplies"));
			
			JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);			
			if (user != null)
			{
				this.context.put("user", user);
			}
		}
		
		this.context.put("facilitator", isfacilitator);
		this.context.put("category", category);
		this.context.put("attachmentsEnabled", true);
		this.context.put("canDownloadAttachments", true);
		this.context.put("posts", posts);
		
		this.context.put("returnToURL", siteNavUrl.toString());
		this.setTemplateName(TemplateKeys.GRADE_VIEW_USER_CATEGORY_REPLIES_GRADEBOOK);	
	}
	
	
	/**
	 * View user forum grades
	 * 
	 * @throws Exception
	 */
	public void viewUserForumGrade() throws Exception
	{
		int forumId = this.request.getIntParameter("forum_id");
		String sakaiUserId = this.request.getParameter("sakai_user_id");
		String returnTo = this.request.getParameter("return_to");
		String returnParams = this.request.getParameter("return_params");
		
		if (forumId < 0 || sakaiUserId == null || returnTo == null)
		{
			return;
		}
		
		String returnPath[] = returnTo.split("~");
		
		if (returnPath.length > 1)
		{
			returnTo = returnTo.replace("~", "/");
		}
		
		String portalUrl = ServerConfigurationService.getPortalUrl();
		StringBuilder siteNavUrl = new StringBuilder();
		siteNavUrl.append(portalUrl);
		siteNavUrl.append("/directtool/");
		siteNavUrl.append(returnTo);
		
		if (returnParams != null && returnParams.trim().length() > 0)
		{
			if ((siteNavUrl.charAt(siteNavUrl.length() - 1) != '/') && (returnParams.charAt(0) != '/'))
			{
				siteNavUrl.append("/");
			}
			siteNavUrl.append(returnParams);
		}
		
		/*if (!UserDirectoryService.getCurrentUser().getId().equalsIgnoreCase(sakaiUserId.trim()))
		{
			return;
		}*/
		
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		boolean isParticipant = false;
		if (!isfacilitator)
		{
			isParticipant = JForumUserUtil.isJForumParticipant(UserDirectoryService.getCurrentUser().getId());
			
			if (isParticipant && !UserDirectoryService.getCurrentUser().getId().equalsIgnoreCase(sakaiUserId.trim()))
			{
				this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
				this.context.put("message", I18n.getMessage("User.NotAuthorized"));
				this.context.put("returnToURL", siteNavUrl.toString());
				return;
			}
		}
		
		if (!(isfacilitator || isParticipant))
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
			this.context.put("message", I18n.getMessage("User.NotAuthorized"));
			this.context.put("returnToURL", siteNavUrl.toString());
			return;
		}
		
		this.context.put("facilitator", isfacilitator);
		
		// Forum forum = ForumRepository.getForum(forumId);
		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);

		if (forum == null) return;
		this.context.put("forum", forum);
		
		//Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumId(forum.getId());
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByForumId(forumId);
		this.context.put("grade", grade);
		
		//int userId = (DataAccessDriver.getInstance().newUserDAO().selectBySakaiUserId(sakaiUserId)).getId();
		
		//Evaluation evaluation = DataAccessDriver.getInstance().newEvaluationDAO().selectEvaluationByForumIdUserId(forumId, userId);
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserForumEvaluation(forumId, sakaiUserId);
		this.context.put("evaluation", evaluation);
		
		if (evaluation == null || !evaluation.isReleased()) 
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
			this.context.put("sectionTitle",  I18n.getMessage("Grade.Forum.ViewGrades")+ " " + forum.getName());
			this.context.put("message", I18n.getMessage("Evaluation.forum.notDone.user"));
			//this.context.put("message", I18n.getMessage("Evaluation.forum.notDone.user", new String[] {siteNavUrl.toString()}));
			this.context.put("returnToURL", siteNavUrl.toString());
		} 
		else 
		{
			// capture student visit time to view the grades
			//evaluation.setReviewedDate(new Timestamp(System.currentTimeMillis()));
			//DataAccessDriver.getInstance().newEvaluationDAO().updateUserReviewedDate(evaluation);
			if (isParticipant)
			{
				jforumGradeService.markUserReviewedDate(evaluation);
			}
			
			if (evaluation.getComments() != null)
			{
				evaluation.setComments(evaluation.getComments().replaceAll("\\r\\n|\\r", "<br/>"));
			}
			
			this.setTemplateName(TemplateKeys.GRADE_VIEW_FORUM_USER);
			this.context.put("sectionTitle",  I18n.getMessage("Grade.Forum.ViewGrades")+ " " + forum.getName());
			//this.context.put("message", I18n.getMessage("Evaluation.done.user", new String[] {siteNavUrl.toString()}));
			this.context.put("returnToURL", siteNavUrl.toString());
		}
		
		if (isfacilitator)
		{
			JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);
			
			if (user != null)
			{
				this.context.put("evalUser", user);
			}
			
			JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
			List<org.etudes.api.app.jforum.Post> posts = jforumPostService.getUserForumPosts(forumId, sakaiUserId, UserDirectoryService.getCurrentUser().getId());
			
			this.context.put("instructorReviewMode", true);
			
			if (posts.size() > 0 )
			{
				this.context.put("attachmentsEnabled", true);
				this.context.put("canDownloadAttachments", true);
				this.context.put("posts", posts);
			}
		}
	}
	
	/**
	 * View user forum replies. This may be for use in different tool like gradebook
	 * 
	 * @throws Exception
	 */
	public void viewUserForumReplies() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		
		int forumId = this.request.getIntParameter("forum_id");
		String sakaiUserId = this.request.getParameter("sakai_user_id");
		String returnTo = this.request.getParameter("return_to");
		String returnParams = this.request.getParameter("return_params");
		
		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId, UserDirectoryService.getCurrentUser().getId());
		if (forum == null)
		{
			this.context.put("errorMessage", I18n.getMessage("Forum.notFound"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_FORUM);
			return;
		}
				
		String returnPath[] = returnTo.split("~");

		if (returnPath.length > 1)
		{
			returnTo = returnTo.replace("~", "/");
		}
		
		StringBuilder siteNavUrl = new StringBuilder();

		String portalUrl = ServerConfigurationService.getPortalUrl();
		siteNavUrl.append(portalUrl);
		siteNavUrl.append("/directtool/");
		siteNavUrl.append(returnTo);
		
		if (returnParams != null && returnParams.trim().length() > 0)
		{
			if ((siteNavUrl.charAt(siteNavUrl.length() - 1) != '/') && (returnParams.charAt(0) != '/'))
			{
				siteNavUrl.append("/");
			}
			siteNavUrl.append(returnParams);
		}
		
		if (!JForumUserUtil.isJForumParticipant(sakaiUserId.trim()))
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
			this.context.put("message", I18n.getMessage("User.NotParticipant"));
			this.context.put("returnToURL", siteNavUrl.toString());
			return;
		}
		
		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		List<org.etudes.api.app.jforum.Post> posts = jforumPostService.getUserForumPosts(forumId, sakaiUserId, UserDirectoryService.getCurrentUser().getId());
		
		if (posts.size() == 0)
		{
			this.context.put("userNoPostsMessage", I18n.getMessage("Grade.userNoReplies"));
			
			JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);			
			if (user != null)
			{
				this.context.put("user", user);
			}
		}
		
		this.context.put("facilitator", isfacilitator);
		this.context.put("forum", forum);
		this.context.put("attachmentsEnabled", true);
		this.context.put("canDownloadAttachments", true);
		this.context.put("posts", posts);
		
		this.context.put("returnToURL", siteNavUrl.toString());
		this.setTemplateName(TemplateKeys.GRADE_VIEW_USER_FORUM_REPLIES_GRADEBOOK);		
	}
	
	/**
	 * View user topic grades
	 * 
	 * @throws Exception
	 */
	public void viewUserTopicGrade() throws Exception
	{
		int topicId = this.request.getIntParameter("topic_id");
		String sakaiUserId = this.request.getParameter("sakai_user_id");
		String returnTo = this.request.getParameter("return_to");
		String returnParams = this.request.getParameter("return_params");
		
		if (topicId < 0 || sakaiUserId == null || returnTo == null)
		{
			return;
		}
		
		String returnPath[] = returnTo.split("~");
		
		if (returnPath.length > 1)
		{
			returnTo = returnTo.replace("~", "/");
		}
		
		StringBuilder siteNavUrl = new StringBuilder();
		
		String portalUrl = ServerConfigurationService.getPortalUrl();
		siteNavUrl.append(portalUrl);
		siteNavUrl.append("/directtool/");
		siteNavUrl.append(returnTo);
		
		if (returnParams != null && returnParams.trim().length() > 0)
		{
			if ((siteNavUrl.charAt(siteNavUrl.length() - 1) != '/') && (returnParams.charAt(0) != '/'))
			{
				siteNavUrl.append("/");
			}
			siteNavUrl.append(returnParams);
		}
		
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		boolean isParticipant = false;
		if (!isfacilitator)
		{
			isParticipant = JForumUserUtil.isJForumParticipant(UserDirectoryService.getCurrentUser().getId());
			
			if (isParticipant && !UserDirectoryService.getCurrentUser().getId().equalsIgnoreCase(sakaiUserId.trim()))
			{
				this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
				this.context.put("message", I18n.getMessage("User.NotAuthorized"));
				this.context.put("returnToURL", siteNavUrl.toString());
				return;
			}
		}
		
		if (!(isfacilitator || isParticipant))
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
			this.context.put("message", I18n.getMessage("User.NotAuthorized"));
			this.context.put("returnToURL", siteNavUrl.toString());
			return;
		}
		
		this.context.put("facilitator", isfacilitator);
		
		//TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
		//Topic topic = tm.selectById(topicId);
		
		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		org.etudes.api.app.jforum.Topic topic = jforumPostService.getTopic(topicId);
		
		//Forum forum = ForumRepository.getForum(topic.getForumId());
		org.etudes.api.app.jforum.Forum forum = topic.getForum();
		
		//Forum forum = ForumRepository.getForum(topic.getForumId());

		if (forum == null) return;
		this.context.put("forum", forum);
		
		this.context.put("topic", topic);
		
		//Grade grade = DataAccessDriver.getInstance().newGradeDAO().selectByForumTopicId(topic.getForumId(), topicId);
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByForumTopicId(topic.getForumId(), topic.getId());
		this.context.put("grade", grade);
		
		//int userId = (DataAccessDriver.getInstance().newUserDAO().selectBySakaiUserId(sakaiUserId)).getId();
		
		//Evaluation evaluation = DataAccessDriver.getInstance().newEvaluationDAO().selectEvaluationByForumIdTopicIdUserId(topic.getForumId(), topicId, userId);
		org.etudes.api.app.jforum.Evaluation evaluation = jforumGradeService.getUserTopicEvaluation(topicId, sakaiUserId);
		this.context.put("evaluation", evaluation);
		
		
		
		if (evaluation == null || !evaluation.isReleased()) 
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
			this.context.put("sectionTitle",  I18n.getMessage("Grade.Topic.ViewGrades")+ " " + topic.getTitle());
			this.context.put("message", I18n.getMessage("Evaluation.topic.notDone.user"));
			//this.context.put("message", I18n.getMessage("Evaluation.topic.notDone.user", new String[] {siteNavUrl.toString()}));
			this.context.put("returnToURL", siteNavUrl.toString());
		} 
		else 
		{
			// capture student visit time to view the grades
			//evaluation.setReviewedDate(new Timestamp(System.currentTimeMillis()));
			//DataAccessDriver.getInstance().newEvaluationDAO().updateUserReviewedDate(evaluation);
			if (isParticipant)
			{
				jforumGradeService.markUserReviewedDate(evaluation);
			}
			
			if (evaluation.getComments() != null)
			{
				evaluation.setComments(evaluation.getComments().replaceAll("\\r\\n|\\r", "<br/>"));
			}
			
			this.setTemplateName(TemplateKeys.GRADE_VIEW_TOPIC_USER);
			this.context.put("sectionTitle",  I18n.getMessage("Grade.Topic.ViewGrades")+ " " + topic.getTitle());
			//this.context.put("message", I18n.getMessage("Evaluation.done.user", new String[] {siteNavUrl.toString()}));
			this.context.put("returnToURL", siteNavUrl.toString());
		}
		
		if (isfacilitator)
		{
			JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);
			
			if (user != null)
			{
				this.context.put("evalUser", user);
			}
			
			List<org.etudes.api.app.jforum.Post> posts = jforumPostService.getUserTopicPosts(topicId, sakaiUserId, UserDirectoryService.getCurrentUser().getId());
			
			this.context.put("instructorReviewMode", true);
			
			if (posts.size() > 0 )
			{
				this.context.put("attachmentsEnabled", true);
				this.context.put("canDownloadAttachments", true);
				this.context.put("posts", posts);
			}
		}
	}
	
	/**
	 * View user topic replies. This may be for use in different tool like gradebook
	 * 
	 * @throws Exception
	 */
	public void viewUserTopicReplies() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		
		int topicId = this.request.getIntParameter("topic_id");		
		String sakaiUserId = this.request.getParameter("sakai_user_id");
		String returnTo = this.request.getParameter("return_to");
		String returnParams = this.request.getParameter("return_params");
		
		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		org.etudes.api.app.jforum.Topic topic = jforumPostService.getTopic(topicId);
		if (topic == null)
		{
			this.context.put("errorMessage", I18n.getMessage("Forum.notFound"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_TOPIC);
			return;
		}
		
		String returnPath[] = returnTo.split("~");

		if (returnPath.length > 1)
		{
			returnTo = returnTo.replace("~", "/");
		}
		
		StringBuilder siteNavUrl = new StringBuilder();

		String portalUrl = ServerConfigurationService.getPortalUrl();
		siteNavUrl.append(portalUrl);
		siteNavUrl.append("/directtool/");
		siteNavUrl.append(returnTo);
		
		if (returnParams != null && returnParams.trim().length() > 0)
		{
			if ((siteNavUrl.charAt(siteNavUrl.length() - 1) != '/') && (returnParams.charAt(0) != '/'))
			{
				siteNavUrl.append("/");
			}
			siteNavUrl.append(returnParams);
		}
		
		if (!JForumUserUtil.isJForumParticipant(sakaiUserId.trim()))
		{
			this.setTemplateName(TemplateKeys.EVALUATION_NOT_AVAILABLE_USER);
			this.context.put("message", I18n.getMessage("User.NotParticipant"));
			this.context.put("returnToURL", siteNavUrl.toString());
			return;
		}
		
		org.etudes.api.app.jforum.Forum forum = topic.getForum();		
		List<org.etudes.api.app.jforum.Post> posts = jforumPostService.getUserTopicPosts(topicId, sakaiUserId, UserDirectoryService.getCurrentUser().getId());
		
		if (posts.size() == 0)
		{
			this.context.put("userNoPostsMessage", I18n.getMessage("Grade.userNoReplies"));
			
			JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);			
			if (user != null)
			{
				this.context.put("user", user);
			}
		}

		this.context.put("facilitator", isfacilitator);
		this.context.put("attachmentsEnabled", true);
		this.context.put("canDownloadAttachments", true);
		this.context.put("topic", topic);
		this.context.put("forum", forum);
		this.context.put("posts", posts);
		
		this.context.put("returnToURL", siteNavUrl.toString());
		this.setTemplateName(TemplateKeys.GRADE_VIEW_USER_TOPIC_REPLIES_GRADEBOOK);
	}
	
	/**
	 * show message topic not found
	 */
	private void topicNotFound() {
		this.setTemplateName(TemplateKeys.POSTS_TOPIC_NOT_FOUND);
		this.context.put("message", I18n.getMessage("PostShow.TopicNotFound"));
	}
	
	/**
	 * compare the scores and comments of evaluation objects
	 * 
	 * @param exisEval	The existing evaluation
	 * 
	 * @param modEval	The mdofied evaluation
	 * 
	 * @return		True - if scores or comments changed
	 * 				False - if scores and comments are not changed
	 */
	protected boolean checkEvaluationChanges(Evaluation exisEval, Evaluation modEval)
	{
		if ((exisEval == null) && (modEval == null))
		{
			return false;
		}
		
		if (((exisEval == null) && (modEval != null)) || ((exisEval != null) && (modEval == null)))
		{
			return true;
		}
		else
		{
			// check for score changes
			Float exisScore = exisEval.getScore();
			Float modScore = modEval.getScore();
			
			if ((exisScore == null) && (modScore == null))
			{
				// continue to check comments
			}
			else if (((exisScore == null) && (modScore != null)) || ((exisScore != null) && (modScore == null)))
			{
				return true;
			}
			else if (!exisScore.equals(modScore))
			{
				return true;
			}
			
			// check for comments changes
			String exisComments = exisEval.getComments();
			String modComments = modEval.getComments();
			
			if ((exisComments == null) && (modComments == null))
			{
			}
			else if (((exisComments == null) && (modComments != null)) || ((exisComments != null) && (modComments == null)))
			{
				return true;
			}
			else if (!exisComments.equalsIgnoreCase(modComments))
			{
				return true;
			}
			
			// check for released
			boolean exisReleased = exisEval.isReleased();
			boolean modReleased = modEval.isReleased();
			
			if (exisReleased != modReleased)
			{
				return true;
			}
		}
		
		return false;
	}
	
	
	protected void evaluateCategoryGrades() throws Exception
	{

		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) 
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		/*adjust scores for all evaluations who have scores or if there are replies / posts for a user*/
		String adjustScores = this.request.getParameter("adjust_scores");
		Float adjustScore = null;
		if (adjustScores != null && adjustScores.trim().length() > 0) 
		{
			try 
			{
				adjustScore = Float.parseFloat(adjustScores);
				
				if (adjustScore.floatValue() > 1000) adjustScore = Float.valueOf(1000.0f);
				
				adjustScore = Float.valueOf(((float) Math.round(adjustScore.floatValue() * 100.0f)) / 100.0f);
			} 
			catch (NumberFormatException ne) 
			{			
				if (logger.isWarnEnabled()) logger.warn("evaluateCategoryGrades(): adjust scores: " + ne);
			}
		}
		
		// deduct scores of all users who have submitted late
		String adjustScoresLate = this.request.getParameter("adjust_scores_late");
		Float adjustScoreLate = null;
		if (adjustScoresLate != null && adjustScoresLate.trim().length() > 0) 
		{
			try 
			{
				adjustScoreLate = Float.parseFloat(adjustScoresLate);
				
				adjustScoreLate = Float.valueOf(((float) Math.round(adjustScoreLate.floatValue() * 100.0f)) / 100.0f);
			} 
			catch (NumberFormatException ne) 
			{			
				if (logger.isWarnEnabled()) logger.warn("evaluateCategoryGrades(): deduct score late submissions: " + ne);
			}
		}
		
		/*adjust comments will be added to all users/evaluations, if there are replies / posts for a user
		 * and if a user doesn't have any posts but has a score (manually added by the teacher), 
		 * add/append comments*/
		String adjustComments = this.request.getParameter("adjust_comments");
				
		Enumeration<?> paramNames = this.request.getParameterNames();

		String currentSakuserId = UserDirectoryService.getCurrentUser().getId();

		//int evaluatedBy = (DataAccessDriver.getInstance().newUserDAO().selectBySakaiUserId(sakuserId)).getId();
		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(currentSakuserId);
		
		String releaseAllEvaluated = this.request.getParameter("releaseall");
		boolean releaseEvaluatedScore = false;
		if ((releaseAllEvaluated != null) && (Integer.parseInt(releaseAllEvaluated) == 1))
		{
			releaseEvaluatedScore = true;
		}
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		
		int gradeId = Integer.parseInt(this.request.getParameter("grade_id"));
		int categoryId = this.request.getIntParameter("category_id");
		
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		org.etudes.api.app.jforum.Grade grade = jforumGradeService.getByCategoryId(categoryId);
		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);
		
		//org.etudes.api.app.jforum.Grade grade = category.getGrade();
	
		if (grade == null || grade.getType() != org.etudes.api.app.jforum.Grade.GradeType.CATEGORY.getType())
		{
			this.context.put("errorMessage", I18n.getMessage("Grade.ItemNotGradable"));
			return;
		}
		
		category.setGrade(grade);
		
		List<org.etudes.api.app.jforum.Evaluation> evaluations = null;
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;
		
		List<org.etudes.api.app.jforum.Evaluation> gradeEvaluations = new ArrayList<org.etudes.api.app.jforum.Evaluation>();
		
		//evaluations = jforumGradeService.getCategoryEvaluationsWithPosts(categoryId, evalSort, UserDirectoryService.getCurrentUser().getId()); 
		evaluations = jforumGradeService.getCategoryEvaluations(categoryId);
		
		
		Map<Integer, org.etudes.api.app.jforum.Evaluation> exisEvalMap = new HashMap<Integer, org.etudes.api.app.jforum.Evaluation>();
		
		for(org.etudes.api.app.jforum.Evaluation eval : evaluations)
		{
			exisEvalMap.put(eval.getId(), eval);
		}
		
		while (paramNames.hasMoreElements())
		{
			String paramName = (String) paramNames.nextElement();
			boolean noScore = false;

			if (paramName.endsWith("_score"))
			{
				noScore = false;
				String paramScore = this.request.getParameter(paramName);
				// paramName is in the format gradeId_evalutionId_jforumUserId_score
				String ids[] = paramName.split("_");
				String sakUserId = null;
				//sakUserId_userId
				sakUserId = this.request.getParameter("sakUserId_"+ ids[2]);
				
				Float score = null;
				int totalPostCount = 0;
				try
				{
					String paramTotalPosts = paramName.replace("score", "totalposts");
					String userTotalPosts = this.request.getParameter(paramTotalPosts);
					
					if ((userTotalPosts != null) && (userTotalPosts.trim().length() > 0)) 
					{
						totalPostCount = Integer.parseInt(userTotalPosts.trim());
					}
					
					// late submission
					String paramIsLate = paramName.replace("score", "late");
					String userIsLate = this.request.getParameter(paramIsLate);
					
					if ((paramScore != null) && (paramScore.trim().length() > 0)) 
					{
						score = Float.parseFloat(this.request.getParameter(paramName));
						
						//if (score.floatValue() < 0) score = Float.valueOf(0.0f);
						if (score.floatValue() > 1000) score = Float.valueOf(1000.0f);
						score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
						if (adjustScore != null && JForumUserUtil.isUserActive(sakUserId))
						{
							score = score + adjustScore;
							score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
						}
						
						// deduct score for late posts
						if (adjustScoreLate != null && adjustScoreLate > 0 && userIsLate != null && userIsLate.equalsIgnoreCase("1"))
						{
							score = score - adjustScoreLate;
							score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
						}
					} 
					else 
					{
						// if (totalPostCount > 0 && adjustScore != null && JForumUserUtil.isUserActive(sakUserId)) 
						if (totalPostCount > 0 && JForumUserUtil.isUserActive(sakUserId))
						{
							if (adjustScore != null)
							{
								score = adjustScore.floatValue();
								score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
							}
							
							// deduct score for late posts
							if (adjustScoreLate != null && adjustScoreLate > 0 && userIsLate != null && userIsLate.equalsIgnoreCase("1"))
							{
								if (score != null)
								{
									score = score - adjustScoreLate;
								}
								else
								{
									score = 0 - adjustScoreLate;
								}
								score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
							}
							
							if (score == null)
							{
								noScore = true;
							}
						}
						else
						{
							noScore = true;
						}
					}
				}
				catch (NumberFormatException ne)
				{
					if (logger.isWarnEnabled()) logger.warn("evaluateCategoryGrades() : " + ne);
					continue;
				}
				String paramComments = paramName.replace("score", "comments");
				String comments = this.request.getParameter(paramComments);
				if (adjustComments != null && adjustComments.trim().length() > 0)
				{
					if ((totalPostCount > 0 || !noScore)&& JForumUserUtil.isUserActive(sakUserId)) 
					{
						StringBuffer strBufComments = new StringBuffer();
						strBufComments.append(comments);
						strBufComments.append("\n");
						strBufComments.append(adjustComments);
						
						comments = strBufComments.toString();
					}
				}
				
				boolean releaseScore = false;
				if (releaseEvaluatedScore)
				{
					releaseScore = true;
				}
				else
				{
					String paramRelease = paramName.replace("score", "release");
					String released = this.request.getParameter(paramRelease);
					
					if ((released != null) && (Integer.parseInt(released) == 1))
					{
						releaseScore = true;
					}
				}
				 				
				/*
				 * create evaluation if evaluation id is -1 and has valid grade id 
				 * update evaluation if evaluation is valid id and has valid grade id
				 */
				
				org.etudes.api.app.jforum.Evaluation evaluation = null;

				if (Integer.parseInt(ids[1]) == -1)
				{
					evaluation = jforumGradeService.newEvaluation(Integer.parseInt(ids[0]), currentSakuserId, sakUserId);
					
					if (noScore) 
					{
						if (comments == null || comments.trim().length() == 0) 
						{
							continue;
						}
					}
					evaluation.setScore(score);
					evaluation.setComments(comments.trim());
					evaluation.setReleased(releaseScore);
					
					gradeEvaluations.add(evaluation);
					
				}
				else if (Integer.parseInt(ids[1]) > 0)
				{
					// check for changes don't update if not changed
					int evaluationId = Integer.parseInt(ids[1]);
					
					org.etudes.api.app.jforum.Evaluation exisEval = exisEvalMap.get(evaluationId);
					
					if (noScore) 
					{
						
						if (comments != null && comments.trim().length() == 0) 
						{

							exisEval.setScore(null);
							exisEval.setComments(null);
							exisEval.setReleased(releaseScore);
							exisEval.setEvaluatedBySakaiUserId(currentSakuserId);
						} 
						else 
						{
							exisEval.setScore(null);
							exisEval.setComments(comments.trim());
							exisEval.setReleased(releaseScore);
							exisEval.setEvaluatedBySakaiUserId(currentSakuserId);
							
						}
						
						gradeEvaluations.add(exisEval);
					} 
					else 
					{
						exisEval.setScore(score);
						if (comments != null && comments.trim().length() != 0)
						{
							exisEval.setComments(comments.trim());
						}
						else
						{
							exisEval.setComments(null);
						}
						exisEval.setReleased(releaseScore);
						exisEval.setEvaluatedBySakaiUserId(currentSakuserId);
						
						gradeEvaluations.add(exisEval);
					}
				}
				
			}
		}
		
		/*	If evaluated and checked "Send to Gradebook", update grade "add to gradebook" to true
		 *  and send the grades to grade book
		 *  If "Send to Gradebook" is unchecked, update grade "add to gradebook" to false
		 *  Remove grades from grade book if grade "add to gradebook" is true before
		 */
		String sendToGradeBook = this.request.getParameter("send_to_grade_book");
		boolean addToGradeBook = false;
		if ((sendToGradeBook != null) && (Integer.parseInt(sendToGradeBook) == 1))
		{
			addToGradeBook = true;
		}
	
		grade.setAddToGradeBook(Boolean.valueOf(addToGradeBook));
		
		category.getEvaluations().clear();
		category.getEvaluations().addAll(gradeEvaluations);
		
		jforumCategoryService.evaluateCategory(category);
		
		// if add to grade option unchecked after saving show the error that there is existing title in the gradebook
		if (addToGradeBook)
		{
			//if (!category.getGrade().isAddToGradeBook())
			org.etudes.api.app.jforum.Grade catGrade = jforumGradeService.getByCategoryId(categoryId);
			if (catGrade != null && !catGrade.isAddToGradeBook()) 
			{
				if (catGrade.getPoints() <= 0)
				{
					this.context.put("errorMessage", I18n.getMessage("Grade.GradeBookAssignmentHasIllegalPointsException"));
				}
				else
				{
					this.context.put("errorMessage", I18n.getMessage("Grade.GradeBookConflictingAssignmentNameException"));
				}
			}
		}
	}
	
	protected void evaluateForumGrades() throws Exception
	{

		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) 
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		/*adjust scores for all evaluations who have scores or if there are replies / posts for a user*/
		String adjustScores = this.request.getParameter("adjust_scores");
		Float adjustScore = null;
		if (adjustScores != null && adjustScores.trim().length() > 0) 
		{
			try 
			{
				adjustScore = Float.parseFloat(adjustScores);
				
				if (adjustScore.floatValue() > 1000) adjustScore = Float.valueOf(1000.0f);
				
				adjustScore = Float.valueOf(((float) Math.round(adjustScore.floatValue() * 100.0f)) / 100.0f);
			} 
			catch (NumberFormatException ne) 
			{			
				if (logger.isWarnEnabled()) logger.warn("evaluateForumGrades(): adjust scores: " + ne);
			}
		}
		
		// deduct scores of all users who have submitted late
		String adjustScoresLate = this.request.getParameter("adjust_scores_late");
		Float adjustScoreLate = null;
		if (adjustScoresLate != null && adjustScoresLate.trim().length() > 0) 
		{
			try 
			{
				adjustScoreLate = Float.parseFloat(adjustScoresLate);
				
				adjustScoreLate = Float.valueOf(((float) Math.round(adjustScoreLate.floatValue() * 100.0f)) / 100.0f);
			} 
			catch (NumberFormatException ne) 
			{			
				if (logger.isWarnEnabled()) logger.warn("evaluateForumGrades(): deduct score late submissions: " + ne);
			}
		}
				
		/*adjust comments will be added to all users/evaluations, if there are replies / posts for a user
		 * and if a user doesn't have any posts but has a score (manually added by the teacher), 
		 * add/append comments*/
		String adjustComments = this.request.getParameter("adjust_comments");
				
		Enumeration<?> paramNames = this.request.getParameterNames();

		String currentSakuserId = UserDirectoryService.getCurrentUser().getId();

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(currentSakuserId);
		
		String releaseAllEvaluated = this.request.getParameter("releaseall");
		boolean releaseEvaluatedScore = false;
		if ((releaseAllEvaluated != null) && (Integer.parseInt(releaseAllEvaluated) == 1))
		{
			releaseEvaluatedScore = true;
		}
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		
		int gradeId = Integer.parseInt(this.request.getParameter("grade_id"));
		int forumId = this.request.getIntParameter("forum_id");
		
		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);
		
		org.etudes.api.app.jforum.Grade grade = forum.getGrade();
	
		if (grade == null || grade.getType() != org.etudes.api.app.jforum.Grade.GradeType.FORUM.getType())
		{
			this.context.put("errorMessage", I18n.getMessage("Grade.ItemNotGradable"));
			return;
		}
		
		List<org.etudes.api.app.jforum.Evaluation> evaluations = null;
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;
		
		List<org.etudes.api.app.jforum.Evaluation> gradeEvaluations = new ArrayList<org.etudes.api.app.jforum.Evaluation>();
		
		evaluations = jforumGradeService.getForumEvaluations(forumId);		
		
		Map<Integer, org.etudes.api.app.jforum.Evaluation> exisEvalMap = new HashMap<Integer, org.etudes.api.app.jforum.Evaluation>();
		
		for(org.etudes.api.app.jforum.Evaluation eval : evaluations)
		{
			exisEvalMap.put(eval.getId(), eval);
		}
		
		while (paramNames.hasMoreElements())
		{
			String paramName = (String) paramNames.nextElement();
			boolean noScore = false;

			if (paramName.endsWith("_score"))
			{
				noScore = false;
				String paramScore = this.request.getParameter(paramName);
				// paramName is in the format gradeId_evalutionId_jforumUserId_score
				String ids[] = paramName.split("_");
				String sakUserId = null;
				//sakUserId_userId
				sakUserId = this.request.getParameter("sakUserId_"+ ids[2]);
				
				Float score = null;
				int totalPostCount = 0;
				try
				{
					String paramTotalPosts = paramName.replace("score", "totalposts");
					String userTotalPosts = this.request.getParameter(paramTotalPosts);
					
					if ((userTotalPosts != null) && (userTotalPosts.trim().length() > 0)) 
					{
						totalPostCount = Integer.parseInt(userTotalPosts.trim());
					}
					
					// late submission
					String paramIsLate = paramName.replace("score", "late");
					String userIsLate = this.request.getParameter(paramIsLate);
					
					if ((paramScore != null) && (paramScore.trim().length() > 0)) 
					{
						score = Float.parseFloat(this.request.getParameter(paramName));
						
						//if (score.floatValue() < 0) score = Float.valueOf(0.0f);
						if (score.floatValue() > 1000) score = Float.valueOf(1000.0f);
						score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
						if (adjustScore != null && JForumUserUtil.isUserActive(sakUserId))
						{
							score = score + adjustScore;
							score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
						}
						
						// deduct score for late posts
						if (adjustScoreLate != null && adjustScoreLate > 0 && userIsLate != null && userIsLate.equalsIgnoreCase("1"))
						{
							score = score - adjustScoreLate;
							score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
						}
					} 
					else 
					{
						// if (totalPostCount > 0 && adjustScore != null && JForumUserUtil.isUserActive(sakUserId)) 
						if (totalPostCount > 0 && JForumUserUtil.isUserActive(sakUserId))
						{
							if (adjustScore != null)
							{
								score = adjustScore.floatValue();
								score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
							}
							
							// deduct score for late posts
							if (adjustScoreLate != null && adjustScoreLate > 0 && userIsLate != null && userIsLate.equalsIgnoreCase("1"))
							{
								if (score != null)
								{
									score = score - adjustScoreLate;
								}
								else
								{
									score = 0 - adjustScoreLate;
								}
								score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
							}
							
							if (score == null)
							{
								noScore = true;
							}
						}
						else
						{
							noScore = true;
						}
					}
				}
				catch (NumberFormatException ne)
				{
					if (logger.isWarnEnabled()) logger.warn("evaluateForumGrades() : " + ne);
					continue;
				}
				String paramComments = paramName.replace("score", "comments");
				String comments = this.request.getParameter(paramComments);
				if (adjustComments != null && adjustComments.trim().length() > 0)
				{
					if ((totalPostCount > 0 || !noScore)&& JForumUserUtil.isUserActive(sakUserId)) 
					{
						StringBuffer strBufComments = new StringBuffer();
						strBufComments.append(comments);
						strBufComments.append("\n");
						strBufComments.append(adjustComments);
						
						comments = strBufComments.toString();
					}
				}
				
				boolean releaseScore = false;
				if (releaseEvaluatedScore)
				{
					releaseScore = true;
				}
				else
				{
					String paramRelease = paramName.replace("score", "release");
					String released = this.request.getParameter(paramRelease);
					
					if ((released != null) && (Integer.parseInt(released) == 1))
					{
						releaseScore = true;
					}
				}
				 				
				/*
				 * create evaluation if evaluation id is -1 and has valid grade id 
				 * update evaluation if evaluation is valid id and has valid grade id
				 */
				
				org.etudes.api.app.jforum.Evaluation evaluation = null;

				if (Integer.parseInt(ids[1]) == -1)
				{
					evaluation = jforumGradeService.newEvaluation(Integer.parseInt(ids[0]), currentSakuserId, sakUserId);
					
					if (noScore) 
					{
						if (comments == null || comments.trim().length() == 0) 
						{
							continue;
						}
					}
					evaluation.setScore(score);
					evaluation.setComments(comments.trim());
					evaluation.setReleased(releaseScore);
					
					gradeEvaluations.add(evaluation);
					
				}
				else if (Integer.parseInt(ids[1]) > 0)
				{
					// check for changes don't update if not changed
					int evaluationId = Integer.parseInt(ids[1]);
					
					org.etudes.api.app.jforum.Evaluation exisEval = exisEvalMap.get(evaluationId);
					
					if (noScore) 
					{
						
						if (comments != null && comments.trim().length() == 0) 
						{

							exisEval.setScore(null);
							exisEval.setComments(null);
							exisEval.setReleased(releaseScore);
							exisEval.setEvaluatedBySakaiUserId(currentSakuserId);
						} 
						else 
						{
							exisEval.setScore(null);
							exisEval.setComments(comments.trim());
							exisEval.setReleased(releaseScore);
							exisEval.setEvaluatedBySakaiUserId(currentSakuserId);
							
						}
						
						gradeEvaluations.add(exisEval);
					} 
					else 
					{
						exisEval.setScore(score);
						if (comments != null && comments.trim().length() != 0)
						{
							exisEval.setComments(comments.trim());
						}
						else
						{
							exisEval.setComments(null);
						}
						exisEval.setReleased(releaseScore);
						exisEval.setEvaluatedBySakaiUserId(currentSakuserId);
						
						gradeEvaluations.add(exisEval);
					}
				}
				
			}
		}
		
		/*	If evaluated and checked "Send to Gradebook", update grade "add to gradebook" to true
		 *  and send the grades to grade book
		 *  If "Send to Gradebook" is unchecked, update grade "add to gradebook" to false
		 *  Remove grades from grade book if grade "add to gradebook" is true before
		 */
		String sendToGradeBook = this.request.getParameter("send_to_grade_book");
		boolean addToGradeBook = false;
		if ((sendToGradeBook != null) && (Integer.parseInt(sendToGradeBook) == 1))
		{
			addToGradeBook = true;
		}
	
		grade.setAddToGradeBook(Boolean.valueOf(addToGradeBook));
		
		forum.getEvaluations().clear();
		forum.getEvaluations().addAll(gradeEvaluations);
		
		jforumForumService.evaluateForum(forum);
		
		// if add to grade option unchecked after saving show the error that there is existing title in the gradebook
		if (addToGradeBook)
		{
			/*
			String gradebookUid = ToolManager.getInstance().getCurrentPlacement().getContext();
			JForumGBService jForumGBService = null;
			jForumGBService = (JForumGBService)ComponentManager.get("org.etudes.api.app.jforum.JForumGBService");
			if (jForumGBService.isAssignmentDefined(gradebookUid, category.getName()))
			{
			}
			*/
			org.etudes.api.app.jforum.Grade forumGrade = jforumGradeService.getByForumId(forumId);
			if (forumGrade != null && !forumGrade.isAddToGradeBook()) 
			{				
				if (forumGrade.getPoints() <= 0)
				{
					this.context.put("errorMessage", I18n.getMessage("Grade.GradeBookAssignmentHasIllegalPointsException"));
				}
				else
				{
					this.context.put("errorMessage", I18n.getMessage("Grade.GradeBookConflictingAssignmentNameException"));
				}				
			}
		}
	}
		
	protected void evaluateTopicGrades() throws Exception
	{

		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) 
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToGrade"));
			this.setTemplateName(TemplateKeys.GRADE_EVAL_NOT_AUTHORIZED);
			return;
		}
		/*adjust scores for all evaluations who have scores or if there are replies / posts for a user*/
		String adjustScores = this.request.getParameter("adjust_scores");
		Float adjustScore = null;
		if (adjustScores != null && adjustScores.trim().length() > 0) 
		{
			try 
			{
				adjustScore = Float.parseFloat(adjustScores);
				
				if (adjustScore.floatValue() > 1000) adjustScore = Float.valueOf(1000.0f);
				
				adjustScore = Float.valueOf(((float) Math.round(adjustScore.floatValue() * 100.0f)) / 100.0f);
			} 
			catch (NumberFormatException ne) 
			{			
				if (logger.isWarnEnabled()) logger.warn("evaluateForumGrades(): adjust scores: " + ne);
			}
		}
		
		// deduct scores of all users who have submitted late
		String adjustScoresLate = this.request.getParameter("adjust_scores_late");
		Float adjustScoreLate = null;
		if (adjustScoresLate != null && adjustScoresLate.trim().length() > 0) 
		{
			try 
			{
				adjustScoreLate = Float.parseFloat(adjustScoresLate);
				
				adjustScoreLate = Float.valueOf(((float) Math.round(adjustScoreLate.floatValue() * 100.0f)) / 100.0f);
			} 
			catch (NumberFormatException ne) 
			{			
				if (logger.isWarnEnabled()) logger.warn("evaluateForumGrades(): deduct score late submissions: " + ne);
			}
		}
		
		/*adjust comments will be added to all users/evaluations, if there are replies / posts for a user
		 * and if a user doesn't have any posts but has a score (manually added by the teacher), 
		 * add/append comments*/
		String adjustComments = this.request.getParameter("adjust_comments");
				
		Enumeration<?> paramNames = this.request.getParameterNames();

		String currentSakuserId = UserDirectoryService.getCurrentUser().getId();

		JForumUserService jforumUserService = (JForumUserService) ComponentManager.get("org.etudes.api.app.jforum.JForumUserService");
		org.etudes.api.app.jforum.User user = jforumUserService.getBySakaiUserId(currentSakuserId);
		
		String releaseAllEvaluated = this.request.getParameter("releaseall");
		boolean releaseEvaluatedScore = false;
		if ((releaseAllEvaluated != null) && (Integer.parseInt(releaseAllEvaluated) == 1))
		{
			releaseEvaluatedScore = true;
		}
		JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
		
		int gradeId = Integer.parseInt(this.request.getParameter("grade_id"));
		int topicId = this.request.getIntParameter("topic_id");
		
		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		org.etudes.api.app.jforum.Topic topic = jforumPostService.getTopic(topicId);
		
		org.etudes.api.app.jforum.Grade grade = topic.getGrade();
	
		if (grade == null || grade.getType() != org.etudes.api.app.jforum.Grade.GradeType.TOPIC.getType())
		{
			this.context.put("errorMessage", I18n.getMessage("Grade.ItemNotGradable"));
			return;
		}
		
		List<org.etudes.api.app.jforum.Evaluation> evaluations = null;
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;
		
		List<org.etudes.api.app.jforum.Evaluation> gradeEvaluations = new ArrayList<org.etudes.api.app.jforum.Evaluation>();
		
		evaluations = jforumGradeService.getTopicEvaluations(topic.getForumId(), topic.getId());	
		
		Map<Integer, org.etudes.api.app.jforum.Evaluation> exisEvalMap = new HashMap<Integer, org.etudes.api.app.jforum.Evaluation>();
		
		for(org.etudes.api.app.jforum.Evaluation eval : evaluations)
		{
			exisEvalMap.put(eval.getId(), eval);
		}
		
		while (paramNames.hasMoreElements())
		{
			String paramName = (String) paramNames.nextElement();
			boolean noScore = false;

			if (paramName.endsWith("_score"))
			{
				noScore = false;
				String paramScore = this.request.getParameter(paramName);
				// paramName is in the format gradeId_evalutionId_jforumUserId_score
				String ids[] = paramName.split("_");
				String sakUserId = null;
				//sakUserId_userId
				sakUserId = this.request.getParameter("sakUserId_"+ ids[2]);
				
				Float score = null;
				int totalPostCount = 0;
				try
				{
					String paramTotalPosts = paramName.replace("score", "totalposts");
					String userTotalPosts = this.request.getParameter(paramTotalPosts);
					
					if ((userTotalPosts != null) && (userTotalPosts.trim().length() > 0)) 
					{
						totalPostCount = Integer.parseInt(userTotalPosts.trim());
					}
					
					// late submission
					String paramIsLate = paramName.replace("score", "late");
					String userIsLate = this.request.getParameter(paramIsLate);
					
					if ((paramScore != null) && (paramScore.trim().length() > 0)) 
					{
						score = Float.parseFloat(this.request.getParameter(paramName));
						
						//if (score.floatValue() < 0) score = Float.valueOf(0.0f);
						if (score.floatValue() > 1000) score = Float.valueOf(1000.0f);
						score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
						if (adjustScore != null && JForumUserUtil.isUserActive(sakUserId))
						{
							score = score + adjustScore;
							score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
						}
						
						// deduct score for late posts
						if (adjustScoreLate != null && adjustScoreLate > 0 && userIsLate != null && userIsLate.equalsIgnoreCase("1"))
						{
							score = score - adjustScoreLate;
							score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
						}
					} 
					else 
					{
						// if (totalPostCount > 0 && adjustScore != null && JForumUserUtil.isUserActive(sakUserId)) 
						if (totalPostCount > 0 && JForumUserUtil.isUserActive(sakUserId))
						{
							if (adjustScore != null)
							{
								score = adjustScore.floatValue();
								score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
							}
							
							// deduct score for late posts
							if (adjustScoreLate != null && adjustScoreLate > 0 && userIsLate != null && userIsLate.equalsIgnoreCase("1"))
							{
								if (score != null)
								{
									score = score - adjustScoreLate;
								}
								else
								{
									score = 0 - adjustScoreLate;
								}
								score = Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
							}
							
							if (score == null)
							{
								noScore = true;
							}
						}
						else
						{
							noScore = true;
						}
					}
				}
				catch (NumberFormatException ne)
				{
					if (logger.isWarnEnabled()) logger.warn("evaluateForumGrades() : " + ne);
					continue;
				}
				String paramComments = paramName.replace("score", "comments");
				String comments = this.request.getParameter(paramComments);
				if (adjustComments != null && adjustComments.trim().length() > 0)
				{
					if ((totalPostCount > 0 || !noScore)&& JForumUserUtil.isUserActive(sakUserId)) 
					{
						StringBuffer strBufComments = new StringBuffer();
						strBufComments.append(comments);
						strBufComments.append("\n");
						strBufComments.append(adjustComments);
						
						comments = strBufComments.toString();
					}
				}
				
				boolean releaseScore = false;
				if (releaseEvaluatedScore)
				{
					releaseScore = true;
				}
				else
				{
					String paramRelease = paramName.replace("score", "release");
					String released = this.request.getParameter(paramRelease);
					
					if ((released != null) && (Integer.parseInt(released) == 1))
					{
						releaseScore = true;
					}
				}
				 				
				/*
				 * create evaluation if evaluation id is -1 and has valid grade id 
				 * update evaluation if evaluation is valid id and has valid grade id
				 */
				
				org.etudes.api.app.jforum.Evaluation evaluation = null;

				if (Integer.parseInt(ids[1]) == -1)
				{
					evaluation = jforumGradeService.newEvaluation(Integer.parseInt(ids[0]), currentSakuserId, sakUserId);
					
					if (noScore) 
					{
						if (comments == null || comments.trim().length() == 0) 
						{
							continue;
						}
					}
					evaluation.setScore(score);
					evaluation.setComments(comments.trim());
					evaluation.setReleased(releaseScore);
					
					gradeEvaluations.add(evaluation);
					
				}
				else if (Integer.parseInt(ids[1]) > 0)
				{
					// check for changes don't update if not changed
					int evaluationId = Integer.parseInt(ids[1]);
					
					org.etudes.api.app.jforum.Evaluation exisEval = exisEvalMap.get(evaluationId);
					
					if (noScore) 
					{
						
						if (comments != null && comments.trim().length() == 0) 
						{

							exisEval.setScore(null);
							exisEval.setComments(null);
							exisEval.setReleased(releaseScore);
							exisEval.setEvaluatedBySakaiUserId(currentSakuserId);
						} 
						else 
						{
							exisEval.setScore(null);
							exisEval.setComments(comments.trim());
							exisEval.setReleased(releaseScore);
							exisEval.setEvaluatedBySakaiUserId(currentSakuserId);
							
						}
						
						gradeEvaluations.add(exisEval);
					} 
					else 
					{
						exisEval.setScore(score);
						if (comments != null && comments.trim().length() != 0)
						{
							exisEval.setComments(comments.trim());
						}
						else
						{
							exisEval.setComments(null);
						}
						exisEval.setReleased(releaseScore);
						exisEval.setEvaluatedBySakaiUserId(currentSakuserId);
						
						gradeEvaluations.add(exisEval);
					}
				}
				
			}
		}
		
		/*	If evaluated and checked "Send to Gradebook", update grade "add to gradebook" to true
		 *  and send the grades to grade book
		 *  If "Send to Gradebook" is unchecked, update grade "add to gradebook" to false
		 *  Remove grades from grade book if grade "add to gradebook" is true before
		 */
		String sendToGradeBook = this.request.getParameter("send_to_grade_book");
		boolean addToGradeBook = false;
		if ((sendToGradeBook != null) && (Integer.parseInt(sendToGradeBook) == 1))
		{
			addToGradeBook = true;
		}
	
		grade.setAddToGradeBook(Boolean.valueOf(addToGradeBook));
		
		topic.getEvaluations().clear();
		topic.getEvaluations().addAll(gradeEvaluations);
		
		jforumPostService.evaluateTopic(topic);
		
		// if add to grade option unchecked after saving show the error that there is existing title in the gradebook
		if (addToGradeBook)
		{
			org.etudes.api.app.jforum.Grade topicGrade = jforumGradeService.getByForumTopicId(topic.getForumId(), topic.getId());
			if (topicGrade != null && !topicGrade.isAddToGradeBook())
			{
				if (topicGrade.getPoints() <= 0)
				{
					this.context.put("errorMessage", I18n.getMessage("Grade.GradeBookAssignmentHasIllegalPointsException"));
				}
				else
				{
					this.context.put("errorMessage", I18n.getMessage("Grade.GradeBookConflictingAssignmentNameException"));
				}
			}
		}
	}
	
	/**
	 * get forum group users
	 * @param forum			Forum
	 * @param site			Site
	 * @param evaluationDao	evaluationDao
	 * @param evaluations	evaluations
	 * @throws Exception
	 */
	protected void getForumGroupUsers(Forum forum, List<Evaluation> evaluations) throws Exception
	{
		if (forum.getAccessType() == Forum.ACCESS_GROUPS)
		{
			EvaluationDAO evaluationDao = DataAccessDriver.getInstance().newEvaluationDAO();
			if ((forum.getGroups() != null) && (forum.getGroups().size() > 0))
			{
				Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
				
				// get forum group users
				Collection sakaiSiteGroups = site.getGroups();
				List forumGroupsIds = forum.getGroups();
				List<String> sakaiSiteGroupUserIds = new ArrayList<String>();
				Map<String, Group> sakaiUserGroups = new HashMap<String, Group>();
				for (Iterator i = sakaiSiteGroups.iterator(); i.hasNext();)
				{
					Group group = (Group) i.next();
					
					if (forumGroupsIds.contains(group.getId()))
					{
						Set members = group.getMembers();
						for (Iterator iter = members.iterator(); iter.hasNext();)
						{
							Member member = (Member) iter.next();						
							sakaiSiteGroupUserIds.add(member.getUserId());
							
							sakaiUserGroups.put(member.getUserId(), group);
						}
					}
				}
				
				// show users belong to the forum group
				for (Iterator<Evaluation> i = evaluations.iterator(); i.hasNext();)
				{
					Evaluation evaluation = i.next();
					if (!sakaiSiteGroupUserIds.contains(evaluation.getSakaiUserId()))
					{
						if (evaluation.getId() > 0)
						{
							evaluationDao.delete(evaluation.getId());
						}
						i.remove();
					}
					else
					{
						Group group = sakaiUserGroups.get(evaluation.getSakaiUserId());
						evaluation.setUserSakaiGroupName(group.getTitle());
					}
				}
	
				
				this.context.put("sakaiSiteGroupUserIds", sakaiSiteGroupUserIds);
			}
			else
			{
				for (Iterator<Evaluation> i = evaluations.iterator(); i.hasNext();)
				{
					Evaluation evaluation = i.next();
					if (evaluation.getId() > 0)
					{
						evaluationDao.delete(evaluation.getId());
					}
					i.remove();
				}
				this.context.put("sakaiSiteGroupUserIds", new ArrayList<String>());
			}
		}
	}
	
	/**
	 * Gets the forum group users
	 * 
	 * @param forum
	 */
	protected void getForumGroupUsers(org.etudes.api.app.jforum.Forum forum)
	{
		if (forum.getAccessType() == Forum.ACCESS_GROUPS)
		{
			if ((forum.getGroups() != null) && (forum.getGroups().size() > 0))
			{
				Site site;
				try
				{
					org.etudes.api.app.jforum.Category category = forum.getCategory();
					site = SiteService.getSite(category.getContext());
				}
				catch (IdUnusedException e)
				{
					return;
				}
				
				// get forum group users
				Collection sakaiSiteGroups = site.getGroups();
				List forumGroupsIds = forum.getGroups();
				List<String> sakaiSiteGroupUserIds = new ArrayList<String>();

				for (Iterator i = sakaiSiteGroups.iterator(); i.hasNext();)
				{
					Group group = (Group) i.next();
					
					if (forumGroupsIds.contains(group.getId()))
					{
						Set members = group.getMembers();
						for (Iterator iter = members.iterator(); iter.hasNext();)
						{
							Member member = (Member) iter.next();						
							sakaiSiteGroupUserIds.add(member.getUserId());							
						}
					}
				}
				
				this.context.put("sakaiSiteGroupUserIds", sakaiSiteGroupUserIds);
			}
			else
			{
				this.context.put("sakaiSiteGroupUserIds", new ArrayList<String>());
			}
		}
	}
	
	/**
	 * get evaluation sort
	 * 
	 * @param sortColumn	Sort column
	 * 
	 * @param sortDirection	Sort direction
	 * 
	 * @return	The evaluation sort
	 */
	protected org.etudes.api.app.jforum.Evaluation.EvaluationsSort getSortEvaluations(String sortColumn, String sortDirection)
	{
		org.etudes.api.app.jforum.Evaluation.EvaluationsSort evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;

		if (sortColumn != null && sortDirection != null)
		{
			if (sortColumn.equals("name") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_a;
			}
			else if (sortColumn.equals("name") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_name_d;
			}
			else if (sortColumn.equals("posts") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.total_posts_a;
			}
			else if (sortColumn.equals("posts") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.total_posts_d;
			}
			else if (sortColumn.equals("scores") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.scores_a;
			}
			else if (sortColumn.equals("scores") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.scores_d;
			}
			else if (sortColumn.equals("grouptitle") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.group_title_a;
			}
			else if (sortColumn.equals("grouptitle") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.group_title_d;
			}
			else if (sortColumn.equals("lastpostdate") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_post_date_a;
			}
			else if (sortColumn.equals("lastpostdate") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.last_post_date_d;
			}
			else if (sortColumn.equals("firstpostdate") && sortDirection.equals("a"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.first_post_date_a;
			}
			else if (sortColumn.equals("firstpostdate") && sortDirection.equals("d"))
			{
				evalSort = org.etudes.api.app.jforum.Evaluation.EvaluationsSort.first_post_date_d;
			}
		}
		
		return evalSort;
	}
	
	/**
	 * sort evaluations for group titles
	 * @param evalSort		evalution sort
	 * @param evaluations	evalutions
	 */
	protected void sortEvaluationGroups(EvaluationDAO.EvaluationsSort evalSort, List<Evaluation> evaluations)
	{
		if (evalSort == EvaluationsSort.group_title_a)
		{
			Collections.sort(evaluations, new Comparator<Evaluation>()
			{
				public int compare(Evaluation eval1, Evaluation eval2)
				{
					return eval1.getUserSakaiGroupName().compareTo(eval2.getUserSakaiGroupName());
				}
			});
		} 
		else if (evalSort == EvaluationsSort.group_title_d)
		{
			Collections.sort(evaluations, new Comparator<Evaluation>()
			{
				public int compare(Evaluation eval1, Evaluation eval2)
				{
					return -1 * eval1.getUserSakaiGroupName().compareTo(eval2.getUserSakaiGroupName());
				}
			});
		}
	}
	
}
