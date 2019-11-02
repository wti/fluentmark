/*******************************************************************************
 * Copyright (c) 2016 - 2017 Certiv Analytics and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package net.certiv.fluentmark.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.certiv.fluentmark.FluentUI;
import net.certiv.fluentmark.preferences.Prefs;
import net.certiv.fluentmark.util.FloorKeyMap;
import net.certiv.fluentmark.util.Indent;

public final class Lines {

	private static boolean isBlank(String line) {
		return null == line || 0 == line.length() || 0 == line.trim().length();
	}

	protected static class Line {

		final String text;
		final int offset;
		final int length;
		final int idx;
		Type kind = Type.UNDEFINED; // effective
		Type nKind = kind; // natural/original
		PagePart part;

		private final boolean blankPrior;
		private final boolean isBlank;
		private final boolean blankNext;

		private Line() {
			blankPrior = true;
			blankNext = true;
			isBlank = true;
			text = "";
			offset = 0;
			length = 0;
			idx = -1;
		}

		Line(String line, int delimLen, Line prior, boolean blankNext) {
			this.text = line;
			this.offset = prior.offset + prior.length;
			this.length = line.length() + delimLen;
			this.idx = prior.idx + 1;

			this.blankPrior = prior.isBlank();
			this.blankNext = blankNext;
			this.isBlank = Lines.isBlank(line);
		}

		@Override
		public String toString() {
			return String.format("%4d %-6.6s/%-6.6s [%5d:%3d] %s", //
					idx, kind.toString(), nKind.toString(), offset, length, text);
		}

		/** Just ws. */
		public boolean isBlank() {
			return isBlank;
		}

		public boolean isBlankPrior() {
			return blankPrior;
		}

		public boolean isBlankNext() {
			return blankNext;
		}
	}

	private static final Line NULL_LINE = new Line();
	private final Line[] lines;
	private FloorKeyMap lineMap;
	private final boolean isMathInline;

	public Lines(String[] lines, String lineDelim, boolean isMathInline) {
		this.lines = new Line[lines.length];
		this.lineMap = new FloorKeyMap(lines.length);
		this.isMathInline = isMathInline;
		Line prior = new Line();
		int delimLen = null == lineDelim ? 0 : lineDelim.length();
		for (int idx = 0; idx < lines.length; idx++) {
			boolean nextIsBlank = idx + 1 == lines.length ? true : isBlank(lines[idx + 1]);
			Line line = new Line(lines[idx], delimLen, prior, nextIsBlank);
			this.lines[idx] = line;
			lineMap.put(line.offset, idx);
			prior = line;
		}
	}

	/** Number of lines in content */
	public int length() {
		return lines.length;
	}

	public FloorKeyMap getOffsetMap() {
		return lineMap;
	}

	public Line getLine(int idx) {
		return getLine0(idx);
	}

	private Line getLine0(int idx) {
		if (0 > idx || idx >= lines.length) {
			return NULL_LINE;
		}
		return lines[idx];
	}

	public int getOffset(int idx) {
		return getLine0(idx).offset;
	}

	public String getText(int idx) {
		return getLine0(idx).text;
	}

	public int getTextLength(int idx) {
		return getLine0(idx).length;
	}

	public Type getKind(int idx) {
		return getLine0(idx).kind;
	}

	public Type getOriginalKind(int idx) {
		return getLine0(idx).nKind;
	}

	public void setKind(int idx, Type kind) {
		Line line = getLine0(idx);
		if (NULL_LINE != line) {
			line.kind = kind;
			if (line.nKind == Type.UNDEFINED) {
				line.nKind = kind; // preserve the original kind
			}
		}
	}

	public PagePart getPagePart(int idx) {
		return getLine0(idx).part;
	}

	public void setPagePart(int idx, PagePart part) {
		Line line = getLine0(idx);
		if (NULL_LINE != line) {
			line.part = part;
		}
	}

	public Type identifyKind(int idx) {
		Line curr = getLine0(idx);
		Line next = getLine0(idx + 1);
		return identifyKind0(curr, next, idx == 0);
	}

	public boolean isType(Type target, int idx) {
		Line curr = getLine0(idx);
		Line next = getLine0(idx + 1);
		return isType0(target, curr, next, idx == 0, isMathInline);
	}

	/**
	 * Avoid recalculating line type by only verifying queried type.
	 * 
	 * @param target Type to check
	 * @param curr Line current
	 * @param next Line following
	 * @param isIdx0 boolean true only for before-first-line
	 * @param isMathInline boolean true to support math pattern that also matches variables
	 * @return true if curr Line is Type target.
	 */
	private static boolean isType0(Type target, Line curr, Line next, boolean isIdx0,
			boolean isMathInline) {
		if (null == target) {
			return false;
		}
		final boolean haveNext = NULL_LINE != next;
		final String curTxt = curr.text;
		final boolean priorblank = curr.isBlankPrior();
		final boolean curblank = curr.isBlank();
		final boolean nxtblank = !haveNext || next.isBlank();
		final boolean nxtnxtblank = !haveNext || next.isBlankNext();
		switch (target) {
		case BLANK:
			return curblank;
		case FRONT_MATTER:
			return isIdx0 && P.prefix3dash.is(curTxt);
		case HEADER:
			if (P.prefix1hash.is(curTxt)) {
				return true;
			}
			String nxtTxt = haveNext ? next.text : "";
			return (!curblank && nxtnxtblank && (P.prefix3dash.is(nxtTxt) || P.prefix3equals.is(nxtTxt)));
		case SETEXT:
			return (!priorblank && nxtblank && (P.prefix3dash.is(curTxt) || P.prefix3equals.is(curTxt)));
		case MATH_BLOCK:
			return isMathInline && P.prefix2dollar.is(curTxt); // TODO $variables look like math...
		case MATH_BLOCK_INLINE:
			return priorblank && nxtblank && P.mathinline.is(curTxt);
		case COMMENT:
			return P.prefixXmlCommentStart.is(curTxt) || P.prefixXmlCommentEnd.is(curTxt);
		case HRULE:
			return priorblank && nxtblank && (P.prefix3Underbar.is(curTxt) || P.prefix3Star.is(curTxt));
		case TABLE:
			return P.anyTable.is(curTxt);
		case LIST:
			return P.prefixUList.is(curTxt) || P.prefixOList.is(curTxt);
		case QUOTE:
			return P.anyQuote.is(curTxt);
		case DEFINITION:
			return P.anyDef.is(curTxt);
		case REFERENCE:
			return P.anyRef.is(curTxt);
		case HTML_BLOCK:
			return P.anyHtmlBlock.is(curTxt);
		case CODE_BLOCK:
			return P.prefixCodeBacktick.is(curTxt) || P.prefixCodeTilde.is(curTxt)
					|| (priorblank && P.prefixCodeStart.is(curTxt))
					|| (nxtblank && P.prefixCodeEnd.is(curTxt));
		case CODE_BLOCK_INDENTED:
			return P.prefixCodeBlock.is(curTxt);
		case TEXT:
			return Type.TEXT == identifyKind0(curr, next, isIdx0); // by exclusion
		default:
			return false;
		}
	}

	private static Type identifyKind0(Line curr, Line next, boolean isIdx0) {
		final boolean haveNext = NULL_LINE != next;

		String curTxt = curr.text;
		String nxtTxt = haveNext ? next.text : "";

		boolean priorblank = curr.isBlankPrior();
		boolean curblank = curr.isBlank();
		boolean nxtblank = haveNext ? next.isBlank() : true;
		boolean nxtnxtblank = haveNext ? next.isBlankNext() : true;

		if (curblank)
			return Type.BLANK;

		if (isIdx0 && P.prefix3dash.is(curTxt))
			return Type.FRONT_MATTER;

		if (P.prefix1hash.is(curTxt))
			return Type.HEADER;
		if (!curblank && nxtnxtblank && (P.prefix3Dash.is(nxtTxt) || P.prefix3equals.is(nxtTxt)))
			return Type.HEADER;
		if (!priorblank && nxtblank && (P.prefix3Dash.is(curTxt) || P.prefix3equals.is(curTxt)))
			return Type.SETEXT;

		if (P.prefix2dollar.is(curTxt))
			return Type.MATH_BLOCK;
		if (priorblank && nxtblank && P.mathinline.is(curTxt))
			return Type.MATH_BLOCK_INLINE;

		if (P.prefixXmlCommentStart.is(curTxt))
			return Type.COMMENT;
		if (P.prefixXmlCommentEnd.is(curTxt))
			return Type.COMMENT;

		if (priorblank && nxtblank && P.prefix3Underbar.is(curTxt))
			return Type.HRULE;
		if (priorblank && nxtblank && P.prefix3Star.is(curTxt))
			return Type.HRULE;
		if (priorblank && nxtblank && P.prefix3Dash.is(curTxt))
			return Type.HRULE;

		if (P.anyTable.is(curTxt))
			return Type.TABLE;

		if (P.prefixUList.is(curTxt))
			return Type.LIST;
		if (P.prefixOList.is(curTxt))
			return Type.LIST;

		if (P.anyQuote.is(curTxt))
			return Type.QUOTE;
		if (P.anyDef.is(curTxt))
			return Type.DEFINITION;
		if (P.anyRef.is(curTxt))
			return Type.REFERENCE;

		if (P.anyHtmlBlock.is(curTxt))
			return Type.HTML_BLOCK;

		if (P.prefixCodeBacktick.is(curTxt))
			return Type.CODE_BLOCK;
		if (P.prefixCodeTilde.is(curTxt))
			return Type.CODE_BLOCK;
		if (P.prefixCodeBlock.is(curTxt))
			return Type.CODE_BLOCK_INDENTED;

		if (priorblank && P.prefixCodeStart.is(curTxt))
			return Type.CODE_BLOCK;
		if (nxtblank && P.prefixCodeEnd.is(curTxt))
			return Type.CODE_BLOCK;

		return Type.TEXT;
	}

	public int nextMatching(int mark, String exact) {
		return nextMatching(mark, null, exact);
	}

	public int nextMatching(int mark, Type kind) {
		return nextMatching(mark, kind, null);
	}

	public int nextMatching(int mark, Type kind, String exact) {
		final int length = length();
		for (int idx = mark + 1; idx < length; idx++) {
			if ((kind == null || isType(kind, idx))
					&& (exact == null || getLine0(idx).text.startsWith(exact))) {
				return idx;
			}
		}
		return length - 1;
	}

	public void clear() {
		lineMap.clear();
	}

	public void dispose() {
		clear();
	}

	public static int computeLevel(String text) {
		int width = FluentUI.getDefault().getPreferenceStore().getInt(Prefs.EDITOR_TAB_WIDTH);
		return Indent.measureIndentInSpaces(text, width);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(lines.length * 64);
		for (Line line : lines) {
			sb.append(line.toString());
		}
		return sb.toString();
	}

	/**
	 * Matching template with name and matching style only compiles pattern once
	 * and supports unit testing.  This does not use Pattern to match exact prefixes.
	 */
	public static class P {
		public static final P prefix3dash = prefixExact("prefix3dash", "---");
		public static final P prefix3equals = prefixExact("prefix3equals", "===");
		public static final P prefix1hash = prefixExact("prefix1hash", "#");
		public static final P prefix2dollar = prefixExact("prefix2dollar", "$$");
		public static final P prefixXmlCommentStart = prefixExact("prefixXmlCommentStart", "<!---");
		public static final P prefixXmlCommentEnd = prefixExact("prefixXmlCommentEnd", "--->");
		public static final P prefix3Underbar = prefixExact("prefix3Underbar", "___");
		public static final P prefix3Star = prefixExact("prefix3Star", "***");
		public static final P prefix3Dash = prefixExact("prefix3Dash", "---");
		public static final P prefixCodeBacktick = prefixExact("prefixCodeBacktick", "```");
		public static final P prefixCodeTilde = prefixExact("prefixCodeTilde", "~~~");
		public static final P prefixCodeStart = prefixExact("prefixCodeStart", "@start");
		public static final P prefixCodeEnd = prefixExact("prefixCodeEnd", "@end");
		public static final P prefixCodeBlock = prefix("prefixCodeBlock", "    .*");
		public static final P prefixUList = prefix("prefixUList", "\\s*[*+-]\\s+.*");
		public static final P prefixOList = prefix("prefixOList", "\\s*\\d+\\.\\s+.*");
		public static final P mathinline = any("mathinline", "\\$\\S.*?\\S\\$.*");
		public static final P anyTable = any("anyTable", "(\\|\\s?\\:?---+\\:?\\s?)+\\|.*");
		public static final P anyQuote = any("anyQuote", "(\\>+\\s+)+.*");
		public static final P anyDef = any("anyDef", "\\:\\s+.*");
		public static final P anyRef = any("anyRef", "\\[\\^?\\d+\\]\\:\\s+.*");
		public static final P anyHtmlBlock = any("anyHtmlBlock", "\\</?\\w+(\\s+.*?)?/?\\>.*");
		private static final List<P> all = loadAll();
		
		static P prefixExact(String name, String ps) {
			return new P(name, ps, true); // uses startsWith(..), not Pattern.matcher(..)
		}

		static P prefix(String name, String ps) {
			return new P(name, "^" + ps, false);
		}

		static P any(String name, String ps) {
			return new P(name, ps, false);
		}


		/**
		 * Called when testing to load all values.
		 * 
		 * @return List
		 *         <P>
		 */
		public static List<P> all() {
			return all;
		}

		private static List<P> loadAll() {
			ArrayList<P> result = new ArrayList<P>(32);
			result.add(prefix3Dash);
			result.add(prefix3equals);
			result.add(prefix1hash);
			result.add(prefix2dollar);
			result.add(mathinline);
			result.add(prefixXmlCommentStart);
			result.add(prefixXmlCommentEnd);
			result.add(prefix3Underbar);
			result.add(prefix3Star);
			result.add(prefix3Dash);
			result.add(anyTable);
			result.add(prefixUList);
			result.add(prefixOList);
			result.add(anyQuote);
			result.add(anyDef);
			result.add(anyRef);
			result.add(anyHtmlBlock);
			result.add(prefixCodeBacktick);
			result.add(prefixCodeTilde);
			result.add(prefixCodeBlock);
			result.add(prefixCodeStart);
			result.add(prefixCodeEnd);
			return Collections.unmodifiableList(result);
		}

		public final String name;
		public final String template;
		public final Pattern p;
		public final boolean startsWith;

		public P(String name, String ps, boolean startsWith) {
			this.name = name;
			this.template = ps;
			this.startsWith = startsWith;
			this.p = startsWith ? null : Pattern.compile(ps);
		}

		public boolean is(String s) {
			if (null == s) {
				return false;
			}
			if (startsWith) {
				return s.startsWith(template);
			}
			Matcher m = p.matcher(s);
			// TODO P4 support m.matches() if complete patterns needed
			return m.find();
		}
	}

	/** Test-only access to internal methods. */
	public static final class Lines_TestHook {
		public static Line newLine() {
			return new Line();
		}

		public static Line newLine(String line, int delimLen, Line prior, boolean blankNext) {
			return new Line(line, delimLen, prior, blankNext);
		}

		public static Type identifyKind(Line curr, Line next, boolean isIdx0) {
			return Lines.identifyKind0(curr, next, isIdx0);
		}

		public static boolean isType(Type target, Line curr, Line next, boolean isIdx0,
				boolean isMathInline) {
			return isType0(target, curr, next, isIdx0, isMathInline);
		}
	}
}
