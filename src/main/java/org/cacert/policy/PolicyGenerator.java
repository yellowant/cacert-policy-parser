package org.cacert.policy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.cacert.policy.HTMLSynthesizer.Link;

public class PolicyGenerator {
	public PolicyGenerator(String templateDoc, File target, COD doc,
			int headerLen) throws IOException {
		PolicyTarget out = new HTMLSynthesizer(new PrintWriter(
				new OutputStreamWriter(new FileOutputStream(target), "UTF-8")),
				doc);
		final PolicyParser parser = new PolicyParser(out);
		parser.parse(templateDoc, headerLen);
		out.close();
	}
	private static Map<String, Entity> cods;
	private static Logger LOG = Logger.getLogger(PolicyGenerator.class
			.getCanonicalName());

	public static String REAL_LINK_PREFIX = "//policy.cacert.org/";

	private static File TARGET_PATH = new File(".");

	private static boolean LOG_TO_FILE = false;

	private static String[] single = null;

	public static void main(String[] args) throws IOException {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--prefix") && i + 1 < args.length) {
				REAL_LINK_PREFIX = args[++i];
			} else if (args[i].equals("--logToFile")) {
				LOG_TO_FILE = true;
			} else if (args[i].equals("--targetPath") && i + 1 < args.length) {
				TARGET_PATH = new File(args[++i]);
			} else if (args[i].equals("--convert") && i + 1 < args.length) {
				single = new String[]{args[++i]};
			} else if (args[i].equals("--convertTo") && i + 2 < args.length) {
				single = new String[]{args[++i], args[++i]};
			}
		}
		try {
			initEntities();
			if (LOG_TO_FILE) {
				PrintStream ps = new PrintStream(new File(TARGET_PATH,
						"output-file.txt"));
				System.setOut(ps);
				System.setErr(ps);
			}

			if (single != null) {
				if (single.length == 1) {
					convert(single[0]);
				} else if (single.length == 2) {
					convert(single[0], single[1]);
				} else {
					System.err.println("Warning: Wrong arguments syntax");
				}
			} else {
				convertAllPolicies();
			}

		} catch (AssertionError ae) {
			LOG.severe(String.format("unexpected runtime condition: %s",
					ae.getMessage()));
		}
	}
	private static void convertAllPolicies() throws IOException {
		new File(TARGET_PATH, "policy").mkdir();
		CODListGenerator.generateIndexDocument(TARGET_PATH);

		//get file information from file
		FileInputStream fstream;
		fstream = new FileInputStream("index-def.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream,
				"UTF-8"));
		String strLine;

		while ((strLine = br.readLine()) != null) {
			String[] index = strLine.split(",");
			if (index.length == 1) {
				convert(index[0].trim());
			} else if (index.length == 2) {
				convert(index[0].trim(), index[1].trim());
			}
		}

		br.close();
	}
	public static void initEntities() {
		if (cods != null) {
			return;
		}
		File policyDir = new File(TARGET_PATH, "policyText");
		if (!policyDir.isDirectory()) {
			throw new AssertionError(
					"no directory policyText found, probably started from the wrong directory.");
		}
		File[] policies = policyDir.listFiles();
		HashMap<String, Entity> codsm = new HashMap<>();
		for (File policy : policies) {
			try {
				if (policy.isDirectory()) {
					for (File subpolicy : policy.listFiles()) {
						BufferedReader br = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										subpolicy), "UTF-8"));

						COD doc = parseHeader(br);
						String pref = "";
						if (policy.getName().equals("OAP")) {
							pref = "OAP-";
							if (!doc.getId().startsWith("11.")) {// OAP COD
																	// number
								System.err
										.println("Policy with wrong COD reference "
												+ policy.getName());
							}
						} else if (policy.getName().equals("AP")) {
							Integer.parseInt(doc.getId().substring(3));
							if (!doc.getId().startsWith("13.")) {// AP COD
								// number
								System.err
										.println("Policy with wrong COD reference "
												+ policy.getName());
							}
						}
						if (!(doc.getAbbrev() + ".txt").equals(pref
								+ subpolicy.getName())) {
							System.err.println("Policy in wrong file: in "
									+ subpolicy.getName() + " is "
									+ doc.getAbbrev());
						}
						codsm.put(doc.getAbbrev(), doc);
					}
				} else {
					BufferedReader br = new BufferedReader(
							new InputStreamReader(new FileInputStream(policy),
									"UTF-8"));

					COD doc = parseHeader(br);
					Integer.parseInt(doc.getId());
					if (!(doc.getAbbrev() + ".txt").equals(policy.getName())) {
						System.err.println("Policy in wrong file: in "
								+ policy.getName() + " is " + doc.getAbbrev());
					}
					codsm.put(doc.getAbbrev(), doc);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}

		}
		try (BufferedReader addEntities = new BufferedReader(new FileReader(
				new File(TARGET_PATH, "entities.txt")))) {
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
	public static Map<String, Entity> getEntities() {
		return cods;
	}

	public static List<COD> getCODs() {
		LinkedList<COD> c = new LinkedList<>();
		for (Entity e : cods.values()) {
			if (e instanceof COD) {
				c.add((COD) e);
			}
		}
		return Collections.unmodifiableList(c);
	}

	public static COD parseHeader(BufferedReader br) throws IOException, Error {
		String id = null;
		String abbrev = null, name = null, link = null, status = null;
		LinkedList<Link> changes = new LinkedList<>();
		Link editor = null;

		String line;
		while ((line = br.readLine()) != null) {
			if (line.isEmpty()) {
				break;
			}
			String[] parts = line.split(": ", 2);
			if (parts.length != 2) {
				throw new Error("Invalid CODList line");
			}
			parts[0] = parts[0].toLowerCase();
			switch (parts[0]) {
				case "id" :
					id = parts[1];
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
		COD fixedLink = new COD(abbrev, name, id, link, status, changes, editor);
		return fixedLink;
	}
	private static void convert(String name) throws IOException {
		convert(name, name);
	}
	private static void convert(String path, String name) throws IOException {
		Reader r = new InputStreamReader(new FileInputStream(new File(
				TARGET_PATH, "policyText/" + path + ".txt")), "UTF-8");
		System.out.println("Converting: " + path);
		StringBuffer buf = new StringBuffer();
		char[] buffer = new char[4096];
		int len;
		while ((len = r.read(buffer)) > 0) {
			buf.append(buffer, 0, len);
		}
		int firstEmptyLine = buf.indexOf("\n\n");
		int count = 3;
		for (int i = 0; i < firstEmptyLine; i++) {
			if (buf.charAt(i) == '\n') {
				count++;
			}
		}
		buf.delete(0, firstEmptyLine + 2);
		String document = buf.toString();
		File target = new File(TARGET_PATH, "policy/" + path + ".html");
		target.getAbsoluteFile().getParentFile().mkdirs();
		new PolicyGenerator(document, target, (COD) PolicyGenerator
				.getEntities().get(name), count);
		r.close();
	}
}
