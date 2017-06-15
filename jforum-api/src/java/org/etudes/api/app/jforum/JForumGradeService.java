/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/JForumGradeService.java $ 
 * $Id: JForumGradeService.java 10882 2015-05-19 00:37:57Z murthyt $ 
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
package org.etudes.api.app.jforum;

import java.util.List;

public interface JForumGradeService
{
	/**Gradebook item description**/
	public static final String GRADE_SENDTOGRADEBOOK_DESCRIPTION = "Discussions";
	
	/**Gradebook tool id**/
	public static final String GRADEBOOK_TOOL_ID = "sakai.gradebook.tool";
	
	/**Etudes gradebook tool id */
	public static final String ETUDES_GRADEBOOK_TOOL_ID = "e3.gradebook";
	
	/**JForum Gradebook external id concatenation text**/
	public static final String JFORUM_GRADEBOOK_EXTERNAL_ID_CONCAT = "discussions-";

	/**
	 * adds and modifies the category evaluations. Also deletes evaluations with no score and no comments
	 * 
	 * @param grade		Category grade
	 * 
	 * @param evaluations	Evaluations with grade id, evaluated by sakai user id,  evaluation sakai user id, score , comments and other information
	 * 
	 * @throws JForumAccessException If user has no access to evaluations
	 */
	public void addModifyCategoryEvaluations(Grade grade, List<Evaluation> evaluations) throws JForumAccessException;
	
	/**
	 * adds and modifies the forum evaluations. Also deletes evaluations with no score and no comments
	 * 
	 * @param grade		Forum grade
	 * 
	 * @param evaluations	Evaluations with grade id, evaluated by sakai user id,  evaluation sakai user id, score, comments and other information
	 * 
	 * @throws JForumAccessException If user has no access to evaluations
	 */
	public void addModifyForumEvaluations(Grade grade, List<Evaluation> evaluations) throws JForumAccessException;
	
	/**
	 * adds and modifies the topic evaluations. Also deletes evaluations with no score and no comments
	 * 
	 * @param grade		Topic grade
	 * 
	 * @param evaluations	Evaluations with grade id, evaluated by sakai user id,  evaluation sakai user id, score, comments and other information
	 * 
	 * @throws JForumAccessException If user has no access to evaluations
	 */
	public void addModifyTopicEvaluations(Grade grade, List<Evaluation> evaluations) throws JForumAccessException;	

	/**
	 * Creates the user evaluation if not exists or modifies the existing user evaluation
	 * 
	 * @param evaluation	Evaliation
	 * 
	 * @throws JForumAccessException	If user has no access to evaluate
	 */
	public void addModifyUserEvaluation(Evaluation evaluation) throws JForumAccessException;
	
	/**
	 * Assigns zero to non submitters who has not posted any posts in the category
	 * 
	 * @param categoryId				Category id
	 * 
	 * @param evaluatedBySakaiUserId	Evaluated by sakai user id
	 * 
	 * @throws JForumAccessException	If user has no access to grade the category
	 */
	public void assignZeroCategoryNonSubmitters(int categoryId, String evaluatedBySakaiUserId) throws JForumAccessException;
	
	/**
	 * Assigns zero to non submitters who has not posted any posts in the forum
	 * 
	 * @param forumId					Forum id
	 * 
	 * @param evaluatedBySakaiUserId	Evaluated by sakai user id
	 * 
	 * @throws JForumAccessException	If user has no access to grade the category
	 */
	public void assignZeroForumNonSubmitters(int forumId, String evaluatedBySakaiUserId) throws JForumAccessException;
	
	/**
	 * Assigns zero to non submitters who has not posted any posts in the topic
	 * 
	 * @param forumId					Forum id
	 *
	 * @param topicId					Topic id
	 * 
	 * @param evaluatedBySakaiUserId	Evaluated by sakai user id
	 * 
	 * @throws JForumAccessException	If user has no access to grade the category
	 */
	public void assignZeroTopicNonSubmitters(int forumId, int topicId, String evaluatedBySakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the grade for the category
	 * 
	 * @param categoryId	Category id
	 * 
	 * @return	The grade or null if not existing
	 */
	public Grade getByCategoryId(int categoryId);
	
	/**
	 * Gets the grade for the forum
	 * 
	 * @param forumId	forum id
	 * 
	 * @return	The grade or null if not existing
	 */
	public Grade getByForumId(int forumId);
	
	/**
	 * Gets the grade for the topic
	 * 
	 * @param forumId	forum id
	 * 
	 * @param topicId	topic id
	 * 
	 * @return	The grade or null if not existing
	 */
	public Grade getByForumTopicId(int forumId, int topicId);
	
	/**
	 * Gets the grades of the site
	 * 
	 * @param siteId	The site id
	 * 
	 * @return	The list of grades in the site
	 */
	public List<Grade> getBySite(String siteId);	
	
	/**
	 * Gets the grades for a grade type for a site
	 * 
	 * @param siteId	The site id
	 * 
	 * @param gradeType	
	 * 					The grade types are Grade.GRADE_DISABLED, GRADE_BY_TOPIC, GRADE_BY_FORUM, GRADE_BY_CATEGORY
	 * 					// TODO : change grade type to use enum
	 * 
	 * @return The list of grades in the site for the grade type
	 */
	public List<Grade> getBySiteByGradeType(String siteId, int gradeType);
	
	/**
	 * get gradable category evaluations
	 * 
	 * @param CategoryId	The category id
	 * 
	 * @return	List of evaluations of the gradable category
	 */
	public List<Evaluation> getCategoryEvaluations(int categoryId);
	
	/**
	 * Gets the gradable category evaluations counts
	 * 
	 * @param categoryId	Category id
	 * 
	 * @return	The evaluations count of the gradable category
	 */
	public int getCategoryEvaluationsCount(int categoryId);
	
	/**
	 * Gets and sets all evaluations with posts count of a gradable category including evaluation status, late information, and last post time
	 * 
	 * @param category	Category
	 * 
	 * @param evalSort	Evaluation sort
	 * 
	 * @param sakaiUserId	Sakai user id of the user who is fetching the evaluations
	 * 
	 * @throws JForumAccessException	If user has no access to evaluations
	 */
	public void getCategoryEvaluationsWithPosts(Category category, Evaluation.EvaluationsSort evalSort, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets gradable category evaluations with posts count, late information, and last post time
	 * 
	 * @param categoryId	The category id
	 * 
	 * @param evalSort	Evaluation sort
	 * 
	 * @param sakaiUserId 	Sakai user id of the user who is fetching the evaluations
	 * 
	 * @param all All evaluations or evaluations with posts
	 * 
	 * @return	The gradable category evaluations with posts count, late information, and last post time
	 * 
	 * @throws JForumAccessException If user has no access to evaluations
	 */
	public List<Evaluation> getCategoryEvaluationsWithPosts(int categoryId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all) throws JForumAccessException;
	
	/**
	 * Gets gradable category evaluations with posts count, late information, and last post time
	 * 
	 * @param categoryId	The category id
	 * 
	 * @param evalSort	Evaluation sort
	 * 
	 * @param sakaiUserId 	Sakai user id of the user who is fetching the evaluations
	 * 
	 * @param all All evaluations or evaluations with posts
	 * 
	 * @param includeBlockedDropped		true	- includes	blocked dropped users
	 * 									false	- blocked dropped users not included
	 * 
	 * @return	The gradable category evaluations with posts count, late information, and last post time
	 * 
	 * @throws JForumAccessException If user has no access to evaluations
	 */
	public List<Evaluation> getCategoryEvaluationsWithPosts(int categoryId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped) throws JForumAccessException;
	
	/**
	 * Gets the gradable forums count for a category
	 * 
	 * @param categoryId	Category id
	 * 
	 * @return	Count of gradable forums for a category
	 */
	public int getCategoryGradableForumsCount(int categoryId);
	
	/**
	 * get gradable forum evaluations
	 * 
	 * @param forumId	The forum id
	 * 
	 * @return	List of evaluations of the gradable forum
	 */
	public List<Evaluation> getForumEvaluations(int forumId);
	
	/**
	 * Gets the forum evaluations count
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return	The forum evaluations count
	 */
	public int getForumEvaluationsCount(int forumId);
	
	/**
	 * Gets and sets all evaluations with posts count of a gradable forum including evaluation status
	 * 
	 * @param forum		Forum
	 * 
	 * @param evalSort	Evaluation sort
	 * 
	 * @param sakaiUserId	Sakai user id of the user who is fetching the evaluations
	 * 
	 * @throws JForumAccessException
	 */
	public void getForumEvaluationsWithPosts(Forum forum, Evaluation.EvaluationsSort evalSort, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets gradable forum evaluations with posts count
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param evalSort	Evaluation sort
	 * 
	 * @param sakaiUserId	Sakai user id of the user who is fetching the evaluations
	 * 
	 * @param all All evaluations or evaluations with posts
	 * 
	 * @return	The gradable forum evalautions with posts count
	 * 
	 * @throws JForumAccessException	If user has no access to evaluations
	 */
	public List<Evaluation> getForumEvaluationsWithPosts(int forumId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all) throws JForumAccessException;
	
	/**
	 * Gets gradable forum evaluations with posts count
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param evalSort	Evaluation sort
	 * 
	 * @param sakaiUserId	Sakai user id of the user who is fetching the evaluations
	 * 
	 * @param all All evaluations or evaluations with posts
	 * 
	 * @param includeBlockedDropped		true	- includes	blocked dropped users
	 * 									false	- blocked dropped users not included
	 * 
	 * @return	The gradable forum evalautions with posts count
	 * 
	 * @throws JForumAccessException	If user has no access to evaluations
	 */
	public List<Evaluation> getForumEvaluationsWithPosts(int forumId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped) throws JForumAccessException;
		
	/**
	 * Gets the topic evauations count of the forum
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return	The topic evaluations count of the forum
	 */
	public int getForumTopicEvaluationsCount(int forumId);
	
	/**
	 * Gets the grades of the gradable forums and topics of the category
	 * 
	 * @param categoryId	Category id
	 * 
	 * @return	List of the grades of gradable forums and topics of the category
	 */
	public List<Grade> getForumTopicGradesByCategory(int categoryId);
	
	/**
	 * Gets the topic grades for a forum
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return	The topic grades for a forum
	 */
	public List<Grade> getForumTopicGradesByForum(int forumId);
	
	/**
	 * Gets the forum topics evaluation status
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The forum topics evaluation status
	 */
	public Evaluation.EvaluationStatus getForumTopicsEvaluationStatus(int forumId, String sakaiUserId);
	
	/**
	 * get gradable topic evaluations
	 * 
	 * @param forumId	The forum id
	 * 
	 * @param topicId	The topic id
	 * 
	 * @return	List of evaluations of the gradable topic
	 */
	public List<Evaluation> getTopicEvaluations(int forumId, int topicId);
	
	/**
	 * Gets the gradable topic evaluations count
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param topicId	Topic id
	 * 
	 * @return	The topic evaluations count
	 */
	public int getTopicEvaluationsCount(int forumId, int topicId);
	
	/**
	 * Gets gradable topic evaluations with posts count
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param evalSort	Evaluation sort
	 * 
	 * @param sakaiUserId	Sakai user id of the user who is fetching the evaluations
	 * 
	 * @param all All evaluations or evaluations with posts
	 * 
	 * @return	The gradable topic evalautions with posts count
	 * 
	 * @throws JForumAccessException	If user has no access to evaluations
	 */
	public List<Evaluation> getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all) throws JForumAccessException;
	
	/**
	 * Gets gradable topic evaluations with posts count
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param evalSort	Evaluation sort
	 * 
	 * @param sakaiUserId	Sakai user id of the user who is fetching the evaluations
	 * 
	 * @param all All evaluations or evaluations with posts
	 * 
	 * @param includeBlockedDropped		true	- includes	blocked dropped users
	 * 									false	- blocked dropped users not included
	 * 
	 * @return	The gradable topic evalautions with posts count
	 * 
	 * @throws JForumAccessException	If user has no access to evaluations
	 */
	public List<Evaluation> getTopicEvaluationsWithPosts(int topicId, Evaluation.EvaluationsSort evalSort, String sakaiUserId, boolean all, boolean includeBlockedDropped) throws JForumAccessException;
	
	/**
	 * Gets and sets all evaluations with posts count of a gradable topic including evaluation status
	 * 
	 * @param topic	Topic
	 * 
	 * @param evalSort	Evaluation sort
	 * 
	 * @param sakaiUserId	Sakai user id of the user who is fetching the evaluations
	 * 
	 * @throws JForumAccessException	If user has no access to evaluations
	 */
	public void getTopicEvaluationsWithPosts(Topic topic, Evaluation.EvaluationsSort evalSort, String sakaiUserId) throws JForumAccessException;
	
	/**
	 * Gets the grades of the gradable topics of the forum
	 * 
	 * @param forumId	Forum id
	 * 
	 * @return List of the grades of the gradable topics of the forum
	 */
	public List<Grade> getTopicGradesByForum(int forumId);
	
	/**
	 * Gets the user category evaluation
	 * 
	 * @param categoryId	Category id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	If existing the user category evaluation or null
	 */
	public Evaluation getUserCategoryEvaluation(int categoryId, String sakaiUserId);
	
	/**
	 * Gets the user category evaluation with late posts information
	 * 
	 * @param categoryId	Category id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @param checkLatePosts true - Checks for user late posts
	 * 						 false - don't check for user late posts
	 * 
	 * @return	The user category evaluation with late posts information if there are user posts or user evaluated. Or null if there are no posts and user is not evaluated
	 */
	public Evaluation getUserCategoryEvaluation(int categoryId, String sakaiUserId, boolean checkLatePosts);
	
	/**
	 * Gets the user forum evaluation with late posts information
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	If existing the user category evaluation or null
	 */
	public Evaluation getUserForumEvaluation(int forumId, String sakaiUserId);
	
	/**
	 * Gets the user forum evaluation
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param sakaiUserId	Sakai user id
	 *
	 * @param checkLatePosts true - Checks for user late posts
	 * 						 false - don't check for user late posts
	 * 
	 * @return	The user forum evaluation with late posts information if there are user posts or user evaluated. Or null if there are no posts and user is not evaluated
	 */
	public Evaluation getUserForumEvaluation(int forumId, String sakaiUserId, boolean checkLatePosts);
	
	/**
	 * Gets the user forum topics released evaluations
	 * 
	 * @param forumId	Forum id
	 * 
	 * @param userId	User id
	 * 
	 * @return	The user forum topics released evaluations
	 */
	public Evaluation.EvaluationReviewedStatus getUserForumTopicEvaluationsReviewedDisplayStatus(int forumId, int userId);
	
	/**
	 * Gets the user not reviewed grade evaluations count in the site. Open dates of gradable items are ignored.
	 * 
	 * @param siteId	Site id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The user not reviewed grade evaluations count in the site
	 */
	public int getUserNotReviewedGradeEvaluationsCount(String siteId, String sakaiUserId);
	
	/**
	 * Gets the grade evaluations of the user in the site
	 * 
	 * @param context	Context or site id
	 * 	
	 * @param userId	User id
	 * 
	 * @return	The grade items evaluations of the user in the site
	 */
	public List<Evaluation> getUserSiteEvaluations(String context, int userId);
	
	/**
	 * Gets the user topic evaluation
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	If existing the user topic evaluation or null
	 */
	public Evaluation getUserTopicEvaluation(int topicId, String sakaiUserId);
	
	/**
	 * Gets the user topic evaluation with late posts information
	 * 
	 * @param topicId	Topic id
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	The user topic evaluation with late posts information if there are user posts or user evaluated. Or null if there are no posts and user is not evaluated
	 */
	public Evaluation getUserTopicEvaluation(int topicId, String sakaiUserId, boolean checkLatePosts);
	
	/**
	 * Marks user evaluation review date
	 * 
	 * @param evaluation	Evaluation
	 * 
	 * @throws JForumAccessException	If evaluation user is not part of the site
	 */
	public void markUserReviewedDate(Evaluation evaluation) throws JForumAccessException;
	
	/**
	 * Modifies add to gradebook status
	 * 
	 * @param gradeId	Grade id
	 * 
	 * @param addToGradeBook	true - if grades are added to gradebook
	 * 							false - if grades are not added to gradebook
	 */
	public void modifyAddToGradeBookStatus(int gradeId, boolean addToGradeBook);
	
	/**
	 * Modify forum grade
	 * 
	 * @param grade	Grade
	 */
	public void modifyForumGrade(Grade grade);
	
	/**
	 * Modify topic grade
	 * 
	 * @param grade	Grade
	 */
	public void modifyTopicGrade(Grade grade);
	
	/**
	 * Creates new evaluation
	 * 
	 * @param gradeId	Grade id
	 * 
	 * @param evaluatedBySakaiUserId	Evaluated by sakai user id
	 * 
	 * @param evaluationSakaiUserId		Sakai user id of the evaluation
	 * 
	 * @return	Evaluation with grade id, evaluated by sakai user id and evaluation sakai user id
	 */
	public Evaluation newEvaluation(int gradeId, String evaluatedBySakaiUserId, String evaluationSakaiUserId);
	
	/**
	 * Remove entry in the gradebook
	 * 
	 * @param grade		Grade
	 
	 * @return	true - If the entry is removed from gradebook
	 * 			false - If the entry is not in gradebook or not removed
	 */
	public boolean removeGradebookEntry(Grade grade);
	
	/**
	 * Updates gradebook if the entry is in the gradebook or adds to the gradebook
	 * 
	 * @param grade		Grade
	 * 
	 * @param category	Category
	 * 
	 * @return	true - If the gradebook is updated or added
	 * 			false - If the gradebook tool in not existing
	 */
	public boolean updateGradebook(Grade grade, Category category);
	
	/**
	 * Update gradebook if the entry is in the gradebook or adds to the gradebook
	 * 
	 * @param grade		Grade
	 * 
	 * @param forum		Forum
	 * 
	 * @return	true - If the gradebook is updated
	 * 			false - If the gradebook tool in not existing
	 */
	public boolean updateGradebook(Grade grade, Forum forum);
	
	/**
	 * Update gradebook if the entry is in the gradebook or adds to the gradebook
	 * 
	 * @param grade		Grade
	 * 
	 * @param topic		Topic
	 * 
	 * @return	true - If the gradebook is updated
	 * 			false - If the gradebook tool in not existing
	 */
	public boolean updateGradebook(Grade grade, Topic topic);
}