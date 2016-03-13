import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;
import java.io.BufferedReader;
import java.io.InputStreamReader;



import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;


public class Fuzzer {
     public static final Double NANO_TO_MILLIS = 1000000.0;
	String mainURL;
	private ArrayList<Webpage> pages = new ArrayList<Webpage>();
	private ArrayList<String> cookies = new ArrayList<String>();
	private ArrayList<String> commonWords = new ArrayList<String>();
    private ArrayList<String> sensitiveData = new ArrayList<String>();
    private ArrayList<String> vectorsList = new ArrayList<String>();
	
    private CookieManager cm;
    private static TimedWebClient client;
	private long slowValue = 500;
    private boolean random = false;
    
    public Fuzzer(){
    	client = new TimedWebClient(500);
    }
    
	public ArrayList<String> loadCommonWordsFile(String fileName){
		Scanner input;
		try {
			input = new Scanner(new File(fileName));
			while (input.hasNext()) {
				String s = input.nextLine();
                commonWords.add(s + ".php");
                commonWords.add(s + ".jsp");
                commonWords.add(s);
			}
		} catch (FileNotFoundException e) {
			System.err.println("Common words file error: " + e.getMessage());
		}
		return commonWords;
	}

    public ArrayList<String> loadSensitiveDataFile(String fileName){
        Scanner input;
        try {
            input = new Scanner(new File(fileName));
            while (input.hasNext()) {
                String s = input.nextLine();
                sensitiveData.add(s);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Sensitive Data file error: " + e.getMessage());
        }
        return sensitiveData;
    }

    public ArrayList<String> loadVectorsFile(String fileName){
        Scanner input;
        try {
            input = new Scanner(new File(fileName));
            while (input.hasNext()) {
                String s = input.nextLine();
                vectorsList.add(s);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Vectors file error: " + e.getMessage());
        }
        return vectorsList;
    }
	
	public void loginDVWA() {
        HtmlPage page = null;
        HtmlForm form = null;

            try {
            page = client.getPage("http://127.0.0.1/dvwa/login.php");
            } catch(Exception e) {
                e.printStackTrace();
            }
            //form = page.getForms().get(0);
            form = page.getFirstByXPath("/html/body/div/form");
            form.getInputByName("username").setValueAttribute("admin");
            form.getInputByName("password").setValueAttribute("password");
            try{
                page = form.getInputByName("Login").click();
                mainURL = "http://127.0.0.1/dvwa";
            } catch (Exception e){
                System.out.println("Error logging in: " + e.getMessage());
            }	
    }
	
    public static List<String> getCookies(String s) throws FailingHttpStatusCodeException, IOException{
    	//TimedWebClient cl = new TimedWebClient(slowValue);
    	CookieManager mg = client.getCookieManager();
    	mg.setCookiesEnabled(true);
    	//cl.getPage(url);
        Set<Cookie> arr = mg.getCookies();
        List<String> lst = new ArrayList<String>();
        for (Cookie c : arr){
        	lst.add(c.getName() +": " + c.getPath());
        }
        return lst;
    }
	
	public void SensitiveDataLeak(Webpage page, HtmlPage curr){
		for (String st: sensitiveData){
			if (curr.asXml().contains(st) && !page.getSensitiveData().contains(st)){
				page.getSensitiveData().add(st);
			}
		}
		
	}
	
	public Webpage parseURL(String url){
		Webpage page = null;
		try {
			URL ur = new URL(url);
			page = new Webpage(url);
			if (pages.contains(page)){
				for (Webpage pg : pages){
					if (pg.equals(page)){
						page = pg;
						break;
					}
				}
			} else{
				pages.add(page);
			}
		if (ur.getQuery() != null) {
			for (String query: ur.getQuery().split("&")) {
				String input = query.split("=")[0];
				if (!page.getInputs().contains(input)) {
					page.getInputs().add(input);
				}
			}
		}
		} catch (Exception e){
			System.err.println(e.getMessage());
		}
	return page;
	}
	
	public void discoverLinks(String url, Webpage page){
		try{
			HtmlPage html = client.getPage(url);
            SensitiveDataLeak(page, html);
			List<HtmlAnchor> links = html.getAnchors();
			for (HtmlAnchor link: links){
				URL url1 = new URL(this.mainURL + "/" + link.getHrefAttribute());
				if (!pages.contains(new Webpage(url1.getPath()))) {
					Webpage page1 = parseURL(url1.toString());
					discoverLinks(url1.toString(), page1);
				}
				else {
					parseURL(url1.toString());
				}
			}
		} catch (Exception e){
			System.err.println("Invalid link: " + e.getMessage());
		}
	}
	
	public void discoverPages(){
		//get links via recursion
		discoverLinks(mainURL, parseURL(mainURL));
		//initial page guessing
        for(String word: commonWords){
            try{
                HtmlPage html = client.getPage(mainURL + "/" + word);
                System.out.println("DISCOVER - Valid URL guessed: " + mainURL + "/" + word);
                Webpage p = parseURL(mainURL + "/" + word);
                SensitiveDataLeak(p, html);
            } catch (Exception e) {
                System.err.println("Guessed url couldn't be reached " + mainURL + "/" + word);
            } 
        }
	}
	
	public void discoverForms(){
		for (Webpage p: pages){
			try{
				HtmlPage page = client.getPage(p.getUrl());
				List<HtmlForm> forms = page.getForms();
				for (HtmlForm form: forms){
					p.getForms().add(form);
				}
				SensitiveDataLeak(p, page);
			} catch (Exception e){
				System.err.println("Problem in discovering forms: " + e.getMessage());
			}
		}
	}
	
	public void print(){
		System.out.println("\nValid pages discovered: " + pages.size());
		for(Webpage p: pages){
			System.out.println("url: " + p.getUrl());
			System.out.println("\tURL Inputs: " + p.getInputs().size());
			for(String url: p.getInputs()){
				System.out.println("\t\turl: " + url);
			}
			System.out.println("\tForm Inputs: " + p.getForms().size());
			for(HtmlForm form: p.getForms()){
				System.out.println("\t\tform: " + form);
			}
            System.out.println("\tSensitive data leaked: " + p.getSensitiveData().size());
            for (String data : p.getSensitiveData()) {
                System.out.println("\t\t" + data);
            }
		}
	}
	
	
	
	
	
	
	
	
	
	
	//////////////////////////////////////////////////////////////////
	
	
	
/**	
	
	public static void main(String[] args) throws Exception{
		
		//String[] extensions = {"php","html","jsp"};
		//URL test = new URL("http://127.0.0.1/dvwa/vulnerabilities/fi/?page=include.php");
		//ArrayList<String> data = UrlParse(test);
		//System.out.print(data.toString());
		//URL refined = RebuildUrl(data);
		//System.out.print(refined.toString());
		//String Alteration = urlSearchAlteration(data.get(0));
		//String AlterationLegit = urlSearchAlteration(data.get(4));
		//System.out.print(Alteration);
		//System.out.print(AlterationLegit);
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		
        if (args.length < 2 || args.length > 4){
        	System.out.println("Bad command");
        	return;
        }
        
        if (args.length == 2 && args[0].equals("discover")){
        	URL url = new URL(args[1]);
        	//linkDiscovery(url);
        	System.out.println();
        	System.out.println();
        	//guessURL(url);
        	WebClient client = new WebClient();
        	HtmlPage pg = client.getPage(url);
        	getInputs(pg);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        }
        else if (args.length == 3 && args[0].equals("discover") && args[2].contains("common")){
        	URL url = new URL(args[1]);
        	//linkDiscovery(url);
        	System.out.println();
        	System.out.println();
        	guessURL(url, args[2].substring(args[2].lastIndexOf('=')+1, args[2].length()));
        	System.out.println();
        	System.out.println();
        	WebClient client = new WebClient();
        	HtmlPage pg = client.getPage(url);
        	getInputs(pg);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        }
        else if (args.length == 3 && args[0].equals("discover") && args[2].contains("custom")){
        	URL url = new URL(args[1]);
        	//HtmlPage pg1 = loginDvwa(url);
        	//url = pg1.getUrl();
        	//linkDiscovery(url);
        	System.out.println();
        	System.out.println();
        	
        	//getInputs(pg1);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        }
        else if (args.length == 4 && args[0].equals("discover")&& args[3].contains("common") && args[2].contains("custom")){
        	URL url = new URL(args[1]);
        	System.out.println("LOGIN PAGE INFORMATION");
        	System.out.println();
        	//linkDiscovery(url);
        	System.out.println();
        	System.out.println();
        	guessURL(url, args[3].substring(args[3].lastIndexOf('=')+1, args[3].length()));
        	System.out.println();        	
        	System.out.println();
        	
        	
        	WebClient client = new WebClient();
        	HtmlPage pg = client.getPage(url);
        	getInputs(pg);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        	System.out.println();
        	System.out.println();
        	System.out.println("DVWA MAIN PAGE INFORMATION");
        	//HtmlPage pg1 = loginDvwa();
        	//url = pg1.getUrl();
        	//linkDiscovery(url);
        	System.out.println();
        	System.out.println();
        	guessURL(url, args[3].substring(args[3].lastIndexOf('=')+1, args[3].length()));
        	System.out.println();        	
        	System.out.println();
        	
        	//getInputs(pg1);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        }
	}

**/

	
	// all urls you can reach from the initial page
	public static void linkDiscoveryPage(HtmlPage page) throws FailingHttpStatusCodeException, IOException{
		WebClient client = new WebClient();
		ArrayList<URL> urls = new ArrayList<URL>();
		
		List<HtmlAnchor> lst = page.getAnchors();
		if (lst.size() == 0){
			System.out.println("No URLs discovered.");
			return;
		}
		System.out.println("URLs discovered: ");
		for (HtmlAnchor anc : lst){
	
		}
		//return urls;
	}

	// get all inputs from the page
	public static void getInputs(HtmlPage page){
		@SuppressWarnings("unchecked")
		List<HtmlInput> lst = (List<HtmlInput>) page.getByXPath("//input");
		if (lst.size() == 0){
			System.out.println("No Form inputs  discovered.");
			return;
		}
		System.out.println("Form inputs discovered: ");
		for (HtmlInput inp : lst){
			System.out.println("\tInput Name: "+inp.getNameAttribute() + "\t Input Type: " + inp.getTypeAttribute());
		}
	}
	
	//used to cut a Url into pieces and return an Array of Strings
	public static ArrayList<String> UrlParse(URL url){
		String StrUrl = url.toString();
		ArrayList<String> ParsedUrl = new ArrayList<String>();
		int start = 0;
		int LowestValue = 0;
		boolean record = false;
		for(int x = 0; x < StrUrl.length(); x++){
			String CurrentData = "";
			start : if(StrUrl.charAt(x) == '/' | (StrUrl.length() - 1) == x){
				if((StrUrl.length() - 1) != x){
					if(StrUrl.charAt(x + 1)== '/'){
						break start;
					}
				}
				if(record == false){
					record = true;
					start = x;
				}
				else{
					for(int y = start + 1; y <= x; y++){
						CurrentData = CurrentData + StrUrl.charAt(y);
					}
					ParsedUrl.add(LowestValue, CurrentData);
					LowestValue++;
					start = x;
				}
			}
		}
		return ParsedUrl;
	}
	
	public String Extend(String Extender, String Extendee){
		String NewWord = Extendee + Extender;
		return NewWord;
	}
	
	//Detects if a search based function is in the url
	public static boolean urlSearchDetect(String urlSegment){
		boolean start = false;
		boolean end = false;
		for(int x = 0; x < urlSegment.length(); x++){
			if(urlSegment.charAt(x) == '?'){
				start = true;
			}
			if(urlSegment.charAt(x) == '=' & start == true){
				end = true;
			}
		}
		return end;
		
	}
	
	//Grabs the search based function in the url so it can be used for Extend method
	public static String urlSearchAlteration(String urlSegment){
		String searchinfo = "";
		boolean state = false;
		if(urlSearchDetect(urlSegment) == true){
			for(int x = 0; x < urlSegment.length(); x++){
				if(state == false){
					searchinfo = searchinfo + urlSegment.charAt(x);
				}
				if(urlSegment.charAt(x) == '='){
					state = true;
				}
			}
		}
		return searchinfo;
	}
	
	public static URL RebuildUrl(ArrayList<String> ArrayStrURL){
		URL site = null;
		String url = "https://www.";
		for(int x = 0; x < ArrayStrURL.size(); x++){
			url = url + ArrayStrURL.get(x);
		}
		try {
			site = new URL(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return site;
	}
        
        public static void guessURL(URL url, String st) throws FailingHttpStatusCodeException, IOException{
            String new_url = "";
            ArrayList<URL> goodURLs = new ArrayList<URL>();
            String Url = url.toString();
            ParsedFile f = new ParsedFile();
            ArrayList<String> words = f.Parse(st);
            
            String extensions[] = {".jsp", ".php", ".html", "js", ""};
            
            for(String word :words){
                for(String e : extensions){
                    new_url = Url;  //set url equal to passed in url
                    new_url += "/"; // add / at the end of the url
                    new_url += word;  //append the word from the file to url
                    new_url += e; //add extension on the url
                    
                    
                    URL test_url = new URL(new_url);
                    
                    try{
                    HttpURLConnection conn = (HttpURLConnection) test_url.openConnection(); // open connection trying 
                    int responseCode = conn.getResponseCode();
                    if(responseCode != 404){
                        goodURLs.add(new URL(new_url));
                    }
                }
                //catch any exceptions
                catch(Exception except){
                        except.printStackTrace();
                        }
                }
            }
            
            if (goodURLs.size() == 0){
            	System.out.println("No pages guessed.");
            	return;
            }
            System.out.println("Pages guessed");
            for (URL ur : goodURLs){
            	System.out.println(ur.toString());
            }
        }
        
        public static void getCookies(URL url) throws FailingHttpStatusCodeException, IOException{
        	WebClient cl = new WebClient();
        	CookieManager mg = cl.getCookieManager();
        	mg.setCookiesEnabled(true);
        	cl.getPage(url);
            Set<Cookie> arr = mg.getCookies();
            if (arr.size()==0){
            	System.out.println("No Cookies doscovered");
            	return;
            }
            System.out.println("Cookies discovered: ");
            for (Cookie c : arr){
            	System.out.println("\tCookie Name: " + c.getName());
            }
        }
		        private static Boolean checkResposeTime(URL url, String s){
            boolean check = false;
            try {
                long nanoStart = System.nanoTime();
                long milliStart = System.currentTimeMillis(); 
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while (in.readLine() != null) {
                    in.close();
                }
                long nanoEnd = System.nanoTime();
                long milliEnd = System.currentTimeMillis();
                long nanoTime = nanoEnd - nanoStart;
                long milliTime = milliEnd - milliStart;
                Long l = Long.valueOf(s);
                TimeUnit.SECONDS.toMillis(l);
                if( milliTime < l){
                    check = true;
                }
            } catch (IOException ex) {
      
            }
            
            return check;
        }
        
        private static String convert(long nanoTime, long milliTime)
    {
        // convert nanoseconds to milliseconds and display both times with three digits of precision (microsecond)
        String formatted = String.format("%,.3f", nanoTime / NANO_TO_MILLIS);
        String milli = String.format("%,.3f", milliTime / 1.0 );
        return formatted;
    }
}

