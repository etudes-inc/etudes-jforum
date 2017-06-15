/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/view/admin/CategoryAction.java $ 
 * $Id: CategoryAction.java 10882 2015-05-19 00:37:57Z murthyt $ 
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
package org.etudes.jforum.view.admin;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.JForumAccessException;
import org.etudes.api.app.jforum.JForumCategoryForumsExistingException;
import org.etudes.api.app.jforum.JForumCategoryService;
import org.etudes.api.app.jforum.JForumGBService;
import org.etudes.api.app.jforum.JForumGradeService;
import org.etudes.api.app.jforum.JForumGradesModificationException;
import org.etudes.api.app.jforum.JForumItemEvaluatedException;
import org.etudes.api.app.jforum.JForumService.Type;
import org.etudes.gradebook.api.GradebookItemType;
import org.etudes.jforum.JForum;
import org.etudes.jforum.dao.CategoryDAO;
import org.etudes.jforum.dao.DataAccessDriver;
import org.etudes.jforum.dao.EvaluationDAO;
import org.etudes.jforum.dao.GradeDAO;
import org.etudes.jforum.dao.generic.CourseCategoryDAO;
import org.etudes.jforum.entities.Category;
import org.etudes.jforum.entities.Evaluation;
import org.etudes.jforum.entities.Grade;
import org.etudes.jforum.repository.ForumRepository;
import org.etudes.jforum.util.I18n;
import org.etudes.jforum.util.SafeHtml;
import org.etudes.jforum.util.TreeGroup;
import org.etudes.jforum.util.date.DateUtil;
import org.etudes.jforum.util.preferences.ConfigKeys;
import org.etudes.jforum.util.preferences.SakaiSystemGlobals;
import org.etudes.jforum.util.preferences.SystemGlobals;
import org.etudes.jforum.util.preferences.TemplateKeys;
import org.etudes.jforum.util.user.JForumUserUtil;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.cover.UserDirectoryService;

/**
 * ViewHelper for category administration.
 * 
 * @author Rafael Steil
 */
/**
 * 8/10/05 - Mallika - adding code to delete from CourseCategory when category is deleted
 * 8/10/05 - Mallika - adding code so only categories for this course appear even on the admin panel
 * 9/13/05 - Mallika - adding code to get Facilitator group so all categories added would be accessible to them
 * 10/20/05 - Murthy - trim() added to category name in the insertSave()and editSave() methods,  
 * 						"moderated" corrected to "moderate" in the insertSave() method
 * 9/13/05 - Mallika - changed del msg to not show id
 * 1/5/06 - Mallika - added code to show warning for delete cat
 * 1/6/06 - Mallika - correcting bug in warning code, warning used to always show
 */
public class CategoryAction extends AdminCommand 
{
	private CategoryDAO cm = DataAccessDriver.getInstance().newCategoryDAO();
	private CourseCategoryDAO ccm = DataAccessDriver.getInstance().newCourseCategoryDAO();
	private GradeDAO gm = DataAccessDriver.getInstance().newGradeDAO();
	private EvaluationDAO evaldao = DataAccessDriver.getInstance().newEvaluationDAO();
	
	private static Log logger = LogFactory.getLog(CategoryAction.class);
	
	// Listing
	public void list() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}

		//this.context.put("categories", ForumCommon.getAllCategoriesAndForums(true));
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		List<org.etudes.api.app.jforum.Category> categories = jforumCategoryService.getManageCategories(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		
		this.context.put("categories", categories);
		
		this.context.put("repository", new ForumRepository());
		
		this.context.put("viewTitleManageCatg", true);
		
		//context.put("calendarDateTimeFormat", SakaiSystemGlobals.getValue(ConfigKeys.CALENDAR_DATE_TIME_FORMAT));

		this.setTemplateName(TemplateKeys.CATEGORY_LIST);
	}
	
	// One more, one more
	public void insert() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		this.context.put("groups", new TreeGroup().getNodes());
		this.context.put("selectedList", new ArrayList());
		this.setTemplateName(TemplateKeys.CATEGORY_INSERT);
		this.context.put("action", "insertSave");
		this.context.put("viewTitleManageCatg", true);
		
		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("enableGrading", Boolean.TRUE);
		}
	}
	
	// Edit
	public void edit() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}

		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");

		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(this.request.getIntParameter("category_id"));
		this.context.put("category", category);
		
		if (category == null)
		{
			if (logger.isErrorEnabled())
			{
				logger.error("category with id " + this.request.getIntParameter("category_id") + " not found");
			}
			this.context.put("message", I18n.getMessage("Forums.Category.NotFound"));
			this.setTemplateName(TemplateKeys.ITEM_NOT_FOUND_MESSAGE);
			return;
		}
		//this.context.put("category", this.cm.selectById(this.request.getIntParameter("category_id")));
		//this.context.put("grade", this.gm.selectByCategoryId(this.request.getIntParameter("category_id")));
		this.setTemplateName(TemplateKeys.CATEGORY_EDIT);
		this.context.put("action", "editSave");
		this.context.put("viewTitleManageCatg", true);

		//int forumDatesCount = DataAccessDriver.getInstance().newForumDAO().getForumDatesCount(this.request.getIntParameter("category_id"));
		//this.context.put("forumDates", ((forumDatesCount > 0) ? true : false));
		boolean forumDates = ForumRepository.isCategoryForumDates(category.getId());
		this.context.put("forumDates", forumDates);
				
		//int topicDatesCount = DataAccessDriver.getInstance().newTopicDAO().getTopicDatesCountByCategory(this.request.getIntParameter("category_id"));
		//this.context.put("topicDates", ((topicDatesCount > 0) ? true : false));
		boolean topicDates = ForumRepository.isCategoryForumTopicDates(category.getId());
		this.context.put("topicDates", topicDates);
		//context.put("calendarDateTimeFormat", SakaiSystemGlobals.getValue(ConfigKeys.CALENDAR_DATE_TIME_FORMAT));
		
		if (!category.isGradable())
		{
			JForumGradeService jforumGradeService = (JForumGradeService) ComponentManager.get("org.etudes.api.app.jforum.JForumGradeService");
			int categoryGradableForumCount = jforumGradeService.getCategoryGradableForumsCount(category.getId());
			
			this.context.put("categoryGradableForums", ((categoryGradableForumCount > 0) ? true : false));
		}

		Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
		if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
		{
			this.context.put("enableGrading", Boolean.TRUE);
		}
	}
	
	//  Save information
	public void editSave() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}

		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");

		org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(this.request.getIntParameter("categories_id"));

		category.setModifiedBySakaiUserId(UserDirectoryService.getCurrentUser().getId());

		category.setTitle(SafeHtml.escapeJavascript(this.request.getParameter("category_name").trim()));

		if (category.getAccessDates() != null)
		{

			// open, due, allow until dates
			String startDateParam = this.request.getParameter("start_date");
			if (startDateParam != null && startDateParam.trim().length() > 0)
			{
				Date startDate;
				try
				{
					startDate = DateUtil.getDateFromString(startDateParam.trim());
				}
				catch (ParseException e)
				{
					this.context.put("errorMessage", I18n.getMessage("Forums.Forum.DateParseError"));
					this.edit();
					return;
				}
				category.getAccessDates().setOpenDate(startDate);
				
				String hideUntilOpen = this.request.getParameter("hide_until_open");
				if (hideUntilOpen != null && "1".equals(hideUntilOpen))
				{
					category.getAccessDates().setHideUntilOpen(Boolean.TRUE);
				}
				else
				{
					category.getAccessDates().setHideUntilOpen(Boolean.FALSE);
				}
			}
			else
			{
				category.getAccessDates().setOpenDate(null);
				category.getAccessDates().setHideUntilOpen(Boolean.FALSE);
			}

			String endDateParam = this.request.getParameter("end_date");
			if (endDateParam != null && endDateParam.trim().length() > 0)
			{
				Date endDate;
				try
				{
					endDate = DateUtil.getDateFromString(endDateParam.trim());
				}
				catch (ParseException e)
				{
					this.context.put("errorMessage", I18n.getMessage("Forums.Forum.DateParseError"));
					this.request.addParameter("category_id", String.valueOf(category.getId()));
					this.edit();
					return;
				}
				category.getAccessDates().setDueDate(endDate);

				/*String lockCategory = this.request.getParameter("lock_category");

				if (lockCategory != null && "1".equals(lockCategory))
				{
					category.getAccessDates().setLocked(true);
				}
				else
				{
					category.getAccessDates().setLocked(false);
				}*/
			}
			else
			{
				category.getAccessDates().setDueDate(null);
				//category.getAccessDates().setLocked(false);
			}
			
			// allow until
			String allowUntilDateParam = this.request.getParameter("allow_until_date");
			if (allowUntilDateParam != null && allowUntilDateParam.trim().length() > 0)
			{
				Date allowUntilDate;
				try
				{
					allowUntilDate = DateUtil.getDateFromString(allowUntilDateParam.trim());
				}
				catch (ParseException e)
				{
					this.context.put("errorMessage", I18n.getMessage("Forums.Forum.DateParseError"));
					this.edit();
					return;
				}
				category.getAccessDates().setAllowUntilDate(allowUntilDate);			
			}
			else
			{
				category.getAccessDates().setAllowUntilDate(null);
			}
		}

		boolean isGradable = "1".equals(this.request.getParameter("grade_category"));

		// grades
		category.setGradable(Boolean.valueOf(isGradable));

		if (category.isGradable())
		{
			org.etudes.api.app.jforum.Grade grade = category.getGrade();
			if (grade == null)
			{
				grade = jforumCategoryService.newGrade(category);
			}

			grade.setContext(ToolManager.getCurrentPlacement().getContext());
			try
			{
				Float points = Float.parseFloat(this.request.getParameter("point_value"));

				if (points.floatValue() < 0)
					points = Float.valueOf(0.0f);
				if (points.floatValue() > 1000)
					points = Float.valueOf(1000.0f);
				points = Float.valueOf(((float) Math.round(points.floatValue() * 100.0f)) / 100.0f);

				grade.setPoints(points);
			}
			catch (NumberFormatException ne)
			{
				grade.setPoints(0f);
			}

			String minPostsRequired = this.request.getParameter("min_posts_required");

			if ((minPostsRequired != null) && ("1".equals(minPostsRequired)))
			{
				try
				{
					grade.setMinimumPostsRequired(true);
					int minimumPosts = this.request.getIntParameter("min_posts");
					grade.setMinimumPosts(minimumPosts);
				}
				catch (NumberFormatException ne)
				{
					grade.setMinimumPosts(0);
				}
			}
			else
			{
				grade.setMinimumPostsRequired(false);
				grade.setMinimumPosts(0);
			}

			String sendToGradebook = this.request.getParameter("send_to_grade_book");
			boolean addToGradeBook = false;
			if ((sendToGradebook != null) && (Integer.parseInt(sendToGradebook) == 1))
			{
				addToGradeBook = true;
			}
			
			// check for duplicate titles in etudes gradebook tool
			Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
			if (((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) == null)) && (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
			{
				org.etudes.gradebook.api.GradebookService etudesGradebookService = (org.etudes.gradebook.api.GradebookService)ComponentManager.get("org.etudes.gradebook.api.GradebookService");
				
				// check for current item
				if (!etudesGradebookService.isTitleDefined(category.getContext(), UserDirectoryService.getCurrentUser().getId(), category.getTitle(), String.valueOf(category.getId()), GradebookItemType.category))
				{
					if (!etudesGradebookService.isTitleAvailable(category.getContext(), UserDirectoryService.getCurrentUser().getId(), category.getTitle()))
					{
						addToGradeBook = false;
					}
				}
			}
			
			grade.setAddToGradeBook(Boolean.valueOf(addToGradeBook));
		}
		else
		{
			category.setGrade(null);
		}

		try
		{
			jforumCategoryService.modifyCategory(category);
			
			// if add to grade option unchecked after saving show the error that there is existing title in the gradebook
			String sendToGradebook = this.request.getParameter("send_to_grade_book");
			boolean addToGradeBook = false;
			if ((sendToGradebook != null) && (Integer.parseInt(sendToGradebook) == 1))
			{
				addToGradeBook = true;
			}			
			if (category.isGradable() && category.getGrade() != null && addToGradeBook)
			{
				/*JForumGBService jForumGBService = null;
				jForumGBService = (JForumGBService)ComponentManager.get("org.etudes.api.app.jforum.JForumGBService");

				String gradebookUid = ToolManager.getInstance().getCurrentPlacement().getContext();
				if (jForumGBService != null && jForumGBService.isAssignmentDefined(gradebookUid, category.getTitle()))
				{
					this.context.put("errorMessage", I18n.getMessage("Grade.AddEditCategoryGradeBookConflictingAssignmentNameException"));
				}*/
				
				if (!category.getGrade().isAddToGradeBook())
				{
					if (category.getGrade().getPoints() <= 0)
					{
						this.context.put("errorMessage", I18n.getMessage("Grade.AddEditCategoryGradeBookAssignmentHasIllegalPointsException"));
					}
					else
					{
						this.context.put("errorMessage", I18n.getMessage("Grade.AddEditCategoryGradeBookConflictingAssignmentNameException"));
					}
					this.request.addParameter("category_id", String.valueOf(category.getId()));
					this.edit();
					return;
				}
			}
		}
		catch (JForumAccessException e)
		{
			// already check access
		}
		catch (JForumGradesModificationException e)
		{
			JForum.enableCancelCommit();
			this.context.put("errorMessage", I18n.getMessage("Category.Form.CannotEditCategoryGrade"));
			this.request.addParameter("category_id", String.valueOf(category.getId()));
			this.edit();
			return;
		}
		
		// auto save navigation
		autoSaveNavigation();
	}
	
	public void delete() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();

		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}

		String ids[] = this.request.getParameterValues("categories_id");
		List<String> errors = new ArrayList<String>();
		StringBuffer catNameList = new StringBuffer();
		StringBuffer gradedCatNameList = new StringBuffer();
		boolean errFlag = false;
		boolean errGradedFlag = false;

		if (ids != null)
		{
			JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
			
			for (int i = 0; i < ids.length; i++)
			{
				int categoryId = Integer.parseInt(ids[i]);
				
				try
				{
					jforumCategoryService.deleteCategory(categoryId, UserDirectoryService.getCurrentUser().getId());
				}
				catch (JForumAccessException e)
				{
					// already checked the access
				}
				catch (JForumCategoryForumsExistingException e)
				{
					org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);
					catNameList.append(category.getTitle());
					catNameList.append(",");
					errFlag = true;
				}
				catch (JForumItemEvaluatedException e)
				{
					org.etudes.api.app.jforum.Category category = jforumCategoryService.getCategory(categoryId);
					catNameList.append(category.getTitle());
					catNameList.append(",");
					errFlag = true;
				}
				/*if (this.cm.canDelete(Integer.parseInt(ids[i])))
				{
					int id = Integer.parseInt(ids[i]);

					Category c = this.cm.selectById(id);

					// remove grades associated with category
					if (c.isGradeCategory())
					{
						Grade grade = this.gm.selectByCategoryId(c.getId());

						int evalCount = this.evaldao.selectEvaluationsCountByGradeId(grade.getId());

						if (evalCount > 0)
						{
							gradedCatNameList.append(c.getName());
							gradedCatNameList.append(",");
							errGradedFlag = true;
							continue;
						}

						// remove entry from gradebook
						if (grade.isAddToGradeBook())
						{
							removeEntryFromGradeBook(grade);
						}

						// delete grade
						this.gm.delete(grade.getId());
					}

					this.cm.delete(id);

					this.ccm.delete(id);

					ForumRepository.removeCourseCategory(c);
				}
				else
				{

					int id = Integer.parseInt(ids[i]);
					Category c = this.cm.selectById(id);
					catNameList.append(c.getName());
					catNameList.append(",");
					errFlag = true;
				}*/
			}
		}
		if (errFlag == true)
		{
			String catNameListStr = catNameList.toString();
			if (catNameListStr.endsWith(","))
			{
				catNameListStr = catNameListStr.substring(0, catNameListStr.length() - 1);
			}
			errors.add(I18n.getMessage(I18n.CANNOT_DELETE_CATEGORY, new Object[] { catNameListStr }));
		}

		if (errGradedFlag == true)
		{
			String gradedCatNameListStr = gradedCatNameList.toString();
			if (gradedCatNameListStr.endsWith(","))
			{
				gradedCatNameListStr = gradedCatNameListStr.substring(0, gradedCatNameListStr.length() - 1);
			}
			errors.add(I18n.getMessage(I18n.CANNOT_DELETE_GRADED_CATEGORY, new Object[] { gradedCatNameListStr }));
		}

		if (errors.size() > 0)
		{
			this.context.put("errorMessage", errors);
		}

		this.list();
	}
	
	// A new one
	public void insertSave() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		
		org.etudes.api.app.jforum.Category category = jforumCategoryService.newCategory();
		
		category.setTitle(SafeHtml.escapeJavascript(this.request.getParameter("category_name").trim()));
		category.setContext(ToolManager.getCurrentPlacement().getContext());
		category.setCreatedBySakaiUserId(UserDirectoryService.getCurrentUser().getId());
	
		boolean isGradable = "1".equals(this.request.getParameter("grade_category"));
		
		// open, due, allow until dates
		String startDateParam = this.request.getParameter("start_date");
		if (startDateParam != null && startDateParam.trim().length() > 0)
		{
			Date startDate;
			try
			{
				startDate = DateUtil.getDateFromString(startDateParam.trim());
			}
			catch (ParseException e)
			{
				this.context.put("errorMessage", I18n.getMessage("Forums.Forum.DateParseError"));
				this.insert();
				return;
			}
			category.getAccessDates().setOpenDate(startDate);
			
			String hideUntilOpen = this.request.getParameter("hide_until_open");
			if (hideUntilOpen != null && "1".equals(hideUntilOpen))
			{
				category.getAccessDates().setHideUntilOpen(Boolean.TRUE);
			}
			else
			{
				category.getAccessDates().setHideUntilOpen(Boolean.FALSE);
			}
		}
		else
		{
			category.getAccessDates().setOpenDate(null);
			category.getAccessDates().setHideUntilOpen(Boolean.FALSE);
		}

		// due date
		String endDateParam = this.request.getParameter("end_date");
		if (endDateParam != null && endDateParam.trim().length() > 0)
		{
			Date endDate;
			try
			{
				endDate = DateUtil.getDateFromString(endDateParam.trim());
			}
			catch (ParseException e)
			{
				this.context.put("errorMessage", I18n.getMessage("Forums.Forum.DateParseError"));
				this.insert();
				return;
			}
			category.getAccessDates().setDueDate(endDate);
			//String lockCategory = this.request.getParameter("lock_category");
			/*if (lockCategory != null && "1".equals(lockCategory))
			{
				category.getAccessDates().setLocked(Boolean.TRUE);
			}
			else
			{
				category.getAccessDates().setLocked(Boolean.FALSE);
			}*/
		}
		else
		{
			category.getAccessDates().setDueDate(null);
			//category.getAccessDates().setLocked(Boolean.FALSE);
		}
		
		// allow until
		String allowUntilDateParam = this.request.getParameter("allow_until_date");
		if (allowUntilDateParam != null && allowUntilDateParam.trim().length() > 0)
		{
			Date allowUntilDate;
			try
			{
				allowUntilDate = DateUtil.getDateFromString(allowUntilDateParam.trim());
			}
			catch (ParseException e)
			{
				this.context.put("errorMessage", I18n.getMessage("Forums.Forum.DateParseError"));
				this.insert();
				return;
			}
			category.getAccessDates().setAllowUntilDate(allowUntilDate);			
		}
		else
		{
			category.getAccessDates().setAllowUntilDate(null);
		}
		
		// grades
		category.setGradable(Boolean.valueOf(isGradable));
		
		if (category.isGradable())
		{
			org.etudes.api.app.jforum.Grade grade = category.getGrade();
						
			grade.setContext(ToolManager.getCurrentPlacement().getContext());
			try 
			{
				Float points = Float.parseFloat(this.request.getParameter("point_value"));
				
				if (points.floatValue() < 0) points = Float.valueOf(0.0f);
				if (points.floatValue() > 1000) points = Float.valueOf(1000.0f);
				points = Float.valueOf(((float) Math.round(points.floatValue() * 100.0f)) / 100.0f);
				
				grade.setPoints(points);
			} 
			catch (NumberFormatException ne) 
			{
				grade.setPoints(0f);
			}
			
			String minPostsRequired = this.request.getParameter("min_posts_required");
			
			if ((minPostsRequired != null) && ("1".equals(minPostsRequired)))
			{
				try 
				{
					grade.setMinimumPostsRequired(true);
					int minimumPosts = this.request.getIntParameter("min_posts");
					grade.setMinimumPosts(minimumPosts);				
				} 
				catch (NumberFormatException ne) 
				{
					grade.setMinimumPosts(0);
				}
			}
			
			String sendToGradebook = this.request.getParameter("send_to_grade_book");
			boolean addToGradeBook = false;
			if ((sendToGradebook != null) && (Integer.parseInt(sendToGradebook) == 1))
			{
				addToGradeBook = true;
			}
			
			if (addToGradeBook)
			{
				// check for duplicate titles in etudes gradebook tool
				Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
				if (((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) == null)) && (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
				{
					org.etudes.gradebook.api.GradebookService etudesGradebookService = (org.etudes.gradebook.api.GradebookService)ComponentManager.get("org.etudes.gradebook.api.GradebookService");
					
					if (!etudesGradebookService.isTitleAvailable(category.getContext(), UserDirectoryService.getCurrentUser().getId(), category.getTitle()))
					{
						addToGradeBook = false;
					}
				}
			}
			grade.setAddToGradeBook(Boolean.valueOf(addToGradeBook));			
		}
		
		try
		{
			jforumCategoryService.createCategory(category);
			
			// if add to grade option unchecked after saving show the error that there is existing title in the gradebook
			String sendToGradebook = this.request.getParameter("send_to_grade_book");
			boolean addToGradeBook = false;
			if ((sendToGradebook != null) && (Integer.parseInt(sendToGradebook) == 1))
			{
				addToGradeBook = true;
			}
			
			if (category.isGradable() && category.getGrade() != null && addToGradeBook)
			{
				if (!category.getGrade().isAddToGradeBook())
				{
					if (category.getGrade().getPoints() <= 0)
					{
						this.context.put("errorMessage", I18n.getMessage("Grade.AddEditCategoryGradeBookAssignmentHasIllegalPointsException"));
					}
					else
					{
						this.context.put("errorMessage", I18n.getMessage("Grade.AddEditCategoryGradeBookConflictingAssignmentNameException"));
					}
					this.request.addParameter("category_id", String.valueOf(category.getId()));
					this.edit();
					return;
				}
			}
		}
		catch (JForumAccessException e)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		// auto save navigation
		autoSaveNavigation();
	}
	
	public void up() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		this.processOrdering(true);
	}
	
	public void down() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		this.processOrdering(false);
	}
	
	/**
	 * Change the order of the category
	 * 
	 * @throws Exception
	 */
	public void reorder() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		int navCatId = this.request.getIntParameter("navid");
		
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		
		//Category reorderCategory = ForumRepository.getCategory(navCatId);
		
		org.etudes.api.app.jforum.Category reorderCategory = jforumCategoryService.getCategory(navCatId);
		
		if (reorderCategory != null)
		{
			//Category toChange = new Category(reorderCategory, true);
			
			//List categories = ForumRepository.getUserAllCourseCategories();
			List<org.etudes.api.app.jforum.Category> categories = jforumCategoryService.getManageCategories(ToolManager.getInstance().getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
	
			int posToMove = this.request.getIntParameter("catreorder_"+ navCatId);
			
			if (posToMove <= categories.size())
			{
				//Category otherCategory = new Category((Category)categories.get(index - 1), true);
				//this.cm.updateOrder(toChange, otherCategory);
				
				//org.etudes.api.app.jforum.Category otherCategory = categories.get(index - 1);
				jforumCategoryService.modifyOrder(reorderCategory.getId(), posToMove, UserDirectoryService.getCurrentUser().getId());
			}
		}
		
		this.list();		
	}
	
	private void processOrdering(boolean up) throws Exception
	{
		Category toChange = new Category(ForumRepository.getCategory(Integer.parseInt(
				this.request.getParameter("category_id"))), true);
		
		List categories = ForumRepository.getUserAllCourseCategories();
		
		int index = categories.indexOf(toChange);
		if (index == -1 || (up && index == 0) || (!up && index + 1 == categories.size())) {
			this.list();
			return;
		}
		
		if (up) {
			// Get the category which comes *before* the category we want to change
			Category otherCategory = new Category((Category)categories.get(index - 1), true);
			this.cm.setOrderUp(toChange, otherCategory);
		}
		else {
			// Get the category which comes *after* the category we want to change
			Category otherCategory = new Category((Category)categories.get(index + 1), true);
			this.cm.setOrderDown(toChange, otherCategory);
		}
		
		//ForumRepository.reloadCategory(toChange);
		ForumRepository.reloadCourseCategory(toChange);
		this.list();
	}
	
	/**
	 * Apply changes made to category list.
	 * @throws Exception
	 */
	public void applyChanges() throws Exception
	{
		
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		String actionMode = JForum.getRequest().getParameter("actionMode");
		
		if ((actionMode != null))
		{
			//if (JForum.getRequest().getParameter("deleteCategories") != null)
			if (actionMode.trim().equalsIgnoreCase("deleteCategories"))
			{
				this.delete();
			}
			// else if (JForum.getRequest().getParameter("saveCategories") != null)
			else if (actionMode.trim().equalsIgnoreCase("saveCategories"))
			{
				this.saveCategoryDates();
				//this.list();
				String navCatId = this.request.getParameter("navid");			
				
				if (navCatId == null || (navCatId.trim().length() == 0))
				{
					autoSaveNavigation();
				}
				else
				{
					try
					{
						String navTo = this.request.getParameter("autosavenav");
										
						if (navTo != null)
						{
							if (navTo.trim().equalsIgnoreCase("catedit"))
							{
								String path = this.request.getContextPath() +"/adminCategories/edit/"+ navCatId +SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);;
								JForum.setRedirect(path);
								return;
							}
							else if (navTo.trim().equalsIgnoreCase("catreorder"))
							{
								this.reorder();
								return;
							}
						}
						autoSaveNavigation();
					}
					catch (NumberFormatException e)
					{
						autoSaveNavigation();
					}				
				}
			}
		}
	}
	
	/**
	 * save categiry start and end dates
	 */
	protected void saveCategoryDates() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		List<String> errors = new ArrayList<String>();
		boolean errFlag = false;
		StringBuffer categoryNameList = new StringBuffer();
		
		Enumeration<?> paramNames = this.request.getParameterNames();
		
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		List<org.etudes.api.app.jforum.Category> existingCategories = jforumCategoryService.getManageCategories(ToolManager.getInstance().getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());
		
		Map<Integer, org.etudes.api.app.jforum.Category> exisCatMap = new HashMap<Integer, org.etudes.api.app.jforum.Category>();
		for (org.etudes.api.app.jforum.Category exisCat : existingCategories)
		{
			exisCatMap.put(Integer.valueOf(exisCat.getId()), exisCat);
		}
		
		while (paramNames.hasMoreElements())
		{
			String paramName = (String) paramNames.nextElement();

			if (paramName.startsWith("startdate_"))
			{
				// paramName is in the format startdate_forumId
				String id[] = paramName.split("_");
				String categoryId = null;
				categoryId = id[1];
				
				org.etudes.api.app.jforum.Category existingCategory = exisCatMap.get(Integer.valueOf(categoryId));
				
				if (existingCategory ==  null)
				{
					continue;
				}
				
				//startdate_forumId
				String startDateParam = null;				
				startDateParam = this.request.getParameter("startdate_"+ id[1]);
				
				Date startDate = null;
				Boolean hideUntilOpen = Boolean.FALSE;
				if (startDateParam != null && startDateParam.trim().length() > 0)
				{
					try
					{
						startDate = DateUtil.getDateFromString(startDateParam.trim());
						
						//hideuntilopen_categoryId
						String hideUntilOpenStr = this.request.getParameter("hideuntilopen_"+ id[1]);
						if (hideUntilOpenStr != null && "1".equals(hideUntilOpenStr))
						{
							hideUntilOpen = Boolean.TRUE;
						}
					} 
					catch (ParseException e)
					{
						errFlag = true;
						categoryNameList.append(existingCategory.getTitle());
						categoryNameList.append(",");
						continue;
					}
				}
				
				//enddate_categoryId
				String endDateParam = null;
				endDateParam = this.request.getParameter("enddate_"+ id[1]);
				
				Date endDate = null;
				//boolean lockCategory = false;
				if (endDateParam != null && endDateParam.trim().length() > 0)
				{
					try
					{
						endDate = DateUtil.getDateFromString(endDateParam.trim());
					} 
					catch (ParseException e)
					{
						errFlag = true;
						categoryNameList.append(existingCategory.getTitle());
						categoryNameList.append(",");
						continue;
					}
										
					//lockcategory_categoryId				
					/*String lockCategoryStr = this.request.getParameter("lockcategory_"+ id[1]);
					if (lockCategoryStr != null && "1".equals(lockCategoryStr))
					{
						lockCategory = true;
					}*/
				}
				
				//allowuntildate_categoryId
				String allowUntilDateParam = null;
				allowUntilDateParam = this.request.getParameter("allowuntildate_"+ id[1]);
				
				Date allowUntilDate = null;
				if (allowUntilDateParam != null && allowUntilDateParam.trim().length() > 0)
				{
					try
					{
						allowUntilDate = DateUtil.getDateFromString(allowUntilDateParam.trim());
					} 
					catch (ParseException e)
					{
						errFlag = true;
						categoryNameList.append(existingCategory.getTitle());
						categoryNameList.append(",");
						continue;
					}
				}
				
				// update if there are date changes
				boolean datesChanged = false;
				
				// open date
				if (existingCategory.getAccessDates().getOpenDate() == null)
				{
					if (startDate != null)
					{
						datesChanged = true;
					}
				}
				else
				{
					if (startDate == null)
					{
						datesChanged = true;
					}
					else if (!startDate.equals(existingCategory.getAccessDates().getOpenDate()))
					{
						datesChanged = true;
					}
					else if (startDate.equals(existingCategory.getAccessDates().getOpenDate()))
					{
						if (!existingCategory.getAccessDates().isHideUntilOpen().equals(hideUntilOpen))
						{
							datesChanged = true;
						}
					}
				}
				
				// due date
				if (!datesChanged)
				{
					if (existingCategory.getAccessDates().getDueDate() == null)
					{
						if (endDate != null)
						{
							datesChanged = true;
						}
					}
					else
					{
						if (endDate == null)
						{
							datesChanged = true;
						}
						else if (!endDate.equals(existingCategory.getAccessDates().getDueDate()))
						{
							datesChanged = true;
						}
					}
				}
				
				// allow until date
				if (!datesChanged)
				{
					if (existingCategory.getAccessDates().getAllowUntilDate() == null)
					{
						if (allowUntilDate != null)
						{
							datesChanged = true;
						}
					}
					else
					{
						if (allowUntilDate == null)
						{
							datesChanged = true;
						}
						else if (!allowUntilDate.equals(existingCategory.getAccessDates().getAllowUntilDate()))
						{
							datesChanged = true;
						}
					}
				}
				
				if (datesChanged)
				{
					/*int topicDatesCount = DataAccessDriver.getInstance().newTopicDAO().getTopicDatesCountByCategory(existingCategory.getId());
					int forumDatesCount = DataAccessDriver.getInstance().newForumDAO().getForumDatesCount(existingCategory.getId());
					
					if ((topicDatesCount > 0) || (forumDatesCount > 0))
					{
						existingCategory.getAccessDates().setOpenDate(null);
						existingCategory.getAccessDates().setDueDate(null);
						existingCategory.getAccessDates().setLocked(false);
					}*/
					//DataAccessDriver.getInstance().newCategoryDAO().updateDates(c);
					
					if (startDate != null)
					{
						existingCategory.getAccessDates().setOpenDate(startDate);
						existingCategory.getAccessDates().setHideUntilOpen(hideUntilOpen);
					}
					else
					{
						existingCategory.getAccessDates().setOpenDate(null);
						existingCategory.getAccessDates().setHideUntilOpen(null);
					}
					
					if (endDate != null)
					{
						existingCategory.getAccessDates().setDueDate(endDate);
						//existingCategory.getAccessDates().setLocked(Boolean.valueOf(lockCategory));
					}
					else
					{
						existingCategory.getAccessDates().setDueDate(null);
						//existingCategory.getAccessDates().setLocked(Boolean.FALSE);
					}
					
					if (allowUntilDate != null)
					{
						existingCategory.getAccessDates().setAllowUntilDate(allowUntilDate);
					}
					else
					{
						existingCategory.getAccessDates().setAllowUntilDate(null);
					}
					
					jforumCategoryService.updateDates(existingCategory, Type.CATEGORY);
				}
			}
		}		
		
		if (errFlag == true)
		{
		  String categoryNameListStr = categoryNameList.toString();
		  if (categoryNameListStr.endsWith(","))
		  {
			  categoryNameListStr = categoryNameListStr.substring(0,categoryNameListStr.length()-1);
		  }
		  errors.add(I18n.getMessage("Forums.List.CannotUpdateForumDates", new Object[]{categoryNameListStr}));
		}
		
		if (errors.size() > 0) {
			this.context.put("errorMessage", errors);
		}
	}
	
	
	/**
	 * send grades to gradebook
	 * @param grade			grade
	 * @param gradebookUid	gradebookuid
	 * @param jForumGBService	jforum gradebook service
	 * @throws Exception
	 */
	protected void sendGradesToGradebook(Grade grade, String gradebookUid, JForumGBService jForumGBService) throws Exception
	{
		if (grade.isAddToGradeBook())
		{
			// send grades to gradebook
			List<Evaluation> evaluations = null;
			EvaluationDAO.EvaluationsSort evalSort = EvaluationDAO.EvaluationsSort.last_name_a;
			evaluations = DataAccessDriver.getInstance().newEvaluationDAO().selectCategoryEvaluations(grade.getCategoryId(), evalSort);
			
			Map<String, Double> scores = new HashMap<String, Double>();
			for(Evaluation eval: evaluations) 
			{
				if (eval.isReleased())
				{
					String key = eval.getSakaiUserId();
					Float userScore = eval.getScore();
					scores.put(key, (userScore == null) ? null : Double.valueOf(userScore.doubleValue()));
				}
			}
			jForumGBService.updateExternalAssessmentScores(gradebookUid, "discussions-"+ String.valueOf(grade.getId()), scores);
		}
	}
}
