package org.cacert.policy;

public interface PolicyTarget {

	public abstract void emitHeading(int order, String content, String id);

	public abstract void emitContent(String content);

	public abstract void endParagraph();

	public abstract void emitUnorderedListItem(String content, int lvl);

	public abstract void emitOrderedListItem(String content);

	public abstract void startTable();

	public abstract void emitTableCell(String content);

	public abstract void newTableRow();

	public abstract void endTable();

	public abstract int getListCounter();

	public abstract String close();

}
