package com.aziz.demosec.service;

import com.aziz.demosec.dto.donation.AssignmentEmailProjection;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Envoie les deux emails de notification après assignation d'un don.
     * Appelé de façon asynchrone pour ne jamais bloquer l'opération principale.
     */
    @Async
    public void sendAssignmentEmails(AssignmentEmailProjection data, Long aidRequestId) {
        sendToRequester(data, aidRequestId);
        sendToDonor(data, aidRequestId);
    }

    // ─── Email 1 : vers le demandeur ─────────────────────────────────────────

    private void sendToRequester(AssignmentEmailProjection data, Long aidRequestId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(data.getRequesterEmail());
            helper.setSubject("Votre demande d'aide a été acceptée ✅");
            helper.setText(buildRequesterHtml(data, aidRequestId), true);

            mailSender.send(message);
            log.info("[EMAIL] Email envoyé au demandeur {} <{}>",
                    data.getRequesterName(), data.getRequesterEmail());

        } catch (MessagingException e) {
            log.error("[EMAIL] Échec envoi email demandeur {} : {}",
                    data.getRequesterEmail(), e.getMessage());
        }
    }

    // ─── Email 2 : vers le donneur ────────────────────────────────────────────

    private void sendToDonor(AssignmentEmailProjection data, Long aidRequestId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(data.getDonorEmail());
            helper.setSubject("Votre donation a été attribuée ✅");
            helper.setText(buildDonorHtml(data, aidRequestId), true);

            mailSender.send(message);
            log.info("[EMAIL] Email envoyé au donneur {} <{}>",
                    data.getDonorName(), data.getDonorEmail());

        } catch (MessagingException e) {
            log.error("[EMAIL] Échec envoi email donneur {} : {}",
                    data.getDonorEmail(), e.getMessage());
        }
    }

    // ─── Templates HTML ──────────────────────────────────────────────────────

    private String buildRequesterHtml(AssignmentEmailProjection d, Long aidRequestId) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"/></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1);">

                <!-- Header -->
                <div style="background:#27AE60;padding:30px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:24px;">MedicarePI</h1>
                  <p style="color:#d5f5e3;margin:8px 0 0;">Plateforme de soutien médical</p>
                </div>

                <!-- Body -->
                <div style="padding:30px;">
                  <h2 style="color:#27AE60;">Bonne nouvelle, %s ! 🎉</h2>
                  <p style="color:#555;line-height:1.6;">
                    Votre demande d'aide a été <strong>acceptée et un don vous a été attribué</strong>.
                  </p>

                  <!-- Donor card -->
                  <div style="background:#f0fdf4;border-left:4px solid #27AE60;border-radius:4px;padding:20px;margin:20px 0;">
                    <h3 style="color:#166534;margin:0 0 12px;">Informations du donneur</h3>
                    <table style="width:100%%;border-collapse:collapse;">
                      <tr>
                        <td style="padding:6px 0;color:#555;width:40%%;">👤 Nom complet</td>
                        <td style="padding:6px 0;color:#111;font-weight:bold;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:6px 0;color:#555;">📞 Téléphone</td>
                        <td style="padding:6px 0;color:#111;font-weight:bold;">%s</td>
                      </tr>
                    </table>
                  </div>

                  <div style="background:#fffbeb;border:1px solid #f59e0b;border-radius:4px;padding:15px;margin:20px 0;">
                    <p style="margin:0;color:#92400e;">
                      💬 <strong>Prochaine étape :</strong> Vous pouvez contacter le donneur directement
                      par téléphone pour organiser la réception du don.
                    </p>
                  </div>

                  <p style="color:#555;">Merci de faire confiance à MedicarePI.</p>
                </div>

                <!-- Footer -->
                <div style="background:#f8f8f8;padding:15px;text-align:center;border-top:1px solid #eee;">
                  <p style="color:#999;font-size:12px;margin:0;">
                    Cet email a été envoyé automatiquement par MedicarePI — ne pas répondre.
                  </p>
                </div>

              </div>
            </body>
            </html>
            """.formatted(
                d.getRequesterName(),
                d.getDonorName(),
                d.getDonorPhone() != null ? d.getDonorPhone() : "Non renseigné"
            );
    }

    private String buildDonorHtml(AssignmentEmailProjection d, Long aidRequestId) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"/></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1);">

                <!-- Header -->
                <div style="background:#2E86AB;padding:30px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:24px;">MedicarePI</h1>
                  <p style="color:#bde0f0;margin:8px 0 0;">Plateforme de soutien médical</p>
                </div>

                <!-- Body -->
                <div style="padding:30px;">
                  <h2 style="color:#2E86AB;">Merci pour votre générosité, %s ! 💙</h2>
                  <p style="color:#555;line-height:1.6;">
                    Votre donation a été <strong>attribuée avec succès</strong> à une personne dans le besoin.
                  </p>

                  <!-- Requester card -->
                  <div style="background:#eff6ff;border-left:4px solid #2E86AB;border-radius:4px;padding:20px;margin:20px 0;">
                    <h3 style="color:#1e3a5f;margin:0 0 12px;">Bénéficiaire de votre don</h3>
                    <table style="width:100%%;border-collapse:collapse;">
                      <tr>
                        <td style="padding:6px 0;color:#555;width:40%%;">👤 Nom complet</td>
                        <td style="padding:6px 0;color:#111;font-weight:bold;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:6px 0;color:#555;">📞 Téléphone</td>
                        <td style="padding:6px 0;color:#111;font-weight:bold;">%s</td>
                      </tr>
                    </table>
                  </div>

                  <div style="background:#fffbeb;border:1px solid #f59e0b;border-radius:4px;padding:15px;margin:20px 0;">
                    <p style="margin:0;color:#92400e;">
                      💬 <strong>Information :</strong> Cette personne va vous contacter prochainement
                      par téléphone pour organiser la récupération du don.
                    </p>
                  </div>

                  <p style="color:#555;">
                    Votre geste fait une vraie différence. Merci d'être là pour MedicarePI.
                  </p>
                </div>

                <!-- Footer -->
                <div style="background:#f8f8f8;padding:15px;text-align:center;border-top:1px solid #eee;">
                  <p style="color:#999;font-size:12px;margin:0;">
                    Cet email a été envoyé automatiquement par MedicarePI — ne pas répondre.
                  </p>
                </div>

              </div>
            </body>
            </html>
            """.formatted(
                d.getDonorName(),
                d.getRequesterName(),
                d.getRequesterPhone() != null ? d.getRequesterPhone() : "Non renseigné"
            );
    }
}
