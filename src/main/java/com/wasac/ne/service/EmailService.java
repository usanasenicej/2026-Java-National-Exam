package com.wasac.ne.service;

import com.wasac.ne.enums.UserRole;
import com.wasac.ne.util.RoleDescriptions;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // =========================================================================
    // CORE SEND — never throws, logs failure only
    // =========================================================================

    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent → {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // Never throw — a failed email must not crash the business operation
        }
    }

    // =========================================================================
    // LEGACY FALLBACK — called by AuthService with plain-text bodies
    // We now redirect these to their proper dedicated HTML methods
    // =========================================================================

    /**
     * Generic fallback. Only used when no dedicated method exists.
     * Wraps plain text in a clean HTML template.
     */
    public void sendEmail(String to, String subject, String plainBody) {
        // Escape % so String.formatted() inside template() doesn't crash
        String safe = plainBody
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("%", "&#37;")
                .replace("\n", "<br>");
        send(to, subject, template(subject, "<p class='body-text'>" + safe + "</p>"));
    }

    // =========================================================================
    // 1. CUSTOMER REGISTRATION — OTP + verification link
    // =========================================================================

    /**
     * Called by AuthService.register() — sends the OTP and verification link.
     * This replaces the old generic sendEmail() call for registration.
     */
    public void sendRegistrationVerificationEmail(String to, String name, String otp, String verifyLink) {
        String content = row("Account Verification") +
            "<p class='body-text'>Dear <strong>" + esc(name) + "</strong>,</p>" +
            "<p class='body-text'>Thank you for registering with the WASAC Utility Billing System. " +
            "Please verify your email address using one of the two options below.</p>" +
            "<div class='divider'></div>" +
            "<p class='label'>Option 1 &mdash; Enter this verification code:</p>" +
            "<div class='otp-box'>" + esc(otp) + "</div>" +
            "<p class='hint'>This code expires in <strong>10 minutes</strong>. Do not share it with anyone.</p>" +
            "<div class='divider'></div>" +
            "<p class='label'>Option 2 &mdash; Click the verification link:</p>" +
            "<div style='text-align:center;margin:20px 0;'>" +
            "  <a href='" + verifyLink + "' class='btn'>Verify My Account</a>" +
            "</div>" +
            "<p class='hint'>This link expires in 24 hours.</p>";

        send(to, "Verify Your WASAC Account", template("Account Verification", content));
    }

    // =========================================================================
    // 2. WELCOME EMAIL
    // =========================================================================

    public void sendWelcomeEmail(String to, String name) {
        String content = row("Welcome") +
            "<p class='body-text'>Dear <strong>" + esc(name) + "</strong>,</p>" +
            "<p class='body-text'>Your WASAC Utility Billing System account has been activated. " +
            "You can now log in and access your account.</p>" +
            "<div class='info-box'>" +
            "  <p class='info-title'>With your account you can:</p>" +
            "  <ul class='info-list'>" +
            "    <li>View your utility bills</li>" +
            "    <li>Track your meter readings</li>" +
            "    <li>Monitor your payment history</li>" +
            "    <li>Receive billing notifications</li>" +
            "  </ul>" +
            "</div>" +
            "<p class='body-text'>If you have any questions, please contact your nearest WASAC office.</p>";

        send(to, "Welcome to WASAC Utility Billing", template("Welcome to WASAC", content));
    }

    // =========================================================================
    // 3. OTP EMAIL (generic — password reset, etc.)
    // =========================================================================

    public void sendOtpEmail(String to, String name, String otp, String purpose) {
        String content = row("Verification Code") +
            "<p class='body-text'>Dear <strong>" + esc(name) + "</strong>,</p>" +
            "<p class='body-text'>You requested a verification code for <strong>" + esc(purpose) + "</strong>.</p>" +
            "<p class='label'>Your verification code:</p>" +
            "<div class='otp-box'>" + esc(otp) + "</div>" +
            "<div class='warning-box'>" +
            "  This code expires in <strong>10 minutes</strong>. Do not share it with anyone." +
            "</div>" +
            "<p class='hint'>If you did not request this code, please ignore this email.</p>";

        send(to, "Your WASAC Verification Code", template("Verification Code", content));
    }

    public void sendPasswordResetEmail(String to, String name, String otp) {
        sendOtpEmail(to, name, otp, "password reset");
    }

    // =========================================================================
    // 4. ACCOUNT VERIFIED CONFIRMATION
    // =========================================================================

    public void sendAccountVerifiedEmail(String to, String name) {
        String content = row("Account Verified") +
            "<p class='body-text'>Dear <strong>" + esc(name) + "</strong>,</p>" +
            "<div class='success-box'>Your WASAC account has been verified successfully. You can now log in.</div>" +
            "<p class='body-text'>If you did not perform this action, please contact our support team immediately.</p>";

        send(to, "WASAC Account Verified", template("Account Verified", content));
    }

    // =========================================================================
    // 5. BILL PAYMENT CONFIRMATION
    // =========================================================================

    public void sendBillNotificationEmail(String to, String name, String message) {
        String content = row("Payment Confirmation") +
            "<p class='body-text'>Dear <strong>" + esc(name) + "</strong>,</p>" +
            "<div class='success-box'>" +
            "  <strong>Payment Successfully Processed</strong><br>" +
            "  <span style='color:#2e7d32;'>" + esc(message).replace("\n", "<br>") + "</span>" +
            "</div>" +
            "<p class='body-text'>Thank you for your payment. Your account balance has been updated.</p>" +
            "<p class='hint'>Please keep this email as your payment receipt.</p>";

        send(to, "WASAC Payment Confirmation", template("Payment Confirmation", content));
    }

    // =========================================================================
    // 6. STAFF ACCOUNT CREDENTIALS (Operator / Finance)
    // =========================================================================

    public void sendAccountCredentialsEmail(String to, String name, String email,
                                            String temporaryPassword, Set<UserRole> roles) {
        String roleNames = RoleDescriptions.formatRoleNames(roles);
        String roleDesc  = RoleDescriptions.describeRoles(roles).replace("\n", "<br>");
        String loginUrl  = baseUrl + "/swagger-ui.html";

        String content = row("Your Account Credentials") +
            "<p class='body-text'>Dear <strong>" + esc(name) + "</strong>,</p>" +
            "<p class='body-text'>An administrator has created your account on the WASAC Utility Billing System. " +
            "Below are your login credentials.</p>" +
            "<table class='cred-table'>" +
            "  <tr><td class='cred-label'>Email Address</td><td class='cred-value'>" + esc(email) + "</td></tr>" +
            "  <tr><td class='cred-label'>Temporary Password</td>" +
            "      <td class='cred-value cred-password'>" + esc(temporaryPassword) + "</td></tr>" +
            "  <tr><td class='cred-label'>Assigned Role</td><td class='cred-value'>" + esc(roleNames) + "</td></tr>" +
            "</table>" +
            "<div class='info-box' style='margin-top:20px;'>" +
            "  <p class='info-title'>Your Responsibilities</p>" +
            "  <p style='margin:6px 0 0;color:#444;font-size:14px;'>" + roleDesc + "</p>" +
            "</div>" +
            "<div class='warning-box'>" +
            "  You must change your temporary password immediately after your first login." +
            "</div>" +
            "<div style='text-align:center;margin:28px 0;'>" +
            "  <a href='" + loginUrl + "' class='btn'>Log In Now</a>" +
            "</div>";

        send(to, "Your WASAC Account Has Been Created", template("Account Credentials", content));
    }

    // =========================================================================
    // 7. ROLE CHANGE NOTIFICATION
    // =========================================================================

    public void sendRoleChangeEmail(String to, String name, Set<UserRole> previousRoles, Set<UserRole> newRoles) {
        String prev    = RoleDescriptions.formatRoleNames(previousRoles);
        String updated = RoleDescriptions.formatRoleNames(newRoles);
        String desc    = RoleDescriptions.describeRoles(newRoles).replace("\n", "<br>");

        String content = row("Role Update") +
            "<p class='body-text'>Dear <strong>" + esc(name) + "</strong>,</p>" +
            "<p class='body-text'>Your role on the WASAC Utility Billing System has been updated by an administrator.</p>" +
            "<table class='cred-table'>" +
            "  <tr><td class='cred-label'>Previous Role</td><td class='cred-value'>" + esc(prev) + "</td></tr>" +
            "  <tr><td class='cred-label'>New Role</td><td class='cred-value cred-highlight'>" + esc(updated) + "</td></tr>" +
            "</table>" +
            "<div class='info-box' style='margin-top:20px;'>" +
            "  <p class='info-title'>Updated Responsibilities</p>" +
            "  <p style='margin:6px 0 0;color:#444;font-size:14px;'>" + desc + "</p>" +
            "</div>" +
            "<div class='warning-box'>If you did not expect this change, contact your administrator immediately.</div>";

        send(to, "Your WASAC Role Has Been Updated", template("Role Update", content));
    }

    // =========================================================================
    // HTML TEMPLATE ENGINE
    // =========================================================================

    /** Escape HTML special characters */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Section row heading inside the card */
    private static String row(String title) {
        return "<p class='section-title'>" + title + "</p>";
    }

    /** Full HTML email template — clean white + light blue, no icons */
    private static String template(String pageTitle, String bodyContent) {
        return "<!DOCTYPE html><html lang='en'><head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1.0'>" +
            "<title>" + pageTitle + "</title>" +
            "<style>" +
            "  body{margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;}" +
            "  .wrapper{width:100%;background:#f4f7fb;padding:40px 0;}" +
            "  .card{max-width:580px;margin:0 auto;background:#ffffff;" +
            "        border-radius:10px;box-shadow:0 2px 12px rgba(0,0,0,0.08);overflow:hidden;}" +

            // Header band
            "  .header{background:#1565c0;padding:32px 40px;text-align:center;}" +
            "  .header-logo{display:inline-block;background:#ffffff;color:#1565c0;" +
            "               font-size:13px;font-weight:700;letter-spacing:1px;padding:6px 16px;" +
            "               border-radius:4px;margin-bottom:14px;}" +
            "  .header h1{margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:0.3px;}" +
            "  .header p{margin:6px 0 0;color:#bbdefb;font-size:12px;}" +

            // Teal sub-bar
            "  .subbar{background:#e3f2fd;padding:12px 40px;border-bottom:1px solid #c9e3fb;}" +
            "  .subbar-title{margin:0;color:#1565c0;font-size:15px;font-weight:600;}" +

            // Body
            "  .body{padding:32px 40px;}" +
            "  .section-title{font-size:11px;font-weight:700;color:#1565c0;letter-spacing:1.2px;" +
            "                  text-transform:uppercase;margin:0 0 16px;border-bottom:2px solid #e3f2fd;" +
            "                  padding-bottom:6px;}" +
            "  .body-text{color:#444;font-size:15px;line-height:1.7;margin:0 0 14px;}" +
            "  .label{color:#555;font-size:13px;font-weight:600;margin:16px 0 8px;}" +
            "  .hint{color:#888;font-size:12px;margin:8px 0 0;}" +
            "  .divider{border:none;border-top:1px solid #e8f0fb;margin:24px 0;}" +

            // OTP display
            "  .otp-box{text-align:center;background:#e8f4fd;border:2px solid #1565c0;" +
            "            border-radius:8px;padding:18px;margin:12px 0;" +
            "            font-size:38px;font-weight:900;letter-spacing:14px;" +
            "            color:#1565c0;font-family:Courier New,monospace;}" +

            // Credentials table
            "  .cred-table{width:100%;border-collapse:collapse;margin:16px 0;border-radius:8px;overflow:hidden;}" +
            "  .cred-table tr:nth-child(odd){background:#f4f7fb;}" +
            "  .cred-table tr:nth-child(even){background:#eaf3fb;}" +
            "  .cred-label{padding:12px 16px;font-size:13px;color:#555;font-weight:600;" +
            "               width:38%;border-right:1px solid #d8eaf8;}" +
            "  .cred-value{padding:12px 16px;font-size:14px;color:#222;}" +
            "  .cred-password{font-family:Courier New,monospace;font-size:17px;font-weight:700;" +
            "                  color:#1565c0;letter-spacing:2px;}" +
            "  .cred-highlight{color:#1565c0;font-weight:700;}" +

            // Info box
            "  .info-box{background:#e8f4fd;border-left:4px solid #1565c0;border-radius:6px;" +
            "             padding:16px;margin:16px 0;}" +
            "  .info-title{margin:0 0 6px;font-size:13px;font-weight:700;color:#1565c0;" +
            "               text-transform:uppercase;letter-spacing:0.8px;}" +
            "  .info-list{margin:6px 0 0;padding-left:18px;color:#444;font-size:14px;line-height:1.8;}" +

            // Warning box
            "  .warning-box{background:#fff8e1;border-left:4px solid #f59e0b;border-radius:6px;" +
            "                padding:14px 16px;margin:16px 0;color:#78350f;font-size:13px;font-weight:600;}" +

            // Success box
            "  .success-box{background:#e8f5e9;border-left:4px solid #2e7d32;border-radius:6px;" +
            "                padding:16px;margin:16px 0;color:#1b5e20;font-size:14px;}" +

            // Button
            "  .btn{display:inline-block;background:#1565c0;color:#ffffff;text-decoration:none;" +
            "        font-size:14px;font-weight:700;padding:13px 32px;border-radius:6px;" +
            "        letter-spacing:0.3px;}" +

            // Footer
            "  .footer{background:#f4f7fb;padding:20px 40px;text-align:center;" +
            "           border-top:1px solid #e3edf8;}" +
            "  .footer p{margin:0;color:#999;font-size:11px;line-height:1.8;}" +
            "  .footer strong{color:#1565c0;}" +
            "</style>" +
            "</head><body>" +
            "<div class='wrapper'>" +
            "  <div class='card'>" +

            // Header
            "    <div class='header'>" +
            "      <div class='header-logo'>WASAC</div>" +
            "      <h1>Utility Billing System</h1>" +
            "      <p>Water &amp; Sanitation Corporation of Rwanda</p>" +
            "    </div>" +

            // Sub-bar
            "    <div class='subbar'><p class='subbar-title'>" + pageTitle + "</p></div>" +

            // Body
            "    <div class='body'>" + bodyContent + "</div>" +

            // Footer
            "    <div class='footer'>" +
            "      <p><strong>WASAC &mdash; Water and Sanitation Corporation</strong></p>" +
            "      <p>This is an automated message. Please do not reply to this email.</p>" +
            "      <p>&copy; 2026 WASAC Utility Billing System. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</div>" +
            "</body></html>";
    }
}
