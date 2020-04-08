/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.client.configuration;

import org.eclipse.ditto.model.base.json.JsonSchemaVersion;

import javax.annotation.Nullable;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkArgument;
import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;

/**
 * Provides Ditto WebSocket messaging specific configuration.
 *
 * @since 1.0.0
 */
public final class WebSocketMessagingConfiguration implements MessagingConfiguration {

    private final JsonSchemaVersion jsonSchemaVersion;
    private final URI endpointUri;
    private final boolean reconnectEnabled;
    @Nullable private final ProxyConfiguration proxyConfiguration;
    @Nullable private final TrustStoreConfiguration trustStoreConfiguration;

    private WebSocketMessagingConfiguration(final JsonSchemaVersion jsonSchemaVersion, final URI endpointUri,
            final boolean reconnectEnabled, @Nullable final ProxyConfiguration proxyConfiguration,
            @Nullable final TrustStoreConfiguration trustStoreConfiguration) {
        this.jsonSchemaVersion = jsonSchemaVersion;
        this.endpointUri = endpointUri;
        this.reconnectEnabled = reconnectEnabled;
        this.proxyConfiguration = proxyConfiguration;
        this.trustStoreConfiguration = trustStoreConfiguration;
    }

    public static MessagingConfiguration.Builder newBuilder() {
        return new WebSocketMessagingConfigurationBuilder();
    }

    @Override
    public JsonSchemaVersion getJsonSchemaVersion() {
        return jsonSchemaVersion;
    }

    @Override
    public URI getEndpointUri() {
        return endpointUri;
    }

    @Override
    public boolean isReconnectEnabled() {
        return reconnectEnabled;
    }

    @Override
    public Optional<ProxyConfiguration> getProxyConfiguration() {
        return Optional.ofNullable(proxyConfiguration);
    }

    @Override
    public Optional<TrustStoreConfiguration> getTrustStoreConfiguration() {
        return Optional.ofNullable(trustStoreConfiguration);
    }

    private static final class WebSocketMessagingConfigurationBuilder implements MessagingConfiguration.Builder {

        private static final List<String> ALLOWED_URI_SCHEME = Arrays.asList("wss", "ws");
        private static final String WS_PATH = "/ws/";
        private static final String WS_PATH_REGEX = "/ws/(1|2)/?";

        private JsonSchemaVersion jsonSchemaVersion = JsonSchemaVersion.LATEST;
        private URI endpointUri;
        private boolean reconnectEnabled = true;
        private ProxyConfiguration proxyConfiguration;
        private TrustStoreConfiguration trustStoreConfiguration;

        @Override
        public MessagingConfiguration.Builder jsonSchemaVersion(final JsonSchemaVersion jsonSchemaVersion) {
            this.jsonSchemaVersion = checkNotNull(jsonSchemaVersion, "jsonSchemaVersion");
            return this;
        }

        @Override
        public MessagingConfiguration.Builder endpoint(final String endpoint) {
            final URI uri = URI.create(checkNotNull(endpoint));
            final String uriScheme = uri.getScheme();
            checkArgument(uriScheme, ALLOWED_URI_SCHEME::contains, () -> {
                final String msgTemplate = "Scheme {0} not allowed for endpoint URI! Must be one of {1}.";
                return MessageFormat.format(msgTemplate, uriScheme, ALLOWED_URI_SCHEME);
            });

            this.endpointUri = uri;
            return this;
        }

        @Override
        public MessagingConfiguration.Builder reconnectEnabled(final boolean reconnectEnabled) {
            this.reconnectEnabled = reconnectEnabled;
            return this;
        }

        @Override
        public MessagingConfiguration.Builder proxyConfiguration(final ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = checkNotNull(proxyConfiguration, "proxyConfiguration");
            return this;
        }

        @Override
        public MessagingConfiguration.Builder trustStoreConfiguration(
                final TrustStoreConfiguration trustStoreConfiguration) {
            this.trustStoreConfiguration = checkNotNull(trustStoreConfiguration, "trustStoreConfiguration");
            return this;
        }

        @Override
        public MessagingConfiguration build() {
            final URI wsEndpointUri= appendWsPathIfNecessary(this.endpointUri, jsonSchemaVersion);
            return new WebSocketMessagingConfiguration(jsonSchemaVersion, wsEndpointUri, reconnectEnabled,
                    proxyConfiguration, trustStoreConfiguration);
        }

        private static URI appendWsPathIfNecessary(final URI baseUri, final JsonSchemaVersion schemaVersion) {
            if (needToAppendWsPath(baseUri)) {
                final String pathWithoutTrailingSlashes = removeTrailingSlashFromPath(baseUri.getPath());
                final String newPath = pathWithoutTrailingSlashes + WS_PATH + schemaVersion.toString();
                return baseUri.resolve(newPath);
            } else {
                checkIfBaseUriAndSchemaVersionMatch(baseUri, schemaVersion);
                return baseUri;
            }
        }

        private static boolean needToAppendWsPath(final URI baseUri) {
            final Pattern pattern = Pattern.compile(WS_PATH_REGEX);
            final Matcher matcher = pattern.matcher(baseUri.toString());
            return !matcher.find();
        }

        private static void checkIfBaseUriAndSchemaVersionMatch(final URI baseUri, final JsonSchemaVersion schemaVersion) {
            final String path = removeTrailingSlashFromPath(baseUri.getPath());
            final String apiVersion = path.substring(path.length() - 1, path.length());
            if (!schemaVersion.toString().equals(apiVersion)) {
                throw new IllegalArgumentException("The jsonSchemaVersion and apiVersion of the endpoint do not match. " +
                        "Either remove the ws path from the endpoint or " +
                        "use the same jsonSchemaVersion as in the ws path of the endpoint.");
            }
        }

        private static String removeTrailingSlashFromPath(final String path) {
            return path.replaceFirst("/+$", "");
        }

    }

}
