package com.werxltd.jsonrpc.events;

import javax.servlet.ServletConfig;

public class JSONRPCMessage {
	/*
	 * The following are message types which also serve to indicate the type
	 * of information that is also being supplied in the data field.
	 */
	public static final int INIT          = 10;
	public static final int EXCEPTION     = 60;
	
	private int code;
	
	private ServletConfig sevletconfig;
	
	public JSONRPCMessage( int code) {
    	this.code = code;
    }
    
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public void setServletConfig(ServletConfig serverconfig) {
		this.sevletconfig = serverconfig;
	}

	public ServletConfig getServletConfig() {
		return sevletconfig;
	}

}
