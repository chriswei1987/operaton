/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.cfg;


import static java.nio.charset.StandardCharsets.UTF_8;
import static org.operaton.bpm.engine.impl.cmd.HistoryCleanupCmd.MAX_THREADS_NUMBER;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.operaton.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;
import org.operaton.bpm.engine.ArtifactFactory;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.impl.AuthorizationServiceImpl;
import org.operaton.bpm.engine.impl.DecisionServiceImpl;
import org.operaton.bpm.engine.impl.DefaultArtifactFactory;
import org.operaton.bpm.engine.impl.DefaultPriorityProvider;
import org.operaton.bpm.engine.impl.ExternalTaskServiceImpl;
import org.operaton.bpm.engine.impl.FilterServiceImpl;
import org.operaton.bpm.engine.impl.FormServiceImpl;
import org.operaton.bpm.engine.impl.HistoryServiceImpl;
import org.operaton.bpm.engine.impl.IdentityServiceImpl;
import org.operaton.bpm.engine.impl.ManagementServiceImpl;
import org.operaton.bpm.engine.impl.ModificationBatchJobHandler;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.PriorityProvider;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.RepositoryServiceImpl;
import org.operaton.bpm.engine.impl.RestartProcessInstancesJobHandler;
import org.operaton.bpm.engine.impl.RuntimeServiceImpl;
import org.operaton.bpm.engine.impl.ServiceImpl;
import org.operaton.bpm.engine.impl.TaskServiceImpl;
import org.operaton.bpm.engine.impl.application.ProcessApplicationManager;
import org.operaton.bpm.engine.impl.batch.BatchJobHandler;
import org.operaton.bpm.engine.impl.batch.BatchMonitorJobHandler;
import org.operaton.bpm.engine.impl.batch.BatchSeedJobHandler;
import org.operaton.bpm.engine.impl.batch.deletion.DeleteHistoricProcessInstancesJobHandler;
import org.operaton.bpm.engine.impl.batch.deletion.DeleteProcessInstancesJobHandler;
import org.operaton.bpm.engine.impl.batch.externaltask.SetExternalTaskRetriesJobHandler;
import org.operaton.bpm.engine.impl.batch.job.SetJobRetriesJobHandler;
import org.operaton.bpm.engine.impl.batch.message.MessageCorrelationBatchJobHandler;
import org.operaton.bpm.engine.impl.batch.removaltime.BatchSetRemovalTimeJobHandler;
import org.operaton.bpm.engine.impl.batch.removaltime.DecisionSetRemovalTimeJobHandler;
import org.operaton.bpm.engine.impl.batch.removaltime.ProcessSetRemovalTimeJobHandler;
import org.operaton.bpm.engine.impl.batch.update.UpdateProcessInstancesSuspendStateJobHandler;
import org.operaton.bpm.engine.impl.batch.variables.BatchSetVariablesHandler;
import org.operaton.bpm.engine.impl.bpmn.behavior.ExternalTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.deployer.BpmnDeployer;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParser;
import org.operaton.bpm.engine.impl.bpmn.parser.DefaultFailedJobParseListener;
import org.operaton.bpm.engine.impl.calendar.BusinessCalendarManager;
import org.operaton.bpm.engine.impl.calendar.CycleBusinessCalendar;
import org.operaton.bpm.engine.impl.calendar.DueDateBusinessCalendar;
import org.operaton.bpm.engine.impl.calendar.DurationBusinessCalendar;
import org.operaton.bpm.engine.impl.calendar.MapBusinessCalendarManager;
import org.operaton.bpm.engine.impl.cfg.auth.AuthorizationCommandChecker;
import org.operaton.bpm.engine.impl.cfg.auth.DefaultAuthorizationProvider;
import org.operaton.bpm.engine.impl.cfg.auth.DefaultPermissionProvider;
import org.operaton.bpm.engine.impl.cfg.auth.PermissionProvider;
import org.operaton.bpm.engine.impl.cfg.auth.ResourceAuthorizationProvider;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantCommandChecker;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProvider;
import org.operaton.bpm.engine.impl.cfg.standalone.StandaloneTransactionContextFactory;
import org.operaton.bpm.engine.impl.cmd.HistoryCleanupCmd;
import org.operaton.bpm.engine.impl.cmmn.CaseServiceImpl;
import org.operaton.bpm.engine.impl.cmmn.deployer.CmmnDeployer;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionManager;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionManager;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseSentryPartManager;
import org.operaton.bpm.engine.impl.cmmn.handler.DefaultCmmnElementHandlerRegistry;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransformFactory;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransformListener;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransformer;
import org.operaton.bpm.engine.impl.cmmn.transformer.DefaultCmmnTransformFactory;
import org.operaton.bpm.engine.impl.db.DbIdGenerator;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManagerFactory;
import org.operaton.bpm.engine.impl.db.entitymanager.cache.DbEntityCacheKeyMapping;
import org.operaton.bpm.engine.impl.db.sql.DbSqlPersistenceProviderFactory;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.delegate.DefaultDelegateInterceptor;
import org.operaton.bpm.engine.impl.diagnostics.DiagnosticsCollector;
import org.operaton.bpm.engine.impl.diagnostics.DiagnosticsRegistry;
import org.operaton.bpm.engine.impl.digest.Default16ByteSaltGenerator;
import org.operaton.bpm.engine.impl.digest.PasswordEncryptor;
import org.operaton.bpm.engine.impl.digest.PasswordManager;
import org.operaton.bpm.engine.impl.digest.SaltGenerator;
import org.operaton.bpm.engine.impl.digest.Sha512HashDigest;
import org.operaton.bpm.engine.impl.dmn.batch.DeleteHistoricDecisionInstancesJobHandler;
import org.operaton.bpm.engine.impl.dmn.configuration.DmnEngineConfigurationBuilder;
import org.operaton.bpm.engine.impl.dmn.deployer.DecisionDefinitionDeployer;
import org.operaton.bpm.engine.impl.dmn.deployer.DecisionRequirementsDefinitionDeployer;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionManager;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionManager;
import org.operaton.bpm.engine.impl.el.CommandContextFunctions;
import org.operaton.bpm.engine.impl.el.DateTimeFunctions;
import org.operaton.bpm.engine.impl.el.ElProviderCompatible;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.el.JuelExpressionManager;
import org.operaton.bpm.engine.impl.errorcode.ExceptionCodeProvider;
import org.operaton.bpm.engine.impl.event.CompensationEventHandler;
import org.operaton.bpm.engine.impl.event.ConditionalEventHandler;
import org.operaton.bpm.engine.impl.event.EventHandler;
import org.operaton.bpm.engine.impl.event.EventHandlerImpl;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.event.SignalEventHandler;
import org.operaton.bpm.engine.impl.externaltask.DefaultExternalTaskPriorityProvider;
import org.operaton.bpm.engine.impl.form.deployer.OperatonFormDefinitionDeployer;
import org.operaton.bpm.engine.impl.form.engine.FormEngine;
import org.operaton.bpm.engine.impl.form.engine.HtmlFormEngine;
import org.operaton.bpm.engine.impl.form.engine.JuelFormEngine;
import org.operaton.bpm.engine.impl.form.entity.OperatonFormDefinitionManager;
import org.operaton.bpm.engine.impl.form.type.AbstractFormFieldType;
import org.operaton.bpm.engine.impl.form.type.BooleanFormType;
import org.operaton.bpm.engine.impl.form.type.DateFormType;
import org.operaton.bpm.engine.impl.form.type.FormTypes;
import org.operaton.bpm.engine.impl.form.type.LongFormType;
import org.operaton.bpm.engine.impl.form.type.StringFormType;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidator;
import org.operaton.bpm.engine.impl.form.validator.FormValidators;
import org.operaton.bpm.engine.impl.form.validator.MaxLengthValidator;
import org.operaton.bpm.engine.impl.form.validator.MaxValidator;
import org.operaton.bpm.engine.impl.form.validator.MinLengthValidator;
import org.operaton.bpm.engine.impl.form.validator.MinValidator;
import org.operaton.bpm.engine.impl.form.validator.ReadOnlyValidator;
import org.operaton.bpm.engine.impl.form.validator.RequiredValidator;
import org.operaton.bpm.engine.impl.history.DefaultHistoryRemovalTimeProvider;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.HistoryRemovalTimeProvider;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInstanceManager;
import org.operaton.bpm.engine.impl.history.event.HostnameProvider;
import org.operaton.bpm.engine.impl.history.event.SimpleIpBasedProvider;
import org.operaton.bpm.engine.impl.history.handler.CompositeDbHistoryEventHandler;
import org.operaton.bpm.engine.impl.history.handler.CompositeHistoryEventHandler;
import org.operaton.bpm.engine.impl.history.handler.DbHistoryEventHandler;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.engine.impl.history.parser.HistoryParseListener;
import org.operaton.bpm.engine.impl.history.producer.CacheAwareCmmnHistoryEventProducer;
import org.operaton.bpm.engine.impl.history.producer.CacheAwareHistoryEventProducer;
import org.operaton.bpm.engine.impl.history.producer.CmmnHistoryEventProducer;
import org.operaton.bpm.engine.impl.history.producer.DefaultDmnHistoryEventProducer;
import org.operaton.bpm.engine.impl.history.producer.DmnHistoryEventProducer;
import org.operaton.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.operaton.bpm.engine.impl.history.transformer.CmmnHistoryTransformListener;
import org.operaton.bpm.engine.impl.identity.DefaultPasswordPolicyImpl;
import org.operaton.bpm.engine.impl.identity.ReadOnlyIdentityProvider;
import org.operaton.bpm.engine.impl.identity.WritableIdentityProvider;
import org.operaton.bpm.engine.impl.identity.db.DbIdentityServiceProvider;
import org.operaton.bpm.engine.impl.incident.CompositeIncidentHandler;
import org.operaton.bpm.engine.impl.incident.DefaultIncidentHandler;
import org.operaton.bpm.engine.impl.incident.IncidentHandler;
import org.operaton.bpm.engine.impl.interceptor.CommandContextFactory;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutorImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandInterceptor;
import org.operaton.bpm.engine.impl.interceptor.DelegateInterceptor;
import org.operaton.bpm.engine.impl.interceptor.ExceptionCodeInterceptor;
import org.operaton.bpm.engine.impl.interceptor.SessionFactory;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultJobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultJobPriorityProvider;
import org.operaton.bpm.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.operaton.bpm.engine.impl.jobexecutor.JobDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.NotifyAcquisitionRejectedJobsHandler;
import org.operaton.bpm.engine.impl.jobexecutor.ProcessEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.RejectedJobsHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerActivateJobDefinitionHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerActivateProcessDefinitionHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventSubprocessJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendJobDefinitionHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerTaskListenerJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.BatchWindowManager;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.DefaultBatchWindowManager;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupBatch;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupHandler;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupHelper;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupJobHandler;
import org.operaton.bpm.engine.impl.metrics.MetricsRegistry;
import org.operaton.bpm.engine.impl.metrics.MetricsReporterIdProvider;
import org.operaton.bpm.engine.impl.metrics.parser.MetricsBpmnParseListener;
import org.operaton.bpm.engine.impl.metrics.parser.MetricsCmmnTransformListener;
import org.operaton.bpm.engine.impl.metrics.reporter.DbMetricsReporter;
import org.operaton.bpm.engine.impl.migration.DefaultMigrationActivityMatcher;
import org.operaton.bpm.engine.impl.migration.DefaultMigrationInstructionGenerator;
import org.operaton.bpm.engine.impl.migration.MigrationActivityMatcher;
import org.operaton.bpm.engine.impl.migration.MigrationInstructionGenerator;
import org.operaton.bpm.engine.impl.migration.batch.MigrationBatchJobHandler;
import org.operaton.bpm.engine.impl.migration.validation.activity.MigrationActivityValidator;
import org.operaton.bpm.engine.impl.migration.validation.activity.NoCompensationHandlerActivityValidator;
import org.operaton.bpm.engine.impl.migration.validation.activity.SupportedActivityValidator;
import org.operaton.bpm.engine.impl.migration.validation.activity.SupportedPassiveEventTriggerActivityValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.AsyncAfterMigrationValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.AsyncMigrationValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.AsyncProcessStartMigrationValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.MigratingActivityInstanceValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.MigratingCompensationInstanceValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.MigratingTransitionInstanceValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.NoUnmappedCompensationStartEventValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.NoUnmappedLeafInstanceValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.SupportedActivityInstanceValidator;
import org.operaton.bpm.engine.impl.migration.validation.instance.VariableConflictActivityInstanceValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.AdditionalFlowScopeInstructionValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.CannotAddMultiInstanceBodyValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.CannotAddMultiInstanceInnerActivityValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.CannotRemoveMultiInstanceInnerActivityValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.ConditionalEventUpdateEventTriggerValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.GatewayMappingValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.MigrationInstructionValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.OnlyOnceMappedActivityInstructionValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.SameBehaviorInstructionValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.SameEventScopeInstructionValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.SameEventTypeValidator;
import org.operaton.bpm.engine.impl.migration.validation.instruction.UpdateEventTriggersValidator;
import org.operaton.bpm.engine.impl.mock.MocksResolverFactory;
import org.operaton.bpm.engine.impl.optimize.OptimizeManager;
import org.operaton.bpm.engine.impl.persistence.GenericManagerFactory;
import org.operaton.bpm.engine.impl.persistence.deploy.Deployer;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.CacheFactory;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DefaultCacheFactory;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.AttachmentManager;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationManager;
import org.operaton.bpm.engine.impl.persistence.entity.BatchManager;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayManager;
import org.operaton.bpm.engine.impl.persistence.entity.CommentManager;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentManager;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionManager;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionManager;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskManager;
import org.operaton.bpm.engine.impl.persistence.entity.FilterManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricActivityInstanceManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricBatchManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricCaseActivityInstanceManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricCaseInstanceManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricDetailManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricExternalTaskLogManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIdentityLinkLogManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricProcessInstanceManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricStatisticsManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricTaskInstanceManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceManager;
import org.operaton.bpm.engine.impl.persistence.entity.IdentityInfoManager;
import org.operaton.bpm.engine.impl.persistence.entity.IdentityLinkManager;
import org.operaton.bpm.engine.impl.persistence.entity.IncidentManager;
import org.operaton.bpm.engine.impl.persistence.entity.JobDefinitionManager;
import org.operaton.bpm.engine.impl.persistence.entity.JobManager;
import org.operaton.bpm.engine.impl.persistence.entity.MeterLogManager;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionManager;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyManager;
import org.operaton.bpm.engine.impl.persistence.entity.ReportManager;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceManager;
import org.operaton.bpm.engine.impl.persistence.entity.SchemaLogManager;
import org.operaton.bpm.engine.impl.persistence.entity.StatisticsManager;
import org.operaton.bpm.engine.impl.persistence.entity.TableDataManager;
import org.operaton.bpm.engine.impl.persistence.entity.TaskManager;
import org.operaton.bpm.engine.impl.persistence.entity.TaskReportManager;
import org.operaton.bpm.engine.impl.persistence.entity.TenantManager;
import org.operaton.bpm.engine.impl.persistence.entity.UserOperationLogManager;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceManager;
import org.operaton.bpm.engine.impl.repository.DefaultDeploymentHandlerFactory;
import org.operaton.bpm.engine.impl.runtime.ConditionHandler;
import org.operaton.bpm.engine.impl.runtime.CorrelationHandler;
import org.operaton.bpm.engine.impl.runtime.DefaultConditionHandler;
import org.operaton.bpm.engine.impl.runtime.DefaultCorrelationHandler;
import org.operaton.bpm.engine.impl.runtime.DefaultDeserializationTypeValidator;
import org.operaton.bpm.engine.impl.scripting.ScriptFactory;
import org.operaton.bpm.engine.impl.scripting.engine.BeansResolverFactory;
import org.operaton.bpm.engine.impl.scripting.engine.DefaultScriptEngineResolver;
import org.operaton.bpm.engine.impl.scripting.engine.OperatonScriptEngineManager;
import org.operaton.bpm.engine.impl.scripting.engine.ResolverFactory;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptBindingsFactory;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptEngineResolver;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptingEngines;
import org.operaton.bpm.engine.impl.scripting.engine.VariableScopeResolverFactory;
import org.operaton.bpm.engine.impl.scripting.env.ScriptEnvResolver;
import org.operaton.bpm.engine.impl.scripting.env.ScriptingEnvironment;
import org.operaton.bpm.engine.impl.telemetry.dto.DatabaseImpl;
import org.operaton.bpm.engine.impl.telemetry.dto.InternalsImpl;
import org.operaton.bpm.engine.impl.telemetry.dto.JdkImpl;
import org.operaton.bpm.engine.impl.telemetry.dto.ProductImpl;
import org.operaton.bpm.engine.impl.telemetry.dto.TelemetryDataImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.ParseUtil;
import org.operaton.bpm.engine.impl.util.ProcessEngineDetails;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.impl.variable.ValueTypeResolverImpl;
import org.operaton.bpm.engine.impl.variable.serializer.BooleanValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ByteArrayValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.DateValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.DefaultVariableSerializers;
import org.operaton.bpm.engine.impl.variable.serializer.DoubleValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.FileValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.IntegerValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.JavaObjectSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.LongValueSerlializer;
import org.operaton.bpm.engine.impl.variable.serializer.NullValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ShortValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.StringValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializerFactory;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializers;
import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.DeploymentHandlerFactory;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.WhitelistingDeserializationTypeValidator;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author Tom Baeyens
 */
public abstract class ProcessEngineConfigurationImpl extends ProcessEngineConfiguration {

  protected static ConfigurationLogger LOG = ProcessEngineLogger.CONFIG_LOGGER;

  public static final String DB_SCHEMA_UPDATE_CREATE = "create";
  public static final String DB_SCHEMA_UPDATE_DROP_CREATE = "drop-create";

  public static final int HISTORYLEVEL_NONE = HistoryLevel.HISTORY_LEVEL_NONE.getId();
  public static final int HISTORYLEVEL_ACTIVITY = HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId();
  public static final int HISTORYLEVEL_AUDIT = HistoryLevel.HISTORY_LEVEL_AUDIT.getId();
  public static final int HISTORYLEVEL_FULL = HistoryLevel.HISTORY_LEVEL_FULL.getId();

  public static final String DEFAULT_WS_SYNC_FACTORY = "org.operaton.bpm.engine.impl.webservice.CxfWebServiceClientFactory";

  public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/operaton/bpm/engine/impl/mapping/mappings.xml";

  public static final int DEFAULT_FAILED_JOB_LISTENER_MAX_RETRIES = 3;

  public static final int DEFAULT_INVOCATIONS_PER_BATCH_JOB = 1;

  protected static final Map<Object, Object> DEFAULT_BEANS_MAP = new HashMap<>();

  protected static final String PRODUCT_NAME = "Operaton BPM Runtime";

  public static SqlSessionFactory cachedSqlSessionFactory;

  protected static final Map<Integer, String> ISOLATION_LEVEL_CONSTANT_NAMES = Map.of(
      Connection.TRANSACTION_READ_COMMITTED, "READ_COMMITTED",
      Connection.TRANSACTION_READ_UNCOMMITTED, "READ_UNCOMMITTED",
      Connection.TRANSACTION_REPEATABLE_READ, "REPEATABLE_READ",
      Connection.TRANSACTION_SERIALIZABLE, "SERIALIZABLE");

  // SERVICES /////////////////////////////////////////////////////////////////

  protected volatile RepositoryService repositoryService = new RepositoryServiceImpl();
  protected volatile RuntimeService runtimeService = new RuntimeServiceImpl();
  protected volatile HistoryService historyService = new HistoryServiceImpl();
  protected volatile IdentityService identityService = new IdentityServiceImpl();
  protected volatile TaskService taskService = new TaskServiceImpl();
  protected volatile FormService formService = new FormServiceImpl();
  protected volatile ManagementService managementService = new ManagementServiceImpl(this);
  protected volatile AuthorizationService authorizationService = new AuthorizationServiceImpl();
  protected volatile CaseService caseService = new CaseServiceImpl();
  protected volatile FilterService filterService = new FilterServiceImpl();
  protected volatile ExternalTaskService externalTaskService = new ExternalTaskServiceImpl();
  protected volatile DecisionService decisionService = new DecisionServiceImpl();
  protected volatile OptimizeService optimizeService = new OptimizeService();

  // COMMAND EXECUTORS ////////////////////////////////////////////////////////

  // Command executor and interceptor stack
  /**
   * the configurable list which will be {@link #initInterceptorChain(java.util.List) processed} to build the {@link #commandExecutorTxRequired}
   */
  protected volatile List<CommandInterceptor> customPreCommandInterceptorsTxRequired;
  protected volatile List<CommandInterceptor> customPostCommandInterceptorsTxRequired;

  protected volatile List<CommandInterceptor> commandInterceptorsTxRequired;

  /**
   * this will be initialized during the configurationComplete()
   */
  protected volatile CommandExecutor commandExecutorTxRequired;

  /**
   * the configurable list which will be {@link #initInterceptorChain(List) processed} to build the {@link #commandExecutorTxRequiresNew}
   */
  protected volatile List<CommandInterceptor> customPreCommandInterceptorsTxRequiresNew;
  protected volatile List<CommandInterceptor> customPostCommandInterceptorsTxRequiresNew;

  protected volatile List<CommandInterceptor> commandInterceptorsTxRequiresNew;

  /**
   * this will be initialized during the configurationComplete()
   */
  protected volatile CommandExecutor commandExecutorTxRequiresNew;

  /**
   * Separate command executor to be used for db schema operations. Must always use NON-JTA transactions
   */
  protected volatile CommandExecutor commandExecutorSchemaOperations;

  // SESSION FACTORIES ////////////////////////////////////////////////////////

  protected volatile List<SessionFactory> customSessionFactories;
  protected volatile DbSqlSessionFactory dbSqlSessionFactory;
  protected volatile Map<Class<?>, SessionFactory> sessionFactories;

  // DEPLOYERS ////////////////////////////////////////////////////////////////

  protected volatile List<Deployer> customPreDeployers;
  protected volatile List<Deployer> customPostDeployers;
  protected volatile List<Deployer> deployers;
  protected volatile DeploymentCache deploymentCache;

  // CACHE ////////////////////////////////////////////////////////////////////

  protected volatile CacheFactory cacheFactory;
  protected volatile int cacheCapacity = 1000;
  protected volatile boolean enableFetchProcessDefinitionDescription = true;

  // JOB EXECUTOR /////////////////////////////////////////////////////////////

  protected volatile List<JobHandler> customJobHandlers;
  protected volatile Map<String, JobHandler> jobHandlers;
  protected volatile JobExecutor jobExecutor;

  protected volatile PriorityProvider<JobDeclaration<?, ?>> jobPriorityProvider;

  protected volatile long jobExecutorPriorityRangeMin = Long.MIN_VALUE;
  protected volatile long jobExecutorPriorityRangeMax = Long.MAX_VALUE;

  /**
   * When set to false, exclusivity (no parallel execution) of tasks is applied per process instance.
   * When set to true, exclusivity (no parallel execution) of tasks is extended across all hierarchies of each given
   * process instance.
   * <p>
   * The feature is targeting processes which might spawn a hierarchy of subprocesses e.g. via a call activity.
   * The legacy behaviour does not guarantee sequential execution of any spawned subprocesses. Instead, it works only
   * at the root level. When the feature is enabled, any spawned subprocess of the root process will be acquired and
   * executed sequentially by one thread.
   * <p>
   * Note that the above configuration might introduce performance implications in complex process modelling that involves
   * high multi-instance multiplicity and numerous subprocesses.
   * <p>
   * Use the feature in combination with awareness of your process modeling.
   * <p>
   * Default value: false; to keep the legacy behaviour backwards compatible.
   */
  protected volatile boolean jobExecutorAcquireExclusiveOverProcessHierarchies = false;

  // EXTERNAL TASK /////////////////////////////////////////////////////////////
  protected volatile PriorityProvider<ExternalTaskActivityBehavior> externalTaskPriorityProvider;

  // MYBATIS SQL SESSION FACTORY //////////////////////////////////////////////

  protected volatile SqlSessionFactory sqlSessionFactory;
  protected volatile TransactionFactory transactionFactory;


  // ID GENERATOR /////////////////////////////////////////////////////////////
  protected volatile IdGenerator idGenerator;
  protected volatile DataSource idGeneratorDataSource;
  protected volatile String idGeneratorDataSourceJndiName;

  // INCIDENT HANDLER /////////////////////////////////////////////////////////

  protected volatile Map<String, IncidentHandler> incidentHandlers;
  protected volatile List<IncidentHandler> customIncidentHandlers;

  // BATCH ////////////////////////////////////////////////////////////////////

  protected volatile Map<String, BatchJobHandler<?>> batchHandlers;
  protected volatile List<BatchJobHandler<?>> customBatchJobHandlers;

  /**
   * Number of jobs created by a batch seed job invocation
   */
  protected volatile int batchJobsPerSeed = 100;
  /**
   * Number of invocations executed by a single batch job
   */
  protected volatile int invocationsPerBatchJob = DEFAULT_INVOCATIONS_PER_BATCH_JOB;

  /**
   * Map to set an individual value for each batch type to
   * control the invocations per batch job. Unless specified
   * in this map, value of 'invocationsPerBatchJob' is used.
   */
  protected volatile Map<String, Integer> invocationsPerBatchJobByBatchType;

  /**
   * seconds to wait between polling for batch completion
   */
  protected volatile int batchPollTime = 30;
  /**
   * default priority for batch jobs
   */
  protected volatile long batchJobPriority = DefaultPriorityProvider.DEFAULT_PRIORITY;

  // OTHER ////////////////////////////////////////////////////////////////////
  protected volatile List<FormEngine> customFormEngines;
  protected volatile Map<String, FormEngine> formEngines;

  protected volatile List<AbstractFormFieldType> customFormTypes;
  protected volatile FormTypes formTypes;
  protected volatile FormValidators formValidators;
  protected volatile Map<String, Class<? extends FormFieldValidator>> customFormFieldValidators;

  /** don't throw parsing exceptions for Operaton Forms if set to true*/
  protected volatile boolean disableStrictOperatonFormParsing = false;

  protected volatile List<TypedValueSerializer> customPreVariableSerializers;
  protected volatile List<TypedValueSerializer> customPostVariableSerializers;
  protected volatile VariableSerializers variableSerializers;
  protected volatile VariableSerializerFactory fallbackSerializerFactory;
  protected volatile boolean implicitVariableUpdateDetectionEnabled = true;

  protected volatile String defaultSerializationFormat = Variables.SerializationDataFormats.JAVA.getName();
  protected volatile boolean javaSerializationFormatEnabled = false;
  protected volatile String defaultCharsetName = null;
  protected volatile Charset defaultCharset = null;

  protected volatile ExpressionManager expressionManager;
  protected volatile ElProvider dmnElProvider;
  protected volatile ScriptingEngines scriptingEngines;
  protected volatile List<ResolverFactory> resolverFactories;
  protected volatile ScriptingEnvironment scriptingEnvironment;
  protected volatile List<ScriptEnvResolver> scriptEnvResolvers;
  protected volatile ScriptFactory scriptFactory;
  protected volatile ScriptEngineResolver scriptEngineResolver;
  protected volatile String scriptEngineNameJavaScript;
  protected volatile boolean autoStoreScriptVariables = false;
  protected volatile boolean enableScriptCompilation = true;
  protected volatile boolean enableScriptEngineCaching = true;
  protected volatile boolean enableFetchScriptEngineFromProcessApplication = true;
  protected volatile boolean enableScriptEngineLoadExternalResources = false;
  protected volatile boolean enableScriptEngineNashornCompatibility = false;
  protected volatile boolean configureScriptEngineHostAccess = true;
  protected volatile boolean skipIsolationLevelCheck = false;

  /**
   * When set to false, the following behavior changes:
   * <ul>
   * <li>The automated schema maintenance (creating and dropping tables, see property <code>databaseSchemaUpdate</code>)
   *   does not cover the tables required for CMMN execution.</li>
   * <li>CMMN resources are not deployed as {@link CaseDefinition} to the engine.</li>
   * <li>Tasks from CMMN cases are not returned by the {@link TaskQuery}.</li>
   * </ul>
   */
  protected volatile boolean cmmnEnabled = true;

    /**
   * When set to false, the following behavior changes:
   * <ul>
   * <li>The automated schema maintenance (creating and dropping tables, see property <code>databaseSchemaUpdate</code>)
   *   does not cover the tables required for DMN execution.</li>
   * <li>DMN resources are not deployed as {@link DecisionDefinition} or
   *   {@link DecisionRequirementsDefinition} to the engine.</li>
   * </ul>
   */
  protected volatile boolean dmnEnabled = true;
  /**
   * When set to <code>false</code>, the following behavior changes:
   * <ul>
   *   <li>Standalone tasks can no longer be created via API.</li>
   *   <li>Standalone tasks are not returned by the TaskQuery.</li>
   * </ul>
   */
  protected volatile boolean standaloneTasksEnabled = true;

  protected volatile boolean enableGracefulDegradationOnContextSwitchFailure = true;

  protected volatile BusinessCalendarManager businessCalendarManager;

  protected volatile String wsSyncFactoryClassName = DEFAULT_WS_SYNC_FACTORY;

  protected volatile CommandContextFactory commandContextFactory;
  protected volatile TransactionContextFactory transactionContextFactory;
  protected volatile BpmnParseFactory bpmnParseFactory;

  // cmmn
  protected volatile CmmnTransformFactory cmmnTransformFactory;
  protected volatile DefaultCmmnElementHandlerRegistry cmmnElementHandlerRegistry;

  // dmn
  protected volatile DefaultDmnEngineConfiguration dmnEngineConfiguration;
  protected volatile DmnEngine dmnEngine;

  /**
   * a list of DMN FEEL custom function providers
   */
  protected volatile List<FeelCustomFunctionProvider> dmnFeelCustomFunctionProviders;

  /**
   * Enable DMN FEEL legacy behavior
   */
  protected volatile boolean dmnFeelEnableLegacyBehavior = false;

  /**
   * Controls whether blank DMN table outputs are swallowed or returned as {@code null}.
   */
  protected volatile boolean dmnReturnBlankTableOutputAsNull = false;

  protected volatile HistoryLevel historyLevel;

  /**
   * a list of supported history levels
   */
  protected List<HistoryLevel> historyLevels;

  /**
   * a list of supported custom history levels
   */
  protected volatile List<HistoryLevel> customHistoryLevels;

  protected volatile List<BpmnParseListener> preParseListeners;
  protected volatile List<BpmnParseListener> postParseListeners;

  protected volatile List<CmmnTransformListener> customPreCmmnTransformListeners;
  protected volatile List<CmmnTransformListener> customPostCmmnTransformListeners;

  protected volatile Map<Object, Object> beans;

  protected volatile boolean isDbIdentityUsed = true;
  protected volatile boolean isDbHistoryUsed = true;

  protected volatile DelegateInterceptor delegateInterceptor;

  protected volatile CommandInterceptor actualCommandExecutor;

  protected volatile RejectedJobsHandler customRejectedJobsHandler;

  protected volatile Map<String, EventHandler> eventHandlers;
  protected volatile List<EventHandler> customEventHandlers;

  protected volatile FailedJobCommandFactory failedJobCommandFactory;

  protected volatile String databaseTablePrefix = "";

  /**
   * In some situations you want to set the schema to use for table checks / generation if the database metadata
   * doesn't return that correctly, see https://jira.codehaus.org/browse/ACT-1220,
   * https://jira.codehaus.org/browse/ACT-1062
   */
  protected volatile String databaseSchema = null;

  protected volatile boolean isCreateDiagramOnDeploy = false;

  protected volatile ProcessApplicationManager processApplicationManager;

  protected volatile CorrelationHandler correlationHandler;

  protected volatile ConditionHandler conditionHandler;

  /**
   * session factory to be used for obtaining identity provider sessions
   */
  protected volatile SessionFactory identityProviderSessionFactory;

  protected volatile PasswordEncryptor passwordEncryptor;

  protected volatile List<PasswordEncryptor> customPasswordChecker;

  protected volatile PasswordManager passwordManager;

  protected volatile SaltGenerator saltGenerator;

  protected volatile Set<String> registeredDeployments;

  protected volatile DeploymentHandlerFactory deploymentHandlerFactory;

  protected volatile ResourceAuthorizationProvider resourceAuthorizationProvider;

  protected volatile List<ProcessEnginePlugin> processEnginePlugins = new ArrayList<>();

  protected volatile HistoryEventProducer historyEventProducer;

  protected volatile CmmnHistoryEventProducer cmmnHistoryEventProducer;

  protected volatile DmnHistoryEventProducer dmnHistoryEventProducer;

  /**
   * As an instance of {@link org.operaton.bpm.engine.impl.history.handler.CompositeHistoryEventHandler}
   * it contains all the provided history event handlers that process history events.
   */
  protected volatile HistoryEventHandler historyEventHandler;

  /**
   *  Allows users to add additional {@link HistoryEventHandler}
   *  instances to process history events.
   */
  protected volatile List<HistoryEventHandler> customHistoryEventHandlers = new ArrayList<>();

  /**
   * If true, the default {@link DbHistoryEventHandler} will be included in the list
   * of history event handlers.
   */
  protected volatile boolean enableDefaultDbHistoryEventHandler = true;

  protected volatile PermissionProvider permissionProvider;

  protected volatile boolean isExecutionTreePrefetchEnabled = true;

  /**
   * If true, the incident handlers init as {@link CompositeIncidentHandler} and
   * multiple incident handlers can be added for the same Incident type.
   * However, only the result from the "main" incident handler will be returned.
   * <p>
   * All {@link customIncidentHandlers} will be added as sub handlers to {@link CompositeIncidentHandler} for same handler type.
   * <p>
   * By default, main handler is {@link DefaultIncidentHandler}.
   * To override the main handler you need create {@link CompositeIncidentHandler} with your main IncidentHandler and
   * init {@link incidentHandlers} before setting up the engine.
   *
   * @see CompositeIncidentHandler
   * @see #initIncidentHandlers
   */
  protected volatile boolean isCompositeIncidentHandlersEnabled = false;

  /**
   * If true the process engine will attempt to acquire an exclusive lock before
   * creating a deployment.
   */
  protected volatile boolean isDeploymentLockUsed = true;

  /**
   * If true then several deployments will be processed strictly sequentially. When false they may be processed in parallel.
   */
  protected volatile boolean isDeploymentSynchronized = true;

  /**
   * Allows setting whether the process engine should try reusing the first level entity cache.
   * Default setting is false, enabling it improves performance of asynchronous continuations.
   */
  protected volatile boolean isDbEntityCacheReuseEnabled = false;

  protected volatile boolean isInvokeCustomVariableListeners = true;

  /**
   * The process engine created by this configuration.
   */
  protected volatile ProcessEngineImpl processEngine;

  /**
   * used to create instances for listeners, JavaDelegates, etc
   */
  protected volatile ArtifactFactory artifactFactory;

  protected volatile DbEntityCacheKeyMapping dbEntityCacheKeyMapping = DbEntityCacheKeyMapping.defaultEntityCacheKeyMapping();

  /**
   * the metrics registry
   */
  protected volatile MetricsRegistry metricsRegistry;

  protected volatile DbMetricsReporter dbMetricsReporter;

  protected volatile boolean isMetricsEnabled = true;
  protected volatile boolean isDbMetricsReporterActivate = true;

  protected volatile MetricsReporterIdProvider metricsReporterIdProvider;

  protected volatile boolean isTaskMetricsEnabled = true;

  /**
   * the historic job log host name
   */
  protected volatile String hostname;
  protected volatile HostnameProvider hostnameProvider;

  /**
   * handling of expressions submitted via API; can be used as guards against remote code execution
   */
  protected volatile boolean enableExpressionsInAdhocQueries = false;
  protected volatile boolean enableExpressionsInStoredQueries = true;

  /**
   * If false, disables XML eXternal Entity (XXE) Processing. This provides protection against XXE Processing attacks.
   */
  protected volatile boolean enableXxeProcessing = false;

  /**
   * If true, user operation log entries are only written if there is an
   * authenticated user present in the context. If false, user operation log
   * entries are written regardless of authentication state.
   */
  protected volatile boolean restrictUserOperationLogToAuthenticatedUsers = true;

  /**
   * Maximum number of operation log entries written per synchronous operation.
   * APIs that can affect multiple entities can produce an operation log entry each.
   * This property controls how the operation logs are handled. Possible values:
   * <ul>
   *   <li>1 (Default): Always write one summary operation log including message name, number of affected entities, number of variables set by operation, and async=false</li>
   *   <lI>-1: Disable limiting of operation logs for synchronous APIs. Write one log entry per affected entity. Caution: This may impact performance with large data.</li>
   *   <li>1<x<=Long.MAX_VALUE: Write at most x operation logs per synchronous operation. If an operation exceeds x, a {@link ProcessEngineException} is thrown.</li>
   * </ul>
   */
  protected volatile long logEntriesPerSyncOperationLimit = 1L;

  protected volatile boolean disableStrictCallActivityValidation = false;

  protected volatile boolean isBpmnStacktraceVerbose = false;

  protected volatile boolean forceCloseMybatisConnectionPool = true;

  protected volatile TenantIdProvider tenantIdProvider = null;

  protected volatile List<CommandChecker> commandCheckers = null;

  protected volatile List<String> adminGroups;

  protected volatile List<String> adminUsers;

  // Migration
  protected volatile MigrationActivityMatcher migrationActivityMatcher;

  protected volatile List<MigrationActivityValidator> customPreMigrationActivityValidators;
  protected volatile List<MigrationActivityValidator> customPostMigrationActivityValidators;
  protected volatile MigrationInstructionGenerator migrationInstructionGenerator;

  protected volatile List<MigrationInstructionValidator> customPreMigrationInstructionValidators;
  protected volatile List<MigrationInstructionValidator> customPostMigrationInstructionValidators;
  protected volatile List<MigrationInstructionValidator> migrationInstructionValidators;

  protected volatile List<MigratingActivityInstanceValidator> customPreMigratingActivityInstanceValidators;
  protected volatile List<MigratingActivityInstanceValidator> customPostMigratingActivityInstanceValidators;
  protected volatile List<MigratingActivityInstanceValidator> migratingActivityInstanceValidators;
  protected volatile List<MigratingTransitionInstanceValidator> migratingTransitionInstanceValidators;
  protected volatile List<MigratingCompensationInstanceValidator> migratingCompensationInstanceValidators;

  // Default user permission for task
  protected volatile Permission defaultUserPermissionForTask;

  /**
   * Historic instance permissions are disabled by default
   */
  protected volatile boolean enableHistoricInstancePermissions = false;

  protected volatile boolean isUseSharedSqlSessionFactory = false;

  //History cleanup configuration
  protected volatile String historyCleanupBatchWindowStartTime;
  protected volatile String historyCleanupBatchWindowEndTime = "00:00";

  protected volatile Date historyCleanupBatchWindowStartTimeAsDate;
  protected volatile Date historyCleanupBatchWindowEndTimeAsDate;

  protected volatile Map<Integer, BatchWindowConfiguration> historyCleanupBatchWindows = new HashMap<>();

  //shortcuts for batch windows configuration available to be configured from XML
  protected volatile String mondayHistoryCleanupBatchWindowStartTime;
  protected volatile String mondayHistoryCleanupBatchWindowEndTime;
  protected volatile String tuesdayHistoryCleanupBatchWindowStartTime;
  protected volatile String tuesdayHistoryCleanupBatchWindowEndTime;
  protected volatile String wednesdayHistoryCleanupBatchWindowStartTime;
  protected volatile String wednesdayHistoryCleanupBatchWindowEndTime;
  protected volatile String thursdayHistoryCleanupBatchWindowStartTime;
  protected volatile String thursdayHistoryCleanupBatchWindowEndTime;
  protected volatile String fridayHistoryCleanupBatchWindowStartTime;
  protected volatile String fridayHistoryCleanupBatchWindowEndTime;
  protected volatile String saturdayHistoryCleanupBatchWindowStartTime;
  protected volatile String saturdayHistoryCleanupBatchWindowEndTime;
  protected volatile String sundayHistoryCleanupBatchWindowStartTime;
  protected volatile String sundayHistoryCleanupBatchWindowEndTime;

  protected volatile int historyCleanupDegreeOfParallelism = 1;

  protected volatile String historyTimeToLive;

  /**
   * Feature flag that fails the deployment of process, decision, and case resources if their historyTimeToLive is {@code null}.
   */
  protected volatile boolean enforceHistoryTimeToLive = true;

  protected volatile String batchOperationHistoryTimeToLive;
  protected volatile Map<String, String> batchOperationsForHistoryCleanup;
  protected volatile Map<String, Integer> parsedBatchOperationsForHistoryCleanup;

  /**
   * Default priority for history cleanup jobs. */
  protected volatile long historyCleanupJobPriority = DefaultPriorityProvider.DEFAULT_PRIORITY;

  /**
   * Specifies how often a cleanup job will be executed before an incident is raised.
   * This property overrides the global {@code defaultNumberOfRetries} property which has a default value of {@code 3}.
   */
  protected volatile int historyCleanupDefaultNumberOfRetries = Integer.MIN_VALUE;

  /**
   * Time to live for historic job log entries written by history cleanup jobs.
   * Must be an ISO-8601 conform String specifying only a number of days. Only
   * works in conjunction with removal-time-based cleanup strategy.
   */
  protected volatile String historyCleanupJobLogTimeToLive;

  protected volatile String taskMetricsTimeToLive;
  protected volatile Integer parsedTaskMetricsTimeToLive;

  protected volatile BatchWindowManager batchWindowManager = new DefaultBatchWindowManager();

  protected volatile HistoryRemovalTimeProvider historyRemovalTimeProvider;

  protected volatile String historyRemovalTimeStrategy;

  protected volatile String historyCleanupStrategy;

  /**
   * Size of batch in which history cleanup data will be deleted. {@link HistoryCleanupBatch#MAX_BATCH_SIZE} must be respected.
   */
  private volatile int historyCleanupBatchSize = 500;
  /**
   * Indicates the minimal amount of data to trigger the history cleanup.
   */
  private volatile int historyCleanupBatchThreshold = 10;

  private volatile boolean historyCleanupMetricsEnabled = true;

  /**
   * Controls whether engine participates in history cleanup or not.
   */
  protected volatile boolean historyCleanupEnabled = true;

  private volatile int failedJobListenerMaxRetries = DEFAULT_FAILED_JOB_LISTENER_MAX_RETRIES;

  protected volatile String failedJobRetryTimeCycle;

  // login attempts ///////////////////////////////////////////////////////
  protected volatile int loginMaxAttempts = 10;
  protected volatile int loginDelayFactor = 2;
  protected volatile int loginDelayMaxTime = 60;
  protected volatile int loginDelayBase = 3;

  // webapps logging
  protected volatile boolean webappsAuthenticationLoggingEnabled = false;

  // max results limit
  protected volatile int queryMaxResultsLimit = Integer.MAX_VALUE;

  // logging context property names (with default values)
  protected volatile String loggingContextActivityId = "activityId";
  protected volatile String loggingContextActivityName = "activityName";
  protected volatile String loggingContextApplicationName = "applicationName";
  protected volatile String loggingContextBusinessKey;// default == null => disabled by default
  protected volatile String loggingContextProcessDefinitionId = "processDefinitionId";
  protected volatile String loggingContextProcessDefinitionKey;// default == null => disabled by default
  protected volatile String loggingContextProcessInstanceId = "processInstanceId";
  protected volatile String loggingContextTenantId = "tenantId";
  protected volatile String loggingContextEngineName = "engineName";

  // logging levels (with default values)
  protected volatile String logLevelBpmnStackTrace = "DEBUG";

  // OLEs for foreign key constraint violations on databases that rollback on SQL exceptions, e.g. PostgreSQL
  protected volatile boolean enableOptimisticLockingOnForeignKeyViolation = true;

  protected volatile int telemetryRequestTimeout;
  protected volatile TelemetryDataImpl telemetryData;
  protected volatile DiagnosticsRegistry diagnosticsRegistry;
  protected volatile DiagnosticsCollector diagnosticsCollector;

  // Exception Codes ///////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Disables the {@link ExceptionCodeInterceptor} and therefore the whole exception code feature.
   */
  protected volatile boolean disableExceptionCode;

  /**
   * Disables the default implementation of {@link ExceptionCodeProvider} which allows overriding the reserved
   * exception codes > {@link ExceptionCodeInterceptor#MAX_CUSTOM_CODE} or < {@link ExceptionCodeInterceptor#MIN_CUSTOM_CODE}.
   */
  protected volatile boolean disableBuiltinExceptionCodeProvider;

  /**
   * Allows registering a custom implementation of a {@link ExceptionCodeProvider}
   * allowing to provide custom exception codes.
   */
  protected volatile ExceptionCodeProvider customExceptionCodeProvider;

  /**
   * Holds the default implementation of {@link ExceptionCodeProvider}.
   */
  protected volatile ExceptionCodeProvider builtinExceptionCodeProvider;

  /** Controls whether the time cycle is re-evaluated when due. */
  protected volatile boolean reevaluateTimeCycleWhenDue = false;

  /**
   * Size of batch in which removal time data will be updated. {@link ProcessSetRemovalTimeJobHandler#MAX_CHUNK_SIZE} must be respected.
   */
  protected volatile int removalTimeUpdateChunkSize = 500;

  /**
   * @return {@code true} if the exception code feature is disabled and vice-versa.
   */
  public boolean isDisableExceptionCode() {
    return disableExceptionCode;
  }

  /**
   * Setter to disables the {@link ExceptionCodeInterceptor} and therefore the whole exception code feature.
   */
  public void setDisableExceptionCode(boolean disableExceptionCode) {
    this.disableExceptionCode = disableExceptionCode;
  }

  /**
   * @return {@code true} if the built-in exception code provider is disabled and vice-versa.
   */
  public boolean isDisableBuiltinExceptionCodeProvider() {
    return disableBuiltinExceptionCodeProvider;
  }

  /**
   * Setter to disables the default implementation of {@link ExceptionCodeProvider} which allows overriding the reserved
   * exception codes > {@link ExceptionCodeInterceptor#MAX_CUSTOM_CODE} or < {@link ExceptionCodeInterceptor#MIN_CUSTOM_CODE}.
   */
  public void setDisableBuiltinExceptionCodeProvider(boolean disableBuiltinExceptionCodeProvider) {
    this.disableBuiltinExceptionCodeProvider = disableBuiltinExceptionCodeProvider;
  }

  /**
   * @return a custom implementation of a {@link ExceptionCodeProvider} allowing to provide custom error codes.
   */
  public ExceptionCodeProvider getCustomExceptionCodeProvider() {
    return customExceptionCodeProvider;
  }

  /**
   * Setter to register a custom implementation of a {@link ExceptionCodeProvider} allowing to provide custom error codes.
   */
  public void setCustomExceptionCodeProvider(ExceptionCodeProvider customExceptionCodeProvider) {
    this.customExceptionCodeProvider = customExceptionCodeProvider;
  }

  public ExceptionCodeProvider getBuiltinExceptionCodeProvider() {
    return builtinExceptionCodeProvider;
  }

  public void setBuiltinExceptionCodeProvider(ExceptionCodeProvider builtinExceptionCodeProvider) {
    this.builtinExceptionCodeProvider = builtinExceptionCodeProvider;
  }

  // buildProcessEngine ///////////////////////////////////////////////////////

  @Override
  public ProcessEngine buildProcessEngine() {
    init();
    processEngine = new ProcessEngineImpl(this);
    invokePostProcessEngineBuild(processEngine);
    return processEngine;
  }

  // init /////////////////////////////////////////////////////////////////////

  protected void init() {
    invokePreInit();
    initDefaultCharset();
    initHistoryLevel();
    initHistoryEventProducer();
    initCmmnHistoryEventProducer();
    initDmnHistoryEventProducer();
    initHistoryEventHandler();
    initExpressionManager();
    initBeans();
    initArtifactFactory();
    initFormEngines();
    initFormTypes();
    initFormFieldValidators();
    initScripting();
    initDmnEngine();
    initBusinessCalendarManager();
    initCommandContextFactory();
    initTransactionContextFactory();

    // Database type needs to be detected before CommandExecutors are initialized
    initDataSource();

    initExceptionCodeProvider();
    initCommandExecutors();
    initServices();
    initIdGenerator();
    initFailedJobCommandFactory();
    initDeployers();
    initJobProvider();
    initExternalTaskPriorityProvider();
    initBatchHandlers();
    initJobExecutor();
    initTransactionFactory();
    initSqlSessionFactory();
    initIdentityProviderSessionFactory();
    initSessionFactories();
    initValueTypeResolver();
    initTypeValidator();
    initSerialization();
    initDelegateInterceptor();
    initEventHandlers();
    initProcessApplicationManager();
    initCorrelationHandler();
    initConditionHandler();
    initIncidentHandlers();
    initPasswordDigest();
    initDeploymentRegistration();
    initDeploymentHandlerFactory();
    initResourceAuthorizationProvider();
    initPermissionProvider();
    initHostName();
    initMetrics();
    initDiagnostics();
    initMigration();
    initCommandCheckers();
    initDefaultUserPermissionForTask();
    initHistoryRemovalTime();
    initHistoryCleanup();
    initInvocationsPerBatchJobByBatchType();
    initAdminUser();
    initAdminGroups();
    initPasswordPolicy();
    initOperationLog();
    invokePostInit();
  }

  public void initExceptionCodeProvider() {
    if (!isDisableBuiltinExceptionCodeProvider()) {
      builtinExceptionCodeProvider = new ExceptionCodeProvider() {};

    }
  }

  protected void initTypeValidator() {
    if (deserializationTypeValidator == null) {
      deserializationTypeValidator = new DefaultDeserializationTypeValidator();
    }
    if (deserializationTypeValidator instanceof WhitelistingDeserializationTypeValidator validator) {
      validator.setAllowedClasses(deserializationAllowedClasses);
      validator.setAllowedPackages(deserializationAllowedPackages);
    }
  }

  public void initHistoryRemovalTime() {
    initHistoryRemovalTimeProvider();
    initHistoryRemovalTimeStrategy();
  }

  public void initHistoryRemovalTimeStrategy() {
    if (historyRemovalTimeStrategy == null) {
      historyRemovalTimeStrategy = HISTORY_REMOVAL_TIME_STRATEGY_END;
    }

    if (!HISTORY_REMOVAL_TIME_STRATEGY_START.equals(historyRemovalTimeStrategy) &&
      !HISTORY_REMOVAL_TIME_STRATEGY_END.equals(historyRemovalTimeStrategy) &&
      !HISTORY_REMOVAL_TIME_STRATEGY_NONE.equals(historyRemovalTimeStrategy)) {
      throw LOG.invalidPropertyValue("historyRemovalTimeStrategy", historyRemovalTimeStrategy,
        String.format("history removal time strategy must be set to '%s', '%s' or '%s'", HISTORY_REMOVAL_TIME_STRATEGY_START, HISTORY_REMOVAL_TIME_STRATEGY_END, HISTORY_REMOVAL_TIME_STRATEGY_NONE));
    }
  }

  public void initHistoryRemovalTimeProvider() {
    if (historyRemovalTimeProvider == null) {
      historyRemovalTimeProvider = new DefaultHistoryRemovalTimeProvider();
    }
  }

  public void initHistoryCleanup() {
    initHistoryCleanupStrategy();

    //validate number of threads
    if (historyCleanupDegreeOfParallelism < 1 || historyCleanupDegreeOfParallelism > MAX_THREADS_NUMBER) {
      throw LOG.invalidPropertyValue("historyCleanupDegreeOfParallelism", String.valueOf(historyCleanupDegreeOfParallelism),
        String.format("value for number of threads for history cleanup should be between 1 and %s", HistoryCleanupCmd.MAX_THREADS_NUMBER));
    }

    if (historyCleanupBatchWindowStartTime != null) {
      initHistoryCleanupBatchWindowStartTime();
    }

    if (historyCleanupBatchWindowEndTime != null) {
      initHistoryCleanupBatchWindowEndTime();
    }

    initHistoryCleanupBatchWindowsMap();

    if (historyCleanupBatchSize > HistoryCleanupHandler.MAX_BATCH_SIZE || historyCleanupBatchSize <= 0) {
      throw LOG.invalidPropertyValue("historyCleanupBatchSize", String.valueOf(historyCleanupBatchSize),
          String.format("value for batch size should be between 1 and %s", HistoryCleanupHandler.MAX_BATCH_SIZE));
    }

    if (historyCleanupBatchThreshold < 0) {
      throw LOG.invalidPropertyValue("historyCleanupBatchThreshold", String.valueOf(historyCleanupBatchThreshold),
          "History cleanup batch threshold cannot be negative.");
    }

    initHistoryTimeToLive();

    initBatchOperationsHistoryTimeToLive();

    initHistoryCleanupJobLogTimeToLive();

    initTaskMetricsTimeToLive();
  }

  protected void initHistoryCleanupStrategy() {
    if (historyCleanupStrategy == null) {
      historyCleanupStrategy = HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED;
    }

    if (!HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED.equals(historyCleanupStrategy) &&
      !HISTORY_CLEANUP_STRATEGY_END_TIME_BASED.equals(historyCleanupStrategy)) {
      throw LOG.invalidPropertyValue("historyCleanupStrategy", historyCleanupStrategy,
        String.format("history cleanup strategy must be either set to '%s' or '%s'", HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED, HISTORY_CLEANUP_STRATEGY_END_TIME_BASED));
    }

    if (HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED.equals(historyCleanupStrategy) &&
      HISTORY_REMOVAL_TIME_STRATEGY_NONE.equals(historyRemovalTimeStrategy)) {
      throw LOG.invalidPropertyValue("historyRemovalTimeStrategy", historyRemovalTimeStrategy,
        String.format("history removal time strategy cannot be set to '%s' in conjunction with '%s' history cleanup strategy", HISTORY_REMOVAL_TIME_STRATEGY_NONE, HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED));
    }
  }

  private void initHistoryCleanupBatchWindowsMap() {
    if (mondayHistoryCleanupBatchWindowStartTime != null || mondayHistoryCleanupBatchWindowEndTime != null) {
      historyCleanupBatchWindows.put(Calendar.MONDAY, new BatchWindowConfiguration(mondayHistoryCleanupBatchWindowStartTime, mondayHistoryCleanupBatchWindowEndTime));
    }

    if (tuesdayHistoryCleanupBatchWindowStartTime != null || tuesdayHistoryCleanupBatchWindowEndTime != null) {
      historyCleanupBatchWindows.put(Calendar.TUESDAY, new BatchWindowConfiguration(tuesdayHistoryCleanupBatchWindowStartTime, tuesdayHistoryCleanupBatchWindowEndTime));
    }

    if (wednesdayHistoryCleanupBatchWindowStartTime != null || wednesdayHistoryCleanupBatchWindowEndTime != null) {
      historyCleanupBatchWindows.put(Calendar.WEDNESDAY, new BatchWindowConfiguration(wednesdayHistoryCleanupBatchWindowStartTime, wednesdayHistoryCleanupBatchWindowEndTime));
    }

    if (thursdayHistoryCleanupBatchWindowStartTime != null || thursdayHistoryCleanupBatchWindowEndTime != null) {
      historyCleanupBatchWindows.put(Calendar.THURSDAY, new BatchWindowConfiguration(thursdayHistoryCleanupBatchWindowStartTime, thursdayHistoryCleanupBatchWindowEndTime));
    }

    if (fridayHistoryCleanupBatchWindowStartTime != null || fridayHistoryCleanupBatchWindowEndTime != null) {
      historyCleanupBatchWindows.put(Calendar.FRIDAY, new BatchWindowConfiguration(fridayHistoryCleanupBatchWindowStartTime, fridayHistoryCleanupBatchWindowEndTime));
    }

    if (saturdayHistoryCleanupBatchWindowStartTime != null ||saturdayHistoryCleanupBatchWindowEndTime != null) {
      historyCleanupBatchWindows.put(Calendar.SATURDAY, new BatchWindowConfiguration(saturdayHistoryCleanupBatchWindowStartTime, saturdayHistoryCleanupBatchWindowEndTime));
    }

    if (sundayHistoryCleanupBatchWindowStartTime != null || sundayHistoryCleanupBatchWindowEndTime != null) {
      historyCleanupBatchWindows.put(Calendar.SUNDAY, new BatchWindowConfiguration(sundayHistoryCleanupBatchWindowStartTime, sundayHistoryCleanupBatchWindowEndTime));
    }
  }

  protected void initInvocationsPerBatchJobByBatchType() {
    if (invocationsPerBatchJobByBatchType == null) {
      invocationsPerBatchJobByBatchType = new HashMap<>();

    } else {
      Set<String> batchTypes = invocationsPerBatchJobByBatchType.keySet();
      batchTypes.stream()
          // batchHandlers contains custom & built-in batch handlers
          .filter(batchType -> !batchHandlers.containsKey(batchType))
          .forEach(LOG::invalidBatchTypeForInvocationsPerBatchJob);
    }
  }

  protected void initHistoryTimeToLive() {
    try {
      ParseUtil.parseHistoryTimeToLive(historyTimeToLive);
    } catch (Exception e) {
      throw LOG.invalidPropertyValue("historyTimeToLive", historyTimeToLive, e);
    }
  }

  protected void initBatchOperationsHistoryTimeToLive() {
    try {
      ParseUtil.parseHistoryTimeToLive(batchOperationHistoryTimeToLive);
    } catch (Exception e) {
      throw LOG.invalidPropertyValue("batchOperationHistoryTimeToLive", batchOperationHistoryTimeToLive, e);
    }

    if (batchOperationsForHistoryCleanup == null) {
      batchOperationsForHistoryCleanup = new HashMap<>();
    } else {
      for (var batchOperationEntry: batchOperationsForHistoryCleanup.entrySet()) {
        String batchOperation = batchOperationEntry.getKey();
        String timeToLive = batchOperationEntry.getValue();
        if (!batchHandlers.containsKey(batchOperation)) {
          LOG.invalidBatchOperation(batchOperation, timeToLive);
        }

        try {
          ParseUtil.parseHistoryTimeToLive(timeToLive);
        } catch (Exception e) {
          throw LOG.invalidPropertyValue("history time to live for " + batchOperation + " batch operations", timeToLive, e);
        }
      }
    }

    if (batchHandlers != null && batchOperationHistoryTimeToLive != null) {
      batchHandlers.keySet().forEach(batchOperation ->
        batchOperationsForHistoryCleanup.computeIfAbsent(batchOperation, k -> batchOperationHistoryTimeToLive)
      );
    }

    parsedBatchOperationsForHistoryCleanup = new HashMap<>();
    if (batchOperationsForHistoryCleanup != null) {
      for (var batchOperationEntry: batchOperationsForHistoryCleanup.entrySet()) {
        String operation = batchOperationEntry.getKey();
        Integer historyTTL = ParseUtil.parseHistoryTimeToLive(batchOperationEntry.getValue());
        parsedBatchOperationsForHistoryCleanup.put(operation, historyTTL);
      }
    }
  }

  private void initHistoryCleanupBatchWindowEndTime() {
    try {
      historyCleanupBatchWindowEndTimeAsDate = HistoryCleanupHelper.parseTimeConfiguration(historyCleanupBatchWindowEndTime);
    } catch (ParseException e) {
      throw LOG.invalidPropertyValue("historyCleanupBatchWindowEndTime", historyCleanupBatchWindowEndTime);
    }
  }

  private void initHistoryCleanupBatchWindowStartTime() {
    try {
      historyCleanupBatchWindowStartTimeAsDate = HistoryCleanupHelper.parseTimeConfiguration(historyCleanupBatchWindowStartTime);
    } catch (ParseException e) {
      throw LOG.invalidPropertyValue("historyCleanupBatchWindowStartTime", historyCleanupBatchWindowStartTime);
    }
  }

  protected void initHistoryCleanupJobLogTimeToLive() {
    try {
      ParseUtil.parseHistoryTimeToLive(historyCleanupJobLogTimeToLive);
    } catch (Exception e) {
      throw LOG.invalidPropertyValue("historyCleanupJobLogTimeToLive", historyCleanupJobLogTimeToLive, e);
    }
  }

  protected void initTaskMetricsTimeToLive() {
    try {
      parsedTaskMetricsTimeToLive = ParseUtil.parseHistoryTimeToLive(taskMetricsTimeToLive);
    } catch (Exception e) {
      throw LOG.invalidPropertyValue("taskMetricsTimeToLive", taskMetricsTimeToLive, e);
    }
  }

  protected void invokePreInit() {
    for (ProcessEnginePlugin plugin : processEnginePlugins) {
      LOG.pluginActivated(plugin.toString(), getProcessEngineName());
      plugin.preInit(this);
    }
  }

  protected void invokePostInit() {
    for (ProcessEnginePlugin plugin : processEnginePlugins) {
      plugin.postInit(this);
    }
  }

  protected void invokePostProcessEngineBuild(ProcessEngine engine) {
    for (ProcessEnginePlugin plugin : processEnginePlugins) {
      plugin.postProcessEngineBuild(engine);
    }
  }

  // failedJobCommandFactory ////////////////////////////////////////////////////////

  protected void initFailedJobCommandFactory() {
    if (failedJobCommandFactory == null) {
      failedJobCommandFactory = new DefaultFailedJobCommandFactory();
    }
    if (postParseListeners == null) {
      postParseListeners = new ArrayList<>();
    }
    postParseListeners.add(new DefaultFailedJobParseListener());

  }

  // incident handlers /////////////////////////////////////////////////////////////

  protected void initIncidentHandlers() {
    if (incidentHandlers == null) {
      incidentHandlers = new HashMap<>();

      DefaultIncidentHandler failedJobIncidentHandler = new DefaultIncidentHandler(Incident.FAILED_JOB_HANDLER_TYPE);
      DefaultIncidentHandler failedExternalTaskIncidentHandler = new DefaultIncidentHandler(
          Incident.EXTERNAL_TASK_HANDLER_TYPE);

      if (isCompositeIncidentHandlersEnabled) {
        addIncidentHandler(new CompositeIncidentHandler(failedJobIncidentHandler));
        addIncidentHandler(new CompositeIncidentHandler(failedExternalTaskIncidentHandler));
      } else {
        addIncidentHandler(failedJobIncidentHandler);
        addIncidentHandler(failedExternalTaskIncidentHandler);
      }

    }
    if (customIncidentHandlers != null) {
      for (IncidentHandler incidentHandler : customIncidentHandlers) {
        addIncidentHandler(incidentHandler);
      }
    }
  }

  // batch ///////////////////////////////////////////////////////////////////////

  public void initBatchHandlers() {
    if (batchHandlers == null) {
      batchHandlers = new HashMap<>();

      MigrationBatchJobHandler migrationHandler = new MigrationBatchJobHandler();
      batchHandlers.put(migrationHandler.getType(), migrationHandler);

      ModificationBatchJobHandler modificationHandler = new ModificationBatchJobHandler();
      batchHandlers.put(modificationHandler.getType(), modificationHandler);

      DeleteProcessInstancesJobHandler deleteProcessJobHandler = new DeleteProcessInstancesJobHandler();
      batchHandlers.put(deleteProcessJobHandler.getType(), deleteProcessJobHandler);

      DeleteHistoricProcessInstancesJobHandler deleteHistoricProcessInstancesJobHandler = new DeleteHistoricProcessInstancesJobHandler();
      batchHandlers.put(deleteHistoricProcessInstancesJobHandler.getType(), deleteHistoricProcessInstancesJobHandler);

      SetJobRetriesJobHandler setJobRetriesJobHandler = new SetJobRetriesJobHandler();
      batchHandlers.put(setJobRetriesJobHandler.getType(), setJobRetriesJobHandler);

      SetExternalTaskRetriesJobHandler setExternalTaskRetriesJobHandler = new SetExternalTaskRetriesJobHandler();
      batchHandlers.put(setExternalTaskRetriesJobHandler.getType(), setExternalTaskRetriesJobHandler);

      RestartProcessInstancesJobHandler restartProcessInstancesJobHandler = new RestartProcessInstancesJobHandler();
      batchHandlers.put(restartProcessInstancesJobHandler.getType(), restartProcessInstancesJobHandler);

      UpdateProcessInstancesSuspendStateJobHandler suspendProcessInstancesJobHandler = new UpdateProcessInstancesSuspendStateJobHandler();
      batchHandlers.put(suspendProcessInstancesJobHandler.getType(), suspendProcessInstancesJobHandler);

      DeleteHistoricDecisionInstancesJobHandler deleteHistoricDecisionInstancesJobHandler = new DeleteHistoricDecisionInstancesJobHandler();
      batchHandlers.put(deleteHistoricDecisionInstancesJobHandler.getType(), deleteHistoricDecisionInstancesJobHandler);

      ProcessSetRemovalTimeJobHandler processSetRemovalTimeJobHandler = new ProcessSetRemovalTimeJobHandler();
      batchHandlers.put(processSetRemovalTimeJobHandler.getType(), processSetRemovalTimeJobHandler);

      DecisionSetRemovalTimeJobHandler decisionSetRemovalTimeJobHandler = new DecisionSetRemovalTimeJobHandler();
      batchHandlers.put(decisionSetRemovalTimeJobHandler.getType(), decisionSetRemovalTimeJobHandler);

      BatchSetRemovalTimeJobHandler batchSetRemovalTimeJobHandler = new BatchSetRemovalTimeJobHandler();
      batchHandlers.put(batchSetRemovalTimeJobHandler.getType(), batchSetRemovalTimeJobHandler);

      BatchSetVariablesHandler batchSetVariablesHandler = new BatchSetVariablesHandler();
      batchHandlers.put(batchSetVariablesHandler.getType(), batchSetVariablesHandler);

      MessageCorrelationBatchJobHandler messageCorrelationJobHandler = new MessageCorrelationBatchJobHandler();
      batchHandlers.put(messageCorrelationJobHandler.getType(), messageCorrelationJobHandler);
    }

    if (customBatchJobHandlers != null) {
      for (BatchJobHandler<?> customBatchJobHandler : customBatchJobHandlers) {
        batchHandlers.put(customBatchJobHandler.getType(), customBatchJobHandler);
      }
    }

    if (removalTimeUpdateChunkSize > ProcessSetRemovalTimeJobHandler.MAX_CHUNK_SIZE || removalTimeUpdateChunkSize <= 0) {
      throw LOG.invalidPropertyValue("removalTimeUpdateChunkSize", String.valueOf(removalTimeUpdateChunkSize),
          String.format("value for chunk size should be between 1 and %s", ProcessSetRemovalTimeJobHandler.MAX_CHUNK_SIZE));
    }
  }

  // command executors ////////////////////////////////////////////////////////

  protected abstract Collection<? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequired();

  protected abstract Collection<? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequiresNew();

  protected void initCommandExecutors() {
    initActualCommandExecutor();
    initCommandInterceptorsTxRequired();
    initCommandExecutorTxRequired();
    initCommandInterceptorsTxRequiresNew();
    initCommandExecutorTxRequiresNew();
    initCommandExecutorDbSchemaOperations();
  }

  protected void initActualCommandExecutor() {
    actualCommandExecutor = new CommandExecutorImpl();
  }

  protected void initCommandInterceptorsTxRequired() {
    if (commandInterceptorsTxRequired == null) {
      if (customPreCommandInterceptorsTxRequired != null) {
        commandInterceptorsTxRequired = new ArrayList<>(customPreCommandInterceptorsTxRequired);
      } else {
        commandInterceptorsTxRequired = new ArrayList<>();
      }
      commandInterceptorsTxRequired.addAll(getDefaultCommandInterceptorsTxRequired());
      if (customPostCommandInterceptorsTxRequired != null) {
        commandInterceptorsTxRequired.addAll(customPostCommandInterceptorsTxRequired);
      }
      commandInterceptorsTxRequired.add(actualCommandExecutor);
    }
  }

  protected void initCommandInterceptorsTxRequiresNew() {
    if (commandInterceptorsTxRequiresNew == null) {
      if (customPreCommandInterceptorsTxRequiresNew != null) {
        commandInterceptorsTxRequiresNew = new ArrayList<>(customPreCommandInterceptorsTxRequiresNew);
      } else {
        commandInterceptorsTxRequiresNew = new ArrayList<>();
      }
      commandInterceptorsTxRequiresNew.addAll(getDefaultCommandInterceptorsTxRequiresNew());
      if (customPostCommandInterceptorsTxRequiresNew != null) {
        commandInterceptorsTxRequiresNew.addAll(customPostCommandInterceptorsTxRequiresNew);
      }
      commandInterceptorsTxRequiresNew.add(actualCommandExecutor);
    }
  }

  protected void initCommandExecutorTxRequired() {
    if (commandExecutorTxRequired == null) {
      commandExecutorTxRequired = initInterceptorChain(commandInterceptorsTxRequired);
    }
  }

  protected void initCommandExecutorTxRequiresNew() {
    if (commandExecutorTxRequiresNew == null) {
      commandExecutorTxRequiresNew = initInterceptorChain(commandInterceptorsTxRequiresNew);
    }
  }

  protected void initCommandExecutorDbSchemaOperations() {
    if (commandExecutorSchemaOperations == null) {
      // in default case, we use the same command executor for DB Schema Operations as for runtime operations.
      // configurations that Use JTA Transactions should override this method and provide a custom command executor
      // that uses NON-JTA Transactions.
      commandExecutorSchemaOperations = commandExecutorTxRequired;
    }
  }

  protected CommandInterceptor initInterceptorChain(List<CommandInterceptor> chain) {
    if (chain == null || chain.isEmpty()) {
      throw new ProcessEngineException("invalid command interceptor chain configuration: " + chain);
    }
    for (int i = 0; i < chain.size() - 1; i++) {
      chain.get(i).setNext(chain.get(i + 1));
    }
    return chain.get(0);
  }

  // services /////////////////////////////////////////////////////////////////

  protected void initServices() {
    initService(repositoryService);
    initService(runtimeService);
    initService(historyService);
    initService(identityService);
    initService(taskService);
    initService(formService);
    initService(managementService);
    initService(authorizationService);
    initService(caseService);
    initService(filterService);
    initService(externalTaskService);
    initService(decisionService);
    initService(optimizeService);
  }

  protected void initService(Object service) {
    if (service instanceof ServiceImpl serviceImpl) {
      serviceImpl.setCommandExecutor(commandExecutorTxRequired);
    }
    if (service instanceof RepositoryServiceImpl repositorySrvc) {
      repositorySrvc.setDeploymentCharset(getDefaultCharset());
    }
  }

  // DataSource ///////////////////////////////////////////////////////////////

  protected void initDataSource() {
    if (dataSource == null) {
      if (dataSourceJndiName != null) {
        try {
          dataSource = (DataSource) new InitialContext().lookup(dataSourceJndiName);
        } catch (Exception e) {
          throw new ProcessEngineException("couldn't lookup datasource from " + dataSourceJndiName + ": " + e.getMessage(), e);
        }

      } else if (jdbcUrl != null) {
        if ((jdbcDriver == null) || (jdbcUrl == null) || (jdbcUsername == null)) {
          throw new ProcessEngineException("DataSource or JDBC properties have to be specified in a process engine configuration");
        }

        PooledDataSource pooledDataSource =
            new PooledDataSource(ReflectUtil.getClassLoader(), jdbcDriver, jdbcUrl, jdbcUsername, jdbcPassword);

        if (jdbcMaxActiveConnections > 0) {
          pooledDataSource.setPoolMaximumActiveConnections(jdbcMaxActiveConnections);
        }
        if (jdbcMaxIdleConnections > 0) {
          pooledDataSource.setPoolMaximumIdleConnections(jdbcMaxIdleConnections);
        }
        if (jdbcMaxCheckoutTime > 0) {
          pooledDataSource.setPoolMaximumCheckoutTime(jdbcMaxCheckoutTime);
        }
        if (jdbcMaxWaitTime > 0) {
          pooledDataSource.setPoolTimeToWait(jdbcMaxWaitTime);
        }
        if (jdbcPingEnabled) {
          pooledDataSource.setPoolPingEnabled(true);
          if (jdbcPingQuery != null) {
            pooledDataSource.setPoolPingQuery(jdbcPingQuery);
          }
          pooledDataSource.setPoolPingConnectionsNotUsedFor(jdbcPingConnectionNotUsedFor);
        }
        dataSource = pooledDataSource;
      }

      if (dataSource instanceof PooledDataSource pooledDataSource) {
        // ACT-233: connection pool of Ibatis is not properely initialized if this is not called!
        pooledDataSource.forceCloseAll();
      }
    }

    if (dataSource != null) {
      checkTransactionIsolationLevel();
    }

    if (databaseType == null) {
      initDatabaseType();
    }
  }

  protected void checkTransactionIsolationLevel() {
    Integer currentIsolationLevel = getCurrentTransactionIsolationLevel();
    if (currentIsolationLevel != null && currentIsolationLevel != Connection.TRANSACTION_READ_COMMITTED) {
      String isolationLevelName = ISOLATION_LEVEL_CONSTANT_NAMES.get(currentIsolationLevel);
      if (skipIsolationLevelCheck) {
        LOG.logSkippedIsolationLevelCheck(isolationLevelName);
      } else {
        throw LOG.invalidTransactionIsolationLevel(isolationLevelName);
      }
    }
  }

  protected Integer getCurrentTransactionIsolationLevel() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.getTransactionIsolation();
    } catch (SQLException e) {
      return null;
    }
  }

  protected static Properties databaseTypeMappings = getDefaultDatabaseTypeMappings();
  protected static final String MY_SQL_PRODUCT_NAME = "MySQL";
  protected static final String MARIA_DB_PRODUCT_NAME = "MariaDB";
  protected static final String MARIA_DB_IDENTIFIER = "mariadb";
  protected static final String POSTGRES_DB_PRODUCT_NAME = "PostgreSQL";

  protected static Properties getDefaultDatabaseTypeMappings() {
    Properties databaseTypeMappings = new Properties();
    databaseTypeMappings.setProperty("H2", "h2");
    databaseTypeMappings.setProperty(MY_SQL_PRODUCT_NAME, "mysql");
    databaseTypeMappings.setProperty(MARIA_DB_PRODUCT_NAME, MARIA_DB_IDENTIFIER);
    databaseTypeMappings.setProperty("Oracle", "oracle");
    databaseTypeMappings.setProperty(POSTGRES_DB_PRODUCT_NAME, "postgres");
    databaseTypeMappings.setProperty("Microsoft SQL Server", "mssql");
    databaseTypeMappings.setProperty("DB2", "db2");
    databaseTypeMappings.setProperty("DB2", "db2");
    databaseTypeMappings.setProperty("DB2/NT", "db2");
    databaseTypeMappings.setProperty("DB2/NT64", "db2");
    databaseTypeMappings.setProperty("DB2 UDP", "db2");
    databaseTypeMappings.setProperty("DB2/LINUX", "db2");
    databaseTypeMappings.setProperty("DB2/LINUX390", "db2");
    databaseTypeMappings.setProperty("DB2/LINUXX8664", "db2");
    databaseTypeMappings.setProperty("DB2/LINUXZ64", "db2");
    databaseTypeMappings.setProperty("DB2/400 SQL", "db2");
    databaseTypeMappings.setProperty("DB2/6000", "db2");
    databaseTypeMappings.setProperty("DB2 UDB iSeries", "db2");
    databaseTypeMappings.setProperty("DB2/AIX64", "db2");
    databaseTypeMappings.setProperty("DB2/HPUX", "db2");
    databaseTypeMappings.setProperty("DB2/HP64", "db2");
    databaseTypeMappings.setProperty("DB2/SUN", "db2");
    databaseTypeMappings.setProperty("DB2/SUN64", "db2");
    databaseTypeMappings.setProperty("DB2/PTX", "db2");
    databaseTypeMappings.setProperty("DB2/2", "db2");
    return databaseTypeMappings;
  }

  public void initDatabaseType() {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      DatabaseMetaData databaseMetaData = connection.getMetaData();
      String databaseProductName = databaseMetaData.getDatabaseProductName();
      if (MY_SQL_PRODUCT_NAME.equals(databaseProductName)) {
        databaseProductName = checkForMariaDb(databaseMetaData, databaseProductName);
      }
      if (POSTGRES_DB_PRODUCT_NAME.equals(databaseProductName)) {
        databaseProductName = POSTGRES_DB_PRODUCT_NAME;
      }
      LOG.debugDatabaseproductName(databaseProductName);
      databaseType = databaseTypeMappings.getProperty(databaseProductName);
      ensureNotNull("couldn't deduct database type from database product name '" + databaseProductName + "'", "databaseType", databaseType);
      LOG.debugDatabaseType(databaseType);

      initDatabaseVendorAndVersion(databaseMetaData);

    } catch (SQLException e) {
      throw LOG.databaseConnectionAccessException(e);
    } finally {
      try {
        if (connection != null) {
          connection.close();
        }
      } catch (SQLException e) {
        LOG.databaseConnectionCloseException(e);
      }
    }
  }

  /**
   * The product name of mariadb is still 'MySQL'. This method
   * tries if it can find some evidence for mariadb. If it is successful
   * it will return "MariaDB", otherwise the provided database name.
   */
  protected String checkForMariaDb(DatabaseMetaData databaseMetaData, String databaseName) {
    try {
      String databaseProductVersion = databaseMetaData.getDatabaseProductVersion();
      if (databaseProductVersion != null && databaseProductVersion.toLowerCase().contains(MARIA_DB_IDENTIFIER)) {
        return MARIA_DB_PRODUCT_NAME;
      }
    } catch (SQLException ignore) {
    }

    try {
      String driverName = databaseMetaData.getDriverName();
      if (driverName != null && driverName.toLowerCase().contains(MARIA_DB_IDENTIFIER)) {
        return MARIA_DB_PRODUCT_NAME;
      }
    } catch (SQLException ignore) {
    }

    String metaDataClassName = databaseMetaData.getClass().getName();
    if (metaDataClassName != null && metaDataClassName.toLowerCase().contains(MARIA_DB_IDENTIFIER)) {
      return MARIA_DB_PRODUCT_NAME;
    }

    return databaseName;
  }

  protected void initDatabaseVendorAndVersion(DatabaseMetaData databaseMetaData) throws SQLException {
    databaseVendor = databaseMetaData.getDatabaseProductName();
    databaseVersion = databaseMetaData.getDatabaseProductVersion();
  }

  // myBatis SqlSessionFactory ////////////////////////////////////////////////

  protected void initTransactionFactory() {
    if (transactionFactory == null) {
      if (transactionsExternallyManaged) {
        transactionFactory = new ManagedTransactionFactory();
      } else {
        transactionFactory = new JdbcTransactionFactory();
      }
    }
  }

  protected void initSqlSessionFactory() {

    // to protect access to cachedSqlSessionFactory see CAM-6682
    synchronized (ProcessEngineConfigurationImpl.class) {

      if (isUseSharedSqlSessionFactory) {
        sqlSessionFactory = cachedSqlSessionFactory;
      }

      if (sqlSessionFactory == null) {
        try (InputStream inputStream = getMyBatisXmlConfigurationSteam()) {

          // update the jdbc parameters to the configured ones...
          Environment environment = new Environment("default", transactionFactory, dataSource);
          Reader reader = new InputStreamReader(inputStream);

          Properties properties = new Properties();

          if (isUseSharedSqlSessionFactory) {
            properties.put("prefix", "${@org.operaton.bpm.engine.impl.context.Context@getProcessEngineConfiguration().databaseTablePrefix}");
          } else {
            properties.put("prefix", databaseTablePrefix);
          }

          initSqlSessionFactoryProperties(properties, databaseTablePrefix, databaseType);

          XMLConfigBuilder parser = new XMLConfigBuilder(reader, "", properties);
          Configuration configuration = parser.getConfiguration();
          configuration.setEnvironment(environment);
          configuration = parser.parse();

          configuration.setDefaultStatementTimeout(jdbcStatementTimeout);

          if (isJdbcBatchProcessing()) {
            configuration.setDefaultExecutorType(ExecutorType.BATCH);
          }

          sqlSessionFactory = new DefaultSqlSessionFactory(configuration);

          if (isUseSharedSqlSessionFactory) {
            cachedSqlSessionFactory = sqlSessionFactory;
          }


        } catch (Exception e) {
          throw new ProcessEngineException("Error while building ibatis SqlSessionFactory: " + e.getMessage(), e);
        }
      }
    }
  }

  @SuppressWarnings("unused")
  public static void initSqlSessionFactoryProperties(Properties properties, String databaseTablePrefix, String databaseType) {

    if (databaseType != null) {
      properties.put("limitBefore", DbSqlSessionFactory.databaseSpecificLimitBeforeStatements.get(databaseType));
      properties.put("limitAfter", DbSqlSessionFactory.databaseSpecificLimitAfterStatements.get(databaseType));
      properties.put("limitBeforeWithoutOffset", DbSqlSessionFactory.databaseSpecificLimitBeforeWithoutOffsetStatements.get(databaseType));
      properties.put("limitAfterWithoutOffset", DbSqlSessionFactory.databaseSpecificLimitAfterWithoutOffsetStatements.get(databaseType));

      properties.put("optimizeLimitBeforeWithoutOffset", DbSqlSessionFactory.optimizeDatabaseSpecificLimitBeforeWithoutOffsetStatements.get(databaseType));
      properties.put("optimizeLimitAfterWithoutOffset", DbSqlSessionFactory.optimizeDatabaseSpecificLimitAfterWithoutOffsetStatements.get(databaseType));
      properties.put("innerLimitAfter", DbSqlSessionFactory.databaseSpecificInnerLimitAfterStatements.get(databaseType));
      properties.put("limitBetween", DbSqlSessionFactory.databaseSpecificLimitBetweenStatements.get(databaseType));
      properties.put("limitBetweenFilter", DbSqlSessionFactory.databaseSpecificLimitBetweenFilterStatements.get(databaseType));
      properties.put("limitBetweenAcquisition", DbSqlSessionFactory.databaseSpecificLimitBetweenAcquisitionStatements.get(databaseType));
      properties.put("orderBy", DbSqlSessionFactory.databaseSpecificOrderByStatements.get(databaseType));
      properties.put("limitBeforeNativeQuery", DbSqlSessionFactory.databaseSpecificLimitBeforeNativeQueryStatements.get(databaseType));
      properties.put("distinct", DbSqlSessionFactory.databaseSpecificDistinct.get(databaseType));
      properties.put("numericCast", DbSqlSessionFactory.databaseSpecificNumericCast.get(databaseType));

      properties.put("limitBeforeInUpdate", DbSqlSessionFactory.databaseSpecificLimitBeforeInUpdate.get(databaseType));
      properties.put("limitAfterInUpdate", DbSqlSessionFactory.databaseSpecificLimitAfterInUpdate.get(databaseType));

      properties.put("countDistinctBeforeStart", DbSqlSessionFactory.databaseSpecificCountDistinctBeforeStart.get(databaseType));
      properties.put("countDistinctBeforeEnd", DbSqlSessionFactory.databaseSpecificCountDistinctBeforeEnd.get(databaseType));
      properties.put("countDistinctAfterEnd", DbSqlSessionFactory.databaseSpecificCountDistinctAfterEnd.get(databaseType));

      properties.put("escapeChar", DbSqlSessionFactory.databaseSpecificEscapeChar.get(databaseType));

      properties.put("bitand1", DbSqlSessionFactory.databaseSpecificBitAnd1.get(databaseType));
      properties.put("bitand2", DbSqlSessionFactory.databaseSpecificBitAnd2.get(databaseType));
      properties.put("bitand3", DbSqlSessionFactory.databaseSpecificBitAnd3.get(databaseType));

      properties.put("datepart1", DbSqlSessionFactory.databaseSpecificDatepart1.get(databaseType));
      properties.put("datepart2", DbSqlSessionFactory.databaseSpecificDatepart2.get(databaseType));
      properties.put("datepart3", DbSqlSessionFactory.databaseSpecificDatepart3.get(databaseType));

      properties.put("trueConstant", DbSqlSessionFactory.databaseSpecificTrueConstant.get(databaseType));
      properties.put("falseConstant", DbSqlSessionFactory.databaseSpecificFalseConstant.get(databaseType));

      properties.put("dbSpecificDummyTable", DbSqlSessionFactory.databaseSpecificDummyTable.get(databaseType));
      properties.put("dbSpecificIfNullFunction", DbSqlSessionFactory.databaseSpecificIfNull.get(databaseType));

      properties.put("dayComparator", DbSqlSessionFactory.databaseSpecificDaysComparator.get(databaseType));

      properties.put("collationForCaseSensitivity", DbSqlSessionFactory.databaseSpecificCollationForCaseSensitivity.get(databaseType));

      properties.put("authJoinStart", DbSqlSessionFactory.databaseSpecificAuthJoinStart.get(databaseType));
      properties.put("authJoinEnd", DbSqlSessionFactory.databaseSpecificAuthJoinEnd.get(databaseType));
      properties.put("authJoinSeparator", DbSqlSessionFactory.databaseSpecificAuthJoinSeparator.get(databaseType));

      properties.put("authJoin1Start", DbSqlSessionFactory.databaseSpecificAuth1JoinStart.get(databaseType));
      properties.put("authJoin1End", DbSqlSessionFactory.databaseSpecificAuth1JoinEnd.get(databaseType));
      properties.put("authJoin1Separator", DbSqlSessionFactory.databaseSpecificAuth1JoinSeparator.get(databaseType));

      properties.put("extractTimeUnitFromDate", DbSqlSessionFactory.databaseSpecificExtractTimeUnitFromDate.get(databaseType));
      properties.put("authCheckMethodSuffix", DbSqlSessionFactory.databaseSpecificAuthCheckMethodSuffix.getOrDefault(databaseType, ""));

      Map<String, String> constants = DbSqlSessionFactory.dbSpecificConstants.get(databaseType);
      for (Entry<String, String> entry : constants.entrySet()) {
        properties.put(entry.getKey(), entry.getValue());
      }
    }
  }

  protected InputStream getMyBatisXmlConfigurationSteam() {
    return ReflectUtil.getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
  }

  // session factories ////////////////////////////////////////////////////////

  protected void initIdentityProviderSessionFactory() {
    if (identityProviderSessionFactory == null) {
      identityProviderSessionFactory = new GenericManagerFactory(DbIdentityServiceProvider.class);
    }
  }

  protected void initSessionFactories() {
    if (sessionFactories == null) {
      sessionFactories = new HashMap<>();

      initPersistenceProviders();

      addSessionFactory(new DbEntityManagerFactory(idGenerator));

      addSessionFactory(new GenericManagerFactory(AttachmentManager.class));
      addSessionFactory(new GenericManagerFactory(CommentManager.class));
      addSessionFactory(new GenericManagerFactory(DeploymentManager.class));
      addSessionFactory(new GenericManagerFactory(ExecutionManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricActivityInstanceManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricCaseActivityInstanceManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricStatisticsManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricDetailManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricProcessInstanceManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricCaseInstanceManager.class));
      addSessionFactory(new GenericManagerFactory(UserOperationLogManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricTaskInstanceManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricVariableInstanceManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricIncidentManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricIdentityLinkLogManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricJobLogManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricExternalTaskLogManager.class));
      addSessionFactory(new GenericManagerFactory(IdentityInfoManager.class));
      addSessionFactory(new GenericManagerFactory(IdentityLinkManager.class));
      addSessionFactory(new GenericManagerFactory(JobManager.class));
      addSessionFactory(new GenericManagerFactory(JobDefinitionManager.class));
      addSessionFactory(new GenericManagerFactory(ProcessDefinitionManager.class));
      addSessionFactory(new GenericManagerFactory(PropertyManager.class));
      addSessionFactory(new GenericManagerFactory(ResourceManager.class));
      addSessionFactory(new GenericManagerFactory(ByteArrayManager.class));
      addSessionFactory(new GenericManagerFactory(TableDataManager.class));
      addSessionFactory(new GenericManagerFactory(TaskManager.class));
      addSessionFactory(new GenericManagerFactory(TaskReportManager.class));
      addSessionFactory(new GenericManagerFactory(VariableInstanceManager.class));
      addSessionFactory(new GenericManagerFactory(EventSubscriptionManager.class));
      addSessionFactory(new GenericManagerFactory(StatisticsManager.class));
      addSessionFactory(new GenericManagerFactory(IncidentManager.class));
      addSessionFactory(new GenericManagerFactory(AuthorizationManager.class));
      addSessionFactory(new GenericManagerFactory(FilterManager.class));
      addSessionFactory(new GenericManagerFactory(MeterLogManager.class));
      addSessionFactory(new GenericManagerFactory(ExternalTaskManager.class));
      addSessionFactory(new GenericManagerFactory(ReportManager.class));
      addSessionFactory(new GenericManagerFactory(BatchManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricBatchManager.class));
      addSessionFactory(new GenericManagerFactory(TenantManager.class));
      addSessionFactory(new GenericManagerFactory(SchemaLogManager.class));

      addSessionFactory(new GenericManagerFactory(CaseDefinitionManager.class));
      addSessionFactory(new GenericManagerFactory(CaseExecutionManager.class));
      addSessionFactory(new GenericManagerFactory(CaseSentryPartManager.class));

      addSessionFactory(new GenericManagerFactory(DecisionDefinitionManager.class));
      addSessionFactory(new GenericManagerFactory(DecisionRequirementsDefinitionManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricDecisionInstanceManager.class));

      addSessionFactory(new GenericManagerFactory(OperatonFormDefinitionManager.class));

      addSessionFactory(new GenericManagerFactory(OptimizeManager.class));

      sessionFactories.put(ReadOnlyIdentityProvider.class, identityProviderSessionFactory);

      // check whether identityProviderSessionFactory implements WritableIdentityProvider
      Class<?> identityProviderType = identityProviderSessionFactory.getSessionType();
      if (WritableIdentityProvider.class.isAssignableFrom(identityProviderType)) {
        sessionFactories.put(WritableIdentityProvider.class, identityProviderSessionFactory);
      }

    }
    if (customSessionFactories != null) {
      for (SessionFactory sessionFactory : customSessionFactories) {
        addSessionFactory(sessionFactory);
      }
    }
  }

  protected void initPersistenceProviders() {
    ensurePrefixAndSchemaFitToegether(databaseTablePrefix, databaseSchema);
    dbSqlSessionFactory = new DbSqlSessionFactory(jdbcBatchProcessing);
    dbSqlSessionFactory.setDatabaseType(databaseType);
    dbSqlSessionFactory.setIdGenerator(idGenerator);
    dbSqlSessionFactory.setSqlSessionFactory(sqlSessionFactory);
    dbSqlSessionFactory.setDbIdentityUsed(isDbIdentityUsed);
    dbSqlSessionFactory.setDbHistoryUsed(isDbHistoryUsed);
    dbSqlSessionFactory.setCmmnEnabled(cmmnEnabled);
    dbSqlSessionFactory.setDmnEnabled(dmnEnabled);
    dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);

    //hack for the case when schema is defined via databaseTablePrefix parameter and not via databaseSchema parameter
    if (databaseTablePrefix != null && databaseSchema == null && databaseTablePrefix.contains(".")) {
      databaseSchema = databaseTablePrefix.split("\\.")[0];
    }
    dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
    addSessionFactory(dbSqlSessionFactory);
    addSessionFactory(new DbSqlPersistenceProviderFactory());
  }

  protected void initMigration() {
    initMigrationInstructionValidators();
    initMigrationActivityMatcher();
    initMigrationInstructionGenerator();
    initMigratingActivityInstanceValidators();
    initMigratingTransitionInstanceValidators();
    initMigratingCompensationInstanceValidators();
  }

  protected void initMigrationActivityMatcher() {
    if (migrationActivityMatcher == null) {
      migrationActivityMatcher = new DefaultMigrationActivityMatcher();
    }
  }

  protected void initMigrationInstructionGenerator() {
    if (migrationInstructionGenerator == null) {
      migrationInstructionGenerator = new DefaultMigrationInstructionGenerator(migrationActivityMatcher);
    }

    List<MigrationActivityValidator> migrationActivityValidators = new ArrayList<>();
    if (customPreMigrationActivityValidators != null) {
      migrationActivityValidators.addAll(customPreMigrationActivityValidators);
    }
    migrationActivityValidators.addAll(getDefaultMigrationActivityValidators());
    if (customPostMigrationActivityValidators != null) {
      migrationActivityValidators.addAll(customPostMigrationActivityValidators);
    }
    migrationInstructionGenerator = migrationInstructionGenerator
        .migrationActivityValidators(migrationActivityValidators)
        .migrationInstructionValidators(migrationInstructionValidators);
  }

  protected void initMigrationInstructionValidators() {
    if (migrationInstructionValidators == null) {
      migrationInstructionValidators = new ArrayList<>();
      if (customPreMigrationInstructionValidators != null) {
        migrationInstructionValidators.addAll(customPreMigrationInstructionValidators);
      }
      migrationInstructionValidators.addAll(getDefaultMigrationInstructionValidators());
      if (customPostMigrationInstructionValidators != null) {
        migrationInstructionValidators.addAll(customPostMigrationInstructionValidators);
      }
    }
  }

  protected void initMigratingActivityInstanceValidators() {
    if (migratingActivityInstanceValidators == null) {
      migratingActivityInstanceValidators = new ArrayList<>();
      if (customPreMigratingActivityInstanceValidators != null) {
        migratingActivityInstanceValidators.addAll(customPreMigratingActivityInstanceValidators);
      }
      migratingActivityInstanceValidators.addAll(getDefaultMigratingActivityInstanceValidators());
      if (customPostMigratingActivityInstanceValidators != null) {
        migratingActivityInstanceValidators.addAll(customPostMigratingActivityInstanceValidators);
      }

    }
  }

  protected void initMigratingTransitionInstanceValidators() {
    if (migratingTransitionInstanceValidators == null) {
      migratingTransitionInstanceValidators = new ArrayList<>();
      migratingTransitionInstanceValidators.addAll(getDefaultMigratingTransitionInstanceValidators());
    }
  }

  protected void initMigratingCompensationInstanceValidators() {
    if (migratingCompensationInstanceValidators == null) {
      migratingCompensationInstanceValidators = new ArrayList<>();

      migratingCompensationInstanceValidators.add(new NoUnmappedLeafInstanceValidator());
      migratingCompensationInstanceValidators.add(new NoUnmappedCompensationStartEventValidator());
    }
  }


  /**
   * When providing a schema and a prefix  the prefix has to be the schema ending with a dot.
   */
  protected void ensurePrefixAndSchemaFitToegether(String prefix, String schema) {
    if (schema == null) {
      return;
    }
    if (prefix == null || !prefix.startsWith(schema + ".")) {
      throw new ProcessEngineException("When setting a schema the prefix has to be schema + '.'. Received schema: " + schema + " prefix: " + prefix);
    }
  }

  protected void addSessionFactory(SessionFactory sessionFactory) {
    sessionFactories.put(sessionFactory.getSessionType(), sessionFactory);
  }

  // deployers ////////////////////////////////////////////////////////////////

  protected void initDeployers() {
    if (this.deployers == null) {
      this.deployers = new ArrayList<>();
      if (customPreDeployers != null) {
        this.deployers.addAll(customPreDeployers);
      }
      this.deployers.addAll(getDefaultDeployers());
      if (customPostDeployers != null) {
        this.deployers.addAll(customPostDeployers);
      }
    }
    if (deploymentCache == null) {
      List<Deployer> deployerList = new ArrayList<>();
      if (customPreDeployers != null) {
        deployerList.addAll(customPreDeployers);
      }
      deployerList.addAll(getDefaultDeployers());
      if (customPostDeployers != null) {
        deployerList.addAll(customPostDeployers);
      }

      initCacheFactory();
      deploymentCache = new DeploymentCache(cacheFactory, cacheCapacity);
      deploymentCache.setDeployers(deployerList);
    }
  }

  protected Collection<? extends Deployer> getDefaultDeployers() {
    List<Deployer> defaultDeployers = new ArrayList<>();

    BpmnDeployer bpmnDeployer = getBpmnDeployer();
    defaultDeployers.add(bpmnDeployer);

    defaultDeployers.add(getOperatonFormDeployer());

    if (isCmmnEnabled()) {
      CmmnDeployer cmmnDeployer = getCmmnDeployer();
      defaultDeployers.add(cmmnDeployer);
    }

    if (isDmnEnabled()) {
      DecisionRequirementsDefinitionDeployer decisionRequirementsDefinitionDeployer = getDecisionRequirementsDefinitionDeployer();
      DecisionDefinitionDeployer decisionDefinitionDeployer = getDecisionDefinitionDeployer();
      // the DecisionRequirementsDefinition cacheDeployer must be before the DecisionDefinitionDeployer
      defaultDeployers.add(decisionRequirementsDefinitionDeployer);
      defaultDeployers.add(decisionDefinitionDeployer);
    }

    return defaultDeployers;
  }

  protected BpmnDeployer getBpmnDeployer() {
    BpmnDeployer bpmnDeployer = new BpmnDeployer();
    bpmnDeployer.setExpressionManager(expressionManager);
    bpmnDeployer.setIdGenerator(idGenerator);

    if (bpmnParseFactory == null) {
      bpmnParseFactory = new DefaultBpmnParseFactory();
    }

    BpmnParser bpmnParser = new BpmnParser(expressionManager, bpmnParseFactory);

    if (preParseListeners != null) {
      bpmnParser.getParseListeners().addAll(preParseListeners);
    }
    bpmnParser.getParseListeners().addAll(getDefaultBPMNParseListeners());
    if (postParseListeners != null) {
      bpmnParser.getParseListeners().addAll(postParseListeners);
    }

    bpmnDeployer.setBpmnParser(bpmnParser);

    return bpmnDeployer;
  }

  protected List<BpmnParseListener> getDefaultBPMNParseListeners() {
    List<BpmnParseListener> defaultListeners = new ArrayList<>();
    if (!HistoryLevel.HISTORY_LEVEL_NONE.equals(historyLevel)) {
      defaultListeners.add(new HistoryParseListener(historyEventProducer));
    }
    if (isMetricsEnabled) {
      defaultListeners.add(new MetricsBpmnParseListener());
    }
    return defaultListeners;
  }

  protected OperatonFormDefinitionDeployer getOperatonFormDeployer() {
    OperatonFormDefinitionDeployer deployer = new OperatonFormDefinitionDeployer();
    deployer.setIdGenerator(idGenerator);
    return deployer;
  }

  protected CmmnDeployer getCmmnDeployer() {
    CmmnDeployer cmmnDeployer = new CmmnDeployer();

    cmmnDeployer.setIdGenerator(idGenerator);

    if (cmmnTransformFactory == null) {
      cmmnTransformFactory = new DefaultCmmnTransformFactory();
    }

    if (cmmnElementHandlerRegistry == null) {
      cmmnElementHandlerRegistry = new DefaultCmmnElementHandlerRegistry();
    }

    CmmnTransformer cmmnTransformer = new CmmnTransformer(expressionManager, cmmnElementHandlerRegistry, cmmnTransformFactory);

    if (customPreCmmnTransformListeners != null) {
      cmmnTransformer.getTransformListeners().addAll(customPreCmmnTransformListeners);
    }
    cmmnTransformer.getTransformListeners().addAll(getDefaultCmmnTransformListeners());
    if (customPostCmmnTransformListeners != null) {
      cmmnTransformer.getTransformListeners().addAll(customPostCmmnTransformListeners);
    }

    cmmnDeployer.setTransformer(cmmnTransformer);

    return cmmnDeployer;
  }

  protected List<CmmnTransformListener> getDefaultCmmnTransformListeners() {
    List<CmmnTransformListener> defaultListener = new ArrayList<>();
    if (!HistoryLevel.HISTORY_LEVEL_NONE.equals(historyLevel)) {
      defaultListener.add(new CmmnHistoryTransformListener(cmmnHistoryEventProducer));
    }
    if (isMetricsEnabled) {
      defaultListener.add(new MetricsCmmnTransformListener());
    }
    return defaultListener;
  }

  protected DecisionDefinitionDeployer getDecisionDefinitionDeployer() {
    DecisionDefinitionDeployer decisionDefinitionDeployer = new DecisionDefinitionDeployer();
    decisionDefinitionDeployer.setIdGenerator(idGenerator);
    decisionDefinitionDeployer.setTransformer(dmnEngineConfiguration.getTransformer());
    return decisionDefinitionDeployer;
  }

  protected DecisionRequirementsDefinitionDeployer getDecisionRequirementsDefinitionDeployer() {
    DecisionRequirementsDefinitionDeployer drdDeployer = new DecisionRequirementsDefinitionDeployer();
    drdDeployer.setIdGenerator(idGenerator);
    drdDeployer.setTransformer(dmnEngineConfiguration.getTransformer());
    return drdDeployer;
  }

  public DmnEngine getDmnEngine() {
    return dmnEngine;
  }

  public void setDmnEngine(DmnEngine dmnEngine) {
    this.dmnEngine = dmnEngine;
  }

  public DefaultDmnEngineConfiguration getDmnEngineConfiguration() {
    return dmnEngineConfiguration;
  }

  public void setDmnEngineConfiguration(DefaultDmnEngineConfiguration dmnEngineConfiguration) {
    this.dmnEngineConfiguration = dmnEngineConfiguration;
  }

  // job executor /////////////////////////////////////////////////////////////

  protected void initJobExecutor() {
    if (jobExecutor == null) {
      jobExecutor = new DefaultJobExecutor();
    }

    jobHandlers = new HashMap<>();
    TimerExecuteNestedActivityJobHandler timerExecuteNestedActivityJobHandler = new TimerExecuteNestedActivityJobHandler();
    jobHandlers.put(timerExecuteNestedActivityJobHandler.getType(), timerExecuteNestedActivityJobHandler);

    TimerCatchIntermediateEventJobHandler timerCatchIntermediateEvent = new TimerCatchIntermediateEventJobHandler();
    jobHandlers.put(timerCatchIntermediateEvent.getType(), timerCatchIntermediateEvent);

    TimerStartEventJobHandler timerStartEvent = new TimerStartEventJobHandler();
    jobHandlers.put(timerStartEvent.getType(), timerStartEvent);

    TimerStartEventSubprocessJobHandler timerStartEventSubprocess = new TimerStartEventSubprocessJobHandler();
    jobHandlers.put(timerStartEventSubprocess.getType(), timerStartEventSubprocess);

    AsyncContinuationJobHandler asyncContinuationJobHandler = new AsyncContinuationJobHandler();
    jobHandlers.put(asyncContinuationJobHandler.getType(), asyncContinuationJobHandler);

    ProcessEventJobHandler processEventJobHandler = new ProcessEventJobHandler();
    jobHandlers.put(processEventJobHandler.getType(), processEventJobHandler);

    TimerSuspendProcessDefinitionHandler suspendProcessDefinitionHandler = new TimerSuspendProcessDefinitionHandler();
    jobHandlers.put(suspendProcessDefinitionHandler.getType(), suspendProcessDefinitionHandler);

    TimerActivateProcessDefinitionHandler activateProcessDefinitionHandler = new TimerActivateProcessDefinitionHandler();
    jobHandlers.put(activateProcessDefinitionHandler.getType(), activateProcessDefinitionHandler);

    TimerSuspendJobDefinitionHandler suspendJobDefinitionHandler = new TimerSuspendJobDefinitionHandler();
    jobHandlers.put(suspendJobDefinitionHandler.getType(), suspendJobDefinitionHandler);

    TimerActivateJobDefinitionHandler activateJobDefinitionHandler = new TimerActivateJobDefinitionHandler();
    jobHandlers.put(activateJobDefinitionHandler.getType(), activateJobDefinitionHandler);

    TimerTaskListenerJobHandler taskListenerJobHandler = new TimerTaskListenerJobHandler();
    jobHandlers.put(taskListenerJobHandler.getType(), taskListenerJobHandler);

    BatchSeedJobHandler batchSeedJobHandler = new BatchSeedJobHandler();
    jobHandlers.put(batchSeedJobHandler.getType(), batchSeedJobHandler);

    BatchMonitorJobHandler batchMonitorJobHandler = new BatchMonitorJobHandler();
    jobHandlers.put(batchMonitorJobHandler.getType(), batchMonitorJobHandler);

    HistoryCleanupJobHandler historyCleanupJobHandler = new HistoryCleanupJobHandler();
    jobHandlers.put(historyCleanupJobHandler.getType(), historyCleanupJobHandler);

    for (JobHandler batchHandler : batchHandlers.values()) {
      jobHandlers.put(batchHandler.getType(), batchHandler);
    }

    // if we have custom job handlers, register them
    if (getCustomJobHandlers() != null) {
      for (JobHandler customJobHandler : getCustomJobHandlers()) {
        jobHandlers.put(customJobHandler.getType(), customJobHandler);
      }
    }

    jobExecutor.setAutoActivate(jobExecutorActivate);

    if (jobExecutor.getRejectedJobsHandler() == null) {
      if (customRejectedJobsHandler != null) {
        jobExecutor.setRejectedJobsHandler(customRejectedJobsHandler);
      } else {
        jobExecutor.setRejectedJobsHandler(new NotifyAcquisitionRejectedJobsHandler());
      }
    }

    // verify job executor priority range is configured correctly
    if (jobExecutorPriorityRangeMin > jobExecutorPriorityRangeMax) {
      throw ProcessEngineLogger.JOB_EXECUTOR_LOGGER.jobExecutorPriorityRangeException(
          "jobExecutorPriorityRangeMin can not be greater than jobExecutorPriorityRangeMax");
    }

    if (jobExecutorPriorityRangeMin > historyCleanupJobPriority || jobExecutorPriorityRangeMax < historyCleanupJobPriority) {
      ProcessEngineLogger.JOB_EXECUTOR_LOGGER.infoJobExecutorDoesNotHandleHistoryCleanupJobs(this);
    }
    if (jobExecutorPriorityRangeMin > batchJobPriority || jobExecutorPriorityRangeMax < batchJobPriority) {
      ProcessEngineLogger.JOB_EXECUTOR_LOGGER.infoJobExecutorDoesNotHandleBatchJobs(this);
    }
  }

  protected void initJobProvider() {
    if (producePrioritizedJobs && jobPriorityProvider == null) {
      jobPriorityProvider = new DefaultJobPriorityProvider();
    }
  }

  //external task /////////////////////////////////////////////////////////////

  protected void initExternalTaskPriorityProvider() {
    if (producePrioritizedExternalTasks && externalTaskPriorityProvider == null) {
      externalTaskPriorityProvider = new DefaultExternalTaskPriorityProvider();
    }
  }

  // history //////////////////////////////////////////////////////////////////

  public void initHistoryLevel() {
    if (historyLevel != null) {
      setHistory(historyLevel.getName());
    }

    if (historyLevels == null) {
      historyLevels = new ArrayList<>();
      historyLevels.add(HistoryLevel.HISTORY_LEVEL_NONE);
      historyLevels.add(HistoryLevel.HISTORY_LEVEL_ACTIVITY);
      historyLevels.add(HistoryLevel.HISTORY_LEVEL_AUDIT);
      historyLevels.add(HistoryLevel.HISTORY_LEVEL_FULL);
    }

    if (customHistoryLevels != null) {
      historyLevels.addAll(customHistoryLevels);
    }

    if (HISTORY_VARIABLE.equalsIgnoreCase(history)) {
      historyLevel = HistoryLevel.HISTORY_LEVEL_ACTIVITY;
      LOG.usingDeprecatedHistoryLevelVariable();
    } else {
      for (HistoryLevel historyLvl : historyLevels) {
        if (historyLvl.getName().equalsIgnoreCase(history)) {
          this.historyLevel = historyLvl;
        }
      }
    }

    // do allow null for history level in case of "auto"
    if (historyLevel == null && !ProcessEngineConfiguration.HISTORY_AUTO.equalsIgnoreCase(history)) {
      throw new ProcessEngineException("invalid history level: " + history);
    }
  }

  // id generator /////////////////////////////////////////////////////////////

  protected void initIdGenerator() {
    if (idGenerator == null) {
      CommandExecutor idGeneratorCommandExecutor = null;
      if (idGeneratorDataSource != null) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(idGeneratorDataSource);
        processEngineConfiguration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_FALSE);
        processEngineConfiguration.init();
        idGeneratorCommandExecutor = processEngineConfiguration.getCommandExecutorTxRequiresNew();
      } else if (idGeneratorDataSourceJndiName != null) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSourceJndiName(idGeneratorDataSourceJndiName);
        processEngineConfiguration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_FALSE);
        processEngineConfiguration.init();
        idGeneratorCommandExecutor = processEngineConfiguration.getCommandExecutorTxRequiresNew();
      } else {
        idGeneratorCommandExecutor = commandExecutorTxRequiresNew;
      }

      DbIdGenerator dbIdGenerator = new DbIdGenerator();
      dbIdGenerator.setIdBlockSize(idBlockSize);
      dbIdGenerator.setCommandExecutor(idGeneratorCommandExecutor);
      idGenerator = dbIdGenerator;
    }
  }

  // OTHER ////////////////////////////////////////////////////////////////////

  protected void initCommandContextFactory() {
    if (commandContextFactory == null) {
      commandContextFactory = new CommandContextFactory();
      commandContextFactory.setProcessEngineConfiguration(this);
    }
  }

  protected void initTransactionContextFactory() {
    if (transactionContextFactory == null) {
      transactionContextFactory = new StandaloneTransactionContextFactory();
    }
  }

  protected void initValueTypeResolver() {
    if (valueTypeResolver == null) {
      valueTypeResolver = new ValueTypeResolverImpl();
    }
  }

  protected void initDefaultCharset() {
    if (defaultCharset == null) {
      if (defaultCharsetName == null) {
        defaultCharsetName = UTF_8.name();
      }
      defaultCharset = Charset.forName(defaultCharsetName);
    }
  }

  protected void initMetrics() {
    if (isMetricsEnabled) {

      if (metricsRegistry == null) {
        metricsRegistry = new MetricsRegistry();
      }

      initDefaultMetrics(metricsRegistry);

      if (dbMetricsReporter == null) {
        dbMetricsReporter = new DbMetricsReporter(metricsRegistry, commandExecutorTxRequired);
      }
    }
  }

  protected void initHostName() {
    if (hostname == null) {
      if (hostnameProvider == null) {
        hostnameProvider = new SimpleIpBasedProvider();
      }
      hostname = hostnameProvider.getHostname(this);
    }
  }

  protected void initDefaultMetrics(MetricsRegistry metricsRegistry) {
    metricsRegistry.createMeter(Metrics.ACTIVTY_INSTANCE_START);
    metricsRegistry.createDbMeter(Metrics.ACTIVTY_INSTANCE_END);

    metricsRegistry.createDbMeter(Metrics.JOB_ACQUISITION_ATTEMPT);
    metricsRegistry.createDbMeter(Metrics.JOB_ACQUIRED_SUCCESS);
    metricsRegistry.createDbMeter(Metrics.JOB_ACQUIRED_FAILURE);
    metricsRegistry.createDbMeter(Metrics.JOB_SUCCESSFUL);
    metricsRegistry.createDbMeter(Metrics.JOB_FAILED);
    metricsRegistry.createDbMeter(Metrics.JOB_LOCKED_EXCLUSIVE);
    metricsRegistry.createDbMeter(Metrics.JOB_EXECUTION_REJECTED);

    metricsRegistry.createMeter(Metrics.ROOT_PROCESS_INSTANCE_START);

    metricsRegistry.createMeter(Metrics.EXECUTED_DECISION_INSTANCES);
    metricsRegistry.createMeter(Metrics.EXECUTED_DECISION_ELEMENTS);
  }

  protected void initSerialization() {
    if (variableSerializers == null) {
      variableSerializers = new DefaultVariableSerializers();

      if (customPreVariableSerializers != null) {
        for (TypedValueSerializer<?> customVariableType : customPreVariableSerializers) {
          variableSerializers.addSerializer(customVariableType);
        }
      }

      // register built-in serializers
      variableSerializers.addSerializer(new NullValueSerializer());
      variableSerializers.addSerializer(new StringValueSerializer());
      variableSerializers.addSerializer(new BooleanValueSerializer());
      variableSerializers.addSerializer(new ShortValueSerializer());
      variableSerializers.addSerializer(new IntegerValueSerializer());
      variableSerializers.addSerializer(new LongValueSerlializer());
      variableSerializers.addSerializer(new DateValueSerializer());
      variableSerializers.addSerializer(new DoubleValueSerializer());
      variableSerializers.addSerializer(new ByteArrayValueSerializer());
      variableSerializers.addSerializer(new JavaObjectSerializer());
      variableSerializers.addSerializer(new FileValueSerializer());

      if (customPostVariableSerializers != null) {
        for (TypedValueSerializer<?> customVariableType : customPostVariableSerializers) {
          variableSerializers.addSerializer(customVariableType);
        }
      }

    }
  }

  protected void initFormEngines() {
    if (formEngines == null) {
      formEngines = new HashMap<>();
      // html form engine = default form engine
      FormEngine defaultFormEngine = new HtmlFormEngine();
      formEngines.put(null, defaultFormEngine); // default form engine is looked up with null
      formEngines.put(defaultFormEngine.getName(), defaultFormEngine);
      FormEngine juelFormEngine = new JuelFormEngine();
      formEngines.put(juelFormEngine.getName(), juelFormEngine);

    }
    if (customFormEngines != null) {
      for (FormEngine formEngine : customFormEngines) {
        formEngines.put(formEngine.getName(), formEngine);
      }
    }
  }

  protected void initFormTypes() {
    if (formTypes == null) {
      formTypes = new FormTypes();
      formTypes.addFormType(new StringFormType());
      formTypes.addFormType(new LongFormType());
      formTypes.addFormType(new DateFormType("dd/MM/yyyy"));
      formTypes.addFormType(new BooleanFormType());
    }
    if (customFormTypes != null) {
      for (AbstractFormFieldType customFormType : customFormTypes) {
        formTypes.addFormType(customFormType);
      }
    }
  }

  protected void initFormFieldValidators() {
    if (formValidators == null) {
      formValidators = new FormValidators();
      formValidators.addValidator("min", MinValidator.class);
      formValidators.addValidator("max", MaxValidator.class);
      formValidators.addValidator("minlength", MinLengthValidator.class);
      formValidators.addValidator("maxlength", MaxLengthValidator.class);
      formValidators.addValidator("required", RequiredValidator.class);
      formValidators.addValidator("readonly", ReadOnlyValidator.class);
    }
    if (customFormFieldValidators != null) {
      for (Entry<String, Class<? extends FormFieldValidator>> validator : customFormFieldValidators.entrySet()) {
        formValidators.addValidator(validator.getKey(), validator.getValue());
      }
    }

  }

  protected void initScripting() {
    if (resolverFactories == null) {
      resolverFactories = new ArrayList<>();
      resolverFactories.add(new MocksResolverFactory());
      resolverFactories.add(new VariableScopeResolverFactory());
      resolverFactories.add(new BeansResolverFactory());
    }
    if (scriptEngineResolver == null) {
      scriptEngineResolver = new DefaultScriptEngineResolver(new OperatonScriptEngineManager());
    }
    if (scriptingEngines == null) {
      scriptingEngines = new ScriptingEngines(new ScriptBindingsFactory(resolverFactories), scriptEngineResolver);
      scriptingEngines.setEnableScriptEngineCaching(enableScriptEngineCaching);
    }
    if (scriptFactory == null) {
      scriptFactory = new ScriptFactory();
    }
    if (scriptEnvResolvers == null) {
      scriptEnvResolvers = new ArrayList<>();
    }
    if (scriptingEnvironment == null) {
      scriptingEnvironment = new ScriptingEnvironment(scriptFactory, scriptEnvResolvers, scriptingEngines);
    }
  }

  protected void initDmnEngine() {
    if (dmnEngine == null) {

      if (dmnEngineConfiguration == null) {
        dmnEngineConfiguration = (DefaultDmnEngineConfiguration) DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
      }

      DmnEngineConfigurationBuilder dmnEngineConfigurationBuilder = new DmnEngineConfigurationBuilder(dmnEngineConfiguration)
          .dmnHistoryEventProducer(dmnHistoryEventProducer)
          .scriptEngineResolver(scriptingEngines)
          .feelCustomFunctionProviders(dmnFeelCustomFunctionProviders)
          .enableFeelLegacyBehavior(dmnFeelEnableLegacyBehavior)
          .returnBlankTableOutputAsNull(dmnReturnBlankTableOutputAsNull);

      if (dmnElProvider != null) {
        dmnEngineConfigurationBuilder.elProvider(dmnElProvider);
      } else if (expressionManager instanceof ElProviderCompatible elProviderCompatible) {
        dmnEngineConfigurationBuilder.elProvider(elProviderCompatible.toElProvider());
      }

      dmnEngineConfiguration = dmnEngineConfigurationBuilder.build();

      dmnEngine = dmnEngineConfiguration.buildEngine();

    } else if (dmnEngineConfiguration == null) {
      dmnEngineConfiguration = (DefaultDmnEngineConfiguration) dmnEngine.getConfiguration();
    }
  }

  protected void initExpressionManager() {
    if (expressionManager == null) {
      expressionManager = new JuelExpressionManager(beans);
    }


    expressionManager.addFunction(CommandContextFunctions.CURRENT_USER,
        ReflectUtil.getMethod(CommandContextFunctions.class, CommandContextFunctions.CURRENT_USER));
    expressionManager.addFunction(CommandContextFunctions.CURRENT_USER_GROUPS,
        ReflectUtil.getMethod(CommandContextFunctions.class, CommandContextFunctions.CURRENT_USER_GROUPS));

    expressionManager.addFunction(DateTimeFunctions.NOW,
        ReflectUtil.getMethod(DateTimeFunctions.class, DateTimeFunctions.NOW));
    expressionManager.addFunction(DateTimeFunctions.DATE_TIME,
        ReflectUtil.getMethod(DateTimeFunctions.class, DateTimeFunctions.DATE_TIME));
  }

  protected void initBusinessCalendarManager() {
    if (businessCalendarManager == null) {
      MapBusinessCalendarManager mapBusinessCalendarManager = new MapBusinessCalendarManager();
      mapBusinessCalendarManager.addBusinessCalendar(DurationBusinessCalendar.NAME, new DurationBusinessCalendar());
      mapBusinessCalendarManager.addBusinessCalendar(DueDateBusinessCalendar.NAME, new DueDateBusinessCalendar());
      mapBusinessCalendarManager.addBusinessCalendar(CycleBusinessCalendar.NAME, new CycleBusinessCalendar());

      businessCalendarManager = mapBusinessCalendarManager;
    }
  }

  protected void initDelegateInterceptor() {
    if (delegateInterceptor == null) {
      delegateInterceptor = new DefaultDelegateInterceptor();
    }
  }

  protected void initEventHandlers() {
    if (eventHandlers == null) {
      eventHandlers = new HashMap<>();

      SignalEventHandler signalEventHander = new SignalEventHandler();
      eventHandlers.put(signalEventHander.getEventHandlerType(), signalEventHander);

      CompensationEventHandler compensationEventHandler = new CompensationEventHandler();
      eventHandlers.put(compensationEventHandler.getEventHandlerType(), compensationEventHandler);

      EventHandler messageEventHandler = new EventHandlerImpl(EventType.MESSAGE);
      eventHandlers.put(messageEventHandler.getEventHandlerType(), messageEventHandler);

      EventHandler conditionalEventHandler = new ConditionalEventHandler();
      eventHandlers.put(conditionalEventHandler.getEventHandlerType(), conditionalEventHandler);

    }
    if (customEventHandlers != null) {
      for (EventHandler eventHandler : customEventHandlers) {
        eventHandlers.put(eventHandler.getEventHandlerType(), eventHandler);
      }
    }
  }

  protected void initCommandCheckers() {
    if (commandCheckers == null) {
      commandCheckers = new ArrayList<>();

      // add the default command checkers
      commandCheckers.add(new TenantCommandChecker());
      commandCheckers.add(new AuthorizationCommandChecker());
    }
  }

  protected void initBeans() {
    if (beans == null) {
      beans = DEFAULT_BEANS_MAP;
    }
  }

  protected void initArtifactFactory() {
    if (artifactFactory == null) {
      artifactFactory = new DefaultArtifactFactory();
    }
  }

  protected void initProcessApplicationManager() {
    if (processApplicationManager == null) {
      processApplicationManager = new ProcessApplicationManager();
    }
  }

  // correlation handler //////////////////////////////////////////////////////
  protected void initCorrelationHandler() {
    if (correlationHandler == null) {
      correlationHandler = new DefaultCorrelationHandler();
    }

  }

  // condition handler //////////////////////////////////////////////////////
  protected void initConditionHandler() {
    if (conditionHandler == null) {
      conditionHandler = new DefaultConditionHandler();
    }
  }

  // deployment handler //////////////////////////////////////////////////////
  protected void initDeploymentHandlerFactory() {
    if (deploymentHandlerFactory == null) {
      deploymentHandlerFactory = new DefaultDeploymentHandlerFactory();
    }
  }

  // history handlers /////////////////////////////////////////////////////

  protected void initHistoryEventProducer() {
    if (historyEventProducer == null) {
      historyEventProducer = new CacheAwareHistoryEventProducer();
    }
  }

  protected void initCmmnHistoryEventProducer() {
    if (cmmnHistoryEventProducer == null) {
      cmmnHistoryEventProducer = new CacheAwareCmmnHistoryEventProducer();
    }
  }

  protected void initDmnHistoryEventProducer() {
    if (dmnHistoryEventProducer == null) {
      dmnHistoryEventProducer = new DefaultDmnHistoryEventProducer();
    }
  }

  protected void initHistoryEventHandler() {
    if (historyEventHandler == null) {
      if (enableDefaultDbHistoryEventHandler) {
        historyEventHandler = new CompositeDbHistoryEventHandler(customHistoryEventHandlers);
      } else {
        historyEventHandler = new CompositeHistoryEventHandler(customHistoryEventHandlers);
      }
    }
  }

  // password digest //////////////////////////////////////////////////////////

  protected void initPasswordDigest() {
    if(saltGenerator == null) {
      saltGenerator = new Default16ByteSaltGenerator();
    }
    if (passwordEncryptor == null) {
      passwordEncryptor = new Sha512HashDigest();
    }
    if(customPasswordChecker == null) {
      customPasswordChecker = Collections.emptyList();
    }
    if(passwordManager == null) {
      passwordManager = new PasswordManager(passwordEncryptor, customPasswordChecker);
    }
  }

  public void initPasswordPolicy() {
    if(passwordPolicy == null && enablePasswordPolicy) {
      passwordPolicy = new DefaultPasswordPolicyImpl();
    }
  }

  protected void initDeploymentRegistration() {
    if (registeredDeployments == null) {
      registeredDeployments = new CopyOnWriteArraySet<>();
    }
  }

  protected void initOperationLog() {
    if(logEntriesPerSyncOperationLimit < -1 || logEntriesPerSyncOperationLimit == 0) {
      throw new ProcessEngineException(
          "Invalid configuration for logEntriesPerSyncOperationLimit. Configured value needs to be either -1 or greater than 0 but was "
              + logEntriesPerSyncOperationLimit + ".");
    }
  }

  // cache factory //////////////////////////////////////////////////////////

  protected void initCacheFactory() {
    if (cacheFactory == null) {
      cacheFactory = new DefaultCacheFactory();
    }
  }

  // resource authorization provider //////////////////////////////////////////

  protected void initResourceAuthorizationProvider() {
    if (resourceAuthorizationProvider == null) {
      resourceAuthorizationProvider = new DefaultAuthorizationProvider();
    }
  }

  protected void initPermissionProvider() {
    if (permissionProvider == null) {
      permissionProvider = new DefaultPermissionProvider();
    }
  }

  protected void initDefaultUserPermissionForTask() {
    if (defaultUserPermissionForTask == null) {
      if (Permissions.UPDATE.getName().equals(defaultUserPermissionNameForTask)) {
        defaultUserPermissionForTask = Permissions.UPDATE;
      } else if (Permissions.TASK_WORK.getName().equals(defaultUserPermissionNameForTask)) {
        defaultUserPermissionForTask = Permissions.TASK_WORK;
      } else {
        throw LOG.invalidConfigDefaultUserPermissionNameForTask(defaultUserPermissionNameForTask, new String[]{Permissions.UPDATE.getName(), Permissions.TASK_WORK.getName()});
      }
    }
  }

  protected void initAdminUser() {
    if (adminUsers == null) {
      adminUsers = new ArrayList<>();
    }
  }

  protected void initAdminGroups() {
    if (adminGroups == null) {
      adminGroups = new ArrayList<>();
    }
    if (adminGroups.isEmpty() || !(adminGroups.contains(Groups.OPERATON_ADMIN))) {
      adminGroups.add(Groups.OPERATON_ADMIN);
    }
  }

  protected void initDiagnostics() {
    if (diagnosticsRegistry == null) {
      diagnosticsRegistry = new DiagnosticsRegistry();
    }
    if (telemetryData == null) {
      initTelemetryData();
    }
    if (diagnosticsCollector == null) {
      diagnosticsCollector = new DiagnosticsCollector(telemetryData, diagnosticsRegistry, metricsRegistry);
    }
  }

  protected void initTelemetryData() {
    DatabaseImpl database = new DatabaseImpl(databaseVendor, databaseVersion);

    JdkImpl jdk = ParseUtil.parseJdkDetails();

    InternalsImpl internals = new InternalsImpl(database, diagnosticsRegistry.getApplicationServer(), jdk);
    internals.setDataCollectionStartDate(ClockUtil.getCurrentTime());

    String operatonIntegration = diagnosticsRegistry.getOperatonIntegration();
    if (operatonIntegration != null && !operatonIntegration.isEmpty()) {
      internals.getOperatonIntegration().add(operatonIntegration);
    }

    ProcessEngineDetails engineInfo = ParseUtil
        .parseProcessEngineVersion(true);

    ProductImpl product = new ProductImpl(PRODUCT_NAME, engineInfo.getVersion(), engineInfo.getEdition(), internals);

    // installationId=null, the id will be fetched later from database
    telemetryData = new TelemetryDataImpl(null, product);
  }

  // getters and setters //////////////////////////////////////////////////////

  @Override
  public String getProcessEngineName() {
    return processEngineName;
  }

  public HistoryLevel getHistoryLevel() {
    return historyLevel;
  }

  public void setHistoryLevel(HistoryLevel historyLevel) {
    this.historyLevel = historyLevel;
  }

  public HistoryLevel getDefaultHistoryLevel() {
    if (historyLevels != null) {
      for (HistoryLevel historyLvl : historyLevels) {
        if (HISTORY_DEFAULT != null && HISTORY_DEFAULT.equalsIgnoreCase(historyLvl.getName())) {
          return historyLvl;
        }
      }
    }

    return null;
  }

  @Override
  public ProcessEngineConfigurationImpl setProcessEngineName(String processEngineName) {
    this.processEngineName = processEngineName;
    return this;
  }

  public List<CommandInterceptor> getCustomPreCommandInterceptorsTxRequired() {
    return customPreCommandInterceptorsTxRequired;
  }

  public ProcessEngineConfigurationImpl setCustomPreCommandInterceptorsTxRequired(List<CommandInterceptor> customPreCommandInterceptorsTxRequired) {
    this.customPreCommandInterceptorsTxRequired = customPreCommandInterceptorsTxRequired;
    return this;
  }

  public List<CommandInterceptor> getCustomPostCommandInterceptorsTxRequired() {
    return customPostCommandInterceptorsTxRequired;
  }

  public ProcessEngineConfigurationImpl setCustomPostCommandInterceptorsTxRequired(List<CommandInterceptor> customPostCommandInterceptorsTxRequired) {
    this.customPostCommandInterceptorsTxRequired = customPostCommandInterceptorsTxRequired;
    return this;
  }

  public List<CommandInterceptor> getCommandInterceptorsTxRequired() {
    return commandInterceptorsTxRequired;
  }

  public ProcessEngineConfigurationImpl setCommandInterceptorsTxRequired(List<CommandInterceptor> commandInterceptorsTxRequired) {
    this.commandInterceptorsTxRequired = commandInterceptorsTxRequired;
    return this;
  }

  public CommandExecutor getCommandExecutorTxRequired() {
    return commandExecutorTxRequired;
  }

  public ProcessEngineConfigurationImpl setCommandExecutorTxRequired(CommandExecutor commandExecutorTxRequired) {
    this.commandExecutorTxRequired = commandExecutorTxRequired;
    return this;
  }

  public List<CommandInterceptor> getCustomPreCommandInterceptorsTxRequiresNew() {
    return customPreCommandInterceptorsTxRequiresNew;
  }

  public ProcessEngineConfigurationImpl setCustomPreCommandInterceptorsTxRequiresNew(List<CommandInterceptor> customPreCommandInterceptorsTxRequiresNew) {
    this.customPreCommandInterceptorsTxRequiresNew = customPreCommandInterceptorsTxRequiresNew;
    return this;
  }

  public List<CommandInterceptor> getCustomPostCommandInterceptorsTxRequiresNew() {
    return customPostCommandInterceptorsTxRequiresNew;
  }

  public ProcessEngineConfigurationImpl setCustomPostCommandInterceptorsTxRequiresNew(List<CommandInterceptor> customPostCommandInterceptorsTxRequiresNew) {
    this.customPostCommandInterceptorsTxRequiresNew = customPostCommandInterceptorsTxRequiresNew;
    return this;
  }

  public List<CommandInterceptor> getCommandInterceptorsTxRequiresNew() {
    return commandInterceptorsTxRequiresNew;
  }

  public ProcessEngineConfigurationImpl setCommandInterceptorsTxRequiresNew(List<CommandInterceptor> commandInterceptorsTxRequiresNew) {
    this.commandInterceptorsTxRequiresNew = commandInterceptorsTxRequiresNew;
    return this;
  }

  public CommandExecutor getCommandExecutorTxRequiresNew() {
    return commandExecutorTxRequiresNew;
  }

  public ProcessEngineConfigurationImpl setCommandExecutorTxRequiresNew(CommandExecutor commandExecutorTxRequiresNew) {
    this.commandExecutorTxRequiresNew = commandExecutorTxRequiresNew;
    return this;
  }

  public RepositoryService getRepositoryService() {
    return repositoryService;
  }

  public ProcessEngineConfigurationImpl setRepositoryService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
    return this;
  }

  public RuntimeService getRuntimeService() {
    return runtimeService;
  }

  public ProcessEngineConfigurationImpl setRuntimeService(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
    return this;
  }

  public HistoryService getHistoryService() {
    return historyService;
  }

  public ProcessEngineConfigurationImpl setHistoryService(HistoryService historyService) {
    this.historyService = historyService;
    return this;
  }

  public IdentityService getIdentityService() {
    return identityService;
  }

  public ProcessEngineConfigurationImpl setIdentityService(IdentityService identityService) {
    this.identityService = identityService;
    return this;
  }

  public TaskService getTaskService() {
    return taskService;
  }

  public ProcessEngineConfigurationImpl setTaskService(TaskService taskService) {
    this.taskService = taskService;
    return this;
  }

  public FormService getFormService() {
    return formService;
  }

  public ProcessEngineConfigurationImpl setFormService(FormService formService) {
    this.formService = formService;
    return this;
  }

  public ManagementService getManagementService() {
    return managementService;
  }

  public AuthorizationService getAuthorizationService() {
    return authorizationService;
  }

  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public ProcessEngineConfigurationImpl setManagementService(ManagementService managementService) {
    this.managementService = managementService;
    return this;
  }

  public CaseService getCaseService() {
    return caseService;
  }

  public void setCaseService(CaseService caseService) {
    this.caseService = caseService;
  }

  public FilterService getFilterService() {
    return filterService;
  }

  public void setFilterService(FilterService filterService) {
    this.filterService = filterService;
  }

  public ExternalTaskService getExternalTaskService() {
    return externalTaskService;
  }

  public void setExternalTaskService(ExternalTaskService externalTaskService) {
    this.externalTaskService = externalTaskService;
  }

  public DecisionService getDecisionService() {
    return decisionService;
  }

  public void setDecisionService(DecisionService decisionService) {
    this.decisionService = decisionService;
  }

  public OptimizeService getOptimizeService() {
    return optimizeService;
  }

  public Map<Class<?>, SessionFactory> getSessionFactories() {
    return sessionFactories;
  }

  public ProcessEngineConfigurationImpl setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
    this.sessionFactories = sessionFactories;
    return this;
  }

  public List<Deployer> getDeployers() {
    return deployers;
  }

  public ProcessEngineConfigurationImpl setDeployers(List<Deployer> deployers) {
    this.deployers = deployers;
    return this;
  }

  public JobExecutor getJobExecutor() {
    return jobExecutor;
  }

  public ProcessEngineConfigurationImpl setJobExecutor(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
    return this;
  }

  public PriorityProvider<JobDeclaration<?, ?>> getJobPriorityProvider() {
    return jobPriorityProvider;
  }

  public void setJobPriorityProvider(PriorityProvider<JobDeclaration<?, ?>> jobPriorityProvider) {
    this.jobPriorityProvider = jobPriorityProvider;
  }

  public long getJobExecutorPriorityRangeMin() {
    return jobExecutorPriorityRangeMin;
  }

  public ProcessEngineConfigurationImpl setJobExecutorPriorityRangeMin(long jobExecutorPriorityRangeMin) {
    this.jobExecutorPriorityRangeMin = jobExecutorPriorityRangeMin;
    return this;
  }

  public long getJobExecutorPriorityRangeMax() {
    return jobExecutorPriorityRangeMax;
  }

  public ProcessEngineConfigurationImpl setJobExecutorPriorityRangeMax(long jobExecutorPriorityRangeMax) {
    this.jobExecutorPriorityRangeMax = jobExecutorPriorityRangeMax;
    return this;
  }

  public PriorityProvider<ExternalTaskActivityBehavior> getExternalTaskPriorityProvider() {
    return externalTaskPriorityProvider;
  }

  public void setExternalTaskPriorityProvider(PriorityProvider<ExternalTaskActivityBehavior> externalTaskPriorityProvider) {
    this.externalTaskPriorityProvider = externalTaskPriorityProvider;
  }

  public IdGenerator getIdGenerator() {
    return idGenerator;
  }

  public ProcessEngineConfigurationImpl setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }

  public String getWsSyncFactoryClassName() {
    return wsSyncFactoryClassName;
  }

  public ProcessEngineConfigurationImpl setWsSyncFactoryClassName(String wsSyncFactoryClassName) {
    this.wsSyncFactoryClassName = wsSyncFactoryClassName;
    return this;
  }

  public Map<String, FormEngine> getFormEngines() {
    return formEngines;
  }

  public ProcessEngineConfigurationImpl setFormEngines(Map<String, FormEngine> formEngines) {
    this.formEngines = formEngines;
    return this;
  }

  public FormTypes getFormTypes() {
    return formTypes;
  }

  public ProcessEngineConfigurationImpl setFormTypes(FormTypes formTypes) {
    this.formTypes = formTypes;
    return this;
  }

  public ScriptingEngines getScriptingEngines() {
    return scriptingEngines;
  }

  public ProcessEngineConfigurationImpl setScriptingEngines(ScriptingEngines scriptingEngines) {
    this.scriptingEngines = scriptingEngines;
    return this;
  }

  public VariableSerializers getVariableSerializers() {
    return variableSerializers;
  }

  public VariableSerializerFactory getFallbackSerializerFactory() {
    return fallbackSerializerFactory;
  }

  public void setFallbackSerializerFactory(VariableSerializerFactory fallbackSerializerFactory) {
    this.fallbackSerializerFactory = fallbackSerializerFactory;
  }

  public boolean isImplicitVariableUpdateDetectionEnabled() {
    return implicitVariableUpdateDetectionEnabled;
  }

  public void setImplicitVariableUpdateDetectionEnabled(boolean newValue) {
    this.implicitVariableUpdateDetectionEnabled = newValue;
  }

  public ProcessEngineConfigurationImpl setVariableTypes(VariableSerializers variableSerializers) {
    this.variableSerializers = variableSerializers;
    return this;
  }

  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }

  public ProcessEngineConfigurationImpl setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
    return this;
  }

  public ElProvider getDmnElProvider() {
    return dmnElProvider;
  }

  public ProcessEngineConfigurationImpl setDmnElProvider(ElProvider elProvider) {
    this.dmnElProvider = elProvider;
    return this;
  }

  public BusinessCalendarManager getBusinessCalendarManager() {
    return businessCalendarManager;
  }

  public ProcessEngineConfigurationImpl setBusinessCalendarManager(BusinessCalendarManager businessCalendarManager) {
    this.businessCalendarManager = businessCalendarManager;
    return this;
  }

  public CommandContextFactory getCommandContextFactory() {
    return commandContextFactory;
  }

  public ProcessEngineConfigurationImpl setCommandContextFactory(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
    return this;
  }

  public TransactionContextFactory getTransactionContextFactory() {
    return transactionContextFactory;
  }

  public ProcessEngineConfigurationImpl setTransactionContextFactory(TransactionContextFactory transactionContextFactory) {
    this.transactionContextFactory = transactionContextFactory;
    return this;
  }

  public BpmnParseFactory getBpmnParseFactory() {
    return bpmnParseFactory;
  }

  public ProcessEngineConfigurationImpl setBpmnParseFactory(BpmnParseFactory bpmnParseFactory) {
    this.bpmnParseFactory = bpmnParseFactory;
    return this;
  }


  public List<Deployer> getCustomPreDeployers() {
    return customPreDeployers;
  }


  public ProcessEngineConfigurationImpl setCustomPreDeployers(List<Deployer> customPreDeployers) {
    this.customPreDeployers = customPreDeployers;
    return this;
  }


  public List<Deployer> getCustomPostDeployers() {
    return customPostDeployers;
  }


  public ProcessEngineConfigurationImpl setCustomPostDeployers(List<Deployer> customPostDeployers) {
    this.customPostDeployers = customPostDeployers;
    return this;
  }

  public void setCacheFactory(CacheFactory cacheFactory) {
    this.cacheFactory = cacheFactory;
  }

  public void setCacheCapacity(int cacheCapacity) {
    this.cacheCapacity = cacheCapacity;
  }

  public void setEnableFetchProcessDefinitionDescription(boolean enableFetchProcessDefinitionDescription){
    this.enableFetchProcessDefinitionDescription = enableFetchProcessDefinitionDescription;
  }

  public boolean getEnableFetchProcessDefinitionDescription() {
    return this.enableFetchProcessDefinitionDescription;
  }

  public Permission getDefaultUserPermissionForTask() {
    return defaultUserPermissionForTask;
  }

  public ProcessEngineConfigurationImpl setDefaultUserPermissionForTask(Permission defaultUserPermissionForTask) {
    this.defaultUserPermissionForTask = defaultUserPermissionForTask;
    return this;
  }

  public ProcessEngineConfigurationImpl setEnableHistoricInstancePermissions(boolean enable) {
    this.enableHistoricInstancePermissions = enable;
    return this;
  }

  public boolean isEnableHistoricInstancePermissions() {
    return enableHistoricInstancePermissions;
  }

  public Map<String, JobHandler> getJobHandlers() {
    return jobHandlers;
  }


  public ProcessEngineConfigurationImpl setJobHandlers(Map<String, JobHandler> jobHandlers) {
    this.jobHandlers = jobHandlers;
    return this;
  }


  public SqlSessionFactory getSqlSessionFactory() {
    return sqlSessionFactory;
  }


  public ProcessEngineConfigurationImpl setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }


  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

  public ProcessEngineConfigurationImpl setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    return this;
  }

  public TransactionFactory getTransactionFactory() {
    return transactionFactory;
  }

  public ProcessEngineConfigurationImpl setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }

  public List<SessionFactory> getCustomSessionFactories() {
    return customSessionFactories;
  }

  public ProcessEngineConfigurationImpl setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
    this.customSessionFactories = customSessionFactories;
    return this;
  }

  public List<JobHandler> getCustomJobHandlers() {
    return customJobHandlers;
  }

  public ProcessEngineConfigurationImpl setCustomJobHandlers(List<JobHandler> customJobHandlers) {
    this.customJobHandlers = customJobHandlers;
    return this;
  }

  public List<FormEngine> getCustomFormEngines() {
    return customFormEngines;
  }

  public ProcessEngineConfigurationImpl setCustomFormEngines(List<FormEngine> customFormEngines) {
    this.customFormEngines = customFormEngines;
    return this;
  }

  public List<AbstractFormFieldType> getCustomFormTypes() {
    return customFormTypes;
  }


  public ProcessEngineConfigurationImpl setCustomFormTypes(List<AbstractFormFieldType> customFormTypes) {
    this.customFormTypes = customFormTypes;
    return this;
  }

  public List<TypedValueSerializer> getCustomPreVariableSerializers() {
    return customPreVariableSerializers;
  }


  public ProcessEngineConfigurationImpl setCustomPreVariableSerializers(List<TypedValueSerializer> customPreVariableTypes) {
    this.customPreVariableSerializers = customPreVariableTypes;
    return this;
  }


  public List<TypedValueSerializer> getCustomPostVariableSerializers() {
    return customPostVariableSerializers;
  }


  public ProcessEngineConfigurationImpl setCustomPostVariableSerializers(List<TypedValueSerializer> customPostVariableTypes) {
    this.customPostVariableSerializers = customPostVariableTypes;
    return this;
  }

  public List<BpmnParseListener> getCustomPreBPMNParseListeners() {
    return preParseListeners;
  }

  public void setCustomPreBPMNParseListeners(List<BpmnParseListener> preParseListeners) {
    this.preParseListeners = preParseListeners;
  }

  public List<BpmnParseListener> getCustomPostBPMNParseListeners() {
    return postParseListeners;
  }

  public void setCustomPostBPMNParseListeners(List<BpmnParseListener> postParseListeners) {
    this.postParseListeners = postParseListeners;
  }

  /**
   * @deprecated Use {@link #getCustomPreBPMNParseListeners} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public List<BpmnParseListener> getPreParseListeners() {
    return preParseListeners;
  }

  /**
   * @deprecated Use {@link #setCustomPreBPMNParseListeners} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void setPreParseListeners(List<BpmnParseListener> preParseListeners) {
    this.preParseListeners = preParseListeners;
  }

  /**
   * @deprecated Use {@link #getCustomPostBPMNParseListeners} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public List<BpmnParseListener> getPostParseListeners() {
    return postParseListeners;
  }

  /**
   * @deprecated Use {@link #setCustomPostBPMNParseListeners} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void setPostParseListeners(List<BpmnParseListener> postParseListeners) {
    this.postParseListeners = postParseListeners;
  }

  public List<CmmnTransformListener> getCustomPreCmmnTransformListeners() {
    return customPreCmmnTransformListeners;
  }

  public void setCustomPreCmmnTransformListeners(List<CmmnTransformListener> customPreCmmnTransformListeners) {
    this.customPreCmmnTransformListeners = customPreCmmnTransformListeners;
  }

  public List<CmmnTransformListener> getCustomPostCmmnTransformListeners() {
    return customPostCmmnTransformListeners;
  }

  public void setCustomPostCmmnTransformListeners(List<CmmnTransformListener> customPostCmmnTransformListeners) {
    this.customPostCmmnTransformListeners = customPostCmmnTransformListeners;
  }

  public Map<Object, Object> getBeans() {
    return beans;
  }

  public void setBeans(Map<Object, Object> beans) {
    this.beans = beans;
  }

  public boolean getSkipIsolationLevelCheck() {
    return this.skipIsolationLevelCheck;
  }

  public ProcessEngineConfigurationImpl setSkipIsolationLevelCheck(boolean skipIsolationLevelCheck) {
    this.skipIsolationLevelCheck = skipIsolationLevelCheck;
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setClassLoader(ClassLoader classLoader) {
    super.setClassLoader(classLoader);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setDatabaseType(String databaseType) {
    super.setDatabaseType(databaseType);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setDataSource(DataSource dataSource) {
    super.setDataSource(dataSource);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
    super.setDatabaseSchemaUpdate(databaseSchemaUpdate);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setHistory(String history) {
    super.setHistory(history);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setIdBlockSize(int idBlockSize) {
    super.setIdBlockSize(idBlockSize);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcDriver(String jdbcDriver) {
    super.setJdbcDriver(jdbcDriver);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcPassword(String jdbcPassword) {
    super.setJdbcPassword(jdbcPassword);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcUrl(String jdbcUrl) {
    super.setJdbcUrl(jdbcUrl);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcUsername(String jdbcUsername) {
    super.setJdbcUsername(jdbcUsername);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJobExecutorActivate(boolean jobExecutorActivate) {
    super.setJobExecutorActivate(jobExecutorActivate);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerDefaultFrom(String mailServerDefaultFrom) {
    super.setMailServerDefaultFrom(mailServerDefaultFrom);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerHost(String mailServerHost) {
    super.setMailServerHost(mailServerHost);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerPassword(String mailServerPassword) {
    super.setMailServerPassword(mailServerPassword);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerPort(int mailServerPort) {
    super.setMailServerPort(mailServerPort);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerUseTLS(boolean useTLS) {
    super.setMailServerUseTLS(useTLS);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setMailServerUsername(String mailServerUsername) {
    super.setMailServerUsername(mailServerUsername);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
    super.setJdbcMaxActiveConnections(jdbcMaxActiveConnections);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
    super.setJdbcMaxCheckoutTime(jdbcMaxCheckoutTime);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
    super.setJdbcMaxIdleConnections(jdbcMaxIdleConnections);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
    super.setJdbcMaxWaitTime(jdbcMaxWaitTime);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setTransactionsExternallyManaged(boolean transactionsExternallyManaged) {
    super.setTransactionsExternallyManaged(transactionsExternallyManaged);
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcPingEnabled(boolean jdbcPingEnabled) {
    this.jdbcPingEnabled = jdbcPingEnabled;
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcPingQuery(String jdbcPingQuery) {
    this.jdbcPingQuery = jdbcPingQuery;
    return this;
  }

  @Override
  public ProcessEngineConfigurationImpl setJdbcPingConnectionNotUsedFor(int jdbcPingNotUsedFor) {
    this.jdbcPingConnectionNotUsedFor = jdbcPingNotUsedFor;
    return this;
  }

  public boolean isDbIdentityUsed() {
    return isDbIdentityUsed;
  }


  public void setDbIdentityUsed(boolean isDbIdentityUsed) {
    this.isDbIdentityUsed = isDbIdentityUsed;
  }

  public boolean isDbHistoryUsed() {
    return isDbHistoryUsed;
  }

  public void setDbHistoryUsed(boolean isDbHistoryUsed) {
    this.isDbHistoryUsed = isDbHistoryUsed;
  }

  public List<ResolverFactory> getResolverFactories() {
    return resolverFactories;
  }

  public void setResolverFactories(List<ResolverFactory> resolverFactories) {
    this.resolverFactories = resolverFactories;
  }

  public DeploymentCache getDeploymentCache() {
    return deploymentCache;
  }

  public void setDeploymentCache(DeploymentCache deploymentCache) {
    this.deploymentCache = deploymentCache;
  }

  public DeploymentHandlerFactory getDeploymentHandlerFactory() {
    return deploymentHandlerFactory;
  }

  public ProcessEngineConfigurationImpl setDeploymentHandlerFactory(DeploymentHandlerFactory deploymentHandlerFactory) {
    this.deploymentHandlerFactory = deploymentHandlerFactory;
    return this;
  }

  public ProcessEngineConfigurationImpl setDelegateInterceptor(DelegateInterceptor delegateInterceptor) {
    this.delegateInterceptor = delegateInterceptor;
    return this;
  }

  public DelegateInterceptor getDelegateInterceptor() {
    return delegateInterceptor;
  }

  public RejectedJobsHandler getCustomRejectedJobsHandler() {
    return customRejectedJobsHandler;
  }

  public ProcessEngineConfigurationImpl setCustomRejectedJobsHandler(RejectedJobsHandler customRejectedJobsHandler) {
    this.customRejectedJobsHandler = customRejectedJobsHandler;
    return this;
  }

  public EventHandler getEventHandler(String eventType) {
    return eventHandlers.get(eventType);
  }

  public void setEventHandlers(Map<String, EventHandler> eventHandlers) {
    this.eventHandlers = eventHandlers;
  }

  public Map<String, EventHandler> getEventHandlers() {
    return eventHandlers;
  }

  public List<EventHandler> getCustomEventHandlers() {
    return customEventHandlers;
  }

  public void setCustomEventHandlers(List<EventHandler> customEventHandlers) {
    this.customEventHandlers = customEventHandlers;
  }

  public FailedJobCommandFactory getFailedJobCommandFactory() {
    return failedJobCommandFactory;
  }

  public ProcessEngineConfigurationImpl setFailedJobCommandFactory(FailedJobCommandFactory failedJobCommandFactory) {
    this.failedJobCommandFactory = failedJobCommandFactory;
    return this;
  }

  /**
   * Allows configuring a database table prefix which is used for all runtime operations of the process engine.
   * For example, if you specify a prefix named 'PRE1.', activiti will query for executions in a table named
   * 'PRE1.ACT_RU_EXECUTION_'.
   * <p>
   * <p/>
   * <strong>NOTE: the prefix is not respected by automatic database schema management. If you use
   * {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_CREATE_DROP}
   * or {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_TRUE}, activiti will create the database tables
   * using the default names, regardless of the prefix configured here.</strong>
   *
   * @since 5.9
   */
  public ProcessEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
    this.databaseTablePrefix = databaseTablePrefix;
    return this;
  }

  public String getDatabaseTablePrefix() {
    return databaseTablePrefix;
  }

  public boolean isCreateDiagramOnDeploy() {
    return isCreateDiagramOnDeploy;
  }

  public ProcessEngineConfiguration setCreateDiagramOnDeploy(boolean createDiagramOnDeploy) {
    this.isCreateDiagramOnDeploy = createDiagramOnDeploy;
    return this;
  }

  public String getDatabaseSchema() {
    return databaseSchema;
  }

  public void setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
  }

  public DataSource getIdGeneratorDataSource() {
    return idGeneratorDataSource;
  }

  public void setIdGeneratorDataSource(DataSource idGeneratorDataSource) {
    this.idGeneratorDataSource = idGeneratorDataSource;
  }

  public String getIdGeneratorDataSourceJndiName() {
    return idGeneratorDataSourceJndiName;
  }

  public void setIdGeneratorDataSourceJndiName(String idGeneratorDataSourceJndiName) {
    this.idGeneratorDataSourceJndiName = idGeneratorDataSourceJndiName;
  }

  public ProcessApplicationManager getProcessApplicationManager() {
    return processApplicationManager;
  }

  public void setProcessApplicationManager(ProcessApplicationManager processApplicationManager) {
    this.processApplicationManager = processApplicationManager;
  }

  public CommandExecutor getCommandExecutorSchemaOperations() {
    return commandExecutorSchemaOperations;
  }

  public void setCommandExecutorSchemaOperations(CommandExecutor commandExecutorSchemaOperations) {
    this.commandExecutorSchemaOperations = commandExecutorSchemaOperations;
  }

  public CorrelationHandler getCorrelationHandler() {
    return correlationHandler;
  }

  public void setCorrelationHandler(CorrelationHandler correlationHandler) {
    this.correlationHandler = correlationHandler;
  }

  public ConditionHandler getConditionHandler() {
    return conditionHandler;
  }

  public void setConditionHandler(ConditionHandler conditionHandler) {
    this.conditionHandler = conditionHandler;
  }

  public ProcessEngineConfigurationImpl setHistoryEventHandler(HistoryEventHandler historyEventHandler) {
    this.historyEventHandler = historyEventHandler;
    return this;
  }

  public HistoryEventHandler getHistoryEventHandler() {
    return historyEventHandler;
  }

  public boolean isEnableDefaultDbHistoryEventHandler() {
    return enableDefaultDbHistoryEventHandler;
  }

  public void setEnableDefaultDbHistoryEventHandler(boolean enableDefaultDbHistoryEventHandler) {
    this.enableDefaultDbHistoryEventHandler = enableDefaultDbHistoryEventHandler;
  }

  public List<HistoryEventHandler> getCustomHistoryEventHandlers() {
    return customHistoryEventHandlers;
  }

  public void setCustomHistoryEventHandlers(List<HistoryEventHandler> customHistoryEventHandlers) {
    this.customHistoryEventHandlers = customHistoryEventHandlers;
  }

  public IncidentHandler getIncidentHandler(String incidentType) {
    return incidentHandlers.get(incidentType);
  }

  public void addIncidentHandler(IncidentHandler incidentHandler) {
    IncidentHandler existsHandler = incidentHandlers.get(incidentHandler.getIncidentHandlerType());

    if (existsHandler instanceof CompositeIncidentHandler compositeHandler) {
      compositeHandler.add(incidentHandler);
    } else {
      incidentHandlers.put(incidentHandler.getIncidentHandlerType(), incidentHandler);
    }
  }

  public Map<String, IncidentHandler> getIncidentHandlers() {
    return incidentHandlers;
  }

  public void setIncidentHandlers(Map<String, IncidentHandler> incidentHandlers) {
    this.incidentHandlers = incidentHandlers;
  }

  public List<IncidentHandler> getCustomIncidentHandlers() {
    return customIncidentHandlers;
  }

  public void setCustomIncidentHandlers(List<IncidentHandler> customIncidentHandlers) {
    this.customIncidentHandlers = customIncidentHandlers;
  }

  public Map<String, BatchJobHandler<?>> getBatchHandlers() {
    return batchHandlers;
  }

  public void setBatchHandlers(Map<String, BatchJobHandler<?>> batchHandlers) {
    this.batchHandlers = batchHandlers;
  }

  public List<BatchJobHandler<?>> getCustomBatchJobHandlers() {
    return customBatchJobHandlers;
  }

  public void setCustomBatchJobHandlers(List<BatchJobHandler<?>> customBatchJobHandlers) {
    this.customBatchJobHandlers = customBatchJobHandlers;
  }

  public int getBatchJobsPerSeed() {
    return batchJobsPerSeed;
  }

  public void setBatchJobsPerSeed(int batchJobsPerSeed) {
    this.batchJobsPerSeed = batchJobsPerSeed;
  }

  public Map<String, Integer> getInvocationsPerBatchJobByBatchType() {
    return invocationsPerBatchJobByBatchType;
  }

  public ProcessEngineConfigurationImpl setInvocationsPerBatchJobByBatchType(Map<String, Integer> invocationsPerBatchJobByBatchType) {
    this.invocationsPerBatchJobByBatchType = invocationsPerBatchJobByBatchType;
    return this;
  }

  public int getInvocationsPerBatchJob() {
    return invocationsPerBatchJob;
  }

  public void setInvocationsPerBatchJob(int invocationsPerBatchJob) {
    this.invocationsPerBatchJob = invocationsPerBatchJob;
  }

  public int getBatchPollTime() {
    return batchPollTime;
  }

  public void setBatchPollTime(int batchPollTime) {
    this.batchPollTime = batchPollTime;
  }

  public long getBatchJobPriority() {
    return batchJobPriority;
  }

  public void setBatchJobPriority(long batchJobPriority) {
    this.batchJobPriority = batchJobPriority;
  }

  public long getHistoryCleanupJobPriority() {
    return historyCleanupJobPriority;
  }

  public ProcessEngineConfigurationImpl setHistoryCleanupJobPriority(long historyCleanupJobPriority) {
    this.historyCleanupJobPriority = historyCleanupJobPriority;
    return this;
  }

  public int getHistoryCleanupDefaultNumberOfRetries() {
    return this.historyCleanupDefaultNumberOfRetries;
  }

  public ProcessEngineConfigurationImpl setHistoryCleanupDefaultNumberOfRetries(int historyCleanupDefaultNumberOfRetries) {
    this.historyCleanupDefaultNumberOfRetries = historyCleanupDefaultNumberOfRetries;
    return this;
  }

  public SessionFactory getIdentityProviderSessionFactory() {
    return identityProviderSessionFactory;
  }

  public void setIdentityProviderSessionFactory(SessionFactory identityProviderSessionFactory) {
    this.identityProviderSessionFactory = identityProviderSessionFactory;
  }

  public SaltGenerator getSaltGenerator() {
    return saltGenerator;
  }

  public void setSaltGenerator(SaltGenerator saltGenerator) {
    this.saltGenerator = saltGenerator;
  }

  public void setPasswordEncryptor(PasswordEncryptor passwordEncryptor) {
    this.passwordEncryptor = passwordEncryptor;
  }

  public PasswordEncryptor getPasswordEncryptor() {
    return passwordEncryptor;
  }

  public List<PasswordEncryptor> getCustomPasswordChecker() {
    return customPasswordChecker;
  }

  public void setCustomPasswordChecker(List<PasswordEncryptor> customPasswordChecker) {
    this.customPasswordChecker = customPasswordChecker;
  }

  public PasswordManager getPasswordManager() {
    return passwordManager;
  }

  public void setPasswordManager(PasswordManager passwordManager) {
    this.passwordManager = passwordManager;
  }

  public Set<String> getRegisteredDeployments() {
    return registeredDeployments;
  }

  public void setRegisteredDeployments(Set<String> registeredDeployments) {
    this.registeredDeployments = registeredDeployments;
  }

  public ResourceAuthorizationProvider getResourceAuthorizationProvider() {
    return resourceAuthorizationProvider;
  }

  public void setResourceAuthorizationProvider(ResourceAuthorizationProvider resourceAuthorizationProvider) {
    this.resourceAuthorizationProvider = resourceAuthorizationProvider;
  }

  public PermissionProvider getPermissionProvider() {
    return permissionProvider;
  }

  public void setPermissionProvider(PermissionProvider permissionProvider) {
    this.permissionProvider = permissionProvider;
  }

  public List<ProcessEnginePlugin> getProcessEnginePlugins() {
    return processEnginePlugins;
  }

  public void setProcessEnginePlugins(List<ProcessEnginePlugin> processEnginePlugins) {
    this.processEnginePlugins = processEnginePlugins;
  }

  public ProcessEngineConfigurationImpl setHistoryEventProducer(HistoryEventProducer historyEventProducer) {
    this.historyEventProducer = historyEventProducer;
    return this;
  }

  public HistoryEventProducer getHistoryEventProducer() {
    return historyEventProducer;
  }

  public ProcessEngineConfigurationImpl setCmmnHistoryEventProducer(CmmnHistoryEventProducer cmmnHistoryEventProducer) {
    this.cmmnHistoryEventProducer = cmmnHistoryEventProducer;
    return this;
  }

  public CmmnHistoryEventProducer getCmmnHistoryEventProducer() {
    return cmmnHistoryEventProducer;
  }

  public ProcessEngineConfigurationImpl setDmnHistoryEventProducer(DmnHistoryEventProducer dmnHistoryEventProducer) {
    this.dmnHistoryEventProducer = dmnHistoryEventProducer;
    return this;
  }

  public DmnHistoryEventProducer getDmnHistoryEventProducer() {
    return dmnHistoryEventProducer;
  }

  public Map<String, Class<? extends FormFieldValidator>> getCustomFormFieldValidators() {
    return customFormFieldValidators;
  }

  public void setCustomFormFieldValidators(Map<String, Class<? extends FormFieldValidator>> customFormFieldValidators) {
    this.customFormFieldValidators = customFormFieldValidators;
  }

  public void setFormValidators(FormValidators formValidators) {
    this.formValidators = formValidators;
  }

  public FormValidators getFormValidators() {
    return formValidators;
  }

  public ProcessEngineConfigurationImpl setDisableStrictOperatonFormParsing(boolean disableStrictOperatonFormParsing) {
    this.disableStrictOperatonFormParsing = disableStrictOperatonFormParsing;
    return this;
  }

  public boolean isDisableStrictOperatonFormParsing() {
    return disableStrictOperatonFormParsing;
  }

  public boolean isExecutionTreePrefetchEnabled() {
    return isExecutionTreePrefetchEnabled;
  }

  public void setExecutionTreePrefetchEnabled(boolean isExecutionTreePrefetchingEnabled) {
    this.isExecutionTreePrefetchEnabled = isExecutionTreePrefetchingEnabled;
  }

  public ProcessEngineImpl getProcessEngine() {
    return processEngine;
  }

  /**
   * If set to true, the process engine will save all script variables (created from Java Script, Groovy ...)
   * as process variables.
   */
  public void setAutoStoreScriptVariables(boolean autoStoreScriptVariables) {
    this.autoStoreScriptVariables = autoStoreScriptVariables;
  }

  /**
   * @return true if the process engine should save all script variables (created from Java Script, Groovy ...)
   * as process variables.
   */
  public boolean isAutoStoreScriptVariables() {
    return autoStoreScriptVariables;
  }

  /**
   * If set to true, the process engine will attempt to pre-compile script sources at runtime
   * to optimize script task execution performance.
   */
  public void setEnableScriptCompilation(boolean enableScriptCompilation) {
    this.enableScriptCompilation = enableScriptCompilation;
  }

  /**
   * @return true if compilation of script sources ins enabled. False otherwise.
   */
  public boolean isEnableScriptCompilation() {
    return enableScriptCompilation;
  }

  public boolean isEnableGracefulDegradationOnContextSwitchFailure() {
    return enableGracefulDegradationOnContextSwitchFailure;
  }

  /**
   * <p>If set to true, the process engine will tolerate certain exceptions that may result
   * from the fact that it cannot switch to the context of a process application that has made
   * a deployment.</p>
   * <p>
   * <p>Affects the following scenarios:</p>
   * <ul>
   * <li><b>Determining job priorities</b>: uses a default priority in case an expression fails to evaluate</li>
   * </ul>
   */
  public void setEnableGracefulDegradationOnContextSwitchFailure(boolean enableGracefulDegradationOnContextSwitchFailure) {
    this.enableGracefulDegradationOnContextSwitchFailure = enableGracefulDegradationOnContextSwitchFailure;
  }

  /**
   * @return true if the process engine acquires an exclusive lock when creating a deployment.
   */
  public boolean isDeploymentLockUsed() {
    return isDeploymentLockUsed;
  }

  /**
   * If set to true, the process engine will acquire an exclusive lock when creating a deployment.
   * This ensures that {@link DeploymentBuilder#enableDuplicateFiltering(boolean)} works correctly in a clustered environment.
   */
  public void setDeploymentLockUsed(boolean isDeploymentLockUsed) {
    this.isDeploymentLockUsed = isDeploymentLockUsed;
  }

  /**
   * @return true if deployment processing must be synchronized
   */
  public boolean isDeploymentSynchronized() {
    return isDeploymentSynchronized;
  }

  /**
   * Sets if deployment processing must be synchronized.
   * @param deploymentSynchronized {@code true} when deployment must be synchronized,
   * {@code false} when several depoloyments may be processed in parallel
   */
  public void setDeploymentSynchronized(boolean deploymentSynchronized) {
    isDeploymentSynchronized = deploymentSynchronized;
  }

  public boolean isCmmnEnabled() {
    return cmmnEnabled;
  }

  public void setCmmnEnabled(boolean cmmnEnabled) {
    this.cmmnEnabled = cmmnEnabled;
  }

  public boolean isDmnEnabled() {
    return dmnEnabled;
  }

  public void setDmnEnabled(boolean dmnEnabled) {
    this.dmnEnabled = dmnEnabled;
  }

  public boolean isStandaloneTasksEnabled() {
    return standaloneTasksEnabled;
  }

  public ProcessEngineConfigurationImpl setStandaloneTasksEnabled(boolean standaloneTasksEnabled) {
    this.standaloneTasksEnabled = standaloneTasksEnabled;
    return this;
  }

  public boolean isCompositeIncidentHandlersEnabled() {
    return isCompositeIncidentHandlersEnabled;
  }

  public ProcessEngineConfigurationImpl setCompositeIncidentHandlersEnabled(boolean compositeIncidentHandlersEnabled) {
    this.isCompositeIncidentHandlersEnabled = compositeIncidentHandlersEnabled;
    return this;
  }

  public ScriptFactory getScriptFactory() {
    return scriptFactory;
  }

  public ScriptingEnvironment getScriptingEnvironment() {
    return scriptingEnvironment;
  }

  public void setScriptFactory(ScriptFactory scriptFactory) {
    this.scriptFactory = scriptFactory;
  }

  public ScriptEngineResolver getScriptEngineResolver() {
    return scriptEngineResolver;
  }

  public ProcessEngineConfigurationImpl setScriptEngineResolver(ScriptEngineResolver scriptEngineResolver) {
    this.scriptEngineResolver = scriptEngineResolver;
    if (scriptingEngines != null) {
      scriptingEngines.setScriptEngineResolver(scriptEngineResolver);
    }
    return this;
  }

  public void setScriptingEnvironment(ScriptingEnvironment scriptingEnvironment) {
    this.scriptingEnvironment = scriptingEnvironment;
  }

  public List<ScriptEnvResolver> getEnvScriptResolvers() {
    return scriptEnvResolvers;
  }

  public void setEnvScriptResolvers(List<ScriptEnvResolver> scriptEnvResolvers) {
    this.scriptEnvResolvers = scriptEnvResolvers;
  }

  public String getScriptEngineNameJavaScript() {
    return scriptEngineNameJavaScript;
  }

  public ProcessEngineConfigurationImpl setScriptEngineNameJavaScript(String scriptEngineNameJavaScript) {
    this.scriptEngineNameJavaScript = scriptEngineNameJavaScript;
    return this;
  }

  public ProcessEngineConfiguration setArtifactFactory(ArtifactFactory artifactFactory) {
    this.artifactFactory = artifactFactory;
    return this;
  }

  public ArtifactFactory getArtifactFactory() {
    return artifactFactory;
  }

  public String getDefaultSerializationFormat() {
    return defaultSerializationFormat;
  }

  public ProcessEngineConfigurationImpl setDefaultSerializationFormat(String defaultSerializationFormat) {
    this.defaultSerializationFormat = defaultSerializationFormat;
    return this;
  }

  public boolean isJavaSerializationFormatEnabled() {
    return javaSerializationFormatEnabled;
  }

  public void setJavaSerializationFormatEnabled(boolean javaSerializationFormatEnabled) {
    this.javaSerializationFormatEnabled = javaSerializationFormatEnabled;
  }

  public ProcessEngineConfigurationImpl setDefaultCharsetName(String defaultCharsetName) {
    this.defaultCharsetName = defaultCharsetName;
    return this;
  }

  public ProcessEngineConfigurationImpl setDefaultCharset(Charset defautlCharset) {
    this.defaultCharset = defautlCharset;
    return this;
  }

  public Charset getDefaultCharset() {
    return defaultCharset;
  }

  public boolean isDbEntityCacheReuseEnabled() {
    return isDbEntityCacheReuseEnabled;
  }

  public ProcessEngineConfigurationImpl setDbEntityCacheReuseEnabled(boolean isDbEntityCacheReuseEnabled) {
    this.isDbEntityCacheReuseEnabled = isDbEntityCacheReuseEnabled;
    return this;
  }

  public DbEntityCacheKeyMapping getDbEntityCacheKeyMapping() {
    return dbEntityCacheKeyMapping;
  }

  public ProcessEngineConfigurationImpl setDbEntityCacheKeyMapping(DbEntityCacheKeyMapping dbEntityCacheKeyMapping) {
    this.dbEntityCacheKeyMapping = dbEntityCacheKeyMapping;
    return this;
  }

  public ProcessEngineConfigurationImpl setCustomHistoryLevels(List<HistoryLevel> customHistoryLevels) {
    this.customHistoryLevels = customHistoryLevels;
    return this;
  }

  public List<HistoryLevel> getHistoryLevels() {
    return historyLevels;
  }

  public List<HistoryLevel> getCustomHistoryLevels() {
    return customHistoryLevels;
  }

  public boolean isInvokeCustomVariableListeners() {
    return isInvokeCustomVariableListeners;
  }

  public ProcessEngineConfigurationImpl setInvokeCustomVariableListeners(boolean isInvokeCustomVariableListeners) {
    this.isInvokeCustomVariableListeners = isInvokeCustomVariableListeners;
    return this;
  }

  public void close() {
    if (forceCloseMybatisConnectionPool && dataSource instanceof PooledDataSource pooledDataSource) {
      // ACT-233: connection pool of Ibatis is not properely initialized if this is not called!
      pooledDataSource.forceCloseAll();
    }
  }

  public MetricsRegistry getMetricsRegistry() {
    return metricsRegistry;
  }

  public ProcessEngineConfigurationImpl setMetricsRegistry(MetricsRegistry metricsRegistry) {
    this.metricsRegistry = metricsRegistry;
    return this;
  }

  public ProcessEngineConfigurationImpl setMetricsEnabled(boolean isMetricsEnabled) {
    this.isMetricsEnabled = isMetricsEnabled;
    return this;
  }

  public boolean isMetricsEnabled() {
    return isMetricsEnabled;
  }

  public DbMetricsReporter getDbMetricsReporter() {
    return dbMetricsReporter;
  }

  public ProcessEngineConfigurationImpl setDbMetricsReporter(DbMetricsReporter dbMetricsReporter) {
    this.dbMetricsReporter = dbMetricsReporter;
    return this;
  }

  public boolean isDbMetricsReporterActivate() {
    return isDbMetricsReporterActivate;
  }

  public ProcessEngineConfigurationImpl setDbMetricsReporterActivate(boolean isDbMetricsReporterEnabled) {
    this.isDbMetricsReporterActivate = isDbMetricsReporterEnabled;
    return this;
  }

  /**
   * @deprecated Use {@link #getHostnameProvider()} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public MetricsReporterIdProvider getMetricsReporterIdProvider() {
    return metricsReporterIdProvider;
  }

  /**
   * @deprecated Use {@link #setHostnameProvider(HostnameProvider)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public ProcessEngineConfigurationImpl setMetricsReporterIdProvider(MetricsReporterIdProvider metricsReporterIdProvider) {
    this.metricsReporterIdProvider = metricsReporterIdProvider;
    return this;
  }

  public String getHostname() {
    return hostname;
  }

  public ProcessEngineConfigurationImpl setHostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

  public HostnameProvider getHostnameProvider() {
    return hostnameProvider;
  }

  public ProcessEngineConfigurationImpl setHostnameProvider(HostnameProvider hostnameProvider) {
    this.hostnameProvider = hostnameProvider;
    return this;
  }

  public boolean isTaskMetricsEnabled() {
    return isTaskMetricsEnabled;
  }

  public ProcessEngineConfigurationImpl setTaskMetricsEnabled(boolean isTaskMetricsEnabled) {
    this.isTaskMetricsEnabled = isTaskMetricsEnabled;
    return this;
  }

  public boolean isEnableScriptEngineCaching() {
    return enableScriptEngineCaching;
  }

  public ProcessEngineConfigurationImpl setEnableScriptEngineCaching(boolean enableScriptEngineCaching) {
    this.enableScriptEngineCaching = enableScriptEngineCaching;
    return this;
  }

  public boolean isEnableFetchScriptEngineFromProcessApplication() {
    return enableFetchScriptEngineFromProcessApplication;
  }

  public ProcessEngineConfigurationImpl setEnableFetchScriptEngineFromProcessApplication(boolean enable) {
    this.enableFetchScriptEngineFromProcessApplication = enable;
    return this;
  }

  public boolean isEnableScriptEngineLoadExternalResources() {
    return enableScriptEngineLoadExternalResources;
  }

  public ProcessEngineConfigurationImpl setEnableScriptEngineLoadExternalResources(boolean enableScriptEngineLoadExternalResources) {
    this.enableScriptEngineLoadExternalResources = enableScriptEngineLoadExternalResources;
    return this;
  }

  public boolean isEnableScriptEngineNashornCompatibility() {
    return enableScriptEngineNashornCompatibility;
  }

  public ProcessEngineConfigurationImpl setEnableScriptEngineNashornCompatibility(boolean enableScriptEngineNashornCompatibility) {
    this.enableScriptEngineNashornCompatibility = enableScriptEngineNashornCompatibility;
    return this;
  }

  public boolean isConfigureScriptEngineHostAccess() {
    return configureScriptEngineHostAccess;
  }

  public ProcessEngineConfigurationImpl setConfigureScriptEngineHostAccess(boolean configureScriptEngineHostAccess) {
    this.configureScriptEngineHostAccess = configureScriptEngineHostAccess;
    return this;
  }

  public boolean isEnableExpressionsInAdhocQueries() {
    return enableExpressionsInAdhocQueries;
  }

  public void setEnableExpressionsInAdhocQueries(boolean enableExpressionsInAdhocQueries) {
    this.enableExpressionsInAdhocQueries = enableExpressionsInAdhocQueries;
  }

  public boolean isEnableExpressionsInStoredQueries() {
    return enableExpressionsInStoredQueries;
  }

  public void setEnableExpressionsInStoredQueries(boolean enableExpressionsInStoredQueries) {
    this.enableExpressionsInStoredQueries = enableExpressionsInStoredQueries;
  }

  public boolean isEnableXxeProcessing() {
    return enableXxeProcessing;
  }

  public void setEnableXxeProcessing(boolean enableXxeProcessing) {
    this.enableXxeProcessing = enableXxeProcessing;
  }

  public ProcessEngineConfigurationImpl setBpmnStacktraceVerbose(boolean isBpmnStacktraceVerbose) {
    this.isBpmnStacktraceVerbose = isBpmnStacktraceVerbose;
    return this;
  }

  public boolean isBpmnStacktraceVerbose() {
    return this.isBpmnStacktraceVerbose;
  }

  public boolean isForceCloseMybatisConnectionPool() {
    return forceCloseMybatisConnectionPool;
  }

  public ProcessEngineConfigurationImpl setForceCloseMybatisConnectionPool(boolean forceCloseMybatisConnectionPool) {
    this.forceCloseMybatisConnectionPool = forceCloseMybatisConnectionPool;
    return this;
  }

  public boolean isRestrictUserOperationLogToAuthenticatedUsers() {
    return restrictUserOperationLogToAuthenticatedUsers;
  }

  public ProcessEngineConfigurationImpl setRestrictUserOperationLogToAuthenticatedUsers(boolean restrictUserOperationLogToAuthenticatedUsers) {
    this.restrictUserOperationLogToAuthenticatedUsers = restrictUserOperationLogToAuthenticatedUsers;
    return this;
  }

  public long getLogEntriesPerSyncOperationLimit() {
    return logEntriesPerSyncOperationLimit;
  }

  public ProcessEngineConfigurationImpl setLogEntriesPerSyncOperationLimit(long logEntriesPerSyncOperationLimit) {
    this.logEntriesPerSyncOperationLimit = logEntriesPerSyncOperationLimit;
    return this;
  }

  public ProcessEngineConfigurationImpl setTenantIdProvider(TenantIdProvider tenantIdProvider) {
    this.tenantIdProvider = tenantIdProvider;
    return this;
  }

  public TenantIdProvider getTenantIdProvider() {
    return this.tenantIdProvider;
  }

  public void setMigrationActivityMatcher(MigrationActivityMatcher migrationActivityMatcher) {
    this.migrationActivityMatcher = migrationActivityMatcher;
  }

  public MigrationActivityMatcher getMigrationActivityMatcher() {
    return migrationActivityMatcher;
  }


  public void setCustomPreMigrationActivityValidators(List<MigrationActivityValidator> customPreMigrationActivityValidators) {
    this.customPreMigrationActivityValidators = customPreMigrationActivityValidators;
  }

  public List<MigrationActivityValidator> getCustomPreMigrationActivityValidators() {
    return customPreMigrationActivityValidators;
  }

  public void setCustomPostMigrationActivityValidators(List<MigrationActivityValidator> customPostMigrationActivityValidators) {
    this.customPostMigrationActivityValidators = customPostMigrationActivityValidators;
  }

  public List<MigrationActivityValidator> getCustomPostMigrationActivityValidators() {
    return customPostMigrationActivityValidators;
  }

  public List<MigrationActivityValidator> getDefaultMigrationActivityValidators() {
    List<MigrationActivityValidator> migrationActivityValidators = new ArrayList<>();
    migrationActivityValidators.add(SupportedActivityValidator.INSTANCE);
    migrationActivityValidators.add(SupportedPassiveEventTriggerActivityValidator.INSTANCE);
    migrationActivityValidators.add(NoCompensationHandlerActivityValidator.INSTANCE);
    return migrationActivityValidators;
  }

  public void setMigrationInstructionGenerator(MigrationInstructionGenerator migrationInstructionGenerator) {
    this.migrationInstructionGenerator = migrationInstructionGenerator;
  }

  public MigrationInstructionGenerator getMigrationInstructionGenerator() {
    return migrationInstructionGenerator;
  }

  public void setMigrationInstructionValidators(List<MigrationInstructionValidator> migrationInstructionValidators) {
    this.migrationInstructionValidators = migrationInstructionValidators;
  }

  public List<MigrationInstructionValidator> getMigrationInstructionValidators() {
    return migrationInstructionValidators;
  }

  public void setCustomPostMigrationInstructionValidators(List<MigrationInstructionValidator> customPostMigrationInstructionValidators) {
    this.customPostMigrationInstructionValidators = customPostMigrationInstructionValidators;
  }

  public List<MigrationInstructionValidator> getCustomPostMigrationInstructionValidators() {
    return customPostMigrationInstructionValidators;
  }

  public void setCustomPreMigrationInstructionValidators(List<MigrationInstructionValidator> customPreMigrationInstructionValidators) {
    this.customPreMigrationInstructionValidators = customPreMigrationInstructionValidators;
  }

  public List<MigrationInstructionValidator> getCustomPreMigrationInstructionValidators() {
    return customPreMigrationInstructionValidators;

  }

  public List<MigrationInstructionValidator> getDefaultMigrationInstructionValidators() {
    List<MigrationInstructionValidator> validators = new ArrayList<>();
    validators.add(new SameBehaviorInstructionValidator());
    validators.add(new SameEventTypeValidator());
    validators.add(new OnlyOnceMappedActivityInstructionValidator());
    validators.add(new CannotAddMultiInstanceBodyValidator());
    validators.add(new CannotAddMultiInstanceInnerActivityValidator());
    validators.add(new CannotRemoveMultiInstanceInnerActivityValidator());
    validators.add(new GatewayMappingValidator());
    validators.add(new SameEventScopeInstructionValidator());
    validators.add(new UpdateEventTriggersValidator());
    validators.add(new AdditionalFlowScopeInstructionValidator());
    validators.add(new ConditionalEventUpdateEventTriggerValidator());
    return validators;
  }

  public void setMigratingActivityInstanceValidators(List<MigratingActivityInstanceValidator> migratingActivityInstanceValidators) {
    this.migratingActivityInstanceValidators = migratingActivityInstanceValidators;
  }

  public List<MigratingActivityInstanceValidator> getMigratingActivityInstanceValidators() {
    return migratingActivityInstanceValidators;
  }

  public void setCustomPostMigratingActivityInstanceValidators(List<MigratingActivityInstanceValidator> customPostMigratingActivityInstanceValidators) {
    this.customPostMigratingActivityInstanceValidators = customPostMigratingActivityInstanceValidators;
  }

  public List<MigratingActivityInstanceValidator> getCustomPostMigratingActivityInstanceValidators() {
    return customPostMigratingActivityInstanceValidators;
  }

  public void setCustomPreMigratingActivityInstanceValidators(List<MigratingActivityInstanceValidator> customPreMigratingActivityInstanceValidators) {
    this.customPreMigratingActivityInstanceValidators = customPreMigratingActivityInstanceValidators;
  }

  public List<MigratingActivityInstanceValidator> getCustomPreMigratingActivityInstanceValidators() {
    return customPreMigratingActivityInstanceValidators;
  }

  public List<MigratingTransitionInstanceValidator> getMigratingTransitionInstanceValidators() {
    return migratingTransitionInstanceValidators;
  }

  public List<MigratingCompensationInstanceValidator> getMigratingCompensationInstanceValidators() {
    return migratingCompensationInstanceValidators;
  }

  public List<MigratingActivityInstanceValidator> getDefaultMigratingActivityInstanceValidators() {
    List<MigratingActivityInstanceValidator> validators = new ArrayList<>();

    validators.add(new NoUnmappedLeafInstanceValidator());
    validators.add(new VariableConflictActivityInstanceValidator());
    validators.add(new SupportedActivityInstanceValidator());

    return validators;
  }

  public List<MigratingTransitionInstanceValidator> getDefaultMigratingTransitionInstanceValidators() {
    List<MigratingTransitionInstanceValidator> validators = new ArrayList<>();

    validators.add(new NoUnmappedLeafInstanceValidator());
    validators.add(new AsyncAfterMigrationValidator());
    validators.add(new AsyncProcessStartMigrationValidator());
    validators.add(new AsyncMigrationValidator());

    return validators;
  }

  public List<CommandChecker> getCommandCheckers() {
    return commandCheckers;
  }

  public void setCommandCheckers(List<CommandChecker> commandCheckers) {
    this.commandCheckers = commandCheckers;
  }

  public ProcessEngineConfigurationImpl setUseSharedSqlSessionFactory(boolean isUseSharedSqlSessionFactory) {
    this.isUseSharedSqlSessionFactory = isUseSharedSqlSessionFactory;
    return this;
  }

  public boolean isUseSharedSqlSessionFactory() {
    return isUseSharedSqlSessionFactory;
  }

  public boolean getDisableStrictCallActivityValidation() {
    return disableStrictCallActivityValidation;
  }

  public void setDisableStrictCallActivityValidation(boolean disableStrictCallActivityValidation) {
    this.disableStrictCallActivityValidation = disableStrictCallActivityValidation;
  }

  public String getHistoryCleanupBatchWindowStartTime() {
    return historyCleanupBatchWindowStartTime;
  }

  public void setHistoryCleanupBatchWindowStartTime(String historyCleanupBatchWindowStartTime) {
    this.historyCleanupBatchWindowStartTime = historyCleanupBatchWindowStartTime;
  }

  public String getHistoryCleanupBatchWindowEndTime() {
    return historyCleanupBatchWindowEndTime;
  }

  public void setHistoryCleanupBatchWindowEndTime(String historyCleanupBatchWindowEndTime) {
    this.historyCleanupBatchWindowEndTime = historyCleanupBatchWindowEndTime;
  }

  public String getMondayHistoryCleanupBatchWindowStartTime() {
    return mondayHistoryCleanupBatchWindowStartTime;
  }

  public void setMondayHistoryCleanupBatchWindowStartTime(String mondayHistoryCleanupBatchWindowStartTime) {
    this.mondayHistoryCleanupBatchWindowStartTime = mondayHistoryCleanupBatchWindowStartTime;
  }

  public String getMondayHistoryCleanupBatchWindowEndTime() {
    return mondayHistoryCleanupBatchWindowEndTime;
  }

  public void setMondayHistoryCleanupBatchWindowEndTime(String mondayHistoryCleanupBatchWindowEndTime) {
    this.mondayHistoryCleanupBatchWindowEndTime = mondayHistoryCleanupBatchWindowEndTime;
  }

  public String getTuesdayHistoryCleanupBatchWindowStartTime() {
    return tuesdayHistoryCleanupBatchWindowStartTime;
  }

  public void setTuesdayHistoryCleanupBatchWindowStartTime(String tuesdayHistoryCleanupBatchWindowStartTime) {
    this.tuesdayHistoryCleanupBatchWindowStartTime = tuesdayHistoryCleanupBatchWindowStartTime;
  }

  public String getTuesdayHistoryCleanupBatchWindowEndTime() {
    return tuesdayHistoryCleanupBatchWindowEndTime;
  }

  public void setTuesdayHistoryCleanupBatchWindowEndTime(String tuesdayHistoryCleanupBatchWindowEndTime) {
    this.tuesdayHistoryCleanupBatchWindowEndTime = tuesdayHistoryCleanupBatchWindowEndTime;
  }

  public String getWednesdayHistoryCleanupBatchWindowStartTime() {
    return wednesdayHistoryCleanupBatchWindowStartTime;
  }

  public void setWednesdayHistoryCleanupBatchWindowStartTime(String wednesdayHistoryCleanupBatchWindowStartTime) {
    this.wednesdayHistoryCleanupBatchWindowStartTime = wednesdayHistoryCleanupBatchWindowStartTime;
  }

  public String getWednesdayHistoryCleanupBatchWindowEndTime() {
    return wednesdayHistoryCleanupBatchWindowEndTime;
  }

  public void setWednesdayHistoryCleanupBatchWindowEndTime(String wednesdayHistoryCleanupBatchWindowEndTime) {
    this.wednesdayHistoryCleanupBatchWindowEndTime = wednesdayHistoryCleanupBatchWindowEndTime;
  }

  public String getThursdayHistoryCleanupBatchWindowStartTime() {
    return thursdayHistoryCleanupBatchWindowStartTime;
  }

  public void setThursdayHistoryCleanupBatchWindowStartTime(String thursdayHistoryCleanupBatchWindowStartTime) {
    this.thursdayHistoryCleanupBatchWindowStartTime = thursdayHistoryCleanupBatchWindowStartTime;
  }

  public String getThursdayHistoryCleanupBatchWindowEndTime() {
    return thursdayHistoryCleanupBatchWindowEndTime;
  }

  public void setThursdayHistoryCleanupBatchWindowEndTime(String thursdayHistoryCleanupBatchWindowEndTime) {
    this.thursdayHistoryCleanupBatchWindowEndTime = thursdayHistoryCleanupBatchWindowEndTime;
  }

  public String getFridayHistoryCleanupBatchWindowStartTime() {
    return fridayHistoryCleanupBatchWindowStartTime;
  }

  public void setFridayHistoryCleanupBatchWindowStartTime(String fridayHistoryCleanupBatchWindowStartTime) {
    this.fridayHistoryCleanupBatchWindowStartTime = fridayHistoryCleanupBatchWindowStartTime;
  }

  public String getFridayHistoryCleanupBatchWindowEndTime() {
    return fridayHistoryCleanupBatchWindowEndTime;
  }

  public void setFridayHistoryCleanupBatchWindowEndTime(String fridayHistoryCleanupBatchWindowEndTime) {
    this.fridayHistoryCleanupBatchWindowEndTime = fridayHistoryCleanupBatchWindowEndTime;
  }

  public String getSaturdayHistoryCleanupBatchWindowStartTime() {
    return saturdayHistoryCleanupBatchWindowStartTime;
  }

  public void setSaturdayHistoryCleanupBatchWindowStartTime(String saturdayHistoryCleanupBatchWindowStartTime) {
    this.saturdayHistoryCleanupBatchWindowStartTime = saturdayHistoryCleanupBatchWindowStartTime;
  }

  public String getSaturdayHistoryCleanupBatchWindowEndTime() {
    return saturdayHistoryCleanupBatchWindowEndTime;
  }

  public void setSaturdayHistoryCleanupBatchWindowEndTime(String saturdayHistoryCleanupBatchWindowEndTime) {
    this.saturdayHistoryCleanupBatchWindowEndTime = saturdayHistoryCleanupBatchWindowEndTime;
  }

  public String getSundayHistoryCleanupBatchWindowStartTime() {
    return sundayHistoryCleanupBatchWindowStartTime;
  }

  public void setSundayHistoryCleanupBatchWindowStartTime(String sundayHistoryCleanupBatchWindowStartTime) {
    this.sundayHistoryCleanupBatchWindowStartTime = sundayHistoryCleanupBatchWindowStartTime;
  }

  public String getSundayHistoryCleanupBatchWindowEndTime() {
    return sundayHistoryCleanupBatchWindowEndTime;
  }

  public void setSundayHistoryCleanupBatchWindowEndTime(String sundayHistoryCleanupBatchWindowEndTime) {
    this.sundayHistoryCleanupBatchWindowEndTime = sundayHistoryCleanupBatchWindowEndTime;
  }

  public Date getHistoryCleanupBatchWindowStartTimeAsDate() {
    return historyCleanupBatchWindowStartTimeAsDate;
  }

  public void setHistoryCleanupBatchWindowStartTimeAsDate(Date historyCleanupBatchWindowStartTimeAsDate) {
    this.historyCleanupBatchWindowStartTimeAsDate = historyCleanupBatchWindowStartTimeAsDate;
  }

  public void setHistoryCleanupBatchWindowEndTimeAsDate(Date historyCleanupBatchWindowEndTimeAsDate) {
    this.historyCleanupBatchWindowEndTimeAsDate = historyCleanupBatchWindowEndTimeAsDate;
  }

  public Date getHistoryCleanupBatchWindowEndTimeAsDate() {
    return historyCleanupBatchWindowEndTimeAsDate;
  }

  public Map<Integer, BatchWindowConfiguration> getHistoryCleanupBatchWindows() {
    return historyCleanupBatchWindows;
  }

  public void setHistoryCleanupBatchWindows(Map<Integer, BatchWindowConfiguration> historyCleanupBatchWindows) {
    this.historyCleanupBatchWindows = historyCleanupBatchWindows;
  }

  public int getHistoryCleanupBatchSize() {
    return historyCleanupBatchSize;
  }

  public void setHistoryCleanupBatchSize(int historyCleanupBatchSize) {
    this.historyCleanupBatchSize = historyCleanupBatchSize;
  }

  public int getHistoryCleanupBatchThreshold() {
    return historyCleanupBatchThreshold;
  }

  public void setHistoryCleanupBatchThreshold(int historyCleanupBatchThreshold) {
    this.historyCleanupBatchThreshold = historyCleanupBatchThreshold;
  }

  /**
   * Checks if history cleanup metrics are enabled.
   * This method returns {@code true} only if both {@code historyCleanupMetricsEnabled} and
   * {@code isMetricsEnabled} are true
   *
   * @return {@code true} if both history cleanup metrics and general metrics are enabled,
   *         {@code false} otherwise.
   */
  public boolean isHistoryCleanupMetricsEnabled() {
    return historyCleanupMetricsEnabled && isMetricsEnabled;
  }

  public void setHistoryCleanupMetricsEnabled(boolean historyCleanupMetricsEnabled) {
    this.historyCleanupMetricsEnabled = historyCleanupMetricsEnabled;
  }

  public boolean isHistoryCleanupEnabled() {
    return historyCleanupEnabled;
  }

  public ProcessEngineConfigurationImpl setHistoryCleanupEnabled(boolean historyCleanupEnabled) {
    this.historyCleanupEnabled = historyCleanupEnabled;
    return this;
  }

  public String getHistoryTimeToLive() {
    return historyTimeToLive;
  }

  public void setHistoryTimeToLive(String historyTimeToLive) {
    this.historyTimeToLive = historyTimeToLive;
  }

  public boolean isEnforceHistoryTimeToLive() {
    return enforceHistoryTimeToLive;
  }

  public ProcessEngineConfigurationImpl setEnforceHistoryTimeToLive(boolean enforceHistoryTimeToLive) {
    this.enforceHistoryTimeToLive = enforceHistoryTimeToLive;
    return this;
  }

  public ProcessEngineConfigurationImpl setJobExecutorAcquireExclusiveOverProcessHierarchies(boolean jobExecutorAcquireExclusiveOverProcessHierarchies) {
    this.jobExecutorAcquireExclusiveOverProcessHierarchies = jobExecutorAcquireExclusiveOverProcessHierarchies;
    return this;
  }

  public boolean isJobExecutorAcquireExclusiveOverProcessHierarchies() {
    return this.jobExecutorAcquireExclusiveOverProcessHierarchies;
  }

  public String getBatchOperationHistoryTimeToLive() {
    return batchOperationHistoryTimeToLive;
  }

  public int getHistoryCleanupDegreeOfParallelism() {
    return historyCleanupDegreeOfParallelism;
  }

  public void setHistoryCleanupDegreeOfParallelism(int historyCleanupDegreeOfParallelism) {
    this.historyCleanupDegreeOfParallelism = historyCleanupDegreeOfParallelism;
  }

  public void setBatchOperationHistoryTimeToLive(String batchOperationHistoryTimeToLive) {
    this.batchOperationHistoryTimeToLive = batchOperationHistoryTimeToLive;
  }

  public Map<String, String> getBatchOperationsForHistoryCleanup() {
    return batchOperationsForHistoryCleanup;
  }

  public void setBatchOperationsForHistoryCleanup(Map<String, String> batchOperationsForHistoryCleanup) {
    this.batchOperationsForHistoryCleanup = batchOperationsForHistoryCleanup;
  }

  public Map<String, Integer> getParsedBatchOperationsForHistoryCleanup() {
    return parsedBatchOperationsForHistoryCleanup;
  }

  public void setParsedBatchOperationsForHistoryCleanup(Map<String, Integer> parsedBatchOperationsForHistoryCleanup) {
    this.parsedBatchOperationsForHistoryCleanup = parsedBatchOperationsForHistoryCleanup;
  }

  public String getHistoryCleanupJobLogTimeToLive() {
    return historyCleanupJobLogTimeToLive;
  }

  public ProcessEngineConfigurationImpl setHistoryCleanupJobLogTimeToLive(String historyCleanupJobLogTimeToLive) {
    this.historyCleanupJobLogTimeToLive = historyCleanupJobLogTimeToLive;
    return this;
  }

  public String getTaskMetricsTimeToLive() {
    return taskMetricsTimeToLive;
  }

  public ProcessEngineConfigurationImpl setTaskMetricsTimeToLive(String taskMetricsTimeToLive) {
    this.taskMetricsTimeToLive = taskMetricsTimeToLive;
    return this;
  }

  public Integer getParsedTaskMetricsTimeToLive() {
    return parsedTaskMetricsTimeToLive;
  }

  public ProcessEngineConfigurationImpl setParsedTaskMetricsTimeToLive(Integer parsedTaskMetricsTimeToLive) {
    this.parsedTaskMetricsTimeToLive = parsedTaskMetricsTimeToLive;
    return this;
  }

  public BatchWindowManager getBatchWindowManager() {
    return batchWindowManager;
  }

  public void setBatchWindowManager(BatchWindowManager batchWindowManager) {
    this.batchWindowManager = batchWindowManager;
  }

  public HistoryRemovalTimeProvider getHistoryRemovalTimeProvider() {
    return historyRemovalTimeProvider;
  }

  public ProcessEngineConfigurationImpl setHistoryRemovalTimeProvider(HistoryRemovalTimeProvider removalTimeProvider) {
    historyRemovalTimeProvider = removalTimeProvider;
    return this;
  }

  public String getHistoryRemovalTimeStrategy() {
    return historyRemovalTimeStrategy;
  }

  public ProcessEngineConfigurationImpl setHistoryRemovalTimeStrategy(String removalTimeStrategy) {
    historyRemovalTimeStrategy = removalTimeStrategy;
    return this;
  }

  public String getHistoryCleanupStrategy() {
    return historyCleanupStrategy;
  }

  public ProcessEngineConfigurationImpl setHistoryCleanupStrategy(String historyCleanupStrategy) {
    this.historyCleanupStrategy = historyCleanupStrategy;
    return this;
  }

  public int getFailedJobListenerMaxRetries() {
    return failedJobListenerMaxRetries;
  }

  public void setFailedJobListenerMaxRetries(int failedJobListenerMaxRetries) {
    this.failedJobListenerMaxRetries = failedJobListenerMaxRetries;
  }

  public String getFailedJobRetryTimeCycle() {
    return failedJobRetryTimeCycle;
  }

  public void setFailedJobRetryTimeCycle(String failedJobRetryTimeCycle) {
    this.failedJobRetryTimeCycle = failedJobRetryTimeCycle;
  }

  public int getLoginMaxAttempts() {
    return loginMaxAttempts;
  }

  public void setLoginMaxAttempts(int loginMaxAttempts) {
    this.loginMaxAttempts = loginMaxAttempts;
  }

  public int getLoginDelayFactor() {
    return loginDelayFactor;
  }

  public void setLoginDelayFactor(int loginDelayFactor) {
    this.loginDelayFactor = loginDelayFactor;
  }

  public int getLoginDelayMaxTime() {
    return loginDelayMaxTime;
  }

  public void setLoginDelayMaxTime(int loginDelayMaxTime) {
    this.loginDelayMaxTime = loginDelayMaxTime;
  }

  public int getLoginDelayBase() {
    return loginDelayBase;
  }

  public void setLoginDelayBase(int loginInitialDelay) {
    this.loginDelayBase = loginInitialDelay;
  }

  public boolean isWebappsAuthenticationLoggingEnabled() {
    return webappsAuthenticationLoggingEnabled;
  }

  public void setWebappsAuthenticationLoggingEnabled(boolean webappsAuthenticationLoggingEnabled) {
    this.webappsAuthenticationLoggingEnabled = webappsAuthenticationLoggingEnabled;
  }

  public List<String> getAdminGroups() {
    return adminGroups;
  }

  public void setAdminGroups(List<String> adminGroups) {
    this.adminGroups = adminGroups;
  }

  public List<String> getAdminUsers() {
    return adminUsers;
  }

  public void setAdminUsers(List<String> adminUsers) {
    this.adminUsers = adminUsers;
  }

  public int getQueryMaxResultsLimit() {
    return queryMaxResultsLimit;
  }

  public ProcessEngineConfigurationImpl setQueryMaxResultsLimit(int queryMaxResultsLimit) {
    this.queryMaxResultsLimit = queryMaxResultsLimit;
    return this;
  }

  public String getLoggingContextActivityId() {
    return loggingContextActivityId;
  }

  public ProcessEngineConfigurationImpl setLoggingContextActivityId(String loggingContextActivityId) {
    this.loggingContextActivityId = loggingContextActivityId;
    return this;
  }

  public String getLoggingContextActivityName() {
    return loggingContextActivityName;
  }

  public ProcessEngineConfigurationImpl setLoggingContextActivityName(final String loggingContextActivityName) {
    this.loggingContextActivityName = loggingContextActivityName;
    return this;
  }

  public String getLoggingContextApplicationName() {
    return loggingContextApplicationName;
  }

  public ProcessEngineConfigurationImpl setLoggingContextApplicationName(String loggingContextApplicationName) {
    this.loggingContextApplicationName = loggingContextApplicationName;
    return this;
  }

  public String getLoggingContextBusinessKey() {
    return loggingContextBusinessKey;
  }

  public ProcessEngineConfigurationImpl setLoggingContextBusinessKey(String loggingContextBusinessKey) {
    this.loggingContextBusinessKey = loggingContextBusinessKey;
    return this;
  }

  public String getLoggingContextProcessDefinitionId() {
    return loggingContextProcessDefinitionId;
  }

  public ProcessEngineConfigurationImpl setLoggingContextProcessDefinitionId(String loggingContextProcessDefinitionId) {
    this.loggingContextProcessDefinitionId = loggingContextProcessDefinitionId;
    return this;
  }

  public String getLoggingContextProcessDefinitionKey() {
    return loggingContextProcessDefinitionKey;
  }

  public ProcessEngineConfigurationImpl setLoggingContextProcessDefinitionKey(String loggingContextProcessDefinitionKey) {
    this.loggingContextProcessDefinitionKey = loggingContextProcessDefinitionKey;
    return this;
  }

  public String getLoggingContextProcessInstanceId() {
    return loggingContextProcessInstanceId;
  }

  public ProcessEngineConfigurationImpl setLoggingContextProcessInstanceId(String loggingContextProcessInstanceId) {
    this.loggingContextProcessInstanceId = loggingContextProcessInstanceId;
    return this;
  }

  public String getLoggingContextTenantId() {
    return loggingContextTenantId;
  }

  public ProcessEngineConfigurationImpl setLoggingContextTenantId(String loggingContextTenantId) {
    this.loggingContextTenantId = loggingContextTenantId;
    return this;
  }

  public String getLoggingContextEngineName() {
    return loggingContextEngineName;
  }

  public ProcessEngineConfigurationImpl setLoggingContextEngineName(String loggingContextEngineName) {
    this.loggingContextEngineName = loggingContextEngineName;
    return this;
  }

  public String getLogLevelBpmnStackTrace() {
    return logLevelBpmnStackTrace;
  }

  public ProcessEngineConfigurationImpl setLogLevelBpmnStackTrace(final String logLevelBpmnStackTrace) {
    this.logLevelBpmnStackTrace = logLevelBpmnStackTrace;
    return this;
  }

  public boolean isEnableOptimisticLockingOnForeignKeyViolation() {
    return enableOptimisticLockingOnForeignKeyViolation;
  }

  public ProcessEngineConfigurationImpl setEnableOptimisticLockingOnForeignKeyViolation(boolean enableOptimisticLockingOnForeignKeyViolation) {
    this.enableOptimisticLockingOnForeignKeyViolation = enableOptimisticLockingOnForeignKeyViolation;
    return this;
  }

  public List<FeelCustomFunctionProvider> getDmnFeelCustomFunctionProviders() {
    return dmnFeelCustomFunctionProviders;
  }

  public ProcessEngineConfigurationImpl setDmnFeelCustomFunctionProviders(List<FeelCustomFunctionProvider> dmnFeelCustomFunctionProviders) {
    this.dmnFeelCustomFunctionProviders = dmnFeelCustomFunctionProviders;
    return this;
  }

  public boolean isDmnFeelEnableLegacyBehavior() {
    return dmnFeelEnableLegacyBehavior;
  }

  public ProcessEngineConfigurationImpl setDmnFeelEnableLegacyBehavior(boolean dmnFeelEnableLegacyBehavior) {
    this.dmnFeelEnableLegacyBehavior = dmnFeelEnableLegacyBehavior;
    return this;
  }

  public boolean isDmnReturnBlankTableOutputAsNull() {
    return dmnReturnBlankTableOutputAsNull;
  }

  public ProcessEngineConfigurationImpl setDmnReturnBlankTableOutputAsNull(boolean dmnReturnBlankTableOutputAsNull) {
    this.dmnReturnBlankTableOutputAsNull = dmnReturnBlankTableOutputAsNull;
    return this;
  }

  public DiagnosticsCollector getDiagnosticsCollector() {
    return diagnosticsCollector;
  }
  public void setDiagnosticsCollector(DiagnosticsCollector diagnosticsCollector) {
    this.diagnosticsCollector = diagnosticsCollector;
  }

  public TelemetryDataImpl getTelemetryData() {
    return telemetryData;
  }

  public ProcessEngineConfigurationImpl setTelemetryData(TelemetryDataImpl telemetryData) {
    this.telemetryData = telemetryData;
    return this;
  }

  public int getTelemetryRequestTimeout() {
    return telemetryRequestTimeout;
  }

  public ProcessEngineConfigurationImpl setTelemetryRequestTimeout(int telemetryRequestTimeout) {
    this.telemetryRequestTimeout = telemetryRequestTimeout;
    return this;
  }

  public boolean isReevaluateTimeCycleWhenDue() {
    return reevaluateTimeCycleWhenDue;
  }

  public ProcessEngineConfigurationImpl setReevaluateTimeCycleWhenDue(boolean reevaluateTimeCycleWhenDue) {
    this.reevaluateTimeCycleWhenDue = reevaluateTimeCycleWhenDue;
    return this;
  }

  public int getRemovalTimeUpdateChunkSize() {
    return removalTimeUpdateChunkSize;
  }

  public ProcessEngineConfigurationImpl setRemovalTimeUpdateChunkSize(int removalTimeUpdateChunkSize) {
    this.removalTimeUpdateChunkSize = removalTimeUpdateChunkSize;
    return this;
  }

  /**
   * @return a exception code interceptor. The interceptor is not registered in case
   * {@code disableExceptionCode} is configured to {@code true}.
   */
  protected ExceptionCodeInterceptor getExceptionCodeInterceptor() {
    return new ExceptionCodeInterceptor(builtinExceptionCodeProvider, customExceptionCodeProvider);
  }

  public DiagnosticsRegistry getDiagnosticsRegistry() {
    return diagnosticsRegistry;
  }

  public ProcessEngineConfiguration setDiagnosticsRegistry(DiagnosticsRegistry diagnosticsRegistry) {
    this.diagnosticsRegistry = diagnosticsRegistry;
    return this;
  }
}
