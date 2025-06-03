package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.Gson;
import it.polimi.tiw.projects.beans.User;

@WebServlet("/api/checkAuth")
public class CheckAuthServletRIA extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false); // Do not create session if it doesn't exist

        Map<String, Object> responseMap = new HashMap<>();
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            responseMap.put("status", "success");
            responseMap.put("data", user.toJSON());
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            responseMap.put("status", "error");
            responseMap.put("message", "No active session found.");
            // It's common to return 200 OK with a specific payload for "not authenticated" 
            // for checkAuth, rather than a 401, to simplify client logic.
            // Alternatively, could return SC_UNAUTHORIZED (401).
            response.setStatus(HttpServletResponse.SC_OK); 
        }
        response.getWriter().write(gson.toJson(responseMap));
    }
    
    // No init or destroy needed as there's no direct DB connection for this servlet
}
