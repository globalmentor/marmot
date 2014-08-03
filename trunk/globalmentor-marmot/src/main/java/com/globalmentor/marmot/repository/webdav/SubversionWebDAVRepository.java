/*
 * Copyright © 1996-2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository.webdav;

import java.net.*;
import java.util.Set;

import com.globalmentor.marmot.Marmot;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.marmot.repository.svn.MarmotSubversion;
import com.globalmentor.net.URIs;
import com.globalmentor.net.http.*;
import com.globalmentor.net.http.webdav.*;

import static com.globalmentor.apache.subversion.Subversion.getPropertyNamespace;
import static com.globalmentor.marmot.repository.svn.MarmotSubversion.PROPERTY_PREFIX;
import static com.globalmentor.marmot.repository.svn.MarmotSubversion.decodePropertyURIPropertyName;
import static com.globalmentor.net.http.webdav.SubversionWebDAV.*;

/**
 * Subversion repository accessed via WebDAV with autoversioning turned on.
 * <p>
 * Autoversioning must be turned on in Apache using <code>SVNAutoversioning on</code>.
 * </p>
 * <p>
 * This version stores URF properties by using a URI that is the concatenation of {@value SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI} and the encoded
 * version of the URF property URI, using {@value AbstractRepository#PROPERTY_NAME_URI_ESCAPE_CHAR} as the escape character. The standard URI escape character,
 * {@value URIs#ESCAPE_CHAR}, is not a valid name character, so {@value AbstractRepository#PROPERTY_NAME_URI_ESCAPE_CHAR}, which conveniently is not a valid URI
 * character, is used instead.
 * </p>
 * @author Garret Wilson
 * @see Repositories
 */
public class SubversionWebDAVRepository extends WebDAVRepository {

	/** The Subversion custom property namespace converted to a string for quick string comparisons. */
	protected final static String SUBVERSION_CUSTOM_NAMESPACE = SUBVERSION_CUSTOM_NAMESPACE_URI.toString();

	/**
	 * Default constructor with no root URI defined. The root URI must be defined before the repository is opened.
	 */
	public SubversionWebDAVRepository() {
		this(null);
	}

	/**
	 * Repository URI constructor using the default HTTP client. The given repository URI should end in a slash.
	 * @param repositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	 */
	public SubversionWebDAVRepository(final URI repositoryURI) {
		this(repositoryURI, HTTPClient.getInstance()); //construct the class using the default HTTP client		
	}

	/**
	 * Repository URI and HTTP client constructor. The given repository URI should end in a slash.
	 * @param repositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	 * @param httpClient The HTTP client used to create a connection to this resource.
	 */
	public SubversionWebDAVRepository(final URI repositoryURI, final HTTPClient httpClient) {
		this(repositoryURI, repositoryURI, httpClient); //use the same repository URI as the public and private namespaces
	}

	/**
	 * Public repository URI and private repository URI constructor using the default HTTP client. The given private repository URI should end in a slash.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	 */
	public SubversionWebDAVRepository(final URI publicRepositoryURI, final URI privateRepositoryURI) {
		this(publicRepositoryURI, privateRepositoryURI, HTTPClient.getInstance()); //construct the class using the default HTTP client				
	}

	/**
	 * Public repository URI, private repository URI, and HTTP client constructor. The given private repository URI should end in a slash.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	 * @param httpClient The HTTP client used to create a connection to this resource.
	 */
	public SubversionWebDAVRepository(final URI publicRepositoryURI, final URI privateRepositoryURI, final HTTPClient httpClient) {
		super(publicRepositoryURI, privateRepositoryURI, httpClient); //construct the parent class
		final Set<String> ignoredWebDAVNamespaces = getIgnoredWebDAVNamespaces(); //get the map of ignored WebDAV namespaces
		ignoredWebDAVNamespaces.add(SUBVERSION_DAV_NAMESPACE_URI.toString()); //by default ignore the Subversion DAV namespace
	}

	/**
	 * Creates a repository of the same type as this repository with the same access privileges as this one. This factory method is commonly used to use a parent
	 * repository as a factory for other repositories in its namespace.
	 * @param publicRepositoryURI The public URI identifying the location of the new repository.
	 * @param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	 * @throws NullPointerException if the given public repository URI and/or private repository URI is <code>null</code>.
	 */
	protected Repository createSubrepository(final URI publicRepositoryURI, final URI privateRepositoryURI) {
		final SubversionWebDAVRepository repository = new SubversionWebDAVRepository(publicRepositoryURI, privateRepositoryURI, getHTTPClient()); //create a new repository
		repository.setUsername(getUsername()); //transfer authentication info
		repository.setPassword(getPassword()); //transfer authentication info
		return repository; //return the new repository
	}

	/**
	 * Determines the WebDAV property name to represent an URF property. This version uses the encoded URF property URI as the local name of the
	 * {@value SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI} namespace. The standard URI escape character, {@value URIs#ESCAPE_CHAR}, is not a valid name
	 * character, so {@value #PROPERTY_NAME_URI_ESCAPE_CHAR}, which conveniently is not a valid URI character, is used instead.
	 * @param urfPropertyURI The URI of the URF property to represent.
	 * @return A WebDAV property name to use in representing an URF property with the given URF property URI.
	 * @see SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI
	 * @see AbstractRepository#PROPERTY_NAME_URI_ESCAPE_CHAR
	 * @see AbstractRepository#encodePropertyURILocalName(URI)
	 */
	protected WebDAVPropertyName createWebDAVPropertyName(final URI urfPropertyURI) {
		return new WebDAVPropertyName(SUBVERSION_CUSTOM_NAMESPACE, MarmotSubversion.encodePropertyURIPropertyName(urfPropertyURI)); //create and return a new WebDAV property name in the Subversion custom property namespace
	}

	/**
	 * Determines the URF property to represent the given WebDAV property if possible. If the WebDAV property has a local name of
	 * {@value SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI}, the decoded form of its local name, if an absolute URI, will be used as the URF property URI.
	 * The standard URI escape character, {@value URIs#ESCAPE_CHAR}, is not a valid name character, so {@value AbstractRepository#PROPERTY_NAME_URI_ESCAPE_CHAR},
	 * which conveniently is not a valid URI character, is used instead. Otherwise, this method delegates to the super version.
	 * @param webdavPropertyName The name of the WebDAV property.
	 * @return The URI of the URF property to represent the given WebDAV property, or <code>null</code> if the given WebDAV property cannot be represented in URF.
	 * @throws IllegalArgumentException if the given property is not encoded properly.
	 * @see SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI
	 * @see AbstractRepository#PROPERTY_NAME_URI_ESCAPE_CHAR
	 * @see AbstractRepository#decodePropertyURILocalName(String)
	 */
	protected URI getURFPropertyURI(final WebDAVPropertyName webdavPropertyName) {
		if(SUBVERSION_CUSTOM_NAMESPACE.equals(webdavPropertyName.getNamespace())) { //if this is the Subversion custom property namespace
			final String propertyName = webdavPropertyName.getLocalName(); //get the property name
			if(propertyName.startsWith(PROPERTY_PREFIX) || Marmot.ID.equals(getPropertyNamespace(propertyName))) { //TODO once legacy properties are changed, remove namespace check
				return decodePropertyURIPropertyName(webdavPropertyName.getLocalName()); //the URF property URI may be encoded as the local name of the Subversion custom property
			}
		}
		return super.getURFPropertyURI(webdavPropertyName); //if this doesn't appear to be an URF property, treat the property as a normal WebDAV property
	}

}
