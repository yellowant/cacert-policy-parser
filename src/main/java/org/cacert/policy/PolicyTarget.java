package org.cacert.policy;

import org.cacert.policy.HTMLSynthesizer.Link;

/**
 * This is the Interface where policy documents can be parsed to.
 */
public interface PolicyTarget {

	/**
	 * A heading is to be processed.
	 * 
	 * @param order
	 *            the level of which the heading is (e.g. 1 for most important,
	 *            than 2, ... etc).
	 * @param content
	 *            the text content of the heading
	 * @param id
	 *            the identifier, describing this heading's position (e.g.
	 *            <code>1.2a.3</code>)
	 */
	public abstract void emitHeading(int order, String content, String id);

	/**
	 * Output text.
	 * 
	 * @param content
	 *            the text to output.
	 */
	public abstract void emitContent(String content);

	/**
	 * Outputs a in-paragraph linebreak.
	 */
	public abstract void emitLineBreak();

	/**
	 * A paragraph has ended. If there had been {@link #emitContent(String)}
	 * before, a call to this method closes the paragraph and starts a new one.
	 * Note that calls to other functions (e.g.
	 * {@link #emitHeading(int, String, String)},
	 * {@link #emitOrderedListItem(String)}, and others) can also end
	 * paragraphs.
	 */
	public abstract void endParagraph();

	/**
	 * Output an item of an unordered list. If no other calls have been made in
	 * between, successive calls to this function describe the same list. Note
	 * that {@link #endParagraph()} might separate two lists.
	 * 
	 * @param content
	 *            the content of the list item
	 * @param lvl
	 *            the level in which the item is to be displayed
	 */
	public abstract void emitUnorderedListItem(String content, int lvl);

	/**
	 * Output an item of an ordered (numbered) list. Note that this class needs
	 * keep reference of the number, the new item got assigned.
	 * 
	 * @param content
	 *            The text that is in this list item.
	 * @param lvl
	 *            The level of the item (1 of top level and incresing)
	 */
	public abstract void emitOrderedListItem(String content, int lvl);

	/**
	 * Output an item that describes a key.
	 * 
	 * @param content
	 *            The text that is in this list item.
	 * @param lvl
	 *            The level of the item (1 of top level and incresing)
	 */
	public abstract void emitDescriptionItem(String key, String content, int lvl);

	/**
	 * Begins a table.
	 * 
	 * @param clas
	 *            the class to style the table with.
	 */
	public abstract void startTable(String clas);

	/**
	 * Emits a table cell.
	 * 
	 * @param content
	 *            Its plain content.
	 */
	public abstract void emitTableCell(String content);

	/**
	 * Emits a table cell containing a link, and not plain content(
	 * {@link #emitContent(String)})
	 * 
	 * @param link
	 *            the link to display in the table cell
	 */
	public abstract void emitTableCellLink(Link link);

	/**
	 * Emits a "line break" in table rows.
	 */
	public abstract void newTableRow();

	/**
	 * Ends a table.
	 */
	public abstract void endTable();

	/**
	 * Gets the current counter for numbered list (
	 * {@link #emitOrderedListItem(String, int)})
	 * 
	 * @param lvl
	 *            the level to check the content (1 and increasing)
	 * @return the current counter
	 */
	public abstract int getListCounter(int lvl);

	/**
	 * Ends this target's output. (Emitting table of contents, and others)
	 * 
	 * @return the real content.
	 */
	public abstract String close();

}
