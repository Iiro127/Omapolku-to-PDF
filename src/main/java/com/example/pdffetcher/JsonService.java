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
import java.util.List;
import java.util.UUID;

public class JsonService {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static StringBuilder questions = new StringBuilder();
    private static StringBuilder allHtml = new StringBuilder();
    private static final ArrayList<String> seenQuestions = new ArrayList<>();


    public static String generatePdf(String cookie, String contentApiUrl, Integer languageCode) throws Exception {
        String filename;

        for (String taskId : getAllTaskIds(contentApiUrl, cookie)) {
            questions.setLength(0);
            seenQuestions.clear();

            String finnishContent = getFinnishContent(
                    "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttask/" + taskId,
                    cookie,
                    languageCode
            ).replaceAll("(?i)<img[^>]*>", "");

            allHtml.append(finnishContent).append("<div style='page-break-after: always;'></div>");
        }

        // Wrap all content into a single HTML document
        String htmlDoc = """
        <!DOCTYPE html>
        <html xmlns="http://www.w3.org/1999/xhtml">
          <head>
            <meta charset="UTF-8"/>
            <title>Therapy Export</title>
            <style>
              @page { margin: 1.5cm; }
              body { font-family: sans-serif; line-height: 1.4; }
              h1, h2, h3 { color: #333333; }
              .page-break { page-break-after: always; }
            </style>
          </head>
          <body>
            %s
          </body>
        </html>
        """.formatted(allHtml.toString());

        htmlDoc = htmlDoc
                .replace("&ouml;", "ö")
                .replace("&auml;", "ä")
                .replace("&Ouml;", "Ö")
                .replace("&Auml;", "Ä")
                .replace("&aring;", "å")
                .replace("&Aring;", "Å");

        htmlDoc = StringEscapeUtils.unescapeHtml4(htmlDoc);

        filename = "Terapia_" + UUID.randomUUID() + ".pdf";
        try (FileOutputStream os = new FileOutputStream(filename)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlDoc, null);
            builder.toStream(os);
            builder.run();
            System.out.println("PDF generated successfully: " + filename);
        } catch (IOException e) {
            throw new RuntimeException("Error while generating PDF", e);
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
     * @param cookie
     * @return
     * @throws Exception
     */
    private static String getFinnishContent(String contentApiUrl, String cookie, Integer languageCode) throws Exception {
        StringBuilder content = new StringBuilder();
        JsonNode root = mapper.readTree(getJson(contentApiUrl, cookie));

        JsonNode contents = root.path("localizedContentObject").path("content");
        JsonNode title = root.path("localizedTitleObject").path("content");
        JsonNode ingress = root.path("localizedIngressObject").path("content");

        boolean hasVideo = root.toString().contains("iframe") || root.toString().contains("ckeditor-html5-video");
        if (hasVideo) {
            content.append("<i>---Sivu sisältää videon---</i> <br/><br/>");
        }


        handleJson(content, title, "title", languageCode);
        handleJson(content, ingress, "ingress", languageCode);
        handleJson(content, contents, "content", languageCode);

        if (content.toString().isEmpty()) {
            throw new RuntimeException("PDF on tyhjä.");
        }

        String questionnaireId = root.path("questionnaireOriginalReference").asText();

        System.out.println("Questionnaire id: " + questionnaireId);

        if (questionnaireId != null) {
            String q = getQuestions(questionnaireId, cookie, languageCode);
            content.append("\n").append(q);
        }

        return content.toString();
    }


    private static void handleJson(StringBuilder contents, JsonNode content, String type, Integer languageCode){
        if (content.isArray()) {
            for (JsonNode node : content) {
                if (node.path("languageCode").asInt() == languageCode) {
                    switch (type){
                        case "title":
                            contents.append("<b>" + node.path("content").asText() + "</b><br/>");
                            break;
                        case "ingress":
                            contents.append("<i>" + node.path("content").asText() + "</i><br/>");
                            break;
                        case "content":
                            contents.append(stripUnsupportedTags(node.path("content").asText()));
                            break;
                    }
                    contents.append(StringEscapeUtils.unescapeHtml4(node.path("content").asText()));
                    break;
                }
            }
        }
    }

    private static String getQuestions(String questionnaireId, String cookie, Integer languageCode) throws Exception {
        JsonNode root = mapper.readTree(getJson(
                "https://omapolku.terveyskyla.fi/api/questionnaires/getquestionnairebyoriginalreference/" + questionnaireId,
                cookie
        ));

        JsonNode pages = root.path("pages");
        StringBuilder out = new StringBuilder();

        out.setLength(0);

        String questionnaireTitle = getLocalizedText(root.path("localizedTitle"), languageCode);
        if (questionnaireTitle != null && !questionnaireTitle.isBlank()) {
            System.out.println("Loaded questionnaire: " + questionnaireTitle);
            out.append("<br/><i>--- Sisältää kyselyn: ")
                    .append(questionnaireTitle)
                    .append(" ---</i><br/>");
        }

        if (!pages.isArray() || pages.isEmpty()) {
            return "Kysymyksiä ei löytynyt.\n";
        }

        for (JsonNode page : pages) {
            handleQuestionsJson(out, page, languageCode);
        }

        out.append(questions.toString());
        return out.toString();
    }


    private static void handleQuestionsJson(StringBuilder out, JsonNode page, Integer languageCode) {
        JsonNode jsonQuestions = page.path("questions");
        if (jsonQuestions.isArray()) {
            for (JsonNode question : jsonQuestions) {
                String questionTitle = getLocalizedText(question.path("localizedTitle"), languageCode);
                if (questionTitle == null || questionTitle.isBlank()) {
                    questionTitle = "Kysymys (ei otsikkoa)";
                }
                if (!seenQuestions.contains(questionTitle)) {
                    seenQuestions.add(questionTitle);
                    out.append("<b>Kysymys:</b> ").append(questionTitle).append("<br/>");

                    JsonNode options = question.path("options");
                    if (options.isArray()) {
                        for (JsonNode opt : options) {
                            String optHeader = getLocalizedText(opt.path("localizedHeader"), languageCode);
                            if (optHeader == null || optHeader.isBlank()) {
                                optHeader = opt.path("header").asText("");
                            }
                            if (!optHeader.isBlank()) {
                                out.append("&nbsp;&nbsp;- ").append(optHeader).append("<br/>");
                            }
                        }
                    }
                    out.append("<br/>");
                }
            }
        }
    }

    private static String getLocalizedText(JsonNode array, Integer languageCode) {
        if (array != null && array.isArray()) {
            for (JsonNode loc : array) {
                if (loc.path("languageCode").asInt() == languageCode) {
                    String content = loc.path("content").asText(null);
                    if (content != null && !content.isBlank()) {
                        return content;
                    }
                }
            }
        }
        return null;
    }

    public static List<String> getAllTaskIds(String structureApiUrl, String cookie) throws Exception {
        String json = getJson(structureApiUrl, cookie);
        JsonNode root = mapper.readTree(json);

        List<String> taskIds = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode taskNode : root) {
                String taskId = taskNode.path("taskId").asText(null);
                if (taskId != null && !taskId.isBlank()) {
                    taskIds.add(taskId);
                }
            }
        }

        return taskIds;
    }


    private static String stripUnsupportedTags(String html) {
        return html.replaceAll("(?i)<img[^>]*>", "")
                .replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "");
    }
}
