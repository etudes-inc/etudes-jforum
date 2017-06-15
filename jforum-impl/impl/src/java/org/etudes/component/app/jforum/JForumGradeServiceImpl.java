/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/JForumGradeServiceImpl.java $ 
 * $Id: JForumGradeServiceImpl.java 10723 2015-05-06 01:40:48Z murthyt $ 
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
package org.etudes.component.app.jforum;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.AccessDates;
import org.etudes.api.app.jforum.Category;
import org.etudes.api.app.jforum.Evaluation;
import org.etudes.api.app.jforum.Forum;
import org.etudes.api.app.jforum.Grade;
import org.etudes.api.app.jforum.JForumAccessException;
import org.etudes.api.app.jforum.JForumCategoryService;
import org.etudes.api.app.jforum.JForumForumService;
import org.etudes.api.app.jforum.JForumGBService;
import org.etudes.api.app.jforum.JForumGradeService;
import org.etudes.api.app.jforum.JForumPostService;
import org.etudes.api.app.jforum.JForumSecurityService;
import org.etudes.api.app.jforum.JForumUserService;
import org.etudes.api.app.jforum.SpecialAccess;
import org.etudes.api.app.jforum.Topic;
import org.etudes.api.app.jforum.User;
import org.etudes.api.app.jforum.dao.GradeDao;
import org.etudes.component.app.jforum.util.JForumUtil;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;

public class JForumGradeServiceImpl implements JForumGradeService
{
	private static Log logger = LogFactory.getLog(JForumGradeServiceImpl.class);
	
	/** Dependency: gradeDao */
	protected GradeDao gradeDao = null;
	
	/** Dependency: JForumCategoryService. */
	protected JForumCategoryService jforumCategoryService = null;
	
	/** Dependency: JForumForumService. */
	protected JForumForumService jforumForumService = null;
	
	/** Dependency: SqlService */
	protected JForumGBService jforumGBService = null;

	/** Dependency: JForumPostService. */
	protected JForumPostService jforumPostService = null;

	/** Dependency: JForumSecurityService. */
	protected JForumSecurityService jforumSecurityService = null;
	
	/** Dependency: JForumUserService. */
	protected JForumUserService jforumUserService = null;
	
	/** Dependency: SiteService. */
	protected SiteService siteService = null;
	
	/** Dependency: SqlService */
	protected SqlService sqlService = null;
	
	/**
	 * {@inheritDoc}
	 */
	public void addModifyCategoryEvaluations(Grade grade, List<Evaluation> evaluations) throws JForumAccessException
	{
		Grade exisCatGrade = getByCategoryId(grade.getCategoryId());
		
		if (exisCatGrade == null)
		{
			return;
		}
		
		if (grade == null || grade.getContext() == null || grade.getContext().trim().length() == 0)
		{
			return;
		}
		
		if (exisCatGrade.getId() != grade.getId())
		{
			return;
		}
		
		// check grade type - may be not needed
		if (grade.getType() != Grade.GradeType.CATEGORY.getType())
		{
			return;
		}
		
		Category category = jforumCategoryService.getCategory(grade.getCategoryId());
		
		if (category == null)
		{
			return;
		}
		
		if (evaluations.isEmpty())
		{
			/* "Send to Gradebook" may be changed*/
			
			//update gradebook
			updateGradebook(grade, category);
			
			// update grade for "add to grade book" status and update gradebook.
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
			
			return;
		}
				
		// check evaluated by has access to update or add evaluations.
		String evaluatedBySakaiUserId = null;
		evaluatedBySakaiUserId = evaluations.get(0).getEvaluatedBySakaiUserId();
		
		if (evaluatedBySakaiUserId == null)
		{
			return;
		}
		
		if (!jforumSecurityService.isJForumFacilitator(grade.getContext(), evaluatedBySakaiUserId))
		{
			throw new JForumAccessException(evaluatedBySakaiUserId);
		}
		
		// check grade id with evaluation grade id, evaluated by etc... Each user must have only one evaluation
		Map<String, Evaluation> userEvaluations = new HashMap<String, Evaluation>();
				
		Iterator<Evaluation> iter = evaluations.iterator();
		Evaluation evaluation = null;
		
		while(iter.hasNext()) 
		{
			evaluation = iter.next();
			
			// evaluation should belong same grade	
			if (grade.getId() != evaluation.getGradeId())
			{
				iter.remove();
				continue;
			}
			
			// one evaluation per participant user
			if (!jforumSecurityService.isJForumParticipant(grade.getContext(), evaluation.getSakaiUserId()))
			{
				iter.remove();
				continue;
			}
			
			// evaluated by should be same
			if (evaluation.getEvaluatedBySakaiUserId() == null || evaluation.getEvaluatedBySakaiUserId().trim().length() == 0
					|| !evaluation.getEvaluatedBySakaiUserId().trim().equalsIgnoreCase(evaluatedBySakaiUserId))
			{
				iter.remove();
				continue;
			}
			
			if (!userEvaluations.containsKey(evaluation.getSakaiUserId()))
			{
				userEvaluations.put(evaluation.getSakaiUserId(), evaluation);
			}
		} 
		
		List<Evaluation> userEvals = new ArrayList<Evaluation>();
		userEvals.addAll(userEvaluations.values());
		
		gradeDao.addUpdateEvaluations(grade, userEvals);
		 
		//update gradebook
		updateGradebook(grade, category);
		
		// update grade for "add to grade book" status and update gradebook.
		if (grade.isAddToGradeBook())
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
		else
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void addModifyForumEvaluations(Grade grade, List<Evaluation> evaluations) throws JForumAccessException
	{
		Grade exisForumGrade = getByForumId(grade.getForumId());
		
		if (exisForumGrade == null)
		{
			return;
		}
		
		if (grade == null || grade.getContext() == null || grade.getContext().trim().length() == 0)
		{
			return;
		}
		
		// may be not needed
		if (exisForumGrade.getId() != grade.getId())
		{
			return;
		}
		
		// check grade type - may be not needed
		if (grade.getType() != Grade.GradeType.FORUM.getType())
		{
			return;
		}
		
		Forum forum = jforumForumService.getForum(grade.getForumId());
		
		if (forum == null)
		{
			return;
		}
		if (evaluations.isEmpty())
		{
			/* "Send to Gradebook" may be changed*/
			
			updateGradebook(grade, forum);
			
			// update grade for "add to grade book" status and update gradebook.
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
				
			return;
		}
		
		// forum - groups
		filterForumGroupUsers(forum, evaluations);
		
		// check evaluated by has access to update or add evaluations.
		String evaluatedBySakaiUserId = null;
		evaluatedBySakaiUserId = evaluations.get(0).getEvaluatedBySakaiUserId();
		
		if (evaluatedBySakaiUserId == null)
		{
			return;
		}
		
		if (!jforumSecurityService.isJForumFacilitator(grade.getContext(), evaluatedBySakaiUserId))
		{
			throw new JForumAccessException(evaluatedBySakaiUserId);
		}
		
		// check grade id with evaluation grade id, evaluated by etc... Each user must have only one evaluation
		Map<String, Evaluation> userEvaluations = new HashMap<String, Evaluation>();
				
		Iterator<Evaluation> iter = evaluations.iterator();
		Evaluation evaluation = null;
		
		while(iter.hasNext()) 
		{
			evaluation = iter.next();
			
			// evaluation should belong same grade	
			if (grade.getId() != evaluation.getGradeId())
			{
				iter.remove();
				continue;
			}
			
			// one evaluation per participant user
			if (!jforumSecurityService.isJForumParticipant(grade.getContext(), evaluation.getSakaiUserId()))
			{
				iter.remove();
				continue;
			}
			
			// evaluated by should be same
			if (evaluation.getEvaluatedBySakaiUserId() == null || evaluation.getEvaluatedBySakaiUserId().trim().length() == 0
					|| !evaluation.getEvaluatedBySakaiUserId().trim().equalsIgnoreCase(evaluatedBySakaiUserId))
			{
				iter.remove();
				continue;
			}
			
			if (!userEvaluations.containsKey(evaluation.getSakaiUserId()))
			{
				userEvaluations.put(evaluation.getSakaiUserId(), evaluation);
			}
		} 
		
		List<Evaluation> userEvals = new ArrayList<Evaluation>();
		userEvals.addAll(userEvaluations.values());
		
		gradeDao.addUpdateEvaluations(grade, userEvals);
		 
		// update grade book
		updateGradebook(grade, forum);
		
		// update grade for "add to grade book" status and update gradebook.
		if (grade.isAddToGradeBook())
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
		else
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void addModifyTopicEvaluations(Grade grade, List<Evaluation> evaluations) throws JForumAccessException
	{
		if (grade == null || grade.getContext() == null || grade.getContext().trim().length() == 0)
		{
			return;
		}
		
		Grade exisTopicGrade = getByForumTopicId(grade.getForumId(), grade.getTopicId());
		
		if (exisTopicGrade == null)
		{
			return;
		}
		
		// may be not needed
		if (exisTopicGrade.getId() != grade.getId())
		{
			return;
		}
		
		// check grade type - may be not needed
		if (grade.getType() != Grade.GradeType.TOPIC.getType())
		{
			return;
		}
		
		Topic topic = jforumPostService.getTopic(grade.getTopicId());
		
		if (topic == null)
		{
			return;
		}
		
		if (evaluations.isEmpty())
		{
			/* "Send to Gradebook" may be changed*/
			
			// update gradebook
			updateGradebook(grade, topic);
			
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
			
			return;
		}
		
		Forum forum = topic.getForum();
		
		if (forum == null)
		{
			return;
		}
		
		// forum - groups
		filterTopicForumGroupUsers(topic, evaluations);
		
		// check evaluated by has access to update or add evaluations.
		String evaluatedBySakaiUserId = null;
		evaluatedBySakaiUserId = evaluations.get(0).getEvaluatedBySakaiUserId();
		
		if (evaluatedBySakaiUserId == null)
		{
			return;
		}
		
		if (!jforumSecurityService.isJForumFacilitator(grade.getContext(), evaluatedBySakaiUserId))
		{
			throw new JForumAccessException(evaluatedBySakaiUserId);
		}
		
		// check grade id with evaluation grade id, evaluated by etc... Each user must have only one evaluation
		Map<String, Evaluation> userEvaluations = new HashMap<String, Evaluation>();
				
		Iterator<Evaluation> iter = evaluations.iterator();
		Evaluation evaluation = null;
		
		while(iter.hasNext()) 
		{
			evaluation = iter.next();
			
			// evaluation should belong same grade	
			if (grade.getId() != evaluation.getGradeId())
			{
				iter.remove();
				continue;
			}
			
			// one evaluation per participant user
			if (!jforumSecurityService.isJForumParticipant(grade.getContext(), evaluation.getSakaiUserId()))
			{
				iter.remove();
				continue;
			}
			
			// evaluated by should be same
			if (evaluation.getEvaluatedBySakaiUserId() == null || evaluation.getEvaluatedBySakaiUserId().trim().length() == 0
					|| !evaluation.getEvaluatedBySakaiUserId().trim().equalsIgnoreCase(evaluatedBySakaiUserId))
			{
				iter.remove();
				continue;
			}
			
			if (!userEvaluations.containsKey(evaluation.getSakaiUserId()))
			{
				userEvaluations.put(evaluation.getSakaiUserId(), evaluation);
			}
		} 
		
		List<Evaluation> userEvals = new ArrayList<Evaluation>();
		userEvals.addAll(userEvaluations.values());
		
		gradeDao.addUpdateEvaluations(grade, userEvals);
		 
		// update gradebook
		updateGradebook(grade, topic);
		
		// update grade for "add to grade book" status and update gradebook.
		if (grade.isAddToGradeBook())
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());			
		}
		else
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void addModifyUserEvaluation(Evaluation evaluation) throws JForumAccessException
	{
		if (evaluation == null)
		{
			return;
		}
		
		// check evaluated by has access to update or add evaluations.
		String evaluatedBySakaiUserId = evaluation.getEvaluatedBySakaiUserId();
		
		if (evaluatedBySakaiUserId == null || evaluatedBySakaiUserId.trim().length() == 0)
		{
			return;
		}
		
		String evaluationSakaiUserId = evaluation.getSakaiUserId();
		
		if (evaluationSakaiUserId == null || evaluationSakaiUserId.trim().length() == 0)
		{
			return;
		}
		
		Grade grade = gradeDao.selectGradeById(evaluation.getGradeId());
		
		if (grade == null)
		{
			return;
		}
		
		if (!jforumSecurityService.isJForumFacilitator(grade.getContext(), evaluatedBySakaiUserId))
		{
			throw new JForumAccessException(evaluatedBySakaiUserId);
		}
	
		Evaluation exisEval = null;
		
		if (grade.getType() == Grade.GradeType.CATEGORY.getType())
		{
			exisEval = getUserCategoryEvaluation(grade.getCategoryId(), evaluationSakaiUserId);
		}
		
		if (checkEvaluationChanges(exisEval, evaluation))
		{
			gradeDao.addUpdateUserEvaluation(evaluation);
			
			//update gradebook
			updateGradebookForUser(grade, evaluation);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void assignZeroCategoryNonSubmitters(int categoryId, String evaluatedBySakaiUserId) throws JForumAccessException
	{
		if (categoryId <= 0 || evaluatedBySakaiUserId == null || evaluatedBySakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("category information is missing.");
		}
		
		Grade grade = getByCategoryId(categoryId);
				
		if (grade == null)
		{
			return;
		}
		
		Category category = jforumCategoryService.getCategory(grade.getCategoryId());
		
		if (category == null)
		{
			return;
		}
		
		// check for closed date. Only closed category grades should be zero for non submitters
		Date currentTime = new Date();
		boolean assingZeros = false;
		if (category.getAccessDates() != null)
		{
			if (category.getAccessDates().getAllowUntilDate() != null)
			{
				if (currentTime.after(category.getAccessDates().getAllowUntilDate()))
				{
					assingZeros = true;
				}
			}
			else if (category.getAccessDates().getDueDate() != null)
			{
				if (currentTime.after(category.getAccessDates().getDueDate()))
				{
					assingZeros = true;
				}				
			}
		}
		
		if (!assingZeros)
		{
			return;
		}
		
		List<Evaluation> categoryEvaluations = getCategoryEvaluationsWithPosts(category.getId(), Evaluation.EvaluationsSort.total_posts_a, evaluatedBySakaiUserId, true);
		
		Iterator<Evaluation> iterEvaluation = categoryEvaluations.iterator();
		Evaluation evaluation = null;
		
		while(iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			/* No special access check for category */
			
			if (evaluation.getTotalPosts() == 0 && evaluation.getScore() == null)
			{
				evaluation.setScore(new Float(0));
				evaluation.setEvaluatedBySakaiUserId(evaluatedBySakaiUserId);
				
				if (evaluation.getId() <= 0)
				{
					((EvaluationImpl)evaluation).setId(-1);
					((EvaluationImpl)evaluation).setGradeId(grade.getId());
					evaluation.setReleased(Boolean.TRUE);
				}
			}
			else
			{
				evaluation.setEvaluatedBySakaiUserId(evaluatedBySakaiUserId);
			}
		}
		
		gradeDao.addUpdateEvaluations(grade, categoryEvaluations);
		 
		//update gradebook
		updateGradebook(grade, category);
		
		// update grade for "add to grade book" status and update gradebook.
		if (grade.isAddToGradeBook())
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
		else
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void assignZeroForumNonSubmitters(int forumId, String evaluatedBySakaiUserId) throws JForumAccessException
	{
		if (forumId <= 0 || evaluatedBySakaiUserId == null || evaluatedBySakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("forum information is missing.");
		}
		
		Grade grade = getByForumId(forumId);
				
		if (grade == null)
		{
			return;
		}
		
		Forum forum = jforumForumService.getForum(grade.getForumId());
		
		if (forum == null)
		{
			return;
		}
		
		Category category = forum.getCategory();
		
		// check for closed date. Only closed forum grades should be zero for non submitters
		Date currentTime = new Date();
		boolean assingZeros = false;
		if (forum.getAccessDates() != null && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))
		{
			if (forum.getAccessDates().getAllowUntilDate() != null)
			{
				if (currentTime.after(forum.getAccessDates().getAllowUntilDate()))
				{
					assingZeros = true;
				}
			}
			else if (forum.getAccessDates().getDueDate() != null)
			{
				if (currentTime.after(forum.getAccessDates().getDueDate()))
				{
					assingZeros = true;
				}				
			}
		}
		else if (category.getAccessDates() != null && (category.getAccessDates().getOpenDate() != null || category.getAccessDates().getDueDate() != null || category.getAccessDates().getAllowUntilDate() != null))
		{
			if (category.getAccessDates().getAllowUntilDate() != null)
			{
				if (currentTime.after(category.getAccessDates().getAllowUntilDate()))
				{
					assingZeros = true;
				}
			}
			else if (category.getAccessDates().getDueDate() != null)
			{
				if (currentTime.after(category.getAccessDates().getDueDate()))
				{
					assingZeros = true;
				}				
			}
		}
		
		if (!assingZeros)
		{
			return;
		}
		
		/* special access check for forum. Evaluations of members of group only are fetched for group only forum, group check in not needed*/
		List<SpecialAccess> forumSpecialAccessList = forum.getSpecialAccess();
		Map<Integer, SpecialAccess> userSpecialAccess = new HashMap<Integer, SpecialAccess>();
		
		if (forumSpecialAccessList != null && forumSpecialAccessList.size() > 0)
		{
			for (SpecialAccess sa : forumSpecialAccessList)
			{
				AccessDates specialAccessDates = sa.getAccessDates();
				
				if (specialAccessDates != null)
				{
					if (!sa.isOverrideStartDate())
					{
						sa.getAccessDates().setOpenDate(forum.getAccessDates().getOpenDate());
					}
					
					if (!sa.isOverrideHideUntilOpen())
					{
						sa.getAccessDates().setHideUntilOpen(forum.getAccessDates().isHideUntilOpen());
					}
					
					if (!sa.isOverrideEndDate())
					{
						sa.getAccessDates().setDueDate(forum.getAccessDates().getDueDate());
					}
					
					if (!sa.isOverrideAllowUntilDate())
					{
					
						sa.getAccessDates().setAllowUntilDate(forum.getAccessDates().getAllowUntilDate());
					}
					
					List<Integer> specialAccessUserIds = sa.getUserIds();
					
					for (int userId : specialAccessUserIds)
					{
						userSpecialAccess.put(userId, sa);
					}
				}
			}
		}
		
		List<Evaluation> forumEvaluations = getForumEvaluationsWithPosts(forum.getId(), Evaluation.EvaluationsSort.total_posts_a, evaluatedBySakaiUserId, true);
		
		Iterator<Evaluation> iterEvaluation = forumEvaluations.iterator();
		Evaluation evaluation = null;
		
		while(iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			// check user special access dates
			SpecialAccess userSa = userSpecialAccess.get(evaluation.getUserId());
			if (userSa != null)
			{
				boolean assingUserZero = false;
				if (userSa.getAccessDates() != null && (userSa.getAccessDates().getOpenDate() != null || userSa.getAccessDates().getDueDate() != null || userSa.getAccessDates().getAllowUntilDate() != null))
				{
					if (userSa.getAccessDates().getAllowUntilDate() != null)
					{
						if (currentTime.after(userSa.getAccessDates().getAllowUntilDate()))
						{
							assingUserZero = true;
						}
					}
					else if (userSa.getAccessDates().getDueDate() != null)
					{
						if (currentTime.after(userSa.getAccessDates().getDueDate()))
						{
							assingUserZero = true;
						}				
					}
				}
				
				if (!assingUserZero)
				{
					iterEvaluation.remove();
					continue;
				}
			}
			
			if (evaluation.getTotalPosts() == 0 && evaluation.getScore() == null)
			{
				evaluation.setScore(new Float(0));
				evaluation.setEvaluatedBySakaiUserId(evaluatedBySakaiUserId);
				
				if (evaluation.getId() <= 0)
				{
					((EvaluationImpl)evaluation).setId(-1);
					((EvaluationImpl)evaluation).setGradeId(grade.getId());
					evaluation.setReleased(Boolean.TRUE);
				}
			}
			else
			{
				evaluation.setEvaluatedBySakaiUserId(evaluatedBySakaiUserId);
			}
		}
		
		gradeDao.addUpdateEvaluations(grade, forumEvaluations);
		 
		//update gradebook
		updateGradebook(grade, forum);
		
		// update grade for "add to grade book" status and update gradebook.
		if (grade.isAddToGradeBook())
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
		else
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void assignZeroTopicNonSubmitters(int forumId, int topicId, String evaluatedBySakaiUserId) throws JForumAccessException
	{
		if (forumId <= 0 || topicId <= 0 || evaluatedBySakaiUserId == null || evaluatedBySakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("topic information is missing.");
		}
		
		Grade grade = getByForumTopicId(forumId, topicId);
				
		if (grade == null)
		{
			return;
		}
		
		Topic topic = jforumPostService.getTopic(topicId);
		
		if (topic == null)
		{
			return;
		}
		
		Forum forum = topic.getForum();
		
		Category category = forum.getCategory();
		
		// check for closed date. Only closed forum grades should be zero for non submitters
		Date currentTime = new Date();
		boolean assingZeros = false;
		if (topic.getAccessDates() != null && (topic.getAccessDates().getOpenDate() != null || topic.getAccessDates().getDueDate() != null || topic.getAccessDates().getAllowUntilDate() != null))
		{
			if (topic.getAccessDates().getAllowUntilDate() != null)
			{
				if (currentTime.after(topic.getAccessDates().getAllowUntilDate()))
				{
					assingZeros = true;
				}
			}
			else if (topic.getAccessDates().getDueDate() != null)
			{
				if (currentTime.after(topic.getAccessDates().getDueDate()))
				{
					assingZeros = true;
				}				
			}
		}
		else if (forum.getAccessDates() != null && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))
		{
			if (forum.getAccessDates().getAllowUntilDate() != null)
			{
				if (currentTime.after(forum.getAccessDates().getAllowUntilDate()))
				{
					assingZeros = true;
				}
			}
			else if (forum.getAccessDates().getDueDate() != null)
			{
				if (currentTime.after(forum.getAccessDates().getDueDate()))
				{
					assingZeros = true;
				}				
			}
		}
		else if (category.getAccessDates() != null && (category.getAccessDates().getOpenDate() != null || category.getAccessDates().getDueDate() != null || category.getAccessDates().getAllowUntilDate() != null))
		{
			if (category.getAccessDates().getAllowUntilDate() != null)
			{
				if (currentTime.after(category.getAccessDates().getAllowUntilDate()))
				{
					assingZeros = true;
				}
			}
			else if (category.getAccessDates().getDueDate() != null)
			{
				if (currentTime.after(category.getAccessDates().getDueDate()))
				{
					assingZeros = true;
				}				
			}
		}
		
		if (!assingZeros)
		{
			return;
		}
		
		/* special access check for topic. Evaluations of members of group only are fetched for group only forum, group check in not needed*/
		List<SpecialAccess> topicSpecialAccessList = topic.getSpecialAccess();
		Map<Integer, SpecialAccess> userSpecialAccess = new HashMap<Integer, SpecialAccess>();
		
		if (topicSpecialAccessList != null && topicSpecialAccessList.size() > 0)
		{
			for (SpecialAccess sa : topicSpecialAccessList)
			{
				AccessDates specialAccessDates = sa.getAccessDates();
				
				if (specialAccessDates != null)
				{
					if (!sa.isOverrideStartDate())
					{
						sa.getAccessDates().setOpenDate(forum.getAccessDates().getOpenDate());
					}
					
					if (!sa.isOverrideHideUntilOpen())
					{
						sa.getAccessDates().setHideUntilOpen(forum.getAccessDates().isHideUntilOpen());
					}
					
					if (!sa.isOverrideEndDate())
					{
						sa.getAccessDates().setDueDate(forum.getAccessDates().getDueDate());
					}
					
					if (!sa.isOverrideAllowUntilDate())
					{
					
						sa.getAccessDates().setAllowUntilDate(forum.getAccessDates().getAllowUntilDate());
					}
					
					List<Integer> specialAccessUserIds = sa.getUserIds();
					
					for (int userId : specialAccessUserIds)
					{
						userSpecialAccess.put(userId, sa);
					}
				}
			}
		}
		
		List<Evaluation> topicEvaluations = getTopicEvaluationsWithPosts(topic.getId(), Evaluation.EvaluationsSort.total_posts_a, evaluatedBySakaiUserId, true);
		
		Iterator<Evaluation> iterEvaluation = topicEvaluations.iterator();
		Evaluation evaluation = null;
		
		while(iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			// check user special access dates
			SpecialAccess userSa = userSpecialAccess.get(evaluation.getUserId());
			if (userSa != null)
			{
				boolean assingUserZero = false;
				if (userSa.getAccessDates() != null && (userSa.getAccessDates().getOpenDate() != null || userSa.getAccessDates().getDueDate() != null || userSa.getAccessDates().getAllowUntilDate() != null))
				{
					if (userSa.getAccessDates().getAllowUntilDate() != null)
					{
						if (currentTime.after(userSa.getAccessDates().getAllowUntilDate()))
						{
							assingUserZero = true;
						}
					}
					else if (userSa.getAccessDates().getDueDate() != null)
					{
						if (currentTime.after(userSa.getAccessDates().getDueDate()))
						{
							assingUserZero = true;
						}				
					}
				}
				
				if (!assingUserZero)
				{
					iterEvaluation.remove();
					continue;
				}
			}
			
			if (evaluation.getTotalPosts() == 0 && evaluation.getScore() == null)
			{
				evaluation.setScore(new Float(0));
				evaluation.setEvaluatedBySakaiUserId(evaluatedBySakaiUserId);
				
				if (evaluation.getId() <= 0)
				{
					((EvaluationImpl)evaluation).setId(-1);
					((EvaluationImpl)evaluation).setGradeId(grade.getId());
					evaluation.setReleased(Boolean.TRUE);
				}
			}
			else
			{
				evaluation.setEvaluatedBySakaiUserId(evaluatedBySakaiUserId);
			}
		}
		
		gradeDao.addUpdateEvaluations(grade, topicEvaluations);
		 
		//update gradebook
		updateGradebook(grade, topic);
		
		// update grade for "add to grade book" status and update gradebook.
		if (grade.isAddToGradeBook())
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
		else
		{
			gradeDao.updateAddToGradeBookStatus(grade.getId(), grade.isAddToGradeBook());
		}
	}
	
	public void destroy()
	{
		if (logger.isInfoEnabled())
			logger.info("destroy....");
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Grade getByCategoryId(int categoryId)
	{
		if (categoryId <= 0) throw new IllegalArgumentException();
		
		return gradeDao.selectByCategory(categoryId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Grade getByForumId(int forumId)
	{
		if (forumId <= 0) throw new IllegalArgumentException();
		
		return gradeDao.selectByForum(forumId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Grade getByForumTopicId(int forumId, int topicId)
	{
		if (forumId <= 0 || topicId <= 0) throw new IllegalArgumentException();
		
		return gradeDao.selectByForumTopic(forumId, topicId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Grade> getBySite(String siteId)
	{
		if (siteId == null || siteId.trim().length() == 0) throw new IllegalArgumentException();
		
		return gradeDao.selectBySite(siteId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Grade> getBySiteByGradeType(String siteId, int gradeType)
	{
		if (siteId == null || siteId.trim().length() == 0) throw new IllegalArgumentException();
		if (!(gradeType == Grade.GradeType.CATEGORY.getType() || gradeType == Grade.GradeType.FORUM.getType() || gradeType == Grade.GradeType.TOPIC.getType()))
		{
			throw new IllegalArgumentException();
		}
		
		return gradeDao.selectBySiteByGradeType(siteId, gradeType);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getCategoryEvaluations(int categoryId)
	{
		if (categoryId <= 0)
		{
			throw new IllegalArgumentException("Category information is missing.");
		}
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Grade grade = getByCategoryId(categoryId);
		
		if (grade != null)
		{
			evaluations.addAll(gradeDao.selectEvaluations(grade));
		}
		
		return evaluations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getCategoryEvaluationsCount(int categoryId)
	{
		if (categoryId <= 0) throw new IllegalArgumentException("Not a valid category id.");
		
		Grade grade = gradeDao.selectByCategory(categoryId);
		
		if (grade != null)
		{
			return gradeDao.selectEvaluationsCount(grade);
		}
		
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public void getCategoryEvaluationsWithPosts(Category category, Evaluation.EvaluationsSort evalSort, String sakaiUserId) throws JForumAccessException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering getCategoryEvaluationsWithPosts(int categoryId, Evaluation.EvaluationsSort evalSort, String sakaiUserId)......");
		}
		
		if (category == null || category.getId() <= 0)
		{
			throw new IllegalArgumentException("category information is missing.");
		}
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Grade grade = getByCategoryId(category.getId());
				
		if (grade == null)
		{
			return;
		}
		
		Category exisCategory = jforumCategoryService.getCategory(category.getId());
		
		if (exisCategory == null)
		{
			return;
		}
		
		if (!jforumSecurityService.isJForumFacilitator(exisCategory.getContext(), sakaiUserId))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// publish to gradebook if the open date of the grade is in the past
		Date now = new Date();
		if ((exisCategory.getAccessDates() != null) && (exisCategory.getAccessDates().getOpenDate() != null))		
		{
			if (exisCategory.getAccessDates().getOpenDate().before(now))
			{
				if (exisCategory.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, exisCategory);
					}
				}
			}
		}
		
		// sync sakai users
		jforumUserService.getSiteUsers(exisCategory.getContext(), 0, 0);
		
		evaluations.addAll(gradeDao.selectCategoryEvaluationsWithPosts(exisCategory.getId(), evalSort, true));
		
		Iterator<Evaluation> iterEvaluation = evaluations.iterator();
		
		Evaluation evaluation = null;
		String evalSakaiUserId = null;
		
		// set evaluation status
		Evaluation.EvaluationStatus evalStatus = Evaluation.EvaluationStatus.EVALUATED;
		int noPostsCount = 0;
		boolean evalStatusToBeEvaluated = false;
		
		while (iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			evalSakaiUserId = evaluation.getSakaiUserId();
			
			// remove facilitators, observers and admin users
			if (jforumSecurityService.isJForumFacilitator(exisCategory.getContext(), evalSakaiUserId) || jforumSecurityService.isEtudesObserver(exisCategory.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove guests
			if  (jforumSecurityService.isUserGuest(exisCategory.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove teaching assistants
			if  (jforumSecurityService.isUserTeachingAssistant(exisCategory.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// retain only participants
			if (!jforumSecurityService.isJForumParticipant(exisCategory.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove inactive users
			if (!jforumSecurityService.isUserActive(exisCategory.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove the users with blocked role
			if (jforumSecurityService.isUserBlocked(exisCategory.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// set sakai display id
			try
			{
				org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(evalSakaiUserId);
				((EvaluationImpl)evaluation).setSakaiDisplayId(sakUser.getDisplayId());
			}
			catch (UserNotDefinedException e)
			{
				iterEvaluation.remove();
			}
			
			if (!evalStatusToBeEvaluated)
			{
				if (evaluation.getTotalPosts() > 0)
				{
					if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
					{
						evalStatus = Evaluation.EvaluationStatus.TO_BE_EVALUATED;
						evalStatusToBeEvaluated = true;
					}
				}
				else
				{
					if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
					{
						noPostsCount++;
					}				
				}
			}
			
			if (evaluation.getLastPostTime() != null && evaluation.getFirstPostTime() != null && evaluation.getLastPostTime().equals(evaluation.getFirstPostTime()))
			{
				((EvaluationImpl) evaluation).setLastPostTime(null);
			}
		}
		
		// set evaluation status
		if (evaluations.size() == noPostsCount)
		{
			evalStatus = Evaluation.EvaluationStatus.NO_POSTS_NO_EVALS;
		}

		((CategoryImpl)category).setEvaluationStatus(evalStatus);
		
		//sort evaluations
		sortEvaluations(evaluations, evalSort);
		
		category.getEvaluations().clear();
		category.getEvaluations().addAll(evaluations);		
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting getCategoryEvaluationsWithPosts(Category category, Evaluation.EvaluationsSort evalSort, String sakaiUserId)......");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getCategoryEvaluationsWithPosts(int categoryId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all) throws JForumAccessException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering getCategoryEvaluationsWithPosts(int categoryId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all)......");
		}
		
		if (categoryId <= 0)
		{
			throw new IllegalArgumentException("category information is missing.");
		}
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Grade grade = getByCategoryId(categoryId);
				
		if (grade == null)
		{
			return evaluations;
		}
		
		Category category = jforumCategoryService.getCategory(categoryId);
		
		if (!jforumSecurityService.isJForumFacilitator(category.getContext(), sakaiUserId))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// publish to gradebook if the open date of the grade is in the past
		Date now = new Date();
		if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))		
		{
			if (category.getAccessDates().getOpenDate().before(now))
			{
				if (category.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, category);
					}
				}
			}
		}
		
		// sync sakai users
		jforumUserService.getSiteUsers(category.getContext(), 0, 0);
		
		evaluations.addAll(gradeDao.selectCategoryEvaluationsWithPosts(categoryId, evalSort, all));
		
		Iterator<Evaluation> iterEvaluation = evaluations.iterator();
		
		Evaluation evaluation = null;
		String evalSakaiUserId = null;
		
		while (iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			evalSakaiUserId = evaluation.getSakaiUserId();
			
			// remove facilitators and admin users
			if (jforumSecurityService.isJForumFacilitator(category.getContext(), evalSakaiUserId) || jforumSecurityService.isEtudesObserver(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove guests
			if  (jforumSecurityService.isUserGuest(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove teaching assistants
			if  (jforumSecurityService.isUserTeachingAssistant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// retain only participants
			if (!jforumSecurityService.isJForumParticipant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove inactive users
			if (!jforumSecurityService.isUserActive(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove the users with blocked role
			if (jforumSecurityService.isUserBlocked(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// set sakai display id
			try
			{
				org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(evalSakaiUserId);
				((EvaluationImpl)evaluation).setSakaiDisplayId(sakUser.getDisplayId());
			}
			catch (UserNotDefinedException e)
			{
				iterEvaluation.remove();
				continue;
			}
			
			if (evaluation.getLastPostTime() != null && evaluation.getFirstPostTime() != null && evaluation.getLastPostTime().equals(evaluation.getFirstPostTime()))
			{
				((EvaluationImpl) evaluation).setLastPostTime(null);
			}
		}
		//sort evaluations
		sortEvaluations(evaluations, evalSort);
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting getCategoryEvaluationsWithPosts(int categoryId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all)......");
		}
		
		return evaluations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getCategoryEvaluationsWithPosts(int categoryId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped) throws JForumAccessException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering getCategoryEvaluationsWithPosts(int categoryId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped)......");
		}
		
		if (categoryId <= 0)
		{
			throw new IllegalArgumentException("category information is missing.");
		}
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Grade grade = getByCategoryId(categoryId);
				
		if (grade == null)
		{
			return evaluations;
		}
		
		Category category = jforumCategoryService.getCategory(categoryId);
		
		if (!jforumSecurityService.isJForumFacilitator(category.getContext(), sakaiUserId))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// publish to gradebook if the open date of the grade is in the past
		Date now = new Date();
		if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))		
		{
			if (category.getAccessDates().getOpenDate().before(now))
			{
				if (category.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, category);
					}
				}
			}
		}
		
		// sync sakai users
		jforumUserService.getSiteUsers(category.getContext(), 0, 0);
		
		evaluations.addAll(gradeDao.selectCategoryEvaluationsWithPosts(categoryId, evalSort, all));
		
		Iterator<Evaluation> iterEvaluation = evaluations.iterator();
		
		Evaluation evaluation = null;
		String evalSakaiUserId = null;
		
		while (iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			evalSakaiUserId = evaluation.getSakaiUserId();
			
			// remove facilitators and admin users
			if (jforumSecurityService.isJForumFacilitator(category.getContext(), evalSakaiUserId) || jforumSecurityService.isEtudesObserver(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove guests
			if  (jforumSecurityService.isUserGuest(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove teaching assistants
			if  (jforumSecurityService.isUserTeachingAssistant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			if (!includeBlockedDropped)
			{
				// retain only participants
				if (!jforumSecurityService.isJForumParticipant(category.getContext(), evalSakaiUserId))
				{
					iterEvaluation.remove();
					continue;
				}
				
				// remove inactive users
				if (!jforumSecurityService.isUserActive(category.getContext(), evalSakaiUserId))
				{
					iterEvaluation.remove();
					continue;
				}
				
				
				// remove the users with blocked role
				if (jforumSecurityService.isUserBlocked(category.getContext(), evalSakaiUserId))
				{
					iterEvaluation.remove();
					continue;
				}
			}
			
			// set sakai display id
			try
			{
				org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(evalSakaiUserId);
				((EvaluationImpl)evaluation).setSakaiDisplayId(sakUser.getDisplayId());
			}
			catch (UserNotDefinedException e)
			{
				iterEvaluation.remove();
				continue;
			}
			
			if (evaluation.getLastPostTime() != null && evaluation.getFirstPostTime() != null && evaluation.getLastPostTime().equals(evaluation.getFirstPostTime()))
			{
				((EvaluationImpl) evaluation).setLastPostTime(null);
			}
		}
		//sort evaluations
		sortEvaluations(evaluations, evalSort);
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting getCategoryEvaluationsWithPosts(int categoryId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped)......");
		}
		
		return evaluations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getCategoryGradableForumsCount(int categoryId)
	{
		if (categoryId <= 0) throw new IllegalArgumentException();
		
		return gradeDao.selectCategoryGradableForumsCount(categoryId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getForumEvaluations(int forumId)
	{
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Grade grade = getByForumId(forumId);
		
		if (grade != null)
		{
			evaluations.addAll(gradeDao.selectEvaluations(grade));
		}
		
		return evaluations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getForumEvaluationsCount(int forumId)
	{
		if (forumId <= 0) throw new IllegalArgumentException("Not a valid forum id.");
		
		Grade grade = gradeDao.selectByForum(forumId);
		
		if (grade != null)
		{
			return gradeDao.selectEvaluationsCount(grade);
		}
		
		return 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void getForumEvaluationsWithPosts(Forum forum, Evaluation.EvaluationsSort evalSort, String sakaiUserId) throws JForumAccessException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering getForumEvaluationsWithPosts(int forumId, Evaluation.EvaluationsSort evalSort, String sakaiUserId)......");
		}
		
		if (forum == null || forum.getId() <= 0)
		{
			throw new IllegalArgumentException("forum information is missing.");
		}
		
		Forum exisForum = jforumForumService.getForum(forum.getId());
		
		if (exisForum == null)
		{
			return;
		}
		
		Grade grade = getByForumId(exisForum.getId());
		
		if (grade == null)
		{
			return;
		}
		
		Category category = exisForum.getCategory();
		
		if (!jforumSecurityService.isJForumFacilitator(category.getContext(), sakaiUserId))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// publish to gradebook if the open date of the grade is in the past
		Date now = new Date();
		if ((exisForum.getAccessDates() != null) && (exisForum.getAccessDates().getOpenDate() != null || exisForum.getAccessDates().getDueDate() != null || exisForum.getAccessDates().getAllowUntilDate() != null))		
		{
			if (exisForum.getAccessDates().getOpenDate() != null && exisForum.getAccessDates().getOpenDate().before(now))
			{
				if (exisForum.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, exisForum);
					}
				}
			}
		}
		else 		
		{
			if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
			{
				if (category.getAccessDates().getOpenDate().before(now))
				{
					if (category.getAccessDates().isHideUntilOpen())
					{
						if (grade.getItemOpenDate() != null)
						{
							updateGradebook(grade, exisForum);
						}
					}
				}
			}
		}
		
		// sync sakai users
		jforumUserService.getSiteUsers(category.getContext(), 0, 0);
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		evaluations.addAll(gradeDao.selectForumEvaluationsWithPosts(exisForum.getId(), evalSort, true));
		
		Iterator<Evaluation> iterEvaluation = evaluations.iterator();
		
		Evaluation evaluation = null;
		String evalSakaiUserId = null;
		
		// set evaluation status
		Evaluation.EvaluationStatus evalStatus = Evaluation.EvaluationStatus.EVALUATED;
		int noPostsCount = 0;
		boolean evalStatusToBeEvaluated = false;
		while (iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			evalSakaiUserId = evaluation.getSakaiUserId();
			
			// remove facilitators and admin users
			if (jforumSecurityService.isJForumFacilitator(category.getContext(), evalSakaiUserId) || jforumSecurityService.isEtudesObserver(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove guests
			if  (jforumSecurityService.isUserGuest(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove teaching assistants
			if  (jforumSecurityService.isUserTeachingAssistant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// retain only participants
			if (!jforumSecurityService.isJForumParticipant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove inactive users
			if (!jforumSecurityService.isUserActive(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove the users with blocked role
			if (jforumSecurityService.isUserBlocked(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// set sakai display id
			try
			{
				org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(evalSakaiUserId);
				((EvaluationImpl)evaluation).setSakaiDisplayId(sakUser.getDisplayId());
			}
			catch (UserNotDefinedException e)
			{
				iterEvaluation.remove();
			}
			
			if (forum.getAccessType() != Forum.ForumAccess.GROUPS.getAccessType())
			{
				if (!evalStatusToBeEvaluated)
				{
					if (evaluation.getTotalPosts() > 0)
					{
						if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
						{
							evalStatus = Evaluation.EvaluationStatus.TO_BE_EVALUATED;
							evalStatusToBeEvaluated = true;
						}
					}
					else
					{
						if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
						{
							noPostsCount++;
						}				
					}
				}				
			}
			
			if (evaluation.getLastPostTime() != null && evaluation.getFirstPostTime() != null && evaluation.getLastPostTime().equals(evaluation.getFirstPostTime()))
			{
				((EvaluationImpl) evaluation).setLastPostTime(null);
			}
		}
		
		//sort evaluations
		sortEvaluations(evaluations, evalSort);
		
		//
		if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
		{
			filterForumGroupUsers(forum, evaluations);

			sortEvaluationGroups(evalSort, evaluations);
		}
		else
		{
			// set evaluation status
			if (evaluations.size() == noPostsCount)
			{
				evalStatus = Evaluation.EvaluationStatus.NO_POSTS_NO_EVALS;
			}

			((ForumImpl)forum).setEvaluationStatus(evalStatus);
		}
		
		forum.getEvaluations().clear();
		forum.getEvaluations().addAll(evaluations);
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting getForumEvaluationsWithPosts(Forum forum, Evaluation.EvaluationsSort evalSort, String sakaiUserId)......");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getForumEvaluationsWithPosts(int forumId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all) throws JForumAccessException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering getForumEvaluationsWithPosts(int forumId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all)......");
		}
		
		if (forumId <= 0)
		{
			throw new IllegalArgumentException("forum information is missing.");
		}
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Grade grade = getByForumId(forumId);
		
		if (grade == null)
		{
			return evaluations;
		}
		
		Forum forum = jforumForumService.getForum(forumId);
		
		Category category = forum.getCategory();
		
		if (!jforumSecurityService.isJForumFacilitator(category.getContext(), sakaiUserId))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// publish to gradebook if the open date of the grade is in the past
		Date now = new Date();
		if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))		
		{
			if (forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().before(now))
			{
				if (forum.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, forum);
					}
				}
			}
		}
		else 		
		{
			if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
			{
				if (category.getAccessDates().getOpenDate().before(now))
				{
					if (category.getAccessDates().isHideUntilOpen())
					{
						if (grade.getItemOpenDate() != null)
						{
							updateGradebook(grade, forum);
						}
					}
				}
			}
		}
		
		// sync sakai users
		jforumUserService.getSiteUsers(category.getContext(), 0, 0);
		
		evaluations.addAll(gradeDao.selectForumEvaluationsWithPosts(forumId, evalSort, all));
		
		Iterator<Evaluation> iterEvaluation = evaluations.iterator();
		
		Evaluation evaluation = null;
		String evalSakaiUserId = null;
		while (iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			evalSakaiUserId = evaluation.getSakaiUserId();
			
			// remove facilitators, observers and admin users
			if (jforumSecurityService.isJForumFacilitator(category.getContext(), evalSakaiUserId) || jforumSecurityService.isEtudesObserver(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove guests
			if  (jforumSecurityService.isUserGuest(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove teaching assistants
			if  (jforumSecurityService.isUserTeachingAssistant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// retain only participants
			if (!jforumSecurityService.isJForumParticipant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove inactive users
			if (!jforumSecurityService.isUserActive(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove the users with blocked role
			if (jforumSecurityService.isUserBlocked(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// set sakai display id
			try
			{
				org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(evalSakaiUserId);
				((EvaluationImpl)evaluation).setSakaiDisplayId(sakUser.getDisplayId());
			}
			catch (UserNotDefinedException e)
			{
				iterEvaluation.remove();
				continue;
			}
			
			if (evaluation.getLastPostTime() != null && evaluation.getFirstPostTime() != null && evaluation.getLastPostTime().equals(evaluation.getFirstPostTime()))
			{
				((EvaluationImpl) evaluation).setLastPostTime(null);
			}
		}
		
		//sort evaluations
		sortEvaluations(evaluations, evalSort);
		
		//
		if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
		{
			filterForumGroupUsers(forum, evaluations);

			sortEvaluationGroups(evalSort, evaluations);
		}
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting getForumEvaluationsWithPosts(int forumId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all)......");
		}
		
		return evaluations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getForumEvaluationsWithPosts(int forumId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped) throws JForumAccessException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering getForumEvaluationsWithPosts(int forumId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped)......");
		}
		
		if (forumId <= 0)
		{
			throw new IllegalArgumentException("forum information is missing.");
		}
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Grade grade = getByForumId(forumId);
		
		if (grade == null)
		{
			return evaluations;
		}
		
		Forum forum = jforumForumService.getForum(forumId);
		
		Category category = forum.getCategory();
		
		if (!jforumSecurityService.isJForumFacilitator(category.getContext(), sakaiUserId))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// publish to gradebook if the open date of the grade is in the past
		Date now = new Date();
		if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))		
		{
			if (forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().before(now))
			{
				if (forum.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, forum);
					}
				}
			}
		}
		else 		
		{
			if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
			{
				if (category.getAccessDates().getOpenDate().before(now))
				{
					if (category.getAccessDates().isHideUntilOpen())
					{
						if (grade.getItemOpenDate() != null)
						{
							updateGradebook(grade, forum);
						}
					}
				}
			}
		}
		
		// sync sakai users
		jforumUserService.getSiteUsers(category.getContext(), 0, 0);
		
		evaluations.addAll(gradeDao.selectForumEvaluationsWithPosts(forumId, evalSort, all));
		
		Iterator<Evaluation> iterEvaluation = evaluations.iterator();
		
		Evaluation evaluation = null;
		String evalSakaiUserId = null;
		while (iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			evalSakaiUserId = evaluation.getSakaiUserId();
			
			// remove facilitators, observers and admin users
			if (jforumSecurityService.isJForumFacilitator(category.getContext(), evalSakaiUserId) || jforumSecurityService.isEtudesObserver(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove guests
			if  (jforumSecurityService.isUserGuest(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove teaching assistants
			if  (jforumSecurityService.isUserTeachingAssistant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			if (!includeBlockedDropped)
			{
				// retain only participants
				if (!jforumSecurityService.isJForumParticipant(category.getContext(), evalSakaiUserId))
				{
					iterEvaluation.remove();
					continue;
				}
				
				
				// remove inactive users
				if (!jforumSecurityService.isUserActive(category.getContext(), evalSakaiUserId))
				{
					iterEvaluation.remove();
					continue;
				}
								
				// remove the users with blocked role
				if (jforumSecurityService.isUserBlocked(category.getContext(), evalSakaiUserId))
				{
					iterEvaluation.remove();
					continue;
				}
			}
			
			// set sakai display id
			try
			{
				org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(evalSakaiUserId);
				((EvaluationImpl)evaluation).setSakaiDisplayId(sakUser.getDisplayId());
			}
			catch (UserNotDefinedException e)
			{
				iterEvaluation.remove();
				continue;
			}
			
			if (evaluation.getLastPostTime() != null && evaluation.getFirstPostTime() != null && evaluation.getLastPostTime().equals(evaluation.getFirstPostTime()))
			{
				((EvaluationImpl) evaluation).setLastPostTime(null);
			}
		}
		
		//sort evaluations
		sortEvaluations(evaluations, evalSort);
		
		//
		if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
		{
			filterForumGroupUsers(forum, evaluations);

			sortEvaluationGroups(evalSort, evaluations);
		}
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting getForumEvaluationsWithPosts(int forumId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped)......");
		}
		
		return evaluations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getForumTopicEvaluationsCount(int forumId)
	{
		if (forumId <= 0) throw new IllegalArgumentException("Not a valid forum id.");
		
		return gradeDao.selectForumTopicEvaluationsCount(forumId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Grade> getForumTopicGradesByCategory(int categoryId)
	{
		if (categoryId <= 0) throw new IllegalArgumentException();
		
		return gradeDao.selectForumTopicGradesByCategoryId(categoryId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Grade> getForumTopicGradesByForum(int forumId)
	{
		return gradeDao.selectForumTopicGradesByForumId(forumId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation.EvaluationStatus getForumTopicsEvaluationStatus(int forumId, String sakaiUserId)
	{
		if (forumId <= 0 || (sakaiUserId == null || sakaiUserId.trim().length() == 0))
		{
			throw new IllegalArgumentException("forum id or sakaiUserId is not valid.");
		}
		
		List<Topic> gradableTopics = jforumPostService.getForumGradableTopics(forumId);
		
		Evaluation.EvaluationStatus evalStatus = Evaluation.EvaluationStatus.EVALUATED;	
		boolean statusFound = false;
		
		int topicsNoPostsNoEvalCount = 0;
		
		// eval status update
		for (Topic topic : gradableTopics)
		{
			try
			{
				((TopicImpl)topic).setCurrentSakaiUserId(sakaiUserId);
				
				// set evaluation status
				getTopicEvaluationsWithPosts(topic, Evaluation.EvaluationsSort.total_posts_d, sakaiUserId);
				
				evalStatus = topic.getEvaluationStatus();
				
				if (evalStatus == Evaluation.EvaluationStatus.TO_BE_EVALUATED)
				{
					evalStatus = Evaluation.EvaluationStatus.TO_BE_EVALUATED;
					statusFound = true;
					break;
				}
				if (evalStatus == Evaluation.EvaluationStatus.EVALUATED)
				{
					evalStatus = Evaluation.EvaluationStatus.EVALUATED;
					statusFound = true;
					break;
				}
				else if (evalStatus == Evaluation.EvaluationStatus.NO_POSTS_NO_EVALS)
				{
					topicsNoPostsNoEvalCount++;
				}
			}
			catch (JForumAccessException e)
			{
				return null;
			}
			
			if (statusFound)
			{
				break;
			}
		}
		
		if (topicsNoPostsNoEvalCount == gradableTopics.size())
		{
			evalStatus = Evaluation.EvaluationStatus.NO_POSTS_NO_EVALS;
		}
		
		return evalStatus;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getTopicEvaluations(int forumId, int topicId)
	{
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Grade grade = getByForumTopicId(forumId, topicId);
		
		if (grade != null)
		{
			evaluations.addAll(gradeDao.selectEvaluations(grade));
		}
		
		return evaluations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getTopicEvaluationsCount(int forumId, int topicId)
	{
		if (forumId <= 0 || topicId <= 0) throw new IllegalArgumentException("Not a valid forum or topic id.");
		
		Grade grade = gradeDao.selectByForumTopic(forumId, topicId);
		
		if (grade != null)
		{
			return gradeDao.selectEvaluationsCount(grade);
		}
		
		return 0;
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all) throws JForumAccessException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all)......");
		}
		
		if (topicId <= 0)
		{
			throw new IllegalArgumentException("topic information is missing.");
		}
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Topic topic = jforumPostService.getTopic(topicId);
		
		if (topic == null)
		{
			return evaluations;
		}
		
		Grade grade = getByForumTopicId(topic.getForumId(), topicId);
		
		if (grade == null)
		{
			return evaluations;
		}
		
		Forum forum = topic.getForum();
		
		Category category = forum.getCategory();
		
		if (!jforumSecurityService.isJForumFacilitator(category.getContext(), sakaiUserId))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// publish to gradebook if the open date of the grade is in the past
		Date now = new Date();
		if ((topic.getAccessDates() != null) && (topic.getAccessDates().getOpenDate() != null || topic.getAccessDates().getDueDate() != null || topic.getAccessDates().getAllowUntilDate() != null))		
		{
			if (topic.getAccessDates().getOpenDate() != null && topic.getAccessDates().getOpenDate().before(now))
			{
				if (topic.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, topic);
					}
				}
			}
		}
		else if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))		
		{
			if (forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().before(now))
			{
				if (forum.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, topic);
					}
				}
			}
		}
		else 		
		{
			if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
			{
				if (category.getAccessDates().getOpenDate().before(now))
				{
					if (category.getAccessDates().isHideUntilOpen())
					{
						if (grade.getItemOpenDate() != null)
						{
							updateGradebook(grade, topic);
						}
					}
				}
			}
		}
		
		// sync sakai users
		jforumUserService.getSiteUsers(category.getContext(), 0, 0);
		
		evaluations.addAll(gradeDao.selectTopicEvaluationsWithPosts(topicId, evalSort, all));
		
		Iterator<Evaluation> iterEvaluation = evaluations.iterator();
		
		Evaluation evaluation = null;
		String evalSakaiUserId = null;
		while (iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			evalSakaiUserId = evaluation.getSakaiUserId();
			
			// remove facilitators and admin users
			if (jforumSecurityService.isJForumFacilitator(category.getContext(), evalSakaiUserId) || jforumSecurityService.isEtudesObserver(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove guests
			if  (jforumSecurityService.isUserGuest(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove teaching assistants
			if  (jforumSecurityService.isUserTeachingAssistant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// retain only participants
			if (!jforumSecurityService.isJForumParticipant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove inactive users
			if (!jforumSecurityService.isUserActive(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove the users with blocked role
			if (jforumSecurityService.isUserBlocked(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// set sakai display id
			try
			{
				org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(evalSakaiUserId);
				((EvaluationImpl)evaluation).setSakaiDisplayId(sakUser.getDisplayId());
			}
			catch (UserNotDefinedException e)
			{
				iterEvaluation.remove();
				continue;
			}
			
			if (evaluation.getLastPostTime() != null && evaluation.getFirstPostTime() != null && evaluation.getLastPostTime().equals(evaluation.getFirstPostTime()))
			{
				((EvaluationImpl) evaluation).setLastPostTime(null);
			}
		}
		
		//sort evaluations
		sortEvaluations(evaluations, evalSort);
		
		//
		if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
		{
			filterTopicForumGroupUsers(topic, evaluations);

			sortEvaluationGroups(evalSort, evaluations);
		}
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all)......");
		}
		
		return evaluations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped) throws JForumAccessException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped)......");
		}
		
		if (topicId <= 0)
		{
			throw new IllegalArgumentException("topic information is missing.");
		}
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Topic topic = jforumPostService.getTopic(topicId);
		
		if (topic == null)
		{
			return evaluations;
		}
		
		Grade grade = getByForumTopicId(topic.getForumId(), topicId);
		
		if (grade == null)
		{
			return evaluations;
		}
		
		Forum forum = topic.getForum();
		
		Category category = forum.getCategory();
		
		if (!jforumSecurityService.isJForumFacilitator(category.getContext(), sakaiUserId))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// publish to gradebook if the open date of the grade is in the past
		Date now = new Date();
		if ((topic.getAccessDates() != null) && (topic.getAccessDates().getOpenDate() != null || topic.getAccessDates().getDueDate() != null || topic.getAccessDates().getAllowUntilDate() != null))		
		{
			if (topic.getAccessDates().getOpenDate() != null && topic.getAccessDates().getOpenDate().before(now))
			{
				if (topic.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, topic);
					}
				}
			}
		}
		else if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))		
		{
			if (forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().before(now))
			{
				if (forum.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, topic);
					}
				}
			}
		}
		else 		
		{
			if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
			{
				if (category.getAccessDates().getOpenDate().before(now))
				{
					if (category.getAccessDates().isHideUntilOpen())
					{
						if (grade.getItemOpenDate() != null)
						{
							updateGradebook(grade, topic);
						}
					}
				}
			}
		}
		
		// sync sakai users
		jforumUserService.getSiteUsers(category.getContext(), 0, 0);
		
		evaluations.addAll(gradeDao.selectTopicEvaluationsWithPosts(topicId, evalSort, all));
		
		Iterator<Evaluation> iterEvaluation = evaluations.iterator();
		
		Evaluation evaluation = null;
		String evalSakaiUserId = null;
		while (iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			evalSakaiUserId = evaluation.getSakaiUserId();
			
			// remove facilitators and admin users
			if (jforumSecurityService.isJForumFacilitator(category.getContext(), evalSakaiUserId) || jforumSecurityService.isEtudesObserver(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove guests
			if  (jforumSecurityService.isUserGuest(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove teaching assistants
			if  (jforumSecurityService.isUserTeachingAssistant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			if (!includeBlockedDropped)
			{
				// retain only participants
				if (!jforumSecurityService.isJForumParticipant(category.getContext(), evalSakaiUserId))
				{
					iterEvaluation.remove();
					continue;
				}
				
				// remove inactive users
				if (!jforumSecurityService.isUserActive(category.getContext(), evalSakaiUserId))
				{
					iterEvaluation.remove();
					continue;
				}
				
				// remove the users with blocked role
				if (jforumSecurityService.isUserBlocked(category.getContext(), evalSakaiUserId))
				{
					iterEvaluation.remove();
					continue;
				}
			}
			
			// set sakai display id
			try
			{
				org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(evalSakaiUserId);
				((EvaluationImpl)evaluation).setSakaiDisplayId(sakUser.getDisplayId());
			}
			catch (UserNotDefinedException e)
			{
				iterEvaluation.remove();
				continue;
			}
			
			if (evaluation.getLastPostTime() != null && evaluation.getFirstPostTime() != null && evaluation.getLastPostTime().equals(evaluation.getFirstPostTime()))
			{
				((EvaluationImpl) evaluation).setLastPostTime(null);
			}
		}
		
		//sort evaluations
		sortEvaluations(evaluations, evalSort);
		
		//
		if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
		{
			filterTopicForumGroupUsers(topic, evaluations);

			sortEvaluationGroups(evalSort, evaluations);
		}
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped)......");
		}
		
		return evaluations;		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void getTopicEvaluationsWithPosts(Topic topic, Evaluation.EvaluationsSort evalSort, String sakaiUserId) throws JForumAccessException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId)......");
		}
		
		if (topic == null || topic.getId() <= 0)
		{
			throw new IllegalArgumentException("topic information is missing.");
		}
		
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		
		Topic exisTopic = jforumPostService.getTopic(topic.getId());
		
		if (exisTopic == null)
		{
			return;
		}
		
		Grade grade = getByForumTopicId(exisTopic.getForumId(), exisTopic.getId());
		
		if (grade == null)
		{
			return;
		}
		
		Forum forum = exisTopic.getForum();
		
		Category category = forum.getCategory();
		
		if (!jforumSecurityService.isJForumFacilitator(category.getContext(), sakaiUserId))
		{
			throw new JForumAccessException(sakaiUserId);
		}
		
		// publish to gradebook if the open date of the grade is in the past
		Date now = new Date();
		if ((exisTopic.getAccessDates() != null) && (exisTopic.getAccessDates().getOpenDate() != null || exisTopic.getAccessDates().getDueDate() != null || exisTopic.getAccessDates().getAllowUntilDate() != null))		
		{
			if (exisTopic.getAccessDates().getOpenDate() != null && exisTopic.getAccessDates().getOpenDate().before(now))
			{
				if (exisTopic.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, topic);
					}
				}
			}
		}
		else if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))		
		{
			if (forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().before(now))
			{
				if (forum.getAccessDates().isHideUntilOpen())
				{
					if (grade.getItemOpenDate() != null)
					{
						updateGradebook(grade, topic);
					}
				}
			}
		}
		else 		
		{
			if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
			{
				if (category.getAccessDates().getOpenDate().before(now))
				{
					if (category.getAccessDates().isHideUntilOpen())
					{
						if (grade.getItemOpenDate() != null)
						{
							updateGradebook(grade, topic);
						}
					}
				}
			}
		}
		
		// sync sakai users
		jforumUserService.getSiteUsers(category.getContext(), 0, 0);
		
		evaluations.addAll(gradeDao.selectTopicEvaluationsWithPosts(exisTopic.getId(), evalSort, true));
		
		Iterator<Evaluation> iterEvaluation = evaluations.iterator();
		
		Evaluation evaluation = null;
		String evalSakaiUserId = null;
		
		// set evaluation status
		Evaluation.EvaluationStatus evalStatus = Evaluation.EvaluationStatus.EVALUATED;
		int noPostsCount = 0;
		boolean evalStatusToBeEvaluated = false;
		
		while (iterEvaluation.hasNext()) 
		{
			evaluation = iterEvaluation.next();
			
			evalSakaiUserId = evaluation.getSakaiUserId();
			
			// remove facilitators and admin users
			if (jforumSecurityService.isJForumFacilitator(category.getContext(), evalSakaiUserId) || jforumSecurityService.isEtudesObserver(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove guests
			if  (jforumSecurityService.isUserGuest(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove teaching assistants
			if  (jforumSecurityService.isUserTeachingAssistant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// retain only participants
			if (!jforumSecurityService.isJForumParticipant(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove inactive users
			if (!jforumSecurityService.isUserActive(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// remove the users with blocked role
			if (jforumSecurityService.isUserBlocked(category.getContext(), evalSakaiUserId))
			{
				iterEvaluation.remove();
				continue;
			}
			
			// set sakai display id
			try
			{
				org.sakaiproject.user.api.User sakUser = UserDirectoryService.getUser(evalSakaiUserId);
				((EvaluationImpl)evaluation).setSakaiDisplayId(sakUser.getDisplayId());
			}
			catch (UserNotDefinedException e)
			{
				iterEvaluation.remove();
			}
			
			if (forum.getAccessType() != Forum.ForumAccess.GROUPS.getAccessType())
			{
				if (!evalStatusToBeEvaluated)
				{
					if (evaluation.getTotalPosts() > 0)
					{
						if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
						{
							evalStatus = Evaluation.EvaluationStatus.TO_BE_EVALUATED;
							evalStatusToBeEvaluated = true;
						}
					}
					else
					{
						if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
						{
							noPostsCount++;
						}				
					}
				}				
			}
			
			if (evaluation.getLastPostTime() != null && evaluation.getFirstPostTime() != null && evaluation.getLastPostTime().equals(evaluation.getFirstPostTime()))
			{
				((EvaluationImpl) evaluation).setLastPostTime(null);
			}
		}
		
		//sort evaluations
		sortEvaluations(evaluations, evalSort);
		
		//
		if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
		{
			filterTopicForumGroupUsers(topic, evaluations);

			sortEvaluationGroups(evalSort, evaluations);
		}
		else
		{
			// set evaluation status
			if (evaluations.size() == noPostsCount)
			{
				evalStatus = Evaluation.EvaluationStatus.NO_POSTS_NO_EVALS;
			}

			((TopicImpl)topic).setEvaluationStatus(evalStatus);
		}
		
		topic.getEvaluations().clear();
		topic.getEvaluations().addAll(evaluations);
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId)......");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Grade> getTopicGradesByForum(int forumId)
	{
		if (forumId <= 0) throw new IllegalArgumentException();
		
		return gradeDao.selectTopicGradesByForumId(forumId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation getUserCategoryEvaluation(int categoryId, String sakaiUserId)
	{
		if (categoryId <= 0)
		{
			throw new IllegalArgumentException("Category information is missing.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User information is missing.");
		}
		
		Grade grade = getByCategoryId(categoryId);
		
		if (grade != null)
		{
			// publish to gradebook if the open date of the grade is in the past
			if (grade.getItemOpenDate() != null)
			{
				Date now = new Date();
				Category category = jforumCategoryService.getCategory(categoryId);
				
				if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))		
				{
					if (category.getAccessDates().getOpenDate().before(now))
					{
						if (category.getAccessDates().isHideUntilOpen())
						{
							updateGradebook(grade, category);
						}
					}
				}
			}
						
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);
			
			if (user != null)
			{
				return gradeDao.selectUserCategoryEvaluation(categoryId, user.getId());
			}			
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation getUserCategoryEvaluation(int categoryId, String sakaiUserId, boolean checkLatePosts)
	{
		if (categoryId <= 0)
		{
			throw new IllegalArgumentException("Category information is missing.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User information is missing.");
		}
		
		Grade grade = getByCategoryId(categoryId);
		
		if (grade != null)
		{
			// publish to gradebook if the open date of the grade is in the past
			if (grade.getItemOpenDate() != null)
			{
				Date now = new Date();
				Category category = jforumCategoryService.getCategory(categoryId);
				
				if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))		
				{
					if (category.getAccessDates().getOpenDate().before(now))
					{
						if (category.getAccessDates().isHideUntilOpen())
						{
							updateGradebook(grade, category);
						}
					}
				}
			}
			
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);
			
			if (user != null)
			{
				return gradeDao.selectUserCategoryEvaluation(categoryId, user.getId(), true);
			}			
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation getUserForumEvaluation(int forumId, String sakaiUserId)
	{
		if (forumId <= 0)
		{
			throw new IllegalArgumentException("Forum information is missing.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User information is missing.");
		}
		
		Grade grade = getByForumId(forumId);
		
		if (grade != null)
		{
			// publish to gradebook if the open date of the grade is in the past
			if (grade.getItemOpenDate() != null)
			{
				Date now = new Date();
				Forum forum = jforumForumService.getForum(forumId);
				if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))		
				{
					if (forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().before(now))
					{
						if (forum.getAccessDates().isHideUntilOpen())
						{
							updateGradebook(grade, forum);
						}
					}
				}
				else 		
				{
					Category category = forum.getCategory();
					
					if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
					{
						if (category.getAccessDates().getOpenDate().before(now))
						{
							if (category.getAccessDates().isHideUntilOpen())
							{
								updateGradebook(grade, forum);
							}
						}
					}
				}
			}
						
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);
			
			if (user != null)
			{
				return gradeDao.selectUserForumEvaluation(forumId, user.getId());
			}			
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation getUserForumEvaluation(int forumId, String sakaiUserId, boolean checkLatePosts)
	{
		if (forumId <= 0)
		{
			throw new IllegalArgumentException("Forum information is missing.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User information is missing.");
		}
		
		Grade grade = getByForumId(forumId);
		
		if (grade != null)
		{
			// publish to gradebook if the open date of the grade is in the past
			if (grade.getItemOpenDate() != null)
			{
				Date now = new Date();
				Forum forum = jforumForumService.getForum(forumId);
				if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))		
				{
					if (forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().before(now))
					{
						if (forum.getAccessDates().isHideUntilOpen())
						{
							updateGradebook(grade, forum);
						}
					}
				}
				else 		
				{
					Category category = forum.getCategory();
					
					if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
					{
						if (category.getAccessDates().getOpenDate().before(now))
						{
							if (category.getAccessDates().isHideUntilOpen())
							{
								updateGradebook(grade, forum);
							}
						}
					}
				}
			}
						
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);
			
			if (user != null)
			{
				return gradeDao.selectUserForumEvaluation(forumId, user.getId(), true);
			}			
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation.EvaluationReviewedStatus getUserForumTopicEvaluationsReviewedDisplayStatus(int forumId, int userId)
	{
		if (forumId <= 0 || userId <= 0)
		{
			throw new IllegalArgumentException("forum id or user id is not valid.");
		}
		
		// If there are no released evaluations return no evaluations
		if (gradeDao.selectUserForumTopicAccessibleEvaluationsCount(forumId, userId) == 0)
		{
			return Evaluation.EvaluationReviewedStatus.NO_EVALUATIONS;
		}
		
		List<Evaluation> userForumTopicEvaluations = gradeDao.selectUserForumTopicAccessibleEvaluations(forumId, userId);
		
		/* check for reviewed and not reviewed evaluations. If any of the released evaluations are not reviewed return not reviewed. 
		 * If all the released evaluations are reviewed return true.
		 */		
		Evaluation.EvaluationReviewedStatus reviewedStatus = Evaluation.EvaluationReviewedStatus.REVIEWED;
		
		for (Evaluation evaluation : userForumTopicEvaluations)
		{
			if (evaluation.isReleased())
			{
				if (evaluation.getReviewedDate() == null)
				{
					reviewedStatus = Evaluation.EvaluationReviewedStatus.NOT_REVIEWED;
					break;
				}
				else if (evaluation.getEvaluatedDate() != null && evaluation.getReviewedDate().before(evaluation.getEvaluatedDate()))
				{
					reviewedStatus = Evaluation.EvaluationReviewedStatus.NOT_REVIEWED;;
					break;
				}
			}			
		}
		
		return reviewedStatus;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getUserNotReviewedGradeEvaluationsCount(String siteId, String sakaiUserId)
	{
		if (siteId == null || siteId.trim().length() == 0 || sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("Site id or sakai user id is not valid");
		}
		
		User user = jforumUserService.getBySakaiUserId(sakaiUserId);
		
		int count  = 0;
		
		if (user != null)
		{
			/*ignore check for open dates*/
			List<Evaluation> evaluations = gradeDao.selectUserReleasedEvaluations(siteId, user.getId());
			
			for (Evaluation evaluation : evaluations)
			{
				if (evaluation.getReviewedDate() == null)
				{
					count++;
				}
				else if (evaluation.getEvaluatedDate() != null && evaluation.getReviewedDate().before(evaluation.getEvaluatedDate()))
				{
					count++;
				}
			}
		}
		
		return count;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getUserSiteEvaluations(String context, int userId)
	{
		if (context == null || context.trim().length() == 0 || userId <= 0)
		{
			throw new IllegalArgumentException("Context or userId is not valid.");
		}
		
		return gradeDao.selectUserSiteEvaluations(context, userId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation getUserTopicEvaluation(int topicId, String sakaiUserId)
	{
		if (topicId <= 0)
		{
			throw new IllegalArgumentException("Topic information is missing.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User information is missing.");
		}
		
		Topic topic = jforumPostService.getTopic(topicId);
				
		Grade grade = getByForumTopicId(topic.getForumId(), topic.getId());
		
		if (grade != null)
		{
			// publish to gradebook if the open date of the grade is in the past
			if (grade.getItemOpenDate() != null)
			{
				Date now = new Date();
								
				if ((topic.getAccessDates() != null) && (topic.getAccessDates().getOpenDate() != null || topic.getAccessDates().getDueDate() != null || topic.getAccessDates().getAllowUntilDate() != null))		
				{
					if (topic.getAccessDates().getOpenDate() != null && topic.getAccessDates().getOpenDate().before(now))
					{
						if (topic.getAccessDates().isHideUntilOpen())
						{
							updateGradebook(grade, topic);
						}
					}
				}
				else		
				{
					Forum forum = jforumForumService.getForum(topic.getForumId());
					
					if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))
					{						
						if (forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().before(now))
						{
							if (forum.getAccessDates().isHideUntilOpen())
							{
								updateGradebook(grade, topic);
							}
						}
					}
					else 		
					{
						Category category = forum.getCategory();
						
						if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
						{
							if (category.getAccessDates().getOpenDate().before(now))
							{
								if (category.getAccessDates().isHideUntilOpen())
								{
									updateGradebook(grade, topic);
								}
							}
						}
					}
				}				
			}
			
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);
			
			if (user != null)
			{
				return gradeDao.selectUserTopicEvaluation(topic.getForumId(), topicId, user.getId());
			}			
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation getUserTopicEvaluation(int topicId, String sakaiUserId, boolean checkLatePosts)
	{
		if (topicId <= 0)
		{
			throw new IllegalArgumentException("Topic information is missing.");
		}
		
		if (sakaiUserId == null || sakaiUserId.trim().length() == 0)
		{
			throw new IllegalArgumentException("User information is missing.");
		}
		
		Topic topic = jforumPostService.getTopic(topicId);
				
		Grade grade = getByForumTopicId(topic.getForumId(), topic.getId());
		
		if (grade != null)
		{
			// publish to gradebook if the open date of the grade is in the past
			if (grade.getItemOpenDate() != null)
			{
				Date now = new Date();
								
				if ((topic.getAccessDates() != null) && (topic.getAccessDates().getOpenDate() != null || topic.getAccessDates().getDueDate() != null || topic.getAccessDates().getAllowUntilDate() != null))		
				{
					if (topic.getAccessDates().getOpenDate() != null && topic.getAccessDates().getOpenDate().before(now))
					{
						if (topic.getAccessDates().isHideUntilOpen())
						{
							updateGradebook(grade, topic);
						}
					}
				}
				else		
				{
					Forum forum = jforumForumService.getForum(topic.getForumId());
					
					if ((forum.getAccessDates() != null) && (forum.getAccessDates().getOpenDate() != null || forum.getAccessDates().getDueDate() != null || forum.getAccessDates().getAllowUntilDate() != null))
					{						
						if (forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().before(now))
						{
							if (forum.getAccessDates().isHideUntilOpen())
							{
								updateGradebook(grade, topic);
							}
						}
					}
					else 		
					{
						Category category = forum.getCategory();
						
						if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))
						{
							if (category.getAccessDates().getOpenDate().before(now))
							{
								if (category.getAccessDates().isHideUntilOpen())
								{
									updateGradebook(grade, topic);
								}
							}
						}
					}
				}				
			}
						
			User user = jforumUserService.getBySakaiUserId(sakaiUserId);
			
			if (user != null)
			{
				return gradeDao.selectUserTopicEvaluation(topic.getForumId(), topicId, user.getId(), true);
			}			
		}
		
		return null;
	}
	
	public void init()
	{
		if (logger.isInfoEnabled())
			logger.info("init....");
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void markUserReviewedDate(Evaluation evaluation) throws JForumAccessException
	{
		if (evaluation == null || evaluation.getId() <= 0 || evaluation.getGradeId() <= 0 || evaluation.getSakaiUserId() == null || evaluation.getSakaiUserId().trim().length() == 0)
		{
			return;
		}
		
		Grade grade = gradeDao.selectGradeById(evaluation.getGradeId());
		
		if (grade == null)
		{
			return;
		}
		
		boolean isParticipant = jforumSecurityService.isJForumParticipant(grade.getContext(), evaluation.getSakaiUserId());
		
		if (!isParticipant)
		{
			throw new JForumAccessException(evaluation.getSakaiUserId());
		}
		
		gradeDao.updateUserReviewedDate(evaluation.getId());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyAddToGradeBookStatus(int gradeId, boolean addToGradeBook)
	{
		if (gradeId <= 0)
		{
			throw new IllegalArgumentException("Grade information is missing.");
		}
		
		gradeDao.updateAddToGradeBookStatus(gradeId, addToGradeBook);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyForumGrade(Grade grade)
	{
		if (grade == null)
		{
			new IllegalArgumentException("grade is missing");
		}
		
		if (grade.getId() == 0 || grade.getForumId() == 0 || grade.getTopicId() > 0 || grade.getCategoryId() > 0)
		{
			new IllegalArgumentException("grade information is missing");
		}
		
		gradeDao.updateForumGrade(grade);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void modifyTopicGrade(Grade grade)
	{
		if (grade == null)
		{
			new IllegalArgumentException("grade is missing");
		}
		
		if (grade.getId() == 0 || grade.getForumId() == 0 || grade.getTopicId() == 0 ||  grade.getCategoryId() > 0)
		{
			new IllegalArgumentException("grade information is missing");
		}
		
		gradeDao.updateTopicGrade(grade);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation newEvaluation(int gradeId, String evaluatedBySakaiUserId, String evaluationSakaiUserId)
	{
		if (gradeId <= 0 || evaluationSakaiUserId == null || evaluationSakaiUserId.trim().length() == 0)
		{
			return null;
		}
		
		Evaluation evaluation = new EvaluationImpl();
		((EvaluationImpl)evaluation).setId(-1);
		
		((EvaluationImpl)evaluation).setGradeId(gradeId);
		((EvaluationImpl)evaluation).setSakaiUserId(evaluationSakaiUserId);
		((EvaluationImpl)evaluation).setEvaluatedBySakaiUserId(evaluatedBySakaiUserId);
		
		return evaluation;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean removeGradebookEntry(Grade grade)
	{
		if (grade == null || grade.getContext() == null)
		{
			throw new IllegalArgumentException("grade is null.");
		}
		
		/*if (grade.isAddToGradeBook())
		{
			return false;
		}*/
		
		String gradebookUid = grade.getContext();
		
		if (jforumGBService.isExternalAssignmentDefined(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId())))
		{			
			return jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
		}
		
		// check and delete the record of existing grade schedule that needs to be published to gradebook 
		if (grade.getItemOpenDate() != null)
		{
			// delete existing grade schedule that needs to be published to gradebook
			gradeDao.deleteScheduledGradeToGradebook(grade.getId());
		}
		
		return false;
	}
	
	/**
	 * @param gradeDao the gradeDao to set
	 */
	public void setGradeDao(GradeDao gradeDao)
	{
		this.gradeDao = gradeDao;
	}
	
	/**
	 * @param jforumCategoryService the jforumCategoryService to set
	 */
	public void setJforumCategoryService(JForumCategoryService jforumCategoryService)
	{
		this.jforumCategoryService = jforumCategoryService;
	}

	/**
	 * @param jforumForumService the jforumForumService to set
	 */
	public void setJforumForumService(JForumForumService jforumForumService)
	{
		this.jforumForumService = jforumForumService;
	}
	
	/**
	 * @param jforumGBService 
	 * 				The jforumGBService to set
	 */
	public void setJforumGBService(JForumGBService jforumGBService)
	{
		this.jforumGBService = jforumGBService;
	}
	
	/**
	 * @param jforumPostService the jforumPostService to set
	 */
	public void setJforumPostService(JForumPostService jforumPostService)
	{
		this.jforumPostService = jforumPostService;
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
	 * @param siteService the siteService to set
	 */
	public void setSiteService(SiteService siteService)
	{
		this.siteService = siteService;
	}
	
		
	/**
	 * @param sqlService 
	 * 				The sqlService to set
	 */
	public void setSqlService(SqlService sqlService)
	{
		this.sqlService = sqlService;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean updateGradebook(Grade grade, Category category)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering updateGradebook(Grade grade, Category category)......");
		}
		
		if (grade == null || category == null)
		{
			throw new IllegalArgumentException("grade or category is null.");
		}
		
		String gradebookUid = grade.getContext();
		
		if (!jforumGBService.isGradebookDefined(gradebookUid))
		{
			return false;
		}
		
		// don't add to gradebook. Remove existing entry in the gradebook and remove and scheduled grade entry that to be added to gradebook
		if (!grade.isAddToGradeBook())
		{
			if (jforumGBService.isExternalAssignmentDefined(grade.getContext(), JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT + String.valueOf(grade.getId())))
			{
				jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
			}
			
			if (grade.getItemOpenDate() != null)
			{
				// delete existing grade schedule that needs to be published to gradebook
				gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
			}
			
			return false;
		}
		
		if (grade.getType() != Grade.GradeType.CATEGORY.getType())
		{
			return false;
		}
				
		// update gradebook
		String url = null;
		Date dueDate = null;
		if (category.getAccessDates() != null)
		{
			dueDate = category.getAccessDates().getDueDate();
		}
		
		Date now = new Date();
		
		// add to gradebook
		if ((category.getAccessDates() != null) && (category.getAccessDates().getOpenDate() != null))		
		{
			if (category.getAccessDates().getOpenDate().after(now))
			{
				if (category.getAccessDates().isHideUntilOpen())
				{
					if (jforumGBService.isExternalAssignmentDefined(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId())))
					{
						jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
					}
					// gradeDao.updateAddToGradeBookStatus(grade.getId(), false);
					
					// existing schedule
					if (grade.getItemOpenDate() != null)
					{
						// delete existing grade schedule that needs to be published to gradebook
						gradeDao.deleteScheduledGradeToGradebook(grade.getId());
					}
					
					// add the grade to gradebook when it opened
					gradeDao.addScheduledGradeToGradebook(grade.getId(), category.getAccessDates().getOpenDate());
					return false;
				}
				else
				{
					if (grade.getItemOpenDate() != null)
					{
						// delete existing grade schedule that needs to be published to gradebook
						gradeDao.deleteScheduledGradeToGradebook(grade.getId());
					}
					
				}
			}
			else
			{
				if (grade.getItemOpenDate() != null)
				{
					// delete existing grade schedule that needs to be published to gradebook
					gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
				}
			}
		}
		else
		{
			if (grade.getItemOpenDate() != null)
			{
				// delete existing grade schedule that needs to be published to gradebook
				gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
			}				
		}
		
		if (jforumGBService.isExternalAssignmentDefined(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId())))
		{
			// update gradebook for title, due date, points etc
			jforumGBService.updateExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()), url, category.getTitle(), JForumUtil.toDoubleScore(grade.getPoints()), dueDate);
			sendGradesToGradebook(grade, gradebookUid);
		}
		else
		{
			// add to gradebook
			if (jforumGBService.addExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()), url, category.getTitle(), JForumUtil.toDoubleScore(grade.getPoints()), dueDate, JForumGradeService.GRADE_SENDTOGRADEBOOK_DESCRIPTION))
			{
				sendGradesToGradebook(grade, gradebookUid);	
			}
			else
			{
				// update isAddToGradeBook of grade
				grade.setAddToGradeBook(false);
				modifyAddToGradeBookStatus(grade.getId(), false);
				
				return false;
			}
		}
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting updateGradebook(Grade grade, Category category)......");
		}
		
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean updateGradebook(Grade grade, Forum forum)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering updateGradebook(Grade grade, Forum forum)......");
		}
		
		if (grade == null || forum == null)
		{
			throw new IllegalArgumentException("grade or forum is null.");
		}
		
		String gradebookUid = grade.getContext();
		
		if (!jforumGBService.isGradebookDefined(gradebookUid))
		{
			return false;
		}
		
		if (!grade.isAddToGradeBook())
		{
			if (jforumGBService.isExternalAssignmentDefined(grade.getContext(), JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT + String.valueOf(grade.getId())))
			{
				jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
			}
			
			if (grade.getItemOpenDate() != null)
			{
				// delete existing grade scheduled that needs to be published to gradebook
				gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
			}
			
			return false;
		}
		
		if (grade.getType() != Grade.GradeType.FORUM.getType())
		{
			return false;
		}
		
		// update gradebook
		String url = null;
		Date dueDate = null;
		Date now = new Date();
		if (forum.getAccessType() == Forum.ForumAccess.DENY.getAccessType())
		{
			if (jforumGBService.isExternalAssignmentDefined(grade.getContext(), JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT + String.valueOf(grade.getId())))
			{
				jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
			}
			
			// existing schedule
			if (grade.getItemOpenDate() != null)
			{
				// delete existing grade schedule that needs to be published to gradebook
				gradeDao.deleteScheduledGradeToGradebook(grade.getId());
			}
			
			// update isAddToGradeBook of grade
			grade.setAddToGradeBook(false);
			modifyAddToGradeBookStatus(grade.getId(), false);
			
			return false;
		}
		else if (forum.getAccessDates() != null && ((forum.getAccessDates().getOpenDate() != null) || (forum.getAccessDates().getDueDate() != null) || (forum.getAccessDates().getAllowUntilDate() != null)))
		{
			//if (forum.getAccessDates() != null && forum.getAccessDates().getOpenDate() != null && forum.getAccessDates().getOpenDate().after(now) && forum.getAccessDates().isHideUntilOpen())
			if (forum.getAccessDates().getOpenDate() != null)
			{	
				if (forum.getAccessDates().getOpenDate().after(now))
				{
					if (forum.getAccessDates().isHideUntilOpen())
					{
						if (jforumGBService.isExternalAssignmentDefined(grade.getContext(), JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT + String.valueOf(grade.getId())))
						{
							jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
						}
						// gradeDao.updateAddToGradeBookStatus(grade.getId(), false);
						
						// existing schedule
						if (grade.getItemOpenDate() != null)
						{
							// delete existing grade schedule that needs to be published to gradebook
							gradeDao.deleteScheduledGradeToGradebook(grade.getId());
						}
						
						// add the grade to gradebook when it opened
						gradeDao.addScheduledGradeToGradebook(grade.getId(), forum.getAccessDates().getOpenDate());
						return false;
					}
					else
					{
						if (grade.getItemOpenDate() != null)
						{
							// delete existing grade schedule that needs to be published to gradebook
							gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
						}
					}
				}
				else
				{
					if (grade.getItemOpenDate() != null)
					{
						// delete existing grade schedule that needs to be published to gradebook
						gradeDao.deleteScheduledGradeToGradebook(grade.getId());					
					}
				}
			}
			else
			{
				if (grade.getItemOpenDate() != null)
				{
					// delete existing grade schedule that needs to be published to gradebook
					gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
				}
			}
			dueDate = forum.getAccessDates().getDueDate();
		}
		else
		{
		
			Category category = forum.getCategory();
			
			//if (category.getAccessDates() != null && category.getAccessDates().getOpenDate() != null && category.getAccessDates().getOpenDate().after(now) && category.getAccessDates().isHideUntilOpen())
			if (category.getAccessDates() != null && category.getAccessDates().getOpenDate() != null)
			{
				if (category.getAccessDates().getOpenDate().after(now))
				{
					if (category.getAccessDates().isHideUntilOpen())
					{
						if (jforumGBService.isExternalAssignmentDefined(grade.getContext(), JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT + String.valueOf(grade.getId())))
						{
							jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
						}
						// gradeDao.updateAddToGradeBookStatus(grade.getId(), false);
						
						// existing schedule
						if (grade.getItemOpenDate() != null)
						{
							// delete existing grade schedule that needs to be published to gradebook
							gradeDao.deleteScheduledGradeToGradebook(grade.getId());
						}
						
						// add the grade to gradebook when it opened
						gradeDao.addScheduledGradeToGradebook(grade.getId(), category.getAccessDates().getOpenDate());
						return false;
					}
					else
					{
						if (grade.getItemOpenDate() != null)
						{
							// delete existing grade schedule that needs to be published to gradebook
							gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
						}
					}
				}
				else
				{
					if (grade.getItemOpenDate() != null)
					{
						// delete existing grade schedule that needs to be published to gradebook
						gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
					}
				}
			}
			else
			{
				if (grade.getItemOpenDate() != null)
				{
					// delete existing grade schedule that needs to be published to gradebook
					gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
				}
			}
			
			if (category.getAccessDates() != null && category.getAccessDates().getDueDate() != null || category.getAccessDates().getOpenDate() != null || category.getAccessDates().getAllowUntilDate() != null)
			{
				dueDate = category.getAccessDates().getDueDate();
			}
		}
		
		if (jforumGBService.isExternalAssignmentDefined(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId())))
		{
			// update gradebook for title, due date, points etc
			jforumGBService.updateExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()), url, forum.getName(), JForumUtil.toDoubleScore(grade.getPoints()), dueDate);
			sendGradesToGradebook(grade, gradebookUid);
		}
		else
		{
			// add to gradebook
			if (jforumGBService.addExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()), url, forum.getName(), JForumUtil.toDoubleScore(grade.getPoints()), dueDate, JForumGradeService.GRADE_SENDTOGRADEBOOK_DESCRIPTION))
			{
				sendGradesToGradebook(grade, gradebookUid);	
			}
			else
			{
				// update isAddToGradeBook of grade
				grade.setAddToGradeBook(false);
				modifyAddToGradeBookStatus(grade.getId(), false);
				
				return false;
			}
		}
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting updateGradebook(Grade grade, Forum forum)......");
		}
		
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean updateGradebook(Grade grade, Topic topic)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Entering updateGradebook(Grade grade, Topic topic)......");
		}
		
		if (grade == null || topic == null)
		{
			throw new IllegalArgumentException("grade or topic is null.");
		}
		
		String gradebookUid = grade.getContext();
		if (!jforumGBService.isGradebookDefined(gradebookUid))
		{
			return false;
		}
		
		if (!grade.isAddToGradeBook())
		{
			if (jforumGBService.isExternalAssignmentDefined(grade.getContext(), JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT + String.valueOf(grade.getId())))
			{
				jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
			}
			
			if (grade.getItemOpenDate() != null)
			{
				// delete existing grade scheduled that needs to be published to gradebook
				gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
			}
			return false;
		}
		
		if (grade.getType() != Grade.GradeType.TOPIC.getType())
		{
			return false;
		}
		
		Date now = new Date();
		
		// update gradebook
		String url = null;
		Date dueDate = null;
		if (topic.getAccessDates() != null && ((topic.getAccessDates().getOpenDate() != null) || (topic.getAccessDates().getDueDate() != null) || (topic.getAccessDates().getAllowUntilDate() != null)))
		{
			if (topic.getAccessDates().getOpenDate() != null)
			{	
				if (topic.getAccessDates().getOpenDate().after(now))
				{
					if (topic.getAccessDates().isHideUntilOpen())
					{
						if (jforumGBService.isExternalAssignmentDefined(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId())))
						{
							jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
						}
						
						// existing schedule
						if (grade.getItemOpenDate() != null)
						{
							// delete existing grade schedule that needs to be published to gradebook
							gradeDao.deleteScheduledGradeToGradebook(grade.getId());
						}
						
						// add the grade to gradebook when it opened
						gradeDao.addScheduledGradeToGradebook(grade.getId(), topic.getAccessDates().getOpenDate());
						return false;
					}
					else
					{
						if (grade.getItemOpenDate() != null)
						{
							// delete existing grade schedule that needs to be published to gradebook
							gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
						}
					}
				}
				else
				{
					if (grade.getItemOpenDate() != null)
					{
						// delete existing grade schedule that needs to be published to gradebook
						gradeDao.deleteScheduledGradeToGradebook(grade.getId());					
					}
				}
			}
			else
			{
				if (grade.getItemOpenDate() != null)
				{
					// delete existing grade schedule that needs to be published to gradebook
					gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
				}
			}
			dueDate = topic.getAccessDates().getDueDate();
		
		}
		else
		{
			Forum forum = topic.getForum();
			
			if ((forum.getAccessDates() != null) && ((forum.getAccessDates().getOpenDate() != null) || (forum.getAccessDates().getDueDate() != null) || (forum.getAccessDates().getAllowUntilDate() != null)))
			{
				if (forum.getAccessDates().getOpenDate() != null)
				{	
					if (forum.getAccessDates().getOpenDate().after(now))
					{
						if (forum.getAccessDates().isHideUntilOpen())
						{
							if (jforumGBService.isExternalAssignmentDefined(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId())))
							{
								jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
							}
							
							// existing schedule
							if (grade.getItemOpenDate() != null)
							{
								// delete existing grade schedule that needs to be published to gradebook
								gradeDao.deleteScheduledGradeToGradebook(grade.getId());
							}
							
							// add the grade to gradebook when it opened
							gradeDao.addScheduledGradeToGradebook(grade.getId(), forum.getAccessDates().getOpenDate());
							return false;
						}
						else
						{
							if (grade.getItemOpenDate() != null)
							{
								// delete existing grade schedule that needs to be published to gradebook
								gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
							}
						}
					}
					else
					{
						if (grade.getItemOpenDate() != null)
						{
							// delete existing grade schedule that needs to be published to gradebook
							gradeDao.deleteScheduledGradeToGradebook(grade.getId());					
						}
					}
				}
				else
				{
					if (grade.getItemOpenDate() != null)
					{
						// delete existing grade schedule that needs to be published to gradebook
						gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
					}
				}
				dueDate = forum.getAccessDates().getDueDate();			
			}
			else
			{
				Category category = forum.getCategory();
				
				if (category.getAccessDates() != null && category.getAccessDates().getOpenDate() != null)
				{
					if (category.getAccessDates().getOpenDate().after(now))
					{
						if (category.getAccessDates().isHideUntilOpen())
						{
							if (jforumGBService.isExternalAssignmentDefined(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId())))
							{
								jforumGBService.removeExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()));
							}
							
							// existing schedule
							if (grade.getItemOpenDate() != null)
							{
								// delete existing grade schedule that needs to be published to gradebook
								gradeDao.deleteScheduledGradeToGradebook(grade.getId());
							}
							
							// add the grade to gradebook when it opened
							gradeDao.addScheduledGradeToGradebook(grade.getId(), category.getAccessDates().getOpenDate());
							return false;
						}
						else
						{
							if (grade.getItemOpenDate() != null)
							{
								// delete existing grade schedule that needs to be published to gradebook
								gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
							}
						}
					}
					else
					{
						if (grade.getItemOpenDate() != null)
						{
							// delete existing grade schedule that needs to be published to gradebook
							gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
						}
					}
				}
				else
				{
					if (grade.getItemOpenDate() != null)
					{
						// delete existing grade schedule that needs to be published to gradebook
						gradeDao.deleteScheduledGradeToGradebook(grade.getId());						
					}
				}
				
				if (category.getAccessDates() != null && category.getAccessDates().getDueDate() != null || category.getAccessDates().getOpenDate() != null || category.getAccessDates().getAllowUntilDate() != null)
				{
					dueDate = category.getAccessDates().getDueDate();
				}
			}
		}
		
		
		if (jforumGBService.isExternalAssignmentDefined(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId())))
		{
			// update gradebook for title, due date, points etc
			jforumGBService.updateExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()), url, topic.getTitle(), JForumUtil.toDoubleScore(grade.getPoints()), dueDate);
			sendGradesToGradebook(grade, gradebookUid);
		}
		else
		{
			// add to gradebook
			if (jforumGBService.addExternalAssessment(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT  + String.valueOf(grade.getId()), url, topic.getTitle(), JForumUtil.toDoubleScore(grade.getPoints()), dueDate, JForumGradeService.GRADE_SENDTOGRADEBOOK_DESCRIPTION))
			{
				sendGradesToGradebook(grade, gradebookUid);	
			}
			else
			{
				// update isAddToGradeBook of grade
				grade.setAddToGradeBook(false);
				modifyAddToGradeBookStatus(grade.getId(), false);
				
				return false;
			}
		}
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Exiting updateGradebook(Grade grade, Topic topic)......");
		}
		
		return true;
	}
	
	/**
	 * Checks for evaluation changes
	 * 
	 * @param exisEval	Existing evaluation
	 * 
	 * @param modEval	Modified evaluation
	 * 
	 * @return	true - if evaluation is modified
	 * 			false - if evaluation is not modified
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
	
	/**
	 * filters the forum group users
	 * 
	 * @param forum	Forum
	 * 
	 * @param evaluations	Evaluations
	 */
	protected void filterForumGroupUsers(Forum forum, List<Evaluation> evaluations)
	{
		if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
		{
			if ((forum.getGroups() != null) && (forum.getGroups().size() > 0))
			{
				Site site;
				try
				{
					Category category = forum.getCategory();
					site = siteService.getSite(category.getContext());
				}
				catch (IdUnusedException e)
				{
					return;
				}
				
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
				
				// set evaluation status
				Evaluation.EvaluationStatus evalStatus = Evaluation.EvaluationStatus.EVALUATED;
				int noPostsCount = 0;
				boolean evalStatusToBeEvaluated = false;
				
				// show users belong to the forum group
				for (Iterator<Evaluation> i = evaluations.iterator(); i.hasNext();)
				{
					Evaluation evaluation = i.next();
					if (!sakaiSiteGroupUserIds.contains(evaluation.getSakaiUserId()))
					{
						if (evaluation.getId() > 0)
						{
							gradeDao.deleteEvaluation(evaluation.getId());
						}
						i.remove();
					}
					else
					{
						Group group = sakaiUserGroups.get(evaluation.getSakaiUserId());
						((EvaluationImpl)evaluation).setUserSakaiGroupName(group.getTitle());
						
						// eval status
						if (!evalStatusToBeEvaluated)
						{
							if (evaluation.getTotalPosts() > 0)
							{
								if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
								{
									evalStatus = Evaluation.EvaluationStatus.TO_BE_EVALUATED;
									evalStatusToBeEvaluated = true;
								}
							}
							else
							{
								if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
								{
									noPostsCount++;
								}				
							}
						}
					}
				}
				
				// set evaluation status
				if (evaluations.size() == noPostsCount)
				{
					evalStatus = Evaluation.EvaluationStatus.NO_POSTS_NO_EVALS;
				}

				((ForumImpl)forum).setEvaluationStatus(evalStatus);
			}
			else
			{
				for (Iterator<Evaluation> i = evaluations.iterator(); i.hasNext();)
				{
					Evaluation evaluation = i.next();
					if (evaluation.getId() > 0)
					{
						gradeDao.deleteEvaluation(evaluation.getId());
					}
					i.remove();
				}
				((ForumImpl)forum).setEvaluationStatus(Evaluation.EvaluationStatus.NO_POSTS_NO_EVALS);
			}
		}
	}
	
	/**
	 * Filter the topic forum group users and sets evaluation status of the topic
	 * 
	 * @param topic	Topic
	 * 
	 * @param evaluations	Evaluations
	 */
	protected void filterTopicForumGroupUsers(Topic topic, List<Evaluation> evaluations)
	{
		Forum forum = topic.getForum();
		
		if (forum.getAccessType() == Forum.ForumAccess.GROUPS.getAccessType())
		{
			if ((forum.getGroups() != null) && (forum.getGroups().size() > 0))
			{
				Site site;
				try
				{
					Category category = forum.getCategory();
					site = siteService.getSite(category.getContext());
				}
				catch (IdUnusedException e)
				{
					return;
				}
				
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
				
				// set evaluation status
				Evaluation.EvaluationStatus evalStatus = Evaluation.EvaluationStatus.EVALUATED;
				int noPostsCount = 0;
				boolean evalStatusToBeEvaluated = false;
				
				// show users belong to the forum group
				for (Iterator<Evaluation> i = evaluations.iterator(); i.hasNext();)
				{
					Evaluation evaluation = i.next();
					if (!sakaiSiteGroupUserIds.contains(evaluation.getSakaiUserId()))
					{
						if (evaluation.getId() > 0)
						{
							gradeDao.deleteEvaluation(evaluation.getId());
						}
						i.remove();
					}
					else
					{
						Group group = sakaiUserGroups.get(evaluation.getSakaiUserId());
						((EvaluationImpl)evaluation).setUserSakaiGroupName(group.getTitle());
						
						// eval status
						if (!evalStatusToBeEvaluated)
						{
							if (evaluation.getTotalPosts() > 0)
							{
								if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
								{
									evalStatus = Evaluation.EvaluationStatus.TO_BE_EVALUATED;
									evalStatusToBeEvaluated = true;
								}
							}
							else
							{
								if ((evaluation.getScore() == null) && (evaluation.getComments() == null))
								{
									noPostsCount++;
								}				
							}
						}
					}
				}
				
				// set evaluation status
				if (evaluations.size() == noPostsCount)
				{
					evalStatus = Evaluation.EvaluationStatus.NO_POSTS_NO_EVALS;
				}

				((TopicImpl)topic).setEvaluationStatus(evalStatus);
			}
			else
			{
				for (Iterator<Evaluation> i = evaluations.iterator(); i.hasNext();)
				{
					Evaluation evaluation = i.next();
					if (evaluation.getId() > 0)
					{
						gradeDao.deleteEvaluation(evaluation.getId());
					}
					i.remove();
				}
				((TopicImpl)topic).setEvaluationStatus(Evaluation.EvaluationStatus.NO_POSTS_NO_EVALS);
			}
		}
	}
	
	/**
	 * Gets the grades
	 * 
	 * @param sql	The SQL query
	 * 
	 * @param fields	Query parameters
	 * 
	 * @return	The list of grades
	 */
	protected List<Grade> getGrades(String sql, Object[] fields)
	{
		List<Grade> grades = new ArrayList<Grade>();
		
		if (sql != null && fields != null)
		{
			grades.addAll(readGrades(sql.toString(), fields));
		}
		
		return grades;
	}
	
	/**
	 * 
	 * @param sql
	 * @param fields
	 * @return
	 */
	protected List<Grade> readGrades(String sql, Object[] fields)
	{
		final List<Grade> grades = new ArrayList<Grade>();
		
		this.sqlService.dbRead(sql, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Grade grade = new GradeImpl();
					((GradeImpl)grade).setId(result.getInt("grade_id"));
					grade.setContext(result.getString("context"));
					grade.setType(result.getInt("grade_type"));
					grade.setForumId(result.getInt("forum_id"));
					grade.setTopicId(result.getInt("topic_id"));
					grade.setPoints(result.getFloat("points"));
					grade.setAddToGradeBook(result.getInt("add_to_gradebook") == 1);
					grade.setCategoryId(result.getInt("categories_id"));
					grade.setMinimumPostsRequired(result.getInt("min_posts_required") == 1);
					grade.setMinimumPosts(result.getInt("min_posts"));
					grades.add(grade);

					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("readGrades: " + e, e);
					}
					return null;
				}
			}
		});
		
		return grades;
	}
	
	/**
	 * Send grades to grade book
	 * 
	 * @param grade		Grade
	 * 
	 * @param gradebookUid	Gradebook id
	 */
	protected void sendGradesToGradebook(Grade grade, String gradebookUid)
	{
		if (grade.isAddToGradeBook())
		{
			// send grades to gradebook
			List<Evaluation> evaluations = null;

			if (grade.getType() == Grade.GradeType.CATEGORY.getType())
			{
				evaluations = gradeDao.selectCategoryEvaluationsWithPosts(grade.getCategoryId(), null, true);
			}
			else if (grade.getType() == Grade.GradeType.FORUM.getType())
			{
				evaluations = gradeDao.selectForumEvaluationsWithPosts(grade.getForumId(), null, true);
			}
			else if (grade.getType() == Grade.GradeType.TOPIC.getType())
			{
				evaluations = gradeDao.selectTopicEvaluationsWithPosts(grade.getTopicId(), null, true);
			}
			
			if (evaluations == null)
			{
				return;
			}
			
			// remove instructor and observer
			Map<String, Double> scores = new HashMap<String, Double>();
			for(Evaluation eval: evaluations) 
			{
				if ((jforumSecurityService.isJForumFacilitator(grade.getContext(), eval.getSakaiUserId())) || (jforumSecurityService.isEtudesObserver(grade.getContext(), eval.getSakaiUserId()))
						|| (jforumSecurityService.isUserGuest(grade.getContext(), eval.getSakaiUserId())) || (jforumSecurityService.isUserTeachingAssistant(grade.getContext(), eval.getSakaiUserId())))
				{
					continue;
				}
								
				if (eval.isReleased())
				{
					String key = eval.getSakaiUserId();
					Float userScore = eval.getScore();
					scores.put(key, (userScore == null) ? null : JForumUtil.toDoubleScore(userScore));
				}
				else
				{
					String key = eval.getSakaiUserId();
					//Float userScore = eval.getScore();
					scores.put(key, null);
				}
			}
			jforumGBService.updateExternalAssessmentScores(gradebookUid, JForumGradeService.JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT + String.valueOf(grade.getId()), scores);
		}
	}
	
	/**
	 * sort evaluations for group titles
	 * 
	 * @param evalSort		evalution sort
	 * 
	 * @param evaluations	evalutions
	 */
	protected void sortEvaluationGroups(Evaluation.EvaluationsSort evalSort, List<Evaluation> evaluations)
	{
		if (evalSort == Evaluation.EvaluationsSort.group_title_a)
		{
			Collections.sort(evaluations, new Comparator<Evaluation>()
			{
				public int compare(Evaluation eval1, Evaluation eval2)
				{
					return eval1.getUserSakaiGroupName().compareTo(eval2.getUserSakaiGroupName());
				}
			});
		} 
		else if (evalSort == Evaluation.EvaluationsSort.group_title_d)
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
	
	/**
	 * Sorts evaluations
	 * 
	 * @param evaluations	Evalutions
	 * 
	 * @param evalSort	Evaluation sort
	 */
	protected void sortEvaluations(List<Evaluation> evaluations, Evaluation.EvaluationsSort evalSort)
	{
		if (evalSort == Evaluation.EvaluationsSort.scores_a)
		{
			Collections.sort(evaluations, new Comparator<Evaluation>()
			{
				/*
				 * used 1000.0f for null scores to appear negative values below
				 * 1000 in proper order
				 */
				public int compare(Evaluation eval1, Evaluation eval2)
				{
					Float f1 = eval1.getScore();
					if (f1 == null)
						f1 = Float.valueOf(1000.0f);
					Float f2 = eval2.getScore();
					if (f2 == null)
						f2 = Float.valueOf(1000.0f);

					int result = 0;
					result = f1.compareTo(f2);

					return result;
				}
			});
		}
		else if (evalSort == Evaluation.EvaluationsSort.scores_d)
		{
			/*
			 * used 1000.0f for null scores to appear negative values below 1000
			 * in proper order
			 */
			Collections.sort(evaluations, new Comparator<Evaluation>()
			{
				public int compare(Evaluation eval1, Evaluation eval2)
				{
					Float f1 = eval1.getScore();
					if (f1 == null)
						f1 = Float.valueOf(1000.0f);
					Float f2 = eval2.getScore();
					if (f2 == null)
						f2 = Float.valueOf(1000.0f);

					int result = 0;
					result = -1 * f1.compareTo(f2);

					return result;
				}
			});
		}
		else if (evalSort == Evaluation.EvaluationsSort.last_post_date_a)
		{
			Collections.sort(evaluations, new Comparator<Evaluation>()
			{
				public int compare(Evaluation eval1, Evaluation eval2)
				{
					if (eval1.getLastPostTime() == null && eval2.getLastPostTime() == null)
					{
						return 0;
					}
					
					if (eval2.getLastPostTime() == null)
					{
						return 1;
					}
					else if (eval1.getLastPostTime() == null)
					{
						return -1;
					}
					return -1 * (eval2.getLastPostTime().compareTo(eval1.getLastPostTime()));
					
				}
			});
		}
		else if (evalSort == Evaluation.EvaluationsSort.last_post_date_d)
		{
			Collections.sort(evaluations, new Comparator<Evaluation>()
			{
				public int compare(Evaluation eval1, Evaluation eval2)
				{
					if (eval1.getLastPostTime() == null && eval2.getLastPostTime() == null)
					{
						return 0;
					}
					
					if (eval1.getLastPostTime() == null)
					{
						return 1;
					}
					else if (eval2.getLastPostTime() == null)
					{
						return -1;
					}
					return eval2.getLastPostTime().compareTo(eval1.getLastPostTime());
				}
			});
		}
	}
	
	/**
	 * Updates the user evaluation entry in the gradebook
	 * 
	 * @param grade	Grade
	 * 
	 * @param evaluation	Evaluation
	 */
	protected void updateGradebookForUser(Grade grade, Evaluation evaluation)
	{
		if (grade == null || !grade.isAddToGradeBook() || evaluation == null) 
		{
			return;		
		}
		
		String gradebookUid = grade.getContext();
		
		if (!jforumGBService.isExternalAssignmentDefined(gradebookUid, "discussions-" + String.valueOf(grade.getId())))
		{
			return;
		}
		
		if ((evaluation.isReleased()) && evaluation.getScore() != null)
		{
			jforumGBService.updateExternalAssessmentScore(gradebookUid, "discussions-"+ String.valueOf(grade.getId()), evaluation.getSakaiUserId(), JForumUtil.toDoubleScore(evaluation.getScore()));
		}
		else
		{
			jforumGBService.updateExternalAssessmentScore(gradebookUid, "discussions-"+ String.valueOf(grade.getId()), evaluation.getSakaiUserId(), null);
		}
	}
}

