package org.icometrix.stdsplugins.updater;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class ReleasesDao {
   static final Logger logger = Logger.getLogger(ReleasesDao.class);

	String proxyserver = null;
	int proxyport;
	String host = null;
	int port;
	String protocol = null;
	String baseUrl = null;
	
	public ReleasesDao(String host, String protocol, String baseUrl, int port, 
			           String proxyServer, int proxyPort){
		this(host, protocol, baseUrl, port);
		this.proxyserver = proxyServer;
		this.proxyport = proxyPort;
	}
	
	public ReleasesDao(String host, String protocol, String baseUrl, int port){
		this.host = host;
		this.port = port;
		this.protocol = protocol;
		this.baseUrl = baseUrl;
	}
	
	public CloseableHttpClient initHttpClient(String proxyServer, int proxyPort) {
		logger.debug("initiating httpClient");
		
		CloseableHttpClient httpClient = null;

		//set time out: HOLY CRAP HOW COMPLEX IS THIS
		//http://stackoverflow.com/questions/6024376/apache-httpcomponents-httpclient-timeout
		RequestConfig.Builder requestBuilder = RequestConfig.custom();
		requestBuilder = requestBuilder.setConnectTimeout(120000);
		requestBuilder = requestBuilder.setConnectionRequestTimeout(120000);

		HttpClientBuilder builder = HttpClientBuilder.create();  
		builder.setDefaultRequestConfig(requestBuilder.build());

		if (httpClient == null) {
			if (proxyServer != null && proxyServer != "") {
				HttpHost proxy = new HttpHost(proxyServer, proxyPort);
				httpClient = builder.setProxy(proxy).build();

			} else {
				httpClient = builder.build();
			}
		}
		logger.debug("httpClient is setup");
		return httpClient;
	}
	
	public HttpHost getReleasesHost(){
		return new HttpHost(this.host, this.port, this.protocol);
	}
	
	public String getReleaseTagLatest() throws ClientProtocolException, IOException{
		logger.debug("getting tag");
		String query = this.baseUrl + "releases/latest";
		CloseableHttpClient httpClient = this.initHttpClient(this.proxyserver, this.proxyport);
		HttpHost host = this.getReleasesHost();
		HttpClientContext context = HttpClientContext.create();
		HttpGet httpGet = new HttpGet(query);
		logger.debug("http setup");
		
		// Assuming UTF-8
		String result = this.getHttpResponseAsConsumedString(httpClient, httpGet, host, context);
		logger.debug("got result");
		return new JSONObject(result).getString("tag");
	}
	
	public File getReleaseFileLatest(File targetDir) throws ClientProtocolException, IOException{
		logger.debug("getting file");
		String query = this.baseUrl + "releases/latest/files";
		
		//duplicate code, but let's first wait the API to stabilizes
		CloseableHttpClient httpClient = this.initHttpClient(this.proxyserver, this.proxyport);
		HttpHost host = this.getReleasesHost();
		HttpClientContext context = HttpClientContext.create();
		HttpGet httpGet = new HttpGet(query);
		
		HttpEntity entity =  httpClient.execute(host, httpGet, context).getEntity();
		
		return httpEntityAsFile(entity, targetDir);
	}
	
	private File httpEntityAsFile(HttpEntity entity, File tempDir) throws IOException {
		
		InputStream stream = entity.getContent();
		
		try {
			BufferedInputStream bis = new BufferedInputStream(stream);
			String prefix = "HTTP-";
			File tempFile = File.createTempFile(prefix, ".tmp", tempDir);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile));
			int inByte;

			while ((inByte = bis.read()) != -1) {
				bos.write(inByte);
			}
			bis.close();
			bos.close();
			
			return tempFile;

		} finally {
			stream.close();
		}
	}
	
	/**
	 *  help method to remove boilerplate linked to connection management and connection leakage
	 * see: https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
	 * Returns response as string
	 * Note: it reads in memory careful with big response
	 */
	private String getHttpResponseAsConsumedString(CloseableHttpClient httpClient, HttpRequestBase request, HttpHost targetHost,  HttpClientContext context) 
			                                      throws ClientProtocolException, IOException{
		try{
			HttpResponse response = httpClient.execute(targetHost, request, context);
			return new BasicResponseHandler().handleResponse(response);
		}
		finally{
			request.releaseConnection();
		}
	}
}
