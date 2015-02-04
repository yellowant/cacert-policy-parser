package org.cacert.policy;

public class Entity {
	private String abbrev;
	private String name;
	private String link;

	public Entity(String abbrev, String name, String link) {
		this.abbrev = abbrev;
		this.name = name;
		this.link = link;
	}
	public String getAbbrev() {
		return abbrev;
	}
	public String getLink() {
		return link;
	}
	public String getName() {
		return name;
	}
	public String getShortLink(String href, String hrefName) {
		if (!abbrev.toUpperCase().equals(abbrev)) {
			return "<a href='" + HTMLSynthesizer.escape(getLink() + href)
					+ "'>" + HTMLSynthesizer.escape(getName()) + "</a>";
		}
		return HTMLSynthesizer.escape(getName()) + " (<a href='"
				+ HTMLSynthesizer.escape(getLink() + href) + "'>"
				+ HTMLSynthesizer.escape(getAbbrev() + hrefName) + "</a>)";
	}
	public String getLongLink(String href, String hrefName) {
		return getShortLink(href, hrefName);
	}

}
