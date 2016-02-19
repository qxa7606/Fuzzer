import java.io.IOException;
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

}
