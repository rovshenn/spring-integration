/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.transformer;

import static org.junit.Assert.assertEquals;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;

import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.Document;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringSource;

/**
 * @author Jonas Partner
 */
public class XsltPayloadTransformerTest {

	XsltPayloadTransformer transformer;

	String doc = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";

	@Before
	public void setUp() throws Exception {
		transformer = new XsltPayloadTransformer(getXslResource());
	}

	@Test
	public void testDocumentAsPayload() throws Exception {
		GenericMessage<Document> documentMessage = new GenericMessage<Document>(XmlTestUtil.getDocumentForString(doc));
		transformer.transform(documentMessage);

	}

	@Test
	public void testSourceAsPayload() throws Exception {
		GenericMessage<Source> message = new GenericMessage<Source>(new StringSource(doc));
		Message<?> transformed = transformer.transform(message);
		DOMResult result = (DOMResult) transformed.getPayload();
		String rootNodeName = ((Document) result.getNode()).getDocumentElement().getNodeName();
		assertEquals("Wrong name for root element after transform", "bob", rootNodeName);
	}

	@Test
	public void testStringAsPayload() throws Exception {
		GenericMessage<Object> message = new GenericMessage<Object>(doc);
		Message<?> transformed = transformer.transform(message);
		DOMResult result = (DOMResult) transformed.getPayload();
		String rootNodeName = ((Document) result.getNode()).getDocumentElement().getNodeName();
		assertEquals("Wrong name for root element after transform", "bob", rootNodeName);
	}

	@Test(expected = MessagingException.class)
	public void testNonXmlString() throws Exception {
		transformer.transform(new StringMessage("test"));
	}

	@Test(expected = MessagingException.class)
	public void testUnsupportedPayloadType() throws Exception {
		transformer.transform(new GenericMessage<Long>(new Long(12)));
	}


	private Resource getXslResource() throws Exception {
		String xsl = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:template match=\"order\"><bob>test</bob></xsl:template></xsl:stylesheet>";
		return new ByteArrayResource(xsl.getBytes("UTF-8"));
	}

}
