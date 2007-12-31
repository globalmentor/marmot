package com.globalmentor.marmot.security;

import java.net.URI;
import java.util.*;

import static com.garretwilson.lang.EnumUtilities.*;
import static com.garretwilson.lang.Objects.*;

import static com.globalmentor.marmot.security.MarmotSecurity.*;

/**The predefined access levels as an enum for working with access levels as a group.
@author Garret Wilson
*/
public enum AccessLevelType
{

	INHERITED(INHERITED_ACCESS_LEVEL_CLASS_URI),

	PRIVATE(PRIVATE_ACCESS_LEVEL_CLASS_URI),

	STEALTH(STEALTH_ACCESS_LEVEL_CLASS_URI,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW,
			PermissionType.EXECUTE,
			PermissionType.READ),

	PREVIEW(PREVIEW_ACCESS_LEVEL_CLASS_URI,
			PermissionType.DISCOVER,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW),

	USE(USE_ACCESS_LEVEL_CLASS_URI,
			PermissionType.DISCOVER,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW,
			PermissionType.EXECUTE),

	RETRIEVE(RETRIEVE_ACCESS_LEVEL_CLASS_URI,
			PermissionType.DISCOVER,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW,
			PermissionType.EXECUTE,
			PermissionType.READ),
	
	EDIT(EDIT_ACCESS_LEVEL_CLASS_URI,
			PermissionType.DISCOVER,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW,
			PermissionType.EXECUTE,
			PermissionType.READ,
			PermissionType.MODIFY_PROPERTIES,
			PermissionType.RENAME,
			PermissionType.ADD,
			PermissionType.SUBTRACT),
	
	FULL(FULL_ACCESS_LEVEL_CLASS_URI,
			PermissionType.DISCOVER,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW,
			PermissionType.EXECUTE,
			PermissionType.READ,
			PermissionType.MODIFY_PROPERTIES,
			PermissionType.MODIFY_SECURITY,
			PermissionType.RENAME,
			PermissionType.ADD,
			PermissionType.SUBTRACT,
			PermissionType.DELETE),

	CUSTOM(CUSTOM_ACCESS_LEVEL_CLASS_URI);

	/**The URI indicating the URF type of this access level.*/
	private final URI typeURI;

		/**@return The URI indicating the URF type of this access level.*/
		public URI getTypeURI() {return typeURI;}

	/**The default permission types allowed for this access level.*/
	private final Set<PermissionType> defaultAllowedPermissionTypes;

		/**@return The default permission types allowed for this access level.*/
		public Set<PermissionType> getDefaultAllowedPermissionTypes() {return defaultAllowedPermissionTypes;}

	/**Type URI constructor.
	@param typeURI The URI indicating the URF type of this access level.
	@exception NullPointerException if the given type URI is <code>null</code>.
	*/
	private AccessLevelType(final URI typeURI, final PermissionType... permissionTypes)
	{
		this.typeURI=checkInstance(typeURI, "Type URI cannot be null.");
		defaultAllowedPermissionTypes=createEnumSet(PermissionType.class, permissionTypes);	//store the default permission types in our set
	}

	/**The lazily-created map of access levels keyed to type URIs.*/
	private static Map<URI, AccessLevelType> typeURIAccessTypeMap=null;

	/**Retrieves an access level from the type URI.
	@param accessLevelTypeURI The access level type URI.
	@return The access level with the given type URI.
	@exception NullPointerException if the given access level type URI is <code>null</code>.
	@exception IllegalArgumentException if the given access level type URI is not recognized.
	*/
	public static AccessLevelType getAccessLevelType(final URI accessLevelTypeURI)
	{
		if(typeURIAccessTypeMap==null)	//if we haven't created the map yet (race conditions here are benign---at the worst it will result in the map initially being created multiple times
		{
			final Map<URI, AccessLevelType> newTypeURIAccessTypeMap=new HashMap<URI, AccessLevelType>();	//create a new map
			for(final AccessLevelType accessLevelType:values())	//for each value
			{
				newTypeURIAccessTypeMap.put(accessLevelType.getTypeURI(), accessLevelType);	//store this access type in the map keyed to the type URI
			}
			typeURIAccessTypeMap=newTypeURIAccessTypeMap;	//update the static map with the one we created and initialized
		}
		final AccessLevelType accessLevelType=typeURIAccessTypeMap.get(checkInstance(accessLevelTypeURI, "Access level type URI cannot be null."));	//look up the access level type from the type URI
		if(accessLevelType==null)	//if we don't know the access level type from the type URI
		{
			throw new IllegalArgumentException("Unrecognized access level type URI: "+accessLevelTypeURI);
		}
		return accessLevelType;	//return the access level type
	}

}
