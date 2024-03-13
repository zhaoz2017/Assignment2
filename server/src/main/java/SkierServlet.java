import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet(name = "SkierServlet", value = "/SkierServlet")
public class SkierServlet extends HttpServlet {
  private static final Gson gson = new Gson();
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final int STATUS_NOT_FOUND = HttpServletResponse.SC_NOT_FOUND;
  private static final int STATUS_BAD_REQUEST = HttpServletResponse.SC_BAD_REQUEST;
  private static final int STATUS_OK = HttpServletResponse.SC_OK;
  private static final int STATUS_CREATED = HttpServletResponse.SC_CREATED;

  private ConnectionFactory factory;
  private String TASK_QUEUE_NAME = "task_queue";

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    factory = new ConnectionFactory();
    factory.setHost("localhost");
    factory.setPort(5672);
    factory.setUsername("admin");
    factory.setPassword("passw0rd");
  }

  // Servlet methods
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType(CONTENT_TYPE_JSON);
    String urlPath = request.getPathInfo();

    if (isPathInvalid(urlPath)) {
      sendResponse(response, "Missing parameters", STATUS_NOT_FOUND);
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isUrlValid(urlParts)) {
      sendResponse(response, "Invalid URL", STATUS_NOT_FOUND);
    } else {
      sendResponse(response, "It works!", STATUS_OK);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String[] pathParams = request.getPathInfo().split("/");
    LiftRide liftRide = parseLiftRide(pathParams, request);

    if (!isValidPathParams(pathParams)) {
      sendResponse(response, "Invalid path parameters", STATUS_BAD_REQUEST);
    } else if (handlePostLogic(liftRide)){
      sendResponse(response, "Lift ride details stored successfully.", STATUS_CREATED);
      sendMessage(liftRide);
    }

  }

  // Helper methods
  private boolean isPathInvalid(String urlPath) {
    return urlPath == null || urlPath.isEmpty();
  }

  private boolean isValidPathParams(String[] pathParams) {
    return pathParams.length >= 8;
  }

  private boolean handlePostLogic(LiftRide liftRide) {
    int resortId = Integer.parseInt(liftRide.getResortId());
    if (resortId < 1 || resortId > 10) {
      return false;
    } else if (!liftRide.getSeasonId().equals("2024")) {
      return false;
    } else if (!liftRide.getDayId().equals("1")) {
      return false;
    } else if (liftRide.getSkierId() < 1 || liftRide.getSkierId() > 100000) {
      return false;
    } else if (liftRide.getTime() < 1 || liftRide.getTime() > 360) {
      return false;
    } else {
      return liftRide.getLiftId() >= 1 && liftRide.getLiftId() <= 40;
    }
  }

  private boolean isUrlValid(String[] urlPath) {
    // Implement specific validation logic
    if (urlPath.length < 8 || !urlPath[0].isEmpty() || !urlPath[2].equals("seasons")
        || !urlPath[4].equals("days") || !urlPath[6].equals("skiers")) {
      return false;
    }
    return true; // Simplified for demonstration
  }

  private LiftRide parseLiftRide (String[] urlPath, HttpServletRequest request) throws IOException {
    BufferedReader reader = request.getReader();
    StringBuilder stringBuilder = new StringBuilder();

    String line;
    while ((line = reader.readLine()) != null) {
      stringBuilder.append(line);
      stringBuilder.append(System.lineSeparator());
    }
    LiftRide liftRide = gson.fromJson(stringBuilder.toString(), LiftRide.class);
    int liftId = liftRide.getLiftId();
    int time = liftRide.getTime();
    String resortId = urlPath[1];
    String seaSonsId = urlPath[3];
    String dayId = urlPath[5];
    int skierId = Integer.parseInt(urlPath[7]);

    return new LiftRide(time, liftId, resortId, seaSonsId, dayId, skierId);
  }

  private void sendResponse(HttpServletResponse response, String message, int status) throws IOException {
    response.setStatus(status);
    response.setContentType(CONTENT_TYPE_JSON);
    response.getWriter().print(gson.toJson("{\"message\": \"" + message + "\"}"));
    response.getWriter().flush();
  }

  private void sendMessage(LiftRide liftRide) {
    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {
      channel.queueDeclare(TASK_QUEUE_NAME, false, false, false, null);
      channel.basicPublish("", TASK_QUEUE_NAME, null, gson.toJson(liftRide, LiftRide.class).getBytes());
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

}
