/*******************************************************************************
 * Copyright (c) 2016 - 2017 Certiv Analytics and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package net.certiv.fluentmark.model;

import java.util.Arrays;
import java.util.Stack;

import net.certiv.fluentmark.model.Lines.Line;

public class Headers {

	public static class Header {

		IParent element;
		int level;
		Line line;

		public Header(IParent element) {
			this.element = element; // for header 0, text is null
		}

		public Header(IParent element, Line line) {
			this(element);
			this.line = line;
			this.level = computeLevel(line.text);
		}

		@Override
		public String toString() {
			return "level: " + level;
		}
	}

	private final Stack<Header> headers = new Stack<>();
	private final Header rootHeader;

	public Headers(PageRoot root) {
		// header 0 is the PageRoot
		this.rootHeader = new Header(root);
		headers.push(rootHeader); 
	}

	public void putHeader(IParent current, Line line) {
		headers.push(new Header(current, line));
	}

	public IParent getCurrentParent() {
		Header top = headers.peek();
		return top.element;
	}

	public IParent getEnclosingParent(int level) {
		// TODO why not headers.elementAt(level) if level < headers.size()?
		// this is not thread-safe anyway...
		if (level < 1) level = 1;
		if (level > 6) level = 6;
		while (headers.peek().level >= level) {
			headers.pop(); // TODO really? "get" that removes headers?
		}
		return headers.peek().element;
	}

	public static int computeLevel(String text) {
		int level = 0;
		while (level < text.length() && text.charAt(level) == '#' && level < 6) {
			level++;
		}
		return level;
	}

	public void clear() {
		// slower, but never get empty header
		headers.retainAll(Arrays.asList(rootHeader));
	}

	public void dispose() {
		headers.clear();
		// TODO ensure not used after disposed?
	}
}
