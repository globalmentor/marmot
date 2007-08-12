package com.globalmentor.marmot.security;

import java.net.URI;
import java.util.*;

import static com.garretwilson.lang.ObjectUtilities.*;
import static com.garretwilson.rdf.RDFUtilities.*;

import static com.globalmentor.marmot.Marmot.*;

/**The predefined permissions as an enum for working with permissions as a group.
@author Garret Wilson
*/
public enum PermissionType
{
	DISCOVER(createReferenceURI(MARMOT_NAMESPACE_URI, DISCOVER_PERMISSION_TYPE_NAME)),

	BROWSE(createReferenceURI(MARMOT_NAMESPACE_URI, BROWSE_PERMISSION_TYPE_NAME)),

	ANNOTATE(createReferenceURI(MARMOT_NAMESPACE_URI, ANNOTATE_PERMISSION_TYPE_NAME)),
	
	PREVIEW(createReferenceURI(MARMOT_NAMESPACE_URI, PREVIEW_PERMISSION_TYPE_NAME)),
	
	EXECUTE(createReferenceURI(MARMOT_NAMESPACE_URI, EXECUTE_PERMISSION_TYPE_NAME)),
	
	READ(createReferenceURI(MARMOT_NAMESPACE_URI, READ_PERMISSION_TYPE_NAME)),

	MODIFY_PROPERTIES(createReferenceURI(MARMOT_NAMESPACE_URI, MODIFY_PROPERTIES_PERMISSION_TYPE_NAME)),

	MODIFY_SECURITY(createReferenceURI(MARMOT_NAMESPACE_URI, MODIFY_SECURITY_PERMISSION_TYPE_NAME)),

	RENAME(createReferenceURI(MARMOT_NAMESPACE_URI, RENAME_PERMISSION_TYPE_NAME)),

	ADD(createReferenceURI(MARMOT_NAMESPACE_URI, ADD_PERMISSION_TYPE_NAME)),

	SUBTRACT(createReferenceURI(MARMOT_NAMESPACE_URI, SUBTRACT_PERMISSION_TYPE_NAME)),

	DELETE(createReferenceURI(MARMOT_NAMESPACE_URI, DELETE_PERMISSION_TYPE_NAME));

	/**The URI indicating the RDF type of this permission type.*/
	private final URI typeURI;

		/**@return The URI indicating the RDF type of this permission type.*/
		public URI getTypeURI() {return typeURI;}

	/**Type URI constructor.
	@param typeURI The URI indicating the RDF type of this permission type.
	@exception NullPointerException if the given type URI is <code>null</code>.
	*/
	private PermissionType(final URI typeURI)
	{
		this.typeURI=checkInstance(typeURI, "Type URI cannot be null.");
	}

	/**The lazily-created map of permission types keyed to type URIs.*/
	private static Map<URI, PermissionType> typeURIPermissionTypeMap=null;

	/**Retrieves an permission type from the type URI.
	@param permissionTypeURI The permission type URI.
	@return The permission type with the given type URI.
	@exception NullPointerException if the given permission type URI is <code>null</code>.
	@exception IllegalArgumentException if the given permission type URI is not recognized.
	*/
	public static PermissionType getPermissionType(final URI permissionTypeURI)
	{
		if(typeURIPermissionTypeMap==null)	//if we haven't created the map yet (race conditions here are benign---at the worst it will result in the map initially being created multiple times
		{
			final Map<URI, PermissionType> newTypeURIPermissionTypeMap=new HashMap<URI, PermissionType>();	//create a new map
			for(final PermissionType PermissionType:values())	//for each value
			{
				newTypeURIPermissionTypeMap.put(PermissionType.getTypeURI(), PermissionType);	//store this permission type in the map keyed to the type URI
			}
			typeURIPermissionTypeMap=newTypeURIPermissionTypeMap;	//update the static map with the one we created and initialized
		}
		final PermissionType permissionType=typeURIPermissionTypeMap.get(checkInstance(permissionTypeURI, "Permission type URI cannot be null."));	//look up the permission type from the type URI
		if(permissionType==null)	//if we don't know the permission type from the type URI
		{
			throw new IllegalArgumentException("Unrecognized permission type URI: "+permissionTypeURI);
		}
		return permissionType;	//return the permission type
	}
}
