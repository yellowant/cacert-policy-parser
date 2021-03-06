package org.cacert.policy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cacert.policy.HTMLSynthesizer.Link;

public class CODListGenerator {
	PolicyTarget target;
	private PolicyGenerator generator;
	public CODListGenerator(PolicyGenerator generator, PolicyTarget target,
			File targetPath) {
		this.generator = generator;
		this.target = target;
		List<COD> cods = new ArrayList<>(generator.getCODs());

		HashMap<String, String> comments = new HashMap<>();
		StringBuffer content = new StringBuffer();
		String line;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(targetPath, "CODList.txt")),
				"UTF-8"))) {
			boolean inHeader = true;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("Comment-") && inHeader) {
					String[] parts = line.substring(8).split(": ", 2);
					comments.put(parts[0], parts[1]);
				}
				if (line.isEmpty() && inHeader) {
					inHeader = false;
				}
				if (!inHeader) {
					content.append(line);
					content.append('\n');
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		target.emitHeading(1, "1 Introduction", "1");
		PolicyParser pp = new PolicyParser(generator, target);
		pp.parse(content.toString(), 0);

		target.emitHeading(1, "2 List of Documents", "2");
		emitCODTable(target, cods, comments);
		target.close();
	}
	private void emitCODTable(PolicyTarget target, List<COD> cods,
			HashMap<String, String> comments) {
		target.startTable("codList");
		target.emitTableCell("#");
		target.emitTableCell("Abbrev");
		target.emitTableCell("official Link");
		target.emitTableCell("Editor");
		target.emitTableCell("Since");
		target.newTableRow();
		target.emitTableCell("Status");
		target.emitTableCell("Name");
		target.emitTableCell("comment");
		target.emitTableCell("");
		target.emitTableCell("last Update");
		for (COD cod : cods) {
			try {
				cod.emitCODIndexLines(target, comments);
			} catch (Exception ex) {
				generator.reportError("WARNING: Problem with index for "
						+ cod.getId());
			}
		}
		target.endTable();
	}
	public static void generateIndexDocument(PolicyGenerator entityResolver,
			File targetPath) throws IOException {
		new CODListGenerator(entityResolver, new HTMLSynthesizer(
				entityResolver, new PrintWriter(new OutputStreamWriter(
						new FileOutputStream(new File(targetPath,
								"policy/index.html")), "UTF-8")), new COD(
						"CDL", "Controlled Document List", "", "", "POLICY",
						new LinkedList<Link>(), null) {
					@Override
					public void printHeader(PolicyGenerator resolver,
							PrintWriter out) {
						emitBigTitle(out);
					}
				}), targetPath);
	}
}
