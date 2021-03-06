/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/dao/generic/CoursePrivateMessageDAO.java $ 
 * $Id: CoursePrivateMessageDAO.java 3638 2012-12-02 21:33:06Z ggolden $ 
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
package org.etudes.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


import org.etudes.jforum.JForum;
import org.etudes.jforum.util.preferences.SystemGlobals;
import org.sakaiproject.tool.cover.ToolManager;
//Mallika's import end
/**
 * @author Mallika M Thoppay
 * 8/17/05 - Mallika - file created
*/
public class CoursePrivateMessageDAO 
{
	/**
	 * @see 
	 */
	public List selectAll() throws Exception 
	{
		PreparedStatement p = JForum.getConnection().prepareStatement(SystemGlobals.getSql("CoursePrivateMessageModel.selectAll"));
		p.setString(1,ToolManager.getCurrentPlacement().getContext());
		
		List l = new ArrayList();
		ResultSet rs = p.executeQuery();
		
		CoursePrivateMessage cpm = new CoursePrivateMessage();
		while (rs.next()) {
			cpm = this.getCoursePrivateMessage(rs);
			l.add(cpm);
		}
		
		rs.close();
		p.close();
		
		return l;
	}
	
	protected CoursePrivateMessage getCoursePrivateMessage(ResultSet rs) throws Exception
	{
		CoursePrivateMessage cpm = new CoursePrivateMessage();
		
		cpm.setCourseId(rs.getString("course_id"));
		cpm.setPrivmsgsId(rs.getInt("privmsgs_id"));
		
		return cpm;
	}
	
	public void addNew(CoursePrivateMessage cpm) throws Exception 
	{
		
		PreparedStatement p = JForum.getConnection().prepareStatement(SystemGlobals.getSql("CoursePrivateMessageModel.addNew"));
		
		p.setString(1, cpm.getCourseId());
		p.setInt(2, cpm.getPrivmsgsId());
		
		p.execute();
		
		p.close();
	
	}	
	/**
	 * @see 
	 */
	public void delete(int privmsgsId) throws Exception 
	{
		//System.out.println("Course Group delete working");
		PreparedStatement p = JForum.getConnection().prepareStatement(SystemGlobals.getSql("CoursePrivateMessageModel.delete"));
		p.setInt(1, privmsgsId);
		p.executeUpdate();
		
		p.close();
	}
	
}
