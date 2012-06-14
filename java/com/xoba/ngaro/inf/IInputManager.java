package com.xoba.ngaro.inf;

import java.io.IOException;

public interface IInputManager {

	public void pushInputName(String name) throws IOException;

	/**
	 * same semantics as java.io.Inputstream.read()
	 * 
	 * @return
	 * @throws IOException
	 */
	public int read() throws IOException;
}