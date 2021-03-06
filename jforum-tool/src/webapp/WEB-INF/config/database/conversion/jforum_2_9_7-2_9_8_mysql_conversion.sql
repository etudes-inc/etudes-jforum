---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/webapp/WEB-INF/config/database/conversion/jforum_2_9_7-2_9_8_mysql_conversion.sql $ 
-- $Id: jforum_2_9_7-2_9_8_mysql_conversion.sql 4453 2013-03-07 22:20:31Z murthyt $ 
----------------------------------------------------------------------------------- 
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
----------------------------------------------------------------------------------
--------------------------------------------------------------------------
-- This is for MySQL, JForum 2.9.7 to JForum 2.9.8
--------------------------------------------------------------------------
-- Note : Before running this script back up the updated tables
--
-- create table jforum_schedule_grades_gradebook
CREATE  TABLE jforum_schedule_grades_gradebook (
  grade_id MEDIUMINT UNSIGNED NOT NULL ,  
  open_date DATETIME NOT NULL ,
  PRIMARY KEY (grade_id)
);