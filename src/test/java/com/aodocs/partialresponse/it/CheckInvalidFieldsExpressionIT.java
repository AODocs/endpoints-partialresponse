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

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.aodocs.partialresponse.discovery.ResourceTreeRepositoryTest;
import com.aodocs.partialresponse.fieldsexpression.FieldsExpression;
import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionTree;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

/**
 * Checks that "bad" fields expression are the same on a Google API or with FieldsExpressionTree.contains
 */
@RunWith(Parameterized.class)
public class CheckInvalidFieldsExpressionIT extends DiscoveryApiIntegrationTest {

	
	private static final FieldsExpressionTree REST_DESCRIPTION_SCHEMA = ResourceTreeRepositoryTest
			.getResourceFromApi("discovery", "v1", "RestDescription");
	
	private final String invalidFieldsExpression;
	
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "**" },
				{ "doesNotExist" },
				{ "name/doesNotExist" },
				{ "labels/doesNotExist" },
				{ "parameters/id/doesNotExist" },
				{ "*,doesNotExist" },
				{ "parameters/*,parameters/*/doesNotExist" },
		});
	}
	
	public CheckInvalidFieldsExpressionIT(String invalidFieldsExpression) throws IOException {
		this.invalidFieldsExpression = invalidFieldsExpression;
	}
	
	@Test
	public void check() throws IOException {
		try {
			loadDriveV3Discovery(invalidFieldsExpression);
			throw new IllegalStateException("Wrong invalid fields expression assumption for : '" + invalidFieldsExpression + "'");
		} catch (GoogleJsonResponseException e) {
			//expected to fail
		}
		assertFalse(FieldsExpression.parse(invalidFieldsExpression).isValidAgainst(REST_DESCRIPTION_SCHEMA));
	}
	
}
