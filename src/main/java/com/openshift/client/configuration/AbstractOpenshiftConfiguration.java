/*******************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.openshift.client.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.openshift.internal.client.utils.StreamUtils;
import com.openshift.internal.client.utils.UrlUtils;

/**
 * @author André Dietisheim
 * @author Corey Daley
 */
public abstract class AbstractOpenshiftConfiguration implements IOpenShiftConfiguration {

	protected static final String KEY_RHLOGIN = "default_rhlogin";
	protected static final String KEY_LIBRA_SERVER = "libra_server";
	protected static final String KEY_LIBRA_DOMAIN = "libra_domain";

	protected static final String KEY_PASSWORD = "rhpassword";
	protected static final String KEY_CLIENT_ID = "client_id";

	protected static final String KEY_OPENSHIFT_CLOUD_DOMAIN = "OPENSHIFT_CLOUD_DOMAIN";
	protected static final String KEY_OPENSHIFT_BROKER_HOST = "OPENSHIFT_BROKER_HOST";
	
	protected static final String KEY_TIMEOUT = "timeout";
	protected static final String DEFAULT_OPENSHIFT_TIMEOUT = "180000"; // 3mins

	protected static final String KEY_DISABLE_BAD_SSL_CIPHERS = "disable_bad_sslciphers";

	private static final Pattern QUOTED_REGEX = Pattern.compile("['\"]*([^'\"]+)['\"]*");
	private static final char SINGLEQUOTE = '\'';

	private static final String SYSPROPERTY_PROXY_PORT = "proxyPort";
	private static final String SYSPROPERTY_PROXY_HOST = "proxyHost";
	private static final String SYSPROPERTY_PROXY_SET = "proxySet";

	private Properties properties;
	private File file;

	private boolean doSSLChecks = false;

	public enum ConfigurationOptions {
		YES, NO, AUTO;
		
		private static ConfigurationOptions safeValueOf(String string) {
			if (string == null) {
				return NO;
			}
			
			try {
				return valueOf(string.toUpperCase());
			} catch (IllegalArgumentException e) {
				return NO;
			}
		}
	}

	protected AbstractOpenshiftConfiguration() throws IOException {
		this(null, null);
	}

	protected AbstractOpenshiftConfiguration(IOpenShiftConfiguration parentConfiguration) throws IOException {
		this(null, parentConfiguration);
	}

	protected AbstractOpenshiftConfiguration(File file, IOpenShiftConfiguration parentConfiguration)
			throws IOException {
		initProperties(file, parentConfiguration == null ? null : parentConfiguration.getProperties());
	}

	protected void initProperties(File file) throws IOException {
		initProperties(file, null);
	}

	protected void initProperties(Properties defaultProperties) throws IOException {
		initProperties(null, defaultProperties);
	}

	protected void initProperties(File file, Properties defaultProperties) throws IOException {
		this.file = file;
		this.properties = getProperties(file, defaultProperties);
	}

	protected Properties getProperties(File file, Properties defaultProperties) throws IOException {

		if (file == null
				|| !file.canRead()) {
			return new Properties(defaultProperties);
		}

		FileReader reader = null;
		try {
			Properties properties = new Properties(defaultProperties);
			reader = new FileReader(file);
			properties.load(reader);
			return properties;
		} finally {
			StreamUtils.close(reader);
		}
	}

	protected File getFile() {
		return file;
	}

	public Properties getProperties() {
		return properties;
	}

	public void save() throws IOException {
		if (file == null) {
			return;
		}
		Writer writer = null;
		try {
			writer = new FileWriter(file);
			properties.store(writer, "");
		} finally {
			StreamUtils.close(writer);
		}
	}

	public void setRhlogin(String rhlogin) {
		properties.put(KEY_RHLOGIN, rhlogin);
	}

	public String getRhlogin() {
		return removeQuotes(properties.getProperty(KEY_RHLOGIN));
	}

	public void setLibraServer(String libraServer) {
		properties.put(KEY_LIBRA_SERVER, ensureIsSingleQuoted(libraServer));
	}

	public String getLibraServer() {
		return UrlUtils.ensureStartsWithHttps(removeQuotes(properties.getProperty(KEY_LIBRA_SERVER)));
	}

	public void setLibraDomain(String libraDomain) {
		properties.put(KEY_LIBRA_DOMAIN, ensureIsSingleQuoted(libraDomain));
	}

	public String getLibraDomain() {
		return removeQuotes(properties.getProperty(KEY_LIBRA_DOMAIN));
	}

	public Integer getTimeout() {
		return Integer.parseInt(properties.getProperty(KEY_TIMEOUT));
	}

	protected String ensureIsSingleQuoted(String value) {
		return SINGLEQUOTE + removeQuotes(value) + SINGLEQUOTE;
	}

	protected String removeQuotes(String value) {
		if (value == null) {
			return null;
		}
		Matcher matcher = QUOTED_REGEX.matcher(value);
		if (matcher.find()
				&& matcher.groupCount() == 1) {
			return matcher.group(1);
		} else {
			return value;
		}
	}

	public String getPassword() {
		return removeQuotes(properties.getProperty(KEY_PASSWORD));
	}

	public String getClientId() {
		return properties.getProperty(KEY_CLIENT_ID);
	}

	public ConfigurationOptions getDisableBadSSLCiphers() {
		return ConfigurationOptions.safeValueOf(
				removeQuotes(properties.getProperty(KEY_DISABLE_BAD_SSL_CIPHERS)));
	}

	public void setDisableBadSSLCiphers(ConfigurationOptions option) {
		properties.setProperty(KEY_DISABLE_BAD_SSL_CIPHERS, option.toString());
	}
	
	public void setEnableSSLCertChecks(boolean doSSLChecks) {
		this.doSSLChecks = doSSLChecks;
	}

	public boolean getProxySet() {
		return toBoolean(removeQuotes(properties.getProperty(SYSPROPERTY_PROXY_SET)));
	}

	public String getProxyHost() {
		return removeQuotes(properties.getProperty(SYSPROPERTY_PROXY_HOST));
	}

	public String getProxyPort() {
		return removeQuotes(properties.getProperty(SYSPROPERTY_PROXY_PORT));
	}

	private boolean toBoolean(String string) {
		if (string != null) {
			return Boolean.parseBoolean(string);
		} else {
			return false;
		}
	}
}
