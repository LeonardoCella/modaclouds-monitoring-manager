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

import it.polimi.modaclouds.monitoring.monitoring_manager.MonitoringManager;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class MultipleResourcesDataServer extends ServerResource {

	private Logger logger = LoggerFactory
			.getLogger(MultipleResourcesDataServer.class);

	@Get
	public void getResources() {
		try {
			MonitoringManager manager = (MonitoringManager) getContext()
					.getAttributes().get("manager");
			Model model = manager.getCurrentModel();
			this.getResponse().setStatus(Status.SUCCESS_OK);
			this.getResponse().setEntity(new Gson().toJson(model),
					MediaType.APPLICATION_JSON);
		} catch (Exception e) {
			logger.error("Error while getting current model", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
					e.getMessage());
			this.getResponse().setEntity(
					"Error while getting current model: " + e.toString(),
					MediaType.TEXT_PLAIN);
		} finally {
			this.getResponse().commit();
			this.commit();
			this.release();
		}
	}

	@Post
	public void updateResources(Representation rep) {
		try {
			MonitoringManager manager = (MonitoringManager) getContext()
					.getAttributes().get("manager");
			String payload = rep.getText();

			Gson gson = new Gson();
			Model deserialised = gson.fromJson(payload, Model.class);

			manager.updateModel(deserialised);
			this.getResponse().setStatus(Status.SUCCESS_NO_CONTENT);

		} catch (JsonSyntaxException e) {
			logger.error("Error while adding components: {}", e.getMessage());
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e);
			this.getResponse().setEntity(
					"Error while adding components: " + e.getMessage(),
					MediaType.TEXT_PLAIN);
		} catch (Exception e) {
			logger.error("Error while adding components", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
			this.getResponse().setEntity(
					"Error while adding components: "
							+ Throwables.getStackTraceAsString(e),
					MediaType.TEXT_PLAIN);
		} finally {
			this.getResponse().commit();
			this.commit();
			this.release();
		}
	}

	@Put
	public void uploadModel(Representation rep) {
		try {
			// invoking manager and payload of the post request
			MonitoringManager manager = (MonitoringManager) getContext()
					.getAttributes().get("manager");
			String payload = rep.getText();

			// deserialisation from json to ModelUpdates.class
			Gson gson = new Gson();
			Model deserialised = gson.fromJson(payload, Model.class);

			// upload the new model in the knowledge base and responde to the
			// request
			manager.uploadModel(deserialised);
			this.getResponse().setStatus(Status.SUCCESS_NO_CONTENT);

		} catch (JsonSyntaxException e) {
			logger.error("Error while uploading the model: {}", e.getMessage());
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e);
			this.getResponse().setEntity(
					"Error while uploading the model: " + e.getMessage(),
					MediaType.TEXT_PLAIN);
		} catch (Exception e) {
			logger.error("Error while uploading the model", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
					e.getMessage());
			this.getResponse().setEntity(
					"Error while uploading the model: "
							+ Throwables.getStackTraceAsString(e),
					MediaType.TEXT_PLAIN);
		} finally {
			this.getResponse().commit();
			this.commit();
			this.release();
		}
	}

}