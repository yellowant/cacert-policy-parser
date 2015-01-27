package org.cacert.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestHTMLSynthesizer {
	StringWriter sw = new StringWriter();
	PolicyTarget hs = new HTMLSynthesizer(new PrintWriter(sw), "CCA");
	@BeforeClass
	public static void setClassUp() {
		PolicyGenerator.initEntities();
	}
	@Before
	public void setUp() throws Exception {
	}

	private void finish(String expected) {
		assertEquals(expected, hs.close().replace("\r", "").replace("\n", ""));
	}

	@Test
	public void testSimpleContent() {
		hs.emitContent("content");
		finish("<p>content</p>");
	}

	@Test
	public void testSimpleContentEscape() {
		hs.emitContent("content<\"");
		finish("<p>content&lt;&quot;</p>");
	}

	@Test
	public void testSimpleContentMultiline() {
		hs.emitContent("content<\"");
		hs.emitContent("c2");
		finish("<p>content&lt;&quot;c2</p>");
	}

	@Test
	public void testSimpleContentMultiparagraph() {
		hs.emitContent("content<\"");
		hs.endParagraph();
		hs.emitContent("c2");
		finish("<p>content&lt;&quot;</p><p>c2</p>");
	}

	@Test
	public void testList() {
		hs.emitContent("a");
		hs.emitOrderedListItem("li");
		hs.emitOrderedListItem("li2<");
		hs.endParagraph();
		hs.emitContent("b");
		finish("<p>a</p><ol>  <li>li</li>  <li>li2&lt;</li></ol><p>b</p>");
	}

	@Test
	public void testUnorderedlist() {
		hs.emitContent("a");
		hs.emitUnorderedListItem("li", 1);
		hs.emitUnorderedListItem("li2<", 2);
		hs.emitUnorderedListItem("li3", 1);
		hs.endParagraph();
		hs.emitContent("b");
		finish("<p>a</p><ul>  <li>li</li><ul>  <li>li2&lt;</li></ul>  <li>li3</li></ul><p>b</p>");
	}

	@Test
	public void testSimpleTable() {
		hs.emitContent("a");
		hs.startTable();
		hs.emitTableCell("a1<");
		hs.emitTableCell("a2");
		hs.newTableRow();
		hs.emitTableCell("b1");
		hs.emitTableCell("b2");
		try {
			hs.endParagraph();
			fail("that should not work");
		} catch (Error e) {
		}
		try {
			hs.emitContent("");
			fail("that should not work");
		} catch (Error e) {
		}
		hs.endTable();
		hs.endParagraph();
		hs.emitContent("b");
		finish("<p>a</p><table border='1'><tr><td>a1&lt;</td><td>a2</td></tr><tr><td>b1</td><td>b2</td></tr></table><p>b</p>");
	}
}
