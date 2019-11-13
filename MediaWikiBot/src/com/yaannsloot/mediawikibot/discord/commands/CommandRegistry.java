package com.yaannsloot.mediawikibot.discord.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.yaannsloot.mediawikibot.resolvers.GenericResolver;
import com.yaannsloot.mediawikibot.resolvers.Resolver;
import com.yaannsloot.mediawikibot.resolvers.WikiaResolver;

public class CommandRegistry {

	// Put Global command related variables here

	// Help command will display snippets in the order they are in this list
	public static List<Command> DefaultInstances = new ArrayList<Command>(
			Arrays.asList(new HelpCommand(), new QueryCommand()));

	public static List<Resolver> ResolverList = new ArrayList<Resolver>(Arrays.asList(new GenericResolver(), new WikiaResolver()));

}
