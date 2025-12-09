package edu.au.life.shortenit.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoIpService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEO_API_URL = "https://ip-api.com/json/";

    public GeoLocation getGeoLocation(String ipAddress) {
        // Handle localhost/private IPs
        if (ipAddress == null || ipAddress.isEmpty() ||
                ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1") ||
                ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") ||
                ipAddress.equals("::1")) {
            return new GeoLocation("Unknown", "Unknown");
        }

        try {
            String url = GEO_API_URL + ipAddress;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("status") && "success".equals(jsonNode.get("status").asText())) {
                String country = jsonNode.has("country") ? jsonNode.get("country").asText() : "Unknown";
                String city = jsonNode.has("city") ? jsonNode.get("city").asText() : "Unknown";

                return new GeoLocation(country, city);
            }
        } catch (Exception e) {
            log.error("Error getting geo location for IP: {}", ipAddress, e);
        }

        return new GeoLocation("Unknown", "Unknown");
    }

    public static class GeoLocation {
        private final String country;
        private final String city;

        public GeoLocation(String country, String city) {
            this.country = country;
            this.city = city;
        }

        public String getCountry() {
            return country;
        }

        public String getCity() {
            return city;
        }
    }
}
