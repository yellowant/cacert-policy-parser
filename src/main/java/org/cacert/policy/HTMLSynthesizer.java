package org.cacert.policy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Outputs a policy document as HTML.
 */
public class HTMLSynthesizer implements PolicyTarget {
	static class Link {
		String name;
		String link;
		private static final Pattern p = Pattern
				.compile("([^(]+)\\(([^)]+)\\)");
		public Link(String text, String target) {
			name = text;
			link = target;

		}
		public Link(String string) {
			Matcher m = p.matcher(string);
			if (m.matches()) {
				name = m.group(1);
				link = m.group(2);
			} else {
				name = string;
				link = null;
			}
		}
		@Override
		public String toString() {
			if (link == null) {
				return escape(name);
			} else {
				return "<a href='" + escape(link) + "'>" + escape(name)
						+ "</a>";
			}
		}
	}
	private enum State {
		EMPTY, PARAGRAPH, UL, OL, TABLE
	}
	static String escape(String s) {
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}

	private PrintWriter realOut;

	private PrintWriter out;
	private StringWriter content;
	private PrintWriter headS;
	private StringWriter head;

	private int listDepth = 0;
	private int[] listCounter = new int[2];
	private State s = State.EMPTY;

	private COD myDoc;

	public HTMLSynthesizer(PrintWriter out, COD doc) {
		this.realOut = out;
		this.out = new PrintWriter(content = new StringWriter());
		this.headS = new PrintWriter(head = new StringWriter());
		myDoc = doc;
	}
	private boolean state(State state) {
		if (state == s) {
			return false;
		}
		switch (s) {
			case EMPTY :
				break;
			case OL :
				while (listDepth > 0) {
					listCounter[listDepth - 1] = 0;
					listDepth--;
					out.println("</ol>");
				}
				break;
			case PARAGRAPH :
				out.println("</p>");
				break;
			case UL :
				while (listDepth > 0) {
					listDepth--;
					out.println("</ul>");
				}
				break;
			case TABLE :
				throw new Error("Only text is allowed in tables.");
		}
		s = state;
		return true;
	}

	private int TOCDepth;
	@Override
	public void emitHeading(int order, String content, String id) {
		boolean opened = order > TOCDepth;
		while (order > TOCDepth) {
			headS.print("<ul><li>");
			TOCDepth++;
		}
		while (order < TOCDepth) {
			headS.print("</li></ul>");
			TOCDepth--;
		}
		headS.println((opened ? "" : "</li><li>") + "<a href='#s" + escape(id)
				+ "'>" + escape(content) + "</a>");

		state(State.EMPTY);
		out.println("<h" + (order + 1) + " id='s" + escape(id) + "'>"
				+ escape(content) + "</h" + (order + 1) + ">");
	}

	@Override
	public void emitContent(String content) {
		if (state(State.PARAGRAPH)) {
			out.println("<p>");
		}
		out.println(formatContent(content));

	}
	@Override
	public void emitLineBreak() {
		if (s != State.PARAGRAPH) {
			throw new Error("line breaks are only allowed in paragraphs");
		}
		out.println("<br/>");
	}
	private String formatContent(String content) {
		StringBuffer resolved = new StringBuffer();
		int i = 0;
		int next;
		while ((next = content.indexOf('{', i)) != -1) {
			resolved.append(formatPlain(escape(content.substring(i, next))));
			int end = content.indexOf('}', next);
			if (end == -1) {
				throw new Error("Unterminated brace");
			}
			String subst = content.substring(next + 1, end);
			resolved.append(resolveLink(subst));
			i = end + 1;
		}
		resolved.append(formatPlain(escape(content.substring(i,
				content.length()))));
		return resolved.toString();
	}
	private String formatPlain(String escape) {
		for (Entity e : PolicyGenerator.getEntities().values()) {
			if (escape.matches("(^|.*[^A-Z])" + e.getAbbrev() + "([^A-Z].*|$)")) {
				System.err.println("WARNING: possible unlinked entity "
						+ e.getAbbrev() + " in " + escape);
			}
		}
		return escape.replaceAll("\\[i\\]([^\\[]+)\\[/i\\]", "<i>$1</i>");
	}
	private String resolveLink(String content) {
		if (content.startsWith("&")) {
			String[] parts = content.split("#", 2);
			String anchor = "";
			String hrefName = "";
			if (parts.length == 2) {
				anchor = "#s" + parts[1];
				hrefName = " Section " + parts[1];
			}
			if (content.startsWith("&&")) {
				return PolicyGenerator.getEntities().get(parts[0].substring(2))
						.getLongLink(anchor, hrefName);
			} else if (parts[0].equals("&")) {
				return myDoc.getShortLink(anchor, hrefName);
			} else {
				return PolicyGenerator.getEntities().get(parts[0].substring(1))
						.getShortLink(anchor, hrefName);
			}
		} else if (content.matches("[a-z]+://[^ ]+ .*")) {
			String[] parts = content.split(" ", 2);
			System.out.println("WARNING, Unchecked external link: " + parts[0]
					+ "=>" + parts[1]);
			return new Link(parts[1], parts[0]).toString();
		} else if (content.matches("[a-z]+://[^ ]+")) {
			System.out.println("WARNING, Unchecked plain link: " + content);
			return "[" + new Link(content, content).toString() + "]";
		}
		return "-- INVALID -- ";
	}
	@Override
	public void endParagraph() {
		state(State.EMPTY);
	}

	@Override
	public void emitUnorderedListItem(String content, int lvl) {
		state(State.UL);
		while (listDepth < lvl) {
			listDepth++;
			out.println("<ul>");
		}
		while (listDepth > lvl) {
			listDepth--;
			out.println("</ul>");
		}
		out.println("  <li>" + formatContent(content) + "</li>");
	}
	@Override
	public void emitOrderedListItem(String content, int lvl) {
		state(State.OL);
		while (listDepth < lvl) {
			listDepth++;
			out.println("<ol>");
		}
		while (listDepth > lvl) {
			listDepth--;
			listCounter[listDepth] = 0;
			out.println("</ol>");
		}
		listCounter[listDepth - 1]++;
		out.println("  <li>" + formatContent(content) + "</li>");
	}
	@Override
	public void emitDescriptionItem(String key, String content, int lvl) {
		state(State.UL);
		while (listDepth < lvl) {
			listDepth++;
			out.println("<ul>");
		}
		while (listDepth > lvl) {
			listDepth--;
			out.println("</ul>");
		}
		out.println("  <li><span class='desc-key' style='font-weight: bold'>"
				+ formatContent(key) + "</span> "//
				+ formatContent(content) + "</li>");
	}

	@Override
	public void startTable(String clas) {
		state(State.TABLE);
		if (clas != null) {
			out.println("<table border='1' class='" + escape(clas) + "'><tr>");
		} else {
			out.println("<table border='1'><tr>");
		}
	}
	@Override
	public void emitTableCell(String content) {
		out.println("<td>" + formatContent(content) + "</td>");
	}
	@Override
	public void emitTableCellLink(Link content) {
		out.println("<td>" + content + "</td>");
	}
	@Override
	public void newTableRow() {
		out.println("</tr>");
		out.print("<tr>");
	}
	@Override
	public void endTable() {
		s = State.EMPTY;
		out.println("</tr></table>");
	}

	@Override
	public int getListCounter(int lvl) {
		return listCounter[lvl - 1];
	}
	@Override
	public String close() {
		state(State.EMPTY);
		realOut.println("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"stylesheet\" type=\"text/css\" href=\"static/policy.css\"></style><title>"
				+ myDoc.generateTitle() + "</title><body>");
		myDoc.printHeader(realOut);
		while (0 < TOCDepth) {
			headS.print("</li></ul>");
			TOCDepth--;
		}
		realOut.print(head.toString());
		String cStr = content.toString();
		realOut.print(cStr);
		realOut.println("</body></html>");
		realOut.close();
		return cStr;
	}
}
