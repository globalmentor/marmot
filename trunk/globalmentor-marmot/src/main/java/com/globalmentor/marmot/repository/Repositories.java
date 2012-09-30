/*
 * Copyright © 2011-2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import static com.globalmentor.java.Appendables.*;
import static com.globalmentor.java.Conditions.*;

import java.io.*;
import java.net.URI;

import org.urframework.*;


/**
 * Constants and utilities for working with repositories.
 * 
 * @author Garret Wilson
 */
public class Repositories
{

	/**
	 * Prints the given resource to the given appendable.
	 * @param resource The resource to print.
	 * @param appendable The appendable to which the resource should be printed.
	 * @return The given appendable.
	 * @throws NullPointerException if the given resource and/or appendable is <code>null</code>.
	 * @throws IOException if there was an error writing to the appendable.
	 */
	public static <A extends Appendable> A print(final URFResource resource, final A appendable) throws IOException
	{
		URFTURFGenerator.forPrint(false).generateResources(appendable, false, resource).append('\n');
		return appendable;
	}

	/**
	 * Prints all resources in the given repository to the given appendable.
	 * @param repository The repository to print.
	 * @param appendable The appendable to which the repository should be printed.
	 * @return The given appendable.
	 * @throws NullPointerException if the given repository and/or appendable is <code>null</code>.
	 * @throws IOException if there was an error writing to the appendable.
	 * @throws IOException if there was an error accessing a repository or writing to the appendable.
	 */
	public static <A extends Appendable> A print(final Repository repository, final A appendable) throws IOException
	{
		return print(repository, repository.getRootURI(), appendable);
	}

	/**
	 * Prints the identified resource and all ancestors in the repository to the given appendable.
	 * @param repository The repository in which the resource lies.
	 * @param resourceURI The URI identifying the resource to print.
	 * @param appendable The appendable to which the resource should be printed.
	 * @return The given appendable.
	 * @throws NullPointerException if the given repository, resource URI, and/or appendable is <code>null</code>.
	 * @throws IOException if there was an error accessing a repository or writing to the appendable.
	 */
	public static <A extends Appendable> A print(final Repository repository, final URI resourceURI, final A appendable) throws IOException
	{
		return print(repository, repository.getResourceDescription(resourceURI), appendable);
	}

	/**
	 * Prints the provided resource and all ancestors in the repository to the given appendable.
	 * @param repository The repository in which the resource lies.
	 * @param resource The resource to print.
	 * @param appendable The appendable to which the resource should be printed.
	 * @return The given appendable.
	 * @throws NullPointerException if the given repository, resource, and/or appendable is <code>null</code>.
	 * @throws IOException if there was an error accessing a repository or writing to the appendable.
	 */
	public static <A extends Appendable> A print(final Repository repository, final URFResource resource, final A appendable) throws IOException
	{
		return print(repository, resource, 0, appendable, URFTURFGenerator.forPrint(repository.getRootURI(), false));
	}

	/**
	 * Prints the provided resource and all ancestors in the repository to the given appendable.
	 * @param repository The repository in which the resource lies.
	 * @param resource The resource to print.
	 * @param level The zero-based level of the resource to be printed.
	 * @param appendable The appendable to which the resource should be printed.
	 * @return The given appendable.
	 * @throws NullPointerException if the given repository, resource, and/or appendable is <code>null</code>.
	 * @throws IOException if there was an error accessing a repository or writing to the appendable.
	 */
	public static <A extends Appendable> A print(final Repository repository, final URFResource resource, final int level, final A appendable,
			final URFTURFGenerator urfTURFGenerator) throws IOException
	{
		try
		{
			append(appendable, '\t', level); //indent
			urfTURFGenerator.generateResources(appendable, false, resource).append('\n');
		}
		catch(final IOException ioException)
		{
			throw unexpected(ioException);
		}
		for(final URFResource childResourceDescription : repository.getChildResourceDescriptions(resource.getURI()))
		{
			print(repository, childResourceDescription, level + 1, appendable, urfTURFGenerator);
		}
		return appendable;
	}

}
