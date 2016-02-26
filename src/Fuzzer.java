import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;

import java.net.HttpURLConnection;


public class Fuzzer {
	public static void main(String[] args) throws Exception{
		
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		HtmlPage mainpage = null;
    	WebClient client = new WebClient();
    	
        if (args.length < 2 || args.length > 4){
        	System.out.println("Bad command");
        	return;
        }
        
        if (args.length == 2 && args[0].equals("discover")){
        	URL url = new URL(args[1]);
        	if(url.toString() == "http://127.0.0.1/dvwa/login.php"){
        		mainpage = loginDvwa(url);
        	}
        	else{
        		mainpage = client.getPage(url);
        	}
        	linkDiscoveryPage(mainpage);
        	System.out.println();
        	System.out.println();
        	//guessURL(url);
        	HtmlPage pg = client.getPage(url);
        	getInputs(pg);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        }
        else if (args.length == 3 && args[0].equals("discover") && args[2].contains("common")){
        	URL url = new URL(args[1]);
        	linkDiscoveryPage(mainpage);
        	System.out.println();
        	System.out.println();
        	guessURL(url, args[2].substring(args[2].lastIndexOf('=')+1, args[2].length()));
        	System.out.println();
        	System.out.println();
        	HtmlPage pg = client.getPage(url);
        	getInputs(pg);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        }
        else if (args.length == 3 && args[0].equals("discover") && args[2].contains("custom")){
        	URL url = new URL(args[1]);
        	HtmlPage pg1 = loginDvwa(url);
        	url = pg1.getUrl();
        	linkDiscoveryPage(mainpage);
        	System.out.println();
        	System.out.println();
        	//***ParseURL goes below this line***//
        	getInputs(pg1);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        }
        else if (args.length == 4 && args[0].equals("discover")&& args[3].contains("common") && args[2].contains("custom")){
        	URL url = new URL(args[1]);
        	System.out.println("LOGIN PAGE INFORMATION");
        	System.out.println();
        	linkDiscoveryPage(mainpage);
        	System.out.println();
        	System.out.println();
        	guessURL(url, args[3].substring(args[3].lastIndexOf('=')+1, args[3].length()));
        	System.out.println();        	
        	System.out.println();
        	
        	
        	HtmlPage pg = client.getPage(url);
        	getInputs(pg);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        	System.out.println();
        	System.out.println();
        	System.out.println("DVWA MAIN PAGE INFORMATION");
        	HtmlPage pg1 = loginDvwa(url);
        	url = pg1.getUrl();
        	linkDiscoveryPage(mainpage);
        	System.out.println();
        	System.out.println();
        	guessURL(url, args[3].substring(args[3].lastIndexOf('=')+1, args[3].length()));
        	System.out.println();        	
        	System.out.println();
        	//***ParseURL goes below this line***//
        	getInputs(pg1);
        	System.out.println();
        	System.out.println();
        	getCookies(url);
        }
	}

	// custom login to loginDvwa
	public static final HtmlPage loginDvwa(URL url) throws Exception{
		WebClient client = new WebClient();
        client.getCookieManager().setCookiesEnabled(true);
       	final HtmlPage page = client.getPage(url);
       	@SuppressWarnings("unchecked")
		final ArrayList<HtmlForm> forms = (ArrayList<HtmlForm>) page.getByXPath("//form");
       	HtmlForm form = forms.get(0);
        final HtmlTextInput textField =  form.getInputByName("username");
        final HtmlPasswordInput pwd =  form.getInputByName("password");  
        textField.setValueAttribute("admin");
        pwd.setValueAttribute("password");
        System.out.println("Logged in");
        final HtmlPage pg = (HtmlPage) form.getInputByName("Login").click();
        
    	HtmlPage ppg = client.getPage(pg.getUrl());
    	
		List<HtmlAnchor> lst = ppg.getAnchors();
		for (HtmlAnchor an : lst){
			System.out.println(an.getHrefAttribute());
		}
    	
        return pg;    
	}
	
	
	public static HtmlPage loginBodgeit(URL url){
		return null;
	}
	
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
			String ss = anc.getHrefAttribute();
			if(page.getUrl().toString().charAt(page.getUrl().toString().length()-1) != '/'){
				urls.add(new URL(page.getUrl() + ss));
			}
			else {
				urls.add(new URL(page.getUrl() + ss.substring(1, ss.length())));
			}
		}
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
}
