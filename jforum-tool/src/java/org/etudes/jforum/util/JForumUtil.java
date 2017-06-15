/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/util/JForumUtil.java $ 
 * $Id: JForumUtil.java 3638 2012-12-02 21:33:06Z ggolden $ 
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
package org.etudes.jforum.util;

import org.sakaiproject.db.cover.SqlService;
import org.sakaiproject.user.api.User;

public class JForumUtil 
{
	// Private constructor
	private JForumUtil()
	{
	}

	public static boolean isAutoKeyGenerationSupported()
	{
		// The mysql driver supports auto-generated keys, while oracle does not.
		return SqlService.getVendor().startsWith("mysql");
	}

	public static void copyUser(User sakaiUser, org.etudes.jforum.entities.User jforumUser)
	{
		String fname = sakaiUser.getFirstName();
		if (fname == null || fname.length() == 0)
		{
			fname = " ";
		}
		jforumUser.setFirstName(fname);

		String lname = sakaiUser.getLastName();
		if (lname == null || lname.length() == 0)
		{
			lname = "Guest";
		}
		jforumUser.setLastName(lname);

		String email = sakaiUser.getEmail();
		if (email == null)
		{
			// revised by rashmi to show username as emailaddress in
			// case of guest account
			// email =
			// SystemGlobals.getValue(ConfigKeys.SSO_DEFAULT_EMAIL);
			email = sakaiUser.getEid();
		}
		jforumUser.setEmail(email);

		// 07/17/06 - Murthy
		jforumUser.setSakaiUserId(sakaiUser.getId());
		jforumUser.setUsername(sakaiUser.getEid());
	}
	
	/**
	 * This is from mneme
	 * Convert the points / score value into a double, without picking up float to double conversion junk
	 * 
	 * @param score
	 *        The value to convert.
	 * @return The converted value.
	 */
	static public double toDoubleScore(Float score)
	{
		if (score == null) return 0.0d;

		// we want only .xx precision, and we don't want any double junk from the float to double conversion
		float times100 = score.floatValue() * 100f;

		Double rv = (double) times100;

		rv = rv / 100d;
		return rv;
	}
}
