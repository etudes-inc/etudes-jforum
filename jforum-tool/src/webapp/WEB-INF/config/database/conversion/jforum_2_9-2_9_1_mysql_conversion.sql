---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/webapp/WEB-INF/config/database/conversion/jforum_2_9-2_9_1_mysql_conversion.sql $ 
-- $Id: jforum_2_9-2_9_1_mysql_conversion.sql 4098 2012-12-29 00:30:12Z murthyt $ 
----------------------------------------------------------------------------------- 
-- 
-- Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013 Etudes, Inc. 
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
-- This is for MySQL, JForum 2.9 to JForum 2.9.1
--------------------------------------------------------------------------
-- Note : Before running this script back up the updated tables
--
-- add allow_until_date to jforum_search_topics
ALTER TABLE jforum_search_topics ADD COLUMN allow_until_date DATETIME NULL DEFAULT NULL  AFTER search_time, ADD COLUMN hide_until_open TINYINT(1) NULL DEFAULT 0  AFTER allow_until_date ;