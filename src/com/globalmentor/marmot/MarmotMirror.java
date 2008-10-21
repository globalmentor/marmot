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

import static com.globalmentor.java.Characters.*;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.util.CommandLineArguments.*;

import com.globalmentor.marmot.repository.*;
import com.globalmentor.marmot.repository.RepositorySynchronizer.Resolution;
import com.globalmentor.marmot.repository.file.FileRepository;
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
	public final static String VERSION="Alpha Version 0.1 build 2008-10-11";

		//parameters
	/**The command-line parameter for the source repository.*/
	public final static String SOURCE_REPOSITORY_PARAMETER="sourcerepository";
	/**The command-line parameter for the source repository username.*/
	public final static String SOURCE_USERNAME_PARAMETER="sourceusername";
	/**The command-line parameter for the source repository username.*/
	public final static String SOURCE_PASSWORD_PARAMETER="sourcepassword";
	/**The command-line parameter for the destination repository.*/
	public final static String DESTINATION_REPOSITORY_PARAMETER="destinationrepository";
	/**The command-line parameter for the destination repository username.*/
	public final static String DESTINATION_USERNAME_PARAMETER="destinationusername";
	/**The command-line parameter for the destination repository username.*/
	public final static String DESTINATION_PASSWORD_PARAMETER="destinationpassword";

	/**The command-line parameter for test mode.*/
	public final static String TEST_PARAMETER="test";
	/**The command-line parameter for verbose.*/
	public final static String VERBOSE_PARAMETER="verbose";
	/**The command-line parameter for debugging HTTP; requires debug mode on.*/
	public final static String DEBUGHTTP_PARAMETER="debughttp";
	/**The command-line parameter for the resolution mode.*/
	public final static String RESOLUTION_PARAMETER="resolution";
	/**The command-line parameter for the resource resolution mode.*/
	public final static String RESOURCE_RESOLUTION_PARAMETER="resourceresolution";
	/**The command-line parameter for the content resolution mode.*/
	public final static String CONTENT_RESOLUTION_PARAMETER="contentresolution";
	/**The command-line parameter for the metadata resolution mode.*/
	public final static String METADATA_RESOLUTION_PARAMETER="metadataresolution";

	/**Argument constructor.
	@param args The command line arguments.
	*/
	public MarmotMirror(final String[] args)
	{
		super(MARMOT_MIRROR_URI, args);	//construct the parent class
		DCMI.setTitle(this, TITLE); //set the application name
//TODO convert to URF		RDFVersion.addVersion(this, VERSION);  //set the application version
		DCMI.setRights(this, COPYRIGHT); //set the application copyright
	}

	/**The main application method.
	@return The application status.
	*/ 
	public int main()
	{
		final String[] args=getArgs();	//get the arguments
		final String sourceResourceString=getParameter(args, "source");	//get the source parameter
		final String destinationResourceString=getParameter(args, "destination");	//get the destination parameter
		if(sourceResourceString==null || destinationResourceString==null)	//if the source and/or destination parameter is missing
		{
			System.out.println(TITLE);
			System.out.println(VERSION);
			System.out.println(COPYRIGHT);
			System.out.println("Usage: MarmotMirror [-sourcerepository <source repository URI>] [-sourceusername <source username>] [-sourcepassword <source password>] -source <source URI> " +
					"[-destinationrepository <destination repository URI>] [-destinationusername <destination username>] [-destinationpassword <destination password>] -destination <destination> " +
					"[-resolution] [-resourceresolution] [-contentresolution] [-metadataresolution] [-test] [-verbose] [-debughttp]");
			System.out.println("");
			System.out.println("Synchronization occurs on three levels: individual resources (i.e. orphans), metadata, and content, each of which can have a different resolution specified.");
			System.out.println("");
			System.out.println("-"+SOURCE_PASSWORD_PARAMETER+": The source repository password, if appropriate.");
			System.out.println("-"+DESTINATION_USERNAME_PARAMETER+": The destination repository username, if appropriate.");
			System.out.println("-"+DESTINATION_PASSWORD_PARAMETER+": The destination repository password, if appropriate.");
			System.out.println("-"+RESOLUTION_PARAMETER+": The default resolution for encountered conditions; defaults to \"backup\".");
			System.out.println("  backup: The source will overwrite the destination; the destination is intended to be a mirror of the source.");
			System.out.println("  restore: The destination will overwrite the source; the source is intended to be a mirror of the destination.");
			System.out.println("  synchronize: Newer information will overwrite older information; the source and destination are intended to be updated with the latest changes from each; for orphan reso");
			System.out.println("-"+RESOURCE_RESOLUTION_PARAMETER+": How an orphan resource situation (i.e. one resource exists and the other does not) will be resolved.");
			System.out.println("-"+CONTENT_RESOLUTION_PARAMETER+": How a content discrepancy will be resolved.");
			System.out.println("-"+METADATA_RESOLUTION_PARAMETER+": How a metadata discrepancy will be resolved.");
			System.out.println("-"+TEST_PARAMETER+": If specified, no changed will be made.");
			System.out.println("-"+VERBOSE_PARAMETER+": If specified, debug will be turned on to a report level of "+Debug.ReportLevel.INFO+".");
			System.out.println("-"+DEBUGHTTP_PARAMETER+": Whether HTTP communication is logged; requires debug to be turned on.");
			return 0;
		}
		if(hasParameter(args, VERBOSE_PARAMETER))	//if verbose is turned on
		{
			try	//TODO improve
			{
				Debug.setDebug(true);	//turn on debug
				Debug.setMinimumReportLevel(Debug.ReportLevel.INFO);
			}
			catch(final IOException ioException)
			{
				throw new AssertionError(ioException);
			}
		}
		final String sourceRepositoryString=getParameter(args, "sourcerepository");	//get the source repository parameter
		final URI sourceResourceURI=guessAbsoluteURI(sourceResourceString);	//get the source URI
		final URI sourceRepositoryURI=sourceRepositoryString!=null ? guessAbsoluteURI(sourceRepositoryString) : getParentURI(sourceResourceURI);	//if the source repository is not specified, use the parent URI
		final String destinationRepositoryString=getParameter(args, "destinationrepository");	//get the destination repository parameter
		final URI destinationResourceURI=guessAbsoluteURI(destinationResourceString);	//get the destination URI
		final URI destinationRepositoryURI=destinationRepositoryString!=null ? guessAbsoluteURI(destinationRepositoryString) : getParentURI(destinationResourceURI);	//if the destination repository is not specified, use the parent URI
		HTTPClient.getInstance().setLogged(Debug.isDebug() && hasParameter(args, DEBUGHTTP_PARAMETER));	//if debugging is turned on, tell the HTTP client to log its data TODO generalize
		Debug.info("Mirroring from", sourceResourceURI, "to", destinationResourceURI+".");
		final Repository sourceRepository=createRepository(sourceRepositoryURI, getParameter(args, SOURCE_USERNAME_PARAMETER), getParameter(args, SOURCE_PASSWORD_PARAMETER));	//create the correct type of repository for the source
		final Repository destinationRepository=createRepository(destinationRepositoryURI, getParameter(args, DESTINATION_USERNAME_PARAMETER), getParameter(args, DESTINATION_PASSWORD_PARAMETER));	//create the correct type of repository for the destination
		try
		{
			final RepositorySynchronizer repositorySynchronizer=new RepositorySynchronizer();	//create a new synchronizer
			final String resolutionString=getParameter(args, RESOLUTION_PARAMETER);	//set the resolutions if provided
			if(resolutionString!=null)
			{
				repositorySynchronizer.setResolution(Resolution.valueOf(resolutionString.toUpperCase()));
			}
			final String resourceResolutionString=getParameter(args, RESOURCE_RESOLUTION_PARAMETER);
			if(resourceResolutionString!=null)
			{
				repositorySynchronizer.setResourceResolution(Resolution.valueOf(resourceResolutionString.toUpperCase()));
			}
			final String contentResolutionString=getParameter(args, CONTENT_RESOLUTION_PARAMETER);
			if(contentResolutionString!=null)
			{
				repositorySynchronizer.setContentResolution(Resolution.valueOf(contentResolutionString.toUpperCase()));
			}
			final String metadataResolutionString=getParameter(args, METADATA_RESOLUTION_PARAMETER);
			if(metadataResolutionString!=null)
			{
				repositorySynchronizer.setMetadataResolution(Resolution.valueOf(metadataResolutionString.toUpperCase()));
			}
			repositorySynchronizer.setTest(hasParameter(args, TEST_PARAMETER));	//specify whether this is a test run
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
	<p>The following types of repository schemes are recognized:</p>
	<ul>
		<li>{@value HTTP#HTTP_URI_SCHEME}</li>
		<li>{@value HTTP#HTTPS_SCHEME}</li>
		<li>{@value URIs#FILE_SCHEME}</li>
	</ul>
	@param repositoryURI The URI of the repository to create.
	@param username The username of the repository, or <code>null</code> if no username is appropriate.
	@param password The password of the repository, or <code>null</code> if no password is appropriate.
	@return A repository for the given URI.
	@exception IllegalArgumentException if the type of the given repository URI is not recognized.
	*/
	protected static Repository createRepository(final URI repositoryURI, final String username, final String password)
	{
		if(HTTP.isHTTPURI(repositoryURI))	//if this is an HTTP repository URI
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
		else if(FILE_SCHEME.equals(repositoryURI.getScheme()))	//if this is a file repository URI
		{
			return new FileRepository(repositoryURI);	//create a file-based repository
		}
		else	//if we don't recognize the repository type
		{
			throw new IllegalArgumentException("Unrecognized repository type: "+repositoryURI);
		}
		
	}
}