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

package org.springframework.integration.handler.annotation;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.message.MessageMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link MessageMapper} implementation for annotated handler methods.
 * Method parameters are matched against the Message payload as well as its
 * header attributes and properties. If a method parameter is annotated with
 * {@link HeaderAttribute @HeaderAttribute} or {@link HeaderProperty @HeaderProperty},
 * the annotation's value will be used as an attribute/property key. If such an
 * annotation contains no value, then the parameter name will be used as long as
 * the information is available in the class file (requires compilation with
 * debug settings for parameter names). If neither annotation is present, then
 * the parameter will typically match the Message payload. However, if a Map or
 * Properties object is expected, and the paylaod is not itself assignable to
 * that type, then the MessageHeader attributes will be passed in the case of
 * a Map-typed parameter, or the MessageHeader properties will be passed in the
 * case of a Properties-typed parameter. 
 * 
 * @author Mark Fisher
 */
public class AnnotationMethodMessageMapper implements MessageMapper {

	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private final Method method;

	private MethodParameterMetadata[] parameterMetadata;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public AnnotationMethodMessageMapper(Method method) {
		Assert.notNull(method, "method must not be null");
		this.method = method;
	}


	public void initialize() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Class<?>[] paramTypes = this.method.getParameterTypes();			
			this.parameterMetadata = new MethodParameterMetadata[paramTypes.length];
			for (int i = 0; i < parameterMetadata.length; i++) {
				MethodParameter methodParam = new MethodParameter(this.method, i);
				methodParam.initParameterNameDiscovery(this.parameterNameDiscoverer);
				GenericTypeResolver.resolveParameterType(methodParam, this.method.getDeclaringClass());
				Object[] paramAnns = methodParam.getParameterAnnotations();
				String attributeName = null;
				String propertyName = null;
				for (int j = 0; j < paramAnns.length; j++) {
					Object paramAnn = paramAnns[j];
					if (HeaderAttribute.class.isInstance(paramAnn)) {
						HeaderAttribute headerAttribute = (HeaderAttribute) paramAnn;
						attributeName = this.resolveParameterNameIfNecessary(headerAttribute.value(), methodParam);
						parameterMetadata[i] = new MethodParameterMetadata(HeaderAttribute.class, attributeName, headerAttribute.required());
					}
					else if (HeaderProperty.class.isInstance(paramAnn)) {
						HeaderProperty headerProperty = (HeaderProperty) paramAnn;
						propertyName = this.resolveParameterNameIfNecessary(headerProperty.value(), methodParam);
						parameterMetadata[i] = new MethodParameterMetadata(HeaderProperty.class, propertyName, headerProperty.required());
					}
				}
				if (attributeName != null && propertyName != null) {
					throw new ConfigurationException("The @HeaderAttribute and @HeaderProperty annotations " +
							"are mutually exclusive. They should not both be provided on the same parameter.");
				}
				if (attributeName == null && propertyName == null) {
					parameterMetadata[i] = new MethodParameterMetadata(methodParam.getParameterType(), null, false);
				}
			}
			this.initialized = true;
		}
	}

	public Object[] mapMessage(Message message) {
		if (message == null) {
			return null;
		}
		if (message.getPayload() == null) {
			throw new IllegalArgumentException("Message payload must not be null.");
		}
		if (!this.initialized) {
			this.initialize();
		}
		Object[] args = new Object[this.parameterMetadata.length];
		for (int i = 0; i < this.parameterMetadata.length; i++) {
			MethodParameterMetadata metadata = this.parameterMetadata[i];
			Class<?> expectedType = metadata.type;
			if (expectedType.equals(HeaderAttribute.class)) {
				Object value = message.getHeaders().get(metadata.key);
				if (value == null && metadata.required) {
					throw new MessageHandlingException(message,
							"required attribute '" + metadata.key + "' not available");
				}
				args[i] = value;
			}
			else if (expectedType.equals(HeaderProperty.class)) {
				Object value = message.getHeaders().get(metadata.key);
				if (value == null && metadata.required) {
					throw new MessageHandlingException(message,
							"required property '" + metadata.key + "' not available");
				}
				args[i] = value;
			}
			else if (expectedType.isAssignableFrom(message.getClass())) {
				args[i] = message;
			}
			else if (expectedType.isAssignableFrom(message.getPayload().getClass())) {
				args[i] = message.getPayload();
			}
			else if (expectedType.equals(Map.class)) {
				args[i] = message.getHeaders();
			}
			else if (expectedType.equals(Properties.class)) {
				args[i] = this.getStringTypedHeaders(message);
			}
			else {
				args[i] = message.getPayload();
			}
		}
		return args;
	}

	private Properties getStringTypedHeaders(Message<?> message) {
		Properties properties = new Properties();
		MessageHeaders headers = message.getHeaders();
		for (String key : headers.keySet()) {
			Object value = headers.get(key);
			if (value instanceof String) {
				properties.setProperty(key, (String) value);
			}
		}
		return properties;
	}

	private String resolveParameterNameIfNecessary(String paramName, MethodParameter methodParam) {
		if (!StringUtils.hasText(paramName)) {
			paramName = methodParam.getParameterName();
			if (paramName == null) {
				throw new IllegalStateException("No parameter name specified and not available in class file.");
			}
		}
		return paramName;
	}


	private static class MethodParameterMetadata {

		private final Class<?> type;

		private final String key;

		private final boolean required;


		MethodParameterMetadata(Class<?> type, String key, boolean required) {
			this.type = type;
			this.key = key;
			this.required = required;
		}

	}

}
