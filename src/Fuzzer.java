import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;


public class Fuzzer {
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

		HtmlPage pg = loginDvwa(new URL("http://127.0.0.1/dvwa/login.php"));
		//HtmlPage pg = loginDvwa(new URL("http://www.google.com"));
		System.out.println(pg.asText());
	}

	// custom login to loginDvwa
	public static HtmlPage loginDvwa(URL url) throws Exception{
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
            
        return (HtmlPage) form.getInputByName("Login").click();
            
	}
	
	
	public static HtmlPage loginBodgeit(URL url){
		return null;
	}
	
	// all urls you can reach from the initial page
	public static ArrayList<URL> linkDiscovery(URL url) throws FailingHttpStatusCodeException, IOException{
		WebClient client = new WebClient();
		HtmlPage page = client.getPage(url);
		ArrayList<URL> urls = new ArrayList<URL>();
		
		List<HtmlAnchor> lst = page.getAnchors();
		for (HtmlAnchor anc : lst){
			String ss = anc.getHrefAttribute();
			if (url.toString().charAt(url.toString().length()-1) != '/'){
				urls.add(new URL(url + ss));
			}
			else {
				urls.add(new URL(url + ss.substring(1, ss.length())));
			}
		}
		return urls;
	}

	// get all inputs from the page
	public ArrayList<HtmlInput> getInputs(HtmlPage page){
		@SuppressWarnings("unchecked")
		List<HtmlInput> lst = (List<HtmlInput>) page.getByXPath("//input");
		return (ArrayList<HtmlInput>) lst;
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

}
