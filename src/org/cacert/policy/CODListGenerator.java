package org.cacert.policy;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class CODListGenerator {
	PolicyTarget target;
	public CODListGenerator(PolicyTarget target) {
		this.target = target;
		List<COD> cods = new ArrayList<>(PolicyGenerator.getCODs());
		target.startTable("codList");
		target.emitTableCell("#");
		target.emitTableCell("Abbrev");
		target.emitTableCell("Since");
		target.emitTableCell("Name");
		target.emitTableCell("Editor");
		target.newTableRow();
		target.emitTableCell("");
		target.emitTableCell("official Link");
		target.emitTableCell("last Update");
		target.emitTableCell("comment");
		target.emitTableCell("");

		HashMap<String, String> comments = new HashMap<>();
		Collections.sort(cods, new Comparator<COD>() {

			@Override
			public int compare(COD o1, COD o2) {

				String i1 = o1.getId();
				String i2 = o2.getId();
				if (i1.length() == 1) {
					i1 = "0" + i1;
				}
				if (i2.length() == 1) {
					i2 = "0" + i2;
				}
				return i1.compareTo(i2);
			}
		});
		for (COD cod : cods) {
			cod.emitCODIndexLines(target, comments);
		}
		target.endTable();
		target.close();
	}
	public static void main(String[] args) throws IOException {
		PolicyGenerator.initEntities();
		new CODListGenerator(new HTMLSynthesizer(new PrintWriter("index.html"),
				"CDL"));
	}
}
