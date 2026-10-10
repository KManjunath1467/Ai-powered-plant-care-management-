package com.myplant.service;

import com.myplant.dto.NotificationDTO;
import com.myplant.entity.Notification;
import com.myplant.entity.Plant;
import com.myplant.entity.User;
import com.myplant.repository.NotificationRepository;
import com.myplant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Notification Service
 * 
 * Handles sending and tracking notifications.
 * Responsibilities:
 * 1. Create notification records in database
 * 2. Send email notifications via Spring Mail
 * 3. Send WhatsApp notifications via Twilio (future implementation)
 * 4. Track notification delivery status
 * 5. Provide notification history to users
 * 
 * Note: WhatsApp integration would require Twilio service implementation
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final UserService userService;

    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${twilio.whatsapp-number:}")
    private String twilioWhatsAppNumber;

    /**
     * Create and send a watering reminder notification
     * 
     * @param userId the user's ID
     * @param plantId the plant's ID (if plant-specific)
     * @param title notification title
     * @param message notification message
     * @param sendEmail whether to send via email
     * @param sendWhatsApp whether to send via WhatsApp
     * @return created notification as DTO
     */
    public NotificationDTO createAndSendNotification(
            Long userId,
            Long plantId,
            String title,
            String message,
            boolean sendEmail,
            boolean sendWhatsApp) {

        User user = userService.getUserById(userId);

        // Create notification record
        Notification notification = new Notification();
        notification.setUser(user);

        if (plantId != null) {
            Plant plant = new Plant();
            plant.setId(plantId);
            notification.setPlant(plant);
        }

        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(Notification.NotificationType.WATERING_REMINDER);
        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setChannel(Notification.NotificationChannel.IN_APP);

        // Send email if requested
        if (sendEmail) {
            notification.setChannel(Notification.NotificationChannel.EMAIL);
            sendEmailNotification(notification);
        }

        // Send WhatsApp if requested (future implementation)
        if (sendWhatsApp) {
            notification.setChannel(Notification.NotificationChannel.WHATSAPP);
            sendWhatsAppNotification(notification);
        }

        Notification savedNotification = notificationRepository.save(notification);
        return convertToDTO(savedNotification);
    }

    /**
     * Send email notification
     * 
     * @param notification the notification to send
     */
    private void sendEmailNotification(Notification notification) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(notification.getUser().getEmail());
            email.setSubject("[MyPlant] " + notification.getTitle());
            email.setText(notification.getMessage() + "\n\n" +
                    "Visit MyPlant dashboard to manage your plants and watering schedules.\n\n" +
                    "Happy planting! 🌱");

            mailSender.send(email);

            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setFailureReason(e.getMessage());
        }
    }

    /** Send a real WhatsApp message through Twilio. A successful API response means accepted by Twilio, not necessarily delivered. */
    private void sendWhatsAppNotification(Notification notification) {
        try {
            if (twilioAccountSid == null || twilioAccountSid.isBlank()
                    || twilioAuthToken == null || twilioAuthToken.isBlank()
                    || twilioWhatsAppNumber == null || twilioWhatsAppNumber.isBlank()) {
                throw new IllegalStateException("WhatsApp is not configured. Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_WHATSAPP_NUMBER.");
            }
            User user = notification.getUser();
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                throw new IllegalStateException("Add a phone number with country code to the user profile before enabling WhatsApp notifications.");
            }
            if (!Boolean.TRUE.equals(user.getWhatsappNotifications())) {
                throw new IllegalStateException("WhatsApp notifications are disabled in the user's preferences.");
            }

            String to = user.getPhoneNumber().trim().replaceAll("\\s+", "");
            if (!to.startsWith("whatsapp:")) {
                if (!to.startsWith("+")) {
                    throw new IllegalArgumentException("WhatsApp phone number must include country code, for example +91XXXXXXXXXX.");
                }
                to = "whatsapp:" + to;
            }
            String from = twilioWhatsAppNumber.trim();
            if (!from.startsWith("whatsapp:")) from = "whatsapp:" + from;

            Twilio.init(twilioAccountSid.trim(), twilioAuthToken.trim());
            Message sent = Message.creator(new PhoneNumber(to), new PhoneNumber(from),
                    "🌿 MyPlant Alert\n" + notification.getTitle() + "\n\n" + notification.getMessage()).create();

            if (sent.getSid() == null || sent.getSid().isBlank()) {
                throw new IllegalStateException("Twilio did not return a message identifier.");
            }
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setFailureReason(null);
        } catch (Exception e) {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setFailureReason("WhatsApp submission failed: " + e.getMessage());
        }
    }

    /**
     * Get all notifications for a user
     * 
     * @param userId the user's ID
     * @return list of notifications
     */
    public List<NotificationDTO> getUserNotifications(Long userId) {
        User user = userService.getUserById(userId);

        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for a user
     * 
     * @param userId the user's ID
     * @return list of unread notifications
     */
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        User user = userService.getUserById(userId);

        return notificationRepository.findByUserAndStatus(user, Notification.NotificationStatus.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notification count for a user
     *
     * @param userId the user's ID
     * @return count of unread notifications
     */
    public int getUnreadCount(Long userId) {
        User user = userService.getUserById(userId);
        return notificationRepository.countByUserAndStatus(user, Notification.NotificationStatus.PENDING);
    }

    /**
     * Clear notifications for a user
     *
     * @param userId the user's ID
     */
    public void clearUserNotifications(Long userId) {
        User user = userService.getUserById(userId);
        notificationRepository.deleteByUser(user);
    }

    /**
     * Mark notification as read
     * 
     * @param userId the user's ID
     * @param notificationId the notification's ID
     * @return updated notification
     */
    public NotificationDTO markAsRead(Long userId, Long notificationId) {
        User user = userService.getUserById(userId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Notification does not belong to the current user.");
        }
        
        notification.setStatus(Notification.NotificationStatus.VIEWED);
        notification.setViewedAt(LocalDateTime.now());
        
        Notification updated = notificationRepository.save(notification);
        return convertToDTO(updated);
    }

    /**
     * Convert Notification entity to DTO
     * 
     * @param notification the entity
     * @return DTO
     */
    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO(
                notification.getId(),
                notification.getUser().getId(),
                notification.getPlant() != null ? notification.getPlant().getId() : null,
                notification.getPlant() != null ? notification.getPlant().getName() : null,
                notification.getNotificationType().toString(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getChannel().toString(),
                notification.getStatus().toString(),
                notification.getSentAt(),
                notification.getViewedAt(),
                notification.getCreatedAt(),
                notification.getViewedAt() != null || notification.getStatus() == Notification.NotificationStatus.VIEWED,
                notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null
        );
        return dto;
    }
}
