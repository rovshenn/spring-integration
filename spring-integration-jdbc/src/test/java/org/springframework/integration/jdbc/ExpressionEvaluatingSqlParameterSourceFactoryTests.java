/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * @author Dave Syer
 * @author Meherzad Lahewala
 */
public class ExpressionEvaluatingSqlParameterSourceFactoryTests {

	private final ExpressionEvaluatingSqlParameterSourceFactory factory =
			new ExpressionEvaluatingSqlParameterSourceFactory();

	@Test
	public void testSetStaticParameters() throws Exception {
		factory.setStaticParameters(Collections.singletonMap("foo", "bar"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(null);
		assertThat(source.hasValue("foo")).isTrue();
		assertThat(source.getValue("foo")).isEqualTo("bar");
		assertThat(source.getSqlType("foo")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
	}

	@Test
	public void testMapInput() throws Exception {
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertThat(source.hasValue("foo")).isTrue();
		assertThat(source.getValue("foo")).isEqualTo("bar");
		assertThat(source.getSqlType("foo")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
	}

	@Test
	public void testListOfMapsInput() throws Exception {
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "foo";
		assertThat(source.hasValue(expression)).isTrue();
		assertThat(source.getValue(expression).toString()).isEqualTo("[bar, bucket]");
		assertThat(source.getSqlType(expression)).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
	}

	@Test
	public void testMapInputWithExpression() throws Exception {
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		// This is an illegal parameter name in Spring JDBC so we'd never get this as input
		assertThat(source.hasValue("foo.toUpperCase()")).isTrue();
		assertThat(source.getValue("foo.toUpperCase()")).isEqualTo("BAR");
		assertThat(source.getSqlType("food")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
	}

	@Test
	public void testMapInputWithMappedExpression() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "foo.toUpperCase()"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("foo", "bar"));
		assertThat(source.hasValue("spam")).isTrue();
		assertThat(source.getValue("spam")).isEqualTo("BAR");
		assertThat(source.getSqlType("spam")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
	}

	@Test
	public void testMapInputWithMappedExpressionResolveStatic() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "#staticParameters['foo'].toUpperCase()"));
		factory.setStaticParameters(Collections.singletonMap("foo", "bar"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Collections.singletonMap("crap", "bucket"));
		assertThat(source.hasValue("spam")).isTrue();
		assertThat(source.getValue("spam")).isEqualTo("BAR");
		assertThat(source.getSqlType("spam")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
	}

	@Test
	public void testListOfMapsInputWithExpression() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "foo.toUpperCase()"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "spam";
		assertThat(source.hasValue(expression)).isTrue();
		assertThat(source.getValue(expression).toString()).isEqualTo("[BAR, BUCKET]");
		assertThat(source.getSqlType("foo")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
	}

	@Test
	public void testListOfMapsInputWithExpressionAndTypes() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "foo.toUpperCase()"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.setSqlParameterTypes(Collections.singletonMap("spam", Types.SQLXML));
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "spam";
		assertThat(source.hasValue(expression)).isTrue();
		assertThat(source.getValue(expression).toString()).isEqualTo("[BAR, BUCKET]");
		assertThat(source.getSqlType("spam")).isEqualTo(Types.SQLXML);
	}

	@Test
	public void testListOfMapsInputWithExpressionAndEmptyTypes() throws Exception {
		factory.setParameterExpressions(Collections.singletonMap("spam", "foo.toUpperCase()"));
		factory.setBeanFactory(mock(BeanFactory.class));
		factory.setSqlParameterTypes(Collections.emptyMap());
		factory.afterPropertiesSet();
		SqlParameterSource source = factory.createParameterSource(Arrays.asList(Collections.singletonMap("foo", "bar"),
				Collections.singletonMap("foo", "bucket")));
		String expression = "spam";
		assertThat(source.hasValue(expression)).isTrue();
		assertThat(source.getValue(expression).toString()).isEqualTo("[BAR, BUCKET]");
		assertThat(source.getSqlType("spam")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
	}

}
