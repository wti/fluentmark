/*******************************************************************************
 * Copyright (c) 2016 - 2017 Certiv Analytics and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package net.certiv.fluentmark.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;

import net.certiv.fluentmark.FluentUI;
import net.certiv.fluentmark.editor.Partitions;
import net.certiv.fluentmark.preferences.Prefs;
import net.certiv.fluentmark.preferences.pages.PrefPageEditor;
import net.certiv.fluentmark.util.Cmd;

public class Converter {

	private static final Pattern DOTBEG = Pattern.compile("(~~~+|```+)\\s*dot\\s*", Pattern.DOTALL);
	private static final Pattern DOTEND = Pattern.compile("(~~~+|```+)\\s*", Pattern.DOTALL);
	private IPreferenceStore store;

	public Converter() {
		super();
		store = FluentUI.getDefault().getPreferenceStore();
	}

	public boolean useMathJax() {
		switch (store.getString(Prefs.EDITOR_MD_CONVERTER)) {
			case Prefs.KEY_PANDOC:
				return store.getBoolean(Prefs.EDITOR_PANDOC_MATHJAX);
			default:
				return false;
		}
	}

	public String convert(String basepath, IDocument doc, ITypedRegion[] regions) {
		String text;
		switch (store.getString(Prefs.EDITOR_MD_CONVERTER)) {
			case Prefs.KEY_PANDOC:
				text = getText(doc, regions, true);
				return usePandoc(basepath, text);
			case Prefs.KEY_BLACKFRIDAY:
				text = getText(doc, regions, false);
				return useBlackFriday(basepath, text);
			case Prefs.EDITOR_EXTERNAL_COMMAND:
				text = getText(doc, regions, false);
				return useExternal(basepath, text);
		}
		return "";
	}

	// Use Pandoc
	private String usePandoc(String basepath, String text) {
		String cmd = store.getString(Prefs.EDITOR_PANDOC_PROGRAM);
		if (cmd.trim().isEmpty()) return "";

		List<String> args = new ArrayList<>();
		args.add(cmd);
		args.add("--no-highlight"); // use highlightjs instead
		if (store.getBoolean(Prefs.EDITOR_PANDOC_ADDTOC)) args.add("--toc");
		if (store.getBoolean(Prefs.EDITOR_PANDOC_MATHJAX)) args.add("--mathjax");
		if (!store.getBoolean(Prefs.EDITOR_PANDOC_SMART)) {
			args.add("-f");
			args.add("markdown-smart");
		} else {
			args.add("--ascii");
		}
		return Cmd.process(args.toArray(new String[args.size()]), basepath, text);
	}

	// Use BlackFriday
	private String useBlackFriday(String basepath, String text) {
		String cmd = store.getString(Prefs.EDITOR_BLACKFRIDAY_PROGRAM);
		if (cmd.trim().isEmpty()) return "";

		List<String> args = new ArrayList<>();
		args.add(cmd);
		if (store.getBoolean(Prefs.EDITOR_BLACKFRIDAY_ADDTOC)) {
			args.add("-toc");
		}
		if (store.getBoolean(Prefs.EDITOR_BLACKFRIDAY_SMART)) {
			args.add("-smartypants");
			args.add("-fractions");
		}

		return Cmd.process(args.toArray(new String[args.size()]), basepath, text);
	}

	// Use external command
	private String useExternal(String basepath, String text) {
		String cmd = store.getString(PrefPageEditor.EDITOR_EXTERNAL_COMMAND);
		if (cmd.trim().isEmpty()) {
			return "Specify an external markdown converter command in preferences.";
		}

		String[] args = Cmd.parse(cmd);
		if (args.length > 0) {
			return Cmd.process(args, basepath, text);
		}
		return "";
	}

	private String getText(IDocument doc, ITypedRegion[] regions, boolean inclFM) {
		List<String> parts = new ArrayList<>();
		for (ITypedRegion region : regions) {
			String text = null;
			try {
				text = doc.get(region.getOffset(), region.getLength());
			} catch (BadLocationException e) {
				continue;
			}
			switch (region.getType()) {
				case Partitions.FRONT_MATTER:
					if (!inclFM) continue;
					break;
				case Partitions.DOTBLOCK:
					if (store.getBoolean(Prefs.EDITOR_DOTMODE_ENABLED)) {
						text = filter(text, DOTBEG, DOTEND);
						text = DotGen.runDot(text);
					}
					break;
				default:
					break;
			}
			parts.add(text);
		}

		return String.join(" ", parts);
	}

	private String filter(String text, Pattern beg, Pattern end) {
		String result = text;
		if (beg != null) {
			result = beg.matcher(result).replaceFirst("");
		}
		if (end != null) {
			Matcher m = end.matcher(result);
			int idx = -1;
			while (m.find()) {
				idx = m.start();
			}
			if (idx > -1) {
				result = result.substring(0, idx);
			}
		}
		return result;
	}
}
