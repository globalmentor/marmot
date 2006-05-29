package com.globalmentor.marmot.repository;

import java.io.*;
import java.net.URI;
import java.util.List;

import com.garretwilson.rdf.RDFResource;

/**A Marmot information store.
@author Garret Wilson
*/
public interface Repository
{

	/**@return The URI identifying the location of this repository.*/
	public URI getReferenceURI();
	
	/**Gets an input stream to the contents of the resource specified by the given URI.
	@param resourceURI The URI of the resource to access.
	@return An input stream to the resource represented by the given URI.
	@exception IOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
	*/
	public InputStream getResourceInputStream(final URI resourceURI) throws IOException;
	
	/**Gets an output stream to the contents of the resource specified by the given URI.
	@param resourceURI The URI of the resource to access.
	@return An output stream to the resource represented by the given URI.
	@exception IOException if there is an error accessing the resource.
	*/
	public OutputStream getResourceOutputStream(final URI resourceURI) throws IOException;
	
	/**Retrieves a description of the resource with the given URI.
	@param resourceURI The URI of the resource the description of which should be retrieved.
	@return A description of the resource with the given URI.
	@exception IOException if there is an error accessing the repository.
	*/
	public RDFResource getResourceDescription(final URI resourceURI) throws IOException;

	/**Determines if the resource at the given URI exists.
	@param resourceURI The URI of the resource to check.
	@return <code>true</code> if the resource exists, else <code>false</code>.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean resourceExists(final URI resourceURI) throws IOException;

	/**Determines if the resource at a given URI is a collection.
	This is a convenience method to quickly determine if a resource exists at the given URI
	and retrieving that resource would result in a resource of type <code>file:Folder</code>.
	@param resourceURI The URI of the requested resource.
	@return <code>true</code> if the resource is a collection, else <code>false</code>.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean isCollection(final URI resourceURI) throws IOException;

	/**Determines whether the resource represented by the given URI has children.
	@param resourceURI The URI of the resource.
	@return <code>true</code> if the specified resource has child resources.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean hasChildren(final URI resourceURI) throws IOException;

	/**Retrieves immediate child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@return A list of sub-resource descriptions directly under the given resource.
	@exception IOException if there is an error accessing the repository.
	*/
	public List<RDFResource> getChildResourceDescriptions(final URI resourceURI) throws IOException;

	/**Retrieves child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or <code>-1</code> for an infinite depth.
	@return A list of sub-resources descriptions directly under the given resource.
	@exception IOException if there is an error accessing the repository.
	*/
	public List<RDFResource> getChildResourceDescriptions(final URI resourceURI, final int depth) throws IOException;

	/**Creates an infinitely deep copy of a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be copied.
	@param destinationRepository The repository to which the resource should be copied, which may be this repository.
	@param destinationURI The URI to which the resource should be copied.
	@exception IOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws IOException;

	/**Creates an infinitely deep copy of a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@exception IOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final URI destinationURI) throws IOException;

	/**Deletes a resource.
	@param resourceURI The reference URI of the resource to delete.
	@exception IOException if the resource could not be deleted.
	*/
	public void deleteResource(final URI resourceURI) throws IOException;

	/**Moves a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be moved.
	@param destinationRepository The repository to which the resource should be moved, which may be this repository.
	@param destinationURI The URI to which the resource should be moved.
	@exception IOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws IOException;

	/**Moves a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@exception IOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final URI destinationURI) throws IOException;

}
