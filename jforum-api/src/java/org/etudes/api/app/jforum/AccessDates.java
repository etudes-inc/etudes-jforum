/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/AccessDates.java $ 
 * $Id: AccessDates.java 3638 2012-12-02 21:33:06Z ggolden $ 
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

import java.util.Date;

public interface AccessDates
{
	/**
	 * @return the allowUntilDate
	 */
	Date getAllowUntilDate();
	
	/**
	 * Gets the due date
	 * 
	 * @return	The due date
	 */
	Date getDueDate();

	/**
	 * Gets the open date
	 * 
	 * @return 	The open date
	 */
	Date getOpenDate();
	
	/**
	 * check for valid dates
	 * 
	 * @return	true - if dates are valid
	 * 			false - if dates are invalid
	 */
	boolean isDatesValid();
	
	/**
	 * @return the hideUntilOpen
	 */
	Boolean isHideUntilOpen();

	/**
	 * Gets the locked status after due date
	 * 
	 * @return	true - if locked after the due date
	 * 			false - if not locked after the due date
	 */
	@Deprecated
	Boolean isLocked();
	
	/**
	 * @param allowUntilDate the allowUntilDate to set
	 */
	void setAllowUntilDate(Date allowUntilDate);
	
	/**
	 * Sets the due date
	 * 
	 * @param dueDate 	The dueDate to set
	 */
	void setDueDate(Date dueDate);

	/**
	 * @param hideUntilOpen the hideUntilOpen to set
	 */
	void setHideUntilOpen(Boolean hideUntilOpen);
	
	/**
	 * Sets the locked after due date
	 * 
	 * @param locked 	The locked to set
	 */
	@Deprecated
	void setLocked(Boolean locked);
	
	/**
	 * Sets the open date
	 * 
	 * @param openDate The openDate to set
	 */
	void setOpenDate(Date openDate);
}