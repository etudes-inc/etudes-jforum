/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/dao/PostDao.java $ 
 * $Id: PostDao.java 7799 2014-04-14 23:41:28Z murthyt $ 
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
package org.etudes.api.app.jforum.dao;

import org.etudes.api.app.jforum.Post;

public interface PostDao
{
	/**
	 * Creates post
	 * 
	 * @param topic	Post object
	 * 
	 * @return	The newly created post id
	 */
	public int addNew(Post post);
	
	/**
	 * Adds user post like if it's not already added
	 * 
	 * @param postId	Post id
	 * 
	 * @param userId	User id
	 * 
	 * @return 	true - if user post like is added
	 * 			false - if user post like is not added
	 */
	public boolean addUserPostLike(int postId, int userId);
	
	/**
	 * Deletes the post and it's attachments. If it's first post the topic is also gets deleted
	 * 
	 * @param postId	Post id
	 */
	public void delete(int postId);
	
	
	/**
	 * Delete user post like if existing
	 * 
	 * @param postId	Post id
	 * 
	 * @param userId	User id
	 *
	 * @return 	true - if user post like is removed
	 * 			false - if user post like is not removed or not existing
	 */
	public boolean deleteUserPostLike(int postId, int userId);
	
	/**
	 * Checks to see if user is liked the post
	 * 
	 * @param postId	Post id
	 * 
	 * @param userId	User id
	 * 
	 * @return	true - if user liked the post
	 * 			false - if user is not liked the post yet
	 */
	public boolean isUserLikedPost(int postId, int userId);
	
	/**
	 * Updates the post
	 * 
	 * @param post	Post object
	 */
	public void update(Post post);
}
