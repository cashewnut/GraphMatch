package com.fudanse.graphmatch.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;

public class FileUtil {

	public static CompilationUnit openCU(String filePath) {
		CompilationUnit cu = null;

		FileInputStream in = null;
		try {
			in = new FileInputStream(filePath);
			cu = JavaParser.parse(in); // 解析为语法树
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cu;
	}

	public static CompilationUnit openCU(File file) {
		CompilationUnit cu = null;

		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			cu = JavaParser.parse(in); // 解析为语法树
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cu;
	}

	public static List<File> getJavaFiles(File file) {
		List<File> javaFiles = new ArrayList<>();
		if (!file.exists())
			return null;
		if (!file.isDirectory())
			javaFiles.add(file);
		else {
			File[] files = file.listFiles();
			for (File f : files) {
				if (f.isDirectory())
					javaFiles.addAll(getJavaFiles(f));
				else if (f.getName().length() > 5 && f.getName().substring(f.getName().length() - 5).equals(".java")
						&& except(f.getName()))
					javaFiles.add(f);
			}
		}
		return javaFiles;
	}

	private static boolean except(String fileName) {
		if (fileName.equals("R.java") || fileName.equals("BuildConfig.java") || fileName.startsWith(".")
				|| fileName.startsWith("_") || fileName.startsWith("Test") || fileName.endsWith("Test.java"))
			return false;
		return true;
	}
}
