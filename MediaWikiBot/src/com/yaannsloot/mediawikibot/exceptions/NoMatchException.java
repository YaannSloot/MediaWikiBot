package com.yaannsloot.mediawikibot.exceptions;

public class NoMatchException extends Exception {

	private static final long serialVersionUID = 2471442433345020561L;

	public NoMatchException() {
		super("Command was executed with an invalid input message");
	}

}
