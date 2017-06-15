---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/webapp/WEB-INF/config/database/conversion/jforum_duplicate_users_fix_mysql.sql $ 
-- $Id: jforum_duplicate_users_fix_mysql.sql 3638 2012-12-02 21:33:06Z ggolden $ 
----------------------------------------------------------------------------------- 
-- 
-- Copyright (c) 2008, 2009, 2010, 2011 Etudes, Inc. 
-- 
-- Licensed under the Apache License, Version 2.0 (the "License"); 
-- you may not use this file except in compliance with the License. 
-- You may obtain a copy of the License at 
-- 
-- http://www.apache.org/licenses/LICENSE-2.0 
-- 
-- Unless required by applicable law or agreed to in writing, software 
-- distributed under the License is distributed on an "AS IS" BASIS, 
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
-- See the License for the specific language governing permissions and 
-- limitations under the License. 
----------------------------------------------------------------------------------
--------------------------------------------------------------------------
-- This is for MySQL for sakai_users table
--------------------------------------------------------------------------
--Note : Before running this script back up the updated tables

---------------------------------------
--repeatedly execute the below four queries in the order until there are no records to delete
---------------------------------------
--jforum_site_users table
SELECT j.sakai_site_id, j.user_id FROM jforum_site_users j, (SELECT MAX(user_id) AS user_id FROM jforum_users WHERE sakai_user_id != "" GROUP BY sakai_user_id HAVING COUNT(sakai_user_id) > 1 ORDER BY user_id) AS t1 WHERE j.user_id = t1.user_id;

DELETE j.* FROM jforum_site_users j, (SELECT MAX(user_id) AS user_id FROM jforum_users WHERE sakai_user_id != "" GROUP BY sakai_user_id HAVING COUNT(sakai_user_id) > 1 ORDER BY user_id) AS t1 WHERE j.user_id = t1.user_id;

--jforum_users table
SELECT j.* FROM jforum_users j, (SELECT MAX(user_id) AS user_id FROM jforum_users WHERE sakai_user_id != "" GROUP BY sakai_user_id HAVING COUNT(sakai_user_id) > 1 ORDER BY user_id) AS t1 WHERE j.user_id = t1.user_id;

DELETE j.* FROM jforum_users j, (SELECT MAX(user_id) AS user_id FROM jforum_users WHERE sakai_user_id != "" GROUP BY sakai_user_id HAVING COUNT(sakai_user_id) > 1 ORDER BY user_id) AS t1 WHERE j.user_id = t1.user_id;

---------------------------------------
--after done with the above queries
---------------------------------------
--drop existing index and add unique index on jforum_users table for the column sakai_user_id
ALTER TABLE jforum_users DROP INDEX sakai_user_id_idx;
ALTER TABLE jforum_users ADD UNIQUE INDEX sakai_user_id_idx(sakai_user_id);

