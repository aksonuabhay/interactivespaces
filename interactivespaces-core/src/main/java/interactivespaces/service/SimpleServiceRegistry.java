/*
 * Copyright (C) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package interactivespaces.service;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * A simple implementation of the {@link ServiceRegistry}.
 * 
 * @author Keith M. Hughes
 */
public class SimpleServiceRegistry implements ServiceRegistry {

	/**
	 * All services in the registry.
	 */
	private Map<String, ServiceEntry> services = Maps.newHashMap();

	@Override
	public void registerService(String name, Service service) {
		registerService(name, service, null);
	}

	@Override
	public void registerService(String name, Service service,
			Map<String, Object> metadata) {
		if (metadata == null) {
			metadata = Maps.newHashMap();
		}
		
		services.put(name, new ServiceEntry(service, metadata));
	}

	@Override
	public <T extends Service> T getService(String name) {
		ServiceEntry entry = services.get(name);
		if (entry != null) {
			@SuppressWarnings("unchecked")
			T service = (T) entry.getService();

			return service;
		} else {
			return null;
		}
	}

	/**
	 * An entry in the service map.
	 * 
	 * @author Keith M. Hughes
	 */
	private static class ServiceEntry {

		/**
		 * The service instance.
		 */
		private Service service;

		/**
		 * The metadata for the entry.
		 */
		private Map<String, Object> metadata;

		public ServiceEntry(Service service, Map<String, Object> metadata) {
			this.service = service;
			this.metadata = metadata;
		}

		/**
		 * @return the service
		 */
		public Service getService() {
			return service;
		}

		/**
		 * @return the metadata
		 */
		public Map<String, Object> getMetadata() {
			return metadata;
		}
	}

}