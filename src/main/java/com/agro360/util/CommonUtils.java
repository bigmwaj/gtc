package com.agro360.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommonUtils {

	public static <T> Set<T> merge(Set<T> s1, Set<T> s2){
		Set<T> m = new HashSet<>(s1);
		m.addAll(s2);
		return m;
	}
	
	private static void initJavaTypeMap(Map<String, String> javaTypeMap, String TsType, String ... javaType) {
		for (String jt : javaType) {
			if( !javaTypeMap.containsKey(jt)) {
				javaTypeMap.put(jt, TsType);
			}
		}
	}
	
	public static void initJavaTypeMap(Map<String, String> javaTypeMap) {
		initJavaTypeMap(javaTypeMap, "string", "String");
		initJavaTypeMap(javaTypeMap, "boolean", "boolean", "Boolean");
		initJavaTypeMap(javaTypeMap, "Array", "List", "Set");
		initJavaTypeMap(javaTypeMap, "number", "Short", "short", "long", "Long", "int", "Integer", "double", "Double", "BigDecimal");
	}
	
	public static String getTsType(String[] filterParam, String javaType) {
		var anyType = Arrays.stream(filterParam).noneMatch(javaType::endsWith);
		
		if ( anyType ) {
			return "any";
		}
		return javaType;
	}
}
