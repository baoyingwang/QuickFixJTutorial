package qfjtutorial.util;

import java.io.File;
import java.text.DecimalFormat;

import org.quickfixj.codegenerator.MessageCodeGenerator;
//http://www.quickfixj.org/quickfixj/javadoc/1.6.1/org/quickfixj/codegenerator/MessageCodeGenerator.Task.html
import org.quickfixj.codegenerator.MessageCodeGenerator.Task;

//http://www.quickfixj.org/quickfixj/usermanual/1.6.4/usage/codegen.html
public class CodeGenerator {

	private static final String BIGDECIMAL_TYPE_OPTION = "generator.decimal";
	private static final String ORDERED_FIELDS_OPTION = "generator.orderedFields";
	private static final String OVERWRITE_OPTION = "generator.overwrite";

	private static boolean getOption(String key, boolean defaultValue) {
		return System.getProperties().containsKey(key) ? Boolean.getBoolean(key) : defaultValue;
	}

	public static void main(String... args) {

		// this is the source code from github. I revise it a little bit

		// I changed the structure of output, comparing the default package
		// output messages
		// Because, I hope the QFJ app support multi versions at same time. If
		// we use the default dictionary classes, the field definition is
		// conflicted(all under package - quickfix.field).
		String packageName = "mycompany.";

		String specDir ="C:/baoying.wang/ws/gitnas/QFJTutorial/src/main/resources/dictionary";
		String xformDir="C:/no_synced/tmp/qfj.xformDir";
		String outputBaseDir="C:/no_synced/tmp/qfj.outputBaseDir";
		args = new String[]{specDir,xformDir,outputBaseDir};
		MessageCodeGenerator codeGenerator = new MessageCodeGenerator();
		try {
			if (args.length != 3) {
				String classname = MessageCodeGenerator.class.getName();
				System.err.println("usage: " + classname + " specDir xformDir outputBaseDir");
				return;
			}

			boolean overwrite = getOption(OVERWRITE_OPTION, true);
			boolean orderedFields = getOption(ORDERED_FIELDS_OPTION, false);
			boolean useDecimal = getOption(BIGDECIMAL_TYPE_OPTION, false);

			long start = System.currentTimeMillis();
			final String[] versions = { "FIXT 1.1", "FIX 5.0 SP1", "FIX 5.0 SP2", "FIX 4.4", "FIX 4.2" };
			for (String ver : versions) {
				Task task = new Task();
				task.setName(ver);
				final String version = ver.replaceAll("[ .]", "");
				
				String dictionaryFile=args[0] + "/" + version + ".xml";
				
				task.setSpecification(new File(dictionaryFile));
				task.setTransformDirectory(new File(args[1]));
				task.setMessagePackage(packageName + version.toLowerCase());
				task.setOutputBaseDirectory(new File(args[2]));
				task.setFieldPackage(packageName + version.toLowerCase() + ".field");// the
																						// github
																						// does
																						// not
																						// has
																						// the
																						// version
				task.setOverwrite(overwrite);
				task.setOrderedFields(orderedFields);
				task.setDecimalGenerated(useDecimal);
				codeGenerator.generate(task);
			}
			double duration = System.currentTimeMillis() - start;
			DecimalFormat durationFormat = new DecimalFormat("#.###");
			System.out.println("Time for generation: "
			 + durationFormat.format(duration / 1000L) + " seconds");
		} catch (Exception e) {
			System.err.println("error during code generation :"+ e.toString());
			e.printStackTrace(System.err);
			System.exit(1);
		}

	}
}
