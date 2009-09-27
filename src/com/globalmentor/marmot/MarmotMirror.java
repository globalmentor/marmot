/*
 * Copyright © 1996-2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot;

import java.io.IOException;
import java.net.URI;

import static com.globalmentor.application.CommandLineArguments.*;
import static com.globalmentor.java.Characters.*;
import static com.globalmentor.java.Enums.*;
import static com.globalmentor.net.URIs.*;

import com.globalmentor.application.AbstractApplication;
import com.globalmentor.application.Application;
import com.globalmentor.log.Log;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.marmot.repository.RepositorySynchronizer.Resolution;
import com.globalmentor.marmot.repository.file.FileRepository;
import com.globalmentor.marmot.repository.file.NTFSFileRepository;
import com.globalmentor.marmot.repository.webdav.SubversionWebDAVRepository;
import com.globalmentor.marmot.repository.webdav.WebDAVRepository;
import com.globalmentor.net.URIs;
import com.globalmentor.net.http.*;
import com.globalmentor.urf.dcmi.DCMI;
import com.globalmentor.util.*;

/**Command-line Marmot synchronization utility.
@author Garret Wilson
*/
public class MarmotMirror extends AbstractApplication
{

	/**The application URI.*/
	public final static URI MARMOT_MIRROR_URI=URI.create("http://globalmentor.com/software/marmotmirror");

	/**The application title.*/
	public final static String TITLE="Marmot Mirror"+TRADE_MARK_SIGN_CHAR;

	/**The application copyright.*/
	public final static String COPYRIGHT="Copyright "+COPYRIGHT_SIGN+" 2006-2008 GlobalMentor, Inc. All Rights Reserved.";	//TODO i18n

	/**The version of the application.*/
	public final static String VERSION="1.1 build 2008-11-08";

	/**Application command-line parameters.*/
	public enum Parameter
	{
		/**The source repository.*/
		SOURCE_REPOSITORY,
		/**The source repository type.*/
		SOURCE_REPOSITORY_TYPE,
		/**The source resource.*/
		SOURCE_RESOURCE,
		/**A source resource to ignore.*/
		IGNORE_SOURCE_RESOURCE,
		/**The source repository username.*/
		SOURCE_USERNAME,
		/**The source repository username.*/
		SOURCE_PASSWORD,
		/**The destination repository.*/
		DESTINATION_REPOSITORY,
		/**The destination repository type.*/
		DESTINATION_REPOSITORY_TYPE,
		/**The destination resource.*/
		DESTINATION_RESOURCE,
		/**A destination resource to ignore.*/
		IGNORE_DESTINATION_RESOURCE,
		/**The destination repository username.*/
		DESTINATION_USERNAME,
		/**The destination repository username.*/
		DESTINATION_PASSWORD,
		/**A metadata property to ignore.*/
		IGNORE_PROPERTY,
		/**Whether the content modified property should be updated if requested even if content is not updated.*/
		FORCE_CONTENT_MODIFIED_PROPERTY,
		/**Test mode.*/
		TEST,
		/**Verbose output.*/
		VERBOSE,
		/**Quiet output.*/
		QUIET,
		/**Turns on HTTP debugging; requires debug mode on.*/
		DEBUG_HTTP,
		/**The resolution mode.*/
		RESOLUTION,
		/**The resource resolution mode.*/
		RESOURCE_RESOLUTION,
		/**The content resolution mode.*/
		CONTENT_RESOLUTION,
		/**The metadata resolution mode.*/
		METADATA_RESOLUTION
	}

	/**The types of repository available.*/
	public enum RepositoryType
	{
		/**A file system repository; {@link FileRepository}.*/
		FILE,
		/**A file system repository using NTFS streams for metadata.; {@link NTFSFileRepository}.*/
		NTFS,
		/**A WebDAV repository; {@link WebDAVRepository}.*/
		WEBDAV,
		/**A Subversion repository over WebDAV; {@link SubversionWebDAVRepository}.*/
		SVN;
	}
	
	/**Argument constructor.
	@param args The command line arguments.
	*/
	public MarmotMirror(final String[] args)
	{
		super(MARMOT_MIRROR_URI, args);	//construct the parent class
		DCMI.setTitle(this, TITLE); //set the application name
//TODO set version somehow
		DCMI.setRights(this, COPYRIGHT); //set the application copyright
	}

	/**The main application method.
	@return The application status.
	*/ 
	public int main()
	{
		final String[] args=getArgs();	//get the arguments
		final String sourceRepositoryString=getOption(args, getSerializationName(Parameter.SOURCE_REPOSITORY));	//get the source repository parameter
		final String destinationRepositoryString=getOption(args, getSerializationName(Parameter.DESTINATION_REPOSITORY));	//get the destination repository parameter
		if(sourceRepositoryString==null || destinationRepositoryString==null)	//if the source and/or destination repository parameter is missing
		{
			System.out.println(TITLE);
			System.out.println(VERSION);
			System.out.println(COPYRIGHT);
			System.out.println("Usage: MarmotMirror --source-repository <file|URI> [--source-repositorytype <repository type>] [--source-username <username>] [--source-password <password>] [--source-resource <file|URI>] " +
					"--destination-repository <URI> [--source-repository-type <repository type>] [--destination-username <username>] [--destination-password <password>] [--destination-resource <file|URI>] " +
					"[--ignore-source-resource <file|URI>]* [--ignore-destination-resource <file|URI>]* [--ignore-property <URI>]* [--force-content-modified-property]" +
					"[--resolution] [--resource-resolution] [--content-resolution] [--metadata-resolution] [--test] [--verbose] [--debug-http]");
			System.out.println("");
			System.out.println("Synchronization occurs on three levels: individual resources (i.e. orphans), metadata, and content, each of which can have a different resolution specified.");
			System.out.println("");
			System.out.println("Available repository types:");
			System.out.println("  "+getSerializationName(RepositoryType.FILE)+": A file system repository.");
			System.out.println("  "+getSerializationName(RepositoryType.NTFS)+": A file system repository using NTFS streams for metadata.");
			System.out.println("  "+getSerializationName(RepositoryType.WEBDAV)+": A WebDAV repository.");
			System.out.println("  "+getSerializationName(RepositoryType.SVN)+": A Subversion repository over WebDAV.");
			System.out.println("");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.SOURCE_REPOSITORY)+": The source repository.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.SOURCE_REPOSITORY_TYPE)+": The type of source repository.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.SOURCE_RESOURCE)+": The source resource to synchronize; defaults to the source repository root.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.IGNORE_SOURCE_RESOURCE)+": A source resource to ignore.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.SOURCE_USERNAME)+": The source repository username, if appropriate.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.SOURCE_PASSWORD)+": The source repository password, if appropriate.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.DESTINATION_REPOSITORY)+": The destination repository.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.DESTINATION_REPOSITORY_TYPE)+": The type of destination repository.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.DESTINATION_RESOURCE)+": The destination resource to synchronize; defaults to the destination repository root.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.IGNORE_DESTINATION_RESOURCE)+": A destination resource to ignore.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.DESTINATION_USERNAME)+": The destination repository username, if appropriate.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.DESTINATION_PASSWORD)+": The destination repository password, if appropriate.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.RESOLUTION)+": The default resolution for encountered conditions; defaults to \"backup\".");
			System.out.println("  "+getSerializationName(RepositorySynchronizer.Resolution.BACKUP)+": The source will overwrite the destination; the destination is intended to be a mirror of the source.");
			System.out.println("  "+getSerializationName(RepositorySynchronizer.Resolution.RESTORE)+": The destination will overwrite the source; the source is intended to be a mirror of the destination.");
			System.out.println("  "+getSerializationName(RepositorySynchronizer.Resolution.PRODUCE)+": The source will overwrite the destination, but missing source information will not cause destination information to be removed.");
			System.out.println("  "+getSerializationName(RepositorySynchronizer.Resolution.CONSUME)+": The destination will overwrite the source; but missing destination information will not cause source information to be removed.");
			System.out.println("  "+getSerializationName(RepositorySynchronizer.Resolution.SYNCHRONIZE)+": Newer information will overwrite older information; the source and destination are intended to be updated with the latest changes from each, although for orphan resources this will be consdered the same as "+getSerializationName(RepositorySynchronizer.Resolution.BACKUP)+".");
			System.out.println("  "+getSerializationName(RepositorySynchronizer.Resolution.IGNORE)+": No action will occur.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.RESOURCE_RESOLUTION)+": How an orphan resource situation (i.e. one resource exists and the other does not) will be resolved.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.CONTENT_RESOLUTION)+": How a content discrepancy will be resolved.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.METADATA_RESOLUTION)+": How a metadata discrepancy will be resolved.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.IGNORE_PROPERTY)+": A metadata property to ignore.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.FORCE_CONTENT_MODIFIED_PROPERTY)+": Whether the content modified property should be updated if requested even if content is not updated.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.TEST)+": If specified, no changed will be made.");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.VERBOSE)+": If specified, debug will be set to a minimum report level of "+Log.Level.DEBUG+"; otherwise, the report level will be "+Log.Level.INFO+".");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.QUIET)+": If specified, debug will be set to a minimum report level of "+Log.Level.WARN+"; otherwise, the report level will be "+Log.Level.INFO+".");
			System.out.println(LONG_SWITCH_DELIMITER+getSerializationName(Parameter.DEBUG_HTTP)+": Whether HTTP communication is logged; requires debug to be turned on.");
			return 0;
		}
		configureLog(args);	//configure logging
		final URI sourceRepositoryURI=guessAbsoluteURI(sourceRepositoryString);	//get the source repository URI
		final String sourceResourceString=getOption(args, getSerializationName(Parameter.SOURCE_RESOURCE));	//get the source resource parameter
		final URI sourceResourceURI=sourceResourceString!=null ? guessAbsoluteURI(sourceResourceString) : sourceRepositoryURI;	//if the source resource is not specified, use the repository URI
		final URI destinationRepositoryURI=guessAbsoluteURI(destinationRepositoryString);	//get the destination repository URI
		final String destinationResourceString=getOption(args, getSerializationName(Parameter.DESTINATION_RESOURCE));	//get the destination resource parameter
		final URI destinationResourceURI=destinationResourceString!=null ? guessAbsoluteURI(destinationResourceString) : destinationRepositoryURI;	//if the destination resource is not specified, use the repository URI
		HTTPClient.getInstance().setLogged(hasFlag(args, getSerializationName(Parameter.DEBUG_HTTP)));	//tell the HTTP client to log its data TODO generalize
		final String sourceRepositoryTypeString=getOption(args, getSerializationName(Parameter.SOURCE_REPOSITORY_TYPE));
		final Repository sourceRepository=createRepository(sourceRepositoryTypeString!=null ? getSerializedEnum(RepositoryType.class, sourceRepositoryTypeString) : null, sourceRepositoryURI, getOption(args, getSerializationName(Parameter.SOURCE_USERNAME)), getOption(args, getSerializationName(Parameter.SOURCE_PASSWORD)));	//create the correct type of repository for the source
		final String destinationRepositoryTypeString=getOption(args, getSerializationName(Parameter.DESTINATION_REPOSITORY_TYPE));
		final Repository destinationRepository=createRepository(destinationRepositoryTypeString!=null ? getSerializedEnum(RepositoryType.class, destinationRepositoryTypeString) : null, destinationRepositoryURI, getOption(args, getSerializationName(Parameter.DESTINATION_USERNAME)), getOption(args, getSerializationName(Parameter.DESTINATION_PASSWORD)));	//create the correct type of repository for the destination
		try
		{
			final RepositorySynchronizer repositorySynchronizer=new RepositorySynchronizer();	//create a new synchronizer
			final String resolutionString=getOption(args, getSerializationName(Parameter.RESOLUTION));	//set the resolutions if provided
			if(resolutionString!=null)
			{
				repositorySynchronizer.setResolution(getSerializedEnum(Resolution.class, resolutionString.toUpperCase()));
			}
			final String resourceResolutionString=getOption(args, getSerializationName(Parameter.RESOURCE_RESOLUTION));
			if(resourceResolutionString!=null)
			{
				repositorySynchronizer.setResourceResolution(getSerializedEnum(Resolution.class, resourceResolutionString.toUpperCase()));
			}
			final String contentResolutionString=getOption(args, getSerializationName(Parameter.CONTENT_RESOLUTION));
			if(contentResolutionString!=null)
			{
				repositorySynchronizer.setContentResolution(getSerializedEnum(Resolution.class, contentResolutionString.toUpperCase()));
			}
			final String metadataResolutionString=getOption(args, getSerializationName(Parameter.METADATA_RESOLUTION));
			if(metadataResolutionString!=null)
			{
				repositorySynchronizer.setMetadataResolution(getSerializedEnum(Resolution.class, metadataResolutionString.toUpperCase()));
			}
			for(final String ignoreSourceResourceURIString:getOptions(args, getSerializationName(Parameter.IGNORE_SOURCE_RESOURCE)))	//look at all the source resources to ignore
			{
				repositorySynchronizer.addIgnoreSourceResourceURI(guessAbsoluteURI(ignoreSourceResourceURIString));	//create a URI from the parameter and add this to the source resources to ignore
			}
			for(final String ignoreDestinationResourceURIString:getOptions(args, getSerializationName(Parameter.IGNORE_DESTINATION_RESOURCE)))	//look at all the destination resources to ignore
			{
				repositorySynchronizer.addIgnoreDestinationResourceURI(guessAbsoluteURI(ignoreDestinationResourceURIString));	//create a URI from the parameter and add this to the destination resources to ignore
			}
			for(final String ignorePropertyURIString:getOptions(args, getSerializationName(Parameter.IGNORE_PROPERTY)))	//look at all the properties to ignore
			{
				repositorySynchronizer.addIgnorePropertyURI(URI.create(ignorePropertyURIString));	//create a URI from the parameter and add this to the properties to ignore
			}
			repositorySynchronizer.setForceContentModifiedProperty(hasFlag(args, getSerializationName(Parameter.FORCE_CONTENT_MODIFIED_PROPERTY)));	//specify whether the content modified property should be forced
			repositorySynchronizer.setTest(hasFlag(args, getSerializationName(Parameter.TEST)));	//specify whether this is a test run
			repositorySynchronizer.synchronize(sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//synchronize the resources
		}
		catch(final IOException ioException)	//if there is an error
		{
			displayError(ioException);
			return 1;
		}
		return 0;	//return no error
	}

	/**The main routine that starts the application.
	@param args The command line arguments.
	*/
	public static void main(final String[] args)
	{
		run(new MarmotMirror(args), args);	//start a new application
	}

	/**Create the correct type of respository for the given repository URI.
	<p>If no repository type is specified, a type is guessed.
	Tthe following repository schemes are recognized:</p>
	<dl>
		<dt>{@value HTTP#HTTP_URI_SCHEME}</dt> <dd>{@link RepositoryType#WEBDAV}</dd>
		<dt>{@value HTTP#HTTPS_SCHEME}</dt> <dd>{@link RepositoryType#WEBDAV}</dd>
		<dt>{@value URIs#FILE_SCHEME}</dt> <dd>{@link RepositoryType#FILE}</dd>
	</dl>
	@param repositoryType The type of repository to create, or <code>null</code> if the repository type should be guessed.
	@param repositoryURI The URI of the repository to create.
	@param username The username of the repository, or <code>null</code> if no username is appropriate.
	@param password The password of the repository, or <code>null</code> if no password is appropriate.
	@return A repository for the given URI.
	@exception IllegalArgumentException if the type of the given repository URI cannot be determined.
	*/
	protected static Repository createRepository(RepositoryType repositoryType, final URI repositoryURI, final String username, final String password)
	{
		if(repositoryType==null)	//if no repository type was designated
		{
			if(HTTP.isHTTPURI(repositoryURI))	//if this is an HTTP repository URI
			{
				repositoryType=RepositoryType.WEBDAV;	//assume a WebDAV repository
			}
			else if(FILE_SCHEME.equals(repositoryURI.getScheme()))	//if this is a file repository URI
			{
				repositoryType=RepositoryType.FILE;	//assume a file repository
			}
			else	//if we don't recognize the repository type
			{
				throw new IllegalArgumentException("Unrecognized repository type: "+repositoryURI);
			}
		}
		switch(repositoryType)	//create the correct type of repository
		{
			case FILE: 
				return new FileRepository(repositoryURI);	//create a file-based repository
			case NTFS:
				return new NTFSFileRepository(repositoryURI);	//create a file-based repository
			case WEBDAV:
			{
				final WebDAVRepository webDavRepository=new WebDAVRepository(repositoryURI);	//create a WebDAV-based repository
				if(username!=null)	//set the username if there is one
				{
					webDavRepository.setUsername(username);
				}
				if(password!=null)	//set the password if there is one
				{
					webDavRepository.setPassword(password.toCharArray());
				}
				return webDavRepository;
			}
			case SVN:
			{
				final SubversionWebDAVRepository webDavRepository=new SubversionWebDAVRepository(repositoryURI);	//create a Subversion-based repository
				if(username!=null)	//set the username if there is one
				{
					webDavRepository.setUsername(username);
				}
				if(password!=null)	//set the password if there is one
				{
					webDavRepository.setPassword(password.toCharArray());
				}
				return webDavRepository;
			}
			default:
				throw new AssertionError("Unrecognized repository type: "+repositoryType);
		}
	}
}