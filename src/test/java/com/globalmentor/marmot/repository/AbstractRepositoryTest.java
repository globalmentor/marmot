/*
 * Copyright © 2011-2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.urframework.content.Content.*;
import static org.urframework.dcmi.DCMI.*;

import static java.nio.charset.StandardCharsets.*;
import static java.util.Objects.*;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.junit.*;
import org.urframework.*;
import org.urframework.dcmi.DCMI;

import com.globalmentor.collections.Sets;
import com.globalmentor.iso.datetime.ISODateTime;
import com.globalmentor.java.Bytes;
import com.globalmentor.log.AbstractLoggedTest;
import com.globalmentor.net.*;
import com.globalmentor.time.Time;

/**
 * Abstract base class for running tests on repositories.
 * 
 * <p>
 * Because some platforms (notably the Linux ext3 file system) do not maintain millisecond-level content modified precision, these test compare all times using
 * only second-level precision.
 * </p>
 * 
 * @author Garret Wilson
 */
public abstract class AbstractRepositoryTest extends AbstractLoggedTest {

	/** The repository on which tests are being run. */
	private Repository repository = null;

	/** @return The repository on which tests are being run. */
	protected Repository getRepository() {
		return repository;
	}

	@Before
	public void before() throws IOException {
		repository = createRepository();
	}

	/**
	 * Creates a temporary, empty repository for running tests.
	 * @return A new repository for testing.
	 */
	protected abstract Repository createRepository();

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a resource using supplied contents.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 */
	@Test
	public void testCreateResourceBytes() throws IOException {
		final int initialContentLength = (1 << 10) + 1;
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		testResourceContentBytes(resourceURI, false, Bytes.createRandom(initialContentLength));
		repository.deleteResource(resourceURI); //delete the resource we created
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
	public void testCreateEmptyResource() throws IOException {
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		testResourceContentBytes(resourceURI, false, NO_BYTES);
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a collection resource using supplied contents.</li>
	 * <li>Checking the existence of a collection resource.</li>
	 * <li>Deleting a collection resource.</li>
	 * </ul>
	 */
	@Test
	public void testCreateCollectionBytes() throws IOException {

		final int initialContentLength = (1 << 10) + 1;
		final URI resourceURI = repository.getRootURI().resolve("test/"); //determine a test collection resource URI
		testResourceContentBytes(resourceURI, false, Bytes.createRandom(initialContentLength));
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating an empty resource.</li>
	 * <li>The ability to detect that a non-collection resource is not a collection resource.</li>
	 * <li>Creating an empty collection resource.</li>
	 * <li>The ability to detect that a collection resource is not a non-collection resource.</li>
	 * </ul>
	 */
	@Test
	public void testResourceCollectionDifferentiation() throws IOException {
		//test a resource
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		repository.createResource(resourceURI, NO_BYTES); //create a resource with random contents		
		assertTrue("Created empty resource doesn't exist.", repository.resourceExists(resourceURI));
		assertFalse("Shadowing collection resource exists.", repository.resourceExists(repository.getRootURI().resolve("test.bin/")));
		final URI collectionResourceURI = repository.getRootURI().resolve("test/"); //determine a test collection resource URI
		repository.createCollectionResource(collectionResourceURI);
		assertTrue("Created collection resource doesn't exist.", repository.resourceExists(collectionResourceURI));
		assertFalse("Shadowing resource exists.", repository.resourceExists(repository.getRootURI().resolve("test")));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a collection resource with an i18n name and verifying its existence.</li>
	 * <li>Creating a resource with an i18n name using supplied contents and verifying its existence.</li>
	 * <li>Verifying that the i18n named resource is listed as a child of the i18n names collection.</li>
	 * <li>Deleting an i18n named resource.</li>
	 * <li>Deleting an i18n named collection.</li>
	 * </ul>
	 */
	@Test
	public void testI18n() throws IOException {
		final int initialContentLength = (1 << 10) + 1;
		final URI collectionResourceURI = repository.getRootURI().resolve(URIPath.encode("voc\u00ea-forr\u00f3-arrasta-p\u00e9-cora\u00e7\u00e3o-\u4eba/")); //collection: você-forró-arrasta-pé-coração-人
		repository.createCollectionResource(collectionResourceURI);
		final URI resourceURI = collectionResourceURI.resolve(URIPath.encode("\u0915\u0941\u091b-\u0915\u0941\u091b-\u0939\u094b\u0924\u093e-\u0939\u0948-\u4eba")); //resource: você-forró-arrasta-pé-coração/कुछ-कुछ-होता-है-人
		testResourceContentBytes(resourceURI, false, Bytes.createRandom(initialContentLength));
		final List<URFResource> collectionChildren = repository.getChildResourceDescriptions(collectionResourceURI); //make sure the URIs of the children are what we expect
		assertThat("URIs of i18n child resource not what expected", Sets.immutableSetOf(collectionChildren),
				equalTo(Sets.<URFResource> immutableSetOf(new DefaultURFResource(resourceURI))));
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
		repository.deleteResource(collectionResourceURI); //delete the collection resource we created
		assertFalse("Deleted collection resource still exists.", repository.resourceExists(collectionResourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a resource using supplied binary contents.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>Changing the binary contents of a resource using an output stream.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 */
	@Test
	public void testChangeResourceBinary() throws IOException {
		final int initialContentLength = (1 << 10) + 1;
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		testResourceContentBytes(resourceURI, false, new byte[0], Bytes.createRandom(initialContentLength), Bytes.createRandom(initialContentLength * 2),
				Bytes.createRandom(initialContentLength * 3), new byte[0], Bytes.createRandom(initialContentLength)); //try three different sizes of binary content
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a collection resource using supplied binary contents.</li>
	 * <li>Checking the existence of a collection resource.</li>
	 * <li>Changing the binary contents of a collection resource using an output stream.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 */
	@Test
	public void testChangeCollectionBinary() throws IOException {
		final int initialContentLength = (1 << 10) + 1;
		final URI resourceURI = repository.getRootURI().resolve("test/"); //determine a test collection resource URI
		testResourceContentBytes(resourceURI, false, new byte[0], Bytes.createRandom(initialContentLength), Bytes.createRandom(initialContentLength * 2),
				Bytes.createRandom(initialContentLength * 3), new byte[0], Bytes.createRandom(initialContentLength)); //try three different sizes of binary content
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a resource using supplied text contents.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>Changing the text contents of a resource using an output stream.</li>
	 * <li>Deleting a resource.</li>
	 * </ul>
	 */
	@Test
	public void testChangeResourceText() throws IOException {
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		testResourceContentBytes(resourceURI, false, "This is a test.".getBytes(UTF_8), "This is a test.\nThis is a second line.".getBytes(UTF_8),
				"This really is a test.\nThis is a second line.".getBytes(UTF_8), "This is just a test.".getBytes(UTF_8)); //try three different sizes of text content
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
	public void testCreateSmallResourceOutputStream() throws IOException {
		final int contentLength = (1 << 3) + 1; //>8
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		testResourceContentBytes(resourceURI, true, Bytes.createRandom(contentLength));
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
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
	public void testCreateLargeResourceOutputStream() throws IOException {
		final int contentLength = (1 << 20) + 1; //>1MB
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		testResourceContentBytes(resourceURI, true, Bytes.createRandom(contentLength));
		repository.deleteResource(resourceURI); //delete the resource we created
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating a resource using supplied contents.</li>
	 * <li>Checking the existence of a resource.</li>
	 * <li>If more than one content array is provided, changing the contents of a resource using an output stream.</li>
	 * </ul>
	 * @param resourceURI The URI of the resource to create.
	 * @param streamCreate Whether the resource should initially be created using an output stream rather than supplying the bytes.
	 * @param contents The array of byte arrays of content to use; the first will create the resource, the others will test modifying the resource
	 * @throws NullPointerException if the given resource URI and/or content array is <code>null</code>.
	 * @throws IllegalArgumentException if the array of contents is empty.
	 */
	protected void testResourceContentBytes(final URI resourceURI, final boolean streamCreate, final byte[]... contents) throws IOException {
		checkArgumentPositive(contents.length);
		Time before = new Time().floor(Time.Resolution.SECONDS);
		final Repository repository = getRepository();
		URFResource newResourceDescription;
		if(streamCreate) { //if we should create the resource using a stream
			final OutputStream outputStream = repository.createResource(resourceURI); //create a resource and get an output stream to the contents
			try {
				outputStream.write(contents[0]); //write the contents
			} finally {
				outputStream.close(); //close the output stream
			}
			newResourceDescription = repository.getResourceDescription(resourceURI); //get an updated description of the resource
		} else {
			newResourceDescription = repository.createResource(resourceURI, contents[0]); //create an initial resource with contents
		}
		Time after = new Time().floor(Time.Resolution.SECONDS);
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		assertThat("Invalid content length of created resource.", getContentLength(newResourceDescription), equalTo((long)contents[0].length));
		checkCreatedResourceDateTimes(newResourceDescription, before, after);
		byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(contents[0]));
		for(int i = 1; i < contents.length; ++i) { //look at all the alternate contents, if any
			final byte[] changedContent = contents[i];
			before = after; //shift the dates back
			final OutputStream outputStream = repository.getResourceOutputStream(resourceURI); //get an output stream to the existing resource
			try {
				outputStream.write(changedContent); //write the changed contents
			} finally {
				outputStream.close(); //close the output stream
			}
			after = new Time().floor(Time.Resolution.SECONDS);
			newResourceDescription = repository.getResourceDescription(resourceURI); //get an updated description of the resource
			assertTrue("Changed resource doesn't exist.", repository.resourceExists(resourceURI));
			assertThat("Invalid content length of changed resource.", getContentLength(newResourceDescription), equalTo((long)changedContent.length));
			checkCreatedResourceDateTimes(newResourceDescription, before, after);
		}
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
	public void testCreateCollectionResourceBytes() throws ResourceIOException {
		final int contentLength = (1 << 10) + 1;
		final Repository repository = getRepository();
		final byte[] resourceContents = Bytes.createRandom(contentLength); //create random contents
		final Time beforeCreateCollection = new Time().floor(Time.Resolution.SECONDS);
		final URI collectionURI = repository.getRootURI().resolve("test/"); //determine a test collection URI
		final URFResource newCollectionDescription = repository.createCollectionResource(collectionURI);
		assertTrue("Created collection resource doesn't exist.", repository.resourceExists(collectionURI));
		assertThat("Invalid content length of created collection resource.", getContentLength(newCollectionDescription), equalTo(0L));
		final Time beforeCreateResource = new Time().floor(Time.Resolution.SECONDS);
		final URI resourceURI = collectionURI.resolve("test.bin"); //determine a test resource URI
		final URFResource newResourceDescription = repository.createResource(resourceURI, resourceContents); //create a resource with random contents
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		checkCreatedResourceDateTimes(newCollectionDescription, beforeCreateCollection, beforeCreateResource);
		checkCreatedResourceDateTimes(newResourceDescription, beforeCreateResource, new Time().floor(Time.Resolution.SECONDS));
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
	 * <li>Setting properties individually on a normal resource.</li>
	 * <li>Retrieving properties individually on a normal resource.</li>
	 * <li>Updating properties individually on a normal resource.</li>
	 * </ul>
	 */
	@Test
	public void testResourceProperties() throws ResourceIOException {
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
	public void testCollectionResourceProperties() throws ResourceIOException {
		final Repository repository = getRepository();
		final URI collectionURI = repository.getRootURI().resolve("test/"); //determine a test collection URI
		repository.createCollectionResource(collectionURI);
		testResourceProperties(collectionURI);
		repository.deleteResource(collectionURI); //delete the resource we created
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Creating all needed parent resources for a deeply-nested resource.</li>
	 * <li>Checking the existence of a deeply-nested collection resource.</li>
	 * <li>Creating a resource inside a deeply-nested collection using supplied contents.</li>
	 * <li>Checking the existence of a deeply-nested resource inside a collection.</li>
	 * <li>Copying a deeply-nested resource up the hierarchy inside a collection.</li>
	 * <li>Copying a deeply-nested collection resource up the hierarchy inside a collection.</li>
	 * <li>Verifying that child collection descriptions use collection URIs.</li>
	 * <li>Deleting a deeply-nested collection resource with all its contents.</li>
	 * </ul>
	 */
	@Test
	public void testCopyMoveDeepResource() throws ResourceIOException {
		final int contentLength = (1 << 10) + 1;
		final Repository repository = getRepository();
		final byte[] resourceContents = Bytes.createRandom(contentLength); //create random contents
		final URI collection1URI = repository.getRootURI().resolve("test1/"); //determine some top-level collection
		final URI collection2URI = collection1URI.resolve("test2/");
		final URI collection3URI = collection2URI.resolve("test3/");
		final URI collection4URI = collection3URI.resolve("test4/");
		final URI collection5URI = collection4URI.resolve("test5/"); //determine a deeply-nested test collection URI
		final URI resourceURI = collection5URI.resolve("test.bin"); //determine a test resource URI
		//create parents
		URFResource newCollectionDescription = repository.createParentResources(resourceURI); //create all necessary parent resources for the resource
		assertThat("Last created parent resource doesn't match our deeply-nested collection.", newCollectionDescription.getURI(), equalTo(collection5URI));
		assertTrue("Created root collection resource doesn't exist.", repository.resourceExists(collection1URI));
		assertThat("Invalid content length of created root collection resource.", getContentLength(repository.getResourceDescription(collection1URI)), equalTo(0L));
		assertTrue("Created nested collection resource doesn't exist.", repository.resourceExists(collection5URI));
		assertThat("Invalid content length of created nested collection resource.", getContentLength(newCollectionDescription), equalTo(0L));
		//set collection properties
		final URFResource collection1PropertiesResource = createTestProperties(collection1URI, "Collection 1", "The first collection.");
		repository.setResourceProperties(collection1URI, collection1PropertiesResource.getProperties());
		final URFResource collection2PropertiesResource = createTestProperties(collection2URI, "Collection 2", "The second collection.");
		repository.setResourceProperties(collection2URI, collection2PropertiesResource.getProperties());
		final URFResource collection3PropertiesResource = createTestProperties(collection3URI, "Collection 3", "The third collection.");
		repository.setResourceProperties(collection3URI, collection3PropertiesResource.getProperties());
		final URFResource collection4PropertiesResource = createTestProperties(collection4URI, "Collection 4", "The fourth collection.");
		repository.setResourceProperties(collection4URI, collection4PropertiesResource.getProperties());
		final URFResource collection5PropertiesResource = createTestProperties(collection5URI, "Collection 5", "The fifth collection.");
		repository.setResourceProperties(collection5URI, collection5PropertiesResource.getProperties());
		//create resource
		URFResource newResourceDescription = repository.createResource(resourceURI, resourceContents); //create a resource with random contents
		assertTrue("Created resource doesn't exist.", repository.resourceExists(resourceURI));
		assertThat("Invalid content length of created resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		byte[] newResourceContents = repository.getResourceContents(resourceURI); //read the contents we wrote
		assertThat("Retrieved contents of created resource not what expected.", newResourceContents, equalTo(resourceContents));
		//set resource properties
		final URFResource resourcePropertiesResource = createTestProperties(resourceURI); //create test properties
		repository.setResourceProperties(resourceURI, resourcePropertiesResource.getProperties()); //set those properties individually
		//copy resource up hierarchy
		final URI copyResourceURI = collection3URI.resolve("copy.bin");
		repository.copyResource(resourceURI, copyResourceURI); //copy the resource
		newResourceDescription = repository.getResourceDescription(copyResourceURI); //get a description of the copied resource
		assertThat("Invalid content length of copied resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		newResourceContents = repository.getResourceContents(copyResourceURI); //read the contents we copied
		assertThat("Retrieved contents of copied resource not what expected.", newResourceContents, equalTo(resourceContents));
		checkResourceProperties(newResourceDescription, resourcePropertiesResource.getProperties()); //see if the copied resource has the same properties
		//copy collection resource up hierarchy
		final URI copyCollectionURI = collection3URI.resolve("copy/");
		repository.copyResource(collection5URI, copyCollectionURI); //copy test1/test2/test3/test4/test5/ to test1/test2/test3/copy/
		assertTrue("Copied collection resource doesn't exist.", repository.resourceExists(copyCollectionURI));
		newCollectionDescription = repository.getResourceDescription(copyCollectionURI); //get a description of the copied collection
		assertThat("Invalid content length of copied collection resource.", getContentLength(newCollectionDescription), equalTo(0L));
		checkResourceProperties(newCollectionDescription, collection5PropertiesResource.getProperties()); //see if the copied collection resource has the same properties
		final URI copyCollectionResourceURI = copyCollectionURI.resolve(getName(resourceURI));
		assertTrue("Copied collection nested resource doesn't exist.", repository.resourceExists(copyCollectionResourceURI));
		newResourceDescription = repository.getResourceDescription(copyCollectionResourceURI); //get a description of the resource inside the copied collection
		assertThat("Invalid content length of copied collection nested resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		newResourceContents = repository.getResourceContents(copyCollectionResourceURI); //read the contents of the resource copied along with the collection
		assertThat("Retrieved contents of copied collection nested resource not what expected.", newResourceContents, equalTo(resourceContents));
		checkResourceProperties(newResourceDescription, resourcePropertiesResource.getProperties()); //see if the resource in the copied collection has the same properties
		final List<URFResource> collection3Children = repository.getChildResourceDescriptions(collection3URI); //make sure the URIs of the children are what we expect
		assertThat("URIs of child resources not what expected", Sets.immutableSetOf(collection3Children), equalTo(Sets.<URFResource> immutableSetOf(
				new DefaultURFResource(collection4URI), new DefaultURFResource(copyResourceURI), new DefaultURFResource(copyCollectionURI))));
		//move resource up hierarchy
		final URI moveResourceURI = collection3URI.resolve("move.bin");
		repository.moveResource(resourceURI, moveResourceURI); //move the resource to test1/test2/test3/move.bin
		assertFalse("Moved resource still exists.", repository.resourceExists(resourceURI));
		newResourceDescription = repository.getResourceDescription(moveResourceURI); //get a description of the moved resource
		assertThat("Invalid content length of copied resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		newResourceContents = repository.getResourceContents(moveResourceURI); //read the contents we moved
		assertThat("Retrieved contents of copied resource not what expected.", newResourceContents, equalTo(resourceContents));
		checkResourceProperties(newResourceDescription, resourcePropertiesResource.getProperties()); //see if the moved resource has the same properties
		//move collection resource up hierarchy
		final URI moveCollectionURI = collection1URI.resolve("move/");
		repository.moveResource(collection3URI, moveCollectionURI); //move test1/test2/test3/ to test1/move/
		assertTrue("Moved collection resource doesn't exist.", repository.resourceExists(moveCollectionURI));
		newCollectionDescription = repository.getResourceDescription(moveCollectionURI); //get a description of the copied collection
		assertThat("Invalid content length of moved collection resource.", getContentLength(newCollectionDescription), equalTo(0L));
		checkResourceProperties(newCollectionDescription, collection3PropertiesResource.getProperties()); //see if the moved collection resource has the same properties
		final URI moveCollectionCopyResourceURI = moveCollectionURI.resolve(getName(copyResourceURI));
		assertTrue("Moved collection copied nested resource doesn't exist.", repository.resourceExists(moveCollectionCopyResourceURI));
		newResourceDescription = repository.getResourceDescription(moveCollectionCopyResourceURI); //get a description of the copied resource inside the moved collection
		assertThat("Invalid content length of moved collection copied nested resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		newResourceContents = repository.getResourceContents(moveCollectionCopyResourceURI); //read the contents of the copied resource moved along with the collection
		assertThat("Retrieved contents of moved collection copied nested resource not what expected.", newResourceContents, equalTo(resourceContents));
		checkResourceProperties(newResourceDescription, resourcePropertiesResource.getProperties()); //see if the copied resource in the moved collection has the same properties
		final URI moveCollectionMoveResourceURI = moveCollectionURI.resolve(getName(moveResourceURI));
		assertTrue("Moved collection moved nested resource doesn't exist.", repository.resourceExists(moveCollectionMoveResourceURI));
		newResourceDescription = repository.getResourceDescription(moveCollectionMoveResourceURI); //get a description of the moved resource inside the moved collection
		assertThat("Invalid content length of moved collection moved nested resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		newResourceContents = repository.getResourceContents(moveCollectionMoveResourceURI); //read the contents of the moved resource moved along with the collection
		assertThat("Retrieved contents of moved collection moved nested resource not what expected.", newResourceContents, equalTo(resourceContents));
		checkResourceProperties(newResourceDescription, resourcePropertiesResource.getProperties()); //see if the moved resource in the moved collection has the same properties
		//create resource2 and copy with overwrite, using distinct properties
		final URI resource2URI = collection1URI.resolve("test2.bin"); //test1/test2.bin
		newResourceDescription = repository.createResource(resource2URI, resourceContents); //create a resource with random contents
		final URFResource resource2PropertiesResource = createTestProperties(resource2URI, "Resource2", "Second Resource"); //create test properties
		repository.setResourceProperties(resource2URI, resource2PropertiesResource.getProperties()); //set those properties individually
		repository.copyResource(resource2URI, moveCollectionCopyResourceURI); //copy the resource, overwriting the old copy.bin
		newResourceDescription = repository.getResourceDescription(moveCollectionCopyResourceURI); //get a description of the copied resource
		assertThat("Invalid content length of copied resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		newResourceContents = repository.getResourceContents(moveCollectionCopyResourceURI); //read the contents we copied
		assertThat("Retrieved contents of copied resource not what expected.", newResourceContents, equalTo(resourceContents));
		checkResourceProperties(newResourceDescription, resource2PropertiesResource.getProperties()); //see if the copied resource has the expected properties
		//create resource3 and move with overwrite, using distinct properties
		final URI resource3URI = collection1URI.resolve("test3.bin"); //test1/test3.bin
		newResourceDescription = repository.createResource(resource3URI, resourceContents); //create a resource with random contents
		final URFResource resource3PropertiesResource = createTestProperties(resource3URI, "Resource3", "Third Resource"); //create test properties
		repository.setResourceProperties(resource3URI, resource3PropertiesResource.getProperties()); //set those properties individually
		repository.moveResource(resource3URI, moveCollectionMoveResourceURI); //move the resource, overwriting the old move.bin
		assertFalse("Moved resource still exists.", repository.resourceExists(resourceURI));
		newResourceDescription = repository.getResourceDescription(moveCollectionMoveResourceURI); //get a description of the moved resource
		assertThat("Invalid content length of copied resource.", getContentLength(newResourceDescription), equalTo((long)contentLength));
		newResourceContents = repository.getResourceContents(moveCollectionMoveResourceURI); //read the contents we moved
		assertThat("Retrieved contents of copied resource not what expected.", newResourceContents, equalTo(resourceContents));
		checkResourceProperties(newResourceDescription, resource3PropertiesResource.getProperties()); //see if the moved resource has the expected properties
		//delete resource
		repository.deleteResource(collection1URI); //delete the root collection resource we created, with its contained deeply-nested collections and resource
		assertFalse("Deleted root collection resource still exists.", repository.resourceExists(collection1URI));
		assertFalse("Deleted nested collection resource still exists.", repository.resourceExists(collection5URI));
		assertFalse("Deleted resource still exists.", repository.resourceExists(resourceURI));
		assertFalse("Deleted copied resource still exists.", repository.resourceExists(copyResourceURI));
		assertFalse("Deleted copied collection resource still exists.", repository.resourceExists(copyCollectionURI));
		assertFalse("Deleted copied collection nested resource still exists.", repository.resourceExists(copyCollectionResourceURI));
		assertFalse("Deleted moved collection resource still exists.", repository.resourceExists(moveCollectionURI));
		assertFalse("Deleted moved collection copied nested resource still exists.", repository.resourceExists(moveCollectionCopyResourceURI));
		assertFalse("Deleted moved collection moved nested resource still exists.", repository.resourceExists(moveCollectionCopyResourceURI));
	}

	/**
	 * Tests:
	 * <ul>
	 * <li>Retrieving the description of a non-existing resource.</li>
	 * </ul>
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void testMissingResourceDescription() throws ResourceIOException {
		final URI resourceURI = repository.getRootURI().resolve("test.bin"); //determine a test resource URI
		repository.getResourceDescription(resourceURI); //get a description of a resource that does not exist, which should throw an exception
	}

	/**
	 * Ensures that the dates of a created resource are valid.
	 * @param resourceDescription The description of the created resource.
	 * @param before Some time before the creation of the file
	 * @param after Some time after the creation of the file
	 */
	protected void checkCreatedResourceDateTimes(final URFResource resourceDescription, final Time before, final Time after) {
		Time modified = getModified(resourceDescription);
		if(!isCollectionURI(resourceDescription.getURI())) { //modified datetime is optional for collections
			assertNotNull("Missing modified datetime.", modified);
		}
		if(modified != null) {
			modified = modified.floor(Time.Resolution.SECONDS);
			assertTrue("Modified datetime not in expected range (" + before + ", " + modified + ", " + after + ")",
					(modified.equals(before) || modified.after(before)) && (modified.equals(after) || modified.before(after)));
		}
		Time created = getCreated(resourceDescription);
		if(created != null) {
			created = created.floor(Time.Resolution.SECONDS);
			assertTrue("Modified datetime not equal to created datetime.", !created.after(modified)); //in all the repositories, the created time should never be past the modified time
			//TODO bring back when all repositories support retrieving saved modified date			assertThat("Modified datetime not equal to created datetime.", created, equalTo(modified));
		}
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
	protected void testResourceProperties(final URI resourceURI) throws ResourceIOException {
		final Repository repository = getRepository();
		final URFResource newResourceDescription = repository.getResourceDescription(resourceURI); //get the initial description
		final long contentLength = getContentLength(newResourceDescription);
		Time created = getCreated(newResourceDescription); //optional
		Time modified = getModified(newResourceDescription);
		final long propertyCount = 1L + (created != null ? 1L : 0L) + (modified != null ? 1L : 0L);
		assertThat(newResourceDescription.getPropertyCount(), equalTo(propertyCount)); //content length, content created, content modified
		final URFResource propertiesResource = createTestProperties(resourceURI); //create test properties
		URFResource updatedResourceDescription = repository.setResourceProperties(resourceURI, propertiesResource.getProperties()); //set those properties individually
		assertThat("Retrieved resource description doesn't match that returned from resource setting.", repository.getResourceDescription(resourceURI),
				equalTo(updatedResourceDescription));
		assertThat(updatedResourceDescription.getPropertyCount(), equalTo(propertyCount + propertiesResource.getPropertyCount())); //see if the new property count is correct 
		checkResourceProperties(updatedResourceDescription, propertiesResource.getProperties()); //see if the resource now has the given properties
		assertThat("Content length changed.", getContentLength(updatedResourceDescription), equalTo(contentLength));
		if(created != null && !repository.isLivePropertyURI(CREATED_PROPERTY_URI)) { //if the content.created property is live, we can't expect any particular values
			created = created.floor(Time.Resolution.SECONDS);
			assertThat("Created date changed.", getCreated(updatedResourceDescription).floor(Time.Resolution.SECONDS), equalTo(created));
		}
		if(modified != null && !repository.isLivePropertyURI(MODIFIED_PROPERTY_URI)) { //if the content.modified property is live, we can't expect any particular values
			modified = modified.floor(Time.Resolution.SECONDS);
			assertThat("Modified date changed.", getModified(updatedResourceDescription).floor(Time.Resolution.SECONDS), equalTo(modified));
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
	 * Creates a resource description with test properties, including the following properties:
	 * <ul>
	 * <li>{@link DCMI#TITLE_PROPERTY_URI}</li>
	 * <li>{@link DCMI#DESCRIPTION_PROPERTY_URI}</li>
	 * <li>{@link DCMI#DATE_PROPERTY_URI}</li>
	 * <li>{@link DCMI#LANGUAGE_PROPERTY_URI}</li>
	 * </ul>
	 * @param resourceURI The URI of the description to create.
	 * @return A description of test properties.
	 * @throws NullPointerException if the given resource URI is <code>null</code>.
	 */
	protected URFResource createTestProperties(final URI resourceURI) {
		return createTestProperties(resourceURI, "Test Title", "This is a test description.");
	}

	/**
	 * Creates a resource description with test properties, including the following properties:
	 * <ul>
	 * <li>{@link DCMI#TITLE_PROPERTY_URI}</li>
	 * <li>{@link DCMI#DESCRIPTION_PROPERTY_URI}</li>
	 * <li>{@link DCMI#DATE_PROPERTY_URI}</li>
	 * <li>{@link DCMI#LANGUAGE_PROPERTY_URI}</li>
	 * </ul>
	 * @param resourceURI The URI of the description to create.
	 * @param title The DCMI title to use.
	 * @param description The DCMI description to use.
	 * @return A description of test properties.
	 * @throws NullPointerException if the given resource URI, title, and/or description is <code>null</code>.
	 */
	protected URFResource createTestProperties(final URI resourceURI, final String title, final String description) {
		final URFResource resource = new DefaultURFResource(resourceURI);
		setTitle(resource, requireNonNull(title));
		setDescription(resource, requireNonNull(description));
		setDate(resource, new ISODateTime());
		setLanguage(resource, Locale.ENGLISH);
		return resource;
	}

	/**
	 * Ensures that the properties of a given resource description matches the given properties.
	 * @param resourceDescription The description of the created resource.
	 * @param properties The properties to check.
	 */
	protected void checkResourceProperties(final URFResource resourceDescription, final Iterable<URFProperty> properties) {
		for(final URFProperty property : properties) {
			assertThat("Resource " + resourceDescription + " doesn't have expected property value for " + property.getPropertyURI(),
					resourceDescription.getPropertyValue(property.getPropertyURI()), equalTo(property.getValue()));
		}
	}

	@After
	public void after() throws IOException {
		repository = null;
	}

}
