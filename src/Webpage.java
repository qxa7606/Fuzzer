import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Webpage {

//		HtmlPage page;
		URL url;
		List<String> inputs;
		
		Webpage(URL ur){
			//this.page = pg;
			this.url = ur;
			this.inputs = new ArrayList<String>();
		}
		
		public void addInput(String st){
			inputs.add(st);
		}
		public ArrayList<String> getInputs(){
			return (ArrayList<String>) inputs;
		}
}
