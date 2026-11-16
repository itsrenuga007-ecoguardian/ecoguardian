
package com.ecoguardian.ecoguardian;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AuthGuardFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Public pages and authentication endpoints
        if (path.equals("/")
                || path.equals("/index.html")
                || path.equals("/login.html")
                || path.equals("/register.html")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/favicon.ico")
                || path.equals("/error")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || method.equals("OPTIONS")) {

            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);

        Object userId = session == null
                ? null : session.getAttribute("userId");

        Object roleObject = session == null
                ? null : session.getAttribute("userRole");

        if (userId == null || roleObject == null) {
            deny(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please sign in to continue.");
            return;
        }

        String role = roleObject.toString();

        // Protect dashboard pages
        if (path.equals("/admin.html") && !role.equals("ADMIN")) {
            deny(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Admin access required.");
            return;
        }

        if (path.equals("/worker.html") && !role.equals("WORKER")) {
            deny(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Worker access required.");
            return;
        }

        if ((path.equals("/report.html") || path.equals("/rewards.html"))
                && !role.equals("USER")) {
            deny(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "User access required.");
            return;
        }

        // Protect report API operations by role
        if (path.equals("/api/reports")) {
            boolean allowed =
                    (method.equals("POST") && role.equals("USER"))
                    || (method.equals("GET")
                        && Set.of("ADMIN", "WORKER").contains(role));

            if (!allowed) {
                deny(request, response, HttpServletResponse.SC_FORBIDDEN,
                        "You do not have permission for this operation.");
                return;
            }
        }

        if (path.matches("/api/reports/\\d+/assign")
                && (!method.equals("PUT") || !role.equals("ADMIN"))) {
            deny(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Only an admin can assign workers.");
            return;
        }

        if ((path.matches("/api/reports/\\d+/start")
                || path.matches("/api/reports/\\d+/complete"))
                && (!method.equals("PUT") || !role.equals("WORKER"))) {
            deny(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Only a worker can perform this operation.");
            return;
        }

        // Rewards: users may view; admins may manage.
        if (path.equals("/api/rewards")) {
            boolean allowed =
                    (method.equals("GET")
                        && Set.of("USER", "ADMIN").contains(role))
                    || (method.equals("POST") && role.equals("ADMIN"));

            if (!allowed) {
                deny(request, response, HttpServletResponse.SC_FORBIDDEN,
                        "You do not have permission for this operation.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void deny(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(status);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            Map.of("message", message)));
        } else {
            response.sendRedirect("/login.html");
        }
    }
}
