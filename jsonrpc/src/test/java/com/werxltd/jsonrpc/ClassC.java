package com.werxltd.jsonrpc;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.werxltd.jsonrpc.events.JSONRPCEventListener;
import com.werxltd.jsonrpc.events.JSONRPCMessage;
import com.werxltd.jsonrpc.events.JSONRPCMessageEvent;

public class ClassC implements JSONRPCEventListener {
	public boolean init = false;

	private ServletConfig config;
	private HttpSession session;
	private HttpServletRequest request;
	private HttpServletResponse response;
	
	private Response lastresponse;
	
	public ClassC() {
		init = true;
	}

	public String getConfig() {
		if(config != null) return config.getInitParameter("rpcclasses");
		return "";
	}

	public Integer sessionCounter() {
		Integer count = (Integer) session.getAttribute("count");
		
		if(count == null) count = 1;
		else count += 1;
		
		System.out.println("count is: "+count);
		
		session.setAttribute("count", count);
		
		return (Integer) session.getAttribute("count");
	}
	
	public String test() {
		return "test from class C successful";
	}
	
	public String getLastResponse() {
		if(lastresponse != null) return (String) lastresponse.getResult();
		return "";
	}
	
	public void messageReceived(JSONRPCMessageEvent me) {
		switch(me.message().getCode()) {
			case JSONRPCMessage.INIT:
				config = me.message().getServletConfig();
			break;
			case JSONRPCMessage.BEFOREREQUEST:
				request = me.message().getRequest();
				session = request.getSession(true);
			break;
			case JSONRPCMessage.BEFORERESPONSE:
				response = me.message().getHttpResponse();
				response.setContentType("text/html");
			break;
			case JSONRPCMessage.AFTERRESPONSE:
				lastresponse = me.message().getRPCResponse();
			break;
		}
	}
}
