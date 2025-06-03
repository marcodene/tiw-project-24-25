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

@WebServlet("/api/logout")
public class LogoutServletRIA extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false); // Do not create session if it doesn't exist

        if (session != null) {
            session.invalidate();
        }

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("message", "Logout successful.");
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(responseMap));
    }
    
    // No init or destroy needed as there's no direct DB connection for this servlet
}
