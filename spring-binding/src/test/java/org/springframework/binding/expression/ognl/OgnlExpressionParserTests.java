/*
 * Copyright 2004-2012 the original author or authors.
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
package org.springframework.binding.expression.ognl;

import junit.framework.TestCase;

import org.springframework.binding.convert.converters.StringToDate;
import org.springframework.binding.convert.service.GenericConversionService;
import org.springframework.binding.expression.EvaluationException;
import org.springframework.binding.expression.Expression;
import org.springframework.binding.expression.ExpressionVariable;
import org.springframework.binding.expression.ParserException;
import org.springframework.binding.expression.ValueCoercionException;
import org.springframework.binding.expression.support.FluentParserContext;

public class OgnlExpressionParserTests extends TestCase {

	private OgnlExpressionParser parser = new OgnlExpressionParser();

	private TestBean bean = new TestBean();

	public void testParseSimple() {
		String exp = "flag";
		Expression e = parser.parseExpression(exp, null);
		assertNotNull(e);
		Boolean b = (Boolean) e.getValue(bean);
		assertFalse(b);
	}

	public void testParseSimpleAllowDelimited() {
		parser.setAllowDelimitedEvalExpressions(true);
		String exp = "${flag}";
		Expression e = parser.parseExpression(exp, null);
		assertNotNull(e);
		Boolean b = (Boolean) e.getValue(bean);
		assertFalse(b);
	}

	public void testParseSimpleDelimitedNotAllowed() {
		String exp = "${flag}";
		try {
			parser.parseExpression(exp, null);
			fail("should have failed");
		} catch (ParserException e) {
		}
	}

	public void testParseTemplateSimpleLiteral() {
		String exp = "flag";
		Expression e = parser.parseExpression(exp, new FluentParserContext().template());
		assertNotNull(e);
		assertEquals("flag", e.getValue(bean));
	}

	public void testParseTemplateEmpty() {
		Expression e = parser.parseExpression("", new FluentParserContext().template());
		assertNotNull(e);
		assertEquals("", e.getValue(bean));
	}

	public void testParseTemplateComposite() {
		String exp = "hello ${flag} ${flag} ${flag}";
		Expression e = parser.parseExpression(exp, new FluentParserContext().template());
		assertNotNull(e);
		String str = (String) e.getValue(bean);
		assertEquals("hello false false false", str);
	}

	public void testTemplateEnclosedCompositeNotSupported() {
		String exp = "${hello ${flag} ${flag} ${flag}}";
		try {
			parser.parseExpression(exp, new FluentParserContext().template());
			fail("Should've failed - not intended use");
		} catch (ParserException e) {
		}
	}

	public void testSyntaxError1() {
		try {
			parser.parseExpression("${", new FluentParserContext().template());
			fail();
		} catch (ParserException e) {
		}
		try {
			String exp = "hello ${flag} ${abcd defg";
			parser.parseExpression(exp, null);
			fail("Should've failed - not intended use");
		} catch (ParserException e) {
		}
	}

	public void testSyntaxError2() {
		try {
			parser.parseExpression("${}", new FluentParserContext().template());
			fail("Should've failed - not intended use");
		} catch (ParserException e) {
		}
		try {
			String exp = "hello ${flag} ${}";
			parser.parseExpression(exp, null);
			fail("Should've failed - not intended use");
		} catch (ParserException e) {
		}
	}

	public void testCollectionConstructionSyntax() {
		// lists
		parser.parseExpression("name in {null, \"Untitled\"}", null);
		parser.parseExpression("${name in {null, \"Untitled\"}}", new FluentParserContext().template());

		// native arrays
		parser.parseExpression("new int[] {1, 2, 3}", null);
		parser.parseExpression("${new int[] {1, 2, 3}}", new FluentParserContext().template());

		// maps
		parser.parseExpression("#{ 'foo' : 'foo value', 'bar' : 'bar value' }", null);
		parser.parseExpression("${#{ 'foo' : 'foo value', 'bar' : 'bar value' }}", new FluentParserContext().template());
		parser.parseExpression("#@java.util.LinkedHashMap@{ 'foo' : 'foo value', 'bar' : 'bar value' }", null);
		parser.parseExpression("${#@java.util.LinkedHashMap@{ 'foo' : 'foo value', 'bar' : 'bar value' }}",
				new FluentParserContext().template());

		// complex examples
		parser.parseExpression("b,#{1:2}", null);
		parser.parseExpression("${b,#{1:2}}", new FluentParserContext().template());
		parser.parseExpression("a${b,#{1:2},e}f${g,#{3:4},j}k", new FluentParserContext().template());
	}

	public void testVariables() {
		Expression exp = parser.parseExpression("#var",
				new FluentParserContext().variable(new ExpressionVariable("var", "flag")));
		assertFalse((Boolean) exp.getValue(bean));
	}

	public void testVariablesWithCoersion() {
		Expression exp = parser.parseExpression("#var", new FluentParserContext().variable(new ExpressionVariable(
				"var", "number", new FluentParserContext().expectResult(Long.class))));
		assertEquals(new Long(0), exp.getValue(bean));
	}

	public void testNestedVariablesWithTemplates() {
		Expression exp = parser.parseExpression("#var", new FluentParserContext().variable(new ExpressionVariable(
				"var", "${flag}${#var}", new FluentParserContext().template().variable(
						new ExpressionVariable("var", "number")))));
		assertEquals("false0", exp.getValue(bean));
	}

	public void testGetExpressionString() {
		String expressionString = "maximum";
		Expression exp = parser.parseExpression(expressionString, null);
		assertEquals("maximum", exp.getExpressionString());
	}

	public void testGetValueType() {
		String exp = "flag";
		Expression e = parser.parseExpression(exp, null);
		assertEquals(boolean.class, e.getValueType(bean));
	}

	public void testGetValueTypeNullCollectionValue() {
		String exp = "list[0]";
		Expression e = parser.parseExpression(exp, null);
		assertEquals(null, e.getValueType(bean));
	}

	public void testGetValueWithCoersion() {
		String expressionString = "number";
		Expression exp = parser.parseExpression(expressionString, new FluentParserContext().expectResult(String.class));
		TestBean context = new TestBean();
		assertEquals("0", exp.getValue(context));
	}

	public void testGetValueCoersionError() {
		String expressionString = "number";
		Expression exp = parser.parseExpression(expressionString,
				new FluentParserContext().expectResult(TestBean.class));
		TestBean context = new TestBean();
		try {
			exp.getValue(context);
			fail("Should have failed with coersion");
		} catch (ValueCoercionException e) {
		}
	}

	public void testSetValue() {
		String expressionString = "number";
		Expression exp = parser.parseExpression(expressionString, null);
		TestBean context = new TestBean();
		exp.setValue(context, 5);
		assertEquals(5, context.getNumber());
	}

	public void testSetValueWithCoersion() {
		GenericConversionService cs = (GenericConversionService) parser.getConversionService();
		StringToDate converter = new StringToDate();
		converter.setPattern("yyyy-MM-dd");
		cs.addConverter(converter);
		Expression e = parser.parseExpression("date", null);
		e.setValue(bean, "2008-9-15");
	}

	public void testSetBogusValueWithCoersion() {
		Expression e = parser.parseExpression("date", null);
		try {
			e.setValue(bean, "bogus");
			fail("Should have failed tme");
		} catch (ValueCoercionException ex) {
		}
	}

	public void testReasonCauseLinkingGetValue() {
		String exp = "getException()";
		Expression e = parser.parseExpression(exp, null);
		try {
			e.getValue(bean);
		} catch (EvaluationException ex) {
			assertTrue(ex.getCause().getCause() instanceof IllegalStateException);
		}
	}

	public void testReasonCauseLinkingSetValue() {
		String exp = "exceptionProperty";
		Expression e = parser.parseExpression(exp, null);
		try {
			e.setValue(bean, "does not matter");
		} catch (EvaluationException ex) {
			assertTrue(ex.getCause().getCause() instanceof IllegalStateException);
		}
	}

}
