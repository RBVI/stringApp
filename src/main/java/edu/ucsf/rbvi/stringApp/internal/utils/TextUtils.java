package edu.ucsf.rbvi.stringApp.internal.utils;

public class TextUtils {
	public static String smartDelimit(String terms) {
		// If we already have newlines, just return
		if (terms.indexOf('\n') > 0)
			return terms;

		// Check for tab-delimited
		if (terms.indexOf('\t') > 0)
			return terms.replace('\t','\n');

		// Check for commas
		if (terms.indexOf(',') > 0)
			return quotedDelimit(terms, ',');

		// Check for semi-colons
		if (terms.indexOf(';') > 0)
			return quotedDelimit(terms, ';');

		// Check for spaces
		if (terms.indexOf(' ') > 0)
			return quotedDelimit(terms, ' ');

		return terms;
	}

	private static String quotedDelimit(String str, char delimiter) {
		StringBuilder sb = new StringBuilder();
		boolean escape = false;
		boolean inquote = false;
		for (char c: str.toCharArray()) {
			switch(c) {
				case '\\':
					if (escape) {
						sb.append(c);
						escape = false;
					} else {
						escape = true;
					}
					break;
				case '"':
					if (escape)
						sb.append(c);
					else {
						if (inquote) 
							inquote = false;
						else
							inquote = true;
					}
					break;
				default:
					if (escape) {
						sb.append(c);
						escape = false;
					} else if (inquote) {
						sb.append(c);
					} else if (c == delimiter) {
						sb.append('\n');
					} else 
						sb.append(c);
			}
		}
		return sb.toString();
	}
}
