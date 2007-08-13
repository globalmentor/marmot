package com.globalmentor.marmot;

import java.io.File;
import java.net.*;
import java.util.Date;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import com.garretwilson.io.FileUtilities;
import static com.garretwilson.io.ContentTypeUtilities.*;
import com.garretwilson.net.URIUtilities;
import com.garretwilson.rdf.*;
import com.garretwilson.rdf.dublincore.DCUtilities;
import com.garretwilson.rdf.rdfs.RDFSUtilities;
import com.garretwilson.rdf.xmlschema.IntegerLiteral;
import com.garretwilson.rdf.xpackage.XPackageUtilities;
import com.garretwilson.text.W3CDateFormat;
import com.garretwilson.util.Debug;

import static com.garretwilson.lang.ObjectUtilities.*;
import static com.garretwilson.net.URIConstants.*;
import static com.garretwilson.net.URIUtilities.*;
import static com.garretwilson.rdf.RDFUtilities.*;

/**Constant values and utilities used by Marmot.
@author Garret Wilson
*/
public class Marmot
{

		//predefined users
	/**The principal wildcard character, '*'.*/
	public final static char WILDCARD_PRINCIPAL_CHAR='*';
	/**The predefined "all principals at localhost" <code>mailto</code> URI.*/
	public final static URI ALL_LOCALHOST_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), LOCALHOST_DOMAIN);
	/**The predefined "all principals" <code>mailto</code> URI.*/
	public final static URI ALL_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), String.valueOf(WILDCARD_PRINCIPAL_CHAR));

		//RDF constants

	/**The recommended prefix to the Marmot  namespace.*/
	public final static String MARMOT_NAMESPACE_PREFIX="marmot";

	/**The URI to the Marmot namespace.*/
	public final static URI MARMOT_NAMESPACE_URI=URI.create("http://globalmentor.com/namespaces/marmot#");

		//Marmot property names
	/**Specifies the access rules and permissions.*/
	public final static String ACCESS_PROPERTY_NAME="access";
	/**Specifies the level of access.*/
	public final static String ACCESS_LEVEL_PROPERTY_NAME="accessLevel";
	/**Specifies the list of access rules.*/
	public final static String ACCESS_RULES_PROPERTY_NAME="accessRules";
	/**Specifies a principal by URI. Used in various contexts.*/
	public final static String PRINCIPAL_PROPERTY_NAME="principal";
	/**Specifies the rules list of an access specification.*/
	public final static String RULES_PROPERTY_NAME="rules";
	/**Specifies resource security.*/
	public final static String SECURITY_PROPERTY_NAME="security";
	/**Specifies the selector to select one or more resources.*/
	public final static String SELECT_PROPERTY_NAME="select";

		//Marmot class names
	/**A rule specifying access permissions for zero or more principals.*/
	public final static String ACCESS_RULE_TYPE_NAME="AccessRule";

			//selector types
	/**A selector selecting a resource by a property value.*/
	public final static String PROPERTY_SELECTOR_TYPE_NAME="PropertySelector";
	/**A selector selecting the union of other selectors.*/
	public final static String UNION_SELECTOR_TYPE_NAME="UnionSelector";
	/**A selector selecting all resources.*/
	public final static String UNIVERSAL_SELECTOR_TYPE_NAME="UniversalSelector";
	/**A selector selecting a resource by its URI.*/
	public final static String URI_SELECTOR_TYPE_NAME="URISelector";
			//selector properties
	/**Specifies the URI of a URI selector.*/
	public final static String SELECT_URI_PROPERTY_NAME="selectURI";
	/**Specifies the property of a property selector.*/
	public final static String SELECT_PROPERTY_PROPERTY_NAME="selectProperty";
	/**Specifies the value of a property selector.*/
	public final static String SELECT_VALUE_PROPERTY_NAME="selectValue";
			
				//access levels
	/**Predefined access level type specifying inherited access.*/
	public final static String INHERITED_ACCESS_LEVEL_TYPE_NAME="InheritedAccessLevel";
	/**Predefined access level type allowing custom access.*/
	public final static String CUSTOM_ACCESS_LEVEL_TYPE_NAME="CustomAccessLevel";
	/**Predefined access level type allowing no access.*/
	public final static String PRIVATE_ACCESS_LEVEL_TYPE_NAME="PrivateAccessLevel";
	/**Predefined access level type allowing preview and execute permissions without discovery.*/
	public final static String STEALTH_ACCESS_LEVEL_TYPE_NAME="StealthAccessLevel";
	/**Predefined access level type allowing read and execute permissions of only a subset of the resource contents.*/
	public final static String PREVIEW_ACCESS_LEVEL_TYPE_NAME="PreviewAccessLevel";
	/**Predefined access level type preview and execute permissions but not read permissions.*/
	public final static String USE_ACCESS_LEVEL_TYPE_NAME="UseAccessLevel";
	/**Predefined access level type allowing discover and read permissions.*/
	public final static String RETRIEVE_ACCESS_LEVEL_TYPE_NAME="RetrieveAccessLevel";
	/**Predefined access level type allowing discover, read, and write permissions.*/
	public final static String EDIT_ACCESS_LEVEL_TYPE_NAME="EditAccessLevel";
	/**Predefined access level type allowing discover, read, write, and delete permissions.*/
	public final static String FULL_ACCESS_LEVEL_TYPE_NAME="FullAccessLevel";

			//permission operators
	/**Allows a permission.*/
	public final static String ALLOW_PROPERTY_NAME="allow";
	/**Denies a permission.*/
	public final static String DENY_PROPERTY_NAME="deny";
	
			//permissions
	/**The principal may detect that the resource exists, such as when the resource is listed in the contents of a parent collection. The local name of <code>marmot:DiscoverPermission</code>.*/
	public final static String DISCOVER_PERMISSION_TYPE_NAME="DiscoverPermission";
	/**The principal may view general information about the resource. The local name of <code>marmot:BrowsePermission</code>.*/
	public final static String BROWSE_PERMISSION_TYPE_NAME="BrowsePermission";
	/**The principal may add annotations to the resource. The local name of <code>marmot:AnnotatePermission</code>.*/
	public final static String ANNOTATE_PERMISSION_TYPE_NAME="AnnotatePermission";
	/**The principal may add annotations to the resource. The local name of <code>marmot:PreviewPermission</code>.*/
	public final static String PREVIEW_PERMISSION_TYPE_NAME="PreviewPermission";
	/**The principal may add annotations to the resource. The local name of <code>marmot:ExecutePermission</code>.*/
	public final static String EXECUTE_PERMISSION_TYPE_NAME="ExecutePermission";
	/**The principal may read the literal contents of the resource, including child listings for collections. The local name of <code>marmot:ReadPermission</code>.*/
	public final static String READ_PERMISSION_TYPE_NAME="ReadPermission";
	/**The principal may change, add, and remove resource properties. The local name of <code>marmot:ModifyPropertiesPermission</code>.*/
	public final static String MODIFY_PROPERTIES_PERMISSION_TYPE_NAME="ModifyPropertiesPermission";
	/**The principal may change the permissions describing the security of the resource, including how other principals access the resource. The local name of <code>marmot:ModifyAccessPermission</code>.*/
	public final static String MODIFY_SECURITY_PERMISSION_TYPE_NAME="ModifySecurityPermission";
	/**The principal may rename the resource. The local name of <code>marmot:RenamePermission</code>.*/
	public final static String RENAME_PERMISSION_TYPE_NAME="RenamePermission";
	/**The principal may add to the contents of the resource, including adding children in collections, but may not be able to remove contents. The local name of <code>marmot:AddPermission</code>.*/
	public final static String ADD_PERMISSION_TYPE_NAME="AddPermission";
	/**The principal may subtract from the contents of the resource, including removing children in collections, but may not be able to add contents. The local name of <code>marmot:SubtractPermission</code>.*/
	public final static String SUBTRACT_PERMISSION_TYPE_NAME="SubtractPermission";
	/**The principal may remove the resource, including collections. The local name of <code>marmot:DeletePermission</code>.*/
	public final static String DELETE_PERMISSION_TYPE_NAME="DeletePermission";

		//general resource property names
	/**The time when a resource was last accessed.*/
	public final static String ACCESSED_TIME_PROPERTY_NAME="accessedTime";
	/**The time when a resource was created.*/
	public final static String CREATED_TIME_PROPERTY_NAME="createdTime";
	/**The actual content type of a resource.*/
	public final static String CONTENT_PROPERTY_NAME="content";
	/**The MIME content type of a resource.*/
	public final static String CONTENT_TYPE_PROPERTY_NAME="contentType";
	/**The time when a resource was last modified.*/
	public final static String MODIFIED_TIME_PROPERTY_NAME="modifiedTime";
	/**The name of a resource, which may differ from that indicated by the URI, if any.*/
	public final static String NAME_PROPERTY_NAME="name";
	/**The size of a resource.*/
	public final static String SIZE_PROPERTY_NAME="size";
		//getneral property URIs
	/**The time when a resource was last modified..*/
	public final static URI MODIFIED_TIME_PROPERTY_URI=createReferenceURI(MARMOT_NAMESPACE_URI, MODIFIED_TIME_PROPERTY_NAME);
		//general type names
	/**A resource that is a collection of other resources.*/
	public final static String COLLECTION_CLASS_NAME="Collection";

	/**Determines the label of a resource.
	The resource label is determined in this order:
	<ol>
		<li>The literal value of the <code>rdfs:label</code> property, if present.</li>
		<li>The literal value of the <code>dc:title</code> property, if present.</li>
		<li>The decoded local name of the resource.</li>
	</ol>
	@param resource The resource for which a label should be returned.
	@return The determined label of the resource.
	*/	
	public static String getResourceLabel(final RDFResource resource)
	{
		RDFLiteral labelLiteral=RDFSUtilities.getLabel(resource);	//see if there is an rdfs:label property
		if(labelLiteral==null)	//if there is no rdfs:label defined
		{
			labelLiteral=DCUtilities.getTitle(resource);	//see if there is a dc:title property
		}
		return labelLiteral!=null ? labelLiteral.getLexicalForm() : URIUtilities.getName(resource.getReferenceURI());	//if there is a label, return it; otherwise, return the name of the resource
	}

	/**Determines if the given resource is a collection.
	@param resource The resource the type type of which to check.
	@return <code>true</code> if the given resource is a collection.
	*/
	public static boolean isCollection(final RDFResource resource)
	{
		return isType(resource, MARMOT_NAMESPACE_URI, COLLECTION_CLASS_NAME);
	}

	/**Determines the <code>marmot:name</code> specified by the given resource.
	@param resource The resource for which a name should be returned.
	@return The name of the resource, or <code>null</code> if the name could not be determined.
	*/ 
	public static String getName(final RDFResource resource)
	{
		final RDFLiteral nameLiteral=asInstance(resource.getPropertyValue(MARMOT_NAMESPACE_URI, NAME_PROPERTY_NAME), RDFLiteral.class);
		return nameLiteral!=null ? nameLiteral.getLexicalForm() : null;	//return the name, or null if we couldn't find the name
	}
	/**Sets the <code>marmot:name</code> property of the resource with a new property with the given value.
	@param resource The resource for which the name properties should be replaced.
	@param value The new name value.
	*/
	public static RDFPlainLiteral setName(final RDFResource resource, final String name) 
	{
		return resource.setProperty(MARMOT_NAMESPACE_URI, NAME_PROPERTY_NAME, name); //replace all name properties with a literal name value
	}

	/**Determines the file size specified by the given resource.
	@param resource The resource for which a size should be returned.
	@return The size of the resource, or <code>-1</code> if the size could not be
		determined.
	*/ 
	public static long getSize(final RDFResource resource)
	{
			//try to find the size property
		final IntegerLiteral sizeLiteral=asInstance(resource.getPropertyValue(MARMOT_NAMESPACE_URI, SIZE_PROPERTY_NAME), IntegerLiteral.class);
		return sizeLiteral!=null ? sizeLiteral.getValue().longValue() : -1;	//return the size, or -1 if we couldn't find the size
	}

	/**Replaces all <code>file:size</code> properties of the resource with a new
		property with the given value.
	@param resource The resource for which the size properties should be replaced.
	@param value A long integer size value.
	*/
	public static void setSize(final RDFResource resource, final long value)	//TODO should we use a special xsd:nonNegativeInteger? check and update the XPackage specification 
	{
		resource.setProperty(MARMOT_NAMESPACE_URI, SIZE_PROPERTY_NAME, new IntegerLiteral(value)); //replace all size properties with a literal size value
	}
	/**Returns the value of the first <code>file:createdTime</code> property parsed as a date, using the W3C full date style.
	@param resource The resource the property of which should be located.
	@param style The style of the date formatting.
	@return The value of the first <code>file:createdTime</code> property, or <code>null</code> if no such property exists or it does not contain a date.
	@see W3CDateFormat.Style#DATE_TIME
	*/
	public static Date getCreatedTime(final RDFResource resource)
	{
		return getDate(resource, MARMOT_NAMESPACE_URI, CREATED_TIME_PROPERTY_NAME);
	}
	/**Sets the <code>file:createdTime</code> property with the given value to the resource, using the W3C full date style.
	@param resource The resource to which the property should be set.
	@param date The property value to set.
	@return The added literal property value.
	@see W3CDateFormat.Style#DATE_TIME
	*/
	public static RDFLiteral setCreatedTime(final RDFResource resource, final Date date)
	{
		return setDate(resource, MARMOT_NAMESPACE_URI, CREATED_TIME_PROPERTY_NAME, date);
	}
	/**Returns the value of the first <code>file:modifiedTime</code> property parsed as a date, using the W3C full date style.
	@param resource The resource the property of which should be located.
	@param style The style of the date formatting.
	@return The value of the first <code>file:modifiedTime</code> property, or <code>null</code> if no such property exists or it does not contain a date.
	@see W3CDateFormat.Style#DATE_TIME
	*/
	public static Date getModifiedTime(final RDFResource resource)
	{
		return getDate(resource, MARMOT_NAMESPACE_URI, MODIFIED_TIME_PROPERTY_NAME);
	}
	/**Sets the <code>file:modifiedTime</code> property with the given value to the resource, using the W3C full date style.
	@param resource The resource to which the property should be set.
	@param date The property value to set.
	@return The added literal property value.
	@see W3CDateFormat.Style#DATE_TIME
	*/
	public static RDFLiteral setModifiedTime(final RDFResource resource, final Date date)
	{
		return setDate(resource, MARMOT_NAMESPACE_URI, MODIFIED_TIME_PROPERTY_NAME, date);
	}

	/**Returns the literal <code>marmot:content</code> of the resource.
	@param resource The resource for which the content type should be returned.
	@return This resource's content declaration, or <code>null</code> if the resource has no <code>marmot:content</code> property specified
	@exception ClassCastException if the value of the content property is not a {@link RDFLiteral}.
	*/
	public static RDFLiteral getContent(final RDFResource resource) throws ClassCastException
	{
		return (RDFLiteral)resource.getPropertyValue(MARMOT_NAMESPACE_URI, CONTENT_PROPERTY_NAME);	//return the marmot:content value
	}

	/**Sets this resource's content declaration.
	@param resource The resource for which the content properties should be replaced.
	@param content This resource's content declaration, or <code>null</code> if the resource should have no <code>marmot:content</code> property.
	*/
	public static void setContent(final RDFResource resource, final RDFLiteral content)
	{
		resource.setProperty(MARMOT_NAMESPACE_URI, CONTENT_PROPERTY_NAME, content);	//set the marmot:content property
	}

	/**Adds a <code>mime:contentType</code> property to the resource.
	@param resource The resource to which a property should be added.
	@param mediaType The object that specifies the content type.
	@return The new content type resource, a string literal.
	*/
	public static RDFLiteral addContentType(final RDFResource resource, final ContentType mediaType)
	{
		  //add the string value of the media type as the literal value of the xpackage:contentType property
		return resource.addProperty(MARMOT_NAMESPACE_URI, CONTENT_TYPE_PROPERTY_NAME, mediaType.toString());
	}

	/**Attempts to determine the media type of the given resource. The media type
		is determined in this order:
		<ol>
		  <li>The <code>mime:contentType</code> property, if present, is
				checked.</li>
			<li>The extension of the <code>xpackage:location</code> property
				<code>href</code>, if present, is checked.</li>
			<li>The extension, if any, of the resource URI is checked.</li>
		</ol>
	@param resource The resource of which the media type should be determined.
	@return The media type of the resource, or <code>null</code> if the media
		type could not be determined.
	@see XPackageUtilities#getLocationHRef
	*/
	public static ContentType getMediaType(final RDFResource resource)
	{
		ContentType mediaType=null; //start out assuming we won't find the media type
		final RDFObject mediaTypeObject=resource.getPropertyValue(MARMOT_NAMESPACE_URI, CONTENT_TYPE_PROPERTY_NAME); //return the contentType property
		if(mediaTypeObject!=null) //if there was a content type
		{
			try
			{
				final String mediaTypeString=((RDFLiteral)mediaTypeObject).getLexicalForm(); //get the media type string
				mediaType=new ContentType(mediaTypeString); //create a media type from the string
			}
			catch(ParseException parseException)	//if there is an error parsing the content type
			{
				throw new AssertionError(parseException);	//TODO fix better
			}
			catch(ClassCastException classCastException)
			{
				Debug.warn(classCastException); //if there was an error getting the media type, continue but create a warning
			}
		}
		if(mediaType==null) //if we couldn't find the media type from the contentType property
		{
			try
			{
//G***del Debug.trace("getting location href"); //G***del
				final String href=XPackageUtilities.getLocationHRef(resource);  //get the location of the resource
//G***del Debug.trace("location href: ", href); //G***del
				if(href!=null)  //if this resource has a location
				{
					mediaType=FileUtilities.getMediaType(new File(href)); //get the media type of the file
				}
			}
			catch(ClassCastException classCastException)
			{
				Debug.warn(classCastException); //if there was an error retrieving the location href, continue but create a warning
			}
		}
		if(mediaType==null) //try to find the media type from the URI
		{
			final URI referenceURI=resource.getReferenceURI();	//get the reference URI
			if(referenceURI!=null)	//if there is a reference URI
			{
				
				mediaType=URIUtilities.getMediaType(referenceURI);	//get the media type of the resource reference URI
			}
		}
		return mediaType; //return whatever media type we found, if any
	}

	/**Returns the declared content type of the resource.
	@param resource The resource for which the content type should be returned.
	@return This rule's content type declaration, or <code>null</code> if this rule has no <code>marmot:contentType</code> property specified
	@exception ClassCastException if the value of the content property is not a {@link RDFLiteral}.
	@exception IllegalArgumentException Thrown if the string is not a syntactically correct content type.
	*/
	public static ContentType getContentType(final RDFResource resource) throws ClassCastException
	{
		final RDFLiteral contentTypeLiteral=(RDFLiteral)resource.getPropertyValue(MARMOT_NAMESPACE_URI, CONTENT_TYPE_PROPERTY_NAME);	//get the marmot:contentType value, if any
		return contentTypeLiteral!=null ? createContentType(contentTypeLiteral.getLexicalForm()) : null;	//if there was a content type string, return the content type for it
	}

	/**Replaces all <code>marmot:contentType</code> properties of the resource with a new property with the given value.
	@param resource The resource for which the content type properties should be replaced.
	@param contentType The object that specifies the content type.
	*/
	public static void setContentType(final RDFResource resource, final ContentType contentType)
	{
			//replace all the the content type properties with a literal value of the string version of the content type
		resource.setProperty(MARMOT_NAMESPACE_URI, CONTENT_TYPE_PROPERTY_NAME, new RDFPlainLiteral(contentType.toString()));
	}

}
