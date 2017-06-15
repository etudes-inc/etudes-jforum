/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/CategoryImpl.java $ 
 * $Id: CategoryImpl.java 7551 2014-03-07 19:33:36Z murthyt $ 
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
package org.etudes.component.app.jforum;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.AccessDates;
import org.etudes.api.app.jforum.Category;
import org.etudes.api.app.jforum.Evaluation;
import org.etudes.api.app.jforum.Forum;
import org.etudes.api.app.jforum.Grade;
import org.etudes.api.app.jforum.JForumAccessException;
import org.etudes.api.app.jforum.JForumGradeService;
import org.etudes.api.app.jforum.LastPostInfo;

public class CategoryImpl implements Category
{
	private static Log logger = LogFactory.getLog(CategoryImpl.class);
	
	protected AccessDates accessDates = null;
	
	protected Boolean blocked = Boolean.FALSE;
	
	protected String blockedByDetails;
	
	protected String blockedByTitle;
	
	protected String context  = null;
	
	protected String createdBySakaiUserId;
	
	protected List<Evaluation> evaluations = new ArrayList<Evaluation>();
	
	protected List<Forum> forums = new ArrayList<Forum>();
	
	protected Boolean gradable = Boolean.FALSE;
	
	protected Grade grade = null;
	
	protected int id;
	
	protected LastPostInfo lastPostInfo = null;
	
	protected String modifiedBySakaiUserId;
	
	protected int order;
	
	protected String title = null;
	
	protected Map<String, Integer> userPostCount = new HashMap<String, Integer>();
	
	protected transient String currentSakaiUserId = null;
	
	protected transient JForumGradeService jforumGradeService = null;
	
	protected Evaluation.EvaluationStatus evaluationStatus = null;
	
	protected Evaluation userEvaluation = null;
	
	public CategoryImpl(){}
	
	public CategoryImpl(JForumGradeService jforumGradeService)
	{
		this.jforumGradeService = jforumGradeService;
	}

	protected CategoryImpl(CategoryImpl other)
	{
		this.id = other.id;
		this.title = other.title;
		this.context = other.context;
		this.accessDates = new AccessDatesImpl((AccessDatesImpl)other.accessDates);
		this.gradable = other.gradable;	
		this.lastPostInfo = other.lastPostInfo;
		this.order = other.order;
		
		if (other.grade != null)
		{
			this.grade = new GradeImpl((GradeImpl)other.grade);
		}
		
		this.currentSakaiUserId = other.currentSakaiUserId;
		
		this.blocked = other.blocked;
		this.blockedByDetails = other.blockedByDetails;
		this.blockedByTitle = other.blockedByTitle;	
		
		this.modifiedBySakaiUserId = other.modifiedBySakaiUserId;
		this.jforumGradeService = other.jforumGradeService;
		
		// evaluations - retrieve if needed
		
		this.userEvaluation = other.userEvaluation;
	}

	/**
	 * @param blockedByDetails the blockedByDetails to set
	 */
	/** 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		// two categories are equals if they have the same id
		return ((obj instanceof Category) && (((Category)obj).getId() == this.id));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public AccessDates getAccessDates()
	{
		return this.accessDates;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean getBlocked()
	{
		return blocked;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getBlockedByDetails()
	{
		return blockedByDetails;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getBlockedByTitle()
	{
		return blockedByTitle;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContext()
	{
		return this.context;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getCreatedBySakaiUserId()
	{
		return createdBySakaiUserId;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Evaluation> getEvaluations()
	{
		return evaluations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation.EvaluationStatus getEvaluationStatus()
	{
		if ((!isGradable()) || (getGrade() == null))
		{
			return null;
		}
		
		if (getCurrentSakaiUserId() == null)
		{
			return null;
		}
		
		if (evaluationStatus != null)
		{
			return evaluationStatus;
		}

		try
		{
			// sets evaluation status
			jforumGradeService.getCategoryEvaluationsWithPosts(this, Evaluation.EvaluationsSort.total_posts_a, currentSakaiUserId);
		}
		catch (JForumAccessException e)
		{
			// user is not allowed or has no permission to fetch evaluations
			return null;
		}
		catch (Exception e)
		{
			if (logger.isWarnEnabled())
			{
				logger.warn(this.getClass().getName()+".getEvaluationStatus() : Error while fetching category evaluations: "+ e.toString());
				return null;
			}
		}
		
		return evaluationStatus;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<Forum> getForums()
	{
		return this.forums;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Grade getGrade()
	{
		if (this.grade != null)
		{
			return this.grade;
		}
		
		if (isGradable() && this.jforumGradeService != null && getId() > 0)
		{
			Grade grade = this.jforumGradeService.getByCategoryId(getId());
			this.setGrade(grade);
			
			return grade;
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getId()
	{
		return this.id;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public LastPostInfo getLastPostInfo()
	{
		return lastPostInfo;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getModifiedBySakaiUserId()
	{
		return modifiedBySakaiUserId;
	}
	
	/**
	 * @return the order
	 */
	public int getOrder()
	{
		return order;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getTitle()
	{
		return this.title;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Evaluation getUserEvaluation()
	{
		return userEvaluation;
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Integer> getUserPostCount()
	{
		return userPostCount;
	}

	/** 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() 
	{
		return this.id;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Boolean isGradable()
	{
		return this.gradable;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAccessDates(AccessDates accessDates)
	{
		this.accessDates = accessDates;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setBlocked(Boolean blocked)
	{
		this.blocked = blocked;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setBlockedByDetails(String blockedByDetails)
	{
		this.blockedByDetails = blockedByDetails;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setBlockedByTitle(String blockedByTitle)
	{
		this.blockedByTitle = blockedByTitle;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContext(String context)
	{
		this.context = context;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCreatedBySakaiUserId(String createdBySakaiUserId)
	{
		this.createdBySakaiUserId = createdBySakaiUserId;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setGradable(Boolean gradable)
	{
		this.gradable = gradable;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setGrade(Grade grade)
	{
		this.grade = grade;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id)
	{
		this.id = id;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLastPostInfo(LastPostInfo lastPostInfo)
	{
		this.lastPostInfo = lastPostInfo;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setModifiedBySakaiUserId(String modifiedBySakaiUserId)
	{
		this.modifiedBySakaiUserId = modifiedBySakaiUserId;
	}
	
	/**
	 * @param order the order to set
	 */
	public void setOrder(int order)
	{
		this.order = order;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * @param currentSakaiUserId the currentSakaiUserId to set
	 */
	protected void setCurrentSakaiUserId(String currentSakaiUserId)
	{
		this.currentSakaiUserId = currentSakaiUserId;
	}
	
	/**
	 * Sets the evaluation status
	 * 
	 * @param evaluationStatus	Gradable forum evaluation status
	 */
	protected void setEvaluationStatus(Evaluation.EvaluationStatus evaluationStatus)
	{
		this.evaluationStatus = evaluationStatus;
	}

	/**
	 * Sets the user evaluation for a gradable category. If the user posts are not graded the id is not set.
	 * 
	 * @param userEvaluation	User evaluation
	 */
	protected void setUserEvaluation(Evaluation userEvaluation)
	{
		this.userEvaluation = userEvaluation;
	}

	/**
	 * @return the currentSakaiUserId
	 */
	String getCurrentSakaiUserId()
	{
		return currentSakaiUserId;
	}
}
