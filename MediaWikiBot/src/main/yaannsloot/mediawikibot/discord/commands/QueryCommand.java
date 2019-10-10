package main.yaannsloot.mediawikibot.discord.commands;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import main.yaannsloot.mediawikibot.core.MediaWikiBot;
import main.yaannsloot.mediawikibot.core.entities.QueryResult;
import main.yaannsloot.mediawikibot.core.entities.WikiEndpoint;
import main.yaannsloot.mediawikibot.exceptions.NoMatchException;
import main.yaannsloot.mediawikibot.resolvers.Resolver;
import main.yaannsloot.mediawikibot.tools.BotUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class QueryCommand extends Command {

	@Override
	public boolean CheckForCommandMatch(Message command) {
		boolean result = true;
		List<String> commandWords = Arrays.asList(BotUtils.normalizeSentence(command.getContentRaw()).split(" "));
		List<String> otherCommandWords = Arrays.asList("help");
		if (commandWords.size() >= 2) {
			for (String otherWord : otherCommandWords) {
				if (otherWord.toLowerCase().equals(commandWords.get(1).toLowerCase())) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	@Override
	public void execute(MessageReceivedEvent event) throws NoMatchException {
		if (!(CheckForCommandMatch(event.getMessage()))) {
			throw new NoMatchException();
		}
		List<String> commandWords = Arrays
				.asList(BotUtils.normalizeSentence(event.getMessage().getContentRaw()).split(" "));
		if (commandWords.size() > 2) {
			WikiEndpoint targetEndpoint = null;
			for (WikiEndpoint endpoint : MediaWikiBot.endpoints) {
				if (endpoint.getReferenceId().toLowerCase().equals(commandWords.get(1).toLowerCase())) {
					targetEndpoint = endpoint;
					break;
				}
			}
			if (targetEndpoint != null) {
				Resolver targetResolver = null;
				for(Resolver resolver : CommandRegistry.ResolverList) {
					if(resolver.getResolverId().equals(targetEndpoint.getResolverId())) {
						targetResolver = resolver;
						break;
					}
				}
				if(targetResolver != null) {
					String query = "";
					for (int i = 2; i < commandWords.size(); i++) {
						query += commandWords.get(i) + " ";
					}
					query = BotUtils.normalizeSentence(query);
					QueryResult result = targetResolver.queryEndpoint(targetEndpoint, query);
					if(result != null) {
						EmbedBuilder message = new EmbedBuilder();
						message.setTitle(result.getTitle());
						message.setDescription("[Page Link](" + result.getPageUrl() + ")");
						message.addField("Summary:", result.getSummary(), false);
						message.setColor(targetEndpoint.getDisplayColor());
						if(!result.getThumbnailUrl().equals(""))
							message.setImage(result.getThumbnailUrl());
						event.getChannel().sendMessage(message.build()).queue();
					} else {
						EmbedBuilder message = new EmbedBuilder();
						message.setTitle("No results found");
						message.setColor(Color.red);
						event.getChannel().sendMessage(message.build()).queue();
					}
				} else {
					EmbedBuilder message = new EmbedBuilder();
					message.addField("Bot configuration error",
							"The specified resolver for the endpoint that this prefix refers to does not exist", false);
					message.setColor(Color.red);
					event.getChannel().sendMessage(message.build()).queue();
				}
			} else {
				EmbedBuilder message = new EmbedBuilder();
				message.addField("Unrecognized wiki prefix",
						"The specified wiki prefix is not present in the bot's database", false);
				message.setColor(Color.red);
				event.getChannel().sendMessage(message.build()).queue();
			}
		} else {
			if (commandWords.size() == 2) {
				EmbedBuilder message = new EmbedBuilder();
				message.addField("No query provided", "You must provide a search term when using this command", false);
				message.setColor(Color.red);
				event.getChannel().sendMessage(message.build()).queue();
			} else {
				EmbedBuilder message = new EmbedBuilder();
				message.addField("No wiki prefix provided", "You must provide a valid wiki prefix to use this command",
						false);
				message.setColor(Color.red);
				event.getChannel().sendMessage(message.build()).queue();
			}
		}
	}

	@Override
	public String getHelpSnippet() {
		return MediaWikiBot.botPrefix + " <wiki prefix> <query> - Queries one of the wikis in the bot's database";
	}

}
