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
import java.util.List;

import static com.globalmentor.java.Characters.*;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.util.CommandLineArguments.*;

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

/**Command-line marmot synchronization utility.
@author Garret Wilson
*/
public class MarmotMirror extends Application
{

	/**The application URI.*/
	public final static URI MARMOT_MIRROR_URI=URI.create("urn:x-globalmentor:software:/marmotmirror");

	/**The application title.*/
	public final static String TITLE="Marmot Mirror"+TRADE_MARK_SIGN_CHAR;

	/**The application copyright.*/
	public final static String COPYRIGHT="Copyright "+COPYRIGHT_SIGN+" 2006-2008 GlobalMentor, Inc. All Rights Reserved.";	//TODO i18n

	/**The version of the application.*/
	public final static String VERSION="Alpha Version 0.4 build 2008-10-22";

	/**Application command-line parameters.*/
	public enum Parameter
	{
		/**The source repository.*/
		sourcerepository,
		/**The source repository type.*/
		sourcerepositorytype,
		/**The source resource.*/
		sourceresource,
		/**The source repository username.*/
		sourceusername,
		/**The source repository username.*/
		sourcepassword,
		/**The destination repository.*/
		destinationrepository,
		/**The destination repository type.*/
		destinationrepositorytype,
		/**The destination resource.*/
		destinationresource,
		/**The destination repository username.*/
		destinationusername,
		/**The destination repository username.*/
		destinationpassword,
		/**A metadata property to ignore.*/
		ignoreproperty,
		/**Test mode.*/
		test,
		/**Verbose output.*/
		verbose,
		/**Turns on HTTP debugging; requires debug mode on.*/
		debughttp,
		/**The resolution mode.*/
		resolution,
		/**The resource resolution mode.*/
		resourceresolution,
		/**The content resolution mode.*/
		contentresolution,
		/**The metadata resolution mode.*/
		metadataresolution
	}

	/**The types of repository available.*/
	public enum RepositoryType
	{
		/**A file system repository; {@link FileRepository}.*/
		file,
		/**A file system repository using NTFS streams for metadata.; {@link NTFSFileRepository}.*/
		ntfs,
		/**A WebDAV repository; {@link WebDAVRepository}.*/
		webdav,
		/**A Subversion repository over WebDAV; {@link SubversionWebDAVRepository}.*/
		svn;
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
		final String sourceRepositoryString=getParameter(args, Parameter.sourcerepository.name());	//get the source repository parameter
		final String destinationRepositoryString=getParameter(args, Parameter.destinationrepository.name());	//get the destination repository parameter
		if(sourceRepositoryString==null || destinationRepositoryString==null)	//if the source and/or destination repository parameter is missing
		{
			System.out.println(TITLE);
			System.out.println(VERSION);
			System.out.println(COPYRIGHT);
			System.out.println("Usage: MarmotMirror -sourcerepository <file|URI> [-sourcerepositorytype <repository type>] [-sourceusername <username>] [-sourcepassword <password>] [-sourceresource <file|URI>] " +
					"-destinationrepository <URI> [-sourcerepositorytype <repository type>] [-destinationusername <username>] [-destinationpassword <password>] [-destinationresource <file|URI>] " +
					"[-ignoreproperty <URI>]* " +
					"[-resolution] [-resourceresolution] [-contentresolution] [-metadataresolution] [-test] [-verbose] [-debughttp]");
			System.out.println("");
			System.out.println("Synchronization occurs on three levels: individual resources (i.e. orphans), metadata, and content, each of which can have a different resolution specified.");
			System.out.println("");
			System.out.println("Available repository types:");
			System.out.println("  "+RepositoryType.file.name().toLowerCase()+": A file system repository.");
			System.out.println("  "+RepositoryType.ntfs.name().toLowerCase()+": A file system repository using NTFS streams for metadata.");
			System.out.println("  "+RepositoryType.webdav.name().toLowerCase()+": A WebDAV repository.");
			System.out.println("  "+RepositoryType.svn.name().toLowerCase()+": A Subversion repository over WebDAV..");
			System.out.println("");
			System.out.println("-"+Parameter.sourcerepository+": The source repository.");
			System.out.println("-"+Parameter.sourcerepositorytype+": The type of source repository.");
			System.out.println("-"+Parameter.sourceresource+": The source resource to synchronize; defaults to the source repository root.");
			System.out.println("-"+Parameter.sourceusername+": The source repository username, if appropriate.");
			System.out.println("-"+Parameter.sourcepassword+": The source repository password, if appropriate.");
			System.out.println("-"+Parameter.destinationrepository+": The destination repository.");
			System.out.println("-"+Parameter.destinationrepositorytype+": The type of destination repository.");
			System.out.println("-"+Parameter.destinationresource+": The destination resource to synchronize; defaults to the destination repository root.");
			System.out.println("-"+Parameter.destinationusername+": The destination repository username, if appropriate.");
			System.out.println("-"+Parameter.destinationpassword+": The destination repository password, if appropriate.");
			System.out.println("-"+Parameter.resolution+": The default resolution for encountered conditions; defaults to \"backup\".");
			System.out.println("  "+RepositorySynchronizer.Resolution.BACKUP.name().toLowerCase()+": The source will overwrite the destination; the destination is intended to be a mirror of the source.");
			System.out.println("  "+RepositorySynchronizer.Resolution.RESTORE.name().toLowerCase()+": The destination will overwrite the source; the source is intended to be a mirror of the destination.");
			System.out.println("  "+RepositorySynchronizer.Resolution.PRODUCE.name().toLowerCase()+": The source will overwrite the destination, but missing source information will not cause destination information to be removed.");
			System.out.println("  "+RepositorySynchronizer.Resolution.CONSUME.name().toLowerCase()+": The destination will overwrite the source; but missing destination information will not cause source information to be removed.");
			System.out.println("  "+RepositorySynchronizer.Resolution.SYNCHRONIZE.name().toLowerCase()+": Newer information will overwrite older information; the source and destination are intended to be updated with the latest changes from each, although for orphan resources this will be consdered the same as "+RepositorySynchronizer.Resolution.BACKUP.name().toLowerCase()+".");
			System.out.println("  "+RepositorySynchronizer.Resolution.IGNORE.name().toLowerCase()+": No action will occur.");
			System.out.println("-"+Parameter.resourceresolution+": How an orphan resource situation (i.e. one resource exists and the other does not) will be resolved.");
			System.out.println("-"+Parameter.contentresolution+": How a content discrepancy will be resolved.");
			System.out.println("-"+Parameter.metadataresolution+": How a metadata discrepancy will be resolved.");
			System.out.println("-"+Parameter.ignoreproperty+": A metadata property to ignore.");
			System.out.println("-"+Parameter.test+": If specified, no changed will be made.");
			System.out.println("-"+Parameter.verbose+": If specified, debug will be turned on to a report level of "+Debug.ReportLevel.INFO+".");
			System.out.println("-"+Parameter.debughttp+": Whether HTTP communication is logged; requires debug to be turned on.");
			return 0;
		}
		if(hasParameter(args, Parameter.verbose.name()))	//if verbose is turned on
		{
			try
			{
				Debug.setDebug(true);	//turn on debug
				Debug.setMinimumReportLevel(Debug.ReportLevel.INFO);
			}
			catch(final IOException ioException)
			{
				throw new AssertionError(ioException);
			}
		}
		final URI sourceRepositoryURI=guessAbsoluteURI(sourceRepositoryString);	//get the source repository URI
		final String sourceResourceString=getParameter(args, Parameter.sourceresource.name());	//get the source resource parameter
		final URI sourceResourceURI=sourceResourceString!=null ? guessAbsoluteURI(sourceResourceString) : sourceRepositoryURI;	//if the source resource is not specified, use the repository URI
		final URI destinationRepositoryURI=guessAbsoluteURI(destinationRepositoryString);	//get the destination repository URI
		final String destinationResourceString=getParameter(args, Parameter.destinationresource.name());	//get the destination resource parameter
		final URI destinationResourceURI=destinationResourceString!=null ? guessAbsoluteURI(destinationResourceString) : destinationRepositoryURI;	//if the destination resource is not specified, use the repository URI
		HTTPClient.getInstance().setLogged(Debug.isDebug() && hasParameter(args, Parameter.debughttp.name()));	//if debugging is turned on, tell the HTTP client to log its data TODO generalize
		Debug.info("Mirroring from", sourceResourceURI, "to", destinationResourceURI+".");
		final String sourceRepositoryTypeString=getParameter(args, Parameter.sourcerepositorytype.name());
		final Repository sourceRepository=createRepository(sourceRepositoryTypeString!=null ? RepositoryType.valueOf(sourceRepositoryTypeString) : null, sourceRepositoryURI, getParameter(args, Parameter.sourceusername.name()), getParameter(args, Parameter.sourcepassword.name()));	//create the correct type of repository for the source
		final String destinationRepositoryTypeString=getParameter(args, Parameter.destinationrepositorytype.name());
		final Repository destinationRepository=createRepository(destinationRepositoryTypeString!=null ? RepositoryType.valueOf(destinationRepositoryTypeString) : null, destinationRepositoryURI, getParameter(args, Parameter.destinationusername.name()), getParameter(args, Parameter.destinationpassword.name()));	//create the correct type of repository for the destination
		try
		{
			final RepositorySynchronizer repositorySynchronizer=new RepositorySynchronizer();	//create a new synchronizer
			final String resolutionString=getParameter(args, Parameter.resolution.name());	//set the resolutions if provided
			if(resolutionString!=null)
			{
				repositorySynchronizer.setResolution(Resolution.valueOf(resolutionString.toUpperCase()));
			}
			final String resourceResolutionString=getParameter(args, Parameter.resourceresolution.name());
			if(resourceResolutionString!=null)
			{
				repositorySynchronizer.setResourceResolution(Resolution.valueOf(resourceResolutionString.toUpperCase()));
			}
			final String contentResolutionString=getParameter(args, Parameter.contentresolution.name());
			if(contentResolutionString!=null)
			{
				repositorySynchronizer.setContentResolution(Resolution.valueOf(contentResolutionString.toUpperCase()));
			}
			final String metadataResolutionString=getParameter(args, Parameter.metadataresolution.name());
			if(metadataResolutionString!=null)
			{
				repositorySynchronizer.setMetadataResolution(Resolution.valueOf(metadataResolutionString.toUpperCase()));
			}
			final List<String> propertyURIParameters=getParameters(args, Parameter.ignoreproperty.name());	//see if there are any properties to ignore
			if(propertyURIParameters!=null)	//if we have properties to ignore
			{
				for(final String ignorePropertyURIString:propertyURIParameters)	//look at all the properties to ignore
				{
					repositorySynchronizer.addIgnorePropertyURI(URI.create(ignorePropertyURIString));	//create a URI from the parameter and add this to the properties to ignore
				}
			}
			repositorySynchronizer.setTest(hasParameter(args, Parameter.test.name()));	//specify whether this is a test run
			repositorySynchronizer.synchronize(sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//synchronize the resources
		}
		catch(final IOException ioException)	//if there is an error
		{
			Debug.error(ioException);	//TODO fix; why doesn't writing to System.err work?
			System.err.println("Error: "+ioException);
			return 1;
		}
		Debug.info("Mirroring finished.");
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
		<dt>{@value HTTP#HTTP_URI_SCHEME}</dt> <dd>{@link RepositoryType#webdav}</dd>
		<dt>{@value HTTP#HTTPS_SCHEME}</dt> <dd>{@link RepositoryType#webdav}</dd>
		<dt>{@value URIs#FILE_SCHEME}</dt> <dd>{@link RepositoryType#file}</dd>
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
				repositoryType=RepositoryType.webdav;	//assume a WebDAV repository
			}
			else if(FILE_SCHEME.equals(repositoryURI.getScheme()))	//if this is a file repository URI
			{
				repositoryType=RepositoryType.file;	//assume a file repository
			}
			else	//if we don't recognize the repository type
			{
				throw new IllegalArgumentException("Unrecognized repository type: "+repositoryURI);
			}
		}
		switch(repositoryType)	//create the correct type of repository
		{
			case file: 
				return new FileRepository(repositoryURI);	//create a file-based repository
			case ntfs:
				return new NTFSFileRepository(repositoryURI);	//create a file-based repository
			case webdav:
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
			case svn:
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