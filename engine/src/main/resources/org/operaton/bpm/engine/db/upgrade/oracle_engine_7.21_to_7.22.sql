--
-- Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
-- under one or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information regarding copyright
-- ownership. Camunda licenses this file to you under the Apache License,
-- Version 2.0; you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

insert into ACT_GE_SCHEMA_LOG
values ('1100', CURRENT_TIMESTAMP, '7.22.0');

alter table ACT_RU_TASK add TASK_STATE_ NVARCHAR2(64);

alter table ACT_HI_TASKINST add TASK_STATE_ NVARCHAR2(64);

alter table ACT_RU_JOB add BATCH_ID_ NVARCHAR2(64);
alter table ACT_HI_JOB_LOG add BATCH_ID_ NVARCHAR2(64);

alter table ACT_HI_PROCINST add RESTARTED_PROC_INST_ID_ NVARCHAR2(64);
create index ACT_IDX_HI_PRO_RST_PRO_INST_ID on ACT_HI_PROCINST(RESTARTED_PROC_INST_ID_);
