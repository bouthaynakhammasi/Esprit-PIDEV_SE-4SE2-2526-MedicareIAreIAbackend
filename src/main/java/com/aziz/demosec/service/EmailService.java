package com.aziz.demosec.service;

import com.aziz.demosec.dto.LabStaffPerformanceDTO;
import com.aziz.demosec.dto.RecommendationItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendAlzheimerReport(String toEmail, String doctorName, String patientName,
                                    String technicianName, String diagnostic, String risque,
                                    double confidence, String message, String resultData,
                                    java.util.Map<String, Double> probabilities) {
        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Rapport d'Analyse Neuro-Imagerie — " + patientName + " [MediCareAI]");
            helper.setText(buildAlzheimerReportTemplate(
                    doctorName, patientName, technicianName,
                    diagnostic, risque, confidence, message, probabilities), true);
            mailSender.send(mail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send Alzheimer report email: " + e.getMessage());
        }
    }

    private String buildAlzheimerReportTemplate(String doctorName, String patientName,
                                                 String technicianName, String diagnostic,
                                                 String risque, double confidence,
                                                 String message,
                                                 java.util.Map<String, Double> probabilities) {

        String now    = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        String refNum = "ALZ-" + (System.currentTimeMillis() % 900000 + 100000);

        // ---- Risk label & accent (minimal color, one shade only) ----
        String riskColor = switch (risque) {
            case "URGENT"       -> "#b71c1c";
            case "ATTENTION"    -> "#bf360c";
            case "SURVEILLANCE" -> "#f57f17";
            default             -> "#1b5e20";
        };
        String riskLabel = switch (risque) {
            case "URGENT"       -> "URGENT — Consultation immédiate requise";
            case "ATTENTION"    -> "ÉLEVÉ — Consultation médicale recommandée";
            case "SURVEILLANCE" -> "MODÉRÉ — Suivi médical nécessaire";
            default             -> "NORMAL — Aucun signe pathologique détecté";
        };

        // ---- Clinical interpretation per class ----
        String clinicalInterp = switch (diagnostic) {
            case "No Impairment" ->
                "L'analyse IRM ne révèle aucun signe de déficience cognitive liée à la maladie " +
                "d'Alzheimer. Les structures cérébrales examinées apparaissent dans les limites " +
                "normales pour la population de référence. Un suivi de routine est recommandé.";
            case "Very Mild Impairment" ->
                "L'analyse IRM révèle des signes très précoces et légers pouvant être associés " +
                "à un début de déficience cognitive. Ces anomalies peuvent correspondre à un " +
                "vieillissement cérébral normal ou à un stade prodromal. Une évaluation " +
                "neuropsychologique complémentaire est conseillée pour affiner le diagnostic.";
            case "Mild Impairment" ->
                "L'analyse IRM met en évidence une déficience cognitive légère (MCI — Mild " +
                "Cognitive Impairment). Des modifications structurelles cérébrales caractéristiques " +
                "sont observées. Le patient peut présenter des difficultés mnésiques et attentionnelles. " +
                "Une consultation neurologique avec bilan cognitif approfondi est fortement recommandée.";
            case "Moderate Impairment" ->
                "L'analyse IRM révèle une déficience cognitive modérée avec des altérations " +
                "structurelles cérébrales significatives. Ce stade est associé à des troubles " +
                "cognitifs impactant les activités quotidiennes. Une prise en charge neurologique " +
                "urgente et un suivi thérapeutique adapté sont nécessaires.";
            default -> "Classification en dehors des stades définis. Veuillez consulter un spécialiste.";
        };

        // ---- Performance metrics per class ----
        String[] perf = switch (diagnostic) {
            case "No Impairment"        -> new String[]{"97.0", "65.0", "78.0"};
            case "Very Mild Impairment" -> new String[]{"73.0", "78.0", "75.0"};
            case "Mild Impairment"      -> new String[]{"78.0", "95.0", "86.0"};
            case "Moderate Impairment"  -> new String[]{"92.0", "100.0", "96.0"};
            default                     -> new String[]{"—", "—", "—"};
        };

        // ---- Probability rows ----
        String[] classes = {
            "No Impairment",
            "Very Mild Impairment",
            "Mild Impairment",
            "Moderate Impairment"
        };
        String[] classLabels = {
            "Aucune déficience",
            "Déficience très légère",
            "Déficience légère",
            "Déficience modérée"
        };

        StringBuilder probRows = new StringBuilder();
        for (int i = 0; i < classes.length; i++) {
            double prob     = probabilities != null ? probabilities.getOrDefault(classes[i], 0.0) : 0.0;
            boolean primary = classes[i].equals(diagnostic);
            String barColor = primary ? "#1a1a2e" : "#c8c8c8";
            String txtColor = primary ? "#1a1a2e" : "#777777";
            String weight   = primary ? "700"     : "400";
            String marker   = primary ? "&#9679;" : "&nbsp;&nbsp;";
            probRows.append(String.format("""
                <tr>
                  <td style="padding:7px 0;font-size:12px;color:%s;font-weight:%s;width:175px;">
                    <span style="color:%s;margin-right:6px;font-size:10px;">%s</span>%s
                  </td>
                  <td style="padding:7px 0;width:180px;">
                    <div style="background:#ebebeb;height:8px;border-radius:1px;overflow:hidden;">
                      <div style="background:%s;height:8px;width:%.1f%%;"></div>
                    </div>
                  </td>
                  <td style="padding:7px 0 7px 12px;font-size:12px;font-weight:%s;color:%s;
                             text-align:right;white-space:nowrap;">%.1f%%</td>
                </tr>
                """,
                txtColor, weight,
                primary ? riskColor : "#c8c8c8", marker,
                classLabels[i],
                barColor, prob,
                weight, txtColor, prob
            ));
        }

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width,initial-scale=1"/>
            </head>
            <body style="margin:0;padding:0;background:#ececec;font-family:Arial,Helvetica,sans-serif;">

            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#ececec;padding:32px 0;">
            <tr><td align="center">
            <table width="620" cellpadding="0" cellspacing="0"
                   style="background:#ffffff;border:1px solid #d0d0d0;border-collapse:collapse;">

              <!-- ══ LETTERHEAD ══ -->
              <tr>
                <td style="background:#1a1a2e;padding:22px 36px;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td>
                        <div style="font-size:20px;font-weight:700;color:#ffffff;letter-spacing:1px;">
                          MEDICARE<span style="color:#1a7a6e;">AI</span>
                        </div>
                        <div style="font-size:10px;color:#9ca3af;margin-top:3px;letter-spacing:.5px;">
                          SYSTÈME D'ANALYSE MÉDICALE PAR INTELLIGENCE ARTIFICIELLE
                        </div>
                      </td>
                      <td align="right">
                        <div style="font-size:10px;color:#9ca3af;">Réf. %s</div>
                        <div style="font-size:10px;color:#9ca3af;margin-top:2px;">%s</div>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>

              <!-- ══ REPORT TITLE ══ -->
              <tr>
                <td style="padding:20px 36px 16px;border-bottom:2px solid #1a1a2e;">
                  <div style="font-size:16px;font-weight:700;color:#1a1a2e;letter-spacing:.3px;">
                    RAPPORT D'ANALYSE NEURO-IMAGERIE
                  </div>
                  <div style="font-size:11px;color:#666666;margin-top:4px;">
                    Détection de la maladie d'Alzheimer — Analyse par apprentissage profond (VGG16)
                  </div>
                </td>
              </tr>

              <!-- ══ SECTION 1 : ADMINISTRATIVE INFO ══ -->
              <tr>
                <td style="padding:18px 36px 14px;border-bottom:1px solid #e0e0e0;
                           background:#fafafa;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:12px;">
                    1. Informations Administratives
                  </div>
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td style="padding:4px 0;font-size:12px;color:#444444;width:50%%;">
                        <span style="color:#888888;">Patient&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;:&nbsp;</span>
                        <strong style="color:#1a1a2e;">%s</strong>
                      </td>
                      <td style="padding:4px 0;font-size:12px;color:#444444;">
                        <span style="color:#888888;">Technicien&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;:&nbsp;</span>
                        <strong style="color:#1a1a2e;">%s</strong>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:4px 0;font-size:12px;color:#444444;">
                        <span style="color:#888888;">Destinataire&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;:&nbsp;</span>
                        <strong style="color:#1a1a2e;">%s</strong>
                      </td>
                      <td style="padding:4px 0;font-size:12px;color:#444444;">
                        <span style="color:#888888;">Modalité&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;:&nbsp;</span>
                        <strong style="color:#1a1a2e;">IRM Cérébrale (MRI)</strong>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>

              <!-- ══ SECTION 2 : DIAGNOSTIC RESULT ══ -->
              <tr>
                <td style="padding:18px 36px 14px;border-bottom:1px solid #e0e0e0;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:14px;">
                    2. Résultat du Diagnostic IA
                  </div>
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td style="vertical-align:top;width:50%%;">
                        <div style="font-size:11px;color:#888888;margin-bottom:4px;">
                          CLASSIFICATION
                        </div>
                        <div style="font-size:17px;font-weight:700;color:#1a1a2e;">
                          %s
                        </div>
                      </td>
                      <td style="vertical-align:top;">
                        <div style="font-size:11px;color:#888888;margin-bottom:4px;">
                          INDICE DE CONFIANCE
                        </div>
                        <div style="font-size:17px;font-weight:700;color:#1a1a2e;">
                          %.1f%%
                        </div>
                        <div style="margin-top:6px;background:#e0e0e0;height:6px;border-radius:1px;">
                          <div style="background:#1a1a2e;height:6px;width:%.1f%%;border-radius:1px;"></div>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td colspan="2" style="padding-top:14px;">
                        <div style="border-left:3px solid %s;padding:10px 14px;
                                   background:#f7f7f7;">
                          <div style="font-size:10px;font-weight:700;color:#888888;
                                     text-transform:uppercase;letter-spacing:.06em;margin-bottom:3px;">
                            NIVEAU DE RISQUE
                          </div>
                          <div style="font-size:13px;font-weight:700;color:%s;">
                            %s
                          </div>
                        </div>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>

              <!-- ══ SECTION 3 : PROBABILITY DISTRIBUTION ══ -->
              <tr>
                <td style="padding:18px 36px 14px;border-bottom:1px solid #e0e0e0;
                           background:#fafafa;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:14px;">
                    3. Distribution des Probabilités par Classe
                  </div>
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    %s
                  </table>
                  <div style="margin-top:10px;font-size:10px;color:#aaaaaa;font-style:italic;">
                    &#9679; Classe sélectionnée par le modèle (probabilité dominante)
                  </div>
                </td>
              </tr>

              <!-- ══ SECTION 4 : CLINICAL INTERPRETATION ══ -->
              <tr>
                <td style="padding:18px 36px 14px;border-bottom:1px solid #e0e0e0;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:12px;">
                    4. Interprétation Clinique
                  </div>
                  <div style="font-size:13px;color:#333333;line-height:1.75;">
                    %s
                  </div>
                </td>
              </tr>

              <!-- ══ SECTION 5 : RECOMMENDATION ══ -->
              <tr>
                <td style="padding:18px 36px 14px;border-bottom:1px solid #e0e0e0;
                           background:#fafafa;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:12px;">
                    5. Recommandation du Système
                  </div>
                  <div style="font-size:13px;color:#333333;line-height:1.75;
                              border-left:2px solid #cccccc;padding-left:14px;">
                    %s
                  </div>
                </td>
              </tr>

              <!-- ══ SECTION 6 : MODEL PERFORMANCE ══ -->
              <tr>
                <td style="padding:18px 36px 14px;border-bottom:1px solid #e0e0e0;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:12px;">
                    6. Performances du Modèle
                  </div>
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="font-size:12px;color:#444444;">
                    <tr style="border-bottom:1px solid #eeeeee;">
                      <td style="padding:6px 0;color:#888888;width:50%%;">Modèle utilisé</td>
                      <td style="padding:6px 0;font-weight:600;color:#1a1a2e;">
                        VGG16 Transfer Learning
                      </td>
                    </tr>
                    <tr style="border-bottom:1px solid #eeeeee;">
                      <td style="padding:6px 0;color:#888888;">Précision globale</td>
                      <td style="padding:6px 0;font-weight:600;color:#1a1a2e;">83.96%%</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eeeeee;">
                      <td style="padding:6px 0;color:#888888;">F1-Score global</td>
                      <td style="padding:6px 0;font-weight:600;color:#1a1a2e;">83.58%%</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eeeeee;">
                      <td style="padding:6px 0;color:#888888;">
                        Précision — classe &laquo; %s &raquo;
                      </td>
                      <td style="padding:6px 0;font-weight:600;color:#1a1a2e;">%s%%</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eeeeee;">
                      <td style="padding:6px 0;color:#888888;">Rappel (Recall)</td>
                      <td style="padding:6px 0;font-weight:600;color:#1a1a2e;">%s%%</td>
                    </tr>
                    <tr>
                      <td style="padding:6px 0;color:#888888;">F1-Score (classe)</td>
                      <td style="padding:6px 0;font-weight:600;color:#1a1a2e;">%s%%</td>
                    </tr>
                  </table>
                </td>
              </tr>

              <!-- ══ DISCLAIMER ══ -->
              <tr>
                <td style="padding:16px 36px;background:#f2f2f2;border-top:2px solid #1a1a2e;">
                  <div style="font-size:10px;color:#666666;line-height:1.7;">
                    <strong style="color:#1a1a2e;">AVERTISSEMENT MÉDICAL —</strong>
                    Ce rapport a été généré automatiquement par un système d'intelligence artificielle
                    à des fins d'aide à la décision clinique. Il ne constitue pas un diagnostic médical
                    définitif. La classification produite par le modèle VGG16 est basée sur l'analyse
                    de l'image IRM fournie et doit être interprétée par un professionnel de santé qualifié.
                    La décision diagnostique et thérapeutique finale appartient exclusivement au médecin
                    traitant. MediCareAI décline toute responsabilité en cas d'usage isolé de ce rapport.
                  </div>
                </td>
              </tr>

              <!-- ══ FOOTER ══ -->
              <tr>
                <td style="padding:14px 36px;background:#1a1a2e;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td style="font-size:10px;color:#6b7280;">
                        © 2024 MediCareAI Health Systems · Tous droits réservés
                      </td>
                      <td align="right" style="font-size:10px;color:#6b7280;">
                        Réf. %s
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>

            </table>
            </td></tr>
            </table>
            </body>
            </html>
            """.formatted(
                refNum, now,
                patientName, technicianName, doctorName,
                diagnostic,
                confidence, confidence,
                riskColor, riskColor, riskLabel,
                probRows.toString(),
                clinicalInterp,
                message,
                diagnostic, perf[0], perf[1], perf[2],
                refNum
        );
    }

    public void sendCombinedAlzheimerReport(
            String toEmail,
            String patientName,
            String irmDiagnostic,
            String irmRisk,
            Double irmConfidence,
            Double rfRiskScore,
            String rfRiskLabel,
            String rfRiskMessage,
            List<RecommendationItemDto> recommendations) {
        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Rapport Complet Alzheimer IA — " + patientName + " [MediCareAI]");
            helper.setText(buildCombinedReportTemplate(
                    patientName, irmDiagnostic, irmRisk, irmConfidence,
                    rfRiskScore, rfRiskLabel, rfRiskMessage, recommendations), true);
            mailSender.send(mail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send combined Alzheimer report: " + e.getMessage());
        }
    }

    private String buildCombinedReportTemplate(
            String patientName,
            String irmDiagnostic,
            String irmRisk,
            Double irmConfidence,
            Double rfRiskScore,
            String rfRiskLabel,
            String rfRiskMessage,
            List<RecommendationItemDto> recommendations) {

        String now    = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        String refNum = "RF-" + (System.currentTimeMillis() % 900000 + 100000);

        String irmRiskColor = switch (irmRisk != null ? irmRisk : "") {
            case "URGENT"       -> "#b71c1c";
            case "ATTENTION"    -> "#bf360c";
            case "SURVEILLANCE" -> "#f57f17";
            default             -> "#1b5e20";
        };

        String rfRiskHex = rfRiskLabel != null && rfRiskLabel.contains("Very High") ? "#b71c1c"
                : rfRiskLabel != null && rfRiskLabel.contains("High") ? "#bf360c"
                : rfRiskLabel != null && rfRiskLabel.contains("Moderate") ? "#f57f17"
                : "#1b5e20";

        // ── Build recommendation rows ──────────────────────────────
        StringBuilder highRows   = new StringBuilder();
        StringBuilder mediumRows = new StringBuilder();

        if (recommendations != null) {
            for (RecommendationItemDto r : recommendations) {
                String catColor = switch (r.getCategory() != null ? r.getCategory() : "") {
                    case "Medical"    -> "#1a1a2e";
                    case "Cognitive"  -> "#3b4c8c";
                    default           -> "#2e6b5e";  // Lifestyle
                };
                String row = """
                    <tr style="border-bottom:1px solid #f0f0f0;">
                      <td style="padding:9px 0 9px 8px;vertical-align:top;width:24px;">
                        <span style="font-size:16px;">%s</span>
                      </td>
                      <td style="padding:9px 12px 9px 8px;vertical-align:top;">
                        <div style="font-size:12px;font-weight:700;color:#1a1a2e;">%s</div>
                        <div style="font-size:11px;color:#555555;margin-top:2px;">%s</div>
                      </td>
                      <td style="padding:9px 0;vertical-align:top;text-align:right;white-space:nowrap;">
                        <span style="font-size:10px;font-weight:700;color:%s;
                                     background:#f5f5f5;padding:2px 7px;border-radius:2px;">%s</span>
                      </td>
                    </tr>
                    """.formatted(
                        "High".equals(r.getPriority()) ? "🔴" : "🟡",
                        r.getFeature(),
                        r.getRecommendation(),
                        catColor, r.getCategory()
                );
                if ("High".equals(r.getPriority())) {
                    highRows.append(row);
                } else {
                    mediumRows.append(row);
                }
            }
        }

        String highSection = highRows.length() > 0 ? """
            <div style="font-size:10px;font-weight:700;color:#b71c1c;text-transform:uppercase;
                        letter-spacing:.06em;margin:10px 0 6px;">Priorité Haute</div>
            <table width="100%%" cellpadding="0" cellspacing="0">%s</table>
            """.formatted(highRows) : "";

        String medSection = mediumRows.length() > 0 ? """
            <div style="font-size:10px;font-weight:700;color:#d97706;text-transform:uppercase;
                        letter-spacing:.06em;margin:14px 0 6px;">Priorité Moyenne</div>
            <table width="100%%" cellpadding="0" cellspacing="0">%s</table>
            """.formatted(mediumRows) : "";

        int total = recommendations != null ? recommendations.size() : 0;

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"/></head>
            <body style="margin:0;padding:0;background:#ececec;font-family:Arial,Helvetica,sans-serif;">
            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#ececec;padding:32px 0;">
            <tr><td align="center">
            <table width="640" cellpadding="0" cellspacing="0"
                   style="background:#ffffff;border:1px solid #d0d0d0;border-collapse:collapse;">

              <!-- LETTERHEAD -->
              <tr>
                <td style="background:#1a1a2e;padding:22px 36px;">
                  <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                    <td>
                      <div style="font-size:20px;font-weight:700;color:#ffffff;letter-spacing:1px;">
                        MEDICARE<span style="color:#1a7a6e;">AI</span>
                      </div>
                      <div style="font-size:10px;color:#9ca3af;margin-top:3px;">
                        RAPPORT COMBINÉ — ANALYSE IRM + ÉVALUATION CLINIQUE
                      </div>
                    </td>
                    <td align="right">
                      <div style="font-size:10px;color:#9ca3af;">Réf. %s</div>
                      <div style="font-size:10px;color:#9ca3af;margin-top:2px;">%s</div>
                    </td>
                  </tr></table>
                </td>
              </tr>

              <!-- PATIENT -->
              <tr>
                <td style="padding:16px 36px;background:#fafafa;border-bottom:2px solid #1a1a2e;">
                  <span style="font-size:11px;color:#888888;">Patient :&nbsp;</span>
                  <strong style="font-size:14px;color:#1a1a2e;">%s</strong>
                  <span style="margin:0 12px;color:#dddddd;">|</span>
                  <span style="font-size:11px;color:#888888;">Date :&nbsp;</span>
                  <strong style="font-size:13px;color:#1a1a2e;">%s</strong>
                </td>
              </tr>

              <!-- SECTION 1 : IRM RESULT -->
              <tr>
                <td style="padding:18px 36px 14px;border-bottom:1px solid #e0e0e0;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:14px;">
                    1. Résultat Analyse IRM — VGG16
                  </div>
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td style="width:50%%;vertical-align:top;">
                        <div style="font-size:11px;color:#888888;">Classification</div>
                        <div style="font-size:16px;font-weight:700;color:#1a1a2e;margin-top:3px;">%s</div>
                      </td>
                      <td style="vertical-align:top;">
                        <div style="font-size:11px;color:#888888;">Confiance</div>
                        <div style="font-size:16px;font-weight:700;color:#1a1a2e;margin-top:3px;">%.1f%%</div>
                      </td>
                    </tr>
                    <tr><td colspan="2" style="padding-top:12px;">
                      <div style="border-left:3px solid %s;padding:8px 14px;background:#f7f7f7;">
                        <span style="font-size:12px;font-weight:700;color:%s;">%s — %s</span>
                      </div>
                    </td></tr>
                  </table>
                </td>
              </tr>

              <!-- SECTION 2 : RF CLINICAL RISK -->
              <tr>
                <td style="padding:18px 36px 14px;border-bottom:1px solid #e0e0e0;background:#fafafa;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:14px;">
                    2. Score de Risque Clinique — Random Forest
                  </div>
                  <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                    <td style="width:50%%;vertical-align:top;">
                      <div style="font-size:11px;color:#888888;">Score de risque</div>
                      <div style="font-size:22px;font-weight:700;color:%s;margin-top:3px;">%.1f%%</div>
                    </td>
                    <td style="vertical-align:top;">
                      <div style="font-size:11px;color:#888888;">Classification</div>
                      <div style="font-size:15px;font-weight:700;color:%s;margin-top:3px;">%s</div>
                      <div style="font-size:11px;color:#555555;margin-top:4px;">%s</div>
                    </td>
                  </tr></table>
                  <div style="margin-top:12px;background:#e0e0e0;height:8px;border-radius:2px;">
                    <div style="background:%s;height:8px;width:%.1f%%;border-radius:2px;
                                max-width:100%%;"></div>
                  </div>
                </td>
              </tr>

              <!-- SECTION 3 : RECOMMENDATIONS -->
              <tr>
                <td style="padding:18px 36px 18px;border-bottom:1px solid #e0e0e0;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:6px;">
                    3. Recommandations Personnalisées
                    <span style="background:#1a1a2e;color:#ffffff;font-size:10px;
                                 padding:2px 8px;border-radius:2px;margin-left:8px;">%d</span>
                  </div>
                  %s
                  %s
                </td>
              </tr>

              <!-- DISCLAIMER -->
              <tr>
                <td style="padding:14px 36px;background:#f2f2f2;border-top:2px solid #1a1a2e;">
                  <div style="font-size:10px;color:#666666;line-height:1.7;">
                    <strong style="color:#1a1a2e;">AVERTISSEMENT —</strong>
                    Ce rapport combine les résultats d'une analyse IRM (VGG16) et d'une évaluation
                    clinique (Random Forest). Il constitue une aide à la décision médicale et ne remplace
                    pas le jugement d'un professionnel de santé qualifié. La décision thérapeutique
                    finale appartient exclusivement au médecin traitant.
                  </div>
                </td>
              </tr>

              <!-- FOOTER -->
              <tr>
                <td style="padding:12px 36px;background:#1a1a2e;">
                  <div style="font-size:10px;color:#6b7280;">
                    © 2024 MediCareAI · Réf. %s
                  </div>
                </td>
              </tr>

            </table>
            </td></tr></table>
            </body></html>
            """.formatted(
                refNum, now,
                patientName, now,
                irmDiagnostic != null ? irmDiagnostic : "—",
                irmConfidence != null ? irmConfidence : 0.0,
                irmRiskColor, irmRiskColor,
                irmRisk != null ? irmRisk : "—",
                irmDiagnostic != null ? irmDiagnostic : "—",
                rfRiskHex, rfRiskScore != null ? rfRiskScore : 0.0,
                rfRiskHex, rfRiskLabel != null ? rfRiskLabel : "—",
                rfRiskMessage != null ? rfRiskMessage : "",
                rfRiskHex, rfRiskScore != null ? rfRiskScore : 0.0,
                total,
                highSection, medSection,
                refNum
        );
    }

    // ============================================================
    // AI Narrative Email
    // ============================================================

    public void sendNarrativeEmail(String toEmail, String patientName, String risk,
                                    String diagnostic, String doctorNarrative) {
        try {
            String riskColor = switch (risk != null ? risk : "") {
                case "URGENT"       -> "#b71c1c";
                case "ATTENTION"    -> "#e65100";
                case "SURVEILLANCE" -> "#f57f17";
                default             -> "#1b5e20";
            };

            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#ececec;font-family:Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#ececec;padding:32px 0;">
                <tr><td align="center">
                <table width="640" cellpadding="0" cellspacing="0"
                       style="background:#fff;border:1px solid #d0d0d0;border-collapse:collapse;">

                  <tr>
                    <td style="background:#1a1a2e;padding:22px 32px;">
                      <div style="font-size:18px;font-weight:700;color:#fff;">
                        MEDICARE<span style="color:#1a7a6e;">AI</span>
                      </div>
                      <div style="font-size:10px;color:#9ca3af;margin-top:3px;">
                        AI-GENERATED CLINICAL LAB REPORT — CONFIDENTIAL
                      </div>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding:20px 32px;background:#fafafa;border-bottom:1px solid #e0e0e0;">
                      <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                        <td>
                          <div style="font-size:13px;color:#888;">Patient</div>
                          <div style="font-size:16px;font-weight:700;color:#1a1a2e;">%s</div>
                        </td>
                        <td align="right">
                          <span style="background:%s;color:#fff;font-size:12px;font-weight:700;
                                       padding:5px 14px;border-radius:4px;">%s</span>
                        </td>
                      </tr></table>
                      <div style="font-size:12px;color:#555;margin-top:6px;">
                        AI Diagnostic: <strong>%s</strong>
                      </div>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding:24px 32px;border-bottom:1px solid #e0e0e0;">
                      <div style="font-size:10px;font-weight:700;color:#1a1a2e;text-transform:uppercase;
                                  letter-spacing:.08em;margin-bottom:12px;">
                        🩺 Clinical Summary — For Physician Use Only
                      </div>
                      <div style="font-size:14px;color:#222;line-height:1.8;background:#f5f5f5;
                                  padding:16px;border-left:4px solid #1a1a2e;border-radius:4px;">
                        %s
                      </div>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding:12px 32px;background:#1a1a2e;">
                      <div style="font-size:10px;color:#6b7280;">
                        © 2024 MediCareAI · Confidential Clinical Report — For physician decision support only
                      </div>
                    </td>
                  </tr>

                </table>
                </td></tr></table>
                </body></html>
                """.formatted(
                    patientName,
                    riskColor, risk != null ? risk : "—",
                    diagnostic != null ? diagnostic : "—",
                    doctorNarrative
            );

            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("🤖 AI Lab Report — " + patientName + " [MediCareAI]");
            helper.setText(html, true);
            mailSender.send(mail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send narrative email: " + e.getMessage());
        }
    }

    // ============================================================
    // Alzheimer Follow-Up Alert
    // ============================================================

    public void sendFollowUpAlert(String toEmail, String patientName,
                                   String risk, String diagnostic, String resultDate) {
        try {
            String riskColor = switch (risk) {
                case "URGENT"    -> "#b71c1c";
                case "ATTENTION" -> "#e65100";
                default          -> "#f57f17";
            };

            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#ececec;font-family:Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#ececec;padding:32px 0;">
                <tr><td align="center">
                <table width="600" cellpadding="0" cellspacing="0"
                       style="background:#ffffff;border:1px solid #d0d0d0;border-collapse:collapse;">

                  <tr>
                    <td style="background:#1a1a2e;padding:20px 32px;">
                      <div style="font-size:18px;font-weight:700;color:#ffffff;">
                        MEDICARE<span style="color:#1a7a6e;">AI</span>
                      </div>
                      <div style="font-size:10px;color:#9ca3af;margin-top:3px;">
                        AUTOMATIC ALZHEIMER FOLLOW-UP ALERT
                      </div>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding:24px 32px;">
                      <div style="border-left:4px solid %s;padding:14px 18px;background:#fafafa;margin-bottom:20px;">
                        <div style="font-size:13px;font-weight:700;color:%s;text-transform:uppercase;">
                          %s — Follow-up Required
                        </div>
                        <div style="font-size:12px;color:#555;margin-top:4px;">
                          Patient <strong>%s</strong> had a high-risk result and has not been re-examined in the last 7 days.
                        </div>
                      </div>

                      <table width="100%%" cellpadding="0" cellspacing="0"
                             style="border:1px solid #e0e0e0;border-collapse:collapse;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px 14px;font-size:12px;font-weight:700;color:#1a1a2e;">Patient</td>
                          <td style="padding:10px 14px;font-size:12px;color:#333;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 14px;font-size:12px;font-weight:700;color:#1a1a2e;border-top:1px solid #e0e0e0;">AI Risk</td>
                          <td style="padding:10px 14px;font-size:12px;font-weight:700;color:%s;border-top:1px solid #e0e0e0;">%s</td>
                        </tr>
                        <tr style="background:#f5f5f5;">
                          <td style="padding:10px 14px;font-size:12px;font-weight:700;color:#1a1a2e;border-top:1px solid #e0e0e0;">Diagnostic</td>
                          <td style="padding:10px 14px;font-size:12px;color:#333;border-top:1px solid #e0e0e0;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 14px;font-size:12px;font-weight:700;color:#1a1a2e;border-top:1px solid #e0e0e0;">Last Result Date</td>
                          <td style="padding:10px 14px;font-size:12px;color:#333;border-top:1px solid #e0e0e0;">%s</td>
                        </tr>
                      </table>

                      <div style="margin-top:20px;padding:14px;background:#fff8e1;border:1px solid #ffe082;border-radius:4px;">
                        <div style="font-size:12px;color:#5d4037;line-height:1.6;">
                          <strong>Action taken automatically:</strong> A new lab request (Alzheimer Follow-up)
                          has been created as PENDING in the system. Please review and confirm the appointment.
                        </div>
                      </div>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding:14px 32px;background:#1a1a2e;">
                      <div style="font-size:10px;color:#6b7280;">
                        © 2024 MediCareAI · Automated Alzheimer Follow-Up System
                      </div>
                    </td>
                  </tr>

                </table>
                </td></tr></table>
                </body></html>
                """.formatted(
                    riskColor, riskColor, risk,
                    patientName,
                    patientName,
                    riskColor, risk,
                    diagnostic != null ? diagnostic : "—",
                    resultDate
            );

            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("⚠️ Alzheimer Follow-Up Required — " + patientName + " [MediCareAI]");
            helper.setText(html, true);
            mailSender.send(mail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send follow-up alert: " + e.getMessage());
        }
    }

    // ============================================================
    // LabStaff Daily Performance Report
    // ============================================================

    public void sendPerformanceReport(List<LabStaffPerformanceDTO> stats,
                                       Long totalAnalyses,
                                       Long urgentCount,
                                       String reportDate) {
        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
            helper.setTo("wassimzarai28@gmail.com");
            helper.setSubject("Daily Lab Performance Report — " + reportDate + " [MediCareAI]");
            helper.setText(buildPerformanceReportTemplate(stats, totalAnalyses, urgentCount, reportDate), true);
            mailSender.send(mail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send performance report: " + e.getMessage());
        }
    }

    private String buildPerformanceReportTemplate(List<LabStaffPerformanceDTO> stats,
                                                   Long totalAnalyses,
                                                   Long urgentCount,
                                                   String reportDate) {

        String now = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // ── Build technician rows ──
        StringBuilder rows = new StringBuilder();
        for (LabStaffPerformanceDTO s : stats) {
            String urgentRate = s.getUrgentRate() != null
                    ? String.format("%.1f%%", s.getUrgentRate()) : "0.0%";
            String rateColor = (s.getUrgentRate() != null && s.getUrgentRate() > 30)
                    ? "#b71c1c" : (s.getUrgentRate() != null && s.getUrgentRate() > 15)
                    ? "#f57f17" : "#1b5e20";
            rows.append("""
                <tr style="border-bottom:1px solid #f0f0f0;">
                  <td style="padding:10px 12px;font-size:13px;font-weight:600;color:#1a1a2e;">%s</td>
                  <td style="padding:10px 12px;text-align:center;font-size:13px;color:#333;">%d</td>
                  <td style="padding:10px 12px;text-align:center;font-size:13px;color:#b71c1c;font-weight:700;">%d</td>
                  <td style="padding:10px 12px;text-align:center;font-size:13px;color:#f57f17;">%d</td>
                  <td style="padding:10px 12px;text-align:center;font-size:13px;color:#1565c0;">%d</td>
                  <td style="padding:10px 12px;text-align:center;font-size:13px;font-weight:700;color:%s;">%s</td>
                </tr>
                """.formatted(
                    s.getTechnicianName() != null ? s.getTechnicianName() : "Unknown",
                    s.getTotalAnalyses() != null ? s.getTotalAnalyses() : 0L,
                    s.getUrgentCases() != null ? s.getUrgentCases() : 0L,
                    s.getAttentionCases() != null ? s.getAttentionCases() : 0L,
                    s.getSurveillanceCases() != null ? s.getSurveillanceCases() : 0L,
                    rateColor, urgentRate
            ));
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"/></head>
            <body style="margin:0;padding:0;background:#ececec;font-family:Arial,Helvetica,sans-serif;">
            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#ececec;padding:32px 0;">
            <tr><td align="center">
            <table width="680" cellpadding="0" cellspacing="0"
                   style="background:#ffffff;border:1px solid #d0d0d0;border-collapse:collapse;">

              <!-- HEADER -->
              <tr>
                <td style="background:#1a1a2e;padding:22px 36px;">
                  <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                    <td>
                      <div style="font-size:20px;font-weight:700;color:#ffffff;letter-spacing:1px;">
                        MEDICARE<span style="color:#1a7a6e;">AI</span>
                      </div>
                      <div style="font-size:10px;color:#9ca3af;margin-top:3px;">
                        DAILY LAB STAFF PERFORMANCE REPORT
                      </div>
                    </td>
                    <td align="right">
                      <div style="font-size:10px;color:#9ca3af;">Date: %s</div>
                      <div style="font-size:10px;color:#9ca3af;margin-top:2px;">Generated: %s</div>
                    </td>
                  </tr></table>
                </td>
              </tr>

              <!-- SUMMARY CARDS -->
              <tr>
                <td style="padding:20px 36px;background:#f8f9fa;border-bottom:1px solid #e0e0e0;">
                  <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                    <td style="text-align:center;padding:12px;background:#fff;border:1px solid #e0e0e0;border-radius:4px;">
                      <div style="font-size:28px;font-weight:700;color:#1a1a2e;">%d</div>
                      <div style="font-size:11px;color:#888888;margin-top:2px;">Total Analyses</div>
                    </td>
                    <td style="width:16px;"></td>
                    <td style="text-align:center;padding:12px;background:#fff;border:1px solid #e0e0e0;border-radius:4px;">
                      <div style="font-size:28px;font-weight:700;color:#b71c1c;">%d</div>
                      <div style="font-size:11px;color:#888888;margin-top:2px;">Urgent Cases</div>
                    </td>
                    <td style="width:16px;"></td>
                    <td style="text-align:center;padding:12px;background:#fff;border:1px solid #e0e0e0;border-radius:4px;">
                      <div style="font-size:28px;font-weight:700;color:#1565c0;">%d</div>
                      <div style="font-size:11px;color:#888888;margin-top:2px;">Technicians Active</div>
                    </td>
                  </tr></table>
                </td>
              </tr>

              <!-- TABLE -->
              <tr>
                <td style="padding:20px 36px;">
                  <div style="font-size:10px;font-weight:700;color:#888888;
                              text-transform:uppercase;letter-spacing:.08em;margin-bottom:12px;">
                    Performance by Technician
                  </div>
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="border:1px solid #e0e0e0;border-collapse:collapse;">
                    <tr style="background:#1a1a2e;">
                      <th style="padding:10px 12px;text-align:left;font-size:11px;color:#ffffff;font-weight:600;">Technician</th>
                      <th style="padding:10px 12px;text-align:center;font-size:11px;color:#ffffff;font-weight:600;">Total</th>
                      <th style="padding:10px 12px;text-align:center;font-size:11px;color:#ef9a9a;font-weight:600;">URGENT</th>
                      <th style="padding:10px 12px;text-align:center;font-size:11px;color:#ffcc80;font-weight:600;">ATTENTION</th>
                      <th style="padding:10px 12px;text-align:center;font-size:11px;color:#90caf9;font-weight:600;">SURVEILLANCE</th>
                      <th style="padding:10px 12px;text-align:center;font-size:11px;color:#ffffff;font-weight:600;">Urgent Rate</th>
                    </tr>
                    %s
                  </table>
                </td>
              </tr>

              <!-- FOOTER -->
              <tr>
                <td style="padding:14px 36px;background:#1a1a2e;">
                  <div style="font-size:10px;color:#6b7280;">
                    © 2024 MediCareAI Health Systems · Automated daily report
                  </div>
                </td>
              </tr>

            </table>
            </td></tr></table>
            </body></html>
            """.formatted(
                reportDate, now,
                totalAnalyses != null ? totalAnalyses : 0L,
                urgentCount   != null ? urgentCount   : 0L,
                stats.size(),
                rows
        );
    }

    // ============================================================
    // Code Blue Emergency Emails
    // ============================================================

    public void sendCodeBlueAlert(String toEmail, String staffName, String triggeredBy,
                                   Long postId, String postTitle, String meetLink) {
        try {
            String now = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#ececec;font-family:Arial,Helvetica,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#ececec;padding:32px 0;">
                <tr><td align="center">
                <table width="600" cellpadding="0" cellspacing="0"
                       style="background:#ffffff;border:2px solid #c62828;border-collapse:collapse;">
                  <tr>
                    <td style="background:#b71c1c;padding:24px 36px;text-align:center;">
                      <div style="font-size:32px;margin-bottom:8px;">&#x1F198;</div>
                      <div style="font-size:22px;font-weight:700;color:#ffffff;letter-spacing:2px;">CODE BLUE</div>
                      <div style="font-size:11px;color:#ffcdd2;margin-top:4px;">MEDICAL EMERGENCY — IMMEDIATE ACTION REQUIRED</div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:20px 36px;background:#fff8f8;border-bottom:1px solid #ffcdd2;">
                      <div style="font-size:13px;color:#555;">
                        Dear <strong style="color:#1a1a2e;">%s</strong>,<br/><br/>
                        A <strong>CODE BLUE</strong> medical emergency has been triggered by
                        <strong style="color:#b71c1c;">%s</strong>.
                        Your presence and confirmation are required immediately.
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:20px 36px;">
                      <table width="100%%" cellpadding="0" cellspacing="0"
                             style="border:1px solid #e0e0e0;border-collapse:collapse;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#1a1a2e;width:40%%;">Incident</td>
                          <td style="padding:9px 14px;font-size:12px;color:#333;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#1a1a2e;border-top:1px solid #eee;">Triggered by</td>
                          <td style="padding:9px 14px;font-size:12px;color:#333;border-top:1px solid #eee;">%s</td>
                        </tr>
                        <tr style="background:#f5f5f5;">
                          <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#1a1a2e;border-top:1px solid #eee;">Alert time</td>
                          <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#b71c1c;border-top:1px solid #eee;">%s</td>
                        </tr>
                      </table>

                      <!-- Join Meeting Button -->
                      <div style="margin-top:20px;text-align:center;">
                        <a href="%s"
                           style="display:inline-block;background:#1565c0;color:#ffffff;
                                  text-decoration:none;font-size:14px;font-weight:700;
                                  padding:14px 32px;border-radius:8px;letter-spacing:0.5px;">
                          &#x1F4F9; Join Emergency Meeting
                        </a>
                        <div style="font-size:10px;color:#9ca3af;margin-top:8px;">
                          Link: %s
                        </div>
                      </div>

                      <div style="margin-top:16px;padding:12px 16px;background:#fff3e0;border-left:4px solid #e65100;">
                        <div style="font-size:12px;color:#4e342e;">
                          <strong>ACTION REQUIRED:</strong> Confirm your presence in the MediCareAI forum
                          AND join the emergency video meeting.
                        </div>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:12px 36px;background:#b71c1c;text-align:center;">
                      <div style="font-size:10px;color:#ffcdd2;">
                        © 2024 MediCareAI · Medical Emergency Alert System
                      </div>
                    </td>
                  </tr>
                </table>
                </td></tr></table>
                </body></html>
                """.formatted(staffName, triggeredBy, postTitle, triggeredBy, now, meetLink, meetLink);

            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[CODE BLUE] Medical Emergency — " + postTitle + " [MediCareAI]");
            helper.setText(html, true);
            mailSender.send(mail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send Code Blue alert: " + e.getMessage());
        }
    }

    public void sendCodeBlueEscalation(String toEmail, String doctorName, Long postId, String postTitle) {
        try {
            String now = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#ececec;font-family:Arial,Helvetica,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#ececec;padding:32px 0;">
                <tr><td align="center">
                <table width="600" cellpadding="0" cellspacing="0"
                       style="background:#ffffff;border:2px solid #6a1b9a;border-collapse:collapse;">
                  <tr>
                    <td style="background:#6a1b9a;padding:20px 36px;text-align:center;">
                      <div style="font-size:22px;font-weight:700;color:#fff;letter-spacing:1px;">
                        CODE BLUE ESCALATION
                      </div>
                      <div style="font-size:11px;color:#e1bee7;margin-top:3px;">
                        NO DOCTOR CONFIRMED — AUTOMATIC ESCALATION
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:20px 36px;">
                      <div style="font-size:13px;color:#555;margin-bottom:16px;">
                        Dr. <strong>%s</strong>, no doctor has confirmed their presence within
                        <strong>5 minutes</strong> of the CODE BLUE emergency below being triggered.
                        Immediate medical intervention is required.
                      </div>
                      <table width="100%%" cellpadding="0" cellspacing="0"
                             style="border:1px solid #e0e0e0;border-collapse:collapse;">
                        <tr style="background:#f5f5f5;">
                          <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#1a1a2e;width:40%%;">Incident</td>
                          <td style="padding:9px 14px;font-size:12px;color:#333;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#1a1a2e;border-top:1px solid #eee;">Escalated at</td>
                          <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#6a1b9a;border-top:1px solid #eee;">%s</td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:12px 36px;background:#6a1b9a;text-align:center;">
                      <div style="font-size:10px;color:#e1bee7;">
                        © 2024 MediCareAI · Automatic escalation after 5 min without doctor confirmation
                      </div>
                    </td>
                  </tr>
                </table>
                </td></tr></table>
                </body></html>
                """.formatted(doctorName, postTitle, now);

            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[CODE BLUE ESCALATION] Doctor required immediately [MediCareAI]");
            helper.setText(html, true);
            mailSender.send(mail);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send Code Blue escalation: " + e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Reset your MediCareAI password");
            helper.setText(buildEmailTemplate(resetLink), true); // true = HTML

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    private String buildEmailTemplate(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <style>
                body {
                  margin: 0;
                  padding: 0;
                  background-color: #f0f4f8;
                  font-family: 'Segoe UI', Arial, sans-serif;
                }
                .container {
                  max-width: 480px;
                  margin: 40px auto;
                  background: #ffffff;
                  border-radius: 16px;
                  overflow: hidden;
                  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
                }
                .header {
                  background: #ffffff;
                  padding: 36px 40px 20px;
                  text-align: center;
                }
                .logo {
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  gap: 10px;
                  margin-bottom: 28px;
                }
                .logo-icon {
                  background: #1a7a6e;
                  border-radius: 10px;
                  width: 42px;
                  height: 42px;
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                }
                .logo-text {
                  font-size: 22px;
                  font-weight: 700;
                  color: #1a7a6e;
                }
                .title {
                  font-size: 22px;
                  font-weight: 800;
                  color: #111;
                  margin: 0 0 16px;
                }
                .body {
                  padding: 0 40px 32px;
                  text-align: center;
                }
                .greeting {
                  font-size: 16px;
                  color: #333;
                  margin-bottom: 12px;
                }
                .message {
                  font-size: 14px;
                  color: #555;
                  line-height: 1.6;
                  margin-bottom: 28px;
                }
                .btn {
                  display: inline-block;
                  background: #1a7a6e;
                  color: #ffffff !important;
                  text-decoration: none;
                  padding: 16px 48px;
                  border-radius: 12px;
                  font-size: 16px;
                  font-weight: 600;
                  margin-bottom: 28px;
                }
                .expiry {
                  font-size: 13px;
                  color: #888;
                  margin-bottom: 0;
                }
                .divider {
                  border: none;
                  border-top: 1px solid #eee;
                  margin: 24px 40px;
                }
                .footer {
                  text-align: center;
                  padding: 0 40px 32px;
                }
                .footer-icons {
                  margin-bottom: 12px;
                  font-size: 20px;
                  color: #aaa;
                }
                .footer-text {
                  font-size: 11px;
                  color: #aaa;
                  margin-bottom: 8px;
                }
                .footer-links {
                  font-size: 11px;
                  color: #aaa;
                }
                .footer-links a {
                  color: #aaa;
                  text-decoration: none;
                  margin: 0 8px;
                }
              </style>
            </head>
            <body>
              <div class="container">

                <!-- Header -->
                <div class="header">
                  <div class="logo">
                    <div class="logo-icon">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                        <polyline points="2,12 6,6 10,14 14,8 18,16 22,12"
                          stroke="white" stroke-width="2"
                          stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                    </div>
                    <span class="logo-text">MediCareAI</span>
                  </div>
                  <h1 class="title">Reset your password</h1>
                </div>

                <!-- Body -->
                <div class="body">
                  <p class="greeting">Hello,</p>
                  <p class="message">
                    A password reset was requested for your MediCareAI account.
                    If you didn't make this request, please ignore this email.
                  </p>
                  <a href="%s" class="btn">Reset Password</a>
                  <p class="expiry">For security, this link will expire in 1 hour.</p>
                </div>

                <hr class="divider"/>

                <!-- Footer -->
                <div class="footer">
                  <p class="footer-text">© 2024 MEDICAREAI HEALTH SYSTEMS. ALL RIGHTS RESERVED.</p>
                  <div class="footer-links">
                    <a href="#">PRIVACY POLICY</a>
                    <a href="#">TERMS OF SERVICE</a>
                    <a href="#">SUPPORT</a>
                  </div>
                </div>

              </div>
            </body>
            </html>
            """.formatted(resetLink);
    }
}