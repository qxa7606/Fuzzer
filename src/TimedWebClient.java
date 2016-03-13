import com.gargoylesoftware.htmlunit.WebClient;
import java.io.IOException;
import java.net.MalformedURLException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class TimedWebClient extends WebClient {
	private boolean allowRequest;
	private long slowTime;
	private static final long serialVersionUID = 533234567L;
	
	
	public TimedWebClient(long time) {
		super();
		allowRequest = true;
		slowTime = time;
	}

	@SuppressWarnings("unchecked")
	public HtmlPage getPage(String url) throws FailingHttpStatusCodeException,
				MalformedURLException, IOException {
		
		while(!getAllowRequest()); //while TimedClient is in use, wait
		
		startTimer();
		return super.getPage(url);
	}
	
	public synchronized boolean getAllowRequest() {
		return allowRequest;
	}
	
	public synchronized void setAllowRequest(boolean canRequest) {
		allowRequest = canRequest;
	}
	
	private void startTimer() {
		setAllowRequest(false);
		
		Timer timer = new Timer(this, slowTime);
		Thread thread = new Thread(timer);
		
		thread.start();
	}
}

class Timer implements Runnable {
	
	TimedWebClient client;
	Long time;
	
	public Timer(TimedWebClient client, Long time) {
		this.client = client;
		this.time = time;
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(time);
		}catch(InterruptedException e) {
			System.err.println(e.getMessage());
		}
		
		client.setAllowRequest(true);
	}
}

