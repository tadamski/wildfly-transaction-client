/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.wildfly.transaction.client;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.wildfly.client.config.ClientConfiguration;
import org.wildfly.client.config.ConfigXMLParseException;
import org.wildfly.client.config.ConfigurationXMLStreamReader;
import org.wildfly.common.Assert;

/**
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
final class TransactionClientXmlParser {

    private TransactionClientXmlParser() {
    }

    static final String NS_TRANSACTION_CLIENT_1_0 = "urn:wildfly-transaction-client:1.0";

    private static final Set<String> validNamespaces = new HashSet<>(Arrays.asList(NS_TRANSACTION_CLIENT_1_0));

    static void loadConfiguration() {
        final ClientConfiguration clientConfiguration = ClientConfiguration.getInstance();
        if (clientConfiguration != null)
            try (final ConfigurationXMLStreamReader streamReader = clientConfiguration.readConfiguration(validNamespaces)) {
                parseConfiguration(streamReader);
            } catch (ConfigXMLParseException e) {
                throw new IllegalStateException(e);
            }
    }

    private static void parseConfiguration(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        if (reader.hasNext()) {
            if (reader.nextTag() == START_ELEMENT) {
                String namespaceURI = reader.getNamespaceURI();
                if (! validNamespaces.contains(namespaceURI) || ! reader.getLocalName().equals("transaction-client")) {
                    throw reader.unexpectedElement();
                }
                parseTransactionClientElement(reader);
                return;
            }
            throw reader.unexpectedContent();
        }
    }

    private static void parseTransactionClientElement(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        if (reader.getAttributeCount() > 0) {
            throw reader.unexpectedAttribute(0);
        }
        boolean gotDefaultTransactionTimeout = false;
        for (;;) {
            final int next = reader.nextTag();
            if (next == START_ELEMENT) {
                String configuredNamespace = reader.getNamespaceURI();
                if (! validNamespaces.contains(configuredNamespace)) {
                    throw reader.unexpectedElement();
                }
                final String localName = reader.getLocalName();
                if (localName.equals("default-transaction-timeout") && !  gotDefaultTransactionTimeout && inValidNamespace(validNamespaces, configuredNamespace)) {
                    gotDefaultTransactionTimeout  = true;
                    parseDefaultTransactionTimeoutType(reader);
                }
                else {
                    throw reader.unexpectedElement();
                }
            } else if (next == END_ELEMENT) {
                return;
            } else {
                throw Assert.unreachableCode();
            }
        }
    }

    private static boolean inValidNamespace(Set<String> validNamespaces, String configuredNamespace) {
        return validNamespaces.contains(configuredNamespace);
    }

    private static void parseDefaultTransactionTimeoutType(final ConfigurationXMLStreamReader streamReader) throws ConfigXMLParseException {
        final int attributeCount = streamReader.getAttributeCount();
        int timeout = -1 ;
        for (int i = 0; i < attributeCount; i++) {
            if (streamReader.getAttributeNamespace(i) != null && ! streamReader.getAttributeNamespace(i).isEmpty()) {
                throw streamReader.unexpectedAttribute(i);
            }
            final String name = streamReader.getAttributeLocalName(i);
            if (name.equals("seconds")) {
                timeout = streamReader.getIntAttributeValueResolved(i);
            } else {
                throw streamReader.unexpectedAttribute(i);
            }
            ContextTransactionManager.setGlobalDefaultTransactionTimeout(timeout);
        }
        final int next = streamReader.nextTag();
        if (next == END_ELEMENT) {
            return;
        }
        throw streamReader.unexpectedElement();
    }


}