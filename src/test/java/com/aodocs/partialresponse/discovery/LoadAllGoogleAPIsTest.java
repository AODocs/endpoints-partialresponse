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
package com.aodocs.partialresponse.discovery;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.json.JsonParser;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.io.Resources;

/**
 * Checks a lot of Google APIs discovery files by loading
 * all their schema with ResourceTreeRepository.
 */
@RunWith(Parameterized.class)
public class LoadAllGoogleAPIsTest {
	
	@Parameterized.Parameters(name = "{index}: {0}")
	public static URL[] createParams() throws IOException {
		try (InputStream inputStream = Resources.getResource("googleapis/api-list.json").openStream()) {
			JsonParser jsonParser = Utils.getDefaultJsonFactory().createJsonParser(inputStream);
			DirectoryList directoryList = jsonParser.parse(DirectoryList.class);
			return FluentIterable.from(directoryList.getItems())
					.transform(new Function<DirectoryList.Items, String>() {
						@Override
						public String apply(DirectoryList.Items input) {
							return "googleapis/" + input.getName() + "/" + input.getVersion().replace("_", "/") + "/" + input.getName() + "-api.json";
						}
					})
					.filter(new Predicate<String>() {
						@Override
						public boolean apply(String input) {
							try {
								Resources.getResource(input);
								return true;
							} catch (Exception e) {
								System.err.println(input + " is listed but does not exist");
								return false;
							}
						}
					})
					.transform(new Function<String, URL>() {
						@Override
						public URL apply(String input) {
							return Resources.getResource(input);
						}
					})
					.toArray(URL.class);
		}
	}
	
	private URL resource;
	
	public LoadAllGoogleAPIsTest(URL resource) {
		this.resource = resource;
	}
	
	@Test
	public void testLoadAll() throws IOException {
		ResourceTreeRepository.load(resource).loadAllRootNodes();
	}
	
}
