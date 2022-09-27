package com.sdmcraft.smarttagstranslation.core.workflow;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.google.gson.*;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.mail.internet.ContentType;

@Component
public class TranslateSmartTags implements WorkflowProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslateSmartTags.class);

    private CloseableHttpClient httpClient;

    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @Activate
    protected void activate(Map<String, Object> properties) {
        HttpClientBuilder builder = httpClientBuilderFactory.newBuilder().setMaxConnTotal(10);
        if (builder != null) {
            httpClient = builder.build();
        }
    }

    @Deactivate
    protected void deactivate() {
        try {
            httpClient.close();
            LOGGER.debug("Closed http client");
        } catch (IOException e) {
            LOGGER.error("Error while closing client", e); // Ignore
        }
    }

    protected String postToServer(HttpEntity body,
            String serverUrl, CloseableHttpClient httpClient,
            List<Header> headers) throws IOException {
        HttpPost post = new HttpPost(serverUrl);

        if (body != null) {
            post.setEntity(body);
        }

        return request(post, headers);
    }

    private String request(HttpUriRequest request, List<Header> headers) throws IOException {
        if (headers != null) {
            for (Header header : headers) {
                request.addHeader(header);
            }
        }
        long start = System.currentTimeMillis();
        CloseableHttpResponse resp = null;
        try {
            resp = httpClient.execute(request);
            int statusCode = resp.getStatusLine().getStatusCode();
            LOGGER.debug("Request to {} got status {} and took {} ms", request.getURI(), statusCode,
                    System.currentTimeMillis() - start);
            if (statusCode != HttpStatus.SC_OK) {
                throw new IOException("Service returned an error: " +
                        resp.getStatusLine().toString());
            }
            return EntityUtils.toString(resp.getEntity());
        } finally {
            if (resp != null) {
                try {
                    resp.close();
                } catch (IOException e) {
                    LOGGER.warn("Error while closing response", e);
                }
            }
        }
    }

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
            throws WorkflowException {
        try {
            String targetLang = metaDataMap.get("TARGET_LANGUAGE", "en");
            String subscriptionKey = metaDataMap.get("SUBSCRIPTION_KEY", null);
            String location = metaDataMap.get("LOCATION", null);
            List<Header> headers = new ArrayList<>();
            headers.add(new BasicHeader("Ocp-Apim-Subscription-Key", subscriptionKey));
            headers.add(new BasicHeader("Ocp-Apim-Subscription-Region", location));
            Node node = workflowSession.getSession().getNode(workItem.getWorkflowData().getPayload().toString());
            Node predictedTags = node.getNode("jcr:content/metadata/predictedTags");
            NodeIterator itr = predictedTags.getNodes();
            while(itr.hasNext()) {
                Node tag = itr.nextNode();
                String name = tag.getProperty("name").getString();
                StringEntity entity = new StringEntity("[{\"Text\": \"" + name + "\"}]");
                entity.setContentType("application/json");
                String resp = this.postToServer(entity,
                        "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=en&to=" + targetLang,
                        httpClient, headers);
                JsonArray arr = new Gson().fromJson(resp, JsonArray.class);
                JsonObject obj = arr.get(0).getAsJsonObject();
                JsonArray translations = obj.getAsJsonArray("translations");
                JsonObject translation = (JsonObject)translations.get(0);
                String translatedText = translation.get("text").getAsString();

                tag.setProperty("name", translatedText);
            }
        } catch (Exception ex) {
            throw new WorkflowException(ex);
        }
    }
}