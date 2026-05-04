package com.aziz.demosec.config;

import com.aziz.demosec.Entities.ChatChannel;
import com.aziz.demosec.repository.ChatChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatChannelInitializer implements ApplicationRunner {

    private final ChatChannelRepository channelRepository;

    private static final List<String[]> DEFAULT_CHANNELS = List.of(
        new String[]{"Général",      "Canal de discussion général pour tous les professionnels"},
        new String[]{"Urgences",     "Alertes et situations urgentes"},
        new String[]{"Médecins",     "Canal réservé aux médecins"},
        new String[]{"Pharmacie",    "Discussions sur les médicaments et prescriptions"},
        new String[]{"Nutrition",    "Conseils nutritionnels et diététique"},
        new String[]{"Laboratoire",  "Résultats et analyses de laboratoire"}
    );

    @Override
    public void run(ApplicationArguments args) {
        for (String[] ch : DEFAULT_CHANNELS) {
            if (!channelRepository.existsByName(ch[0])) {
                channelRepository.save(ChatChannel.builder()
                        .name(ch[0])
                        .description(ch[1])
                        .build());
                log.info("✅ Canal créé : {}", ch[0]);
            }
        }
        log.info("✅ {} canaux de messagerie disponibles", channelRepository.count());
    }
}
