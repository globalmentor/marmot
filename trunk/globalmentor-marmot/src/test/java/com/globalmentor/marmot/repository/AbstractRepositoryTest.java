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

	/** Tests creating a sole resource with given contents in a byte array. */
	@Test
	public void testCreateResourceBytes() throws ResourceIOException
	{
		final Date beforeCreateResource = new Date();
		final Repository repository = getRepository();
		final byte[] resourceContents = Bytes.createRandom(1 << 10 + 1); //create random contents
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		final URFResource newResourceDescription = repository.createResource(resourceURI, resourceContents); //create a resource with random contents
		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, new Date());
		final byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(resourceContents));
	}

	/** Tests creating a collection with a resource inside it given contents in a byte array. */
	@Test
	public void testCreateCollectionResourceBytes() throws ResourceIOException
	{
		final Repository repository = getRepository();
		final byte[] resourceContents = Bytes.createRandom(1 << 10 + 1); //create random contents
		final Date beforeCreateCollection = new Date();
		final URI collectionURI = repository.getRootURI().resolve("test/"); //determine a test collection URI
		final URFResource newCollectionDescription = repository.createCollectionResource(collectionURI);
		final Date beforeCreateResource = new Date();
		final URI resourceURI = collectionURI.resolve("test.bin"); //determine a test resource URI
		final URFResource newResourceDescription = repository.createResource(resourceURI, resourceContents); //create a resource with random contents
		checkCreatedResourceDateTimes(newCollectionDescription, beforeCreateCollection, beforeCreateResource);
		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, new Date());
		final byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(resourceContents));
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