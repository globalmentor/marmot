/*
 * Copyright © 2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globalmentor.marmot.repository;

import static com.globalmentor.net.URIs.*;
import static com.globalmentor.urf.content.Content.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.net.URI;
import java.util.*;

import org.junit.*;

import com.globalmentor.java.Bytes;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.test.AbstractTest;
import com.globalmentor.urf.URFDateTime;
import com.globalmentor.urf.URFResource;
import com.globalmentor.urf.content.Content;

/**
 * Abstract base class for running tests on repositories.
 * 
 * @author Garret Wilson
 */
public abstract class AbstractRepositoryTest extends AbstractTest
{

	/** The repository on which tests are being run. */
	private Repository repository = null;

	/** @return The repository on which tests are being run. */
	protected Repository getRepository()
	{
		return repository;
	}

	@Before
	public void before()
	{
		repository = createRepository();
	}

	/**
	 * Creates a temporary, empty repository for running tests.
	 * @return A new repository for testing.
	 */
	protected abstract Repository createRepository();

	/*TODO del
		@Test
		public void test() throws ResourceIOException
		{
			final Repository repository=new NTFSFileRepository(new File("example"));
			Repositories.print(repository, System.out);
		}
	*/

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a resource using supplied contents.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 */
	@Test
	public void testCreateResourceBytes() throws ResourceIOException
	{
		final int contentLength = (1 << 10) + 1;
		final Date beforeCreateResource = new Date();
		final Repository repository = getRepository();
		final byte[] resourceContents = Bytes.createRandom(contentLength); //create random contents
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		final URFResource newResourceDescription = repository.createResource(resourceURI, resourceContents); //create a resource with random contents
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		assertThat("Invalid content length of created resource.", Content.getContentLength(newResourceDescription), equalTo((long)contentLength));

		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, new Date());
		final byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(resourceContents));
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a collection resource.</li>
	 * <li>Checking the existence of a collection resource.</li>
	 * <li>Creating a resource inside a collection using supplied contents.</li>
	 * <li>Checking the existence of a resource inside a collection.</li>
	 * <li>Deleting a collection resource with all its contents.</li>
	 * </ul>
	 */
	@Test
	public void testCreateCollectionResourceBytes() throws ResourceIOException
	{
		final int contentLength = (1 << 10) + 1;
		final Repository repository = getRepository();
		final byte[] resourceContents = Bytes.createRandom(contentLength); //create random contents
		final Date beforeCreateCollection = new Date();
		final URI collectionURI = repository.getRootURI().resolve("test/"); //determine a test collection URI
		final URFResource newCollectionDescription = repository.createCollectionResource(collectionURI);
		assertTrue("Created collection resource doesn't exist.", repository.resourceExists(collectionURI));
		assertThat("Invalid content length of created collection resource.", Content.getContentLength(newCollectionDescription), equalTo(0L));
		final Date beforeCreateResource = new Date();
		final URI resourceURI = collectionURI.resolve("test.bin"); //determine a test resource URI
		final URFResource newResourceDescription = repository.createResource(resourceURI, resourceContents); //create a resource with random contents
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		checkCreatedResourceDateTimes(newCollectionDescription, beforeCreateCollection, beforeCreateResource);
		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, new Date());
		assertThat("Invalid content length of created resource.", Content.getContentLength(newResourceDescription), equalTo((long)contentLength));
		final byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(resourceContents));
		repository.deleteResource(collectionURI); //delete the collection resource we created, with its contained resource
		assertFalse("Deleted collection resource still exists.", repository.resourceExists(collectionURI));
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));

	}

	/**
	 * Ensures that the dates of a created resource are valid.
	 * @param resourceDescription The description of the created resource.
	 * @param beforeDate Some date before the creation of the file
	 * @param afterDate Some date after the creation of the file
	 */
	protected void checkCreatedResourceDateTimes(final URFResource resourceDescription, final Date beforeDate, final Date afterDate)
	{
		final URFDateTime modifiedDateTime = getModified(resourceDescription);
		if(!isCollectionURI(resourceDescription.getURI())) //modified datetime is optional for collections
		{
			assertNotNull("Missing modified datetime.", modifiedDateTime);
		}
		if(modifiedDateTime != null)
		{
			assertTrue(
					"Modified datetime not expected range.",
					(modifiedDateTime.equals(beforeDate) || modifiedDateTime.after(beforeDate))
							&& (modifiedDateTime.equals(afterDate) || modifiedDateTime.before(afterDate)));
		}
		final URFDateTime createdDateTime = getCreated(resourceDescription);
		if(createdDateTime != null)
		{
			assertThat("Modified datetime not equal to created datetime.", createdDateTime, equalTo(modifiedDateTime));
		}
	}

	@After
	public void after()
	{
		repository = null;
	}

}
