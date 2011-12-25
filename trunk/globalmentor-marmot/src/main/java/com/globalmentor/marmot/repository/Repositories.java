/*
 * Copyright © 2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import static com.globalmentor.io.Charsets.*;
import static com.globalmentor.java.Characters.*;
import static com.globalmentor.java.Conditions.*;
import static com.globalmentor.java.Strings.*;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.urf.TURF.*;

import java.io.*;
import java.net.URI;

import com.globalmentor.java.Strings;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.net.URIs;
import com.globalmentor.urf.*;

/**
 * Constants and utilities for working with repositories.
 * 
 * <p>
 * To assist in implementing repositories that don't support custom namespaces, this class allows encoding of URF properties by using some other namespace
 * namespace with a local name encoded version of the URF property URI, using {@value #PROPERTY_NAME_URI_ESCAPE_CHAR} as the escape character. The standard URI
 * escape character, {@value URIs#ESCAPE_CHAR}, is not a valid name character, so {@value #PROPERTY_NAME_URI_ESCAPE_CHAR}, which conveniently is not a valid URI
 * character, is used instead.
 * </p>
 * 
 * @author Garret Wilson
 */
public class Repositories
{

	/** The character used to escape URIs to encode them as property names in another namespace. */
	public final static char PROPERTY_NAME_URI_ESCAPE_CHAR = MIDDLE_DOT_CHAR;

	/**
	 * Determines the a property name to represent an URF property by encoded the URF property URI to be a simple local name.
	 * <p>
	 * The standard URI escape character, {@value URIs#ESCAPE_CHAR}, may not be a valid name character for e.g. Subversion using WebDAV, so
	 * {@value #PROPERTY_NAME_URI_ESCAPE_CHAR}, which conveniently is not a valid URI character, is used instead.
	 * </p>
	 * <p>
	 * This method is part of a set for encoding/decoding entire property URIs as a single property local name for those repository types that don't allow
	 * specific namespaces to be set.
	 * </p>
	 * @param urfPropertyURI The URI of the URF property to represent.
	 * @return A property name to use in representing an URF property with the given URF property URI.
	 * @see #PROPERTY_NAME_URI_ESCAPE_CHAR
	 * @see #decodePropertyURILocalName(String)
	 */
	public static String encodePropertyURILocalName(final URI urfPropertyURI)
	{
		return encode(urfPropertyURI, PROPERTY_NAME_URI_ESCAPE_CHAR);
	}

	/**
	 * Determines the URF property to represent the given property local name, which is assumed to have a full property URI encoded in it.
	 * <p>
	 * The standard URI escape character, {@value URIs#ESCAPE_CHAR}, is not a valid name character, so {@value #PROPERTY_NAME_URI_ESCAPE_CHAR}, which conveniently
	 * is not a valid URI character, is used instead.
	 * </p>
	 * <p>
	 * This method is part of a set for encoding/decoding entire property URIs as a single property local name for those repository types that don't allow
	 * specific namespaces to be set.
	 * </p>
	 * @param webdavPropertyName The name of the WebDAV property.
	 * @return The URI of the URF property to represent the given property local name.
	 * @throws IllegalArgumentException if the given local name has no valid absolute URF property URI encoded in it.
	 * @see #PROPERTY_NAME_URI_ESCAPE_CHAR
	 * @see #encodePropertyURILocalName(URI)
	 */
	public static URI decodePropertyURILocalName(final String propertyLocalName)
	{
		final String urfPRopertyURI = decode(propertyLocalName, PROPERTY_NAME_URI_ESCAPE_CHAR); //the URF property URI may be encoded as the local name of the custom property
		return checkAbsolute(URI.create(urfPRopertyURI)); //create an URF property URI from the decoded local name and make sure it is absolute
	}

	/**
	 * Creates a single text value for to represent the given URF property and value(s). At least one property must be given.
	 * <p>
	 * This method is part of a pair of methods to allow multiple typed values encoded in a single string for repositories that don't natively allow multiple or
	 * typed properties.
	 * </p>
	 * @param resourceURI The URI of the resource.
	 * @param properties The URF properties to represent as values encoded in a single string
	 * @return A text value representing the given URF properties.
	 * @throws NullPointerException if the given properties is <code>null</code>.
	 * @throws IllegalArgumentException if no properties are given.
	 * @throws IllegalArgumentException if all of the properties do not have the same property URI.
	 * @throws IllegalArgumentException if there is an error creating the text value.
	 * @see #decodePropertiesTextValue(URF, URFResource, URI, String, URFIO)
	 */
	public static String encodePropertiesTextValue(final URI resourceURI, final URFIO<URFResource> descriptionIO, final URFProperty... properties)
			throws IOException
	{
		if(properties.length == 0) //if no properties are given
		{
			throw new IllegalArgumentException("At least one URF property must be provided to create a WebDAV property.");
		}
		final URI propertyURI = properties[0].getPropertyURI(); //get the URI of the URF property
		//TODO why don't we check to see if there is only one text property, and simply return the text value of that property? was this an oversight in an earlier implementation? we would need to still encode it if the real string starts with the TURF signature
		final URFResource propertyDescription = new DefaultURFResource(resourceURI); //create a new resource description just for this property
		for(final URFProperty property : properties) //for each URF property
		{
			if(!propertyURI.equals(property.getPropertyURI())) //if this URF property has a different URI
			{
				throw new IllegalArgumentException("All URF properties expected to have property URI " + propertyURI + "; found " + property.getPropertyURI() + ".");
			}
			propertyDescription.addProperty(property); //add this property to the resource
		}
		return Strings.write(resourceURI, propertyDescription, descriptionIO, UTF_8_CHARSET); //write the description to a string, using the resource URI as the base URI
	}

	/**
	 * Adds the identified property of the given resource from the given text, which may indicate a set of properties encoded in URF.
	 * <p>
	 * If the property text value begins with {@link TURF#SIGNATURE}, it is assumed to be stored in TURF; the identified property is removed and replaced with all
	 * given properties stored in the parsed TURF description. Otherwise, the text value is assumed to be just another text value and is added to the resource
	 * using the identified property.
	 * </p>
	 * <p>
	 * This method is part of a pair of methods to allow multiple typed values encoded in a single string for repositories that don't natively allow multiple or
	 * typed properties.
	 * </p>
	 * @param urf The URF instance to use in creating new resources.
	 * @param resource The resource the properties of which to add.
	 * @param propertyURI The URI of the property to add.
	 * @param propertyTextValue The text value to add; this value may represent several properties encoded as TURF.
	 * @param descriptionIO The I/O implementation for reading resources.
	 * @throws NullPointerException if the given URF data model, resource, property URI, property text value, and/or description I/O is <code>null</code>.
	 * @throws IllegalArgumentException if the given property text value purports to be TURF but has serialization errors.
	 * @see #encodePropertiesTextValue(URI, URFProperty...)
	 */
	public static void decodePropertiesTextValue(final URF urf, final URFResource resource, final URI propertyURI, final String propertyTextValue,
			final URFIO<URFResource> descriptionIO)
	{
		if(propertyTextValue.startsWith(SIGNATURE)) //if this property value is stored in TURF
		{
			try
			{
				//read a description of the resource from the property, recognizing the resource serialized with URI "" as indicating the given resource
				final URFResource propertyDescription = descriptionIO.read(urf, new ByteArrayInputStream(propertyTextValue.getBytes(UTF_8_CHARSET)), resource.getURI());
				resource.removePropertyValues(propertyURI); //if we were successful (that is, the property text value had no errors), remove any values already present for this value 
				for(final URFProperty property : propertyDescription.getProperties(propertyURI)) //for each read property that we expect in the description
				{
					resource.addProperty(property); //add this property to the given description
				}
			}
			catch(final IOException ioException) //if we had any problem interpreting the text value as TURF
			{
				throw new IllegalArgumentException("Invalid URF property value.", ioException);
			}
		}
		else
		//if this is a normal string property
		{
			resource.addPropertyValue(propertyURI, propertyTextValue); //add the string value to the resource
		}
	}

	public static void print(final Repository repository, final PrintStream printStream) throws ResourceIOException
	{
		print(repository, repository.getRootURI(), printStream);
	}

	public static void print(final Repository repository, final URI resourceURI, final PrintStream printStream) throws ResourceIOException
	{
		print(repository, repository.getResourceDescription(resourceURI), printStream);
	}

	public static void print(final Repository repository, final URFResource resource, final PrintStream printStream) throws ResourceIOException
	{
		final URFTURFGenerator urfTURFGenerator = new URFTURFGenerator(repository.getRootURI());
		urfTURFGenerator.setFormatted(false);
		urfTURFGenerator.setPropertyNamespacePrefixesForced(true);
		print(repository, resource, 0, printStream, urfTURFGenerator);
	}

	public static void print(final Repository repository, final URFResource resource, final int level, final PrintStream printStream,
			final URFTURFGenerator urfTURFGenerator) throws ResourceIOException
	{
		/*TODO del
				printStream.print(createString('\t', level)); //indent
				printStream.println(DefaultResource.toString(resource)); //<<uri>>
				printStream.print(createString('\t', level)); //indent
				printStream.print('[');
		*/
		try
		{
			printStream.print(createString('\t', level)); //indent
			printStream.println(urfTURFGenerator.generateResources(new StringBuilder(), false, resource).toString()); //TODO testing
		}
		catch(final IOException ioException)
		{
			throw unexpected(ioException);
		}
		//		printStream.println(']');
		for(final URFResource childResourceDescription : repository.getChildResourceDescriptions(resource.getURI()))
		{
			print(repository, childResourceDescription, level + 1, printStream, urfTURFGenerator);

		}
	}

}
