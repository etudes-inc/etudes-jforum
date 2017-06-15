/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/trunk/jforum-tool/src/java/org/etudes/jforum/AudioServlet.java $ 
 * $Id: AudioServlet.java 3638 2012-12-02 21:33:06Z ggolden $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2015 Etudes, Inc. 
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
package org.etudes.jforum;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.jforum.util.preferences.ConfigKeys;
import org.etudes.jforum.util.preferences.SakaiSystemGlobals;
import org.sakaiproject.tool.cover.ToolManager;

public class AudioServlet extends HttpServlet
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(AudioServlet.class);

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException	{
		FileInputStream fis = null;
		OutputStream os = null;
		
		String[] urlModel = request.getRequestURI().split("/");
		
		String filename = SakaiSystemGlobals.getValue(ConfigKeys.ATTACHMENTS_STORE_DIR) + "/" + ToolManager.getCurrentPlacement().getContext() + "/"
				+ urlModel[urlModel.length - 1];
		;
		
		try
		{
			if (!new File(filename).exists())
			{
				M_log.warn("doGet AudioServlet " + filename + " does not exist.");
				return;
			}

			fis = new FileInputStream(filename);
			os = response.getOutputStream();
			
			response.setContentType("audio/x-wav");

			response.setContentLength((int) fis.getChannel().size());

			byte[] b = new byte[102400];
			int c = 0;
			while ((c = fis.read(b)) != -1)
			{
				os.write(b, 0, c);
			}
		}
		catch (IOException e)
		{
		
		}
		finally
		{
			try
			{
				if (os != null) os.close();
				fis.close();
			}
			catch (IOException e)
			{
			}
		}
		
	}

}
