package com.globalmentor.marmot.security;

import java.net.URI;
import java.util.*;


import static com.globalmentor.java.Objects.*;
import static com.globalmentor.marmot.security.MarmotSecurity.*;

/**The predefined permissions as an enum for working with permissions as a group.
@author Garret Wilson
*/
public enum PermissionType
{
	DISCOVER(DISCOVER_PERMISSION_CLASS_URI),

	BROWSE(BROWSE_PERMISSION_CLASS_URI),

	ANNOTATE(ANNOTATE_PERMISSION_CLASS_URI),
	
	PREVIEW(PREVIEW_PERMISSION_CLASS_URI),
	
	EXECUTE(EXECUTE_PERMISSION_CLASS_URI),	//TODO maybe rename to "USE" and rename AccessLevelType.USE to something else
	
	READ(READ_PERMISSION_CLASS_URI),

	MODIFY_PROPERTIES(MODIFY_PROPERTIES_PERMISSION_CLASS_URI),

	MODIFY_SECURITY(MODIFY_SECURITY_PERMISSION_CLASS_URI),

	RENAME(RENAME_PERMISSION_CLASS_URI),

	ADD(ADD_PERMISSION_CLASS_URI),

	SUBTRACT(SUBTRACT_PERMISSION_CLASS_URI),

	DELETE(DELETE_PERMISSION_CLASS_URI);

	/**The URI indicating the URF type of this permission type.*/
	private final URI typeURI;

		/**@return The URI indicating the URF type of this permission type.*/
		public URI getTypeURI() {return typeURI;}

	/**Type URI constructor.
	@param typeURI The URI indicating the URF type of this permission type.
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
		if(typeURIPermissionTypeMap==null)	//if we haven't created the map yet (race conditions here are benign---at the worst it will result in the map initially being created multiple times)
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
