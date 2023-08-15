package com.agro360.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TsFieldModel {

	private String name;

	private String type;
	
	private String importJavaType;

	private boolean isGeneric;

	private boolean isEnumValue;

	private String genericType;
	
	private String genericTypeImportJavaType;

	public String getFieldCode() {
		
		if( isEnumValue ) {
			return name;
		}
		
		var tsFieldType = type;
		if (isGeneric && genericType != null && !genericType.isBlank()) {
			tsFieldType += String.format("<%s>", genericType);
		}
		return String.format("\t%s: %s;", name, tsFieldType);
	}
}
