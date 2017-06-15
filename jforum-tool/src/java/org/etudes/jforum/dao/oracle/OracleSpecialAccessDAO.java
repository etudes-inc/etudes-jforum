/********************************************************************************** 
 * $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/java/org/etudes/jforum/dao/oracle/OracleSpecialAccessDAO.java $ 
 * $Id: OracleSpecialAccessDAO.java 3638 2012-12-02 21:33:06Z ggolden $ 
 *********************************************************************************** 
 * 
 * Copyright (c) 2008, 2009, 2010 Etudes, Inc. 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **********************************************************************************/
package org.etudes.jforum.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.etudes.jforum.JForum;
import org.etudes.jforum.entities.SpecialAccess;
import org.etudes.jforum.util.preferences.SystemGlobals;


public class OracleSpecialAccessDAO extends org.etudes.jforum.dao.generic.GenericSpecialAccessDAO
{
	@Override
	public int addNew(SpecialAccess specialAccess) throws Exception
	{
		PreparedStatement p = this.getStatementForAutoKeys("SpecialAccessModel.addNew");

		p.setInt(1, specialAccess.getForumId());
		p.setInt(2, specialAccess.getTopicId());
		
		if (specialAccess.getStartDate() == null)
		{
		  p.setTimestamp(3, null);
		}
		else
		{
		  p.setTimestamp(3, new Timestamp(specialAccess.getStartDate().getTime()));
		}
		
		if (specialAccess.getEndDate() == null)
		{
		  p.setTimestamp(4, null);
		}
		else
		{
		  p.setTimestamp(4, new Timestamp(specialAccess.getEndDate().getTime()));		  
		}		
		p.setInt(5, specialAccess.isOverrideStartDate() ? 1 : 0);
		p.setInt(6, specialAccess.isOverrideEndDate() ? 1 : 0);
		p.setInt(7, specialAccess.isLockOnEndDate() ? 1 : 0);
		
		this.setAutoGeneratedKeysQuery(SystemGlobals.getSql("SpecialAccessModel.lastGeneratedSpecilaAccessId"));

		int specialAccessId = this.executeAutoKeysQuery(p);

		p.close();
		
		// save special access users
		OracleUtils.writeClobUTF16BinaryStream(SystemGlobals.getSql("SpecialAccessModel.addUsers"), specialAccessId, 
													getUserIdString(specialAccess.getUserIds()));

		return specialAccessId;
	
	}
	
	@Override
	public void update(SpecialAccess specialAccess) throws Exception
	{
		PreparedStatement p = JForum.getConnection().prepareStatement(SystemGlobals.getSql("SpecialAccessModel.update"));
		
		p.setInt(1, specialAccess.getForumId());
		p.setInt(2, specialAccess.getTopicId());
		
		if (specialAccess.getStartDate() == null)
		{
			p.setTimestamp(3, null);
		}
		else
		{
			p.setTimestamp(3, new Timestamp(specialAccess.getStartDate().getTime()));
		}
		
		if (specialAccess.getEndDate() == null)
		{
			p.setTimestamp(4, null);
		}
		else
		{
			p.setTimestamp(4, new Timestamp(specialAccess.getEndDate().getTime()));		  
		}
		p.setInt(5, specialAccess.isLockOnEndDate() ? 1 : 0);
		p.setInt(6, specialAccess.isOverrideStartDate() ? 1 : 0);
		p.setInt(7, specialAccess.isOverrideEndDate() ? 1 : 0);
		p.setInt(8, specialAccess.getId());

		p.executeUpdate();
		
		p.close();
		
		// save special access users
		OracleUtils.writeClobUTF16BinaryStream(SystemGlobals.getSql("SpecialAccessModel.addUsers"), specialAccess.getId(), 
													getUserIdString(specialAccess.getUserIds()));
	}
	
	@Override
	protected String getSpecialAccessUsersFromResultSet(ResultSet rs) throws Exception
	{
		return OracleUtils.readClobUTF16BinaryStream(rs, "users");
	}

}