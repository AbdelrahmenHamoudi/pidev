package org.example.Entites.user;

public class LocationInfo {
    private String ip;
    private String country_name;
    private String country_code;
    private String region;
    private String city;
    private double latitude;
    private double longitude;
    private String timezone;
    private String currency;
    private String org;

    // Constructeurs
    public LocationInfo() {}

    // Getters et Setters
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getCountryName() { return country_name; }
    public void setCountryName(String country_name) { this.country_name = country_name; }

    public String getCountryCode() { return country_code; }
    public void setCountryCode(String country_code) { this.country_code = country_code; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getIsp() { return org; }
    public void setIsp(String org) { this.org = org; }

    public String getFlag() {
        if (country_code != null && !country_code.isEmpty()) {
            return "https://flagcdn.com/24x18/" + country_code.toLowerCase() + ".png";
        }
        return null;
    }

    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder();
        if (city != null && !city.isEmpty()) sb.append(city);
        if (region != null && !region.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(region);
        }
        if (country_name != null && !country_name.isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(country_name);
        }
        return sb.toString();
    }
}