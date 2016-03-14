public class fuzz {

	/**
	 * This will scan the dvwa webpage and then give testing based on the vectorlist
	 * inputs, sensitive data, and common word lists.  It will also discover pages
	 * and what they are linked too
	 */
	public static void main(String[] args) {
		
		boolean Testing = false;
		boolean commonWords = false;
		boolean vectorsTests = false;
		boolean sensitiveData = false;
		boolean randomTest = false;
		String startpage = args[1];
		String commonWordsFile = new String("");
		String sensitiveFilename = "";
		String vectorsFile = "";
		int slowTime = 500;
		String type = "";
		Fuzzer fuzzer;
		
		for(int x = 2; x <= args.length - 1 ; x++) {
			//parses the current argument
			if (args[x].contains("common_words=")) {
				commonWords = true;
				commonWordsFile=args[x].substring(15);	
				} 
			else if(args[x].contains("auth=")) {
				type=args[x].substring(14);
				}
			else if(Testing && args[x].toLowerCase().contains("random=")) {
				
				if (args[x].contains("true")) {
					randomTest = true;
				}
			} 
			else if(Testing && args[x].contains("vectors=")) {
				vectorsFile = args[x].substring(10);
				vectorsTests = true;
			} else if(Testing && args[x].contains("slow=")) {
				if (!args[x].substring(7).equals("")) {
					slowTime = Integer.parseInt(args[x].substring(7));
				}
				else if (Testing && args[x].toLowerCase().contains("sensitive=")) {
					if (!args[x].substring(12).equals("")) {
					sensitiveFilename = args[x].substring(12);
					sensitiveData = true;
					}
			} else {
				manMessage("invalid option " + args[x]);
				System.exit(1);
			}
			}
		fuzzer = new Fuzzer(commonWordsFile, vectorsFile, sensitiveFilename, randomTest, slowTime);
		fuzzer.fuzz(startpage, type);
	}
	}
	
	/*----------------------------------------------------------------------------
	 * Method to print out the man page message in the case of invalid parameters,
	 * 	whether it be of wrong type or too few.
	 * 
	 * @param error  Custom error message for each unique situation
	 -----------------------------------------------------------------------------*/
	
	private static void manMessage(String error) {
		System.err.println("Error: " + error);
		System.err.println("fuzz[test]\n");
		System.err.println("COMMANDS:");
		System.err.println("	test - Discover all inputs, then attempt a " +
				"list of exploit vectors on those inputs\n");
		System.err.println("OPTIONS:");
		System.err.println("	--custom-auth=string - Use a hard coded log in system");
		System.err.println("	Discover options:");
		System.err.println("		common_words=file" +
				"file of common words to guess urls " +
				"\n");
		System.err.println("	Test options:");
		System.err.println("		vectors=file - exploits/vunerabilities to use");
		System.err.println("		sensitive=file - filedata that should be secured");
		System.err.println("		random=[true|false] - choosing a random page");
		System.err.println("		slow=500 - Number of milliseconds for a bad response");
	}
}