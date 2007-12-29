package com.globalmentor.marmot;

import java.io.IOException;
import java.net.URI;

import static com.garretwilson.net.URIConstants.*;
import static com.garretwilson.net.URIs.*;

import com.garretwilson.net.http.HTTPClient;
import com.garretwilson.rdf.RDFResource;
import com.garretwilson.rdf.dublincore.*;
import com.garretwilson.rdf.version.*;

import static com.garretwilson.text.Characters.*;
import static com.garretwilson.util.CommandLineArgumentUtilities.*;
import com.garretwilson.util.Application;
import com.garretwilson.util.Debug;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.repository.RepositorySynchronizer;
import com.globalmentor.marmot.repository.file.FileRepository;
import com.globalmentor.marmot.repository.webdav.WebDAVRepository;

/**Marmot synchronization client.
@author Garret Wilson
*/
public class MarmotMirror extends Application<Object>
{

	/**The application URI.*/
	public final static URI MARMOT_MIRROR_URI=URI.create("urn:x-globalmentor:software:/marmotmirror");

	/**The application title.*/
	public final static String TITLE="Marmot Mirror"+TRADE_MARK_SIGN_CHAR;

	/**The application copyright.*/
	public final static String COPYRIGHT="Copyright "+COPYRIGHT_SIGN+" 2006 GlobalMentor, Inc. All Rights Reserved.";	//TODO i18n

	/**The version of the application.*/
	public final static String VERSION="Alpha Version 0.1 build 20061124";

	/**Argument constructor.
	@param args The command line arguments.
	*/
	public MarmotMirror(final String[] args)
	{
		super(MARMOT_MIRROR_URI, args);	//construct the parent class
		DCUtilities.addTitle(this, TITLE); //set the application name
		VersionUtilities.addVersion(this, VERSION);  //set the application version
		DCUtilities.addRights(this, COPYRIGHT); //set the application copyright
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
			System.out.println("Usage: MarmotMirror [-sourceRepository <source repository URI>] -source <source URI> [-destinationRepository <destination repository URI>] -destination <destination>");
			return 0;
		}
		final String sourceRepositoryString=getParameter(args, "sourceRepository");	//get the source repository parameter
		final URI sourceResourceURI=guessAbsoluteURI(sourceResourceString);	//get the source URI
		final URI sourceRepositoryURI=sourceRepositoryString!=null ? guessAbsoluteURI(sourceRepositoryString) : getParentURI(sourceResourceURI);	//if the source repository is not specified, use the parent URI
		final String destinationRepositoryString=getParameter(args, "destinationRepository");	//get the destination repository parameter
		final URI destinationResourceURI=guessAbsoluteURI(destinationResourceString);	//get the destination URI
		final URI destinationRepositoryURI=destinationRepositoryString!=null ? guessAbsoluteURI(destinationRepositoryString) : getParentURI(destinationResourceURI);	//if the destination repository is not specified, use the parent URI

		
		HTTPClient.getInstance().setLogged(Debug.isDebug() && Debug.getReportLevels().contains(Debug.ReportLevel.LOG));	//if debugging is turned on, tell the HTTP client to log its data TODO fix this better---make some sort of flag specifically for communication tracking
		
		
		Debug.info("Mirroring from "+sourceRepositoryURI+" "+sourceResourceURI+" to "+destinationRepositoryURI+" "+destinationResourceURI);
		final Repository sourceRepository=createRepository(sourceRepositoryURI);	//create the correct type of repository for the source
		final Repository destinationRepository=createRepository(destinationRepositoryURI);	//create the correct type of repository for the destination
		try
		{
			new RepositorySynchronizer().synchronize(sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//synchronize the resources
		}
		catch(final IOException ioException)	//if there is an error
		{
			Debug.error(ioException);	//TODO fix; why doesn't writing to System.err work?
			System.err.println("Error: "+ioException);
			return 1;
		}
/*TODO fix
		catch(final Throwable throwable)	//TODO del
		{
			Debug.error(throwable);	//TODO fix; why doesn't writing to System.err work?
			return 1;
		}
*/
		
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
		<li>{@value URIConstants#HTTP_SCHEME}</li>
		<li>{@value URIConstants#HTTPS_SCHEME}</li>
		<li>{@value URIConstants#FILE_SCHEME}</li>
	</ul>
	@param repositoryURI The URI of the repository to create.
	@return A repository for the given URI.
	@exception IllegalArgumentException if the type of the given repository URI is not recognized.
	*/
	protected static Repository createRepository(final URI repositoryURI)
	{
		if(isHTTPScheme(repositoryURI))	//if this is an HTTP repository URI
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
