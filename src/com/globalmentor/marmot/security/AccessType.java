package com.globalmentor.marmot.security;

import java.net.URI;
import java.util.*;

import static com.garretwilson.lang.EnumUtilities.*;
import static com.garretwilson.rdf.RDFUtilities.*;

import static com.globalmentor.marmot.MarmotConstants.*;

/**The predefined access types as an enum for working with access types as a group.
This enum includes an access type, {@link #INHERITED}, which is not technically a predefined access type but instead indicates the absence of an access specification.
@author Garret Wilson
*/
public enum AccessType
{

	INHERITED(null),

	NONE(createReferenceURI(MARMOT_NAMESPACE_URI, NO_ACCESS_TYPE_NAME)),

	STEALTH(createReferenceURI(MARMOT_NAMESPACE_URI, STEALTH_ACCESS_TYPE_NAME),
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW,
			PermissionType.EXECUTE,
			PermissionType.READ),

	PREVIEW(createReferenceURI(MARMOT_NAMESPACE_URI, PREVIEW_ACCESS_TYPE_NAME),
			PermissionType.DISCOVER,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW),

	USE(createReferenceURI(MARMOT_NAMESPACE_URI, USE_ACCESS_TYPE_NAME),
			PermissionType.DISCOVER,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW,
			PermissionType.EXECUTE),

	RETRIEVE(createReferenceURI(MARMOT_NAMESPACE_URI, RETRIEVE_ACCESS_TYPE_NAME),
			PermissionType.DISCOVER,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW,
			PermissionType.EXECUTE,
			PermissionType.READ),
	
	EDIT(createReferenceURI(MARMOT_NAMESPACE_URI, EDIT_ACCESS_TYPE_NAME),
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
	
	FULL(createReferenceURI(MARMOT_NAMESPACE_URI, FULL_ACCESS_TYPE_NAME),
			PermissionType.DISCOVER,
			PermissionType.BROWSE,
			PermissionType.ANNOTATE,
			PermissionType.PREVIEW,
			PermissionType.EXECUTE,
			PermissionType.READ,
			PermissionType.MODIFY_PROPERTIES,
			PermissionType.MODIFY_ACCESS,
			PermissionType.RENAME,
			PermissionType.ADD,
			PermissionType.SUBTRACT,
			PermissionType.DELETE),
	
	CUSTOM(createReferenceURI(MARMOT_NAMESPACE_URI, CUSTOM_ACCESS_TYPE_NAME));

	/**The URI indicating the RDF type of this access type, or <code>null</code> if this is the {@link #INHERITED} access type.*/
	private final URI typeURI;

		/**@return The URI indicating the RDF type of this access type, or <code>null</code> if this is the {@link #INHERITED} access type.*/
		public URI getTypeURI() {return typeURI;}

	/**The default permission types allowed for this access type.*/
	private final Set<PermissionType> defaultAllowedPermissionTypes;

		/**@return The default permission types allowed for this access type.*/
		public Set<PermissionType> getDefaultAllowedPermissionTypes() {return defaultAllowedPermissionTypes;}

	/**Type URI constructor.
	@param typeURI The URI indicating the RDF type of this access type, or <code>null</code> if this is the {@link #INHERITED} access type.
	*/
	private AccessType(final URI typeURI, final PermissionType... permissionTypes)
	{
		this.typeURI=typeURI;
		defaultAllowedPermissionTypes=createEnumSet(PermissionType.class, permissionTypes);	//store the default permission types in our set
	}

	/**The lazily-created map of access types keyed to type URIs.*/
	private static Map<URI, AccessType> typeURIAccessTypeMap=null;

	/**Retrieves an access type from the type URI.
	@param accessTypeURI The permission type URI.
	@return The access type with the given type URI, or <code>null</code> if there is no access type with the given type URI.
	*/
	public static AccessType getAccessType(final URI accessTypeURI)
	{
		if(typeURIAccessTypeMap==null)	//if we haven't created the map yet (race conditions here are benign---at the worst it will result in the map initially being created multiple times
		{
			final Map<URI, AccessType> newTypeURIAccessTypeMap=new HashMap<URI, AccessType>();	//create a new map
			for(final AccessType accessType:values())	//for each value
			{
				newTypeURIAccessTypeMap.put(accessType.getTypeURI(), accessType);	//store this access type in the map keyed to the type URI
			}
			typeURIAccessTypeMap=newTypeURIAccessTypeMap;	//update the static map with the one we created and initialized
		}
		return typeURIAccessTypeMap.get(accessTypeURI);	//look up the access type from the type URI
	}
}
