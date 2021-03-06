/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-api/src/java/org/etudes/api/app/jforum/JForumAttachmentOverQuotaException.java $ 
 * $Id: JForumAttachmentOverQuotaException.java 3638 2012-12-02 21:33:06Z ggolden $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010, 2011, 2012 Etudes, Inc. 
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

public class JForumAttachmentOverQuotaException extends Exception
{
	private static final long serialVersionUID = -3778513390576307244L;
	
	public JForumAttachmentOverQuotaException(int quotaLimit, int actualTotalSize)
	{
		super("The total size of the attachments should not be greater than " + quotaLimit +"kb. The uploaded files have a total of "+ actualTotalSize +"kb.");
	}
}
