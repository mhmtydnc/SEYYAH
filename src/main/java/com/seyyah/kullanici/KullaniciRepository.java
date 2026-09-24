package com.seyyah.kullanici;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KullaniciRepository extends JpaRepository<Kullanici, Long> {

    Optional<Kullanici> findByEposta(String eposta);
}
