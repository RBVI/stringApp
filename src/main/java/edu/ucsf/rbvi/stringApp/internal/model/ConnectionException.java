package edu.ucsf.rbvi.stringApp.internal.model;

/**
 * Checked exception thrown when a request to the STRING server fails, such as due to a
 * malformed URL, an unreachable host, or an unexpected response.
 */
public class ConnectionException extends Exception {

	private static final long serialVersionUID = 7398670567466555687L;
	
	public ConnectionException(String message) {
		super(message);
	}
}
