package com.globalmentor.marmot.repository;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;

import javax.mail.internet.ContentType;

import com.globalmentor.net.*;
import com.globalmentor.urf.*;

/**A Marmot information store.
<p>A repository has the concept of a <dfn>live property</dfn> which are dynamically set based upon resource state, such as content size and last modified date/time.
Live properties cannot be set using normal property manipulation methods and are ignored if requested to be modified.</li>
@author Garret Wilson
*/
public interface Repository
{

	/**@return The URI identifying the location of this repository.*/
	public URI getURI();

	/**@return The base URI of the private URI namespace being managed, which may be the same as the public URI of this repository.*/
	public URI getPrivateRepositoryURI();

	/**Sets the base URI of the private URI namespace being managed.
	@param privateRepositoryURI The base URI of the private URI namespace being managed.
	@exception NullPointerException if the given URI is <code>null</code>.
	*/
	public void setPrivateRepositoryURI(final URI privateRepositoryURI);
	
	/**@return The base URI of the public URI namespace being managed; equivalent to {@link #getURI()}.*/
	public URI getPublicRepositoryURI();

	/**Sets the base URI of the public URI namespace being managed, reference URI of the repository.
	If there currently is no private repository URI, it will be updated to match the given public repository URI.
	@param publicRepositoryURI The base URI of the public URI namespace being managed.
	@exception NullPointerException if the given URI is <code>null</code>.
	*/
	public void setPublicRepositoryURI(final URI publicRepositoryURI);

	/**@return Whether the repository has been opened for access.*/
	public boolean isOpen();

	/**Opens the repository for access.
	If the repository is already open, no action occurs.
	At a minimum the respository must have a public and a private URI specified, even though these may both be the same URI. 
	@exception IllegalStateException if the settings of this repository are inadequate to open the repository.
	@exception ResourceIOException if there is an error opening the repository.
	*/
	public void open() throws ResourceIOException;

	/**Closes the repository.
	If the repository is already closed, no action occurs.
	@exeption ResourceIOException if there is an error closing the repository.
	*/
	public void close() throws ResourceIOException;

	/**Retrieves the live properties, which dynamically determined attributes of the resource such as content size. 
	@return The thread-safe set of URIs of live properties.
	*/
	public Set<URI> getLivePropertyURIs();

	/**Associates the given content type with the given extension, without regard to case.
	@param extension The URI name extension with which the content type should be associated, or <code>null</code> if the content type should be associated with resources that have no extension.
	@param contentType The content type to associate with the given extension.
	@return The content type previously registered with the given extension, or <code>null</code> if no content type was previously registered.
	@exception NullPointerException if the given content type is <code>null</code>.
	*/
	public ContentType registerExtensionContentType(final String extension, final ContentType contentType);

	/**Returns the content type assciated with the given extension, without regard to case.
	@param extension The URI name extension with which the content type is associated, or <code>null</code> if the content type is associated with resources that have no extension.
	@return The content type associated with the given extension, or <code>null</code> if there is no content type associated with the given extension.
	*/
	public ContentType getExtensionContentType(final String extension);

	/**Associates the given charset with the base media type of the given content type.
	Any association will only override resources that do not explicitly have a charset specified.
	Any parameters of the given content type will be ignored.
	@param contentType The content type with which the charset should be associated.
	@param charset The charset to associate with the given content type.
	@return The charset previously registered with the given content type, or <code>null</code> if no charset was previously registered.
	@exception NullPointerException if the given content type and/or charset is <code>null</code>.
	*/
	public Charset registerContentTypeCharset(final ContentType contentType, final Charset charset);

	/**Returns the charset assciated with the given conten type.
	Any parameters of the given content type will be ignored.
	@param contentType The content type with which the charset is associated.
	@return The charset associated with the given content type, or <code>null</code> if there is no charset associated with the given content type.
	@exception NullPointerException if the given content type is <code>null</code>.
	*/
	public Charset getContentTypeCharset(final ContentType contentType);

	/**@return the mapping of charsets associated with base content types.*/
	public Map<ContentType, Charset> getContentTypeCharsets();

	/**Sets the content type charset associations to those specified in the given map.
	Any association will only override resources that do not explicitly have a charset specified.
	The current associations will be lost.
	Any parameters of the given content types will be ignored.
	@param contentTypeCharsets The associations of charsets to base content types.
	@exception NullPointerException if a given content type and/or charset is <code>null</code>.
	*/
	public void setContentTypeCharsets(final Map<ContentType, Charset> contentTypeCharsets);

	/**Determines the URI of the collection resource of the given URI; either the given resource URI if the resource represents a collection, or the parent resource if not.
	@param resourceURI The URI of the resource for which the collection resource URI should be returned.
	@return The URI of the indicated resource's deepest collection resource, or <code>null</code> if the given URI designates a non-collection resource with no collection parent.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URI getCollectionURI(URI resourceURI) throws ResourceIOException;

	/**Determines the URI of the parent resource of the given URI.
	@param resourceURI The URI of the resource for which the parent resource URI should be returned.
	@return The URI of the indicated resource's parent resource, or <code>null</code> if the given URI designates a resource with no parent.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URI getParentResourceURI(final URI resourceURI) throws ResourceIOException;

	/**Gets an input stream to the contents of the resource specified by the given URI.
	@param resourceURI The URI of the resource to access.
	@return An input stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
	*/
	public InputStream getResourceInputStream(final URI resourceURI) throws ResourceIOException;

	/**Gets an output stream to the contents of the resource specified by the given URI.
	An error is generated if the resource does not exist.
	@param resourceURI The URI of the resource to access.
	@return An output stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the resource.
	*/
	public OutputStream getResourceOutputStream(final URI resourceURI) throws ResourceIOException;

	/**Retrieves a description of the resource with the given URI.
	@param resourceURI The URI of the resource the description of which should be retrieved.
	@return A description of the resource with the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URFResource getResourceDescription(final URI resourceURI) throws ResourceIOException;

	/**Determines if the resource at the given URI exists.
	@param resourceURI The URI of the resource to check.
	@return <code>true</code> if the resource exists, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean resourceExists(final URI resourceURI) throws ResourceIOException;

	/**Determines if the resource at a given URI is a collection.
	@param resourceURI The URI of the requested resource.
	@return <code>true</code> if the resource is a collection, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean isCollection(final URI resourceURI) throws ResourceIOException;

	/**Determines whether the resource represented by the given URI has children.
	@param resourceURI The URI of the resource.
	@return <code>true</code> if the specified resource has child resources.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean hasChildren(final URI resourceURI) throws ResourceIOException;

	/**Retrieves child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@return A list of sub-resource descriptions under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public List<URFResource> getChildResourceDescriptions(URI resourceURI) throws ResourceIOException;

	/**Retrieves child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param resourceFilter The filter that determines whether child resources should be included, or <code>null</code> if the child resources should not be filtered.
	@return A list of sub-resource descriptions under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public List<URFResource> getChildResourceDescriptions(URI resourceURI, final ResourceFilter resourceFilter) throws ResourceIOException;

	/**Retrieves child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or <code>-1</code> for an infinite depth.
	@return A list of sub-resource descriptions under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public List<URFResource> getChildResourceDescriptions(URI resourceURI, final int depth) throws ResourceIOException;

	/**Retrieves child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param resourceFilter The filter that determines whether child resources should be included, or <code>null</code> if the child resources should not be filtered.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or <code>-1</code> for an infinite depth.
	@return A list of sub-resource descriptions under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public List<URFResource> getChildResourceDescriptions(URI resourceURI, final ResourceFilter resourceFilter, final int depth) throws ResourceIOException;

	/**Creates all the parent resources necessary for a resource to exist at the given URI.
	If any parent resources already exist, they will not be replaced.
	@param resourceURI The reference URI of a resource which may not exist.
	@return A description of the most immediate parent resource created, or <code>null</code> if no parent resources were required to be created.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if a parent resource could not be created.
	*/
	public URFResource createParentResources(final URI resourceURI) throws ResourceIOException;
	
	/**Creates a new resource with a default description and returns an output stream for writing the contents of the resource.
	If a resource already exists at the given URI it will be replaced.
	The returned output stream should always be closed.
	If a resource with no contents is desired, {@link #createResource(URI, byte[])} with zero bytes is better suited for this task.
	@param resourceURI The reference URI to use to identify the resource.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public OutputStream createResource(final URI resourceURI) throws ResourceIOException;

	/**Creates a new resource with the given description and returns an output stream for writing the contents of the resource.
	If a resource already exists at the given URI it will be replaced.
	The returned output stream should always be closed.
	If a resource with no contents is desired, {@link #createResource(URI, URFResource, byte[])} with zero bytes is better suited for this task.
	It is unspecified whether the resource description will be updated before or after the resource contents are stored.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public OutputStream createResource(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException;

	/**Creates a new resource with a default description and contents.
	If a resource already exists at the given URI it will be replaced.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI and/or resource contents is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(final URI resourceURI, final byte[] resourceContents) throws ResourceIOException;

	/**Creates a new resource with the given description and contents.
	If a resource already exists at the given URI it will be replaced.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(final URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents) throws ResourceIOException;

	/**Creates a collection in the repository.
	@param collectionURI The URI of the collection to be created.
	@return A description of the collection that was created.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error creating the collection.
	*/
	public URFResource createCollection(final URI collectionURI) throws ResourceIOException;

	/**Creates a collection in the repository with the given description.
	@param collectionURI The URI of the collection to be created.
	@param collectionDescription A description of the collection; the resource URI is ignored.
	@return A description of the collection that was created.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error creating the collection.
	*/
	public URFResource createCollection(URI collectionURI, final URFResource collectionDescription) throws ResourceIOException;

	/**Deletes a resource.
	@param resourceURI The reference URI of the resource to delete.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if the resource could not be deleted.
	*/
	public void deleteResource(final URI resourceURI) throws ResourceIOException;

	/**Sets the properties of a resource based upon the given description.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource setResourceProperties(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException;

	/**Sets the properties of a given resource.
	Any existing properties with the same URIs as the given given property/value pairs will be removed.
	All other existing properties will be left unmodified. 
	@param resourceURI The reference URI of the resource.
	@param properties The properties to set.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or properties is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource setResourceProperties(final URI resourceURI, final URFProperty... properties) throws ResourceIOException;

	/**Removes properties from a given resource.
	Any existing properties with the same URIs as the given given property/value pairs will be removed.
	All other existing properties will be left unmodified. 
	@param resourceURI The reference URI of the resource.
	@param propertyURIs The properties to remove.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or property URIs is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource removeResourceProperties(final URI resourceURI, final URI... propertyURIs) throws ResourceIOException;

	/**Alters properties of a given resource.
	This implementation does not support removing specific properties by value.
	This implementation does not support adding properties; only setting properties.
	@param resourceURI The reference URI of the resource.
	@param resourceAlteration The specification of the alterations to be performed on the resource.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource alteration is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be altered.
	@exception UnsupportedOperationException if a property is requested to be removed by value.
	@exception UnsupportedOperationException if a property is requested to be added without the property URI first being removed (i.e. a property addition instead of a property setting).
	*/
	public URFResource alterResourceProperties(URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException;

	/**Creates an infinitely deep copy of a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be copied.
	@param destinationRepository The repository to which the resource should be copied, which may be this repository.
	@param destinationURI The URI to which the resource should be copied.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws ResourceIOException;

	/**Creates an infinitely deep copy of a resource to the specified URI in the specified repository, overwriting any resource at the destionation only if requested.
	@param resourceURI The URI of the resource to be copied.
	@param destinationRepository The repository to which the resource should be copied, which may be this repository.
	@param destinationURI The URI to which the resource should be copied.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite) throws ResourceIOException;

	/**Creates an infinitely deep copy of a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final URI destinationURI) throws ResourceIOException;

	/**Creates an infinitely deep copy of a resource to another URI in this repository, overwriting any resource at the destionation only if requested. 
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void copyResource(final URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException;

	/**Moves a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be moved.
	@param destinationRepository The repository to which the resource should be moved, which may be this repository.
	@param destinationURI The URI to which the resource should be moved.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.	
	@exception ResourceIOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws ResourceIOException;

	/**Moves a resource to the specified URI in the specified repository, overwriting any resource at the destionation only if requested.
	@param resourceURI The URI of the resource to be moved.
	@param destinationRepository The repository to which the resource should be moved, which may be this repository.
	@param destinationURI The URI to which the resource should be moved.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.	
	@exception ResourceIOException if there is an error moving the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite) throws ResourceIOException;

	/**Moves a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final URI destinationURI) throws ResourceIOException;

	/**Moves a resource to another URI in this repository, overwriting any resource at the destionation only if requested.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void moveResource(final URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException;

}
