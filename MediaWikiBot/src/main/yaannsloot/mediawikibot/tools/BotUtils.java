package main.yaannsloot.mediawikibot.tools;

import java.util.List;

/**
 * Contains helpful data processing tools for command input and List/Array
 * comparisons
 * 
 * @author IanSloat
 *
 */
public class BotUtils {

	/**
	 * Checks if a string contains any of a specified array of words.
	 * 
	 * @param inputSentence   The string to compare with the string array
	 * @param Wordlist        The strings or "words" to check for
	 * @param isCaseSensitive Whether or not to ignore case
	 * @param insertSpaces    Whether or not to insert a whitespace character at the
	 *                        beginning and end of each word before comparing with
	 *                        the input sentence
	 * @return true if the sentence contains any of the specified words, false
	 *         otherwise
	 */
	public static boolean checkForWords(String inputSentence, String[] Wordlist, boolean isCaseSensitive,
			boolean insertSpaces) {
		boolean isTrue = false;
		if (!isCaseSensitive) {
			inputSentence = inputSentence.toLowerCase();
			for (int i = 0; i < Wordlist.length; i++)
				Wordlist[i] = Wordlist[i].toLowerCase();
		}
		for (String word : Wordlist) {
			if (insertSpaces)
				word = " " + word + " ";
			if (inputSentence.contains(word))
				isTrue = true;
		}
		return isTrue;
	}

	/**
	 * Checks if a string contains any of a specified array of words.
	 * 
	 * @param inputSentence   The string to compare with the string array
	 * @param Wordlist        The strings or "words" to check for
	 * @param isCaseSensitive Whether or not to ignore case
	 * @return true if the sentence contains any of the specified words, false
	 *         otherwise
	 */
	public static boolean checkForWords(String inputSentence, String[] Wordlist, boolean isCaseSensitive) {
		boolean isTrue = false;
		if (!isCaseSensitive) {
			inputSentence = inputSentence.toLowerCase();
			for (int i = 0; i < Wordlist.length; i++)
				Wordlist[i] = Wordlist[i].toLowerCase();
		}
		for (String word : Wordlist)
			if (inputSentence.contains(word))
				isTrue = true;
		return isTrue;
	}

	/**
	 * Checks if a string contains any of a specified array of words.
	 * 
	 * @param inputSentence The string to compare with the string array
	 * @param Wordlist      The strings or "words" to check for
	 * @return true if the sentence contains any of the specified words, false
	 *         otherwise
	 */
	public static boolean checkForWords(String inputSentence, String[] Wordlist) {
		boolean isTrue = false;
		for (String word : Wordlist)
			if (inputSentence.contains(word))
				isTrue = true;
		return isTrue;
	}

	/**
	 * Checks if a list of strings contain any of a specified array of words.
	 * 
	 * @param inputList The list of strings to compare with the string array
	 * @param Wordlist  The strings or "words" to check for
	 * @return true if the sentence contains any of the specified words, false
	 *         otherwise
	 */
	public static boolean checkForWords(List<String> inputList, String[] Wordlist) {
		boolean isTrue = false;
		for (String word : Wordlist)
			if (inputList.contains(word))
				isTrue = true;
		return isTrue;
	}

	/**
	 * Trims the specified string and removes extra whitespaces that occur within
	 * the string <br>
	 * <br>
	 * Example:<br>
	 * input =
	 * "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;word1&nbsp;&nbsp;&nbsp;&nbsp;word2&nbsp&nbsp;&nbsp;word3&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"<br>
	 * output = "word1 word2 word3"
	 * 
	 * @param input The string to normalize
	 * @return
	 */
	public static String normalizeSentence(String input) {
		String output = "";
		if (input.length() != 0) {
			input = input.trim();
			char previousChar = input.charAt(0);
			for (char c : input.toCharArray()) {
				if (previousChar == ' ') {
					if (c != previousChar) {
						output += c;
					}
				} else {
					output += c;
				}
				previousChar = c;
			}
		}
		return output;
	}

	/**
	 * 
	 * @param list
	 * @param elements
	 * @return
	 */
	public static boolean checkForElement(List<?> list, List<?> elements) {
		boolean value = false;
		for (Object element : elements) {
			if (list.contains(element)) {
				value = true;
				break;
			}
		}
		return value;
	}

	/**
	 * 
	 * @param array
	 * @param word
	 * @return
	 */
	public static boolean stringArrayContains(String[] array, String word) {
		boolean result = false;
		for (String w : array) {
			if (w.equals(word)) {
				result = true;
			}
		}
		return result;
	}

}
