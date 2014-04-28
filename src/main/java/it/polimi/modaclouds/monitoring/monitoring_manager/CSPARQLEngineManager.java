/**
 * Copyright 2014 deib-polimi
 * Contact: deib-polimi <marco.miglierina@polimi.it>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package it.polimi.modaclouds.monitoring.monitoring_manager;

import it.polimi.csparqool.CSquery;
import it.polimi.csparqool.Function;
import it.polimi.csparqool.FunctionParameter;
import it.polimi.csparqool.MalformedQueryException;
import it.polimi.csparqool._body;
import it.polimi.csparqool._graph;
import it.polimi.csparqool.body;
import it.polimi.csparqool.graph;
import it.polimi.modaclouds.qos_models.monitoring_ontology.MO;
import it.polimi.modaclouds.qos_models.monitoring_ontology.Vocabulary;
import it.polimi.modaclouds.qos_models.schema.Action;
import it.polimi.modaclouds.qos_models.schema.MonitoredTarget;
import it.polimi.modaclouds.qos_models.schema.MonitoringRule;
import it.polimi.modaclouds.qos_models.schema.Parameter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import polimi.deib.csparql_rest_api.RSP_services_csparql_API;
import polimi.deib.csparql_rest_api.exception.ObserverErrorException;
import polimi.deib.csparql_rest_api.exception.QueryErrorException;
import polimi.deib.csparql_rest_api.exception.ServerErrorException;
import polimi.deib.csparql_rest_api.exception.StreamErrorException;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.util.QueryUtils;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class CSPARQLEngineManager {

	private Logger logger = LoggerFactory.getLogger(CSPARQLEngineManager.class
			.getName());
	private DatasetAccessor da = DatasetAccessorFactory.createHTTP(MO
			.getKnowledgeBaseDataURL());
	private URL ddaURL;
	private URL sdaURL;
	private RSP_services_csparql_API csparqlAPI;

	private Map<String, String> registeredQueries;
	private List<String> registeredStreams;
	private Map<String, List<String>> ruleQueriesMap;

	// private RuleValidator validator;

	public CSPARQLEngineManager() throws ConfigurationException {
		try {
			loadConfig();
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
		registeredStreams = new ArrayList<String>();
		registeredQueries = new HashMap<String, String>();
		ruleQueriesMap = new HashMap<String, List<String>>();
		// validator = new RuleValidator();
		csparqlAPI = new RSP_services_csparql_API(ddaURL.toString());
	}

	private void loadConfig() throws MalformedURLException,
			ConfigurationException {
		Config config = Config.getInstance();
		ddaURL = createURL(config.getDDAServerAddress(),
				config.getDDAServerPort());
		sdaURL = createURL(config.getSDAServerAddress(),
				config.getSDAServerPort());
	}

	private URL createURL(String address, int port)
			throws MalformedURLException {
		return new URL("http://" + cleanAddress(address) + ":" + port);
	}

	private String cleanAddress(String address) {
		if (address.indexOf("://") != -1)
			address = address.substring(address.indexOf("://") + 3);
		if (address.endsWith("/"))
			address = address.substring(0, address.length() - 1);
		return address;
	}

	public void installRule(MonitoringRule rule, boolean sdaRequired,
			String sdaReturnedMetric) throws RuleInstallationException {
		try {
			String metricName = sdaRequired ? sdaReturnedMetric : rule
					.getMetricName();

			String queryName = getNewQueryName(rule, null);
			String sourceStreamURI = getSourceStreamURI(metricName);
			CSquery query = createActionQuery(rule, queryName, sourceStreamURI,
					sdaRequired);
			String csparqlQuery = query.getCSPARQL();
			logger.info("Query generated:\n" + csparqlQuery);

			if (sdaRequired) {
				String tunnelQueryName = getNewQueryName(rule, "Tunnel");
				String tunnelSourceStreamURI = getSourceStreamURI(rule
						.getMetricName());
				CSquery tunnelQuery = createTunnelQuery(rule, tunnelQueryName,
						tunnelSourceStreamURI);
				String csparqlTunnelQuery = tunnelQuery.getCSPARQL();
				logger.info("Tunnel query generated:\n" + csparqlTunnelQuery);

				registerStream(tunnelSourceStreamURI);
				String tunnelQueryURI = registerQuery(tunnelQueryName,
						csparqlTunnelQuery, rule);
				attachObserver(tunnelQueryURI, sdaURL);
			}

			registerStream(sourceStreamURI);
			registerQuery(queryName, csparqlQuery, rule);

		} catch (QueryErrorException | MalformedQueryException e) {
			logger.error("Internal error", e);
			throw new RuleInstallationException("Internal error", e);
		} catch (ServerErrorException e) {
			logger.error("Connection to the DDA server failed", e);
			throw new RuleInstallationException(
					"Connection to the DDA server failed", e);
		} catch (ObserverErrorException e) {
			logger.error("Connection to the SDA server failed", e);
			throw new RuleInstallationException(
					"Connection to the SDA server failed", e);
		}
	}

	private void attachObserver(String queryURI, URL url)
			throws ServerErrorException, ObserverErrorException {
		csparqlAPI.addObserver(queryURI, url.toString());
	}

	private String registerQuery(String queryName, String csparqlQuery,
			MonitoringRule rule) throws ServerErrorException,
			QueryErrorException {
		String queryURI = csparqlAPI.registerQuery(queryName, csparqlQuery);
		logger.info("Server response, query ID: " + queryURI);
		registeredQueries.put(queryURI, csparqlQuery);
		List<String> queriesURIs = ruleQueriesMap.get(rule.getId());
		if (queriesURIs == null) {
			queriesURIs = new ArrayList<String>();
			ruleQueriesMap.put(rule.getId(), queriesURIs);
		}
		queriesURIs.add(queryURI);
		return queryURI;
	}

	private void registerStream(String streamURI)
			throws RuleInstallationException {
		if (!registeredStreams.contains(streamURI)) {
			logger.info("Registering stream: " + streamURI);
			String response;
			boolean registered = false;
			try {
				response = csparqlAPI.registerStream(streamURI);
				logger.info("Server response: " + response);
				registered = true;
			} catch (Exception e) {
				if (e.getMessage().contains("already exists")) {
					registered = true;
					logger.info("Stream already exists");
				}
			}
			if (!registered)
				throw new RuleInstallationException(
						"Could not register stream " + streamURI);
			registeredStreams.add(streamURI);
		}
	}

	private CSquery createActionQuery(MonitoringRule rule, String queryName,
			String sourceStreamURI, boolean sdaRequired)
			throws MalformedQueryException, RuleInstallationException {
		CSquery query = createQueryTemplate(queryName);
		String[] requiredVars = addActions(rule, query, sdaRequired);
		_body queryBody = new _body();
		addSelect(queryBody, requiredVars, rule);
		_body innerQueryBody = new _body();
		addSelect(innerQueryBody, getInnerQueryRequiredVars(requiredVars), rule);

		query.fromStream(sourceStreamURI, rule.getTimeWindow() + "s",
				rule.getTimeStep() + "s")
				.from(MO.getKnowledgeBaseDataURL() + "?graph=default")
				.where(queryBody
						.where(innerQueryBody.where(createGraphPattern(rule)))
						.groupby(getGroupingClassVariable(rule))
						.having(parseCondition(rule.getCondition(),
								getOutputValueVariable(rule, sdaRequired))));
		return query;
	}

	private String[] getInnerQueryRequiredVars(String[] outerQueryRequiredVars) {
		String[] innerQueryRequiredVars = new String[outerQueryRequiredVars.length];
		for (int i=0; i<outerQueryRequiredVars.length; i++) {
			switch (outerQueryRequiredVars[i]) {
			case QueryVars.OUTPUT:
				innerQueryRequiredVars[i] = QueryVars.INPUT;
				break;
			case QueryVars.TIMESTAMP:
				innerQueryRequiredVars[i] = QueryVars.INPUT_TIMESTAMP;
				break;

			default:
				innerQueryRequiredVars[i] = outerQueryRequiredVars[i];
				break;
			}
		}
		return innerQueryRequiredVars;
	}

	private void addSelect(_body queryBody, String[] variables,
			MonitoringRule rule) throws MalformedQueryException {
		String aggregateFunction = getAggregateFunction(rule);
		for (String var : variables) {
			switch (var) {
			case QueryVars.TIMESTAMP:
				queryBody.selectFunction(QueryVars.TIMESTAMP, Function.MAX,
						QueryVars.INPUT_TIMESTAMP);
				break;
			case QueryVars.INPUT_TIMESTAMP:
				queryBody.selectFunction(QueryVars.INPUT_TIMESTAMP, Function.TIMESTAMP,
						QueryVars.DATUM, MO.shortForm(MO.aboutResource), QueryVars.TARGET);
				break;
			case QueryVars.OUTPUT:
				if (isGroupedMetric(rule)) {
					String[] parameters = getAggregateFunctionParameters(rule);
					queryBody.selectFunction(QueryVars.OUTPUT,
							aggregateFunction, parameters);
				} else {
					queryBody.select(QueryVars.OUTPUT);
				}
				break;
			default:
				queryBody.select(var);
				break;
			}
		}
	}

	private String[] getAggregateFunctionParameters(MonitoringRule rule) {
		String aggregateFunction = getAggregateFunction(rule);
		String[] parameters = new String[FunctionParameter
				.getNumberOfParameters(aggregateFunction)];
		parameters[FunctionParameter.getParameterIdx(aggregateFunction,
				FunctionParameter.INPUT_VARIABLE)] = QueryVars.INPUT;
		if (rule.getMetricAggregation().getParameters() != null) {
			List<Parameter> rulePars = rule.getMetricAggregation()
					.getParameters();
			for (Parameter p : rulePars) {
				int index = FunctionParameter.getParameterIdx(
						aggregateFunction, p.getName());
				parameters[index] = p.getValue().toString();
			}
		}
		return parameters;
	}

	private String getAggregateFunction(MonitoringRule rule) {
		if (!isGroupedMetric(rule))
			return null;
		String aggregateFunction = rule.getMetricAggregation()
				.getAggregateFunction();
		return aggregateFunction;
	}

	private String getGroupingClassVariable(MonitoringRule rule)
			throws RuleInstallationException {
		if (!isGroupedMetric(rule))
			return null;
		String groupingClass = getGroupingClass(rule);
		String targetClass = getTargetClass(rule);
		if (groupingClass.equals(targetClass))
			return QueryVars.TARGET;
		return "?" + groupingClass;
	}

	private String getTargetClass(MonitoringRule rule)
			throws RuleInstallationException {
		List<MonitoredTarget> targets = getMonitoredTargets(rule);
		String targetClass = null;
		for (MonitoredTarget t : targets) {
			if (targetClass != null)
				if (!targetClass.equals(t.getClazz()))
					throw new RuleInstallationException(
							"Monitored targets must belong to the same class");
				else
					targetClass = t.getClazz();
		}
		return targetClass;
	}

	private String getGroupingClass(MonitoringRule rule) {
		if (isGroupedMetric(rule))
			return rule.getMetricAggregation().getGroupingCategoryName();
		else
			return null;
	}

	private boolean isGroupedMetric(MonitoringRule rule) {
		return rule.getMetricAggregation() != null;
	}

	private List<MonitoredTarget> getMonitoredTargets(MonitoringRule rule) {
		List<MonitoredTarget> targets = rule.getMonitoredTargets()
				.getMonitoredTargets();
		if (targets.size() != 1)
			throw new NotImplementedException(
					"Multiple or zero monitored target is not implemented yet");
		return targets;
	}

	private CSquery createTunnelQuery(MonitoringRule rule, String queryName,
			String sourceStreamURI) throws RuleInstallationException {
		try {
			CSquery tunnelQuery = createQueryTemplate(queryName);
			tunnelQuery
					.select(QueryVars.TARGET, QueryVars.INPUT,
							QueryVars.TIMESTAMP)
					.fromStream(sourceStreamURI, rule.getTimeWindow() + "s",
							rule.getTimeStep() + "s")
					.from(MO.getKnowledgeBaseDataURL() + "?graph=default")
					.where(body
							.select(QueryVars.TARGET, QueryVars.INPUT)
							.selectFunction(QueryVars.TIMESTAMP,
									Function.TIMESTAMP, QueryVars.DATUM,
									MO.shortForm(MO.aboutResource),
									QueryVars.TARGET)
							.where(createGraphPattern(rule)));
			return tunnelQuery;
		} catch (MalformedQueryException e) {
			throw new RuleInstallationException(e);
		}
	}

	private CSquery createQueryTemplate(String queryName)
			throws MalformedQueryException {
		CSquery query = CSquery.createDefaultQuery(queryName);
		addPrefixes(query);
		return query;
	}

	private String getNewQueryName(MonitoringRule rule, String suffix) {
		if (suffix == null)
			suffix = "";
		String queryName = CSquery.escapeName(rule.getId()) + suffix;
		while (registeredQueries.containsKey(queryName)) {
			queryName = CSquery.generateRandomName() + suffix;
		}
		return queryName;
	}

	private String getSourceStreamURI(String metric) {
		return MO.streamsURI + metric;
	}

	private String[] addActions(MonitoringRule rule, CSquery query,
			boolean sdaRequired) throws RuleInstallationException {
		String[] requiredVars = null;
		if (rule.getActions() == null)
			return requiredVars;

		for (Action a : rule.getActions().getActions()) {
			switch (a.getName()) {
			case "OutputMetric":
				String outputTargetVariable = getOutputTarget(rule);
				String outputValueVariable = getOutputValueVariable(rule,
						sdaRequired);
				requiredVars = new String[] { outputTargetVariable,
						outputValueVariable, QueryVars.TIMESTAMP };
				query.construct(graph
						.add(CSquery.BLANK_NODE, MO.metric,
								"mo:" + getParValue(a.getParameters(), "name"))
						.add(MO.aboutResource, outputTargetVariable)
						.add(MO.value, outputValueVariable)
						.add(MO.timestamp, QueryVars.TIMESTAMP));
				break;
			case "EnableMonitoringRule":
				throw new NotImplementedException("Action " + a.getName()
						+ " has not been implemented yet.");
				// break;
			case "DisableMonitoringRule":
				throw new NotImplementedException("Action " + a.getName()
						+ " has not been implemented yet.");
				// break;
			case "SetSamplingProbability":
				throw new NotImplementedException("Action " + a.getName()
						+ " has not been implemented yet.");
				// break;
			case "SetSamplingTime":
				throw new NotImplementedException("Action " + a.getName()
						+ " has not been implemented yet.");
				// break;

			default:
				throw new NotImplementedException("Action " + a.getName()
						+ " has not been implemented yet.");
			}
		}

		return requiredVars;
	}

	private String getOutputValueVariable(MonitoringRule rule,
			boolean sdaRequired) {
		if (sdaRequired || !isGroupedMetric(rule))
			return QueryVars.INPUT;
		return QueryVars.OUTPUT;
	}

	private String getOutputTarget(MonitoringRule rule)
			throws RuleInstallationException {
		String outputTarget;
		if (isGroupedMetric(rule)) {
			outputTarget = getGroupingClassVariable(rule);
		} else {
			outputTarget = QueryVars.TARGET;
		}
		return outputTarget;
	}

	private Object getParValue(List<Parameter> parameters, String key) {
		for (Parameter p : parameters) {
			if (p.getName().equals(key))
				return p.getValue();
		}
		return null;
	}

	private void addPrefixes(CSquery query) {
		query.setNsPrefix("xsd", XSD.getURI())
				.setNsPrefix("rdf", RDF.getURI())
				.setNsPrefix("rdfs", RDFS.getURI())
				.setNsPrefix("mo", MO.URI)
				.setNsPrefix(CSquery.getFunctionsPrefix(),
						CSquery.getFunctionsURI());
	}

	private String parseCondition(String condition, String outputValueVariable) {
		return condition != null ? condition.replace("METRIC",
				outputValueVariable) : null;
	}

	private _graph createGraphPattern(MonitoringRule rule)
			throws RuleInstallationException {
		_graph graph = new _graph();
		List<MonitoredTarget> targets = getMonitoredTargets(rule);
		String groupingClass = getGroupingClass(rule);

		graph.add(QueryVars.DATUM, MO.metric,
				MO.prefix + ":" + rule.getMetricName())
				.add(MO.aboutResource, QueryVars.TARGET)
				.add(MO.value, QueryVars.INPUT)
				.add(QueryVars.TARGET, MO.id,
						getTargetIDLiteral(targets.get(0)));

		switch (targets.get(0).getClazz()) {
		case Vocabulary.VM:
			graph.add(QueryVars.TARGET, RDF.type, MO.VM);
			if (groupingClass != null) {
				switch (groupingClass) {
				case Vocabulary.VM:
					break;
				case Vocabulary.CloudProvider:
					graph.add(QueryVars.TARGET, MO.cloudProvider,
							getGroupingClassVariable(rule));
					break;
				default:
					throw new NotImplementedException("Grouping class "
							+ groupingClass + " for target "
							+ targets.get(0).getClazz()
							+ " has not been implemented yet");
				}
				break;
			}
		default:
			throw new NotImplementedException(
					"Cannot install rules with target class "
							+ targets.get(0).getClazz() + " yet");
		}
		return graph;
	}

	private String getTargetIDLiteral(MonitoredTarget monitoredTarget) {
		return "\"" + monitoredTarget.getId() + "\"";
	}

	public String getQuery(String queryId) {
		return registeredQueries.get(queryId);
	}

	private boolean isSubclassOf(String resourceURI, String superClassURI) {
		String queryString = "ASK " + "FROM <" + MO.getKnowledgeBaseDataURL()
				+ "?graph=default>" + "WHERE { <" + resourceURI + "> <"
				+ RDFS.subClassOf + "> <" + superClassURI + "> . }";

		Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL_11);
		QueryExecution qexec = QueryExecutionFactory.create(query);

		return qexec.execAsk();
	}

}