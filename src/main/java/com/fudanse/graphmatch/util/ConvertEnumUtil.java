package com.fudanse.graphmatch.util;

import com.fudanse.graphmatch.enums.EnumAssignOperatorType;
import com.fudanse.graphmatch.enums.EnumBinaryOperatorType;
import com.fudanse.graphmatch.enums.EnumUnaryOperatorType;

import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.UnaryExpr;

public class ConvertEnumUtil {

	/**
	 * 二元操作符的枚举类型转String
	 * @param em
	 * @return String 操作符
	 */
	public static String getBinaryOperator(BinaryExpr.Operator em) {
		switch (em) {
		case or:
			return EnumBinaryOperatorType.or.getValue();
		case and:
			return EnumBinaryOperatorType.and.getValue();
		case binOr:
			return EnumBinaryOperatorType.binOr.getValue();
		case binAnd:
			return EnumBinaryOperatorType.binAnd.getValue();
		case xor:
			return EnumBinaryOperatorType.xor.getValue();
		case equals:
			return EnumBinaryOperatorType.equals.getValue();
		case notEquals:
			return EnumBinaryOperatorType.notEquals.getValue();
		case less:
			return EnumBinaryOperatorType.less.getValue();
		case greater:
			return EnumBinaryOperatorType.greater.getValue();
		case lessEquals:
			return EnumBinaryOperatorType.lessEquals.getValue();
		case greaterEquals:
			return EnumBinaryOperatorType.greaterEquals.getValue();
		case lShift:
			return EnumBinaryOperatorType.lShift.getValue();
		case rSignedShift:
			return EnumBinaryOperatorType.rSignedShift.getValue();
		case rUnsignedShift:
			return EnumBinaryOperatorType.rUnsignedShift.getValue();
		case plus:
			return EnumBinaryOperatorType.plus.getValue();
		case minus:
			return EnumBinaryOperatorType.minus.getValue();
		case times:
			return EnumBinaryOperatorType.times.getValue();
		case divide:
			return EnumBinaryOperatorType.divide.getValue();
		case remainder:
			return EnumBinaryOperatorType.remainder.getValue();
		}
		return null;

	}

	/**
	 * 复制符号枚举转String
	 * @param em
	 * @return String 赋值符号
	 */
	public static String getAssignOperator(AssignExpr.Operator em) {
		switch (em) {
		case assign:
			return EnumAssignOperatorType.assign.getValue();
		case plus:
			return EnumAssignOperatorType.plus.getValue();
		case minus:
			return EnumAssignOperatorType.minus.getValue();
		case star:
			return EnumAssignOperatorType.star.getValue();
		case slash:
			return EnumAssignOperatorType.slash.getValue();
		case and:
			return EnumAssignOperatorType.and.getValue();
		case or:
			return EnumAssignOperatorType.or.getValue();
		case xor:
			return EnumAssignOperatorType.xor.getValue();
		case rem:
			return EnumAssignOperatorType.rem.getValue();
		case lShift:
			return EnumAssignOperatorType.lShift.getValue();
		case rSignedShift:
			return EnumAssignOperatorType.rSignedShift.getValue();
		case rUnsignedShift:
			return EnumAssignOperatorType.rUnsignedShift.getValue();
		}
		return null;
	}
	
	/**
	 * 
	 * @param em
	 * @return String Unary的枚举String
	 */
	public static String getUnaryOperator(UnaryExpr.Operator em){
		switch(em){
		case positive:
			return EnumUnaryOperatorType.positive.getValue();
		case negative:
			return EnumUnaryOperatorType.negative.getValue();
		case preIncrement:
			return EnumUnaryOperatorType.preIncrement.getValue();
		case preDecrement:
			return EnumUnaryOperatorType.preDecrement.getValue();
		case not:
			return EnumUnaryOperatorType.not.getValue();
		case inverse:
			return EnumUnaryOperatorType.inverse.getValue();
		case posIncrement:
			return EnumUnaryOperatorType.posIncrement.getValue();
		case posDecrement:
			return EnumUnaryOperatorType.posDecrement.getValue();
		}
		return null;
	}

}
