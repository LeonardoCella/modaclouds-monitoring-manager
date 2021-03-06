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

public class Observer {

	private String id;
	private String callbackUrl;
	private transient String queryUri;

	public Observer(String id, String callbackUrl, String queryUri) {
		this.id = id;
		this.callbackUrl = callbackUrl;
		this.queryUri = queryUri;
	}

	public String getId() {
		return id;
	}

	public String getCallbackUrl() {
		return callbackUrl;
	}

	public String getQueryUri() {
		return queryUri;
	}

	public void setQueryUri(String queryUri) {
		this.queryUri = queryUri;
	}

	public String getUri() {
		return queryUri + "/observers/" + id;
	}

}
