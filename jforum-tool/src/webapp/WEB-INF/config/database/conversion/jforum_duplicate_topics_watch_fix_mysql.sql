---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/webapp/WEB-INF/config/database/conversion/jforum_duplicate_topics_watch_fix_mysql.sql $ 
-- $Id: jforum_duplicate_topics_watch_fix_mysql.sql 7558 2014-03-07 21:37:07Z murthyt $ 
----------------------------------------------------------------------------------- 
-- 
-- Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Etudes, Inc. 
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
-- This is for MySQL for jforum_topics_watch table
--------------------------------------------------------------------------
-- Note : Before running this script back up the updated tables

---------------------------------------
-- repeatedly execute the below two  queries in the order until there are no records to delete
---------------------------------------
-- drop duplicates in jforum_topics_watch table if any
SELECT j.* FROM jforum_topics_watch j, (SELECT MAX(id) AS id FROM jforum_topics_watch GROUP BY  topic_id, user_id HAVING COUNT(*) > 1) AS t1 WHERE j.id = t1.id;

DELETE j.* FROM jforum_topics_watch j, (SELECT MAX(id) AS id FROM jforum_topics_watch GROUP BY  topic_id, user_id HAVING COUNT(*) > 1) AS t1 WHERE j.id = t1.id;


---------------------------------------
-- after done with the above queries
---------------------------------------
-- add unique index on jforum_topics_watch table on the columns topic_id, user_id, forum_id
ALTER TABLE jforum_topics_watch ADD UNIQUE idx_row_unique(topic_id, user_id, forum_id);
