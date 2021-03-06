---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/jforum/tags/2.27/jforum-tool/src/webapp/WEB-INF/config/database/conversion/JForum_2_2-2_3_oracle_conversion.sql $ 
-- $Id: JForum_2_2-2_3_oracle_conversion.sql 3638 2012-12-02 21:33:06Z ggolden $ 
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
-- This is the Oracle JForum 2.2 to JForum 2.3 conversion script
---------------------------------------------------------------
--for mark topics
---------------------------------------------------------------
--drop topic_views column
ALTER TABLE jforum_topics DROP COLUMN topic_views;

--update for mark topics
ALTER TABLE jforum_sakai_sessions ADD markall_time TIMESTAMP NULL;

--table for mark topics
--CREATE TABLE jforum_topics_mark (
--topic_id NUMBER(8) NOT NULL default 0,
--user_id NUMBER(8) NOT NULL default 0,
--mark_time TIMESTAMP NOT NULL default '0000-00-00 00:00:00',
--PRIMARY KEY  (topic_id,user_id),
--CONSTRAINT fk_jf_topics FOREIGN KEY (topic_id) REFERENCES jforum_topics(topic_id) ON DELETE CASCADE ON UPDATE CASCADE);


CREATE TABLE jforum_topics_mark (
topic_id NUMBER(8) default 0 NOT NULL,
user_id NUMBER(8) default 0 NOT NULL,
mark_time TIMESTAMP not null,
constraint pk_jforum_topics_mark primary key (topic_id,user_id),
constraint fk_jf_topics forEIGN KEY (topic_id) REFERENCES jforum_topics(topic_id) 
ON DELETE CASCADE);

--This does not exists in Oracle we might need to write a trigger for it.
--ON UPDATE CASCADE;
	        
	        
---------------------------------------------------------------
--add 'perm_create_task_topics' to jforum roles table for the 
--the existing Facilitator and Participant roles
---------------------------------------------------------------
--For Facilitators
CREATE TABLE jforum_roles_temp (
  group_id NUMBER(10) DEFAULT 0,
  user_id NUMBER(10) DEFAULT 0,
  name VARCHAR2(255) NOT NULL,
  role_type NUMBER(10) DEFAULT 1
);

INSERT into jforum_roles_temp(group_id, user_id, name, role_type) 
SELECT a.group_id, 0, 'perm_create_task_topics', 1 FROM jforum_groups a 
WHERE a.group_name = 'Facilitator' AND 
a.group_id not IN 
(select a.group_id FROM  jforum_roles a, jforum_groups b 
WHERE a.name = 'perm_create_task_topics' 
AND a.group_id = b.group_id);

---------------------------------------------------------------
--Need to grant trigger for Sakai schema
---------------------------------------------------------------
create or replace trigger trg_jforum_roles_01 before insert 
on jforum_roles
for each row
   --
   --
begin
   if :new.role_id is null then
      select JFORUM_ROLES_SEQ.nextval into :new.role_id from dual;
   end if;
end;

INSERT INTO jforum_roles(group_id, user_id, name, role_type) 
SELECT group_id, user_id, name, role_type from jforum_roles_temp;

DROP TABLE jforum_roles_temp;

--For Participant
CREATE TABLE jforum_roles_temp (
  group_id NUMBER(10) DEFAULT 0,
  user_id NUMBER(10) DEFAULT 0,
  name VARCHAR2(255) NOT NULL,
  role_type NUMBER(10) DEFAULT 1
);

INSERT INTO jforum_roles_temp(group_id, user_id, name, role_type) 
SELECT a.group_id, 0, 'perm_create_task_topics', 0 FROM jforum_groups a 
WHERE a.group_name = 'Participant' AND 
a.group_id NOT IN 
(SELECT a.group_id FROM  jforum_roles a, jforum_groups b 
WHERE a.name = 'perm_create_task_topics' 
AND a.group_id = b.group_id);

INSERT INTO jforum_roles(group_id, user_id, name, role_type) 
SELECT group_id, user_id, name, role_type FROM jforum_roles_temp;

DROP TABLE jforum_roles_temp;
--------------------------------
--Update topic type
--------------------------------
--update announce topic value from 2 to 3 and sticky topic value from 1 to 2
UPDATE jforum_topics SET topic_type = topic_type + 1 WHERE topic_type IN (1, 2);

