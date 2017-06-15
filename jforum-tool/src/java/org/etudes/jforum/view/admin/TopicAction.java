/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/view/admin/TopicAction.java $ 
 * $Id: TopicAction.java 10604 2015-04-24 21:29:29Z murthyt $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2013, 2014, 2015 Etudes, Inc. 
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
import org.etudes.api.app.jforum.JForumCategoryService;
import org.etudes.api.app.jforum.JForumForumService;
import org.etudes.api.app.jforum.JForumGradesModificationException;
import org.etudes.api.app.jforum.JForumItemNotFoundException;
import org.etudes.api.app.jforum.JForumPostService;
import org.etudes.jforum.entities.Forum;
import org.etudes.jforum.repository.ForumRepository;
import org.etudes.jforum.util.I18n;
import org.etudes.jforum.util.date.DateUtil;
import org.etudes.jforum.util.preferences.ConfigKeys;
import org.etudes.jforum.util.preferences.SakaiSystemGlobals;
import org.etudes.jforum.util.preferences.TemplateKeys;
import org.etudes.jforum.util.user.JForumUserUtil;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.cover.UserDirectoryService;

import freemarker.template.TemplateModel;

public class TopicAction extends AdminCommand
{
	private static Log logger = LogFactory.getLog(TopicAction.class);

	/**
	 * show the categories and forums list
	 * 
	 * @throws Exception
	 */
	public void forumList() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		this.context.put("viewTitleManageTopics", true);
		this.context.put("viewTitleManageTopicsMessage", true);
		
		JForumCategoryService jforumCategoryService = (JForumCategoryService) ComponentManager.get("org.etudes.api.app.jforum.JForumCategoryService");
		List<org.etudes.api.app.jforum.Category> categories = jforumCategoryService.getManageCategories(ToolManager.getCurrentPlacement().getContext(), UserDirectoryService.getCurrentUser().getId());

		this.context.put("categories", categories);
		this.context.put("repository", new ForumRepository());
		this.setTemplateName(TemplateKeys.TOPIC_ADMIN_FORUM_LIST);
	}

	@Override
	public void list() throws Exception
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Save topic type, dates and grade. If any updates not done show them else show the categories and forums list after saving
	 * 
	 * @throws Exception
	 */
	public void saveTopics() throws Exception
	{
		
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) {
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		// save topics
		saveTopicAttributes();
		
		TemplateModel errorMessage = this.context.get("errorMessage");
		if (errorMessage != null)
		{
			//String navTo = this.request.getParameter("autosavenav");
			topicsList();
			return;
		}
		
		autoSaveNavigation();		
	}
	
	/**
	 * Show the gradable and re-use topics of the forum
	 *  
	 * @throws Exception
	 */
	public void topicsList() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator)
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		
		this.context.put("viewTitleManageTopics", true);
		this.context.put("viewManageTopics", true);
		int forumId = this.request.getIntParameter("forum_id");
		
		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);
		
		if (forum == null)
		{
			if (logger.isErrorEnabled())
			{
				logger.error("forum with id " + forumId + " not found");
			}
			this.context.put("errorMessage", I18n.getMessage("Forum.notFound"));
			this.setTemplateName(TemplateKeys.ITEM_NOT_FOUND_MESSAGE);
			return;
		}
		
		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		List<org.etudes.api.app.jforum.Topic> topics = jforumPostService.getForumExportAndGradableTopics(forumId);
		
		this.context.put("topics", topics);
		this.context.put("repository", new ForumRepository());
		this.context.put("forum", forum);
		
		if (forum.getGradeType() == org.etudes.api.app.jforum.Grade.GradeType.TOPIC.getType())
		{
			this.context.put("canAddEditGrade", true);
			Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
			if ((site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.GRADEBOOK_TOOL_ID)) != null) || (site.getToolForCommonId(SakaiSystemGlobals.getValue(ConfigKeys.ETUDES_GRADEBOOK_TOOL_ID)) != null))
			{
				this.context.put("enableGrading", Boolean.TRUE);
			}
		}
		
		addGradeTypesToContext();
		
		this.setTemplateName(TemplateKeys.TOPIC_ADMIN_TOPIC_LIST);
		
	}
	
	/**
	 * add forum grade type to context
	 */
	protected void addGradeTypesToContext()
	{
		this.context.put("gradeDisabled", Forum.GRADE_DISABLED);
		this.context.put("gradeForum", Forum.GRADE_BY_FORUM);
		this.context.put("gradeTopic", Forum.GRADE_BY_TOPIC);
		this.context.put("gradeCategory", Forum.GRADE_BY_CATEGORY);		
	}
	
	/**
	 * Read and save topic type, dates and grade. Add updates that are not done to the context.
	 * 
	 * @throws Exception
	 */
	protected void saveTopicAttributes() throws Exception
	{
		boolean isfacilitator = JForumUserUtil.isJForumFacilitator(UserDirectoryService.getCurrentUser().getId()) || SecurityService.isSuperUser();
		
		if (!isfacilitator) 
		{
			this.context.put("message", I18n.getMessage("User.NotAuthorizedToManage"));
			this.setTemplateName(TemplateKeys.MANAGE_NOT_AUTHORIZED);
			return;
		}
		int forumId = this.request.getIntParameter("forum_id");
		
		JForumForumService jforumForumService = (JForumForumService) ComponentManager.get("org.etudes.api.app.jforum.JForumForumService");
		
		org.etudes.api.app.jforum.Forum forum = jforumForumService.getForum(forumId);
		
		if (forum == null)
		{
			if (logger.isErrorEnabled())
			{
				logger.error("forum with id " + forumId + " not found");
			}
			this.context.put("errorMessage", I18n.getMessage("Forum.notFound"));
			this.setTemplateName(TemplateKeys.ITEM_NOT_FOUND_MESSAGE);
			return;
		}
		
		JForumPostService jforumPostService = (JForumPostService) ComponentManager.get("org.etudes.api.app.jforum.JForumPostService");
		List<org.etudes.api.app.jforum.Topic> exisTopics = jforumPostService.getForumExportAndGradableTopics(forumId);
		
		Map<Integer, org.etudes.api.app.jforum.Topic> exisTopicMap = new HashMap<Integer, org.etudes.api.app.jforum.Topic>();
		for (org.etudes.api.app.jforum.Topic existopic : exisTopics)
		{
			exisTopicMap.put(Integer.valueOf(existopic.getId()), existopic);
		}
		
		List<String> errors = new ArrayList<String>();
		boolean errFlagDates = false;
		StringBuffer topicNameListDates = new StringBuffer();
		
		// boolean errFlagGrades = false;
		//StringBuffer topicNameListGrades = new StringBuffer();
		
		Enumeration<?> paramNames = this.request.getParameterNames();
		
		while (paramNames.hasMoreElements())
		{
			String paramName = (String) paramNames.nextElement();
			
			if (paramName.startsWith("topic_type_"))
			{
				// paramName is in the format startdate_topicId
				String id[] = paramName.split("_");
				String topicId = null;
				topicId = id[2];
				
				org.etudes.api.app.jforum.Topic existingTopic = exisTopicMap.get(Integer.valueOf(topicId));
				
				if (existingTopic ==  null)
				{
					continue;
				}
				boolean topicChanged = false;
				
				// topic type - topic_type_topicId
				String topicTypeParam = this.request.getParameter("topic_type_"+ id[2]);
				if (topicTypeParam != null)
				{
					int topicType = this.request.getIntParameter("topic_type_"+ id[2]);
					
					if (topicType != existingTopic.getType())
					{
						existingTopic.setType(topicType);
						topicChanged = true;
					}
				}
				
				// topic re-use/export topic_export_${topic.id}
				String topicExportParam = this.request.getParameter("topic_export_"+ id[2]);
				if (topicExportParam != null)
				{
					if (!existingTopic.isExportTopic())
					{
						existingTopic.setExportTopic(Boolean.TRUE);
						topicChanged = true;
					}
				}
				else
				{
					if (existingTopic.isExportTopic())
					{
						existingTopic.setExportTopic(Boolean.FALSE);
						topicChanged = true;
					}
				}
				
				// Check dates
				String dateParamCheck = this.request.getParameter("startdate_"+ id[2]);
				
				boolean datesChanged = false;
				
				// if dateParamCheck is null no dates existing
				if (dateParamCheck != null)
				{
					boolean topicErrFlagDates = false;
					
					//startdate_topicId
					String startDateParam = null;				
					Date startDate = null;
					Boolean hideUntilOpen = Boolean.FALSE;
					
					startDateParam = this.request.getParameter("startdate_"+ id[2]);
					if (startDateParam != null && startDateParam.trim().length() > 0)
					{				
						try
						{
							startDate = DateUtil.getDateFromString(startDateParam.trim());
							
							//hide_until_open_topicId
							String hideUntilOpenStr = this.request.getParameter("hide_until_open_"+ id[2]);
							if (hideUntilOpenStr != null && "1".equals(hideUntilOpenStr))
							{
								hideUntilOpen = Boolean.TRUE;
							}
						} 
						catch (ParseException e)
						{
							errFlagDates = true;
							topicErrFlagDates = true;
							topicNameListDates.append(existingTopic.getTitle());
							topicNameListDates.append(",");
							//continue;
							startDate = existingTopic.getAccessDates().getOpenDate();
							
							//hide_until_open_topicId
							String hideUntilOpenStr = this.request.getParameter("hide_until_open_"+ id[2]);
							if (hideUntilOpenStr != null && "1".equals(hideUntilOpenStr))
							{
								hideUntilOpen = Boolean.TRUE;
							}
						}
					}
					
					//enddate_topicId
					String endDateParam = null;
					endDateParam = this.request.getParameter("enddate_"+ id[2]);
					Date endDate = null;
					if (endDateParam != null && endDateParam.trim().length() > 0)
					{
						try
						{
							endDate = DateUtil.getDateFromString(endDateParam.trim());
						} 
						catch (ParseException e)
						{
							if (!topicErrFlagDates)
							{
								errFlagDates = true;
								topicErrFlagDates = true;
								topicNameListDates.append(existingTopic.getTitle());
								topicNameListDates.append(",");
								// continue;
								endDate = existingTopic.getAccessDates().getDueDate();
							}
						}					
					}
					
					//allowuntildate_topicId
					String allowUntilDateParam = null;
					allowUntilDateParam = this.request.getParameter("allowuntildate_"+ id[2]);
					
					Date allowUntilDate = null;
					if (allowUntilDateParam != null && allowUntilDateParam.trim().length() > 0)
					{
						try
						{
							allowUntilDate = DateUtil.getDateFromString(allowUntilDateParam.trim());
						} 
						catch (ParseException e)
						{
							if (!topicErrFlagDates)
							{
								errFlagDates = true;
								topicNameListDates.append(existingTopic.getTitle());
								topicNameListDates.append(",");
								// continue;
								allowUntilDate = existingTopic.getAccessDates().getAllowUntilDate();
							}
						}
					}
					
					// update if there are date changes
					datesChanged = false;
					
					// open date
					if (existingTopic.getAccessDates().getOpenDate() == null)
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
						else if (!startDate.equals(existingTopic.getAccessDates().getOpenDate()))
						{
							datesChanged = true;
						}
						else if (startDate.equals(existingTopic.getAccessDates().getOpenDate()))
						{
							if (!existingTopic.getAccessDates().isHideUntilOpen().equals(hideUntilOpen))
							{
								datesChanged = true;
							}
						}
					}
					
					// due date
					if (!datesChanged)
					{
						if (existingTopic.getAccessDates().getDueDate() == null)
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
							else if (!endDate.equals(existingTopic.getAccessDates().getDueDate()))
							{
								datesChanged = true;
							}
						}
					}
					
					// allow until date
					if (!datesChanged)
					{
						if (existingTopic.getAccessDates().getAllowUntilDate() == null)
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
							else if (!allowUntilDate.equals(existingTopic.getAccessDates().getAllowUntilDate()))
							{
								datesChanged = true;
							}
						}
					}
					
					if (datesChanged)
					{
						if (startDate != null)
						{
							existingTopic.getAccessDates().setOpenDate(startDate);
							existingTopic.getAccessDates().setHideUntilOpen(hideUntilOpen);
						}
						else
						{
							existingTopic.getAccessDates().setOpenDate(null);
							existingTopic.getAccessDates().setHideUntilOpen(null);
						}
						
						if (endDate != null)
						{
							existingTopic.getAccessDates().setDueDate(endDate);
						}
						else
						{
							existingTopic.getAccessDates().setDueDate(null);
						}
						
						if (allowUntilDate != null)
						{
							existingTopic.getAccessDates().setAllowUntilDate(allowUntilDate);
						}
						else
						{
							existingTopic.getAccessDates().setAllowUntilDate(null);
						}
						
						//jforumPostService.updateDates(existingTopic);					
					}				
				}
				
				// grading
				boolean gradesChanged = false;
				boolean exisGradesAddToGradebook = false;
				boolean modGradesAddToGradebook = false;
				if (existingTopic.getForumId() == forum.getId() && forum.getGradeType() == org.etudes.api.app.jforum.Grade.GradeType.TOPIC.getType())
				{
					// grade_topic_topicId, point_value_topicId, min_posts_required_topicId, min_posts_topicId, send_to_grade_book_topicId
					String gradeTopicParam = this.request.getParameter("grade_topic_"+ id[2]);
					
					//grade topic
					if (gradeTopicParam != null)
					{
						
						int topicGradeType = this.request.getIntParameter("grade_topic_"+ id[2]);
						Float points = null;
						
						String minPostsRequired = null;
						int minimumPosts = 0;
						
						String sendToGradebook = null;
						int sendToGradebookVal = 0;
						
						if (topicGradeType == org.etudes.api.app.jforum.Topic.TopicGradableCode.YES.getTopicGradableCode())
						{
							try
							{
								points = Float.parseFloat(this.request.getParameter("point_value_" + id[2]));
							}
							catch (NumberFormatException ne)
							{
								if (logger.isWarnEnabled())
								{
									logger.warn("Points parsing error for topic: "+ existingTopic.getTitle(), ne);
								}
							}
							minPostsRequired = this.request.getParameter("min_posts_required_" + id[2]);
							try
							{
								if (minPostsRequired != null)
								{
									minimumPosts = this.request.getIntParameter("min_posts_" + id[2]);
								}
							}
							catch (NumberFormatException ne)
							{
								if (logger.isWarnEnabled())
								{
									logger.warn("Minimum Posts parsing error for topic: "+ existingTopic.getTitle(), ne);
								}
							}
							sendToGradebook = this.request.getParameter("send_to_grade_book_" + id[2]);
							try
							{
								if (sendToGradebook != null)
								{
									sendToGradebookVal = Integer.parseInt(sendToGradebook);
								}
							}
							catch (NumberFormatException ne)
							{
								if (logger.isWarnEnabled())
								{
									logger.warn("Send To Gradebook parse error for topic: "+ existingTopic.getTitle(), ne);
								}
							}
						}
					
						// check with existing grade and update if changed
						if (existingTopic.isGradeTopic())
						{
							if (topicGradeType == org.etudes.api.app.jforum.Topic.TopicGradableCode.YES.getTopicGradableCode())
							{
								// check the changes
								org.etudes.api.app.jforum.Grade exisGrade = existingTopic.getGrade();
								if (exisGrade != null)
								{
									Float exisPoints = exisGrade.getPoints();
									Boolean exisMinPostsRequired = exisGrade.isMinimumPostsRequired();
									int exisMinPostsRequiredVal = exisGrade.getMinimumPosts();
									Boolean exisSendToGradebook = exisGrade.isAddToGradeBook();
									
									exisGradesAddToGradebook = exisSendToGradebook.booleanValue();
									
									// points
									if (exisPoints != null)
									{
										if (points != null)
										{
											if (exisPoints.compareTo(points) != 0)
											{
												gradesChanged = true;
												exisGrade.setPoints(points);
											}
										}
									}
									else
									{
										if (points != null)
										{
											gradesChanged = true;
											exisGrade.setPoints(points);
										}
									}
									
									// min posts
									if (exisMinPostsRequired)
									{
										if ((minPostsRequired != null))
										{
											if ("1".equals(minPostsRequired))
											{
												if (exisMinPostsRequiredVal != minimumPosts)
												{
													gradesChanged = true;
													exisGrade.setMinimumPosts(minimumPosts);
												}
											}
											else
											{
												gradesChanged = true;
												exisGrade.setMinimumPostsRequired(Boolean.FALSE);
												exisGrade.setMinimumPosts(0);
											}
										}
										else
										{
											gradesChanged = true;
											exisGrade.setMinimumPostsRequired(Boolean.FALSE);
											exisGrade.setMinimumPosts(0);
										}
									}
									else										
									{
										if ((minPostsRequired != null) && ("1".equals(minPostsRequired)))
										{
											gradesChanged = true;
											exisGrade.setMinimumPostsRequired(Boolean.TRUE);
											exisGrade.setMinimumPosts(minimumPosts);
										}
									}
									
									// add to gradebook
									if (exisSendToGradebook)
									{
										if (sendToGradebookVal != 1)
										{
											gradesChanged = true;
											exisGrade.setAddToGradeBook(Boolean.FALSE);
										}
									}
									else
									{
										if (sendToGradebookVal == 1)
										{
											gradesChanged = true;
											exisGrade.setAddToGradeBook(Boolean.TRUE);
											modGradesAddToGradebook = true;
										}
									}
								}
								else
								{
									gradesChanged = true;
								}
							}
							else if (topicGradeType == org.etudes.api.app.jforum.Topic.TopicGradableCode.NO.getTopicGradableCode())
							{
								existingTopic.setGradeTopic(Boolean.FALSE);
								existingTopic.setGrade(null);
								gradesChanged = true;
							}
						}
						else
						{
							if (topicGradeType == org.etudes.api.app.jforum.Topic.TopicGradableCode.YES.getTopicGradableCode())
							{
								gradesChanged = true;
								existingTopic.setGradeTopic(Boolean.TRUE);
								
								org.etudes.api.app.jforum.Grade newGrade = jforumPostService.newTopicGrade(existingTopic);
								// points
								if (points != null)
								{									
									newGrade.setPoints(points);
								}
								
								// min posts
								if ((minPostsRequired != null) && ("1".equals(minPostsRequired)))
								{
									newGrade.setMinimumPostsRequired(Boolean.TRUE);
									newGrade.setMinimumPosts(minimumPosts);
								}
								
								// add to gradebook
								if (sendToGradebookVal == 1)
								{
									newGrade.setAddToGradeBook(Boolean.TRUE);
									modGradesAddToGradebook = true;
								}
								else
								{
									newGrade.setAddToGradeBook(Boolean.FALSE);
								}
							}
						}
					}
				}
							
				if (topicChanged || datesChanged || gradesChanged)
				{
					try
					{
						jforumPostService.modifyTopic(existingTopic, UserDirectoryService.getCurrentUser().getId());
						
						if (gradesChanged)
						{
							if (!exisGradesAddToGradebook)
							{
								if (existingTopic.isGradeTopic())
								{
									if (modGradesAddToGradebook)
									{
										org.etudes.api.app.jforum.Grade modGrade = existingTopic.getGrade();
										if (!modGrade.isAddToGradeBook())
										{
											errors.add(I18n.getMessage("Topics.List.CannotUpdateTopicGrades.TitleExistingInGradeBook", new Object[]{existingTopic.getTitle()}));
										}
									}
								}
							}
						}
					}
					catch (JForumItemNotFoundException e)
					{
						//errFlagGrades = true;
						/*
						topicNameListGrades.append(existingTopic.getTitle());
						topicNameListGrades.append(",");
						*/
						errors.add(e.toString());
						continue;
					}
					catch (JForumAccessException e)
					{
						//errFlagGrades = true;
						/*
						topicNameListGrades.append(existingTopic.getTitle());
						topicNameListGrades.append(",");
						*/
						errors.add(e.toString());
						continue;
					}
					catch (JForumGradesModificationException e)
					{
						//errFlagGrades = true;
						/*topicNameListGrades.append(existingTopic.getTitle());
						topicNameListGrades.append(",");*/
						errors.add(I18n.getMessage("Topics.List.CannotUpdateTopicGrades.Evaluated", new Object[]{existingTopic.getTitle()}));
						continue;
					}
				}
			}			
		}		
		
		// date format errors
		if (errFlagDates)
		{
			String topicNameListStr = topicNameListDates.toString();
			if (topicNameListStr.endsWith(","))
			{
				topicNameListStr = topicNameListStr.substring(0,topicNameListStr.length()-1);
			}
			errors.add(I18n.getMessage("Topics.List.CannotUpdateTopicDates", new Object[]{topicNameListStr}));
		}
		
		// grade errors
		/*if (errFlagGrades)
		{
			String topicNameListStr = topicNameListGrades.toString();
			if (topicNameListStr.endsWith(","))
			{
				topicNameListStr = topicNameListStr.substring(0,topicNameListStr.length()-1);
			}
			errors.add(I18n.getMessage("Topics.List.CannotUpdateTopicGrades", new Object[]{topicNameListStr}));
		}*/
		
		if (errors.size() > 0) 
		{
			this.context.put("errorMessage", errors);
		}
	}
}
