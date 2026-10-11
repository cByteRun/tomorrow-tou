package edu.tou.shared;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public class ApiClient {
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private final ObjectMapper json = new ObjectMapper();
  private final String base;
  private volatile String token = "";

  public ApiClient() {
    base =
        System.getProperty(
            "tou.api",
            System.getenv()
                .getOrDefault(
                    "TOU_API_URL",
                    "http://localhost:" + System.getenv().getOrDefault("API_PORT", "18080")));
  }

  public void token(String value) {
    token = value;
  }

  public String base() {
    return base;
  }

  public Object request(String method, String path, Object body) throws Exception {
    byte[] bytes = download(method, path, body);
    return bytes.length == 0 ? null : json.readValue(bytes, Object.class);
  }

  public byte[] download(String method, String path, Object body) throws Exception {
    var response =
        http.send(buildRequest(method, path, body), HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() >= 400) {
      String message;
      try {
        message =
            json.readValue(response.body(), new TypeReference<Map<String, Object>>() {})
                .getOrDefault("message", "Ошибка API")
                .toString();
      } catch (Exception e) {
        message = "Сервер вернул ошибку " + response.statusCode();
      }
      throw new ApiException(response.statusCode(), message);
    }
    return response.body();
  }

  HttpRequest buildRequest(String method, String path, Object body)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    var builder =
        HttpRequest.newBuilder(URI.create(base + "/api" + path))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json");
    String currentToken = token;
    if (!path.equals("/auth/login") && !currentToken.isBlank())
      builder.header("Authorization", "Bearer " + currentToken);
    builder.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)));
    if (body != null) builder.header("Content-Type", "application/json");
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> object(String path) throws Exception {
    return (Map<String, Object>) request("GET", path, null);
  }

  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> rows(String kind) throws Exception {
    return (List<Map<String, Object>>) request("GET", "/records/" + kind, null);
  }

  public static class ApiException extends RuntimeException {
    public final int status;

    public ApiException(int s, String m) {
      super(m);
      status = s;
    }
  }
}
