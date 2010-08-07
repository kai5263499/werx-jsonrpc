package com.werxltd.jsonrpc.events;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.werxltd.jsonrpc.Response;

public class JSONRPCMessage {
	/*
	 * The following are message types which also serve to indicate the type
	 * of information that is also being supplied in the data field.
	 */
	public static final int INIT		  = 10;
	public static final int BEFOREREQUEST = 20;
	public static final int AFTERREQUEST  = 30;	
	public static final int BEFORERESPONSE= 40;
	public static final int AFTERRESPONSE = 50;
	public static final int EXCEPTION	  = 60;
	
	private int code;
	
	private ServletConfig sevletconfig;
	private HttpServletRequest request;
	private HttpServletResponse httpresponse;
	private Response rpcresponse;
	
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

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setHttpResponse(HttpServletResponse response) {
		this.httpresponse = response;
	}

	public HttpServletResponse getHttpResponse() {
		return httpresponse;
	}

	public void setRPCResponse(Response rpcresponse) {
		this.rpcresponse = rpcresponse;
	}

	public Response getRPCResponse() {
		return rpcresponse;
	}
}
