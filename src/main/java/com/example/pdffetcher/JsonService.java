package com.example.pdffetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.commons.text.StringEscapeUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.UUID;

public class JsonService {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static StringBuilder questions = new StringBuilder();
    private static StringBuilder finnishContent = new StringBuilder();
    private static final ArrayList<String> seenQuestions = new ArrayList<>();


    public static String generatePdf(String cookie, String contentApiUrl, String questionApiUrl, Boolean hasVideo, Integer languageCode) throws Exception {
        finnishContent.setLength(0);
        String finnishContent = getFinnishContent(contentApiUrl, cookie, questionApiUrl, hasVideo, languageCode);

        // Openhtmltopdf wrapping
        String htmlDoc = """
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
              <head>
                <meta charset="UTF-8"/>
                <title>Therapy Export</title>
              </head>
              <body>
                %s
              </body>
            </html>
            """.formatted(finnishContent);

        String filename = "Terapia_" + UUID.randomUUID() + ".pdf";
        try (FileOutputStream os = new FileOutputStream(filename)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlDoc, null);
            builder.toStream(os);
            builder.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return filename;
    }

    /**
     * Fetches raw JSON from API
     *
     * @param url API-endpoint
     * @param cookie cookie
     * @return JSON as string
     * @throws Exception error
     */
     private static String getJson(String url, String cookie) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Cookie", cookie)
                .build();

        HttpResponse<String> response = JsonService.client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch JSON. HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Gets all finnish content from JSON
     *
     * @param contentApiUrl
     * @param questionApiUrl
     * @param cookie
     * @return
     * @throws Exception
     */
    private static String getFinnishContent(String contentApiUrl, String cookie, String questionApiUrl, Boolean hasVideo, Integer languageCode) throws Exception {
        JsonNode root = mapper.readTree(getJson(contentApiUrl, cookie));
        JsonNode contents = root.path("treatmentTask")
                .path("localizedContentObject")
                .path("content");
        JsonNode title = root.path("treatmentTask")
                .path("localizedTitleObject")
                .path("content");
        JsonNode ingress = root.path("treatmentTask")
                .path("localizedIngressObject")
                .path("content");

        if (hasVideo) {
            finnishContent.append("<i>---Sivu sisältää videon---</i> <br/><br/>");
        }

        handleJson(title, "title", languageCode);
        handleJson(ingress, "ingress", languageCode);
        handleJson(contents, "content", languageCode);

        if (finnishContent.toString().isEmpty()) {
            throw new RuntimeException("PDF on tyhjä.");

        }

        if (!questionApiUrl.trim().isEmpty()){
            String q = getQuestions(questionApiUrl, cookie);
            finnishContent.append("\n").append(q);
        }

        return finnishContent.toString();
    }

    private static void handleJson(JsonNode content, String type, Integer languageCode){
        if (content.isArray()) {
            for (JsonNode node : content) {
                if (node.path("languageCode").asInt() == languageCode) {
                    switch (type){
                        case "title":
                            finnishContent.append("<b>" + node.path("content").asText() + "</b><br/>");
                            break;
                        case "ingress":
                            finnishContent.append("<i>" + node.path("content").asText() + "</i><br/>");
                            break;
                        case "content":
                            finnishContent.append(node.path("content").asText());
                            break;
                    }
                    finnishContent = new StringBuilder(StringEscapeUtils.unescapeHtml4(String.valueOf(finnishContent)));
                    break;
                }
            }
        }
    }

    private static String getQuestions(String apiUrl, String cookie) throws Exception {
        JsonNode root = mapper.readTree(getJson(apiUrl, cookie));
        JsonNode pages = root.path("pages");

        if (!pages.isArray() || pages.isEmpty()) {
            return "Kysymyksiä ei löytynyt.\n";
        }

        for (JsonNode page : pages) {
            handleQuestionsJson(page);
        }

        return questions.toString();
    }

    private static void handleQuestionsJson(JsonNode page){
        JsonNode jsonQuestions = page.path("questions");
        if (jsonQuestions.isArray()) {
            for (JsonNode question : jsonQuestions) {
                String questionTitle = question.path("title").asText("No title");

                if (seenQuestions.contains(questionTitle)){
                    JsonNode localizedTitles = question.path("localizedTitle");
                    if (localizedTitles.isArray() && !localizedTitles.isEmpty()) {
                        for (JsonNode loc : localizedTitles) {
                            int lang = loc.path("languageCode").asInt();
                            if (lang == 1035) {
                                String localizedContent = loc.path("content").asText();
                                if (localizedContent != null && !localizedContent.isEmpty()) {
                                    questionTitle = localizedContent;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    seenQuestions.add(questionTitle);
                }

                questions.append("Kysymys: ").append(questionTitle).append("<br/>").append("<br/>");
            }
        }
    }
}
