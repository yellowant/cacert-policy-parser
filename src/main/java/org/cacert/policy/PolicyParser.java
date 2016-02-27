package org.cacert.policy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a given Policy document and delivers it to the given
 * {@link #PolicyParser(PolicyTarget)}
 */
public class PolicyParser {
	PolicyTarget out;
	int number;
	private static final Pattern[] headings = new Pattern[]{
			Pattern.compile("([0-9]+[a-z]?)"),
			Pattern.compile("([0-9]+[a-z]?)\\.([0-9]+[a-z]?)"),
			Pattern.compile("([0-9]+[a-z]?)\\.([0-9]+[a-z]?)\\.([0-9]+[a-z]?)"),
			Pattern.compile("([0-9]+[a-z]?)\\.([0-9]+[a-z]?)\\.([0-9]+[a-z]?)\\.([0-9]+[a-z]?)")};
	String[] headingCounter = new String[3];
	private PolicyGenerator generator;

	/**
	 * Creates a new PolicyParser.
	 * 
	 * @param out
	 *            the target to deliver the document to.
	 */
	public PolicyParser(PolicyGenerator generator, PolicyTarget out) {
		this.generator = generator;
		this.out = out;
	}

	/**
	 * Parses the given document. Only use once per instance.
	 * 
	 * @param templateDoc
	 *            the content of the document to process
	 * @param headerLen
	 */
	public void parse(String templateDoc, int headerLen) {
		String[] lines = templateDoc.split("\n");
		for (int i = 0; i < lines.length; i++) {
			int lineN = i + headerLen;
			String line = lines[i];
			line = line.trim();
			if (line.isEmpty()) {
				out.endParagraph();
			} else if (line.startsWith("=")) {
				number = 0;
				if (line.startsWith("= ") && line.endsWith(" =")) {
					handleHeading(line, 1, lineN);
				} else if (line.startsWith("== ") && line.endsWith(" ==")) {
					handleHeading(line, 2, lineN);
				} else if (line.startsWith("=== ") && line.endsWith(" ===")) {
					handleHeading(line, 3, lineN);
				} else if (line.startsWith("==== ") && line.endsWith(" ====")) {
					handleHeading(line, 4, lineN);
				} else {
					generator.reportError("Crappy header in line: " + lineN);
					return;
				}
			} else if (line.startsWith("#")) {
				String[] parts = line.split(" ", 2);
				if (parts.length != 2 || !parts[0].endsWith(".")) {
					throw new Error("Invalid numbering in line " + lineN);
				}
				int depth = 0;
				while (parts[0].startsWith("#")) {
					depth++;
					parts[0] = parts[0].substring(1);
				}
				if (depth > 2) {
					throw new Error(
							"lists with depth grater than 2 for sanity not supported.");
				}
				int num = Integer.parseInt(parts[0].substring(0,
						parts[0].length() - 1));
				parts[1] = parts[1].trim();
				if (num != out.getListCounter(depth) + 1) {
					throw new Error("Invalid numbering in line " + lineN
							+ " is " + num + " should be "
							+ (out.getListCounter(depth) + 1));
				}
				out.emitOrderedListItem(parts[1], depth);
			} else if (line.startsWith("[")) {
				String[] parts = line.substring(1).split("] ", 2);
				if (parts.length != 2) {
					generator
							.reportError("Wrong format for description in line "
									+ lineN);
					return;
				}
				out.emitDescriptionItem(parts[0], parts[1], 1);
			} else if (line.startsWith("* ")) {
				out.emitUnorderedListItem(line.substring(2), 1);
			} else if (line.startsWith("** ")) {
				out.emitUnorderedListItem(line.substring(2), 2);
			} else if (line.startsWith("{|")) {
				out.startTable(null);
				for (int j = i + 1; j < lines.length; j++) {
					line = lines[j];
					line = line.trim();
					if (line.startsWith("|-")) {
						out.newTableRow();
					} else if (line.startsWith("|}")) {
						out.endTable();
						i = j + 1;
						break;
					} else if (line.startsWith("|")) {
						out.emitTableCell(line.substring(1));
					}
				}
			} else {
				boolean breakAfter = false;
				if (line.endsWith("\\\\")) {
					line = line.substring(0, line.length() - 2);
					breakAfter = true;
				}
				out.emitContent(line);
				if (breakAfter) {
					out.emitLineBreak();
				}
			}
		}
	}
	private String handleHeading(String line, int level, int lineN) {
		String content = line.substring(level + 1, line.length() - level - 1);
		String[] parts = content.split(" ", 2);
		if (parts.length != 2) {
			throw new Error("Error in line: " + lineN);
		}
		String number = parts[0];
		Matcher m = headings[level - 1].matcher(number);
		if (!m.matches()) {
			throw new Error("Malformed Heading in line: " + lineN);
		}
		for (int j = 0; j < headingCounter.length; j++) {
			String group = j < level ? m.group(j + 1) : null;
			if (j < level - 1) {
				if (!headingCounter[j].equals(group)) {
					throw new Error("Invalid numbering in line: " + lineN
							+ " got " + group + " expected "
							+ headingCounter[j]);
				}
			} else if (j == level - 1) {
				if (!isAllowedAfter(headingCounter[j], group)) {
					throw new Error("Invalid numbering in line: " + lineN
							+ " got " + group
							+ " expected a possible successor of "
							+ headingCounter[j]);
				}
				headingCounter[j] = group;
			} else {
				headingCounter[j] = null;
			}
		}
		out.emitHeading(level, content, parts[0]);
		return content;

	}
	private boolean isAllowedAfter(String before, String after) {
		int[] beforeI = parseParticle(before);
		int[] afterI = parseParticle(after);
		if (beforeI[0] != afterI[0]) {
			if (afterI[1] != -1) {
				return false;
			}
			if (beforeI[0] == -1) {
				if (afterI[0] == 0 || afterI[0] == 1) {
					return true;
				}
			} else {
				if (afterI[0] == beforeI[0] + 1) {
					return true;
				}
			}
			return false;
		} else {
			return beforeI[1] + 1 == afterI[1];
		}
	}
	private int[] parseParticle(String particle) {
		if (particle == null) {
			return new int[]{-1, -1};
		}
		int rest = -1;
		if (!Character.isDigit(particle.charAt(particle.length() - 1))) {
			rest = particle.charAt(particle.length() - 1) - 'a';
			particle = particle.substring(0, particle.length() - 1);
		}
		return new int[]{Integer.parseInt(particle), rest};
	}

}
