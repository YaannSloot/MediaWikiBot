package com.yaannsloot.mediawikibot.discord.commands;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.io.FileUtils;

import com.yaannsloot.mediawikibot.core.MediaWikiBot;
import com.yaannsloot.mediawikibot.core.entities.QueryResult;
import com.yaannsloot.mediawikibot.exceptions.NoMatchException;
import com.yaannsloot.mediawikibot.resolvers.Resolver;
import com.yaannsloot.mediawikibot.sources.endpoints.WikiEndpoint;
import com.yaannsloot.mediawikibot.tools.BotUtils;

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
					result = false;
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
				for (Resolver resolver : CommandRegistry.ResolverList) {
					if (resolver.getResolverId().equals(targetEndpoint.getResolverId())) {
						targetResolver = resolver;
						break;
					}
				}
				if (targetResolver != null) {
					String query = "";
					for (int i = 2; i < commandWords.size(); i++) {
						query += commandWords.get(i) + " ";
					}
					query = BotUtils.normalizeSentence(query);
					QueryResult result = targetResolver.queryEndpoint(targetEndpoint, query);
					if (result != null) {
						EmbedBuilder message = new EmbedBuilder();
						message.setTitle(result.getTitle(), result.getPageUrl());
						message.setDescription(result.getSummary());
						try {
							message.setFooter("From " + new URL(targetEndpoint.getApiUrl()).getHost() + " | Requested "
									+ new SimpleDateFormat("MM/dd/yyyy")
											.format(Date.from(event.getMessage().getTimeCreated().toInstant())));
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
						message.setColor(targetEndpoint.getDisplayColor());
						if (!result.getThumbnailUrl().equals("")) {
							try {
								try {
									File temp = File.createTempFile("mwb", null);
									FileUtils.copyURLToFile(new URL(result.getThumbnailUrl()), temp);
									Dimension dim = Imaging.getImageSize(temp);
									temp.delete();
									double ratio = dim.getWidth() / dim.getHeight();
									if(ratio > 1.75) {
										message.setImage(result.getThumbnailUrl());
									} else {
										message.setThumbnail(result.getThumbnailUrl());
									}
								} catch (ImageReadException e) {
									e.printStackTrace();
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
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
