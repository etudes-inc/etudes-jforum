/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/entities/AttachmentExtension.java $ 
 * $Id: AttachmentExtension.java 3638 2012-12-02 21:33:06Z ggolden $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008 Etudes, Inc. 
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
 * 
 * Portions completed before July 1, 2004 Copyright (c) 2003, 2004 Rafael Steil, All rights reserved, licensed under the BSD license. 
 * http://www.opensource.org/licenses/bsd-license.php 
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met: 
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following disclaimer. 
 * 2) Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution. 
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE 
 ***********************************************************************************/
package org.etudes.jforum.entities;

/**
 * @author Rafael Steil
 */
public class AttachmentExtension
{	
	private int id;
	private int extensionGroupId;
	private boolean allow;
	private String comment;
	private String extension;
	private String uploadIcon;
	private boolean unknown;
	
	/**
	 * @return Returns the allow.
	 */
	public boolean isAllow()
	{
		return this.allow;
	}
	
	/**
	 * @param allow The allow to set.
	 */
	public void setAllow(boolean allow)
	{
		this.allow = allow;
	}
	
	/**
	 * @return Returns the comment.
	 */
	public String getComment()
	{
		return this.comment;
	}
	
	/**
	 * @param comment The comment to set.
	 */
	public void setComment(String comment)
	{
		this.comment = comment;
	}
	
	/**
	 * @return Returns the extension.
	 */
	public String getExtension()
	{
		return this.extension;
	}
	
	/**
	 * @param extension The extension to set.
	 */
	public void setExtension(String extension)
	{
		this.extension = extension;
	}
	
	/**
	 * @return Returns the extensionGroupId.
	 */
	public int getExtensionGroupId()
	{
		return this.extensionGroupId;
	}
	
	/**
	 * @param extensionGroupId The extensionGroupId to set.
	 */
	public void setExtensionGroupId(int extensionGroupId)
	{
		this.extensionGroupId = extensionGroupId;
	}
	
	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return this.id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(int id)
	{
		this.id = id;
	}
	
	/**
	 * @return Returns the upload_icon.
	 */
	public String getUploadIcon()
	{
		return this.uploadIcon;
	}
	
	/**
	 * @param uploadIcon The upload_icon to set.
	 */
	public void setUploadIcon(String uploadIcon)
	{
		this.uploadIcon = uploadIcon;
	}
	
	/**
	 * @return Returns the unknown.
	 */
	public boolean isUnknown()
	{
		return this.unknown;
	}
	
	/**
	 * @param unknown The unknown to set.
	 */
	public void setUnknown(boolean unknown)
	{
		this.unknown = unknown;
	}
}
