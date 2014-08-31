/*
 * Copyright © 1996-2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository.http;

import java.io.*;

import java.net.*;
import java.text.DateFormat;
import java.util.*;

import org.urframework.*;
import org.urframework.content.*;
import org.urframework.io.URFResourceTURFIO;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.urframework.TURF.*;
import static org.urframework.URF.*;
import static org.urframework.content.Content.*;
import static org.urframework.dcmi.DCMI.*;

import static com.globalmentor.java.Objects.*;
import static com.globalmentor.text.xml.XML.*;

import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.model.Locales.*;
import static com.globalmentor.net.URIs.*;

import com.globalmentor.io.*;
import static com.globalmentor.io.Charsets.*;
import com.globalmentor.java.Strings;
import com.globalmentor.marmot.Marmot;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.net.*;
import com.globalmentor.net.http.*;
import com.globalmentor.util.*;

/**
 * Abstract repository accessed via HTTP.
 * @author Garret Wilson
 */
public abstract class AbstractHTTPRepository extends AbstractHierarchicalSourceRepository {

	//TODO the current technique of erasing the password after each call may become obsolete when the HTTP client supports persistent connections

	/** The extension used for directories to hold resource children. */
	//TODO move if needed	protected static final String DIRECTORY_EXTENSION="@";	//TODO promote to parent file-based class

	/**
	 * Repository URI constructor using the default HTTP client. The given repository URI should end in a slash.
	 * @param repositoryURI The base URI of the repository.
	 */
	public AbstractHTTPRepository(final URI repositoryURI) {
		this(repositoryURI, HTTPClient.getInstance()); //construct the class using the default HTTP client		
	}

	/**
	 * Repository URI and HTTP client constructor. The given repository URI should end in a slash.
	 * @param repositoryURI The base URI of the repository.
	 * @param httpClient The HTTP client used to create a connection to this resource.
	 */
	public AbstractHTTPRepository(final URI repositoryURI, final HTTPClient httpClient) {
		this(repositoryURI, repositoryURI, httpClient); //use the same repository URI as the public and private namespaces
	}

	/**
	 * Public repository URI and private repository URI constructor using the default HTTP client. The given private repository URI should end in a slash.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryURI The base URI of the repository.
	 */
	public AbstractHTTPRepository(final URI publicRepositoryURI, final URI privateRepositoryURI) {
		this(publicRepositoryURI, privateRepositoryURI, HTTPClient.getInstance()); //construct the class using the default HTTP client				
	}

	/**
	 * Public repository URI, private repository URI, and HTTP client constructor. The given private repository URI should end in a slash.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryURI The base URI of the repository.
	 * @param httpClient The HTTP client used to create a connection to this resource.
	 */
	public AbstractHTTPRepository(final URI publicRepositoryURI, final URI privateRepositoryURI, final HTTPClient httpClient) {
		super(publicRepositoryURI, privateRepositoryURI); //construct the parent class
		this.httpClient = httpClient; //save the HTTP client
		final URFResourceTURFIO<URFResource> urfResourceDescriptionIO = (URFResourceTURFIO<URFResource>)getDescriptionIO(); //get the description I/O
		urfResourceDescriptionIO.setBOMWritten(false); //turn off BOM generation
		urfResourceDescriptionIO.setFormatted(false); //turn off formatting
	}

	/** The HTTP client used to create a connection to this resource. */
	private final HTTPClient httpClient;

	/** @return The HTTP client used to create a connection to this resource. */
	protected HTTPClient getHTTPClient() {
		return httpClient;
	}

	/** The username to use in accessing the repository, or <code>null</code> if no username is specified. */
	private String username = null;

	/** @return The username to use in accessing the repository, or <code>null</code> if no username is specified. */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the username to use in accessing the repository.
	 * @param username The username to use in accessing the repository, or <code>null</code> if no username is specified.
	 */
	public void setUsername(final String username) {
		this.username = username;
	}

	/** The password to use in accessing the repository, or <code>null</code> if no password is specified. */
	private char[] password = null;

	/** @return The username to use in accessing the repository, or <code>null</code> if no password is specified. */
	public char[] getPassword() {
		return password;
	}

	/**
	 * Sets the password to use in accessing the repository.
	 * @param password The password to use in accessing the repository, or <code>null</code> if no password is specified.
	 */
	public void setPassword(final char[] password) {
		this.password = password;
	}

	/**
	 * Returns whatever password authentication should be used when communicating with a resource.
	 * @return A password authentication object with the repository's username and password, or <code>null</code> if no username and password are specified.
	 * @see #getUsername()
	 * @see #getPassword()
	 */
	protected PasswordAuthentication getPasswordAuthentication() {
		final String username = getUsername(); //get the username
		final char[] password = getPassword(); //get the password
		return username != null && password != null ? new PasswordAuthentication(username, password) : null; //return new password authentication if this information is available
	}

	/**
	 * Creates an HTTP resource object to communicate with the indicated resource on the server.
	 * @param privateResourceURI The URI of the resource in the private space.
	 * @param passwordAuthentication A password authentication object with the repository's username and password, or <code>null</code> if no username and
	 *          password are specified.
	 * @return A HTTP resource object for communicating with the indicated resource.
	 */
	protected HTTPResource createHTTPResource(final URI privateResourceURI, final PasswordAuthentication passwordAuthentication) {
		return new HTTPResource(privateResourceURI, getHTTPClient(), passwordAuthentication);
	}

	/** {@inheritDoc} This implementation returns <code>false</code> for all resources for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	protected boolean resourceExistsImpl(URI resourceURI) throws ResourceIOException {
		final URI privateResourceURI = getSourceResourceURI(resourceURI); //get the resource URI in the private space
		if(!isSourceResourceVisible(privateResourceURI)) { //if this resource should not be public
			return false; //ignore this resource
		}
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try {
			final HTTPResource httpResource = createHTTPResource(privateResourceURI, passwordAuthentication); //create an HTTP resource
			return httpResource.exists(); //see if the HTTP resource exists		
		} catch(final HTTPRedirectException httpRedirectException) { //if the HTTP resource tries to redirect us somewhere else
			return false; //consider this to indicate that the resource, as identified by the resource URI, does not exist
		} catch(final IOException ioException) { //if an I/O exception occurs
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		} finally {
			if(passwordAuthentication != null) { //if we used password authentication
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/** {@inheritDoc} For collections, this implementation retrieves the content of the {@value #COLLECTION_CONTENT_NAME} file, if any. */
	@Override
	protected InputStream getResourceInputStreamImpl(final URI resourceURI) throws ResourceIOException {
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try {
			if(isCollectionURI(resourceURI)) { //if the resource is a collection
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				final HTTPResource contentHTTPResource = createHTTPResource(getSourceResourceURI(contentURI), passwordAuthentication); //create a resource for special collection content resource
				if(contentHTTPResource.exists()) { //if there is a special collection content resource
					return contentHTTPResource.getInputStream(); //return an input stream to the collection content resource
				} else { //if there is no collection content resource
					return new ByteArrayInputStream(NO_BYTES); //return an input stream to an empty byte array
				}
			} else { //if the resource is not a collection
				final HTTPResource httpResource = createHTTPResource(getSourceResourceURI(resourceURI), passwordAuthentication); //create an HTTP resource
				return httpResource.getInputStream(); //return an input stream to the resource
			}
		} catch(final IOException ioException) { //if an I/O exception occurs
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		} finally {
			if(passwordAuthentication != null) { //if we used password authentication
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/** {@inheritDoc} For collections, this implementation stores the content in the {@value #COLLECTION_CONTENT_NAME} file. */
	/*TODO fix
		@Override
		protected OutputStream getResourceOutputStreamImpl(final URI resourceURI, final URFDateTime newContentModified) throws ResourceIOException
		{
			final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
			try
			{
				final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource TODO cache these resources, maybe TODO remove distinction between URI collection and WebDAV collection
				if(!webdavResource.exists()) {	//if the resource doesn't already exist
					throw new ResourceNotFoundException(resourceURI, "Cannot open output stream to non-existent resource "+resourceURI);
				}
				final WebDAVResource contentWebDAVResource;	//determine the WebDAV resource for accessing the content file
				if(isCollectionURI(resourceURI) && isCollection(resourceURI)) {	//if the resource is a collection (make sure the resource URI is also a collection URI so that we can be sure of resolving the collection content name; WebDAV collections should only have collection URIs anyway) TODO remove distinction between URI collection and WebDAV collection
					final URI contentURI=resolve(resourceURI, COLLECTION_CONTENT_NAME);	//determine the URI to use for content
					contentWebDAVResource=new WebDAVResource(getPrivateURI(contentURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource for special collection content resource
				}
				else {	//if the resource is not a collection
					contentWebDAVResource=webdavResource;	//use the normal WebDAV resource
				}
				OutputStream outputStream=contentWebDAVResource.getOutputStream();	//get an output stream to the content WebDAV resource
				if(newContentModified!=null) {	//if we should update the content modified datetime
					final URFResourceAlteration resourceAlteration=DefaultURFResourceAlteration.createSetPropertiesAlteration(new DefaultURFProperty(Content.MODIFIED_PROPERTY_URI, newContentModified));	//create a resource alteration for setting the content modified property
					outputStream=new DescriptionWriterOutputStreamDecorator(outputStream, resourceURI, resourceAlteration, webdavResource, passwordAuthentication);	//wrap the output stream in a decorator that will update the WebDAV properties after the contents are stored; this method will erase the provided password, if any, after it completes the resource property updates
				}
				return outputStream;	//return the output stream we created
			}
			catch(final IOException ioException) {	//if an I/O exception occurs
				throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
			}
			finally
			{
				if(newContentModified==null && passwordAuthentication!=null) {	//if we didn't do a delayed write we used password authentication
					fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
				}
			}
		}
	*/

	/**
	 * Creates a new resource with the given description and returns an output stream for writing the contents of the resource. If a resource already exists at
	 * the given URI it will be replaced. The returned output stream should always be closed. If a resource with no contents is desired,
	 * {@link #createResource(URI, URFResource, byte[])} with zero bytes is better suited for this task. This implementation updates the resource description
	 * after its contents are stored.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @return An output stream for storing the contents of the resource.
	 * @throws NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	/*TODO fix
		public OutputStream createResource(URI resourceURI, final URFResource resourceDescription) throws ResourceIOException {	//TODO fix to prevent resources with special names
			resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
			final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
			if(subrepository!=this) {	//if the resource URI lies within a subrepository
				return subrepository.createResource(resourceURI, resourceDescription);	//delegate to the subrepository
			}
			checkOpen();	//make sure the repository is open
			final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
			try
			{
				final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
				final WebDAVResource contentWebDAVResource;	//determine the WebDAV resource for accessing the content file
				if(isCollectionURI(resourceURI)) {	//if this is a collection
					webdavResource.mkCol();	//create the collection
					final URI contentURI=resolve(resourceURI, COLLECTION_CONTENT_NAME);	//determine the URI to use for content
					contentWebDAVResource=new WebDAVResource(getPrivateURI(contentURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource for special collection content resource
				}
				else {	//if this is not a collection
					contentWebDAVResource=webdavResource;	//use the normal WebDAV resource
				}
				final OutputStream outputStream=contentWebDAVResource.getOutputStream();	//get an output stream to the content WebDAV resource
				return new DescriptionWriterOutputStreamDecorator(outputStream, resourceURI, DefaultURFResourceAlteration.createResourceAlteration(resourceDescription), webdavResource, passwordAuthentication);	//wrap the output stream in a decorator that will update the WebDAV properties after the contents are stored; this method will erase the provided password, if any, after it completes the resource property updates
			}
			catch(final IOException ioException) {	//if an I/O exception occurs
				throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
			}
		}
	*/

	/**
	 * Creates a new resource with the given description and contents. If a resource already exists at the given URI it will be replaced.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @param resourceContents The contents to store in the resource.
	 * @return A description of the resource that was created.
	 * @throws NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	/*TODO fix
		public URFResource createResource(URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents) throws ResourceIOException {	//TODO fix to prevent resources with special names
			resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
			final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
			if(subrepository!=this) {	//if the resource URI lies within a subrepository
				return subrepository.createResource(resourceURI, resourceDescription, resourceContents);	//delegate to the subrepository
			}
			checkOpen();	//make sure the repository is open
			final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
			try
			{
				final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
				final WebDAVResource contentWebDAVResource;	//determine the WebDAV resource for accessing the content file
				if(isCollectionURI(resourceURI)) {	//if this is a collection
					webdavResource.mkCol();	//create the collection
					final URI contentURI=resolve(resourceURI, COLLECTION_CONTENT_NAME);	//determine the URI to use for content
					contentWebDAVResource=new WebDAVResource(getPrivateURI(contentURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource for special collection content resource
				}
				else {	//if this is not a collection
					contentWebDAVResource=webdavResource;	//use the normal WebDAV resource
				}
				if(resourceContents.length>0 || !isCollectionURI(resourceURI)) {	//don't write empty content for a new collection
					contentWebDAVResource.put(resourceContents);	//create the content WebDAV resource with the given contents
				}
	  		return alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createResourceAlteration(resourceDescription), webdavResource);	//set the properties using the WebDAV resource object
			}
			catch(final IOException ioException) {	//if an I/O exception occurs
				throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
			}
			catch(final DataException dataException) {	//if the data wasn't correct
				throw createResourceIOException(resourceURI, dataException);	//translate the exception to a resource I/O exception and throw that
			}
			finally
			{
				if(passwordAuthentication!=null) {	//if we used password authentication
					fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
				}
			}
		}
	*/

	/** {@inheritDoc} This implementation ignores requests to delete all resource for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	protected void deleteResourceImpl(final URI resourceURI) throws ResourceIOException {
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try {
			final URI sourceResourceURI = getSourceResourceURI(resourceURI);
			if(isSourceResourceVisible(sourceResourceURI)) { //if this is a visible resource
				final HTTPResource httpResource = createHTTPResource(sourceResourceURI, passwordAuthentication); //create an HTTP resource
				httpResource.delete(); //delete the resource
			}
		} catch(final IOException ioException) { //if an I/O exception occurs
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		} finally {
			if(passwordAuthentication != null) { //if we used password authentication
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**
	 * {@inheritDoc} This version makes the following translations:
	 * <dl>
	 * <dt>{@link HTTPForbiddenException}</dt>
	 * <dd>{@link ResourceForbiddenException}</dd>
	 * <dt>{@link HTTPNotFoundException}</dt>
	 * <dd>{@link ResourceNotFoundException}</dd>
	 * <dt>{@link HTTPRedirectException}</dt>
	 * <dd>{@link ResourceNotFoundException}</dd>
	 * <dt>{@link HTTPPreconditionFailedException}</dt>
	 * <dd>{@link ResourceStateException}</dd>
	 * </dl>
	 */
	@Override
	protected ResourceIOException toResourceIOException(final URI resourceURI, final Throwable throwable) {
		if(throwable instanceof HTTPForbiddenException) {
			return new ResourceForbiddenException(resourceURI, throwable);
		} else if(throwable instanceof HTTPNotFoundException) {
			return new ResourceNotFoundException(resourceURI, throwable);
		} else if(throwable instanceof HTTPRedirectException) {
			return new ResourceNotFoundException(resourceURI, throwable);
		} else if(throwable instanceof HTTPPreconditionFailedException) {
			return new ResourceStateException(resourceURI, throwable);
		} else { //if this is not one of our specially-handled exceptions
			return super.toResourceIOException(resourceURI, throwable); //convert the exception normally
		}
	}

}
