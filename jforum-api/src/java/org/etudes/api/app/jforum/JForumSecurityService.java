/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/JForumSecurityService.java $ 
 * $Id: JForumSecurityService.java 9155 2014-11-10 19:35:49Z murthyt $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010, 2011 Etudes, Inc. 
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

public interface JForumSecurityService
{
	/** For the users who can administrate - This is not used */
	public static final String ROLE_ADMIN="jforum.admin";
	
	/** For the users who can manage */
	public static final String ROLE_FACILITATOR="jforum.manage";
	
	/** For the users who can participate */
	public static final String ROLE_PARTICIPANT="jforum.member";
	
	/** For the users who is teaching assistant */
	public static final String ROLE_TEACHING_ASSISTANT="section.role.ta";
		

	/**
	 * Check the role of etudes site observer
	 * 
	 * @param context	Context
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	True - if the user has etudes observer role
	 * 			False - if the user has no etudes observer role
	 */
	public Boolean isEtudesObserver(String context, String sakaiUserId);
	
	/**
	 * Check to see if the current user is jforum facilitator in the current site
	 * 
	 * @return	True - if the user is facilitator
	 * 			False - if the user is not facilitator
	 * 				
	 */
	public Boolean isJForumFacilitator();
	
	/**
	 * Check to see if the current user is jforum facilitator
	 * 
	 * @param context	Context
	 * 
	 * @return	True - if the user is facilitator
	 * 			False - if the user is not facilitator
	 * 				
	 */
	public Boolean isJForumFacilitator(String context);
	
	/**
	 *  Check to see if the user is jforum facilitator
	 *  
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 	
	 * @return	True - if the user is facilitator
	 * 			False - if the user is not facilitator
	 */
	public Boolean isJForumFacilitator(String context, String userId);
	
	/**
	 * Check to see if the current user is jforum participant in the current site
	 * 
	 * @param context	Context
	 * 
	 * @return	True - if the user is participant
	 * 			False - if the user is not participant
	 * 				
	 */
	public Boolean isJForumParticipant();
	
	/**
	 * Check to see if the current user is jforum participant
	 * 
	 * @param context	Context
	 * 
	 * @return	True - if the user is participant
	 * 			False - if the user is not participant
	 * 				
	 */
	public Boolean isJForumParticipant(String context);
	
	/**
	 *  Check to see if the user is jforum participant
	 *  
	 * @param context	Context
	 * 
	 * @param userId	User id
	 * 	
	 * @return	True - if the user is participant
	 * 			False - if the user is not participant
	 */
	public Boolean isJForumParticipant(String context, String userId);
	
	/**
	 * Check user active or inactive in the site
	 * 
	 * @param context		Context
	 * 
	 * @param sakaiUserId	user sakai id
	 * 
	 * @return	true - if user is active in the site
	 * 			false - if user is inactive in the site
	 */
	public Boolean isUserActive(String context, String sakaiUserId);
	
	/**
	 * Check user active or inactive in the site
	 * 
	 * @param context		Context
	 * 
	 * @param sakaiUserId	user sakai id
	 * 
	 * @param checkOtherUsers other users role will be verified in the same request
	 * 
	 * @return	true - if user is active in the site
	 * 			false - if user is inactive in the site
	 */
	public Boolean isUserActive(String context, String sakaiUserId, boolean checkOtherUsers);
	
	/**
	 * Check to see if user is blocked in the site
	 * 
	 * @param context	Context
	 * 
	 * @param sakaiUserId	user sakai id
	 * 
	 * @return	true - if user is blocked in the site
	 * 			false - if user is not blocked in the site
	 */
	public Boolean isUserBlocked(String context, String sakaiUserId);
	
	/**
	 * Check to see if user is blocked in the site
	 * 
	 * @param context	Context
	 * 
	 * @param sakaiUserId	user sakai id
	 * 
	 * @param checkOtherUsers other users role will be verified in the same request
	 * 
	 * @return	true - if user is blocked in the site
	 * 			false - if user is not blocked in the site
	 */
	public Boolean isUserBlocked(String context, String sakaiUserId, boolean checkOtherUsers);
	
	/**
	 * Check user role as facilitator
	 * 
	 * @param context		Context
	 * 
	 * @param sakaiUserId	User sakai id
	 * 
	 * @return	True - if user is facilitator in the site
	 * 			False - if user is not facilitator in the site
	 */
	public Boolean isUserFacilitator(String context, String sakaiUserId);
	
	/**
	 * Check user role as Guest
	 * 
	 * @param context	Context
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	True - if the user has Guest role
	 * 			False - if the user has no Guest role
	 */
	public Boolean isUserGuest(String context, String sakaiUserId);

	/**
	 * Is user Teaching Assistant 
	 * 
	 * @param context	Context
	 * 
	 * @param sakaiUserId	Sakai user id
	 * 
	 * @return	True - if the user is teaching assistant
	 * 			False - if the user is not teaching assistant
	 */
	public Boolean isUserTeachingAssistant(String context, String sakaiUserId);
}
