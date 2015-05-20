package org.cacert.policy;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

public class GenerateDiffs {
	public static void main(String[] args) throws IOException {

		// wdiff -n -w $'\033[30;41m' -x $'\033[0m' -y $'\033[30;42m' -z $'\033[0m' DRP.{old,new} | less -R
		// wdiff -n -w $'<span style=\'background-color:#FFBBBB\'>' -x $'</span>' -y $'<span style=\'background-color: #BBFFBB\'>' -z $'</span>' DRP.{old,new} | sed "s_\$_<br/>\n_"
		File target = new File("old");
		target.mkdirs();
		HashMap<String, String> urls = new HashMap<>();
		urls.put("DRP",
				"https://www.cacert.org/policy/DisputeResolutionPolicy.html");
		urls.put("AP", "https://www.cacert.org/policy/AssurancePolicy.html");
		urls.put("CCA",
				"https://www.cacert.org/policy/CAcertCommunityAgreement.html");
		urls.put("CCS",
				"https://www.cacert.org/policy/ConfigurationControlSpecification.html");
		for (Entry<String, String> entry : urls.entrySet()) {
			URL u = new URL(entry.getValue());
			Reader r = new InputStreamReader(u.openStream());
			clean(r, new FileWriter(new File(target, entry.getKey()
					+ ".old.txt")));
			clean(new FileReader("policy/" + entry.getKey() + ".html"),
					new FileWriter(
							new File(target, entry.getKey() + ".new.txt")));
		}
	}

	/**
	 * Clean the input html so that it's roughly plaintext.
	 * 
	 * This is done by remoing styles,tags, <code>\r</code>'s, double
	 * newlines/spaces, quote-escape sequences
	 * 
	 * @param r
	 *            the input
	 * @param out
	 *            the output
	 * @throws IOException
	 *             if I/O fails
	 */
	private static void clean(Reader r, FileWriter out) throws IOException {
		StringBuffer policy = new StringBuffer();
		char[] buf = new char[1024];
		int len;
		while ((len = r.read(buf)) > 0) {
			policy.append(buf, 0, len);
		}
		String s = policy.toString();
		s = s.replaceAll("<style[^>]+>[^<]*</style>", "");
		s = s.replaceAll("<[^>]+>", "");
		s = s.replaceAll("\\r", "");
		s = s.replaceAll("\n+\\s*", "\n");
		s = s.replaceAll("\n+([^0-9])", " $1");
		s = s.replaceAll(" +", " ");
		s = s.replaceAll("&#39;", "'");
		s = s.replaceAll("&quot;", "\"");
		out.write(s);
		out.close();
	}
}
