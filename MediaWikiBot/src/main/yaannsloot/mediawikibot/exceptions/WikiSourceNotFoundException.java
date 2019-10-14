package main.yaannsloot.mediawikibot.exceptions;

public class WikiSourceNotFoundException extends Exception {
	
	private static final long serialVersionUID = 145472447821912132L;
	
	public WikiSourceNotFoundException(String project) {
		super("The specified source \"" + project + "\" does not exist");
	}

}
