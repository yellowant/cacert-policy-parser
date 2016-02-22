package org.cacert.policy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.cacert.policy.HTMLSynthesizer.Link;

public class PolicyGenerator {
	private File targetPath;
	private PrintStream warningStream;

	public PolicyGenerator(File targetPath, PrintStream warningStream)
			throws IOException {
		this.targetPath = targetPath;
		this.warningStream = warningStream;
		initEntities();
	}
	private void generate(String templateDoc, PrintWriter tgt, COD doc,
			int headerLen) throws UnsupportedEncodingException,
			FileNotFoundException {
		PolicyTarget out = new HTMLSynthesizer(this, tgt, doc);
		PolicyParser parser = new PolicyParser(this, out);
		parser.parse(templateDoc, headerLen);
		out.close();
	}
	private Map<String, Entity> cods;
	private static Logger LOG = Logger.getLogger(PolicyGenerator.class
			.getCanonicalName());

	public static String REAL_LINK_PREFIX = "//policy.cacert.org/";

	public static void main(String[] args) throws IOException {
		File targetPath = new File(".");

		boolean logToFile = false;

		String[] single = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--prefix") && i + 1 < args.length) {
				REAL_LINK_PREFIX = args[++i];
			} else if (args[i].equals("--logToFile")) {
				logToFile = true;
			} else if (args[i].equals("--targetPath") && i + 1 < args.length) {
				targetPath = new File(args[++i]);
			} else if (args[i].equals("--convert") && i + 1 < args.length) {
				single = new String[]{args[++i]};
			} else if (args[i].equals("--convertTo") && i + 2 < args.length) {
				single = new String[]{args[++i], args[++i]};
			}
		}
		try {
			PolicyGenerator pg = new PolicyGenerator(targetPath, logToFile
					? new PrintStream(new File(targetPath, "output-file.txt"))
					: System.out);

			if (single != null) {
				if (single.length == 1) {
					pg.convert(single[0]);
				} else if (single.length == 2) {
					pg.convert(single[0], single[1]);
				} else {
					System.err.println("Warning: Wrong arguments syntax");
				}
			} else {
				pg.convertAllPolicies();
			}

		} catch (AssertionError ae) {
			LOG.severe(String.format("unexpected runtime condition: %s",
					ae.getMessage()));
		}
	}
	private void convertAllPolicies() throws IOException {
		new File(targetPath, "policy").mkdir();
		CODListGenerator.generateIndexDocument(this, targetPath);

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
	public void initEntities() {
		if (cods != null) {
			return;
		}
		File policyDir = new File(targetPath, "policyText");
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
								reportError("Policy with wrong COD reference "
										+ policy.getName());
							}
						} else if (policy.getName().equals("AP")) {
							Integer.parseInt(doc.getId().substring(3));
							if (!doc.getId().startsWith("13.")) {// AP COD
								// number
								reportError("Policy with wrong COD reference "
										+ policy.getName());
							}
						}
						if (!(doc.getAbbrev() + ".txt").equals(pref
								+ subpolicy.getName())) {
							reportError("Policy in wrong file: in "
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
						reportError("Policy in wrong file: in "
								+ policy.getName() + " is " + doc.getAbbrev());
					}
					codsm.put(doc.getAbbrev(), doc);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}

		}
		try (BufferedReader addEntities = new BufferedReader(new FileReader(
				new File(targetPath, "entities.txt")))) {
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
	public Map<String, Entity> getEntities() {
		return cods;
	}

	public List<COD> getCODs() {
		LinkedList<COD> c = new LinkedList<>();
		for (Entity e : cods.values()) {
			if (e instanceof COD) {
				c.add((COD) e);
			}
		}
		return Collections.unmodifiableList(c);
	}

	private static COD parseHeader(BufferedReader br) throws IOException, Error {
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
	private void convert(String name) throws IOException {
		convert(name, name);
	}
	private void convert(String path, String name) throws IOException {
		Reader r = new InputStreamReader(new FileInputStream(new File(
				targetPath, "policyText/" + path + ".txt")), "UTF-8");
		reportError("Converting: " + path);
		File target = new File(targetPath, "policy/" + path + ".html");
		target.getAbsoluteFile().getParentFile().mkdirs();
		PrintWriter tgt = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(target), "UTF-8"));

		doConversion(name, r, tgt);
	}
	public void doConversion(String name, Reader r, PrintWriter tgt)
			throws IOException {
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
		generate(document, tgt, (COD) getEntities().get(name), count);
		r.close();
	}
	public void reportError(String string) {
		warningStream.println(string);
	}
}
