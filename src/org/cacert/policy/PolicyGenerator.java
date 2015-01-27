package org.cacert.policy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.policy.HTMLSynthesizer.Link;

public class PolicyGenerator {
	public PolicyGenerator(String templateDoc, String target, String name)
			throws IOException {
		PolicyTarget out = new HTMLSynthesizer(new PrintWriter(target), name);
		PolicyParser parser = new PolicyParser(out);
		parser.parse(templateDoc);
	}
	private static Map<String, Entity> cods;

	public static void main(String[] args) throws IOException {
		initEntities();
		convert("AP");
		convert("CCA");
		convert("CCS");
	}
	public static void initEntities() {
		if (cods != null) {
			return;
		}
		File[] policies = new File("policyText").listFiles();
		HashMap<String, Entity> codsm = new HashMap<>();
		for (File policy : policies) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						new FileInputStream(policy), "UTF-8"));

				COD doc = parseHeader(br);
				if (!(doc.getAbbrev() + ".txt").equals(policy.getName())) {
					System.err.println("Policy in wrong file: in "
							+ policy.getName() + " is " + doc.getAbbrev());
				}
				codsm.put(doc.getAbbrev(), doc);
			} catch (Throwable t) {
				t.printStackTrace();
			}

		}
		try (BufferedReader addEntities = new BufferedReader(new FileReader(
				"entities.txt"))) {
			String line;
			while ((line = addEntities.readLine()) != null) {
				String[] parts = line.split(";", 3);
				Entity entity = new Entity(parts[0], parts[1], parts[2]);
				codsm.put(entity.getAbbrev(), entity);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		cods = Collections.unmodifiableMap(codsm);
	}
	public static Map<String, Entity> getCODs() {
		return cods;
	}

	public static COD parseHeader(BufferedReader br) throws IOException, Error {
		int id = -1;
		String abbrev = null, name = null, link = null, comment = null, status = null;
		LinkedList<Link> changes = new LinkedList<>();
		Link editor = null;

		String line;
		while ((line = br.readLine()) != null) {
			if (line.isEmpty()) {
				break;
			}
			String[] parts = line.split("=", 2);
			if (parts.length != 2) {
				throw new Error("Invalid CODList line");
			}
			switch (parts[0]) {
				case "id" :
					id = Integer.parseInt(parts[1]);
					break;
				case "abbrev" :
					abbrev = parts[1];
					break;
				case "name" :
					name = parts[1];
					break;
				case "link" :
					link = parts[1];
				case "status" :
					status = parts[1];
					break;
				case "change" :
					changes.add(new Link(parts[1]));
					break;
				case "editor" :
					editor = new Link(parts[1]);
					break;
				default :
					throw new Error("malformed line: " + line);

			}
		}
		COD fixedLink = new COD(abbrev, name, id, link, comment, status,
				changes, editor);
		return fixedLink;
	}
	private static void convert(String name) throws IOException {
		Reader r = new InputStreamReader(new FileInputStream(new File(
				"policyText/" + name + ".txt")), "UTF-8");
		StringBuffer buf = new StringBuffer();
		char[] buffer = new char[4096];
		int len;
		while ((len = r.read(buffer)) > 0) {
			buf.append(buffer, 0, len);
		}
		int firstEmptyLine = buf.indexOf("\n\n");
		buf.delete(0, firstEmptyLine + 2);
		String document = buf.toString();
		new PolicyGenerator(document, name + ".html", name);
		r.close();
	}
}
