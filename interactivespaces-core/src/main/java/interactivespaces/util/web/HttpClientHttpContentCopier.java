/*
 * Copyright (C) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package interactivespaces.util.web;

import interactivespaces.InteractiveSpacesException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

/**
 * An {@link HttpContentCopier} which uses Apache HttpClient.
 * 
 * @author Keith M. Hughes
 */
public class HttpClientHttpContentCopier implements HttpContentCopier {
	
	/**
	 * Number of bytes in the copy buffer.
	 */
	private static final int BUFFER_SIZE = 4096;
	
	/**
	 * Connection manager for the client.
	 * 
	 * <p>
	 * It must be thread safe as the client can be used by multiple threads.
	 */
	private ThreadSafeClientConnManager connectionManager;
	
	/**
	 * The HTTPClient instance which does the actual transfer.
	 */
	private HttpClient client;

	@Override
	public void startup() {
		connectionManager = new ThreadSafeClientConnManager();
		connectionManager.setMaxTotal(100);
		client = new DefaultHttpClient(connectionManager);
	}

	@Override
	public void shutdown() {
		connectionManager.shutdown();
	}

	@Override
	public void copy(String sourceUri, File destination) {
		try {
			HttpGet httpget = new HttpGet(sourceUri);
			HttpResponse response = client.execute(httpget);
			
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_OK) {
	
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					InputStream in = entity.getContent();
					try {
						transferFile(in, destination);
					} finally {
						in.close();
					}
				}
			} else {
				throw new InteractiveSpacesException(String.format("Server returned bad status code %d", statusCode));
			}
		} catch (InteractiveSpacesException e) {
			throw e;
		} catch (Exception e) {
			throw new InteractiveSpacesException("Could not read source URI " + sourceUri, e);
		}
	}

	/**
	 * @param in
	 * @throws IOException
	 */
	protected void transferFile(InputStream in, File destination)
			throws IOException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(destination);
			
			byte[] buffer = new byte[BUFFER_SIZE];
			
			int len;
			while ((len = in.read(buffer)) > 0)
				out.write(buffer, 0, len);
	
			out.flush();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// Don't care.
				}
			}
		}
	}
}