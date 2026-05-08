package domain.entities;

import domain.PrinterStatus;

public class Printer {
    private String name;
    private String ipAddress;
    private String hostName;
    private PrinterStatus status;
    private int port;

    public Printer(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.status = PrinterStatus.UNKNOWN;
        this.port = 9100;
    }

    public Printer(String name, String ipAddress, String hostName, PrinterStatus status, int port) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.hostName = hostName;
        this.status = status;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public PrinterStatus getStatus() {
        return status;
    }

    public void setStatus(PrinterStatus status) {
        this.status = status;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}