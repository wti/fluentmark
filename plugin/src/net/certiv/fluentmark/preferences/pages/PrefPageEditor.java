/*******************************************************************************
 * Copyright (c) 2016 - 2017 Certiv Analytics and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package net.certiv.fluentmark.preferences.pages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;

import net.certiv.fluentmark.FluentUI;
import net.certiv.fluentmark.preferences.BaseFieldEditorPreferencePage;
import net.certiv.fluentmark.preferences.Prefs;
import net.certiv.fluentmark.util.SwtUtil;

public class PrefPageEditor extends BaseFieldEditorPreferencePage implements Prefs {

	private Composite baseComp;
	private Composite viewComp;
	private Composite taskComp;

	public PrefPageEditor() {
		super(GRID);
		setDescription("");
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(FluentUI.getDefault().getPreferenceStore());
	}

	/** Create fields controlling editing assist behavior */
	@Override
	public void createFieldEditors() {

		baseComp = SwtUtil.makeGroupComposite(getFieldEditorParent(), 1, 1, "Editor");

		// Lines
		addField(new BooleanFieldEditor(EDITOR_SMART_BACKSPACE, "Smart backspace", baseComp));
		addField(new BooleanFieldEditor(EDITOR_WORD_WRAP, "Soft word wrapping", baseComp));
		addField(new BooleanFieldEditor(EDITOR_HTML_OPEN, "Open HTML file after creation", baseComp));
		addField(new BooleanFieldEditor(EDITOR_PDF_OPEN, "Open PDF file after creation", baseComp));
		addField(new IntegerFieldEditor(EDITOR_TAB_WIDTH, "Tab size: ", baseComp, 6));

		addField(new BooleanFieldEditor(EDITOR_MATH_INLINE, "Inline math", baseComp));
		addField(new BooleanFieldEditor(EDITOR_FORMATTING_ENABLED, "Formatting enabled", baseComp));
		addField(new IntegerFieldEditor(EDITOR_FORMATTING_COLUMN, "Formatted line width: ", baseComp, 6));

		// ------

		taskComp = SwtUtil.makeGroupComposite(getFieldEditorParent(), 1, 1, "Tasks");

		// Tasks
		addField(new BooleanFieldEditor(EDITOR_TASK_TAGS, "Use task tags ", taskComp));
		addField(new StringFieldEditor(EDITOR_TASK_TAGS_DEFINED, "Task tags defined:", taskComp));
		viewComp = SwtUtil.makeGroupComposite(getFieldEditorParent(), 1, 1, "Preview");

		addField(new IntegerFieldEditor(VIEW_UPDATE_DELAY, "Update rate limiter period (ms): ", viewComp, 6));
		((GridData) viewComp.getLayoutData()).grabExcessHorizontalSpace = false;
	}

	@Override
	protected void adjustSubLayout() {
		adjust(baseComp, 1);
		adjust(taskComp, 1);
		adjust(viewComp, 1);
	}
}
