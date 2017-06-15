/*********************************************************************************** 
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/dao/CourseTimeDAO.java $ 
 * $Id: CourseTimeDAO.java 3638 2012-12-02 21:33:06Z ggolden $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010 Etudes, Inc. 
 * 
 * Portions completed before September 1, 2008 Copyright (c) 2004, 2005, 2006, 2007 Foothill College, ETUDES Project 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. You may 
 * obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License. 
 * 
 **********************************************************************************/
package org.etudes.jforum.dao;

import org.etudes.jforum.entities.CourseTimeObj;

public interface CourseTimeDAO
{

	public CourseTimeObj selectVisitTime(int userId) throws Exception;

	public void addNew(CourseTimeObj cto) throws Exception;

	public void update(CourseTimeObj cto) throws Exception;

	public CourseTimeObj selectMarkAllTime(int userId) throws Exception;

	public void addMarkAllNew(CourseTimeObj cto) throws Exception;

	public void updateMarkAllTime(CourseTimeObj cto) throws Exception;

}