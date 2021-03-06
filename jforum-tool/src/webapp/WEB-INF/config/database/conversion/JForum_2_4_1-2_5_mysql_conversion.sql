---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/webapp/WEB-INF/config/database/conversion/JForum_2_4_1-2_5_mysql_conversion.sql $ 
-- $Id: JForum_2_4_1-2_5_mysql_conversion.sql 3638 2012-12-02 21:33:06Z ggolden $ 
----------------------------------------------------------------------------------- 
-- 
-- Copyright (c) 2008 Etudes, Inc. 
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
-- 
-- Portions completed before July 1, 2004 Copyright (c) 2003, 2004 Rafael Steil, All rights reserved, licensed under the BSD license. 
-- http://www.opensource.org/licenses/bsd-license.php 
-- 
-- Redistribution and use in source and binary forms, 
-- with or without modification, are permitted provided 
-- that the following conditions are met: 
-- 
-- 1) Redistributions of source code must retain the above 
-- copyright notice, this list of conditions and the 
-- following disclaimer. 
-- 2) Redistributions in binary form must reproduce the 
-- above copyright notice, this list of conditions and 
-- the following disclaimer in the documentation and/or 
-- other materials provided with the distribution. 
-- 3) Neither the name of "Rafael Steil" nor 
-- the names of its contributors may be used to endorse 
-- or promote products derived from this software without 
-- specific prior written permission. 
-- 
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
-- HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
-- EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
-- BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
-- MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
-- PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
-- THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
-- FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
-- EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
-- (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
-- SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
-- OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
-- CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
-- IN CONTRACT, STRICT LIABILITY, OR TORT 
-- (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
-- ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
-- ADVISED OF THE POSSIBILITY OF SUCH DAMAGE 
----------------------------------------------------------------------------------
-- This is the MySQL JForum 2.4.1 to JForum 2.5 conversion script
-----------------------------------------------------------------------------------------------------
--For private message attachments, grading and change in task topics to reuse topics
--Back up jforum_privmsgs, jforum_forums, jforum_topics, jforum_search_topics, jforum_categories, 
--jforum_attach_desc tables before running this script
-----------------------------------------------------------------------------------------------------
--add column to the table jforum_privmsgs for private attachments
ALTER TABLE jforum_privmsgs ADD COLUMN privmsgs_attachments TINYINT(1) NOT NULL DEFAULT 0 AFTER privmsgs_attach_sig;

--Table for private message attachments
CREATE TABLE jforum_privmsgs_attach (
  attach_id INTEGER NOT NULL DEFAULT 0,
  privmsgs_id INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY(attach_id, privmsgs_id)
)TYPE=InnoDB;

--add column to the table jforum_forums forum_grade_type
ALTER TABLE jforum_forums ADD COLUMN forum_grade_type SMALLINT UNSIGNED NOT NULL DEFAULT 0 AFTER forum_access_type;

--Table for grading
CREATE TABLE jforum_grade (
  grade_id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
  context VARCHAR(99) NOT NULL DEFAULT '',
  grade_type SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  forum_id MEDIUMINT UNSIGNED NOT NULL DEFAULT 0,
  topic_id MEDIUMINT NOT NULL DEFAULT 0,
  points FLOAT NOT NULL DEFAULT 0,
  add_to_gradebook TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY(grade_id)
)TYPE=InnoDB;

--create indexes for jforum_grade table columns
CREATE INDEX idx_jforum_grade_forum_id_topic_id ON jforum_grade(forum_id ASC, topic_id ASC);

--add column to the table jforum_topics for grading
ALTER TABLE jforum_topics ADD COLUMN topic_grade TINYINT(1) NOT NULL DEFAULT 0 AFTER moderated;
ALTER TABLE jforum_search_topics ADD COLUMN topic_grade TINYINT(1) NOT NULL DEFAULT 0 AFTER moderated;

--Table for evaluations
CREATE TABLE jforum_evaluations (
  evaluation_id MEDIUMINT UNSIGNED NOT NULL auto_increment,
  grade_id MEDIUMINT UNSIGNED NOT NULL DEFAULT 0,
  user_id MEDIUMINT NOT NULL DEFAULT 0,
  sakai_user_id varchar(99) NOT NULL,
  score FLOAT DEFAULT NULL,
  comments TEXT DEFAULT NULL,
  evaluated_by MEDIUMINT DEFAULT NULL,
  evaluated_date DATETIME DEFAULT NULL,
  PRIMARY KEY  (evaluation_id)
) TYPE=InnoDB;

CREATE INDEX idx_jforum_evaluations_grade_id ON jforum_evaluations(grade_id);
CREATE INDEX idx_jforum_evaluations_user_id ON jforum_evaluations(user_id);

--add archived column to jforum_categories
ALTER TABLE jforum_categories ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0 AFTER moderated;

--add topic_export to jforum_topics
ALTER TABLE jforum_topics ADD COLUMN topic_export TINYINT(1) DEFAULT 0 AFTER topic_grade;
ALTER TABLE jforum_search_topics ADD COLUMN topic_export TINYINT(1) DEFAULT 0 AFTER topic_grade;

--increase size for mimetype column to support Office 2007 attachments
ALTER TABLE jforum_attach_desc MODIFY COLUMN mimetype VARCHAR(100) DEFAULT NULL;

--back up jforum_topics before running this statement
--modify task topic type to reuse/export topic
--Run the below statement to update task topic type to reuse/export topic
--UPDATE jforum_topics SET topic_type = 0, topic_export = 1 WHERE topic_type = 1;

