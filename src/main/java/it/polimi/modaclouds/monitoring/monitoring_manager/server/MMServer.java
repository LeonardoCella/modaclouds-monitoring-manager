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
package it.polimi.modaclouds.monitoring.monitoring_manager.server;

import it.polimi.modaclouds.monitoring.monitoring_manager.Config;
import it.polimi.modaclouds.monitoring.monitoring_manager.MonitoringManager;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import it.polimi.modaclouds.qos_models.monitoring_ontology.MonitorableResource;

public class MMServer extends Application{
	private static Component component;
	private static MonitoringManager manager = null;
	private static final String apiVersion = "v1";
	
	private static Logger logger = LoggerFactory.getLogger(MMServer.class.getName());
	
	public static void main(String[] args) throws Exception{

		manager = new MonitoringManager();

		component = new Component();
		component.getServers().add(Protocol.HTTP, Config.getInstance().getMMServerPort());
		component.getClients().add(Protocol.FILE);  

		MMServer mmServer = new MMServer();
		component.getDefaultHost().attach("", mmServer);
		
		component.start();
		
	}
	
	
	public Restlet createInboundRoot(){

		String server_address = component.getServers().get(0).getAddress();
		if(server_address == null){
			server_address = "http://localhost";
			server_address = server_address + ":" + String.valueOf(component.getServers().get(0).getActualPort());
		}

		getContext().getAttributes().put("complete_server_address", server_address);
		getContext().getAttributes().put("manager", manager);

		Router router = new Router(getContext());
		router.setDefaultMatchingMode(Template.MODE_EQUALS);

		router.attach("/"+ apiVersion +"/monitoring-rules", MultipleRulesDataServer.class);
		router.attach("/"+ apiVersion +"/monitoring-rules/{id}", SingleRuleDataServer.class);
		router.attach("/"+ apiVersion +"/metrics", MultipleMetricsDataServer.class);
		router.attach("/"+ apiVersion +"/metrics/{metricname}", SingleMetricDataServer.class);
		router.attach("/"+ apiVersion +"/metrics/{metricname}/observers", MultipleObserversDataServer.class);
		router.attach("/"+ apiVersion +"/metrics/{metricname}/observers/{id}", SingleObserverDataServer.class);
		
		router.attach("/"+ apiVersion +"/update", UpdateDeploymentDataServer.class);
		//TODO maybe should be inserted a list in the payload of the components to be removed, instead using the single id
		router.attach("/"+ apiVersion +"/update/{id}", UpdateDeploymentDataServer.class);
		router.attach("/"+ apiVersion +"/upload", UploadDeploymentDataServer.class);


		return router;
	}
}
