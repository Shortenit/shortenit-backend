package edu.au.life.shortenit.util;

import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

    // device type
    public String getDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }

        String ua = userAgent.toLowerCase();

        // Check for mobile devices
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipod")
        || ua.contains("blackberry") || ua.contains("window phone")) {
            return "mobile";
        }

        // Check for tablets
        if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("android") || ua.contains("mobile")) {
            return "tablet";
        }

        return "Desktop";
    }

    // browser type
    public String getBrowser(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknow";
        }

        String ua = userAgent.toLowerCase();

        // Check in order of specificity
        if (ua.contains("edg/")) {
            return "Edge";
        } else if (ua.contains("chrome/") && !ua.contains("edg")) {
            return "Chrome";
        } else if (ua.contains("firefox/")) {
            return "Firefox";
        } else if (ua.contains("safari/") && !ua.contains("chrome")) {
            return "Safari";
        } else if (ua.contains("opera") || ua.contains("opr/")) {
            return "Opera";
        } else if (ua.contains("msie") || ua.contains("trident/")) {
            return "Internet Explorer";
        }

        return "Other";
    }

    public String getOperatingSystem(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }

        String ua = userAgent.toLowerCase();

        if (ua.contains("windows nt 10")) {
            return "Windows 10";
        } else if (ua.contains("windows nt 6.3")) {
            return "Windows 8.1";
        } else if (ua.contains("windows nt 6.2")) {
            return "Windows 8";
        } else if (ua.contains("windows nt 6.1")) {
            return "Windows 7";
        } else if (ua.contains("windows")) {
            return "Windows";
        } else if (ua.contains("mac os x")) {
            return "macOS";
        } else if (ua.contains("linux")) {
            return "Linux";
        } else if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad")) {
            return "iOS";
        }

        return "Other";
    }
}
