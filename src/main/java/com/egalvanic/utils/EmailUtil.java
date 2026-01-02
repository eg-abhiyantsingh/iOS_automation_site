package com.egalvanic.utils;

import com.egalvanic.constants.AppConstants;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;

/**
 * Email Utility - Send test reports via email
 * 
 * Supports:
 * - SMTP with TLS (Gmail, Outlook, etc.)
 * - HTML email body
 * - Multiple file attachments
 */
public class EmailUtil {

    private EmailUtil() {
        // Private constructor
    }

    /**
     * Send email with attachments
     * 
     * @param to          Recipient email address
     * @param subject     Email subject
     * @param htmlBody    HTML formatted email body
     * @param attachments Array of file paths to attach
     */
    public static void sendEmail(String to, String subject, String htmlBody, String[] attachments) {
        // Validate email configuration before attempting to send
        if (!isEmailConfigValid()) {
            System.err.println("❌ Email configuration is not valid. Skipping email send.");
            System.err.println("   Please set EMAIL_FROM and EMAIL_PASSWORD environment variables or update config.properties");
            return;
        }
        
        // SMTP Properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", AppConstants.SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(AppConstants.SMTP_PORT));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", AppConstants.SMTP_HOST);
        
        // Additional security settings
        props.put("mail.smtp.connectiontimeout", "10000"); // 10 seconds
        props.put("mail.smtp.timeout", "10000"); // 10 seconds
        props.put("mail.smtp.writetimeout", "10000"); // 10 seconds

        // Create session with authentication
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    AppConstants.EMAIL_FROM,
                    AppConstants.EMAIL_PASSWORD
                );
            }
        });

        try {
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(AppConstants.EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // Create multipart message for body + attachments
            Multipart multipart = new MimeMultipart();

            // Add HTML body
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            // Add attachments
            if (attachments != null) {
                for (String filePath : attachments) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        DataSource source = new FileDataSource(file);
                        attachmentPart.setDataHandler(new DataHandler(source));
                        attachmentPart.setFileName(file.getName());
                        multipart.addBodyPart(attachmentPart);
                        System.out.println("📎 Attached: " + file.getName());
                    } else {
                        System.out.println("⚠️ Attachment not found: " + filePath);
                    }
                }
            }

            // Set content
            message.setContent(multipart);

            // Send email
            Transport.send(message);
            System.out.println("✅ Email sent successfully to: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Validate email configuration
     */
    private static boolean isEmailConfigValid() {
        if (AppConstants.EMAIL_FROM == null || AppConstants.EMAIL_FROM.isEmpty() || 
            AppConstants.EMAIL_FROM.equals("your-email@gmail.com")) {
            System.err.println("⚠️ EMAIL_FROM is not configured properly");
            return false;
        }
        
        if (AppConstants.EMAIL_PASSWORD == null || AppConstants.EMAIL_PASSWORD.isEmpty() || 
            AppConstants.EMAIL_PASSWORD.equals("your-app-password")) {
            System.err.println("⚠️ EMAIL_PASSWORD is not configured properly");
            return false;
        }
        
        if (AppConstants.EMAIL_TO == null || AppConstants.EMAIL_TO.isEmpty()) {
            System.err.println("⚠️ EMAIL_TO is not configured properly");
            return false;
        }
        
        return true;
    }

    /**
     * Send simple email without attachments
     */
    public static void sendSimpleEmail(String to, String subject, String htmlBody) {
        sendEmail(to, subject, htmlBody, null);
    }

    /**
     * Send test report email with default settings
     */
    public static void sendTestReportEmail(String detailedReportPath, String clientReportPath) {
        String subject = AppConstants.EMAIL_SUBJECT;
        String body = buildDefaultEmailBody();
        String[] attachments = {detailedReportPath, clientReportPath};
        
        sendEmail(AppConstants.EMAIL_TO, subject, body, attachments);
    }

    /**
     * Build default email body
     */
    private static String buildDefaultEmailBody() {
        StringBuilder body = new StringBuilder();
        body.append("<!DOCTYPE html>");
        body.append("<html>");
        body.append("<head><style>");
        body.append("body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; }");
        body.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }");
        body.append(".content { background: #f8f9fa; padding: 20px; border: 1px solid #ddd; }");
        body.append(".info-table { width: 100%; border-collapse: collapse; margin: 15px 0; }");
        body.append(".info-table td { padding: 10px; border-bottom: 1px solid #eee; }");
        body.append(".info-table td:first-child { font-weight: bold; width: 150px; color: #555; }");
        body.append(".reports-section { margin: 20px 0; padding: 15px; background: white; border-radius: 8px; border: 1px solid #e1e4e8; }");
        body.append(".report-item { padding: 10px; margin: 8px 0; background: #f1f8ff; border-radius: 5px; border-left: 4px solid #0366d6; }");
        body.append(".footer { background: #f1f3f5; padding: 15px; text-align: center; color: #666; font-size: 12px; border-radius: 0 0 10px 10px; }");
        body.append("</style></head>");
        body.append("<body>");
        
        // Header
        body.append("<div class='header'>");
        body.append("<h1 style='margin: 0;'>🧪 eGalvanic iOS Automation</h1>");
        body.append("<p style='margin: 10px 0 0 0;'>Test Execution Report</p>");
        body.append("</div>");
        
        // Content
        body.append("<div class='content'>");
        
        // Info Table
        body.append("<table class='info-table'>");
        body.append("<tr><td>📱 Device</td><td>").append(AppConstants.DEVICE_NAME).append("</td></tr>");
        body.append("<tr><td>📲 Platform</td><td>iOS ").append(AppConstants.PLATFORM_VERSION).append("</td></tr>");
        body.append("<tr><td>📅 Date</td><td>").append(new java.text.SimpleDateFormat("MMMM dd, yyyy HH:mm").format(new java.util.Date())).append("</td></tr>");
        body.append("<tr><td>🔧 Framework</td><td>Appium + TestNG + Page Object Model</td></tr>");
        body.append("</table>");
        
        // Reports Section
        body.append("<div class='reports-section'>");
        body.append("<h3 style='margin-top: 0;'>📊 Attached Reports</h3>");
        body.append("<div class='report-item'>");
        body.append("<strong>📋 Detailed Report</strong><br>");
        body.append("<small>For QA Team - Includes screenshots, logs, and step details</small>");
        body.append("</div>");
        body.append("<div class='report-item'>");
        body.append("<strong>📄 Client Report</strong><br>");
        body.append("<small>For Client - Clean summary with Module > Feature > Test > Pass/Fail</small>");
        body.append("</div>");
        body.append("</div>");
        
        body.append("</div>");
        
        // Footer
        body.append("<div class='footer'>");
        body.append("<p>This is an automated email from eGalvanic iOS Automation Framework.</p>");
        body.append("<p>© 2025 eGalvanic - All Rights Reserved</p>");
        body.append("</div>");
        
        body.append("</body></html>");
        return body.toString();
    }
}
