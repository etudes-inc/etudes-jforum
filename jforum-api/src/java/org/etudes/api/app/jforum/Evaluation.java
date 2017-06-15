/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/Evaluation.java $ 
 * $Id: Evaluation.java 7928 2014-05-07 19:10:15Z murthyt $ 
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
package org.etudes.api.app.jforum;

import java.util.Date;

public interface Evaluation
{
	/**
	 * Evaluation display status
	 *
	 */
	enum EvaluationReviewedStatus
	{
		NO_EVALUATIONS, NO_POSTS, NOT_RELEASED, NOT_REVIEWED, REVIEWED;
	}
	
	/**
	 * Sort options for evaluations
	 */
	enum EvaluationsSort
	{
		first_post_date_a, first_post_date_d, group_title_a, group_title_d, last_name_a, last_name_d, last_post_date_a, last_post_date_d, scores_a, scores_d, total_posts_a, total_posts_d
	}
	
	enum EvaluationStatus
	{
		EVALUATED, NO_POSTS_NO_EVALS, TO_BE_EVALUATED;
	}

	/**
	 * @return the firstPostTime
	 */
	public Date getFirstPostTime();
	
	/**
	 * Gets the comments
	 * 
	 * @return The comments
	 */
	String getComments();

	/**
	 * Gets the evaluated by jforum id
	 * 
	 * @return The evaluatedBy
	 */
	int getEvaluatedBy();	

	/**
	 * @return the evaluatedBySakaiUserId
	 */
	String getEvaluatedBySakaiUserId();
	
	/**
	 * Gets the evaluated date
	 * 
	 * @return The evaluatedDate
	 */
	Date getEvaluatedDate();
	
	/**
	 * Gets the user evaluation reviewed status
	 * 
	 * @return	The user evaluation reviewed status
	 */
	EvaluationReviewedStatus getEvaluationReviewedStatus();

	/**
	 * Gets the grade id
	 * 
	 * @return The gradeId
	 */
	int getGradeId();
	
	/**
	 * Gets the id
	 * 
	 * @return The id
	 */
	int getId();
	
	/**
	 * @return the lastPostTime
	 */
	Date getLastPostTime();

	/**
	 * Sets the user reviewed date
	 * 
	 * @return The reviewedDate
	 */
	Date getReviewedDate();
	
	/**
	 * @return the sakaiDisplayId
	 */
	String getSakaiDisplayId();

	/**
	 * Gets the sakai user id
	 * 
	 * @return The sakaiUserId
	 */
	String getSakaiUserId();
	
	/**
	 * Gets the score
	 * 
	 * @return The score
	 */
	Float getScore();

	/**
	 * @return the totalPosts
	 */
	int getTotalPosts();
	
	/**
	 * @return the userFirstName
	 */
	String getUserFirstName();
	
	/**
	 * Gets the user id
	 * 
	 * @return The userId
	 */
	int getUserId();
	
	/**
	 * @return the userLastName
	 */
	String getUserLastName();

	/**
	 * @return the username
	 */
	String getUsername();
	
	/**
	 * Sets the evaluated by jforum id
	 * 
	 * @param evaluatedBy The evaluatedBy to set
	 */
	//void setEvaluatedBy(int evaluatedBy);

	/**
	 * Sets the evaluated date
	 * 
	 * @param evaluatedDate The evaluatedDate to set
	 */
	//void setEvaluatedDate(Date evaluatedDate);
		
	/**
	 * Sets the grade id
	 * 
	 * @param gradeId The gradeId to set
	 */
	//void setGradeId(int gradeId);
	
	/**
	 * @return the userSakaiGroupName
	 */
	String getUserSakaiGroupName();

	/**
	 * @return 	true - if the user have posts after due date or user special access due date
	 * 			false - if the user all posts are before due date or user have no posts
	 */
	Boolean isLate();

	/**
	 * Sets the sakai user id
	 * 
	 * @param sakaiUserId The sakaiUserId to set
	 */
	//void setSakaiUserId(String sakaiUserId);
	
	/**
	 * Gets the is release
	 * 
	 * @return true - if the evaluation is released
	 * 			false - if the evaluation is not released
	 */
	Boolean isReleased();

	
	/**
	 * Sets the user id
	 * 
	 * @param userId The userId to set
	 */
	//void setUserId(int userId);
	
	/**
	 * Sets the comments
	 * 
	 * @param comments The comments to set
	 */
	void setComments(String comments);
	
	/**
	 * @param evaluatedBySakaiUserId the evaluatedBySakaiUserId to set
	 */
	void setEvaluatedBySakaiUserId(String evaluatedBySakaiUserId);
	
	/**
	 * Sets the released
	 * 
	 * @param released The released to set
	 */
	void setReleased(Boolean released);
	
	/**
	 * Gets the user reviewed date
	 * 
	 * @param reviewedDate The reviewedDate to set
	 */
	void setReviewedDate(Date reviewedDate);
	
	/**
	 * Sets the score
	 * 
	 * @param score The score to set
	 */
	void setScore(Float score);
}
