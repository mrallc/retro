package com.xoba.ngaro.inf;

import java.io.IOException;

public interface IOManager {

	public void pushInputName(String name);

	/**
	 * same semantics as java.io.Inputstream.read()
	 * 
	 * @return
	 * @throws IOException
	 */
	public int read();

	public void write(int c);

	public int rxOpenFile(int mode, String name);

	public int rxReadFile(int slot);

	public int rxWriteFile(int slot, int c);

	public int rxCloseFile(int slot);

	public int rxGetFilePosition(int slot);

	public int rxSetFilePosition(int slot, int pos);

	public int rxGetFileSize(int slot);

	public int rxDeleteFile(String name);

}