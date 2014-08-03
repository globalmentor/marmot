/*
 * Copyright © 2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository.aws;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.globalmentor.java.Objects.*;
import static com.globalmentor.io.Charsets.*;

import com.globalmentor.model.NameValuePair;
import com.globalmentor.net.URIPath;
import com.globalmentor.net.http.HTTPRequest;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.net.http.HTTP.*;
import static com.globalmentor.security.MessageDigests.*;

/**
 * Constants and utilities for Amazon Simple Storage Service (S3).
 * @author Garret Wilson
 */
public class S3 {

	/** The prefix for Amazon-specific HTTP headers. */
	public final static String AMAZON_HTTP_HEADER_PREFIX = "X-Amz-";

	/** The identifier for AWS authorization. */
	public final static String AWS_AUTHORIZATION = "AWS";

	/**
	 * Signs a request to be used with Amazon Web Services.
	 * @param request The request to sign.
	 * @param bucket The bucket in which the resource lies.
	 * @param path The relative path to the resource.
	 * @param awsAccessKeyID The private AWS access key ID.
	 * @throws NullPointerException if the given request, bucket, path, and/or access key ID is <code>null</code>.
	 */
	public void signRequest(final HTTPRequest request, final String bucket, final URIPath path, final String awsAccessKeyID) {
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(request.getMethod()).append('\n'); //HTTP method
		final String md5Header = request.getHeader(CONTENT_MD5_HEADER); //Content-MD5
		if(md5Header != null) {
			stringBuilder.append(md5Header);
		}
		stringBuilder.append('\n');
		final String contentTypeHeader = request.getHeader(CONTENT_TYPE_HEADER); //Content-Type
		if(contentTypeHeader != null) {
			stringBuilder.append(contentTypeHeader);
		}
		stringBuilder.append('\n');
		final String dateHeader = request.getHeader(DATE_HEADER); //Date
		if(dateHeader != null) {
			stringBuilder.append(dateHeader);
		}
		stringBuilder.append('\n');
		final SortedMap<String, String> amazonHeaderMap = new TreeMap<String, String>(); //the map of canonicalized Amazon headers, sorted by header name
		for(final NameValuePair<String, String> header : request.getHeaders()) { //look at each header
			final String canonicalHeaderName = header.getName().toLowerCase(); //the Amazon canonical form of the header name for purposes of signing is in lowercase
			final String headerValue = header.getValue();
			final String currentHeaderValue = amazonHeaderMap.get(canonicalHeaderName); //see if there's already a value
			amazonHeaderMap.put(canonicalHeaderName, currentHeaderValue != null ? currentHeaderValue + ',' + headerValue : headerValue); //unfold multiple headers with the same name (Amazon doesn't indicate the canonical order of the values; this is probably becaues multiple values for the same header occur infrequently)
		}
		for(final Map.Entry<String, String> amazonHeaderEntry : amazonHeaderMap.entrySet()) { //look at the canonicalized Amazon headers in alphabetical order
			stringBuilder.append(amazonHeaderEntry.getKey()).append(HEADER_SEPARATOR).append(amazonHeaderEntry.getValue()).append('\n');
		}
		stringBuilder.append(ROOT_PATH).append(checkInstance(bucket, "Bucket cannot be null.")).append(PATH_SEPARATOR).append(path.checkRelative());
		final String signature;
		try {
			signature = new String(digest(MessageDigest.getInstance(SHA_ALGORITHM), UTF_8_CHARSET, stringBuilder.toString()), UTF_8_CHARSET);
		} catch(final NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new AssertionError("SHA should always be supported.");
		}
		request.setHeader(AUTHORIZATION_HEADER, AWS_AUTHORIZATION + ' ' + awsAccessKeyID + ':' + signature); //sign the request
	}

}
