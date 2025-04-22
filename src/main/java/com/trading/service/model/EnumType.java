package com.trading.service.model;

public enum EnumType {

	Success("0"),
	Fail("1"),
	;
	final String value;
	
	EnumType(String value) {
        this.value = value;
    }

	public String value(String suffix) {
		return value+suffix;
	}
	
    public String value() {
        return value;
    }
}
