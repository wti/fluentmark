package net.certiv.fluentmark.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import net.certiv.fluentmark.model.Lines.Line;
import net.certiv.fluentmark.model.Lines.Lines_TestHook;
import net.certiv.fluentmark.model.Lines.P;

/**
 * TODO P1 move LinesPTest to testing package.
 */
public final class LinesPTest {
	private static final int TEST_SOURCE_COUNT = 52;
	private static final File TEST_SOURCE = new File("test.snippets/patternTests.txt");
	
	public static void main(String[] args) throws Exception {
		new LinesPTest().testPats().testPatsText();
	}
	
	/**
	 * Try API's isType(..) and identifyKind() for (default) TEXT type.
	 */
	public LinesPTest testPatsText() throws Exception {
		Line prior = Lines_TestHook.newLine();
		Line cur = Lines_TestHook.newLine("hello", 1, prior, false);
		Line next = Lines_TestHook.newLine("world", 2, cur, false);
		Type curType = Lines_TestHook.identifyKind(cur, next, false);
		boolean isText = Lines_TestHook.isType(Type.TEXT, cur, next, false, false);
		if (!isText || Type.TEXT != curType) {
			doTestFail("Hello World type=" + curType + (isText ? " but isType" : " and !isType"));
		}
		return this;
	}
	
	/**
	 * Run {@link #TEST_SOURCE_COUNT} tests defined in {@link #TEST_SOURCE}.
	 * @throws Exception
	 */
	public LinesPTest testPats() throws Exception {
		
		AtomicInteger testCount = new AtomicInteger();
		TreeMap<String, PCases> nameToTP = PCaseLoader.load(TEST_SOURCE);
		ArrayList<String> fails = PCases.run(nameToTP.values(), testCount);
		
		if (!fails.isEmpty()) {
			String fail = StringUtils.join(fails.toArray(), "\n");
			doTestFail(fail);
		}
		if (TEST_SOURCE_COUNT != testCount.get()) {
			doTestFail("testCount expected " + TEST_SOURCE_COUNT + ", actual " + testCount);
		}
		return this;
	}
	
	static void doTestFail(String message) {
		throw new RuntimeException(message);
	}
	

	/**
	 * Test cases for P with inputs expected to match or not.
	 */
	static class PCases {
		public static ArrayList<String> run(Iterable<PCases> cases, AtomicInteger countSink) {
			final ArrayList<String> fails = new ArrayList<>();
			int testCount = 0;
			if (null != cases) {
				for (PCases next : cases) {
					boolean[] passfail = {
							true, false
					};
					for (int i = 0; i < passfail.length; i++) {
						for (String test : passfail[i] ? next.expPass : next.expFail) {
							testCount++;
							boolean actual = next.p.is(test);
							if (actual != passfail[i]) {
								String fail = //
										(actual ? "matched" : "! match") + "\t" + //
												next.p.name + "\t" + //
												"\"" + test + "\"";
								fails.add(fail);
							}
						}
					}
				}
			}
			if (null != countSink) {
				countSink.set(testCount);
			}
			return fails;
		}
		
		public final P p;
		public final ArrayList<String> expPass = new ArrayList<>();
		public final ArrayList<String> expFail = new ArrayList<>();

		public PCases(P p) {
			this.p = p;
		}
	}

	/**
	 * Load PCase from file.
	 */
	static class PCaseLoader {
		private static final HashMap<String, P> nameToP = makeNameToP();

		static HashMap<String, P> makeNameToP() {
			final HashMap<String, P> result = new HashMap<>(32);
			try {
				if (!P.all().isEmpty()) {
					for (Field field : P.class.getFields()) {
						if (P.class != field.getType()) {
							continue;
						}
						int mods = field.getModifiers();
						if (Modifier.isStatic(mods) && Modifier.isPublic(mods) && Modifier.isFinal(mods)) {
							P next = (P) field.get(null);
							result.put(field.getName(), next);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}

		/**
		 * Load file where ,-delimited lines are input, "pass" (or not), and p-name. (Note none of the
		 * patterns have comma.).  Extra fields are ignored.
		 * 
		 * @param file File to load
		 * @return TreeMap<String, TP>
		 * @throws IOException
		 */
		static TreeMap<String, PCases> load(File file) throws IOException {
			HashMap<String, P> nameToPMap = nameToP;
			TreeMap<String, PCases> result = new TreeMap<String, PCases>();
			try (BufferedReader r = new BufferedReader(new FileReader(file))) {
				String next;
				while (null != (next = r.readLine())) {
					String[] def = next.split(",");
					if (null != def && 2 < def.length) {
						String test = def[0]; // don't trim input
						boolean pass = "pass".equals(def[1].trim());
						P p = nameToPMap.get(def[2].trim());
						if (null != p && !test.isEmpty()) {
							PCases tp = result.get(p.name);
							if (null == tp) {
								tp = new PCases(p);
								result.put(p.name, tp);
							}
							(pass ? tp.expPass : tp.expFail).add(test);
						}
					} // TODO else report invalid input?
				}
			}
			// Now sort for consistency in order
			for (PCases tp : result.values()) {
				Collections.sort(tp.expFail);
				Collections.sort(tp.expPass);
			}
			return result;
		}
	}
}
