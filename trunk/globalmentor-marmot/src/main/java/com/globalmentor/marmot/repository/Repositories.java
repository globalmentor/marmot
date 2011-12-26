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

import static com.globalmentor.java.Conditions.*;
import static com.globalmentor.java.Strings.*;

import java.io.*;
import java.net.URI;

import com.globalmentor.net.ResourceIOException;
import com.globalmentor.urf.*;

/**
 * Constants and utilities for working with repositories.
 * 
 * @author Garret Wilson
 */
public class Repositories
{
	
	public static URFTURFGenerator createPrintURFGenerator(final URI baseURI)
	{
		final URFTURFGenerator urfTURFGenerator = new URFTURFGenerator(baseURI);
		urfTURFGenerator.setFormatted(false);
		urfTURFGenerator.setPropertyNamespacePrefixesForced(true);
		return urfTURFGenerator;
		
	}

	public static void print(final URFResource resource, final PrintStream printStream)
	{
		try
		{
			printStream.println(createPrintURFGenerator(null).generateResources(new StringBuilder(), false, resource).toString()); //TODO testing
		}
		catch(final IOException ioException)
		{
			throw unexpected(ioException);
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
		print(repository, resource, 0, printStream, createPrintURFGenerator(repository.getRootURI()));
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
