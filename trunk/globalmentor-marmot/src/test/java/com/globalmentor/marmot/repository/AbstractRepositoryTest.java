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

import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.java.Conditions.*;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.urf.content.Content.*;
import static com.globalmentor.urf.dcmi.DCMI.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.junit.*;

import com.globalmentor.java.Bytes;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.test.AbstractTest;
import com.globalmentor.urf.*;
import com.globalmentor.urf.content.Content;
import com.globalmentor.urf.dcmi.DCMI;

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
		assertThat("Invalid content length of created resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, new Date());
		final byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(resourceContents));
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a resource using supplied contents.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>Changing the contents of a resource using an output stream.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 */
	@Test
	public void testChangeResourceBytes() throws IOException
	{
		final int initialContentLength = (1 << 10) + 1;
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		testResourceContentBytes(resourceURI, Bytes.createRandom(initialContentLength), Bytes.createRandom(initialContentLength * 2));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a resource using supplied contents.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>If more than one content array is provided, changing the contents of a resource using an output stream.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 * @param resourceURI The URI of the resource to create.
	 * @param contents The array of byte arrays of content to use; the first will create the resource, the others will test modifying the resource
	 * @throws NullPointerException if the given resource URI and/or content array is <code>null</code>.
	 * @throws IllegalArgumentException if the array of contents is empty.
	 */
	protected void testResourceContentBytes(final URI resourceURI, final byte[]... contents) throws IOException
	{
		checkArgumentPositive(contents.length);
		Date before = new Date();
		final Repository repository = getRepository();
		URFResource newResourceDescription = repository.createResource(resourceURI, contents[0]); //create an initial resource with contents
		Date after = new Date();
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		assertThat("Invalid content length of created resource.", getContentLength(newResourceDescription), equalTo((long)contents[0].length));
		checkCreatedResourceDateTimes(newResourceDescription, before, after);
		byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(contents[0]));
		for(int i = 1; i < contents.length; ++i) //look at all the alternate contents, if any
		{
			final byte[] changedContent = contents[i];
			before = after; //switch the dates back
			final OutputStream outputStream = repository.getResourceOutputStream(resourceURI); //get an output stream to the existing resource
			try
			{
				outputStream.write(changedContent); //write the changed contents
			}
			finally
			{
				outputStream.close(); //close the output stream
			}
			after = new Date();
			newResourceDescription = repository.getResourceDescription(resourceURI); //get an updated description of the resource
			assertTrue("Changed resource doesn't exist.", repository.resourceExists(resourceURI));
			assertThat("Invalid content length of changed resource.", getContentLength(newResourceDescription), equalTo((long)changedContent.length));
			//TODO fix last-modified date for SVNKit repository		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, afterCreateResource);
		}
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a small resource using an output stream.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 */
	@Test
	public void testCreateSmallResourceOutputStream() throws IOException
	{
		testCreateResourceOutputStream((1 << 3) + 1); //>8
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a large resource using an output stream.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 */
	@Test
	public void testCreateLargeResourceOutputStream() throws IOException
	{
		testCreateResourceOutputStream((1 << 20) + 1); //>1MB
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a resource using an output stream.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 * @param contentLength The size of the resource to create.
	 */
	public void testCreateResourceOutputStream(final int contentLength) throws IOException
	{
		final Date beforeCreateResource = new Date();
		final Repository repository = getRepository();
		final byte[] resourceContents = Bytes.createRandom(contentLength); //create random contents
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		final OutputStream outputStream = repository.createResource(resourceURI); //create a resource and get an output stream to the contents
		try
		{
			outputStream.write(resourceContents); //write the contents
		}
		finally
		{
			outputStream.close(); //close the output stream
		}
		final URFResource newResourceDescription = repository.getResourceDescription(resourceURI); //get a description of the new resource
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		assertThat("Invalid content length of created resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
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
		assertThat("Invalid content length of created collection resource.", getContentLength(newCollectionDescription), equalTo(0L));
		final Date beforeCreateResource = new Date();
		final URI resourceURI = collectionURI.resolve("test.bin"); //determine a test resource URI
		final URFResource newResourceDescription = repository.createResource(resourceURI, resourceContents); //create a resource with random contents
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		checkCreatedResourceDateTimes(newCollectionDescription, beforeCreateCollection, beforeCreateResource);
		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, new Date());
		assertThat("Invalid content length of created resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		final byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(resourceContents));
		repository.deleteResource(collectionURI); //delete the collection resource we created, with its contained resource
		assertFalse("Deleted collection resource still exists.", repository.resourceExists(collectionURI));
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating all needed parent resources for a deeply-nested resource.</li>
	 * <li>Checking the existence of a deeply-nested collection resource.</li>
	 * <li>Creating a resource inside a deeply-nested collection using supplied contents.</li>
	 * <li>Checking the existence of a deeply-nested resource inside a collection.</li>
	 * <li>Deleting a deeply-nested collection resource with all its contents.</li>
	 * </ul>
	 */
	@Test
	public void testCreateDeepResourceBytes() throws ResourceIOException
	{
		final int contentLength = (1 << 10) + 1;
		final Repository repository = getRepository();
		final byte[] resourceContents = Bytes.createRandom(contentLength); //create random contents
		final Date beforeCreateCollection = new Date();
		final URI rootCollectionURI = repository.getRootURI().resolve("test1/"); //determine some top-level collection
		final URI collectionURI = rootCollectionURI.resolve("test2/test3/test4/test5/"); //determine a deeply-nested test collection URI
		final URI resourceURI = collectionURI.resolve("test.bin"); //determine a test resource URI
		final URFResource newCollectionDescription = repository.createParentResources(resourceURI); //create all necessary parent resources for the resource
		assertThat("Last created parent resource doesn't match our deeply-nested collection.", newCollectionDescription.getURI(), equalTo(collectionURI));
		assertTrue("Created root collection resource doesn't exist.", repository.resourceExists(rootCollectionURI));
		assertThat("Invalid content length of created root collection resource.", getContentLength(repository.getResourceDescription(rootCollectionURI)),
				equalTo(0L));
		assertTrue("Created nested collection resource doesn't exist.", repository.resourceExists(collectionURI));
		assertThat("Invalid content length of created nested collection resource.", getContentLength(newCollectionDescription), equalTo(0L));
		final Date beforeCreateResource = new Date();
		final URFResource newResourceDescription = repository.createResource(resourceURI, resourceContents); //create a resource with random contents
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		checkCreatedResourceDateTimes(newCollectionDescription, beforeCreateCollection, beforeCreateResource);
		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, new Date());
		assertThat("Invalid content length of created resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		final byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(resourceContents));
		repository.deleteResource(rootCollectionURI); //delete the root collection resource we created, with its contained deeply-nested collections and resource
		assertFalse("Deleted root collection resource still exists.", repository.resourceExists(rootCollectionURI));
		assertFalse("Deleted nested collection resource still exists.", repository.resourceExists(collectionURI));
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a resource with no contents.</li>
	 * <li>Deleting an empty resource.</li>
	 * </ul>
	 */
	@Test
	public void testCreateEmptyResource() throws ResourceIOException
	{
		final Date beforeCreateResource = new Date();
		final Repository repository = getRepository();
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		final URFResource newResourceDescription = repository.createResource(resourceURI, NO_BYTES); //create a resource with no contents
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		assertThat("Invalid content length of created resource.", getContentLength(newResourceDescription), equalTo(0L));
		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, new Date());
		final byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(NO_BYTES));
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Creates a resource description with test properties, including the following properties:
	 * <ul>
	 * <li>{@link DCMI#TITLE_PROPERTY_URI}</li>
	 * <li>{@link DCMI#DESCRIPTION_PROPERTY_URI}</li>
	 * <li>{@link DCMI#DATE_PROPERTY_URI}</li>
	 * <li>{@link DCMI#LANGUAGE_PROPERTY_URI}</li>
	 * </ul>
	 * @param resourceURI The URI to
	 * @return A description of test properties.
	 */
	protected URFResource createTestProperties(final URI resourceURI)
	{
		final URFResource resource = new DefaultURFResource(resourceURI);
		setTitle(resource, "Test Title");
		setDescription(resource, "This is a test description.");
		setDate(resource, new URFDateTime());
		setLanguage(resource, Locale.ENGLISH);
		return resource;
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Setting properties individually on a normal resource.</li>
	 * <li>Retrieving properties individually on a normal resource.</li>
	 * <li>Updating properties individually on a normal resource.</li>
	 * </ul>
	 */
	@Test
	public void testResourceProperties() throws ResourceIOException
	{
		final Repository repository = getRepository();
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		repository.createResource(resourceURI, Bytes.createRandom((1 << 10) + 1)); //create a resource with random contents
		testResourceProperties(resourceURI);
		repository.deleteResource(resourceURI); //delete the resource we created
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Setting properties individually on a collection resource.</li>
	 * <li>Retrieving properties individually on a collection resource.</li>
	 * <li>Updating properties individually on a collection resource.</li>
	 * </ul>
	 */
	@Test
	public void testCollectionResourceProperties() throws ResourceIOException
	{
		final Repository repository = getRepository();
		final URI collectionURI = repository.getRootURI().resolve("test/"); //determine a test collection URI
		repository.createCollectionResource(collectionURI);
		testResourceProperties(collectionURI);
		repository.deleteResource(collectionURI); //delete the resource we created
	}

	/**
	 * Tests setting properties on an existing, new resource:
	 * <ul>
	 * <li>Setting properties individually on the resource.</li>
	 * <li>Retrieving properties individually on the resource.</li>
	 * <li>Updating properties individually on the resource.</li>
	 * </ul>
	 * @param resourceURI The URI of the resource on which to test setting properties.
	 */
	protected void testResourceProperties(final URI resourceURI) throws ResourceIOException
	{
		final Repository repository = getRepository();
		final URFResource newResourceDescription = repository.getResourceDescription(resourceURI); //get the initial description
		final long contentLength = getContentLength(newResourceDescription);
		final URFDateTime createdDateTime = getCreated(newResourceDescription); //optional
		final URFDateTime modifiedDateTime = getModified(newResourceDescription);
		final long propertyCount = 1L + (createdDateTime != null ? 1L : 0L) + (modifiedDateTime != null ? 1L : 0L);
		assertThat(newResourceDescription.getPropertyCount(), equalTo(propertyCount)); //content length, content created, content modified
		final URFResource propertiesResource = createTestProperties(resourceURI); //create test properties
		URFResource updatedResourceDescription = repository.setResourceProperties(resourceURI, propertiesResource.getProperties()); //set those properties individually
		assertThat("Retrieved resource description doesn't match that returned from resource setting.", repository.getResourceDescription(resourceURI),
				equalTo(updatedResourceDescription));
		assertThat(updatedResourceDescription.getPropertyCount(), equalTo(propertyCount + propertiesResource.getPropertyCount())); //see if the new property count is correct 
		checkResourceProperties(updatedResourceDescription, propertiesResource.getProperties()); //see if the resource now has the given properties
		assertThat("Content length changed.", getContentLength(updatedResourceDescription), equalTo(contentLength));
		if(createdDateTime != null)
		{
			assertThat("Created date changed.", getCreated(updatedResourceDescription), equalTo(createdDateTime));
		}
		if(modifiedDateTime != null)
		{
			assertThat("Modified date changed.", getModified(updatedResourceDescription), equalTo(modifiedDateTime));
		}
		final String newTitle = "Modified Title";
		setTitle(propertiesResource, newTitle); //update our title in our local properties
		updatedResourceDescription = repository.setResourceProperties(resourceURI, new DefaultURFProperty(TITLE_PROPERTY_URI, newTitle)); //change the title on the resource
		assertThat("Updated title isn't correct.", getTitle(updatedResourceDescription), equalTo(newTitle));
		checkResourceProperties(updatedResourceDescription, propertiesResource.getProperties()); //see if the resource still has all the other properties
		propertiesResource.removePropertyValues(DESCRIPTION_PROPERTY_URI); //remove the description from our local properties
		updatedResourceDescription = repository.removeResourceProperties(resourceURI, DESCRIPTION_PROPERTY_URI); //remove the description from the resource
		assertFalse("Resource still has removed property.", updatedResourceDescription.hasProperty(DESCRIPTION_PROPERTY_URI));
		checkResourceProperties(updatedResourceDescription, propertiesResource.getProperties()); //see if the resource still has all the other properties
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
					"Modified datetime not in expected range (" + beforeDate.getTime() + ", " + modifiedDateTime.getTime() + ", " + afterDate.getTime() + ")",
					(modifiedDateTime.equals(beforeDate) || modifiedDateTime.after(beforeDate))
							&& (modifiedDateTime.equals(afterDate) || modifiedDateTime.before(afterDate)));
		}
		final URFDateTime createdDateTime = getCreated(resourceDescription);
		if(createdDateTime != null)
		{
			assertThat("Modified datetime not equal to created datetime.", createdDateTime, equalTo(modifiedDateTime));
		}
	}

	/**
	 * Ensures that the properties of a given resource description matches the given properties.
	 * @param resourceDescription The description of the created resource.
	 * @param properties The properties to check.
	 */
	protected void checkResourceProperties(final URFResource resourceDescription, final Iterable<URFProperty> properties)
	{
		for(final URFProperty property : properties)
		{
			assertThat("Resource doesn't have expected property value for " + property.getPropertyURI(),
					resourceDescription.getPropertyValue(property.getPropertyURI()), equalTo(property.getValue()));
		}
	}

	@After
	public void after()
	{
		repository = null;
	}

}
