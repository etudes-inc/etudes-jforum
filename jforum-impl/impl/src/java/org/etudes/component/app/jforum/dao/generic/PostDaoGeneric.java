/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-impl/impl/src/java/org/etudes/component/app/jforum/dao/generic/PostDaoGeneric.java $ 
 * $Id: PostDaoGeneric.java 7799 2014-04-14 23:41:28Z murthyt $ 
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
package org.etudes.component.app.jforum.dao.generic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.jforum.Post;
import org.etudes.api.app.jforum.dao.PostDao;
import org.etudes.component.app.jforum.PostImpl;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;

public abstract class PostDaoGeneric implements PostDao
{
	private static Log logger = LogFactory.getLog(PostDaoGeneric.class);
	
	/** Dependency: SqlService */
	protected SqlService sqlService = null;
	
	/**
	 * {@inheritDoc}
	 */
	public int addNew(Post post)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Start : addNew");
		}
		if (post == null)
		{
			throw new IllegalArgumentException("Post information is required.");
		}
		
		if (post.getId() > 0 || post.getTopicId() <= 0 || post.getForumId() <= 0)
		{
			throw new IllegalArgumentException("New post cannot be created as post may be having id or has no topic or forum id");
		}
		
		// save post
		int postId = insertPost(post);
		((PostImpl)post).setId(postId);
		
		if (post.getSubject() != null && post.getSubject().length() > 100)
		{
			post.setSubject(post.getSubject().substring(0, 99));
		}
		
		//save post text
		insertPostText(post);
		
		if (logger.isDebugEnabled())
		{
			logger.debug("End : addNew");
		}
		
		return postId;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean addUserPostLike(final int postId, final int userId)
	{
		final List<Boolean> addedList = new ArrayList<Boolean>();
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					// insert if no record is existing
					if (!isUserLikedPost(postId, userId))
					{				
						// add post user like and update posts's user likes
						String sql = "INSERT INTO jforum_post_user_likes (post_id, user_id) VALUES (?, ?)";
						
						Object[] fields = new Object[2];
						
						int i = 0;
						
						fields[i++] = postId;
						fields[i++] = userId;
								
						if (!sqlService.dbWrite(sql.toString(), fields)) 
						{
							throw new RuntimeException("addUserPostLike: db write failed");
						}
						
						// add to post likes count
						incrementPostlikes(postId);
						
						addedList.add(Boolean.TRUE);
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while adding user to like the post.", e);
				}
			}
		}, "addUserPostLike: postId id: " + postId +" user id: "+ userId);
		
		boolean success = false;
		
		if (addedList.size() > 0)
		{
			 success = true;
		}
		
		return success;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void delete(int postId)
	{
		deletePostText(postId);
		removeAllPostLikes(postId);
		deletePost(postId);		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean deleteUserPostLike(final int postId, final int userId)
	{
		final List<Boolean> removedList = new ArrayList<Boolean>();
		
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					// remove if record is existing
					if (isUserLikedPost(postId, userId))
					{				
						// remove user post like and update posts's likes count
						String sql = "DELETE FROM jforum_post_user_likes WHERE post_id = ? AND user_id = ?";
						
						Object[] fields = new Object[2];
						
						int i = 0;
						
						fields[i++] = postId;
						fields[i++] = userId;
								
						if (!sqlService.dbWrite(sql.toString(), fields)) 
						{
							throw new RuntimeException("deleteUserPostLike: db write failed");
						}
						
						// reduce post likes count
						decrementPostlikes(postId);
						
						removedList.add(Boolean.TRUE);
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while removing user like of the post.", e);
				}
			}
		}, "deleteUserPostLike: postId id: " + postId +" user id: "+ userId);
		
		boolean success = false;
		
		if (removedList.size() > 0)
		{
			 success = true;
		}
		
		return success;
	}
	
	public boolean isUserLikedPost(int postId, int userId)
	{
		if (postId <= 0 || userId <= 0)
		{
			throw new IllegalArgumentException("post or user information is missing");
		}
		
		String sql = "SELECT user_id FROM jforum_post_user_likes WHERE post_id = ? AND user_id = ?";
				
		Object[] fields;
		int i = 0;
		
		fields = new Object[2];
		fields[i++] = postId;
		fields[i++] = userId;
		
		final List<Integer> userLikedPostList = new ArrayList<Integer>();
		
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					userLikedPostList.add(result.getInt("user_id"));

					return null;
				}
				catch (SQLException e)
				{
					if (logger.isWarnEnabled())
					{
						logger.warn("isUserLikedPost: " + e, e);
					}
					return null;
				}
			}
		});
		
		boolean liked = false;
		if (userLikedPostList.size() > 0)
		{
			liked = true;
		}
		return liked;
	}
	
	/**
	 * @param sqlService
	 *          The sqlService to set
	 */
	public void setSqlService(SqlService sqlService)
	{
		this.sqlService = sqlService;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void update(Post post)
	{
		if (post == null)
		{
			throw new IllegalArgumentException("Post information is required.");
		}
		
		if (post.getId() <= 0 || post.getTopicId() <= 0 || post.getForumId() <= 0)
		{
			throw new IllegalArgumentException("Post cannot be updated as post has no post or topic or forum id");
		}
		
		// update post
		updatePost(post);
		
		// update post text
		updatePostText(post);
	}
	
	/**
	 * Decrease post likes count
	 * 
	 * @param postId	Post id
	 */
	protected void decrementPostlikes(int postId)
	{
		StringBuilder sql = new StringBuilder();
		
		sql.append("UPDATE jforum_posts SET user_likes_count = user_likes_count - 1 WHERE post_id = ?");
		
		Object[] fields = new Object[1];
		int i = 0;

		fields[i++] = postId;
		
		if (!sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("decrementPostlikes: db write failed");
		}
	}
	
	/**
	 * Deletes post
	 * 
	 * @param postId	Post id
	 */
	protected void deletePost(int postId)
	{
		String sql = "DELETE FROM jforum_posts WHERE post_id = ?";
		
		Object[] fields = new Object[1];
		
		int i = 0;
		
		fields[i++] = postId;
				
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("deletePostText: db write failed");
		}		
	}
	
	/**
	 * Deletes post text
	 * 
	 * @param postId	post id
	 */
	protected void deletePostText(int postId)
	{
		String sql = "DELETE FROM jforum_posts_text WHERE post_id = ?";
		
		Object[] fields = new Object[1];
		
		int i = 0;
		
		fields[i++] = postId;
				
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("deletePostText: db write failed");
		}		
	}
	
	/**
	 * increase post likes count
	 * 
	 * @param postId	Post id
	 */
	protected void incrementPostlikes(int postId)
	{
		StringBuilder sql = new StringBuilder();
		
		sql.append("UPDATE jforum_posts SET user_likes_count = user_likes_count + 1 WHERE post_id = ?");
		
		Object[] fields = new Object[1];
		int i = 0;

		fields[i++] = postId;
		
		if (!sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("incrementPostlikes: db write failed");
		}
	}
	
	/**
	 * save new post
	 * 
	 * @param post	post 
	 * 
	 * @return	New post id
	 */
	protected abstract int insertPost(Post post);
	
	/**
	 * saves post text
	 *
	 * @param post	post
	 */
	protected abstract void insertPostText(Post post);
	
	/**
	 * Removes all post likes
	 * 
	 * @param postId	Post id
	 */
	protected void removeAllPostLikes(final int postId)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				try
				{
					// remove all post likes
					String sql = "DELETE FROM jforum_post_user_likes WHERE post_id = ?";
					
					Object[] fields = new Object[1];
					
					int i = 0;
					
					fields[i++] = postId;
							
					if (!sqlService.dbWrite(sql.toString(), fields)) 
					{
						throw new RuntimeException("removeAllPostLikes: db write failed");
					}
					
					// set post count to zero
					sql = "UPDATE jforum_posts SET user_likes_count = 0 WHERE post_id = ?";
					
					fields = new Object[1];
					i = 0;
			
					fields[i++] = postId;
					
					if (!sqlService.dbWrite(null, sql.toString(), fields))
					{
						throw new RuntimeException("removeAllPostLikes - set post count to zero: db write failed");
					}
				}
				catch (Exception e)
				{
					if (logger.isErrorEnabled())
					{
						logger.error(e.toString(), e);
					}
					
					throw new RuntimeException("Error while adding user to like the post.", e);
				}
			}
		}, "removeAllPostLikes: postId id: " + postId);
	}
	
	/**
	 * updates post
	 * 
	 * @param post	Post
	 */
	protected void updatePost(Post post)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE jforum_posts SET topic_id = ?, forum_id = ?, enable_bbcode = ?, enable_html = ?, enable_smilies = ?, enable_sig = ?, ");
		sql.append("post_edit_time = ?, post_edit_count = post_edit_count + 1, poster_ip = ? WHERE post_id = ?");
		
		Object[] fields = new Object[9];
		
		int i = 0;
		Date now = new Date();
		
		fields[i++] = post.getTopicId();
		fields[i++] = post.getForumId();
		fields[i++] = (post.isBbCodeEnabled() ? 1 : 0);
		fields[i++] = (post.isHtmlEnabled() ? 1 : 0);
		fields[i++] = (post.isSmiliesEnabled() ? 1 : 0);
		fields[i++] = (post.isSignatureEnabled() ? 1 : 0);
		fields[i++] = new Timestamp(now.getTime());
		fields[i++] = post.getUserIp();
		fields[i++] = post.getId();
				
		if (!sqlService.dbWrite(sql.toString(), fields)) 
		{
			throw new RuntimeException("updatePost: db write failed");
		}		
	}
	
	/**
	 * updates post text
	 * 
	 * @param post	Post object
	 */
	abstract protected void updatePostText(Post post);
}
