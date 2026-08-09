package com.example.salonflow.ai.bootstrap;

import com.example.salonflow.entity.HairStyle;
import com.example.salonflow.entity.enums.hair.HairGender;
import com.example.salonflow.repository.HairStyleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HairStyleGenderBackfillRunner implements CommandLineRunner {

    private final HairStyleRepository hairStyleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<HairStyle> stylesToUpdate = new ArrayList<>();
        for (HairStyle style : hairStyleRepository.findAll()) {
            if (style.getGender() != null && style.getGender() != HairGender.UNISEX) {
                continue;
            }

            HairStyleSeedCatalog.genderForCode(style.getCode())
                    .ifPresent(gender -> {
                        style.setGender(gender);
                        stylesToUpdate.add(style);
                    });
        }

        if (!stylesToUpdate.isEmpty()) {
            hairStyleRepository.saveAll(stylesToUpdate);
            log.info("Backfilled gender for {} hair style(s)", stylesToUpdate.size());
        }
    }
}
