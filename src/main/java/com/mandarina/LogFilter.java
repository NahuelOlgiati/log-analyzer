package com.mandarina;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

public class LogFilter {

	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final Pattern dateTimePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}");

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java LogFilter <logFileName> <tempFileName> [<wordsToCheck>]");
			return;
		}

		String logFileName = args[0];
		String tempFileName = args.length == 3 ? args[2] : generateTempFileName(logFileName);

		String[] wordsToCheck = args[1].split(",");

		try {
			filter(logFileName, tempFileName, wordsToCheck);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String generateTempFileName(String logFileName) {
		Path logFilePath = Paths.get(logFileName);
		String tempFileName = logFilePath.getParent().resolve("temp.txt").toString();
		return tempFileName;
	}

	private static void filter(String logFileName, String tempFileName, String[] wordsToCheck) {
		try {
			Path logPath = Paths.get(logFileName);
			Path tempPath = Paths.get(tempFileName);

			try (BufferedReader reader = Files.newBufferedReader(logPath, UTF8);
					BufferedWriter writer = Files.newBufferedWriter(tempPath, UTF8, StandardOpenOption.CREATE,
							StandardOpenOption.TRUNCATE_EXISTING)) {

				StringBuilder stackTrace = new StringBuilder();
				boolean inStackTrace = false;
				String line;

				while ((line = reader.readLine()) != null) {
					inStackTrace = processRead(wordsToCheck, writer, stackTrace, line, inStackTrace);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean processRead(String[] wordsToCheck, BufferedWriter writer, StringBuilder stackTrace,
			String newLine, boolean inStackTrace) throws IOException {
		boolean startWithDateTime = startWithDateTime(newLine);

		if (startWithDateTime) {
			if (inStackTrace) {
				boolean containsWords = containsWords(stackTrace, wordsToCheck);

				if (!containsWords) {
					writer.write(stackTrace.toString());
				}

				inStackTrace = false;
				stackTrace.setLength(0);
			} else {
				inStackTrace = true;
				inStackTrace = conditionalAppend(wordsToCheck, stackTrace, newLine, inStackTrace);
			}
		} else {
			if (inStackTrace) {
				inStackTrace = conditionalAppend(wordsToCheck, stackTrace, newLine, inStackTrace);
			}
		}
		return inStackTrace;
	}

	private static boolean conditionalAppend(String[] wordsToCheck, StringBuilder stackTrace, String newLine,
			boolean inStackTrace) {
		boolean containsWords = containsWords(newLine, wordsToCheck);

		if (!containsWords) {
			stackTrace.append(newLine).append(System.lineSeparator());
		} else {
			inStackTrace = false;
			stackTrace.setLength(0);
		}
		return inStackTrace;
	}

	private static boolean startWithDateTime(String line) {
		return dateTimePattern.matcher(line).find();
	}

	private static boolean containsWords(String line, String[] wordsToCheck) {
		for (String word : wordsToCheck) {
			if (line.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsWords(StringBuilder stackTrace, String[] wordsToCheck) {
		for (String word : wordsToCheck) {
			if (stackTrace.toString().contains(word)) {
				return true;
			}
		}
		return false;
	}
}
