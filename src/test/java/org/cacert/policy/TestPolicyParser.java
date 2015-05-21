package org.cacert.policy;

import static org.junit.Assert.assertEquals;

import org.cacert.policy.HTMLSynthesizer.Link;
import org.junit.Test;

public class TestPolicyParser {
	private static class DummyTarget implements PolicyTarget {
		StringBuffer buffer = new StringBuffer();
		@Override
		public void emitHeading(int order, String content, String id) {
			buffer.append(",h" + order + content + "->" + id);
		}

		@Override
		public void emitContent(String content) {
			buffer.append(",c" + content);
		}
		@Override
		public void emitLineBreak() {
			buffer.append(",break");
		}
		@Override
		public void endParagraph() {
			buffer.append(",p");
		}

		@Override
		public void emitUnorderedListItem(String content, int lvl) {
			buffer.append(",ul" + content + lvl);
		}

		@Override
		public void emitOrderedListItem(String content, int lvl) {
			buffer.append(",ol" + content);
		}

		@Override
		public void startTable(String clas) {
			buffer.append(",st");
		}

		@Override
		public void emitTableCell(String content) {
			buffer.append(",tc" + content);
		}

		@Override
		public void newTableRow() {
			buffer.append(",tr");
		}

		@Override
		public void endTable() {
			buffer.append(",/tab");
		}

		@Override
		public int getListCounter(int lvl) {
			return 0;
		}

		@Override
		public String close() {
			buffer.append(",l");
			return "";
		}

		@Override
		public void emitTableCellLink(Link string) {

		}

		@Override
		public void emitDescriptionItem(String key, String content, int lvl) {
			buffer.append(",d" + key + "," + content);
		}

	}
	DummyTarget dt = new DummyTarget();
	PolicyParser pp = new PolicyParser(dt);
	private void finish(String string) {
		dt.close();
		assertEquals(string, dt.buffer.toString());
	}

	@Test
	public void testSimpleHeading0() {
		pp.parse("= 0 Heading =\n= 1 Heading1< =", 0);
		finish(",h10 Heading->0,h11 Heading1<->1,l");
	}

	@Test
	public void testLevelHeading() {
		pp.parse("= 0 Heading =\n== 0.1 Heading1< ==", 0);
		finish(",h10 Heading->0,h20.1 Heading1<->0.1,l");
	}
	@Test
	public void testLevelHeadingBig() {
		pp.parse(
				"= 1 Heading =\n== 1.1 Sub Heading ==\n== 1.2 Sub Heading ==\n== 1.2a Sub Heading ==\n== 1.3 Sub Heading ==\n= 2 Heading =",
				0);
		finish(",h11 Heading->1,h21.1 Sub Heading->1.1,h21.2 Sub Heading->1.2,h21.2a Sub Heading->1.2a,h21.3 Sub Heading->1.3,h12 Heading->2,l");
	}
	@Test
	public void testSimpleHeading() {
		pp.parse("= 1 Heading =\n= 2 Heading1< =", 0);
		finish(",h11 Heading->1,h12 Heading1<->2,l");
	}
	@Test
	public void testSimpleParagraph() {
		pp.parse("= 0 Heading =\nThis is the first\n\nthis is the secodn", 0);
		finish(",h10 Heading->0,cThis is the first,p,cthis is the secodn,l");
	}
	@Test
	public void testMultiParagraph() {
		pp.parse("= 0 Heading =\nThis is the first\nthis is the secodn", 0);
		finish(",h10 Heading->0,cThis is the first,cthis is the secodn,l");
	}
	@Test
	public void testOL() {
		pp.parse("= 0 Heading =\n#1. it1\n#1. it2", 0);
		// because noone updates the counter
		finish(",h10 Heading->0,olit1,olit2,l");
	}
	@Test
	public void testUL() {
		pp.parse("= 0 Heading =\na\n* it1\n* it2\nb\n\nc", 0);
		finish(",h10 Heading->0,ca,ulit11,ulit21,cb,p,cc,l");
	}
	@Test
	public void testULPara() {
		pp.parse("= 0 Heading =\n\n\na\n* it1\n* it2\nb\n\nc", 0);
		finish(",h10 Heading->0,p,p,ca,ulit11,ulit21,cb,p,cc,l");
	}
	@Test
	public void testTable() {
		pp.parse("= 0 Heading =\n{|\n|a\n|b\n|-\n|c\n|d\n|}", 0);
		finish(",h10 Heading->0,st,tca,tcb,tr,tcc,tcd,/tab,l");
	}
}
