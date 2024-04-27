package com.agro360.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CommonUtils {

	public static <T> Set<T> merge(Set<T> s1, Set<T> s2){
		Set<T> m = new HashSet<>(s1);
		m.addAll(s2);
		return m;
	}
	
	public static String getTsType(String[] filterParam, String javaType) {
		switch (javaType) {
		case "String":
			return "string";

		case "boolean":
		case "Boolean":
			return "boolean";
			
		case "List":
		case "Set":
			return "Array";

		case "short":
		case "Short":
		case "long":
		case "Long":
		case "int":
		case "Integer":
		case "double":
		case "Double":			
		case "BigDecimal":			
			return "number";

		default:
			var anyType = Arrays.stream(filterParam).noneMatch(javaType::endsWith);
			
			if ( anyType ) {
				return "any";
			}
			break;
		}
		return javaType;
	}
}
