---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/webapp/WEB-INF/config/database/conversion/jforum_mysql_post_id_data_type_change.sql $ 
-- $Id: jforum_mysql_post_id_data_type_change.sql 3638 2012-12-02 21:33:06Z ggolden $ 
----------------------------------------------------------------------------------- 
-- 
-- Copyright (c) 2008, 2009 , 2010, 2011, 2012 Etudes, Inc. 
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
-- This is for MySQL post_id data type change to bigint 
--------------------------------------------------------------------------
--Note : Before running this script back up the updated tables

--jforum_posts (post_id)
ALTER TABLE jforum_posts MODIFY COLUMN post_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT;

--jforum_posts_text (post_id)
ALTER TABLE jforum_posts_text MODIFY COLUMN post_id BIGINT UNSIGNED NOT NULL;

--jforum_topics (topic_first_post_id, topic_last_post_id)
ALTER TABLE jforum_topics MODIFY COLUMN topic_first_post_id BIGINT UNSIGNED NULL DEFAULT '0', MODIFY COLUMN topic_last_post_id BIGINT UNSIGNED NOT NULL DEFAULT '0';

--jforum_forums (forum_last_post_id)
ALTER TABLE jforum_forums MODIFY COLUMN forum_last_post_id BIGINT UNSIGNED NOT NULL DEFAULT '0';

--jforum_attach (post_id)
ALTER TABLE jforum_attach MODIFY COLUMN post_id BIGINT UNSIGNED NULL DEFAULT NULL;

--jforum_search_wordmatch (post_id) - Database may have huge number of records and current data type is int. This can be ignored to avoid database performance.
--ALTER TABLE jforum_search_wordmatch MODIFY COLUMN post_id BIGINT UNSIGNED NOT NULL;

--jforum_search_topics(topic_first_post_id, topic_last_post_id)
ALTER TABLE jforum_search_topics MODIFY COLUMN topic_first_post_id BIGINT UNSIGNED NULL DEFAULT '0', MODIFY COLUMN topic_last_post_id BIGINT UNSIGNED NOT NULL DEFAULT '0';

--jforum_karma (post_id) - karma is not supported and not in use
ALTER TABLE jforum_karma MODIFY COLUMN post_id BIGINT UNSIGNED NOT NULL;

