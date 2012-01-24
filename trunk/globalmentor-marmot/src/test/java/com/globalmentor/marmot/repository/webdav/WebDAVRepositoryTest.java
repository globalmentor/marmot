/*
 * Copyright © 2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository.webdav;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URI;

import org.junit.Ignore;

import com.globalmentor.marmot.repository.*;
import com.globalmentor.net.http.webdav.WebDAVResource;

/**
 * Tests repositories using a WebDAV repository.
 * 
 * <p>
 * Depends on system property:
 * </p>
 * <dl>
 * <dt>password</dt>
 * <dd>The repository password.</dd>
 * </dl>
 * 
 * @author Garret Wilson
 * @see WebDAVRepository
 */
//ignored for normal builds because of time, authentication, and test server provisioning needs
@Ignore
public class WebDAVRepositoryTest extends AbstractRepositoryTest
{

	/** @return Password authentication for accessing the repository. */
	protected PasswordAuthentication getPasswordAuthentication()
	{
		return new PasswordAuthentication("test", System.getProperty("password", "invalid").toCharArray());
	}

	/** @return The URI identifying the collection used for testing. */
	protected URI getTestCollectionResourceURI()
	{
		return URI.create("https://dav.globalmentor.com/test/junit/");
	}

	@Override
	public void before() throws IOException
	{
		//HTTPClient.getInstance().setLogged(true);
		final WebDAVResource junitTestCollectionResource = new WebDAVResource(getTestCollectionResourceURI(), getPasswordAuthentication());
		if(junitTestCollectionResource.exists()) //get rid of the JUnit test collection if it exists
		{
			junitTestCollectionResource.delete();
		}
		junitTestCollectionResource.mkCol(); //create the collection so that it will be available to serve as a repository
		super.before(); //create the repository normally
	}

	@Override
	protected Repository createRepository()
	{
		final WebDAVRepository webDAVRepository = new WebDAVRepository(getTestCollectionResourceURI());
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication();
		webDAVRepository.setUsername(passwordAuthentication.getUserName());
		webDAVRepository.setPassword(passwordAuthentication.getPassword());
		return webDAVRepository;
	}

	@Override
	public void after() throws IOException
	{
		final WebDAVResource junitTestCollectionResource = new WebDAVResource(getTestCollectionResourceURI(), getPasswordAuthentication());
		if(junitTestCollectionResource.exists()) //get rid of the JUnit test collection
		{
			junitTestCollectionResource.delete();
		}
		super.after();
	}

}
