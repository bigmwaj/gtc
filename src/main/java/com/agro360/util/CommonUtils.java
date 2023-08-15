package com.agro360.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommonUtils {

	public static <T> Set<T> merge(Set<T> s1, Set<T> s2){
		Set<T> m = new HashSet<>(s1);
		m.addAll(s2);
		return m;
	}
	
	public static String getTsType(List<String> filterParam, String javaType) {
		switch (javaType) {
		case "String":
			return "string";

		case "boolean":
		case "Boolean":
			return "boolean";
			
		case "List":
		case "Set":
			return "Array";
			
		case "long":
		case "Long":
		case "int":
		case "Integer":
		case "double":
		case "Double":			
			return "number";

		default:
			var anyType = filterParam.stream().noneMatch(javaType::endsWith);
			
			if ( anyType ) {
				return "any";
			}
			break;
		}
		return javaType;
	}
}
