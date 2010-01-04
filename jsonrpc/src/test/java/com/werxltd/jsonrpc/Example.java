package com.werxltd.jsonrpc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONObject;

public class Example {
	public boolean init = false;

	public Example() {
		init = true;
	}

	public static String test() {
		return "test successful";
	}

	@SuppressWarnings("unused")
	private String hidden() {
		return "hidden method";
	}

	public float add(float a, float b) {
		return a + b;
	}

	public float sub(float a, float b) {
		return a - b;
	}

	public String list(int a, int b, int c) {
		return "a:" + a + " b:" + b + " c:" + c;
	}

	public String echo(String s) {
		return s;
	}

	public String echoJsonString(JSONObject obj)
			throws UnsupportedEncodingException {
		return URLEncoder.encode(obj.toString(), "UTF-8");
	}

	public JSONObject echoJsonObj(JSONObject obj)
			throws UnsupportedEncodingException {
		return obj;
	}
}
