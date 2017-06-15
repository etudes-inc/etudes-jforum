-- -------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/webapp/WEB-INF/config/database/conversion/jforum_2_9_18-2_9_19_sakai_mysql_conversion.sql $ 
-- $Id: jforum_2_9_18-2_9_19_sakai_mysql_conversion.sql 5984 2013-09-19 21:52:21Z murthyt $ 
-- --------------------------------------------------------------------------------- 
-- 
-- Copyright (c) 2013 Etudes, Inc. 
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
-- --------------------------------------------------------------------------------
-- ------------------------------------------------------------------------
-- This is for MySQL, JForum 2.9.18 to JForum 2.9.19
-- ------------------------------------------------------------------------
-- Note : Before running this script back up the updated tables

-- add column privmsgs_bcc_userids to jforum_privmsgs table for bcc users
ALTER TABLE jforum_privmsgs ADD COLUMN privmsgs_bcc_userids LONGTEXT NULL DEFAULT NULL  AFTER privmsgs_draft_to_userids;



