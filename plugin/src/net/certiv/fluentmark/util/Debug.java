package net.certiv.fluentmark.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IStatus;

import net.certiv.fluentmark.FluentUI;
import net.certiv.fluentmark.model.ElementChangedEvent;
import net.certiv.fluentmark.model.IElementChangedListener;
import net.certiv.fluentmark.model.PagePart;

public class Debug {
	public static final boolean ON = Boolean.getBoolean("Debug.ON");

	public static void logInfo(Object... args) {
		log(IStatus.INFO, msg(args));
	}

	public static void logWarn(Object... args) {
		log(IStatus.WARNING, msg(args));
	}

	public static void logError(Object... args) {
		log(IStatus.ERROR, msg(args));
	}

	private static void log(int status, String message) {
		if (ON) {
			FluentUI.log(status, message, (Exception) null);
		}
	}

	public static String msg(Object... args) {
		if (!ON || null == args || 0 == args.length) {
			return "";
		}
		if (1 == args.length && args[0] instanceof String) {
			return (String) args[0];
		}
		StringBuilder sb = new StringBuilder(25 * args.length);
		for (int i = 0; i < args.length; i++) {
			sb.append(args[i]);
			sb.append(' ');
		}
		return sb.toString();
	}

	public static void eventPageRootElementChanged(IElementChangedListener listener, ElementChangedEvent event) {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmm.ss");
			Date date = new Date(System.currentTimeMillis());
			String thread = Thread.currentThread().getName();
			String time = formatter.format(date);
			String m = time + ": FluentUI PageRoot elementChanged\n\tEvent: " //
					+ Debug.str(event) //
					+ "\n\tlistener: " + Debug.toString(listener) //
					+ "\n\tthread: " + thread;
			FluentUI.log(m);
		} catch (Exception | Error e) {
			FluentUI.log("FluentUI Exception logging: " + e.getMessage());
		}

	}

	public static void brEvalInsert(Object editor, Object lineDelim) {
		if (null == editor) {
			Debug.logWarn("EIP: null editor");
		} else if (null == lineDelim) {
			Debug.logWarn("EIP: null delim");
		}
	}

	public static String str(ElementChangedEvent e) {
		// if (source instanceof PageRoot) {
		String typeName = "[" + e.getType() + "]";
		switch (e.getType()) {
		case ElementChangedEvent.POST_CHANGE:
			typeName = "POST_CHANGE";
			break;
		case ElementChangedEvent.POST_RECONCILE:
			typeName = "POST_RECONCILE";
			break;
		}
		String me = toString(e);
		String src = toString(e.getSource());
		Object part = e.getPart();
		String partStr = "" + part;
		if (part instanceof PagePart) {
			partStr = str((PagePart) part);
		}
		return me + "[type=" + typeName + ", part=" + partStr + ", source=" + src + "]";
	}

	public static String str(PagePart part) {
		return "Part[kind=" + part.getKind() + ", outlineContent=" + part.toString() //
				+ ", range=" + part.getSourceRange() //
				+ "]";
	}

	public static String toString(Object o) {
		if (null == o) {
			return "null";
		}
		return shortClassname(o.getClass()) + "@" + Integer.toHexString(o.hashCode());
	}

	public static String shortClassname(Class<?> c) {
		if (null == c) {
			return "nullClass";
		}
		String result = c.getName();
		int loc = result.lastIndexOf('.');
		if (-1 != loc) {
			result = result.substring(loc + 1);
		}
		loc = result.lastIndexOf('$');
		if (-1 != loc) {
			result = result.substring(loc + 1);
		}
		return result;
	}
}
