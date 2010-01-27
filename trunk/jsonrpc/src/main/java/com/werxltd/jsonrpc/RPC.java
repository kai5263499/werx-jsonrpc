package com.werxltd.jsonrpc;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class creates a servlet which implements the JSON-RPC specification.
 * 
 * @author Wes Widner
 * 
 */
@SuppressWarnings("serial")
public class RPC extends HttpServlet {
	protected final static Logger LOG = Logger.getLogger(RPC.class);
	
	private boolean PERSIST_CLASS = true;
	private boolean EXPOSE_METHODS = true;
	private boolean DETAILED_ERRORS = true;
	
	private Response response;
	private Request request;

	private HashMap<String, Object> rpcobjects;
	private HashMap<String, Method> rpcmethods;

	/**
	 * This method reads the servlet configuration for a list of classes it
	 * should scan for acceptable Method objects that can be called remotely.
	 * Acceptable methods are methods that do not have a {@link Modifier}
	 * marking them as abstract or interface methods. Static methods are fine.
	 * <P>
	 * Valid methods are gathered into a {@link HashMap} and instances of
	 * non-static classes are created and reused for subsequent RPC calls.
	 * <P>
	 * Class is marked as final to prevent overriding and possible interference
	 * upstream.
	 * 
	 * @see <a href=
	 *      "http://java.sun.com/j2se/1.5.0/docs/api/java/lang/reflect/Method.html"
	 *      >java.lang.reflect.Method</>
	 * @see <a
	 *      href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/reflect/Modifier.html">java.lang.reflect.Modifier</a>
	 * @see <a
	 *      href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/HashMap.html">java.util.HashMap</a>
	 * @param config
	 *            ServletConfig passed from container upon initialization
	 */
	public final void init(ServletConfig config) throws ServletException {
		try {
			String classnames[] = config.getInitParameter("rpcclasses")
					.replaceAll("\\s*", "").split(",");

			if(config.getInitParameter("expose_methods") != null) EXPOSE_METHODS = config.getInitParameter("expose_methods").equalsIgnoreCase("true");
			if(config.getInitParameter("detailed_errors") != null) DETAILED_ERRORS = config.getInitParameter("detailed_errors").equalsIgnoreCase("true");
			if(config.getInitParameter("persist_class") != null)  PERSIST_CLASS = config.getInitParameter("persist_class").equalsIgnoreCase("true");
			
			if (classnames.length < 1)
				throw new JSONRPCException("No RPC classes specified.");

			rpcmethods = new HashMap<String, Method>();
			rpcobjects = new HashMap<String, Object>();

			for (int o = 0; o < classnames.length; o++) {
				Class<?> c = Class.forName(classnames[o]);
				int classmodifiers = c.getModifiers();

				/*
				 * Class must be public and cannot be an abstract or interface
				 */
				if (Modifier.isAbstract(classmodifiers)
						|| !Modifier.isPublic(classmodifiers)
						|| Modifier.isInterface(classmodifiers))
					continue;

				if (!Modifier.isStatic(classmodifiers)) {
					try {
						rpcobjects.put(c.getName(), c.newInstance());
					} catch (InstantiationException ie) {
						LOG.error("Caught InstantiationException");
						continue;
					}
				}

				Method methods[] = c.getDeclaredMethods();

				for (int i = 0; i < methods.length; i++) {
					int methodmodifiers = methods[i].getModifiers();
					if (!Modifier.isPublic(methodmodifiers))
						continue;

					String methodsig = generateMethodSignature(methods[i]);
					if (methodsig == null)
						continue;

					if (rpcmethods.containsKey(methodsig)) {
						LOG.error("Skipping duplicate method name: ["
								+ methodsig + "]");
						continue;
					}
					rpcmethods.put(methodsig, methods[i]);
				}
			}

			if (classnames.length < 1)
				throw new JSONRPCException("No valid RPC methods found.");

			Class<?> rpcclass = this.getClass();
			Method infoMethod = rpcclass
					.getMethod("listrpcmethods", (Class<?>[]) null);
			rpcmethods.put("listrpcmethods:0", infoMethod);
			rpcobjects.put("listrpcmethods", this);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method generates a string containing the "signature" of a
	 * {@link java.util.Method} object in the form of methodname:paramcount or
	 * test:3
	 * <p>
	 * The output of this method is used to generate keys for Method objects
	 * stored in a {@link HashMap} for easy retrieval.
	 * 
	 * @param Method
	 *            The Method object you would like to generate a signature for.
	 * @return String Contains the "signature" of a Method object in the form of
	 *         methodname:paramcount or test:3
	 */
	private String generateMethodSignature(Method m) {
		int parmscount = 0;
		Class<?> paramclasses[] = m.getParameterTypes();
		for (int j = 0; j < paramclasses.length; j++) {
			if (paramclasses[j].getName().matches("org.json.JSONObject")) {
				return m.getName() + ":JSONObject";
			} else if (paramclasses[j].getName().matches("java.lang.String")
					|| paramclasses[j].isPrimitive())
				parmscount++;
			else
				return null;
		}

		return m.getName() + ":" + parmscount;
	}

	/**
	 * Method lists available RPC methods loaded from configured classes.
	 * 
	 * @return JSONObject Containing available method information.
	 * @throws JSONException
	 */
	public JSONObject listrpcmethods() throws JSONException {
		JSONObject result = new JSONObject();
		Iterator<String> iterator = rpcmethods.keySet().iterator();
		while (iterator.hasNext()) {
			String methodsig = (String) iterator.next();
			Method m = (Method) rpcmethods.get(methodsig);
			int modifiers = m.getModifiers();
			JSONObject methodObj = new JSONObject();
			methodObj.put("name", m.getName());
			methodObj.put("static", Modifier.isStatic(modifiers));
			methodObj.put("class", m.getDeclaringClass().getName());
			methodObj.put("returns", m.getReturnType().getName());
			Class<?> paramclasses[] = m.getParameterTypes();
			for (int i = 0; i < paramclasses.length; i++) {
				methodObj.append("params", paramclasses[i].getName());
			}
			if (!methodObj.has("params"))
				methodObj.put("params", new JSONArray());

			result.append("method", methodObj);
		}

		return result;
	}

	/**
	 * This method attempts to turn generic exceptions into valid JSONRPC
	 * exceptions.
	 * 
	 * @see <a
	 *      href="http://groups.google.com/group/json-rpc/web/json-rpc-1-2-proposal#error-object">JSON-RPC
	 *      Error Specification</a>
	 * 
	 * @param e
	 *            Generic exception thrown, for better, more readable
	 *            exceptions, use JSONRPCExcepton class.
	 * @throws JSONException
	 */
	private void handleException(Exception e) throws JSONException {
		response = new Response(e);
		if (!DETAILED_ERRORS)
			response.clearErrorData();
	}

	/**
	 * This method attempts to take a Throwable object and turn it into a 
	 * valid JSONRPC error.
	 * 
	 * @param t
	 * @throws JSONException
	 */
	private void handleException(Throwable t) throws JSONException {
		response = new Response(t);
		if (!DETAILED_ERRORS)
			response.clearErrorData();
	}
	
	/**
	 * Unified method for outputting the internal jsonresponse (error or not).
	 * Method checks for a "debug" parameter and, if it is set to "true", prints
	 * the JSONObject in a more human-readable fashion.
	 * 
	 * @param req
	 * @param res
	 * @throws IOException
	 * @throws JSONException
	 */
	private void writeResponse(HttpServletRequest req, HttpServletResponse res)
			throws IOException, JSONException {
		String jsonStr = "";
		
		res.setContentType("text/plain");
		
		PrintWriter writer = res.getWriter();
		if (req.getParameter("debug") != null
				&& req.getParameter("debug").matches("true"))
			jsonStr = response.getJSONString(2);
		else
			jsonStr = response.getJSONString();
		
		if(req.getParameter("callback") != null) {
			if(req.getParameter("callback").matches("\\?")) writer.println("("+jsonStr+")");
			else writer.println(req.getParameter("callback")+"("+jsonStr+")");
		} else writer.println(jsonStr);
	}

	/**
	 * @see doGet
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException {
		doGet(req, res);
	}

	/**
	 * Called by servlet container when a request is made.
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException {
		try {
			try {
				handleRequest(req, res);
			} catch (Exception e) {
				e.printStackTrace();
				handleException(e);
			}

			writeResponse(req, res);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e.getMessage());
		} finally {
			request = null;
			response = null;
		}
	}

	/**
	 * 
	 * @param req
	 *            HttpServletRequest given to us from doGet or doPost
	 * @param res
	 *            HttpServletResponse we can output information to, This method
	 *            passes res to the
	 *            {@link #writeResponse(HttpServletRequest, HttpServletResponse)}
	 *            method.
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws JSONRPCException
	 * @throws ClassNotFoundException
	 */
	private void handleRequest(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException, JSONException,
			IllegalAccessException, InvocationTargetException,
			JSONRPCException, ClassNotFoundException {

		response = new Response();
		request = new Request();

		if (req.getParameter("json") != null) {
			request.parseJSON(req.getParameter("json"));
		} else if (req.getParameter("data") != null) {
			request.parseJSON(req.getParameter("data"));
		}

		if (request.getId() == null) {
			if (req.getParameter("id") != null) {
				response.setId(req.getParameter("id"));
			}
		}
		String method = request.getMethod();

		if (method == null) {
			if (req.getParameter("method") != null) {
				method = req.getParameter("method");
				request.setMethod(method);
			} else {
				if(EXPOSE_METHODS) response.setResult(listrpcmethods());
				else throw new JSONRPCException("Unspecified JSON-RPC method.", -32600);
				return;
			}
		}

		if (request.getParamtype() == Request.ParamType.NONE) {
			if (req.getParameter("params") != null) {
				request.parseParams(req.getParameter("params"));
			}
		}

		/*
		 * throw new JSONRPCException(
		 * "JSON parameter failed to parse into valid JSONObject.", -32700);
		 * 
		 * if (!request.has("method")) throw new
		 * JSONRPCException("Unspecified JSON-RPC method.", -32600);
		 * 
		 * throw new JSONRPCException(
		 * "Invalid params data given. Must be object or array.", -32602);
		 */

		// Generate a string which includes the method name and the number of
		// parameters provided
		int param_count = request.getParamCount();
		String methodsig = method + ":" + param_count;

		Object result = new Object();
		Method m = null;

		Object methparams[] = null;
		if (rpcmethods.containsKey(method + ":JSONObject")) {
			m = (Method) rpcmethods.get(method + ":JSONObject");
			methparams = new Object[1];
			methparams[0] = request.getParamObj();
		} else if (rpcmethods.containsKey(methodsig)) {
			m = (Method) rpcmethods.get(methodsig);
			if (param_count > 0) {
				methparams = new Object[param_count];
				Class<?> paramtypes[] = m.getParameterTypes();
				for (int i = 0; i < paramtypes.length; i++) {
					if (paramtypes[i].getName().matches("float")) {
						methparams[i] = Float.parseFloat(request.getParamAt(i));
					} else if (paramtypes[i].getName().matches("int")) {
						methparams[i] = Integer.parseInt(request.getParamAt(i));
					} else if (paramtypes[i].getName().matches("long")) {
						methparams[i] = Long.getLong(request.getParamAt(i));
					} else if (paramtypes[i].getName().matches(
							"java.lang.String")) {
						methparams[i] = request.getParamAt(i);
					} else if (paramtypes[i].getName().matches("double")) {
						methparams[i] = Double.parseDouble(request
								.getParamAt(i));
					} else if (paramtypes[i].getName().matches("boolean")) {
						methparams[i] = Boolean.parseBoolean(request
								.getParamAt(i));
					}
				}
			}
		} else {
			throw new JSONRPCException("JSON-RPC method [" + method + "] with "
					+ param_count + " parameters not found.", -32601);
		}

		try {
			result = runMethod(m, param_count, methparams);
		} catch (InvocationTargetException ite) {
			if(ite.getCause() != null) handleException(ite.getCause());
			else throw ite;
		}
		response.setResult(result);
	}
	
	private Object runMethod(Method m, int param_count, Object[] methparams) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		int modifiers = m.getModifiers();
		Object result = new Object();
		if (Modifier.isStatic(modifiers)) {
			if (param_count > 0)
				result = (Object) m.invoke(null, methparams);
			else
				result = (Object) m.invoke(null);
		} else {
			Object obj = rpcobjects.get(m.getDeclaringClass().getName());
			if (param_count > 0)
				result = (Object) m.invoke(obj, methparams);
			else
				result = (Object) m.invoke(obj);
		}
		
		return result;
	}
}