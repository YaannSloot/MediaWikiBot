package main.yaannsloot.mediawikibot.exceptions;

public class WikiProjectNotFoundException extends Exception {
	
	private static final long serialVersionUID = 145472447821912132L;
	
	public WikiProjectNotFoundException(String project) {
		super("The specified project \"" + project + "\" does not exist");
	}

}
