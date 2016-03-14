
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.CookieHandler;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class Fuzzer {

    public static final Double NANO_TO_MILLIS = 1000000.0;
    private ParsedFile p = new ParsedFile();
    String mainURL;
    private ArrayList<Webpage> pages = new ArrayList<Webpage>();
    private ArrayList<String> cookies = new ArrayList<String>();
    private ArrayList<String> commonWords = new ArrayList<String>();
    private ArrayList<String> sensitiveData = new ArrayList<String>();
    private ArrayList<String> vectorsList = new ArrayList<String>();

    private CookieManager cm;
    private static TimedWebClient client;
    private static long slowValue = 500;
    private Random random;
    private boolean status;

    public Fuzzer(String commonWordsFile, String vectorsFile, String sensitiveDataFile, boolean status, long time){
		pages = new ArrayList<Webpage>();
		cookies = new ArrayList<String>();
		commonWords = loadCommonWordsFile(commonWordsFile);
        sensitiveData = loadSensitiveDataFile(sensitiveDataFile);
        vectorsList = loadVectorsFile(vectorsFile);
        this.status = status;
		cm = new CookieManager();
		cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
	    CookieHandler.setDefault(cm);
		client = new TimedWebClient(time);
        random = new Random();
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
	}

   
    public void loginDVWA() {
        HtmlPage page = null;
        HtmlForm form = null;

        try {
            page = client.getPage("http://127.0.0.1/dvwa/login.php");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //form = page.getForms().get(0);
        form = page.getFirstByXPath("/html/body/div/form");
        form.getInputByName("username").setValueAttribute("admin");
        form.getInputByName("password").setValueAttribute("password");
        try {
            page = form.getInputByName("Login").click();
            mainURL = "http://127.0.0.1/dvwa";
        } catch (Exception e) {
            System.out.println("Error logging in: " + e.getMessage());
        }
    }

    public static List<String> getCookies(String s) throws FailingHttpStatusCodeException, IOException {
       // TimedWebClient cl = new TimedWebClient(slowValue);
        com.gargoylesoftware.htmlunit.CookieManager mg = client.getCookieManager();
        mg.setCookiesEnabled(true);
        //cl.getPage(url);
        Set<Cookie> arr = mg.getCookies();
        List<String> lst = new ArrayList<String>();
        for (Cookie c : arr) {
            lst.add(c.getName() + ": " + c.getPath());
        }
        return lst;
    }

    public void SensitiveDataLeak(Webpage page, HtmlPage curr) {
        for (String st : sensitiveData) {
            if (curr.asXml().contains(st) && !page.getSensitiveData().contains(st)) {
                page.getSensitiveData().add(st);
            }
        }

    }

    private List<HtmlInput> get_Form_Inputs(HtmlForm form) {
        List<HtmlInput> inputs = new ArrayList<HtmlInput>();
        for (DomNode test : form.getChildren()) {
            inputs.addAll(parse(test));
        }
        return inputs;
    }

    private HtmlSubmitInput get_Submit_Input(HtmlForm form) throws ElementNotFoundException {
        for (DomNode test : form.getChildren()) {
            try {
                return get_Submit_Input(test);
            } catch (ElementNotFoundException e) {
            }
        }
        throw new ElementNotFoundException("", "", "");
    }

    private HtmlSubmitInput get_Submit_Input(DomNode test) throws ElementNotFoundException {
        if (test instanceof HtmlSubmitInput) {
            return (HtmlSubmitInput) test;
        } else if (test.hasChildNodes()) {
            for (DomNode n : test.getChildren()) {
                try {
                    return get_Submit_Input(n);
                } catch (ElementNotFoundException e) {
                }
            }
        }
        throw new ElementNotFoundException("", "", "");
    }

    private List<HtmlInput> parse(DomNode node) {
        List<HtmlInput> inputs = new ArrayList<HtmlInput>();

        for (DomNode the_node : node.getChildren()) {
            if (the_node instanceof HtmlTextInput) {
                inputs.add((HtmlInput) the_node);
            } else if (the_node instanceof HtmlHiddenInput) {
                inputs.add((HtmlInput) the_node);
            } else if (the_node instanceof HtmlFileInput) {
                inputs.add((HtmlInput) the_node);
            } else if (the_node instanceof HtmlPasswordInput) {
                inputs.add((HtmlInput) the_node);
            } else if (the_node instanceof HtmlImageInput) {
                inputs.add((HtmlInput) the_node);
            }

            if (the_node.hasChildNodes()) {
                inputs.addAll(parse(the_node));
            }
        }

        return inputs;
    }

    public void discoverLinks(String url, Webpage page) {
        try {
            HtmlPage html = client.getPage(url);
            SensitiveDataLeak(page, html);
            List<HtmlAnchor> links = html.getAnchors();
            for (HtmlAnchor link : links) {
                URL url1 = new URL(this.mainURL + "/" + link.getHrefAttribute());
                if (!pages.contains(new Webpage(url1.getPath()))) {
                    Webpage page1 = parseURL(url1.toString());
                    discoverLinks(url1.toString(), page1);
                } else {
                    parseURL(url1.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Invalid link: " + e.getMessage());
        }
    }

    public void discoverPages() {
        //get links via recursion
        discoverLinks(mainURL, parseURL(mainURL));
        //initial page guessing
        for (String word : commonWords) {
            try {
                HtmlPage html = client.getPage(mainURL + "/" + word);
                System.out.println("DISCOVER - Valid URL guessed: " + mainURL + "/" + word);
                Webpage p = parseURL(mainURL + "/" + word);
                SensitiveDataLeak(p, html);
            } catch (Exception e) {
                System.err.println("Guessed url couldn't be reached " + mainURL + "/" + word);
            }
        }
    }

    public void print() {
        System.out.println("\nValid pages discovered: " + pages.size());
        for (Webpage p : pages) {
            System.out.println("url: " + p.getUrl());
            System.out.println("\tURL Inputs: " + p.getInputs().size());
            for (String url : p.getInputs()) {
                System.out.println("\t\turl: " + url);
            }
            System.out.println("\tForm Inputs: " + p.getForms().size());
            for (HtmlForm form : p.getForms()) {
                System.out.println("\t\tform: " + form);
            }
            System.out.println("\tSensitive data leaked: " + p.getSensitiveData().size());
            for (String data : p.getSensitiveData()) {
                System.out.println("\t\t" + data);
            }
        }
    }
	//////////////////////////////////////////////////////////////////

    // all urls you can reach from the initial page
    public static void linkDiscoveryPage(HtmlPage page) throws FailingHttpStatusCodeException, IOException {
        WebClient client = new WebClient();
        ArrayList<URL> urls = new ArrayList<URL>();

        List<HtmlAnchor> lst = page.getAnchors();
        if (lst.size() == 0) {
            System.out.println("No URLs discovered.");
            return;
        }
        System.out.println("URLs discovered: ");
        for (HtmlAnchor anc : lst) {

        }
        //return urls;
    }

    // get all inputs from the page
    public static void getInputs(HtmlPage page) {
        @SuppressWarnings("unchecked")
        List<HtmlInput> lst = (List<HtmlInput>) page.getByXPath("//input");
        if (lst.size() == 0) {
            System.out.println("No Form inputs  discovered.");
            return;
        }
        System.out.println("Form inputs discovered: ");
        for (HtmlInput inp : lst) {
            System.out.println("\tInput Name: " + inp.getNameAttribute() + "\t Input Type: " + inp.getTypeAttribute());
        }
    }

    //used to cut a Url into pieces and return an Array of Strings
    public static ArrayList<String> UrlParse(URL url) {
        String StrUrl = url.toString();
        ArrayList<String> ParsedUrl = new ArrayList<String>();
        int start = 0;
        int LowestValue = 0;
        boolean record = false;
        for (int x = 0; x < StrUrl.length(); x++) {
            String CurrentData = "";
            start:
            if (StrUrl.charAt(x) == '/' | (StrUrl.length() - 1) == x) {
                if ((StrUrl.length() - 1) != x) {
                    if (StrUrl.charAt(x + 1) == '/') {
                        break start;
                    }
                }
                if (record == false) {
                    record = true;
                    start = x;
                } else {
                    for (int y = start + 1; y <= x; y++) {
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

    public String Extend(String Extender, String Extendee) {
        String NewWord = Extendee + Extender;
        return NewWord;
    }

    //Detects if a search based function is in the url
    public static boolean urlSearchDetect(String urlSegment) {
        boolean start = false;
        boolean end = false;
        for (int x = 0; x < urlSegment.length(); x++) {
            if (urlSegment.charAt(x) == '?') {
                start = true;
            }
            if (urlSegment.charAt(x) == '=' & start == true) {
                end = true;
            }
        }
        return end;

    }

    //Grabs the search based function in the url so it can be used for Extend method
    public static String urlSearchAlteration(String urlSegment) {
        String searchinfo = "";
        boolean state = false;
        if (urlSearchDetect(urlSegment) == true) {
            for (int x = 0; x < urlSegment.length(); x++) {
                if (state == false) {
                    searchinfo = searchinfo + urlSegment.charAt(x);
                }
                if (urlSegment.charAt(x) == '=') {
                    state = true;
                }
            }
        }
        return searchinfo;
    }

    public static URL RebuildUrl(ArrayList<String> ArrayStrURL) {
        URL site = null;
        String url = "https://www.";
        for (int x = 0; x < ArrayStrURL.size(); x++) {
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

    public static void guessURL(URL url, String st) throws FailingHttpStatusCodeException, IOException {
        String new_url = "";
        ArrayList<URL> goodURLs = new ArrayList<URL>();
        String Url = url.toString();
        ParsedFile f = new ParsedFile();
        ArrayList<String> words = f.Parse(st);

        String extensions[] = {".jsp", ".php", ".html", "js", ""};

        for (String word : words) {
            for (String e : extensions) {
                new_url = Url;  //set url equal to passed in url
                new_url += "/"; // add / at the end of the url
                new_url += word;  //append the word from the file to url
                new_url += e; //add extension on the url

                URL test_url = new URL(new_url);

                try {
                    HttpURLConnection conn = (HttpURLConnection) test_url.openConnection(); // open connection trying 
                    int responseCode = conn.getResponseCode();
                    if (responseCode != 404) {
                        goodURLs.add(new URL(new_url));
                    }
                } //catch any exceptions
                catch (Exception except) {
                    except.printStackTrace();
                }
            }
        }

        if (goodURLs.size() == 0) {
            System.out.println("No pages guessed.");
            return;
        }
        System.out.println("Pages guessed");
        for (URL ur : goodURLs) {
            System.out.println(ur.toString());
        }
    }

    public static void getCookies(URL url) throws FailingHttpStatusCodeException, IOException {
        WebClient cl = new WebClient();
        com.gargoylesoftware.htmlunit.CookieManager mg = cl.getCookieManager();
        mg.setCookiesEnabled(true);
        cl.getPage(url);
        Set<Cookie> arr = mg.getCookies();
        if (arr.size() == 0) {
            System.out.println("No Cookies doscovered");
            return;
        }
        System.out.println("Cookies discovered: ");
        for (Cookie c : arr) {
            System.out.println("\tCookie Name: " + c.getName());
        }
    }

    private static Boolean checkResposeTime(URL url, String s) {
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
            if (milliTime < l) {
                check = true;
            }
        } catch (IOException ex) {

        }

        return check;
    }

    private static String convert(long nanoTime, long milliTime) {
        // convert nanoseconds to milliseconds and display both times with three digits of precision (microsecond)
        String formatted = String.format("%,.3f", nanoTime / NANO_TO_MILLIS);
        String milli = String.format("%,.3f", milliTime / 1.0);
        return formatted;
    }

    private boolean testRandPage(Webpage page) {
        System.out.println("Beginning testing of random page: " + page.Url_to_String());
        boolean isSelect = false;
        int rand = random.nextInt(2);
        if (page.getForms().size() > 0 && page.getInputs().size() > 0) {
            if (rand > 0) {
                isSelect = true;
            }
        } else if (page.getForms().size() > 0) {
            isSelect = true;
        } else if (page.getInputs().size() > 0) {
            isSelect = false;
        } else {
            return false;
        }

        if (isSelect) {
            try {
                HtmlForm form = page.getForms().get(random.nextInt(page.getForms().size()));
                List<HtmlInput> inputs = get_Form_Inputs(form);
                HtmlSubmitInput submit = get_Submit_Input(form);

                for (HtmlInput input : inputs) {
                    input.setValueAttribute(vectorsList.get(random.nextInt(vectorsList.size())));
                }

                HtmlPage html = submit.<HtmlPage>click();
                SensitiveDataLeak(page, html);
            } catch (ElementNotFoundException e) {
            } catch (IOException e) {
            }
        } else {
            String base = mainURL + page.Url_to_String() + "?" + page.getInputs().get(random.nextInt(page.getInputs().size()));
            String input = vectorsList.get(random.nextInt(vectorsList.size()));
            base = base.concat(input);
            try {
                System.out.println("Attempting to access url: " + base);
                HtmlPage html = client.getPage(base);
                SensitiveDataLeak(page, html);
            } catch (FailingHttpStatusCodeException e) {
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }
        }

        return true;
    }

    private void page_tester(Webpage curr_page) {
        for (String input : curr_page.getInputs()) {
            String main = mainURL + curr_page.Url_to_String() + "?" + input + "=";
            for (String s : vectorsList) {
                try {
                    System.out.println("Input Test - Attempting to access: " + main + s);
                    HtmlPage html = client.getPage(main + s);
                    SensitiveDataLeak(curr_page, html);
                } catch (FailingHttpStatusCodeException e) { 
                } catch (MalformedURLException e) {
                } catch (IOException e) {
                }
            }
        }

        for (HtmlForm form : curr_page.getForms()) {
            try {
                List<HtmlInput> htmlInputs = get_Form_Inputs(form);
                HtmlSubmitInput submit = get_Submit_Input(form);
                for (String input_tested : vectorsList) {
                    for (HtmlInput htmlInput : htmlInputs) {
                        htmlInput.setValueAttribute(input_tested);
                    }
                    try {
                        System.out.println("Input Test: " + curr_page.Url_to_String());
                        HtmlPage html = submit.<HtmlPage>click();
                        SensitiveDataLeak(curr_page, html);
                    } catch (IOException e) {
                        System.err.println("Failed: " + e.getMessage());
                    }
                }
            } catch (ElementNotFoundException e) {
                System.err.println("No submit button for: " + curr_page.Url_to_String());
            }
        }
    }

	
	public ArrayList<String> loadCommonWordsFile(String fileName){
		Scanner input;
		try {
			input = new Scanner(new File(fileName));
			while (input.hasNext()) {
				String s = input.nextLine();
                commonWords.add(s);
                commonWords.add(s + ".php");
                commonWords.add(s + ".jsp");
			}
		} catch (FileNotFoundException e) {
			System.err.println("common words file error: " + e.getMessage());
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
            System.err.println("sensitive data file error: " + e.getMessage());
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
            System.err.println("sensitive data file error: " + e.getMessage());
        }
        return vectorsList;
    }
	
	private void discoverForms(){
		System.out.println("\nDiscovering all forms from discovered pages.");
		//find all forms from each page discovered
		for (Webpage p: pages){
			try{
				HtmlPage html = client.getPage(this.mainURL + "/" + p.getUrl());
                checkForBadData(p, html);
				List<HtmlForm> forms = html.getForms();
				for (HtmlForm form: forms){
					p.getForms().add(form);
				}
			} catch (FailingHttpStatusCodeException e){
				System.err.println("DISCOVER - The URL guessed was invalid: " + e.getMessage());
			} catch (MalformedURLException e){
				System.err.println("DISCOVER - The URL guessed violated URL convention: " + e.getMessage());
			} catch (IOException e){
				System.err.println("DISCOVER - Error during page guessing: " + e.getMessage());
			}
		}
	}
	
	private Webpage parseURL(String url){
		Webpage page = null;
		try{
			URL temp = new URL(url);
			//if page is already in the list
			if (pages.contains(new Webpage(temp.getPath()))) {
				for (Webpage p: pages){
					if (p.getUrl().equals(temp.getPath())) {
						page = p;
						break;
					}
				}
			}
			else {
				page = new Webpage(temp.getPath());
				pages.add(page);
			}
			//get query and find fizzed inputs
			if (temp.getQuery() != null) {
				for (String query: temp.getQuery().split("&")) {
					String input = query.split("=")[0];
					if (!page.getInputs().contains(input)) {
						page.getInputs().add(input);
					}
				}
			}
		} catch (MalformedURLException e) {
			System.err.println("Invalid URL provided: " + e.getMessage());
		}
		return page;
	}

    private List<HtmlInput> parseDOM(DomNode node){
        List<HtmlInput> inputs = new ArrayList<HtmlInput>();

        for (DomNode n : node.getChildren()) {
            if( n instanceof HtmlTextInput){
                inputs.add((HtmlInput) n);
            }else if(n instanceof HtmlHiddenInput){
                inputs.add((HtmlInput) n);
            }else if(n instanceof HtmlFileInput){
                inputs.add((HtmlInput) n);
            }else if(n instanceof HtmlPasswordInput){
                inputs.add((HtmlInput) n);
            }else if(n instanceof HtmlImageInput){
                inputs.add((HtmlInput) n);
            }

            if (n.hasChildNodes()){
                inputs.addAll(parseDOM(n));
            }
        }

        return inputs;
    }

    private void checkForBadData(Webpage page, HtmlPage content){
        for(String s: sensitiveData){
            if(content.asXml().contains(s) && !page.getSensitiveData().contains(s)){
                page.getSensitiveData().add(s);
            }
        }
    }

    private void testStart(){
        if(!status){
            for(Webpage p: pages) {
                page_tester(p);
            }
        } else {
            for(int i = 0; i < 10; i++){
                int page = random.nextInt(pages.size());
                testRandPage(pages.get(page));
            }
        }
    }
	public void fuzz(String baseUrl, String auth){
        this.mainURL = baseUrl;
        URL newURL = null;
        try {
			newURL = new URL(baseUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Starting discovery process for: " + baseUrl);
		try {
			newURL.openConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loginDVWA();
		discoverPages();
		discoverForms();
        testStart();
		print();
        client.closeAllWindows();
	}
}


