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
			System.out.println("Usage: MarmotMirror [-sourcerepository <source repository URI>] -source <source URI> [-destinationrepository <destination repository URI>] -destination <destination> [-test] [-verbose]");
			System.out.println("-test: If specified, no changed will be made.");
			System.out.println("-verbose: If specified, debug will be turned on to a report level of "+Debug.ReportLevel.INFO+".");
			return 0;
		}
		if(hasSwitch(args, "verbose"))	//if verbose is turned on
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

		
		HTTPClient.getInstance().setLogged(Debug.isDebug() && Debug.getReportLevels().contains(Debug.ReportLevel.LOG));	//if debugging is turned on, tell the HTTP client to log its data TODO fix this better---make some sort of flag specifically for communication tracking
		
		System.out.println("Mirroring from "+sourceResourceURI+" to "+destinationResourceURI+".");
		final Repository sourceRepository=createRepository(sourceRepositoryURI);	//create the correct type of repository for the source
		final Repository destinationRepository=createRepository(destinationRepositoryURI);	//create the correct type of repository for the destination
		try
		{
			final RepositorySynchronizer repositorySynchronizer=new RepositorySynchronizer();	//create a new synchronizer
			repositorySynchronizer.setTest(hasSwitch(args, "test"));	//specify whether this is a test run
			repositorySynchronizer.synchronize(sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//synchronize the resources
		}
		catch(final IOException ioException)	//if there is an error
		{
			Debug.error(ioException);	//TODO fix; why doesn't writing to System.err work?
			System.err.println("Error: "+ioException);
			return 1;
		}
		System.out.println("Mirroring finished.");
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
	@return A repository for the given URI.
	@exception IllegalArgumentException if the type of the given repository URI is not recognized.
	*/
	protected static Repository createRepository(final URI repositoryURI)
	{
		if(HTTP.isHTTPURI(repositoryURI))	//if this is an HTTP repository URI
		{
			return new WebDAVRepository(repositoryURI);	//create a WebDAV-based repository
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