/*-
 * #%L
 * Partial response support for Cloud Endpoints v2
 * ---
 * Copyright (C) 2018 AODocs (Altirnao Inc)
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.aodocs.partialresponse.it;

import java.io.IOException;

import com.google.api.client.googleapis.util.Utils;
import com.google.api.services.discovery.Discovery;
import com.google.api.services.discovery.model.RestDescription;

abstract class DiscoveryApiIntegrationTest {
	
	private static final Discovery CLIENT = new Discovery.Builder(
			Utils.getDefaultTransport(), Utils.getDefaultJsonFactory(), null)
			.setRootUrl("https://www.googleapis.com/")
			.setApplicationName("endpoints-partialresponse").build();
	
	static RestDescription loadDriveV3Discovery(String fields) throws IOException {
		RestDescription restDescription = CLIENT.apis().getRest("drive", "v3").setFields(fields).execute();
		restDescription.setEtag(null);// for some reason (caching ?), the etag field may vary depending on the input fields
		return restDescription;
	}
	
}
