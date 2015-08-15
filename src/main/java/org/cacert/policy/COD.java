package org.cacert.policy;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;

import org.cacert.policy.HTMLSynthesizer.Link;

public class COD extends Entity {
	private String COD;

	private String status;
	private LinkedList<Link> changes;
	private Link editor;
	public COD(String abbrev, String name, String COD, String link,
			String status, LinkedList<Link> changes, Link editor) {
		super(abbrev, name, linkof(abbrev));
		if (!status.equals("POLICY") && !status.equals("DRAFT")) {
			throw new Error("Invalid status of COD " + abbrev + ": " + status);
		}
		this.COD = COD;
		this.status = status;
		this.changes = changes;
		this.editor = editor;

	}
	public static final String LINK_PREFIX = "//policy.cacert.org/";
	private static String linkof(String abbrev) {
		if (abbrev.equals("TTP") || abbrev.equals("PoJAM")) {
			abbrev = "AP-" + abbrev;
		}
		return LINK_PREFIX + abbrev.replace("-", "/") + ".html";
	}

	@Override
	public String getShortLink(String href, String hrefName) {
		return getAbbrev() + " [<a href='"
				+ HTMLSynthesizer.escape(getLink() + href) + "'>COD" + COD
				+ "</a>]" + HTMLSynthesizer.escape(hrefName);
	}
	@Override
	public String getLongLink(String href, String hrefName) {
		return getName() + " (" + getAbbrev() + " [<a href='"
				+ HTMLSynthesizer.escape(getLink())
				+ HTMLSynthesizer.escape(href) + "'>COD" + COD + "</a>]"
				+ HTMLSynthesizer.escape(hrefName) + ")";

	}
	public void printHeader(PrintWriter out) {
		out.println("<table id=\"header\"><tbody><tr>");
		out.println("<td>");
		out.println("Name: " + HTMLSynthesizer.escape(getAbbrev()) + " [COD"
				+ COD + "]<br/>");
		out.println("Status: TESTING, TECHNICAL PREVIEW (but would be "
				+ HTMLSynthesizer.escape(status)
				+ (changes.size() != 0 ? "&nbsp;"
						+ changes.get(changes.size() - 1) : "")
				+ ", if voted for it)<br/>");
		if (editor != null) {
			out.println("Editor: " + editor + "<br/>");
		}
		out.print("Changes: ");
		boolean fst = true;
		for (Link link : changes) {
			out.print((fst ? "" : ", ") + link);
			fst = false;
		}
		out.println("<br/>");
		out.println("Licence: <a href=\"https://wiki.cacert.org/Policy#Licence\""
				+ " title=\"this document is Copyright © CAcert Inc.,"
				+ " licensed openly under CC-by-sa with all disputes resolved under DRP. "
				+ "More at wiki.cacert.org/Policy\">CC-by-sa+DRP</a>");
		out.println("<br/>");
		out.println("This is a rendering of the official policy document text available <a href='"
				+ getLink().replace(".html", ".txt") + "'>here</a>.");
		out.println("</td><td align=\"right\" valign=\"top\">");
		out.println("<a href=\""
				+ HTMLSynthesizer.escape(PolicyGenerator.getEntities()
						.get("PoP").getLink())
				+ "\">TECHNICAL PREIVEW (but would be ");
		out.println("  <img src=\"//cacert.org/policy/images/cacert-"
				+ status.toLowerCase()
				+ ".png\" alt=\"PoP Status - "
				+ status
				+ "\" style=\"border-style: none;\" height=\"31\" width=\"88\">)");
		out.println("</a>");
		out.println("</td></tr></tbody></table>");

		emitBigTitle(out);

	}

	protected void emitBigTitle(PrintWriter out) {
		out.println("<h1>(THIS IS A TECHNICAL PREVIEW AND NOT A CURRENTLY VALID POLICY DOCUMENT)</h1>");
		out.println("<h1>" + generateTitle()
				+ "</h1><h2>Table of Contents</h2>");
	}
	public String generateTitle() {
		return HTMLSynthesizer.escape(getName()) + " ("
				+ HTMLSynthesizer.escape(getAbbrev()) + ")";
	}

	public void emitCODIndexLines(PolicyTarget target,
			HashMap<String, String> comments) {
		target.newTableRow();
		target.emitTableCell("" + COD);
		target.emitTableCell(getAbbrev());
		target.emitTableCellLink(new Link(getLink(), getLink()));
		if (editor != null) {
			target.emitTableCellLink(editor);
		} else {
			target.emitTableCell("");
		}
		target.emitTableCellLink(changes.get(0));
		target.newTableRow();
		target.emitTableCell(status);
		target.emitTableCell(getName());
		String comment = comments.get(getAbbrev());
		if (comment != null) {
			target.emitTableCell(comment);
		} else {
			target.emitTableCell("");
		}
		target.emitTableCell(""); // TODO maybe colspan?
		target.emitTableCellLink(changes.get(changes.size() - 1));

	}

	public String getId() {
		return COD;
	}
}
