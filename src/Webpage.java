import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.Page;

public class Webpage {

		private String url;
		private List<HtmlForm> forms;
		private List<String> inputs;
	    private List<String> sensitiveData;
	    private List<String> cookies;
		
		Webpage(String ur){
			this.url = ur;
			forms = new ArrayList<HtmlForm>();
			inputs = new ArrayList<String>();
	        sensitiveData = new ArrayList<String>();
	        cookies = new ArrayList<String>();
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String s) {
			this.url = s;
		}
		
		public List<HtmlForm> getForms() {
			return forms;
		}

		public List<String> getInputs() {
			return inputs;
		}

		public List<String> getSensitiveData() {
			return sensitiveData;
		}
		
		public boolean equals(Object o){
			if (o instanceof Page){
				if (url.equals(((Page) o).getUrl())){
					return true;
				}
			}
			return false;
		}		
}
